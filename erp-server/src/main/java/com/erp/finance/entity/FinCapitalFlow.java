package com.erp.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("fin_capital_flow")
public class FinCapitalFlow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private Long storeId;
    private Long accountId;
    private String flowNo;
    private LocalDate flowDate;
    private String flowType;
    private String direction;
    private BigDecimal amount;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private String counterpartyType;
    private Long counterpartyId;
    private String counterpartyName;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
