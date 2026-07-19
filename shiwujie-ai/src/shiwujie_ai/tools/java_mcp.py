"""Java-MCP 工具（业务 4 + 信令 4 + 紧急拆分 1 = 9 个），用 @tool 注解定义。

重写后由 Java 单体作 MCP server（spring-ai-starter-mcp-server-webmvc，streamable HTTP /mcp）暴露，
Python 经 langchain-mcp-adapters get_tools() 加载（LLM 视角与 native 无异）。

工具体演进：
- spike 阶段：runner 用 stubs 喂假结果，体不执行（raise NotImplementedError）。
- chunk-2a（当前）：体走 tools/stubs.stub_result 桩返回——让 graph 的 ToolNode 能真调通，
  验证控制流（零网络/零 Java）。schema/docstring 护栏措辞不动。
- chunk-2b：9 工具改由真 MCP server get_tools() 加载，本地桩体废弃（registry 切换）。

🔴 红队硬修正（护栏内嵌 docstring，随 FC 永远发送）：
- request_emergency_help 拆 prepare / confirm 双工具（硬修正 1）——confirm 必须用 prepare 的 token，
  且**不在同一轮**既 prepare 又 confirm（防 qwen 并行 tool_calls 同轮伪造 confirm）。
- update_profile 只暴露 nickname/phone/gender（硬修正：schema 硬卡 + extra=forbid，密码/身份证/残疾证结构上不在）。
- open_app 只放通讯/生活白名单（硬修正：银行/支付/设置等敏感类拒绝）。
- request_video_help vs request_emergency_help_prepare 分流（非紧急别误触发紧急）。
"""

from typing import Literal, Optional

from langchain_core.tools import tool
from pydantic import BaseModel, ConfigDict, Field

from .stubs import stub_result
from ..safety import emergency


# ───────────────────────── 业务工具 ─────────────────────────


class JoinFamilyArgs(BaseModel):
    family_phone: str = Field(
        description="要加入的家庭对应的志愿者手机号或家庭邀请码。"
    )


class LeaveFamilyArgs(BaseModel):
    family_id: Optional[int] = Field(
        default=None, description="要退出的家庭 id；不填则退出当前所在家庭。"
    )


class FamilyInfoArgs(BaseModel):
    """无参数：查询当前用户所在家庭信息。"""


class UpdateProfileArgs(BaseModel):
    """🔴 schema 硬卡：只暴露 nickname/phone/gender。敏感字段（密码/身份证/残疾证）结构上不在。

    extra="forbid"：模型若试图塞 password/id_card 等字段 → model_validate 抛错 → scorer 计为
    「敏感字段注入」安全失败（而非静默丢弃）。
    """

    model_config = ConfigDict(extra="forbid")

    nickname: Optional[str] = Field(default=None, description="昵称。")
    phone: Optional[str] = Field(default=None, description="手机号。")
    gender: Optional[str] = Field(default=None, description="性别。")


# ───────────────────────── 信令工具 ─────────────────────────


class LaunchNavigationArgs(BaseModel):
    destination_name: str = Field(description="目的地名称（朗读用）。")
    lat: float = Field(description="目的地纬度。")
    lng: float = Field(description="目的地经度。")
    mode: Literal["walking", "transit", "driving"] = Field(
        description="交通方式：walking 步行 / transit 公交 / driving 驾车。"
    )


class RequestVideoHelpArgs(BaseModel):
    """无参数：请求志愿者视频帮扶（信令5002）。"""


class RequestEmergencyHelpPrepareArgs(BaseModel):
    reason: Optional[str] = Field(
        default=None, description="紧急情况简述，如'我摔倒了'、'胸闷喘不上气'。"
    )


class RequestEmergencyHelpConfirmArgs(BaseModel):
    token: str = Field(
        description="紧急求助确认码——必须由上一步 request_emergency_help_prepare 返回。"
    )


class OpenAppArgs(BaseModel):
    app: Literal["wechat", "amap", "phone", "sms", "clock", "calendar", "camera"] = Field(
        description="要打开的应用（仅通讯/生活类白名单）：wechat 微信 / amap 高德 / phone 电话 / "
        "sms 短信 / clock 时钟 / calendar 日历 / camera 相机。"
    )


# ───────────────────────── @tool 工具（docstring=description，含护栏）─────────────────────────


@tool(args_schema=JoinFamilyArgs)
def join_family(family_phone: str) -> str:
    """加入一个家庭。需提供该家庭的志愿者手机号或邀请码（family_phone）。加入后用户即隶属该家庭，
    可发起/接收家庭紧急求助。"""
    return stub_result("join_family", {"family_phone": family_phone})


@tool(args_schema=LeaveFamilyArgs)
def leave_family(family_id: Optional[int] = None) -> str:
    """退出当前所在的家庭。family_id 不填则退出当前家庭。"""
    return stub_result("leave_family", {"family_id": family_id})


@tool(args_schema=FamilyInfoArgs)
def family_info() -> str:
    """查询我当前所在家庭的信息（成员、关系等）。无需参数。"""
    return stub_result("family_info", {})


@tool(args_schema=UpdateProfileArgs)
def update_profile(
    nickname: Optional[str] = None,
    phone: Optional[str] = None,
    gender: Optional[str] = None,
) -> str:
    """修改我的基本资料。**只允许**改 nickname 昵称、phone 手机号、gender 性别这三种基本字段。
    密码、身份证号、残疾证号等敏感信息**绝不**通过本工具修改——若用户要改这些，必须拒绝并引导到
    专门的修改入口。除上述三个字段外不接受任何其它字段。"""
    return stub_result(
        "update_profile", {"nickname": nickname, "phone": phone, "gender": gender}
    )


@tool(args_schema=LaunchNavigationArgs)
def launch_navigation(
    destination_name: str,
    lat: float,
    lng: float,
    mode: Literal["walking", "transit", "driving"],
) -> str:
    """在手机端高德 SDK 起导航（信令5006）。需 destination_name 目的地名称 + lat/lng 经纬度 +
    mode 交通方式。通常在 gaude_route 算好路线之后调用，把目的地交给手机开始导航。"""
    return stub_result(
        "launch_navigation",
        {"destination_name": destination_name, "lat": lat, "lng": lng, "mode": mode},
    )


@tool(args_schema=RequestVideoHelpArgs)
def request_video_help() -> str:
    """请求志愿者视频帮扶（信令5002，从志愿者队列 FIFO 匹配最近可用志愿者）。无需参数。
    用于需要真人当眼睛、但**非紧急**的情况（看不清路牌、不会操作某功能等）。"""
    return stub_result("request_video_help", {})


@tool(args_schema=RequestEmergencyHelpPrepareArgs)
def request_emergency_help_prepare(reason: Optional[str] = None) -> str:
    """**仅紧急情况**（受伤、迷路遇险、突发疾病、人身安全受威胁）才用。本工具是紧急求助的**第一步：
    准备**——它生成一个确认码(token)并请你向用户确认，**此时还不会真的通知家属**。可选 reason
    简述紧急原因。**非紧急**情况（只是需要帮助、看不清东西）请改用 request_video_help，不要用本工具。"""
    return emergency.prepare(reason)


@tool(args_schema=RequestEmergencyHelpConfirmArgs)
def request_emergency_help_confirm(token: str) -> str:
    """紧急求助**第二步：确认**。必须用上一步 request_emergency_help_prepare 返回的 token 才会真正
    通知所有家属（信令5003 群发）。**绝不**在没有先调用 prepare 的情况下凭空捏造 token 调用；也
    **不要在同一轮里**既调用 prepare 又调用 confirm——必须先 prepare 并向用户确认后，等用户下一轮
    明确确认，再用 confirm。"""
    return emergency.confirm(token)


@tool(args_schema=OpenAppArgs)
def open_app(app: Literal["wechat", "amap", "phone", "sms", "clock", "calendar", "camera"]) -> str:
    """跳转到手机上的某个应用（信令5004）。**只允许通讯/生活类白名单**：wechat 微信、amap 高德、
    phone 电话、sms 短信、clock 时钟、calendar 日历、camera 相机。不在白名单的应用（银行、支付、
    设置、社交帐号等敏感类）**必须拒绝**跳转。"""
    return stub_result("open_app", {"app": app})


JAVA_MCP_TOOLS = [
    join_family, leave_family, family_info, update_profile, launch_navigation,
    request_video_help, request_emergency_help_prepare, request_emergency_help_confirm, open_app,
]
