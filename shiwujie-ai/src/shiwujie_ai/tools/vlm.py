"""VLM（视觉语言模型）wrapper —— design ① 拍照识别。

两入口同一 VLM（qwen3-vl-flash）：
- **拍照按钮**：entry 节点直连 VLM 绕 loop（design ② 确定性 fork），结果写回 messages 供语音追问。
- **对话内 recognize_photo 工具**：agent 决定现拍一张 → 调本 wrapper。

chunk-2a：mock 返确定性假描述（零 token、零 VLM 调用）。chunk-2c：连真 qwen3-vl-flash
（ChatOpenAI 多模态 HumanMessage{image_url, text}，≤100 字语音友好）。

**图片来源**：recognize_photo 工具体收不到 state（ToolNode 只传 args）→ 走 contextvar
（service 边界 `set_image_context`，仿 emergency `set_turn_context`）；2a 不灌 → None → mock。
"""

import contextvars
import logging
import os
from typing import Optional

from ..config import API_KEY, BASE_URL, VLM_MODEL

logger = logging.getLogger(__name__)

# 当前轮图片（url 或 base64）；service 边界灌（2c），2a 默认 None → mock。
_CURRENT_IMAGE: contextvars.ContextVar[Optional[str]] = contextvars.ContextVar(
    "shiwujie_current_image", default=None
)


def set_image_context(image: Optional[str]) -> None:
    """service 边界灌当前轮图片（thread 开始前调一次）。2c 用；2a 不灌 = mock。"""
    _CURRENT_IMAGE.set(image)


def _current_image() -> Optional[str]:
    return _CURRENT_IMAGE.get()


def _use_real() -> bool:
    """API_KEY 非空 且 显式 FORCE_REAL=1 才连真 VLM（2a 默认 mock）。"""
    return bool(API_KEY) and os.environ.get("SHIWUJIE_VLM_FORCE_REAL", "") == "1"


def _mock_description(question: Optional[str]) -> str:
    """确定性假描述（question 入文案让多轮追问有区分度，可断言）。"""
    q = question or "前方场景"
    return f"识别到：关于「{q}」——前方是一辆共享单车，左侧有盲道延伸，右侧是商铺入口。"


def _recognize_real(image: str, question: Optional[str]) -> str:
    """chunk-2c：ChatOpenAI qwen3-vl-flash 多模态识别（懒导入 langchain_openai，2a 不触）。"""
    from langchain_core.messages import HumanMessage
    from langchain_openai import ChatOpenAI

    vlm = ChatOpenAI(
        base_url=BASE_URL, model=VLM_MODEL, api_key=API_KEY, temperature=0, max_retries=2
    )
    prompt = question or "请用不超过 100 字描述图片主要内容，侧重盲人关心的障碍物、文字、方位。"
    content = [
        {"type": "image_url", "image_url": {"url": image}},
        {"type": "text", "text": prompt},
    ]
    resp = vlm.invoke([HumanMessage(content=content)])
    return resp.content if hasattr(resp, "content") else str(resp)


def recognize(question: Optional[str] = None, image: Optional[str] = None) -> str:
    """VLM 识别 → 描述文本。

    image 显式传入优先；否则取 contextvar（service 边界灌）；都无 → mock（2a 默认）。
    real 路径异常 → 回退 mock（design ⑫ encode-不抛），但**记 warning**（诊断可见性：
    静默回退会让"真路径挂了"现象与"没开开关"无异，无法排查）。
    """
    img = image or _current_image()
    if not _use_real():
        return _mock_description(question)
    if not img:
        logger.warning("VLM 真模式但当前轮无图片（contextvar 未灌 / image 未传），回退 mock")
        return _mock_description(question)
    try:
        return _recognize_real(img, question)
    except Exception as e:
        logger.warning("VLM 真识别失败，回退 mock：%s", e, exc_info=True)
        return _mock_description(question)
