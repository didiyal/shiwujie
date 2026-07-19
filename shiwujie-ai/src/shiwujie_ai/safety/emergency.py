"""紧急求助 turn-bound token —— design ⑬ 红队硬修正 1（graph 侧闸 ②）。

红队 Q18 致命洞：qwen 可在单个 AIMessage 发并行 tool_calls，ToolNode 并发执行——一轮内可同时
prepare() + confirm(竞态命中真 token)，同轮内发 WS5003，**确认问题从没问过**。结构修复：token 绑
(blind_id, issuing_turn)，confirm() **拒绝同轮 token**——token 必须在更早轮签发，即确认问题在跨轮
边界被问过（用户在中间轮答了「是」）。配 emergency 工具 parallel_tool_calls=False（llm.build_llm，
C2a-3 另半）+ App 显式确认面（chunk-2e）= 三道闸。

机制：
- **invoke 边界**（service/测试层）调 set_turn_context(blind_id, issuing_turn) 灌 contextvar——
  graph.invoke 同步执行，整轮（agent+tools）共享该 contextvar；不进 agent 节点（解耦 + 鲁棒）。
- prepare() 读 contextvar → 签 token 绑 (blind_id, issuing_turn)，存 store（此时不通知家属）。
- confirm(token) 读 contextvar → 校验：bound_turn == current_turn → **拒**（同轮攻击面）；
  bound_turn < current_turn → 放行（跨轮 = 确认被问过）。
- **fail-closed**：contextvar 未设（caller 漏调）→ 降级 turn=0，prepare/confirm 同轮 → confirm 永拒，
  绝不会误发紧急求助（盲人安全优先）。

存储：chunk-2a 进程内 dict（跨轮同进程持久，对齐 MemorySaver）；C2a-6 换 Redis（key `ai:emerg:`）。
thread_id == blind_id（design ⑤），故 token 仅绑 (blind_id, issuing_turn)。
"""

import contextvars
import json
import secrets
from typing import Callable, Optional


# 当前 turn 上下文（invoke 边界 set；同 turn 内 prepare/confirm 工具读）。
_CURRENT_TURN: contextvars.ContextVar[Optional[dict]] = contextvars.ContextVar(
    "shiwujie_emergency_turn", default=None
)


def set_turn_context(blind_id: str, issuing_turn: int) -> None:
    """每轮 graph.invoke 前（service/测试层）调：灌 (blind_id, issuing_turn) 进 contextvar。

    issuing_turn 由 service 层按 turn 递增（C2a-7）；测试层显式传。同 turn 内 prepare/confirm 共用。
    """
    _CURRENT_TURN.set({"blind_id": blind_id, "issuing_turn": issuing_turn})


def _ctx_or_default() -> dict:
    """读 contextvar；未设降级 turn=0（fail-closed：confirm 将因同轮被拒）。"""
    ctx = _CURRENT_TURN.get()
    if ctx is None:
        return {"blind_id": "unknown", "issuing_turn": 0}
    return ctx


class EmergencyTokenStore:
    """紧急 token 存储。token 绑 (blind_id, issuing_turn)；verify 拒同轮 token（结构保证跨轮边界）。

    chunk-2a 进程内 dict；C2a-6 换 Redis。token_factory 注入为测试确定性（生产 secrets.token_hex）。
    """

    def __init__(
        self,
        token_factory: Callable[[], str] = lambda: "EMERG-" + secrets.token_hex(3).upper(),
    ):
        self._token_factory = token_factory
        self._by_token: dict[str, dict] = {}

    def issue(self, blind_id: str, issuing_turn: int) -> str:
        token = self._token_factory()
        self._by_token[token] = {"blind_id": blind_id, "issuing_turn": issuing_turn}
        return token

    def verify(self, token: str, blind_id: str, current_turn: int) -> tuple[bool, str]:
        rec = self._by_token.get(token)
        if rec is None:
            return False, "token 不存在或已失效——必须先用 request_emergency_help_prepare 生成。"
        if rec["blind_id"] != blind_id:
            return False, "token 不属于当前用户。"
        if rec["issuing_turn"] == current_turn:
            # 🔴 核心护栏（红队 Q18）：同轮 prepare+confirm = 确认问题没机会被问。
            return False, (
                "同轮内不能既 prepare 又 confirm——必须先 prepare、向用户确认后，"
                "等用户下一轮明确答复再 confirm。"
            )
        if rec["issuing_turn"] > current_turn:
            return False, "token 签发轮晚于当前轮（异常，拒绝）。"
        # bound_turn < current_turn → 跨了轮，确认问题在中间轮被问过 → 放行。
        del self._by_token[token]  # 一次性消费
        return True, "已通知所有家属（信令5003 群发）。"


# 进程级单例（chunk-2a）。生产由 service 层注入 Redis-backed store（C2a-6）。
_store: EmergencyTokenStore = EmergencyTokenStore()


def reset_store(token_factory: Optional[Callable[[], str]] = None) -> EmergencyTokenStore:
    """测试用：重置模块 store（清 token + 可换确定性 token_factory）。返回新 store 供直测。"""
    global _store
    factory = token_factory or (lambda: "EMERG-" + secrets.token_hex(3).upper())
    _store = EmergencyTokenStore(factory)
    return _store


def prepare(reason: Optional[str] = None) -> str:
    """@tool request_emergency_help_prepare 体：签发 turn-bound token（此时不通知家属）。"""
    ctx = _ctx_or_default()
    token = _store.issue(ctx["blind_id"], ctx["issuing_turn"])
    return json.dumps(
        {
            "token": token,
            "message": "确认码已生成。请向用户确认后，用本 token 调用 request_emergency_help_confirm。",
        },
        ensure_ascii=False,
    )


def confirm(token: str) -> str:
    """@tool request_emergency_help_confirm 体：校验 turn-bound token，通过才通知家属。

    软拒绝（返 status=rejected 的 JSON，非抛异常）——agent 读到拒绝消息后可向用户解释/重新确认，
    符合 design ⑫ encode-不抛契约。
    """
    ctx = _ctx_or_default()
    ok, msg = _store.verify(token, ctx["blind_id"], ctx["issuing_turn"])
    return json.dumps({"status": "ok" if ok else "rejected", "message": msg}, ensure_ascii=False)
