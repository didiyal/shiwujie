package com.swj.shiwujie.data.model;

/**
 * 排序项数据模型
 */
public class OrderItem {
    private Boolean asc;
    private String column;

    public OrderItem() {
    }

    public OrderItem(Boolean asc, String column) {
        this.asc = asc;
        this.column = column;
    }

    public Boolean getAsc() {
        return asc;
    }

    public void setAsc(Boolean asc) {
        this.asc = asc;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }
} 