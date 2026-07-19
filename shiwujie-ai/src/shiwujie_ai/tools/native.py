"""Python-native 工具（6 个能力），用 @tool 注解定义。

工具体演进：
- spike 阶段：runner 用 stubs 喂假结果，体不执行（raise NotImplementedError）。
- chunk-2a 早期：体走 tools/stubs.stub_result 桩返回——让 graph 的 ToolNode 能真调通，
  验证控制流（零网络/零 token）。
- **C2a-4（当前完成）：6 体全换真 wrapper**——recognize_photo→vlm、web_search→searchapi、
  get_weather/gaode_*→gaode（design ⑦ 自建 REST）、search_kb→kb BM25。wrapper 内部 2a 走
  mock（零网络零 token）/ 2c 走真外部 API（key+FORCE_REAL），schema/docstring 护栏措辞不动。
  stub_result 不再被本模块引用（stubs.py 仍供 FC runner 兼容用）。

@tool(args_schema=...) 复用下方 Pydantic Args 模型：name=函数名、description=docstring、
args_schema=Pydantic 模型（scorer 经 ALL_ARG_MODELS 取 model_validate 校验）。
"""

from typing import Literal, Optional

from langchain_core.tools import tool
from pydantic import BaseModel, Field

from . import gaode, searchapi, vlm
from ..kb import search as kb_search


# ───────────────────────── Args 模型（scorer 用 model_validate 校验）─────────────────────────


class RecognizePhotoArgs(BaseModel):
    question: Optional[str] = Field(
        default=None,
        description="给视觉模型的具体追问，例如'包装上的保质期'、'前方红绿灯颜色'。纯拍照识别可留空。",
    )


class WebSearchArgs(BaseModel):
    query: str = Field(description="搜索关键词。")


class GetWeatherArgs(BaseModel):
    location: Optional[str] = Field(
        default=None,
        description="要查天气的地点。不填则用用户每轮附带的位置。",
    )


class GaodePoiSearchArgs(BaseModel):
    query: str = Field(description="要找的地点类型或名称，如'最近的医院'、'地铁站'、'工商银行'。")
    city: Optional[str] = Field(default=None, description="限定城市，可留空。")


class GaodeRouteArgs(BaseModel):
    destination: str = Field(description="目的地（名称或地址）。")
    mode: Literal["walking", "transit", "driving"] = Field(
        description="交通方式：walking 步行 / transit 公交 / driving 驾车。"
    )
    origin: Optional[str] = Field(
        default=None, description="出发地，不填则用用户当前位置。"
    )


class SearchKbArgs(BaseModel):
    query: str = Field(description="关于本软件功能的问题，如'怎么加入家庭'、'怎么发起求助'。")


# ───────────────────────── @tool 工具（docstring=description，含护栏）─────────────────────────


@tool(args_schema=RecognizePhotoArgs)
def recognize_photo(question: Optional[str] = None) -> str:
    """拍一张照片并交给视觉模型识别。当用户想'看看前面有什么'、'这是什么'、或对刚拍的照片追问
    细节时调用。会通过 WebSocket 触发手机拍照(信令5001)并返回识别结果。若用户只是随口问常识问题，
    不要调用本工具。可选 question：给视觉模型的具体追问（如'包装上的保质期'）。"""
    # C2a-4：调 vlm.recognize（design ① 两入口同一 VLM；2a mock 假描述、2c 真 qwen3-vl-flash）。
    # 图片来源：contextvar（service 边界 set_image_context，仿 emergency set_turn_context），
    # 2a 不灌 → None → mock；工具体取不到 state 故走 contextvar 而非 args。
    return vlm.recognize(question=question)


@tool(args_schema=WebSearchArgs)
def web_search(query: str) -> str:
    """联网搜索实时信息（新闻、价格、近期事件等互联网内容）。仅当问题需要联网才能回答时调用；
    软件功能介绍用 search_kb，天气用 get_weather，认路用 gaode_poi_search。"""
    # C2a-4：调 searchapi.search（design ⑦ 同款自建 REST；2a mock 假结果、2c 真 searchapi.io）。
    return searchapi.search(query)


@tool(args_schema=GetWeatherArgs)
def get_weather(location: Optional[str] = None) -> str:
    """查询当前天气。用户若指定了地点则查该地点，否则用用户每轮附带的位置。返回温度、天气状况、
    穿衣/出行建议。"""
    # C2a-4：调高德 weather wrapper（design ⑦ 自建 REST；2a mock、2c 真 /weather/weatherInfo）。
    # location 入参是地名/地址（用户口述），gaode.weather 据此查；position 坐标由 service 层注入
    # state，本工具体取不到 state——城市级天气用 location 文本足够（精度需求低）。
    return gaode.weather(city=location)


@tool(args_schema=GaodePoiSearchArgs)
def gaode_poi_search(query: str, city: Optional[str] = None) -> str:
    """高德 POI 搜索：找附近的医院/餐厅/地铁站/银行等地点，返回名称+地址+距离。通常配合
    gaode_route / launch_navigation 完成导航。"""
    # C2a-4：调高德 poi_search wrapper（2a mock 单候选带 500m+lat/lng；2c 真 /place/around 带距离）。
    # 注：用户每轮 position 坐标在 state，工具体取不到——周边搜的 location 由 nav skill 编排层
    # 注入（2c）；2a mock 不区分，返单候选让流程跑通。
    return gaode.poi_search(query, city=city)


@tool(args_schema=GaodeRouteArgs)
def gaode_route(
    destination: str,
    mode: Literal["walking", "transit", "driving"],
    origin: Optional[str] = None,
) -> str:
    """高德路线规划。destination 目的地必填，mode 交通方式必填（步行 walking / 公交 transit /
    驾车 driving），origin 出发地不填则用用户当前位置。返回距离+时间+转向摘要。"""
    # C2a-4：调高德 route wrapper（2a mock 800米/10分钟+样例转向；2c 真 /direction/{mode}）。
    # origin/destination 坐标解析属 nav skill 编排（先 poi_search 取目的地坐标）；工具体收文本，
    # 2a mock 不真算，2c 由编排层补全坐标后调 wrapper。
    return gaode.route(origin=None, destination=destination, mode=mode)


@tool(args_schema=SearchKbArgs)
def search_kb(query: str) -> str:
    """检索本软件的功能说明知识库（怎么加家庭、怎么导航、怎么求助等功能介绍），返回文档原文给主模型
    据以回答。用于'这个软件怎么用''XX功能在哪'类问题。不要用它查实时信息或业务数据。"""
    return kb_search(query)


NATIVE_TOOLS = [recognize_photo, web_search, get_weather, gaode_poi_search, gaode_route, search_kb]
