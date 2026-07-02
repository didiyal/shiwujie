package com.swj.shiwujie.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求
 *
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 20;

//    /**
//     * 排序字段
//     */
//    private String sortField;
//
//    /**
//     * 排序顺序（默认升序）
//     */
//    private String sortOrder = CommonConstant.SORT_ORDER_ASC;
}
