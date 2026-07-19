"""service 包：FastAPI HTTP 入口（缝 A Python 侧逐 turn 端点）。

chunk-2a 薄版（非流式）：POST /ai/turn 跑 graph 返末消息 + GET /health。
流式（stream_mode=custom 进度事件 + StreamingResponse）拆后续子切片。
"""

from .app import app, create_app

__all__ = ["app", "create_app"]
