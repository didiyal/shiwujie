"""kb 包：功能说明知识库（design 3.8 文本库 + BM25）。

KB = 事实（答"是什么/怎么用"），非 tool/非 skill。search_kb 工具查本库返文档原文给主 LLM 据以作答。
v1 = 小结构化 markdown 语料 + BM25 char-bigram 词法检索（不引向量 RAG，记墙撞到再引）。
加文档只需往 kb/docs/ 丢一篇带 frontmatter 的 markdown（_load 启动自动收录）。
"""

from .store import search

__all__ = ["search"]
