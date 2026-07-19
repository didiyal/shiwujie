"""长期偏好存储 + system prompt 注入 —— design 3.9（跨会话 · 后台隐式抽取）。

内容 = **软性/风格化稳定事实**（说话方式 / 常用 APP / 导航习惯），**非**业务权威数据（家庭信息
走 MCP `family_info` 工具取，不重复存）。**merge-with-latest**（偏好可变，更新非追加）。

**chunk-2a：存储 + 注入框架**（进程内 dict 回退，零 Redis 依赖，测试免起 Redis）。
**chunk-2c：** ① **后台异步抽取**（turn 结束后不阻塞答，失败无害下轮重试）——结构化字段规则/启发式
抽 + 模糊字段便宜 LLM 抽，输出结构化 JSON 调 `merge` 入库；② **Redis hash backing**
（`ai:pref:{blind_id}` 热读）+ MySQL 持久。

**注入**：turn 起 `prompts.build_system_prompt` 读本 store → `format_preferences` 短段注 system prompt
（如「用户偏好：回答简洁；导航偏好步行。」）。小、常相关、温柔注入、绝不阻塞或强制动作
（design 3.9 盲人伦理：偏好软、随时被用户言行覆盖）。
"""

from typing import Optional

# 核心偏好字段（2a 框架；2c 抽取按需扩）。值均字符串，语义自描述。
_CORE_FIELDS = ("communication_style", "nav_mode", "frequent_apps")

# 枚举值 → 朗读友好标签。
_STYLE_LABEL = {"concise": "简洁", "detailed": "详尽"}
_NAV_LABEL = {"walking": "步行", "transit": "公交", "driving": "驾车"}


class PreferenceStore:
    """偏好存储：blind_id → dict[field, value]。merge-with-latest（覆写非追加）。

    chunk-2a 进程内 dict（跨轮同进程持久，对齐 MemorySaver / EmergencyTokenStore）；
    2c 换 Redis hash（`ai:pref:{blind_id}` 热读）+ MySQL 持久。
    """

    def __init__(self):
        self._by_blind: dict[str, dict] = {}

    def get(self, blind_id: str) -> dict:
        """读全量偏好（无 → {}，caller 不注入）。注入 hook 用。"""
        return dict(self._by_blind.get(blind_id, {}))

    def merge(self, blind_id: str, partial: Optional[dict]) -> dict:
        """merge-with-latest：partial 覆写对应字段（偏好可变，更新非追加）。返回 merge 后全量。

        2c 后台抽取调本方法。空 partial / None / 空串值字段跳过（不污染）。
        """
        if not partial:
            return self.get(blind_id)
        bucket = self._by_blind.setdefault(blind_id, {})
        for k, v in partial.items():
            if v is None or v == "":
                continue
            bucket[k] = v
        return dict(bucket)

    def clear(self, blind_id: Optional[str] = None) -> None:
        """清空（blind_id=None 清全表，测试用）。"""
        if blind_id is None:
            self._by_blind.clear()
        else:
            self._by_blind.pop(blind_id, None)


# 进程级单例（chunk-2a）。生产由 service 注入 Redis-backed store（2c）。
_store: PreferenceStore = PreferenceStore()


def reset_store() -> PreferenceStore:
    """测试用：清空模块 store，返回新 store 供直测。"""
    global _store
    _store = PreferenceStore()
    return _store


def get_store() -> PreferenceStore:
    """读模块单例 store（prompts 注入 hook / 2c 抽取器用）。"""
    return _store


def format_preferences(prefs: Optional[dict]) -> str:
    """偏好 dict → 注入 system prompt 的短段（自然语言，盲人朗读友好）。

    空 → ""（caller 不注入）。核心字段按朗读标签格式化；未知字段前向兼容尾附（2c 扩字段不破 2a）。
    """
    if not prefs:
        return ""
    parts = []
    style = prefs.get("communication_style")
    if style:
        parts.append(f"回答{_STYLE_LABEL.get(style, style)}")
    nav = prefs.get("nav_mode")
    if nav:
        parts.append(f"导航偏好{_NAV_LABEL.get(nav, nav)}")
    apps = prefs.get("frequent_apps")
    if apps:
        parts.append(f"常用 APP {apps}")
    # 未知字段前向兼容（2c 扩展字段直接展示「键: 值」）。
    for k, v in prefs.items():
        if k in _CORE_FIELDS or not v:
            continue
        parts.append(f"{k}: {v}")
    if not parts:
        return ""
    return "用户偏好：" + "；".join(parts) + "。"
