package com.swj.shiwujie.app.model;


import lombok.Getter;

/**
 * 工具索引
 */
@Getter
public enum ToolIndex {

    APPLY_JOIN_HOME(1, "申请加入家庭"),
    CHECK_HOME_INFO(2, "查看家庭信息"),
    QUIT_HOME(3, "退出家庭"),
    POTATO_RECOGNITION(4, "拍照识别"),
    VIDEO_HELP(5, "志愿者视频求助"),
    EMERGENCY_HELP(6, "家属紧急求助"),
    JUMP_SOFTWARE(7, "跳转软件"),
    NAVIGATION(8, "导航"),
    CHANGE_TONE(9, "切换语气");



    private int index;
    private String name;
    ToolIndex(int index, String name) {
        this.index = index;
        this.name = name;
    }
}
