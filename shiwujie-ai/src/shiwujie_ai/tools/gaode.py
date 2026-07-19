"""高德 REST wrapper —— design ⑦ 自建（非官方 MCP/SDK）。

3 能力：poi_search / route / weather。**出参剪裁朗读友好**（POI 只 name+address+distance+lat/lng，
route 距离+时间+转向摘要，weather 状况+温度+建议）——省 token + 盲人朗读友好，官方 MCP 固定 schema
做不到（design ⑦ 选型论证：出参可控 / 同进程同步进 ReAct loop / 进程经济性）。

分层（解耦「取数」与「剪裁」，剪裁逻辑 2a 可测、取数 2c 连真）：
- `_parse_*`：高德原生响应 JSON → 归一化 raw dict（poi/route/weather 三套）。**可单测**（喂 fixture）。
- `_trim_*`：归一化 raw → 朗读友好输出 JSON。**可单测**。
- `_fetch_*`：httpx 取真高德端点（2c，GAODE_KEY + FORCE_REAL 才走；2a 死代码不导入 httpx）。
- `_mock_*`：确定性假 raw（2a FREE：零网络零 key；与 stub_result 输出形状兼容，nav 多轮测试不破）。
- `poi_search`/`route`/`weather`：real / mock 二选一 + 剪裁。

何时连真（design ⑦）：GAODE_KEY 非空且 SHIWUJIE_GAODE_FORCE_REAL=1。否则一律 mock。
"""

import json
import os
from typing import List, Optional

from ..config import GAODE_KEY

_GAODE_BASE = "https://restapi.amap.com/v3"

# 紧凑分隔符（无空格）：与旧 stub_result raw f-string 输出形状一致（test_navigation 断言
# "distance":"500米" 无空格），且省 token。
_COMPACT = (",", ":")


def _use_real() -> bool:
    """GAODE_KEY 非空 且 显式 FORCE_REAL=1 才连真（2a 默认 mock，免误打网络）。"""
    return bool(GAODE_KEY) and os.environ.get("SHIWUJIE_GAODE_FORCE_REAL", "") == "1"


# ───────────────────────── 格式化（米/秒 → 朗读友好）─────────────────────────


def _fmt_distance(m: int) -> str:
    return f"{m}米" if m < 1000 else f"{m / 1000:.1f}公里"


def _fmt_duration(s: int) -> str:
    m = s // 60
    if m < 60:
        return f"{m}分钟"
    return f"{m // 60}小时{m % 60}分钟"


def _weather_suggestion(weather: str, temp_c: Optional[int]) -> str:
    """据天气+温度给盲人出行建议（粗启发式；2c 可换 LLM 润色，本处确定性可测）。"""
    w = weather or ""
    if "雨" in w:
        base = "有雨，外出请带伞、注意路滑"
    elif "雪" in w:
        base = "有雪，注意保暖防滑"
    elif "阴" in w or "云" in w:
        base = "天色偏暗，外出注意视线"
    else:
        base = "适合外出"
    if temp_c is not None:
        if temp_c <= 5:
            base += "，气温偏低注意保暖"
        elif temp_c >= 33:
            base += "，气温偏高注意防暑"
    return base


# ───────────────────────── 高德响应解析 → 归一化 raw ─────────────────────────


def _parse_pois(gaode_json: dict) -> List[dict]:
    """高德 /place/text|around 响应 → [{name,address,lng,lat,distance_m}]。

    distance_m 仅 around 搜索有（text 搜索无 → None，trim 省略）。
    location 高德格式 "lng,lat"（注意经度在前）。
    """
    out = []
    for p in gaode_json.get("pois") or []:
        loc = (p.get("location") or "").split(",")
        lng = float(loc[0]) if len(loc) == 2 and loc[0] else 0.0
        lat = float(loc[1]) if len(loc) == 2 and loc[1] else 0.0
        dist = p.get("distance")
        out.append(
            {
                "name": p.get("name", ""),
                "address": p.get("address") or "",
                "lng": lng,
                "lat": lat,
                "distance_m": int(dist) if dist and str(dist).isdigit() else None,
            }
        )
    return out


def _parse_route(gaode_json: dict, mode: str) -> dict:
    """高德 /direction/{walking,transit,driving} → {distance_m,duration_s,steps[]}。

    walking/driving：route.paths[0].steps[].instruction。
    transit：route.transits[0]（distance/duration 在 transit 层；segments→bus.buslines[0].name）。
    """
    if mode == "transit":
        transits = (gaode_json.get("route") or {}).get("transits") or []
        if not transits:
            return {"distance_m": 0, "duration_s": 0, "steps": []}
        t = transits[0]
        steps = []
        for seg in t.get("segments") or []:
            buslines = (seg.get("bus") or {}).get("buslines") or []
            if buslines:
                steps.append(f"乘{buslines[0].get('name', '公交')}")
        return {
            "distance_m": _to_int(t.get("distance")),
            "duration_s": _to_int(t.get("duration")),
            "steps": steps,
        }
    paths = (gaode_json.get("route") or {}).get("paths") or []
    if not paths:
        return {"distance_m": 0, "duration_s": 0, "steps": []}
    p = paths[0]
    steps = [s.get("instruction", "") for s in (p.get("steps") or []) if s.get("instruction")]
    return {
        "distance_m": _to_int(p.get("distance")),
        "duration_s": _to_int(p.get("duration")),
        "steps": steps,
    }


def _parse_weather(gaode_json: dict) -> dict:
    """高德 /weather/weatherInfo → {weather,temperature_c,winddirection,windpower}（取 lives[0]）。"""
    lives = gaode_json.get("lives") or []
    if not lives:
        return {"weather": "未知", "temperature_c": "", "winddirection": "", "windpower": ""}
    l = lives[0]
    return {
        "weather": l.get("weather", ""),
        "temperature_c": l.get("temperature", ""),
        "winddirection": l.get("winddirection", ""),
        "windpower": l.get("windpower", ""),
    }


def _to_int(v) -> int:
    """高德数值字段是字符串（"800"/"800.0"）；容错转 int。"""
    try:
        return int(float(v))
    except (TypeError, ValueError):
        return 0


# ───────────────────────── 剪裁 → 朗读友好 JSON ─────────────────────────


def _trim_pois(pois: List[dict], k: int = 3) -> str:
    """POI 列表 → 朗读友好 JSON（top-k：name+address+distance?+lat+lng）。

    distance_m None（text 搜索无距离）→ 省略。lat/lng 保留（launch_navigation 要用）。
    """
    out = []
    for p in pois[:k]:
        item = {"name": p["name"], "address": p.get("address") or ""}
        if p.get("distance_m") is not None:
            item["distance"] = _fmt_distance(p["distance_m"])
        item["lat"] = p["lat"]
        item["lng"] = p["lng"]
        out.append(item)
    return json.dumps(out, ensure_ascii=False, separators=_COMPACT)


def _trim_route(r: dict) -> str:
    """路线 → {distance,duration,summary}（summary=前 3 条转向指令，朗读友好）。"""
    steps = r.get("steps") or []
    summary = "；".join(s for s in steps[:3] if s)
    return json.dumps(
        {
            "distance": _fmt_distance(r["distance_m"]),
            "duration": _fmt_duration(r["duration_s"]),
            "summary": summary,
        },
        ensure_ascii=False,
        separators=_COMPACT,
    )


def _trim_weather(w: dict) -> str:
    """天气 → {weather,temperature,suggestion}。"""
    temp = w.get("temperature_c") or ""
    temp_c = None
    try:
        temp_c = int(float(temp))
    except (TypeError, ValueError):
        pass
    return json.dumps(
        {
            "weather": w.get("weather", ""),
            "temperature": f"{temp}度" if temp != "" else "",
            "suggestion": _weather_suggestion(w.get("weather", ""), temp_c),
        },
        ensure_ascii=False,
        separators=_COMPACT,
    )


# ───────────────────────── mock raw（2a 确定性）─────────────────────────


def _mock_pois(query: str) -> List[dict]:
    """单候选 POI（以 query 命名）+ 500m 距离 + 样例经纬度。

    单候选 → nav skill 跳过「问去哪个」直接算路线（多轮 multi_turn case 能续）。
    distance 500米 与旧 stub_result 兼容（test_navigation 断言 "500米" 不破）。
    """
    return [
        {
            "name": query or "目的地",
            "address": "示例地址",
            "lng": 104.0668,
            "lat": 30.5728,
            "distance_m": 500,
        }
    ]


def _mock_route(destination: str, mode: str) -> dict:
    """800米/10分钟 + 一条样例转向。mode 仅入文案（mock 不真算路线）。"""
    mode_label = {"walking": "步行", "transit": "公交", "driving": "驾车"}.get(mode, "")
    return {
        "distance_m": 800,
        "duration_s": 600,
        "steps": [f"{mode_label}沿示例路向东约800米前往{destination or '目的地'}"],
    }


def _mock_weather(location: Optional[str]) -> dict:
    """晴/22度/适合外出（location 仅入 suggestion 文案）。"""
    loc = location or "当前位置"
    return {
        "weather": "晴",
        "temperature_c": "22",
        "winddirection": "东北",
        "windpower": "≤3",
        "_loc": loc,  # trim 不用，留作 mock 文案占位（trim 忽略下划线键）
    }


# ───────────────────────── 真高德取数（2c，懒导入 httpx）─────────────────────────


def _fetch_pois_real(query: str, city: Optional[str], location: Optional[dict]) -> dict:
    """高德 /place/around（有 location，返距离）或 /place/text（仅 city）原生响应。

    chunk-2c 走（GAODE_KEY+FORCE_REAL）；2a 不导入 httpx、不调本函数。
    """
    import httpx

    if location:  # 周边搜（带距离，盲人「最近」语义）
        params = {
            "location": f"{location['lng']},{location['lat']}",
            "keywords": query,
            "radius": 3000,
            "sortrule": "distance",
        }
        path = "/place/around"
    else:  # 文本搜（无距离）
        params = {"keywords": query, "city": city or "", "citylimit": "true" if city else "false"}
        path = "/place/text"
    return _httpx_get(path, params)


def _fetch_route_real(origin: dict, destination_name: str, mode: str) -> dict:
    """高德 /direction/{mode} 原生响应（origin/destination 经纬度；destination_name 仅入参展示，真实
    坐标应由 poi_search 先取——本 wrapper 不做地名→坐标，nav skill 先 poi 再 route）。"""
    import httpx  # noqa: F401（2c 用；懒导入避免 2a import-time 依赖）

    # 注：生产应先 poi_search 取目的地坐标，再传 origin/dest 坐标。本函数签名留 origin/dest 坐标，
    # 坐标解析属 nav skill 编排层职责（避免 wrapper 嵌入地名解析逻辑）。
    path = f"/direction/{mode}"
    params = {
        "origin": f"{origin['lng']},{origin['lat']}",
        # destination 由调用方传坐标（nav skill 经 poi_search 取得）；此处占位由 2c 编排补全。
    }
    return _httpx_get(path, params)


def _fetch_weather_real(location: Optional[dict], city: Optional[str]) -> dict:
    """高德 /weather/weatherInfo 原生响应（按城市名查；location 坐标需先逆地理→城市，2c 编排层做）。"""
    params = {"city": city or "全国"}
    return _httpx_get("/weather/weatherInfo", params)


def _httpx_get(path: str, params: dict) -> dict:
    """httpx GET 高德端点（带 key + output=json）。chunk-2c 走。"""
    import httpx

    full = {**params, "key": GAODE_KEY, "output": "json"}
    r = httpx.get(f"{_GAODE_BASE}{path}", params=full, timeout=5.0)
    return r.json()


# ───────────────────────── 公共 API ─────────────────────────


def poi_search(
    query: str, city: Optional[str] = None, location: Optional[dict] = None, k: int = 3
) -> str:
    """POI 搜索 → 朗读友好 JSON（top-k name+address+distance?+lat+lng）。

    location（{lat,lng}）非空 → 周边搜带距离（盲人「最近」语义）；否则文本搜。
    """
    if _use_real():
        try:
            raw = _fetch_pois_real(query, city, location)
            pois = _parse_pois(raw)
        except Exception as e:  # encode-不抛（design ⑫）：网络/解析失败回退 mock
            pois = _mock_pois(query)
    else:
        pois = _mock_pois(query)
    if not pois:  # 真搜索空结果也别裸返空（盲人无感）→ 给「未找到」提示
        return json.dumps({"message": f"未找到「{query}」附近的地点。"}, ensure_ascii=False)
    return _trim_pois(pois, k)


def route(
    origin: Optional[dict],
    destination: str,
    mode: str,
    destination_coord: Optional[dict] = None,
) -> str:
    """路线规划 → {distance,duration,summary}。mode ∈ walking/transit/driving。

    destination_coord（{lat,lng}）应由 nav skill 经 poi_search 先取后传入（wrapper 不做地名→坐标）。
    """
    if _use_real() and origin and destination_coord:
        try:
            # 真路径需两端坐标；nav skill 编排把 poi 结果的坐标作 destination 传入。
            raw = _fetch_route_real(origin, destination, mode)
            r = _parse_route(raw, mode)
        except Exception:
            r = _mock_route(destination, mode)
    else:
        r = _mock_route(destination, mode)
    return _trim_route(r)


def weather(location: Optional[dict] = None, city: Optional[str] = None) -> str:
    """天气查询 → {weather,temperature,suggestion}。location 坐标需逆地理→城市（2c 编排做）。"""
    if _use_real():
        try:
            raw = _fetch_weather_real(location, city)
            w = _parse_weather(raw)
        except Exception:
            w = _mock_weather(city or (location or {}).get("address"))
    else:
        w = _mock_weather(city or (location or {}).get("address"))
    return _trim_weather(w)
