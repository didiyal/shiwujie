"""偏好抽取 —— design 3.9 chunk-2c 后台隐式抽取。

turn 结束后**异步**跑：先零成本关键词启发式判有无偏好信号（多数 turn 无 → 跳过省 LLM），
有信号才调便宜 LLM `with_structured_output` 抽结构化 JSON（核心三字段 + 任意额外），返回 dict
交 service `merge` 入库。失败（异常 / 空）→ 返回 `{}`（不 merge，下轮重试）——绝不阻塞主答、
绝不污染 store（design 3.9「失败无害下轮重试」）。

契约：
- `has_preference_signal(messages) -> bool`：关键词启发式（零 LLM），命中才值得抽。
- `extract_preferences(messages, model) -> dict`：async，LLM 抽结构化字段，失败兜底 {}。
- service turn_end 后台 `asyncio.create_task` fire（不 await，不阻塞 ndjson 流）。

不在 `memory/__init__` re-export：service REAL 分支懒导入（与 summarizer 同模式，规避循环 import）。
"""

from typing import List

from langchain_core.messages import BaseMessage, HumanMessage, SystemMessage
from pydantic import BaseModel, Field

# 偏好信号关键词（命中任一才调 LLM；多数 turn 无偏好 → 跳过省 token）。
# 风格 / 交通 / 习惯三类，覆盖核心三字段的表达线索。
_SIGNAL_KEYWORDS = (
    # communication_style
    "简洁", "详细", "啰嗦", "长话", "短说",
    # nav_mode
    "步行", "走路", "走路去", "公交", "地铁", "轨道交通", "驾车", "开车", "打车", "骑车", "单车",
    # frequent_apps / 通用偏好信号
    "偏好", "喜欢", "习惯", "常用", "常坐", "经常用", "一般用", "默认",
)

#: 抽取指令：只抽明确/强暗示的稳定偏好，不臆测，不确定留 null。
_EXTRACT_INSTRUCTION = (
    "你是用户偏好抽取助手。从下面的对话中抽取用户的长期稳定偏好。"
    "只抽用户明确表达或强烈暗示的偏好，不要臆测；不确定的字段必须留 null。"
    "字段：\n"
    "- communication_style：用户对回答风格的偏好。concise=要简洁，detailed=要详尽。不确定 null。\n"
    "- nav_mode：用户偏好的交通方式。walking=步行，transit=公交或地铁，driving=驾车或打车。不确定 null。\n"
    "- frequent_apps：用户常用的 APP 名称，多个用逗号分隔。不确定 null。"
)


class UserPreferences(BaseModel):
    """抽取 schema（核心三字段，均可选——LLM 只填有把握的）。"""

    communication_style: str | None = Field(
        None, description="回答风格偏好：concise 或 detailed。不确定 null。"
    )
    nav_mode: str | None = Field(
        None, description="交通方式偏好：walking/transit/driving。不确定 null。"
    )
    frequent_apps: str | None = Field(
        None, description="常用 APP 名称，逗号分隔。不确定 null。"
    )


def has_preference_signal(messages: List[BaseMessage]) -> bool:
    """关键词启发式：turn 内任一 message 命中信号词 → True（值得调 LLM 抽）。

    零 LLM 成本。多数 turn 无偏好信号 → False → service 跳过抽取，省 token。
    """
    for m in messages:
        content = m.content if isinstance(m.content, str) else ""
        if any(kw in content for kw in _SIGNAL_KEYWORDS):
            return True
    return False


async def extract_preferences(messages: List[BaseMessage], model) -> dict:
    """async LLM 抽结构化偏好 → dict（仅含非空字段）。失败 / 空 → {}。

    用 `with_structured_output(UserPreferences)`（FC 模式，qwen 已验）拿结构化结果；
    去掉 None/空值字段后返回，交 service `merge` 入库。任何异常 → {}（design 3.9 失败无害）。
    """
    structured = model.with_structured_output(UserPreferences)
    try:
        prefs = await structured.ainvoke(
            [
                SystemMessage(content=_EXTRACT_INSTRUCTION),
                HumanMessage(content=_format_transcript(messages)),
            ]
        )
    except Exception:
        return {}
    dumped = prefs.model_dump() if hasattr(prefs, "model_dump") else dict(prefs or {})
    return {k: v for k, v in dumped.items() if v}


def _format_transcript(messages: List[BaseMessage]) -> str:
    """messages → 紧凑文本（角色:内容）喂抽取模型。tool_calls 记工具名（行为信号）。"""
    role_label = {"human": "用户", "ai": "助手", "tool": "工具结果", "system": "系统"}
    lines = []
    for m in messages:
        role = role_label.get(getattr(m, "type", ""), getattr(m, "type", "?"))
        content = m.content if isinstance(m.content, str) else str(m.content)
        snippet = content.replace("\n", " ")
        tcs = getattr(m, "tool_calls", None) or []
        if tcs:
            names = ",".join(tc.get("name", "?") for tc in tcs)
            lines.append(f"{role} 调用[{names}]：{snippet}")
        else:
            lines.append(f"{role}：{snippet}")
    return "\n".join(lines)
