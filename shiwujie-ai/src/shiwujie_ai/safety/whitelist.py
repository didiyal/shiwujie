"""tool-name 白名单 + strict args 收口 —— design ⑬ 红队硬修正 3（Python 侧两护栏）。

红队 Q18 攻击面：qwen 可幻觉工具名（如伪 `confirm_emergency`、或旧未拆分的
`request_emergency_help`——后者会让模型一轮内单调用「确认」绕过 turn-bound token 闸）。本模块是
Python 侧的**收口**：

- **护栏 A（tool-name 白名单）**：仅 ALL_TOOLS 暴露的 16 名合法，幻觉名一律拒。
- **护栏 B（strict args）**：name 对应的 Pydantic args_schema（ALL_ARG_MODELS）model_validate，
  失败即拒（含 `extra="forbid"` 字段门、Literal 枚举门、缺必填）。

**与 ToolNode 的关系**（defense-in-depth）：LangGraph prebuilt ToolNode 已强制
unknown-tool-name→isError + Pydantic validation——本模块不重复运行时闸，而提供三件独立价值：
① **canonical 契约**（TOOL_WHITELIST 单一真值，防约定腐烂——增删工具测试即捕获）；
② **observability**（classify_tool_calls 把模型 tool_calls 分 valid/hallucinated/bad_args，供 FC 退化
  监测 → 触发 Decision B 兜底）；
③ **2b 镜像**（Python 侧收口 = 2b Java MCP server strict schema gate 的对等物，跨进程一致）。

chunk-2c 若 observability 显示幻觉率抬头，再把 validate_tool_call 接进 agent 节点作 pre-flight。
"""

from typing import Optional

from ..tools import ALL_ARG_MODELS

#: 暴露给 qwen 的合法工具名闭集（单一真值，从 ALL_ARG_MODELS 派生）。幻觉名不在此即拒。
TOOL_WHITELIST = frozenset(ALL_ARG_MODELS.keys())


def validate_tool_call(name: str, args: Optional[dict]) -> tuple[bool, Optional[str]]:
    """护栏 A+B：name 在白名单 且 args 过 schema → (True, None)；否则 (False, 原因)。

    args=None 当空 args 处理（无参工具 family_info/request_video_help 合法）。
    """
    if name not in TOOL_WHITELIST:
        return False, f"幻觉工具名「{name}」不在白名单——拒绝（design ⑬ 硬修正 3 护栏 A）。"
    schema = ALL_ARG_MODELS[name]
    try:
        schema.model_validate(args or {})
    except Exception as e:  # Pydantic ValidationError（extra=forbid / Literal / 缺必填 / 类型不符）
        return False, f"工具「{name}」参数校验失败：{e}"
    return True, None


def classify_tool_calls(tool_calls) -> dict:
    """observability：把模型 tool_calls（[{name,args,id}, ...] 或 ToolCall 对象）分三类。

    返回 {"valid":[(name,args)], "hallucinated":[name], "bad_args":[(name,msg)]}。
    供 FC 退化监测 / 触发 Decision B；不阻塞 graph（运行时闸是 ToolNode）。
    """
    valid, hallucinated, bad_args = [], [], []
    for tc in tool_calls or []:
        name = tc.get("name") if isinstance(tc, dict) else getattr(tc, "name", None)
        args = tc.get("args") if isinstance(tc, dict) else getattr(tc, "args", None)
        if name not in TOOL_WHITELIST:
            hallucinated.append(name)
            continue
        ok, msg = validate_tool_call(name, args)
        if ok:
            valid.append((name, args))
        else:
            bad_args.append((name, msg))
    return {"valid": valid, "hallucinated": hallucinated, "bad_args": bad_args}
