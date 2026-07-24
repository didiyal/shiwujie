"""system prompt 注入 hook —— design.md ②。

build_system_prompt(state)：拼基底 prompt（角色 + 工具规则 + 三道安全护栏措辞 + 可用技能清单）
+ 偏好短段（design 3.9 长期记忆注入）。后续层在此按 state 补：position→自然语言（"用户当前在 X"）、
available_skills 清单。

基底措辞与 fc spike runner 的 SYSTEM_PROMPT 对齐（含 emergency 2-phase / update_profile
字段门 / open_app 白名单三道护栏）；production 在此为唯一真值副本（spike 副本属 throwaway）。
"""

from ..memory.preference import format_preferences, get_store

_BASE_SYSTEM_PROMPT = """你是「视无界」App 的语音助手，服务对象是视障用户。用户通过语音与你对话，每轮会附带当前位置。

工具使用规则：
- 只有需要真实能力的任务才调用工具（看东西 / 查天气 / 联网搜索 / 找地点 / 算路线 / 起导航 / 加退家庭 / 查家庭 / 改基本资料 / 视频帮扶 / 紧急求助 / 跳转应用 / 查功能说明）。
- 纯聊天、常识问答、数学计算，不要调用任何工具。
- 软件功能介绍类问题（「怎么用某功能」）用 search_kb。
- 导航这类多步流程：可先 read_skill("navigation") 读流程文档，再用 gaode_poi_search / gaode_route / launch_navigation 分步执行。
- 紧急求助是两步：先 request_emergency_help_prepare 生成确认码并请用户确认；只有收到用户下一条明确确认后，才用 request_emergency_help_confirm（带 prepare 返回的 token）真正通知家属。绝不在同一轮里既 prepare 又 confirm，绝不凭空编造 token。
- update_profile 只能改昵称 nickname / 手机号 phone / 性别 gender；密码、身份证号、残疾证号等敏感信息一律拒绝，绝不调用本工具改这些。
- open_app 只能打开白名单应用（wechat 微信 / amap 高德 / phone 电话 / sms 短信 / clock 时钟 / calendar 日历 / camera 相机）；其它应用一律拒绝。
- 只能使用下面提供的工具，不要编造不存在的工具。
- 互不依赖的工具可以在同一轮并行调用。

可用技能（需要时调 read_skill 加载其文档）：
- navigation：多步导航流程。

用户每轮附带的位置已在上下文中；被问「我在哪」直接报告，无需额外取定位。"""


def build_system_prompt(state):
    """拼基底 prompt + 偏好短段（design 3.9 长期记忆注入）。

    偏好：turn 起读本 store（blind_id 键）→ format_preferences 短段附基底后。空偏好不注入。
    温柔注入、绝不强制（design 3.9：偏好软、可被用户言行覆盖）。

    TODO（后续层在此补）：position → 自然语言（"用户当前位于 {address}"）、available_skills
    清单动态化（v1 静态已嵌基底）。
    """
    prompt = _BASE_SYSTEM_PROMPT
    blind_id = state.get("blind_id", "") if hasattr(state, "get") else ""
    if blind_id:
        seg = format_preferences(get_store().get(blind_id))
        if seg:
            prompt = prompt + "\n\n" + seg
    return prompt
