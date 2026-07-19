"""KB store —— design 3.8 文本库 + BM25 词法检索（v1 不引向量 RAG）。

KB = 盲人 app 功能说明（**事实**，答"是什么/怎么用"），非能力(tool)/非流程(skill)。search_kb(query)
→ 匹配文档原文返主 LLM 据以作答。小结构化语料（~10-40 篇短文，一篇一功能）：向量 embedding 杀鸡
用牛刀，整篇 verbatim 检索优于 chunk + 零幻觉 + 零依赖。启动全量载入内存（零 DB）。

检索：BM25Okapi over (title 加权 3x + aliases + tags + body)。
- **CJK char-bigram 分词**（非单字）：单字会让「家」跨文档误匹配（加入家庭 vs 家属），bigram
  （加入/入家/家庭 vs 家属）消除噪声，单/多命中判定干净可测。latin 词整词。
- 0 命中 → 未找到提示；恰 1 命中 → 返该文档 body 原文；≥2 命中 → 返 top-k(title+summary) 消歧。

何时升 RAG（记墙，design 3.8）：>100 篇 / 非结构化常识语料 / 跨文档综合问答 → 加 embedding。
"""

import re
from pathlib import Path

import yaml
from rank_bm25 import BM25Okapi

from ..constants import PKG_DIR

_DOCS_DIR = PKG_DIR / "kb" / "docs"


def _tokenize(text: str) -> list[str]:
    """latin 整词 + CJK char-bigram。bigram 消单字跨文档误匹配（见模块 docstring）。"""
    text = (text or "").lower()
    tokens = re.findall(r"[a-z0-9]+", text)
    cjk = re.findall(r"[一-鿿]", text)
    tokens.extend(cjk[i] + cjk[i + 1] for i in range(len(cjk) - 1))
    return tokens


def _split_frontmatter(text: str) -> tuple[dict, str]:
    """剥 YAML frontmatter（--- ... ---），返回 (meta_dict, body)。"""
    if not text.startswith("---"):
        return {}, text.strip()
    end = text.find("\n---", 3)
    if end == -1:
        return {}, text.strip()
    meta = yaml.safe_load(text[3:end]) or {}
    return meta, text[end + len("\n---"):].strip()


def _load() -> list[dict]:
    docs = []
    for path in sorted(_DOCS_DIR.glob("*.md")):
        meta, body = _split_frontmatter(path.read_text(encoding="utf-8"))
        docs.append(
            {
                "name": path.stem,
                "title": meta.get("title", path.stem),
                "aliases": meta.get("aliases", []) or [],
                "tags": meta.get("tags", []) or [],
                "summary": meta.get("summary", ""),
                "body": body,
            }
        )
    return docs


_DOCS = _load()
# 索引文本 = title×3（加权）+ aliases + tags + body；title 加权让标题/别名命中权重更高。
_CORPUS = [
    _tokenize(
        " ".join([d["title"]] * 3 + d["aliases"] + d["tags"]) + " " + d["body"]
    )
    for d in _DOCS
]
_BM25 = BM25Okapi(_CORPUS) if _CORPUS else None


def search(query: str, k: int = 3, dominance: float = 2.0) -> str:
    """BM25 检索。

    - 0 命中 → 未找到。
    - 仅 1 命中，或 top 压制 runner-up ≥ dominance 倍 → 返 top 的 body 原文。
      （dominance 过滤「怎么」类功能性虚词 bigram 造成的低分伪匹配——score>0 太松。）
    - 否则（多命中且分数接近）→ 返 top-k(title+summary) 消歧摘要让 LLM 选。
    """
    if _BM25 is None:
        return "（知识库为空）"
    scores = _BM25.get_scores(_tokenize(query))
    ranked = sorted(zip(_DOCS, scores), key=lambda x: x[1], reverse=True)
    matched = [(d, s) for d, s in ranked if s > 0]
    if not matched:
        return "未找到与您问题相关的功能说明。"
    if len(matched) == 1 or matched[0][1] >= dominance * matched[1][1]:
        return matched[0][0]["body"]
    top = matched[:k]
    lines = ["找到多个可能相关的功能说明，请选最贴合用户问题的："]
    for d, _ in top:
        lines.append(f'- {d["title"]}：{d["summary"]}')
    return "\n".join(lines)


def list_titles() -> list[str]:
    """文档标题清单（调试/可观测用）。"""
    return [d["title"] for d in _DOCS]
