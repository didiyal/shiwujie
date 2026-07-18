"""16 个工具统一用 @tool 注解定义（BaseTool 列表）：6 native + 1 read_skill + 9 java_mcp。

每个工具 = 一个 @tool 装饰函数：
- name = 函数名；description = docstring（内嵌 Phase 3.5 护栏）；args_schema = Pydantic Args 模型。
- spike 阶段函数体 raise NotImplementedError（runner 用 stubs 喂假结果）；chunk-2 填真实实现。

ALL_ARG_MODELS 由 {t.name: t.args_schema} 自动派生（scorer 取 model_validate 校验 args），
不再手写 name→model 映射——改名/增删工具只需改一处（@tool 函数本身）。

缝 A 设计：FC 面向的工具 = 6 Python-native + 1 read_skill + 9 Java-MCP（其中 request_emergency_help
按红队硬修正拆 prepare/confirm 双工具）= 共 16 个暴露给 qwen。
"""

from .java_mcp import JAVA_MCP_TOOLS
from .native import NATIVE_TOOLS
from .read_skill import READ_SKILL_TOOLS

#: 暴露给 qwen 的全部工具（BaseTool 列表）—— FC spike 与未来 graph 共用。
ALL_TOOLS = NATIVE_TOOLS + READ_SKILL_TOOLS + JAVA_MCP_TOOLS

#: name -> Pydantic args_schema（scorer 校验 args 用；自动派生，勿手写）。
ALL_ARG_MODELS = {t.name: t.args_schema for t in ALL_TOOLS}

#: 写类工具（误调用即安全事故）—— 单独统计误调用率。
WRITE_TOOLS = {
    "join_family",
    "leave_family",
    "update_profile",
    "request_emergency_help_prepare",
    "request_emergency_help_confirm",
}
