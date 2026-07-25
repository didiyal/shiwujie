package com.swj.shiwujie.app.model;


import lombok.Data;


/**
 * 工具选择返回结果
 */
@Data
public class ToolChoiceRes {
    /**
     * 选择类型：
     * 1 - 直接回复
     * 2 - 调用工具
     */
    private int toolType;


    /**
     * 工具索引(调用工具时)
     */
    private int toolIndex;


    /**
     * 工具输入参数(调用工具时)
     */
    private String toolInput;

}
