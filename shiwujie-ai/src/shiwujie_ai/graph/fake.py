"""FakeChatModel —— chunk-2a 服务烟测 / 控制流测试用的零 token 模型。

生产路径用 llm.build_llm() 真 ChatOpenAI（qwen3.7-plus）；chunk-2a 验证一律注入本 fake（零 token、
零网络），chunk-2c 换回真模型即端到端。graph 对模型实例无感（DI）。

非脚本式（区别于 tests/ 里 _RecChatModel/_ScriptChatModel 那种固定 AIMessage 序列）：按 reply_builder
从当前消息构造一条**无 tool_call** 的 AIMessage——能吃任意 curl 输入，证明 HTTP→graph→HTTP 通路
（请求解析 / position 注入 / graph 调用 / 末消息回流），非「AI 答得好」（那是 chunk-2c）。
"""

from typing import Callable, List

from langchain_core.messages import AIMessage, BaseMessage


def echo_reply(messages: List[BaseMessage]) -> str:
    """默认回声：取最后一条 HumanMessage 内容回声。证明消息流通路。"""
    for m in reversed(messages):
        if getattr(m, "type", None) == "human":
            return f"（FakeChatModel 回声）已收到：{m.content}"
    return "（FakeChatModel 回声）未识别到用户消息。"


class FakeChatModel:
    """零 token 假模型。bind_tools 直返 self（忽略 parallel_tool_calls 等）；invoke 返回声 AIMessage。"""

    def __init__(self, reply_builder: Callable[[List[BaseMessage]], str] = echo_reply):
        self._build = reply_builder

    def bind_tools(self, tools, **kwargs):
        return self

    def invoke(self, messages, **kwargs):
        return AIMessage(content=self._build(list(messages)))
