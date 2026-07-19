"""skill 加载器（Pi 式 read-on-demand）—— design ⑧。

load_skill(name)：读 skills/<name>/SKILL.md，剥 YAML frontmatter，返回 body（流程正文）作
read_skill 工具结果入上下文。system prompt 只常驻 <available_skills> 清单（几十 token），
body 按需 read 加载——非该流程的 turn 不背流程省 context（这是 skill 不 inline 进 prompt 的理由）。

list_skills()（注 system prompt 的 <available_skills> 清单用）待 prompts 层接入时补。
"""

from pathlib import Path

from ..constants import PKG_DIR

_SKILLS_DIR = PKG_DIR / "skills"


def load_skill(name: str) -> str:
    """读 skills/<name>/SKILL.md 的 body（frontmatter 之后的正文）。

    name 不存在 / 文件缺失 → FileNotFoundError（read_skill 工具经 ToolNode 转 isError observation，
    agent 下轮自愈：告诉用户该技能暂不可用）。
    """
    path = _SKILLS_DIR / name / "SKILL.md"
    text = path.read_text(encoding="utf-8")
    return _strip_frontmatter(text)


def _strip_frontmatter(text: str) -> str:
    """剥 YAML frontmatter（开头 --- ... ---），返回正文；无 frontmatter 原样返回。"""
    if not text.startswith("---"):
        return text.strip()
    end = text.find("\n---", 3)
    if end == -1:
        return text.strip()
    return text[end + len("\n---"):].strip()
