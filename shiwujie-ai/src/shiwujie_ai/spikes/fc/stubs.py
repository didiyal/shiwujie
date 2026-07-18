"""spike 工具桩：multi_turn 轮间喂回的假工具结果（spike 不真执行工具，只测 FC 选择/参数）。

桩内容刻意「够用」——让多轮流程能继续（poi 桩带 lat/lng 供 launch；emergency prepare 桩带 token 供 confirm）。
"""

#: navigation 技能文档正文（read_skill("navigation") 的返回）。
NAV_SKILL_BODY = """navigation 技能：多步导航流程。
1. gaode_poi_search(query) 搜索候选地点。
2. 若多候选，朗读 top2-3 名称+距离，问用户「去哪个」。
3. 问用户交通方式：步行 walking / 公交 transit / 驾车 driving。
4. gaode_route(destination, mode) 算路线。
5. 朗读距离/时间/转向摘要。
6. launch_navigation(destination_name, lat, lng, mode) 在手机端起导航。
取消则不 launch；目的地模糊则先搜再确认。"""

def stub_result(name: str, args: dict | None) -> str:
    """返回工具 name 的桩结果（多轮续接用）。未知工具给通用 ok。

    poi_search 桩返回**单一明确目的地**（以 query 命名）——nav skill 步骤 2「若多候选才问去哪个」，
    单候选即跳过确认、直接进算路线；若给模糊多候选，模型会按 skill 纪律卡在「问去哪个」，
    multi_turn case（turn2 仅给交通方式即期望 route）便无法继续。
    """
    args = args or {}
    if name == "read_skill":
        return NAV_SKILL_BODY
    if name == "gaode_poi_search":
        query = args.get("query") or "目的地"
        return (
            f'[{{"name":"{query}","address":"示例地址","distance":"500米",'
            f'"lat":30.5728,"lng":104.0668}}]'
        )
    if name == "gaode_route":
        return '{"distance":"800米","duration":"10分钟","summary":"沿示例路向东约800米"}'
    if name == "launch_navigation":
        return '{"status":"ok","message":"导航已开始"}'
    if name == "get_weather":
        return '{"weather":"晴","temperature":"22度","suggestion":"适合外出，无需带伞"}'
    if name == "web_search":
        return '[{"title":"示例搜索结果","snippet":"这是搜索摘要示例文本。"}]'
    if name == "recognize_photo":
        return '{"description":"前方是一辆共享单车，左侧有盲道延伸。"}'
    if name == "family_info":
        return '{"family":"示例家庭","members":[{"name":"张三","role":"家属"}]}'
    if name == "search_kb":
        return "软件功能说明示例文档：该项功能的操作步骤一、步骤二……"
    if name == "join_family":
        return '{"status":"ok","message":"已申请加入家庭"}'
    if name == "leave_family":
        return '{"status":"ok","message":"已退出家庭"}'
    if name == "update_profile":
        return '{"status":"ok","message":"资料已更新"}'
    if name == "request_video_help":
        return '{"status":"ok","message":"正在为您匹配志愿者"}'
    if name == "request_emergency_help_prepare":
        return (
            '{"token":"EMERG-7K2","message":"确认码已生成。请向用户确认后，'
            '用本 token 调用 request_emergency_help_confirm。"}'
        )
    if name == "request_emergency_help_confirm":
        return '{"status":"ok","message":"已通知所有家属"}'
    if name == "open_app":
        return '{"status":"ok","message":"已打开应用"}'
    return '{"status":"ok"}'
