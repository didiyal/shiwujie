"""Spike-1 报告生成：序列化 → 聚合 → 渲染 markdown go/no-go 报告。

与 runner 分家：runner 只跑到「执行 + 评分」（产出 list[CaseResult]）；
本模块把执行结果序列化成机读 dict、按 (model, arm, set) 聚合 pass-rate / 安全 / 质量指标、
渲染人读 markdown（Decision A 数据底稿）。__main__ 负责编排 run → serialize → aggregate → emit。

产出（写到 reports_dir）：
  fc_raw.json    全量逐 case/rep 明细（机读）
  fc_go_nogo.md  自动汇总（Decision A 数据底稿；C1-6 再据实写最终裁决）
"""

import json
import sys
from dataclasses import asdict
from datetime import datetime

from ... import config, constants
from .runner import CaseResult


def serialize(results: list[CaseResult], mode: str) -> dict:
    return {
        "generated_at": datetime.now().isoformat(timespec="seconds"),
        "mode": mode,
        "primary_model": config.PRIMARY_MODEL,
        "gate_clean_pass_rate": constants.GATE_CLEAN_PASS_RATE,
        "adversarial_downgrade_line": constants.ADVERSARIAL_DOWNGRADE_LINE,
        "reps": constants.REPS,
        "results": [
            {
                "case_id": r.case_id, "set": r.set_name, "category": r.category,
                "model": r.model, "arm": r.arm, "majority_pass": r.majority_pass,
                "reps": [asdict(rr) for rr in r.reps],
            }
            for r in results
        ],
    }


def aggregate(raw: dict) -> dict:
    """按 (model, arm, set) 聚合 pass-rate 与各安全/质量指标。"""
    agg: dict = {}
    for r in raw["results"]:
        key = (r["model"], r["arm"], r["set"])
        a = agg.setdefault(key, {"n": 0, "pass": 0, "by_cat": {},
                                 "write_misuse": 0, "over_call": 0,
                                 "args_invalid": 0, "hallucination": 0,
                                 "errored": 0})
        a["n"] += 1
        if r["majority_pass"]:
            a["pass"] += 1
        cat = a["by_cat"].setdefault(r["category"], {"n": 0, "pass": 0})
        cat["n"] += 1
        cat["pass"] += 1 if r["majority_pass"] else 0
        # 任一 rep 出现该指标即计（case 级）
        flags = {"write_misuse": False, "over_call": False,
                 "args_invalid": False, "hallucination": False, "errored": False}
        for rr in r["reps"]:
            if rr["error"]:
                flags["errored"] = True
            for t in rr["turns"]:
                if t.get("write_misuse"):
                    flags["write_misuse"] = True
                if t.get("over_call"):
                    flags["over_call"] = True
                if t.get("args_invalid"):
                    flags["args_invalid"] = True
                if t.get("hallucination"):
                    flags["hallucination"] = True
        for k, v in flags.items():
            if v:
                a[k] += 1
    # 算 rate
    for a in agg.values():
        a["pass_rate"] = round(a["pass"] / a["n"], 4) if a["n"] else 0
        for cat in a["by_cat"].values():
            cat["pass_rate"] = round(cat["pass"] / cat["n"], 4) if cat["n"] else 0
        for k in ("write_misuse", "over_call", "args_invalid", "hallucination", "errored"):
            a[f"{k}_rate"] = round(a[k] / a["n"], 4) if a["n"] else 0
    return {f"{m}|{arm}|{s}": v for (m, arm, s), v in agg.items()}


def emit(raw: dict, agg: dict, reports_dir):
    reports_dir.mkdir(parents=True, exist_ok=True)
    (reports_dir / "fc_raw.json").write_text(
        json.dumps(raw, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    (reports_dir / "fc_go_nogo.md").write_text(_markdown(raw, agg), encoding="utf-8")
    print(f"[done] -> {reports_dir/'fc_raw.json'} , {reports_dir/'fc_go_nogo.md'}", file=sys.stderr)


def _md_table(agg: dict, keys: list[str]) -> str:
    lines = ["| model | arm | set | n | pass-rate | write-misuse | over-call | args-invalid | halluc | errored |",
             "|---|---|---|---|---|---|---|---|---|---|"]
    for k in keys:
        a = agg.get(k)
        if not a:
            continue
        m, arm, s = k.split("|")
        lines.append(
            f"| {m} | {arm} | {s} | {a['n']} | {a['pass_rate']:.1%} | "
            f"{a['write_misuse_rate']:.1%} | {a['over_call_rate']:.1%} | "
            f"{a['args_invalid_rate']:.1%} | {a['hallucination_rate']:.1%} | {a['errored_rate']:.1%} |"
        )
    return "\n".join(lines)


def _markdown(raw: dict, agg: dict) -> str:
    pm = raw["primary_model"]
    gate = raw["gate_clean_pass_rate"]
    all_keys = list(agg.keys())

    # Decision A 主结论依据：PRIMARY / parallel / clean
    a_key = f"{pm}|parallel|clean"
    a = agg.get(a_key, {})
    clean_rate = a.get("pass_rate", 0)
    decision_a = "成立（go，溶进 loop）" if clean_rate >= gate else "翻车 → 考虑 B（加独立分类节点）"

    # 对抗集整体
    adv_keys = [k for k in all_keys if k.endswith("|adversarial")]
    adv_rates = {k: agg[k]["pass_rate"] for k in adv_keys}

    # 失败样例（PRIMARY/parallel/clean）
    fails = []
    for r in raw["results"]:
        if r["model"] == pm and r["arm"] == "parallel" and r["set"] == "clean" and not r["majority_pass"]:
            # 取首个非空 reason
            reason = ""
            for rr in r["reps"]:
                for t in rr["turns"]:
                    if t.get("reasons"):
                        reason = "; ".join(t["reasons"][:2])
                        break
                if reason:
                    break
            fails.append(f"- `{r['case_id']}` [{r['category']}] {reason}")
            if len(fails) >= 15:
                break

    md = []
    md.append("# Spike-1：qwen function-calling 可靠性（Decision A 数据底稿）\n")
    md.append(f"> 自动生成于 {raw['generated_at']}（mode={raw['mode']}）。本表是 runner 直出的数据；")
    md.append(f"> **最终 go/no-go 裁决见 reports/spikes_go_nogo.md（C1-6 综合 Spike-1+Spike-2 写）**。\n")
    md.append(f"- 主模型：`{pm}`（单模型，无对照臂）；重复 {raw['reps']} 次/严格多数票。")
    md.append(f"- gate：干净集 pass-rate **≥ {gate:.0%}** → Decision A 成立。\n")

    md.append("## PRIMARY 模型（主结论依据）\n")
    md.append(_md_table(agg, all_keys))
    md.append("")
    for k in all_keys:
        a = agg[k]
        md.append(f"### {k.replace('|', ' / ')}\n")
        md.append("| category | n | pass-rate |")
        md.append("|---|---|---|")
        for cat, cv in sorted(a["by_cat"].items()):
            md.append(f"| {cat} | {cv['n']} | {cv['pass_rate']:.1%} |")
        md.append("")

    md.append(f"## Decision A 初判（据 `{pm}` / parallel / clean）\n")
    md.append(f"- 干净集 pass-rate = **{clean_rate:.1%}**（gate {gate:.0%}）→ **{decision_a}**。")
    md.append(f"- 写工具误调用率（parallel/clean）= {a.get('write_misuse_rate',0):.1%}（目标近 0%）。")
    md.append(f"- 对抗集 pass-rate：{ {k.split('|')[1]+'|adv': round(v,3) for k,v in adv_rates.items()} }（< {raw['adversarial_downgrade_line']:.0%} 触发 B 兜底建议；仅报告不阻塞）。")
    md.append("")

    md.append("## 失败样例（PRIMARY / parallel / clean，前 15）\n")
    md.append("\n".join(fails) if fails else "- （无）")
    md.append("")

    return "\n".join(md)
