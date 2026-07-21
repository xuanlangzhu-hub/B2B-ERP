package com.erp.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("fin_receipt")
public class FinReceipt {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private Long storeId;
    private String receiptNo;
    private LocalDate receiptDate;
    private Long customerId;
    private Long accountId;
    private String paymentMethod;
    private BigDecimal receiptAmount;
    private BigDecimal allocatedAmount;
    private BigDecimal unallocatedAmount;
    private String status;
    private String referenceNo;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
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
    @TableField(exist = false)
    private String accountName;
}
