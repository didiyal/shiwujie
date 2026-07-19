"""两层记忆 —— design 3.9。

- **短期**（`compress`）：会话窗口压缩。model.invoke 前的非破坏 transform
  （≈ Pi transformContext），checkpoint 仍存全量 messages。
- **长期**（`preference`）：跨会话偏好，后台异步抽取注入 system prompt。

chunk-2a：summarizer 是确定性 fake（零 token）；偏好抽取留 2c，本块只做存储+注入框架。
"""

from .compress import (
    DEFAULT_TAIL_SIZE,
    DEFAULT_THRESHOLD,
    compress_messages,
    default_fake_summarizer,
    estimate_tokens,
)
from .preference import (
    PreferenceStore,
    format_preferences,
    get_store,
    reset_store,
)

__all__ = [
    # 短期压缩
    "compress_messages",
    "estimate_tokens",
    "default_fake_summarizer",
    "DEFAULT_THRESHOLD",
    "DEFAULT_TAIL_SIZE",
    # 长期偏好
    "PreferenceStore",
    "format_preferences",
    "get_store",
    "reset_store",
]
