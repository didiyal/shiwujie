"""KB BM25 检索测试 —— design 3.8 文本库 + char-bigram 词法检索。

验：单命中返 body 原文；多候选返消歧摘要；无关查询返未找到；盲人口语化别名命中。
char-bigram 分词让单/多命中判定干净（单字「家」不会把「加入家庭」误连到「家属」）。
"""

from shiwujie_ai.kb import search


def test_single_match_returns_body():
    """「怎么加入家庭」只命中 join-family（bigram 加入/入家/家庭 不与他文交叉）→ 返原文。"""
    r = search("怎么加入家庭")
    assert "加入家庭" in r
    assert "找到多个" not in r  # 单命中→body，非消歧摘要


def test_alias_match_colloquial():
    """盲人口语「找人帮忙」经 volunteer-call 别名命中（非字面标题）。"""
    r = search("怎么找人帮忙")
    assert "未找到" not in r
    assert "志愿者" in r  # 命中 volunteer-call body


def test_no_match():
    r = search("量子力学波函数")
    assert "未找到" in r


def test_multiple_candidates_disambiguate():
    """「求助」同时命中紧急求助 + 志愿者视频通话（两者 bigram 均含 求助）→ 返消歧摘要。"""
    r = search("求助")
    assert "找到多个" in r
    assert "紧急求助" in r
    assert "志愿者视频通话" in r
