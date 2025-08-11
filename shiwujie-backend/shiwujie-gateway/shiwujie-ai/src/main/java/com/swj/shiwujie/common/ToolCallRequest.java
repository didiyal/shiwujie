package com.swj.shiwujie.common;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * 工具调用请求
 */
public class ToolCallRequest {

    private Integer type;


    private String data;
}
