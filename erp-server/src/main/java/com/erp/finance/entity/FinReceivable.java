package com.erp.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("fin_receivable")
public class FinReceivable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private Long storeId;
    private String receivableNo;
    private Long customerId;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private LocalDate businessDate;
    private LocalDate dueDate;
    private BigDecimal originalAmount;
    private BigDecimal receivedAmount;
    private BigDecimal writeOffAmount;
    private BigDecimal outstandingAmount;
    private String status;
    private String remark;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long updatedBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
    @Version
    private Integer version;
    @TableField(exist = false)
    private String customerName;
}
