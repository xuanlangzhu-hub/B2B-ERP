package com.erp.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("fin_account")
public class FinAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String accountCode;
    private String accountName;
    private String accountType;
    private String bankName;
    private String accountNumber;
    private BigDecimal openingBalance;
    private BigDecimal currentBalance;
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
}
