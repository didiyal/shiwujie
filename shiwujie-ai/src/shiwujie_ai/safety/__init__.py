"""safety 包：盲人 agent 安全护栏（design ⑬ 红队硬修正）。

- emergency：紧急求助 turn-bound token（红队 Q18 硬修正 1，graph 侧闸 ②）。
- whitelist：tool-name 白名单 + strict args 校验收口（硬修正 3，Python 侧两护栏 / canonical 契约 +
  observability + 2b 镜像；运行时闸是 ToolNode）。
"""

from . import emergency, whitelist

__all__ = ["emergency", "whitelist"]
