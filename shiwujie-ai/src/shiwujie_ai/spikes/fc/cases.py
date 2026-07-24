"""FC case 加载与归一化。

case 顶层扁平字段：id / category / input / tool_calls(必需) / optional(可选不强制) /
forbidden(绝不应出现) / args_checks(可选)；multi_turn 用 turns: [...] 每轮独立同字段。
"""

from dataclasses import dataclass, field
from pathlib import Path

import yaml


@dataclass
class Turn:
    input: str
    required: list[str]           # 期望必需调用的工具（漏调即 fail）
    optional: list[str]           # 调了不罚、不调不罚（吸收多步流程的「自然下一步」）
    forbidden: list[str]          # 绝不应出现的工具
    args_checks: dict = field(default_factory=dict)  # tool -> {field: value | {contains: str}}


@dataclass
class Case:
    id: str
    category: str
    set_name: str                 # "clean" | "adversarial"
    turns: list[Turn]             # 单轮 case：恰好 1 个 turn


def _turn(d: dict) -> Turn:
    return Turn(
        input=d["input"],
        required=list(d.get("tool_calls", [])),
        optional=list(d.get("optional", [])),
        forbidden=list(d.get("forbidden", [])),
        args_checks=d.get("args_checks", {}) or {},
    )


def load_cases(path: Path, set_name: str) -> list[Case]:
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    cases = []
    for d in data["cases"]:
        if "turns" in d:
            turns = [_turn(t) for t in d["turns"]]
        else:
            turns = [_turn(d)]
        cases.append(Case(id=d["id"], category=d["category"], set_name=set_name, turns=turns))
    return cases
