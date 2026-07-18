"""Python-native 工具（6 个能力），用 @tool 注解定义。

spike 阶段函数体不执行（runner 用 stubs 喂假结果）；chunk-2 生产时填真实实现：
recognize_photo 触发 WS 拍照 + VLM、web_search 调 searchapi.io、get_weather 调天气 API、
gaode_* 调高德 SDK、search_kb 查 BM25 索引。

@tool(args_schema=...) 复用下方 Pydantic Args 模型：name=函数名、description=docstring、
args_schema=Pydantic 模型（scorer 经 ALL_ARG_MODELS 取 model_validate 校验）。
"""

from typing import Literal, Optional

from langchain_core.tools import tool
from pydantic import BaseModel, Field


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
    raise NotImplementedError("spike: runner 用 stubs；chunk-2 填 WS 拍照 + VLM 识别")


@tool(args_schema=WebSearchArgs)
def web_search(query: str) -> str:
    """联网搜索实时信息（新闻、价格、近期事件等互联网内容）。仅当问题需要联网才能回答时调用；
    软件功能介绍用 search_kb，天气用 get_weather，认路用 gaode_poi_search。"""
    raise NotImplementedError("spike: runner 用 stubs；chunk-2 填 searchapi.io 调用")


@tool(args_schema=GetWeatherArgs)
def get_weather(location: Optional[str] = None) -> str:
    """查询当前天气。用户若指定了地点则查该地点，否则用用户每轮附带的位置。返回温度、天气状况、
    穿衣/出行建议。"""
    raise NotImplementedError("spike: runner 用 stubs；chunk-2 填天气 API")


@tool(args_schema=GaodePoiSearchArgs)
def gaode_poi_search(query: str, city: Optional[str] = None) -> str:
    """高德 POI 搜索：找附近的医院/餐厅/地铁站/银行等地点，返回名称+地址+距离。通常配合
    gaode_route / launch_navigation 完成导航。"""
    raise NotImplementedError("spike: runner 用 stubs；chunk-2 填高德 POI API")


@tool(args_schema=GaodeRouteArgs)
def gaode_route(
    destination: str,
    mode: Literal["walking", "transit", "driving"],
    origin: Optional[str] = None,
) -> str:
    """高德路线规划。destination 目的地必填，mode 交通方式必填（步行 walking / 公交 transit /
    驾车 driving），origin 出发地不填则用用户当前位置。返回距离+时间+转向摘要。"""
    raise NotImplementedError("spike: runner 用 stubs；chunk-2 填高德路线 API")


@tool(args_schema=SearchKbArgs)
def search_kb(query: str) -> str:
    """检索本软件的功能说明知识库（怎么加家庭、怎么导航、怎么求助等功能介绍），返回文档原文给主模型
    据以回答。用于'这个软件怎么用''XX功能在哪'类问题。不要用它查实时信息或业务数据。"""
    raise NotImplementedError("spike: runner 用 stubs；chunk-2 填 BM25 检索")


NATIVE_TOOLS = [recognize_photo, web_search, get_weather, gaode_poi_search, gaode_route, search_kb]
