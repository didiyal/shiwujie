"""graph State —— design.md ①。

MVP 仅 messages / blind_id 被 agent·tools 节点读写；position / available_skills /
issuing_turn 先落 schema 供后续层（prompts 注 position+技能清单 / safety turn-bound
token）即插即用，避免 State 反复改。

- messages：FC 核心。MessagesState 自带 add_messages reducer（追加非覆盖）。
- blind_id：会话/记忆/checkpoint 键（= thread_id，design ⑤）。
- position：每轮 {lat,lng,address}，entry 注 system prompt（design ②）。
- available_skills：v1 = ["navigation"]，prompts 层注 <available_skills> 清单（design ⑧）。
- issuing_turn：emergency turn-bound token 判据（design ⑬ 硬修正 1）。
"""

from typing import List, Optional

from langgraph.graph import MessagesState
from typing_extensions import NotRequired


class State(MessagesState):
    """扩展 MessagesState（messages + add_messages reducer）补盲人 agent 上下文字段。"""

    blind_id: str
    position: NotRequired[Optional[dict]]
    available_skills: NotRequired[List[str]]
    issuing_turn: NotRequired[int]
