"""spike 桩结果——已迁移至生产 tools/stubs.py（单一真值源）。

本文件保留 re-export 供 spike FC runner 复用（runner 经 `from .stubs import stub_result` 取用）；
勿在此重复定义桩内容，改桩只改 tools/stubs.py。
"""

from ...tools.stubs import NAV_SKILL_BODY, stub_result

__all__ = ["NAV_SKILL_BODY", "stub_result"]
