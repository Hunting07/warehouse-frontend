package com.teach.javafx.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockIn {
    private Integer id;
    private String inCode;
    private String type;
    private BigDecimal totalAmount;
    private Integer status;
    private Integer applyUserId;
    private Integer approveUserId;
    private LocalDateTime createTime;
    private LocalDateTime approveTime;
    private LocalDateTime completeTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getInCode() { return inCode; }
    public void setInCode(String inCode) { this.inCode = inCode; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getApplyUserId() { return applyUserId; }
    public void setApplyUserId(Integer applyUserId) { this.applyUserId = applyUserId; }

    public Integer getApproveUserId() { return approveUserId; }
    public void setApproveUserId(Integer approveUserId) { this.approveUserId = approveUserId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }

    public LocalDateTime getCompleteTime() { return completeTime; }
    public void setCompleteTime(LocalDateTime completeTime) { this.completeTime = completeTime; }
}

