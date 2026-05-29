package com.teach.javafx.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockIn {
    private Integer id;
    private String inCode;
    private Integer type;
    private String typeName;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusName;
    private Integer applyUserId;
    private String applyUserName;
    private Integer approveUserId;
    private String approveUserName;
    private String rejectReason;
    private LocalDateTime createTime;
    private LocalDateTime approveTime;
    private LocalDateTime completeTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getInCode() { return inCode; }
    public void setInCode(String inCode) { this.inCode = inCode; }

    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public Integer getApplyUserId() { return applyUserId; }
    public void setApplyUserId(Integer applyUserId) { this.applyUserId = applyUserId; }

    public String getApplyUserName() { return applyUserName; }
    public void setApplyUserName(String applyUserName) { this.applyUserName = applyUserName; }

    public Integer getApproveUserId() { return approveUserId; }
    public void setApproveUserId(Integer approveUserId) { this.approveUserId = approveUserId; }

    public String getApproveUserName() { return approveUserName; }
    public void setApproveUserName(String approveUserName) { this.approveUserName = approveUserName; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }

    public LocalDateTime getCompleteTime() { return completeTime; }
    public void setCompleteTime(LocalDateTime completeTime) { this.completeTime = completeTime; }
}

