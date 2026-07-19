"""LLM 构造工厂——spike 唯一的 ChatOpenAI 构造点。

runner / __main__ 不直接 import langchain_openai：通过本模块 build_llm 注入给 runner，
便于换端点（改 config）/ mock 测试。base_url/key/model 来自 config.py，工具来自 tools。

两个工厂：
- build_llm：主对话/FC 模型，绑全部工具（agent 节点用）。
- build_llm_plain：不绑工具的裸 ChatOpenAI——给 compress 摘要用（无需 FC，省 16 工具 schema token）。
"""

from langchain_openai import ChatOpenAI

from . import config
from .tools import ALL_TOOLS


def build_llm(model: str, parallel: bool):
    """构造绑定全部工具的 LLM；parallel 控制 parallel_tool_calls。"""
    return (
        ChatOpenAI(
            base_url=config.BASE_URL,
            model=model,
            api_key=config.API_KEY,
            temperature=0,
            max_retries=4,
        )
        .bind_tools(ALL_TOOLS, parallel_tool_calls=parallel)
    )


def build_llm_plain(model: str):
    """构造不绑工具的裸 ChatOpenAI——给 compress 真摘要用（design 3.9，chunk-2c）。

    摘要无需 FC，省 16 工具 schema token + 避免摘要里误触 tool_call。
    """
    return ChatOpenAI(
        base_url=config.BASE_URL,
        model=model,
        api_key=config.API_KEY,
        temperature=0,
        max_retries=4,
    )
