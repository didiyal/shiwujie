"""safety 包：盲人 agent 安全护栏（design ⑬ 红队硬修正）。

- emergency：紧急求助 turn-bound token（红队 Q18 硬修正 1，graph 侧闸 ②）。
- whitelist：tool-name 白名单 + strict args 校验收口（硬修正 3，Python 侧两护栏 / canonical 契约 +
  observability + 2b 镜像；运行时闸是 ToolNode）。
"""

# emergency eager 加载：java_mcp / service 层 `from ..safety import emergency` 依赖它，
# emergency 不反向依赖 tools（无循环风险）。
from . import emergency

# whitelist **懒加载**（__getattr__ 见下）：whitelist.py 顶部 `from ..tools import ALL_ARG_MODELS`，
# 若 eager 加载会与 tools→java_mcp→safety 形成循环 import——当 tools 侧先被入口（如 llm.build_llm
# 首引 tools 时），ALL_ARG_MODELS 在 tools/__init__ 尚未定义 → ImportError。懒加载后 whitelist
# 首次被访问（运行时校验 / 测试）才加载，彼时 tools 必已完整。直接 `from ..safety.whitelist import X`
# 不受影响（Python 直载子模块）。
__all__ = ["emergency", "whitelist"]


def __getattr__(name):
    if name == "whitelist":
        from . import whitelist
        return whitelist
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
