package com.erp.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("fin_other_transaction")
public class FinOtherTransaction {
    @TableId(type = IdType.AUTO) private Long id;
    private Long enterpriseId;
    private Long storeId;
    private String transactionNo;
    private LocalDate transactionDate;
    private String transactionType;
    private String category;
    private Long accountId;
    private BigDecimal amount;
    private String counterparty;
    private String status;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private String remark;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    private Long updatedBy;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    @Version private Integer version;
    @TableField(exist = false) private String accountName;
}
