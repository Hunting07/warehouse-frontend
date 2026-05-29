package com.teach.javafx.models;

import java.math.BigDecimal;

public class StockInItem {
    private Integer id;
    private Integer stockInId;
    private Integer materialId;
    private String materialName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private String materialCode;
    private String materialSpec;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getStockInId() { return stockInId; }
    public void setStockInId(Integer stockInId) { this.stockInId = stockInId; }

    public Integer getMaterialId() { return materialId; }
    public void setMaterialId(Integer materialId) { this.materialId = materialId; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

    public String getMaterialSpec() { return materialSpec; }
    public void setMaterialSpec(String materialSpec) { this.materialSpec = materialSpec; }
}

