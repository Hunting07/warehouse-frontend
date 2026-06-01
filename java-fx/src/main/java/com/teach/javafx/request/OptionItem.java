package com.teach.javafx.request;

import java.math.BigDecimal;
import java.util.Map;

/**
 * OptionItem 选项数据类（仓储系统通用）
 * Integer id  数据项id
 * String name 数据项名称（如下拉框显示的物资名称）
 */
public class OptionItem {
    private Integer id;
    private String name;
    private BigDecimal price;

    public OptionItem() {

    }

    public OptionItem(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public OptionItem(Map<String, Object> map) {
        if (map != null) {
            Object idObj = map.get("id");
            this.id = idObj != null ? ((Number) idObj).intValue() : null;
            this.name = (String) map.get("name");
            
            // 尝试获取价格字段
            Object priceObj = map.get("price");
            if (priceObj == null) {
                priceObj = map.get("unitPrice");
            }
            if (priceObj != null) {
                this.price = new BigDecimal(priceObj.toString());
            }
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return name;
    }
}
