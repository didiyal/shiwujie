"""searchapi.io REST wrapper —— web_search 工具体（design ⑦ 同款自建 REST）。

分层（同 gaode.py）：_parse（searchapi 响应→归一化）/ _trim（top-k title+snippet 朗读友好）
/ _fetch（httpx 真端点，2c gated，懒导入）/ _mock（确定性假结果，2a FREE）。

**出参剪裁**（design ⑦ 论点）：drop link（盲人朗读用 snippet，url 是噪声）→ top-3 title+snippet，
省 token + 朗读友好。

何时连真：SEARCHAPI_KEY 非空 + SHIWUJIE_SEARCHAPI_FORCE_REAL=1。否则 mock（2a 默认）。
"""

import json
import os
from typing import List

from ..config import SEARCHAPI_KEY

_SEARCHAPI_BASE = "https://www.searchapi.io/api/v1/search"
_COMPACT = (",", ":")


def _use_real() -> bool:
    return bool(SEARCHAPI_KEY) and os.environ.get("SHIWUJIE_SEARCHAPI_FORCE_REAL", "") == "1"


def _parse_results(api_json: dict) -> List[dict]:
    """searchapi 响应 → [{title,snippet,link}]（取 organic_results）。"""
    out = []
    for r in api_json.get("organic_results") or []:
        out.append(
            {
                "title": r.get("title", ""),
                "snippet": r.get("snippet", ""),
                "link": r.get("link", ""),
            }
        )
    return out


def _trim_results(results: List[dict], k: int = 3) -> str:
    """top-k title+snippet（drop link——盲人朗读用 snippet，link 是噪声）。"""
    out = [
        {"title": r["title"], "snippet": r.get("snippet", "")}
        for r in results[:k]
        if r.get("title")
    ]
    return json.dumps(out, ensure_ascii=False, separators=_COMPACT)


def _mock_results(query: str) -> List[dict]:
    """确定性假结果（query 入 title 让多轮有区分度、可断言）。"""
    q = query or "搜索"
    return [
        {"title": f"关于「{q}」的搜索结果一", "snippet": f"这是与「{q}」相关的摘要示例文本一。", "link": ""},
        {"title": f"关于「{q}」的搜索结果二", "snippet": f"这是与「{q}」相关的摘要示例文本二。", "link": ""},
    ]


def _fetch_real(query: str) -> dict:
    """searchapi.io GET（engine=baidu）原生响应。chunk-2c 走。"""
    import httpx

    params = {"engine": "baidu", "q": query, "api_key": SEARCHAPI_KEY}
    r = httpx.get(_SEARCHAPI_BASE, params=params, timeout=8.0)
    return r.json()


def search(query: str, k: int = 3) -> str:
    """联网搜索 → top-k title+snippet JSON。real 路径异常 → 回退 mock（design ⑫）。"""
    if _use_real():
        try:
            raw = _fetch_real(query)
            results = _parse_results(raw)
        except Exception:
            results = _mock_results(query)
    else:
        results = _mock_results(query)
    if not results:
        return json.dumps(
            {"message": f"未搜到「{query}」相关结果。"}, ensure_ascii=False, separators=_COMPACT
        )
    return _trim_results(results, k)
