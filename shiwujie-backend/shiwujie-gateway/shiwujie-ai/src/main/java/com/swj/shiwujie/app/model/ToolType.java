package com.swj.shiwujie.app.model;


import lombok.Getter;

/**
 * 工具类型枚举
 */
@Getter
public enum ToolType {
    RETURN(0, "直接返回"),
    NETWORK_SEARCH(1,  "联网搜索"),
    TOOL_CALL(2,  "业务工具调用");



    private int index;
    private String name;

    ToolType(int index, String name) {
        this.index = index;
        this.name = name;
    }

}
