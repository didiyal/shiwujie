"""skills 包：read-on-demand 流程技能（design ⑧）。v1 = navigation。

skill ≠ tool：skill 是多步流程手册（markdown），system prompt 只常驻 <available_skills> 清单，
body 按需 read_skill 加载入上下文，LLM 照流程用普通工具分多轮执行。加 skill 只需建
skills/<name>/SKILL.md（list_skills 自动收录——待 prompts 层接入）。
"""

from .loader import load_skill

__all__ = ["load_skill"]
