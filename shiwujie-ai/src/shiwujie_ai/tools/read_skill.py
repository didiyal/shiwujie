"""read_skill 元工具（1 个），用 @tool 注解定义。

Skill = 多步流程手册（Pi 式 read-on-demand）：system prompt 只常驻 <available_skills> 清单，
body 不常驻；用户要做该流程时 LLM 调 read_skill(name) → body 作 tool_result 入上下文 → 照流程用
普通工具一步步执行。当前仅 navigation 一个 skill。
"""

from typing import Literal

from langchain_core.tools import tool
from pydantic import BaseModel, Field


class ReadSkillArgs(BaseModel):
    name: Literal["navigation"] = Field(
        description="要加载的技能名。当前可用：navigation（多步导航流程）。"
    )


@tool(args_schema=ReadSkillArgs)
def read_skill(name: Literal["navigation"]) -> str:
    """加载一个多步流程技能文档按需阅读。当前可用：navigation（多步导航流程：搜地点 → 报选项 →
    问交通方式 → 算路线 → 朗读摘要 → 起导航）。仅当要执行这类多步流程时调用一次；调用后把文档内容
    作为参考，用 gaode_poi_search / gaode_route / launch_navigation 等具体工具分多轮执行。
    不要用它查功能说明（那是 search_kb）。"""
    raise NotImplementedError("spike: runner 用 stubs；chunk-2 填技能库读取")


READ_SKILL_TOOLS = [read_skill]
