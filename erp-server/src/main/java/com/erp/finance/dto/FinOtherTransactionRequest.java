package com.erp.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinOtherTransactionRequest {
    private Long storeId;
    @NotNull(message = "业务日期不能为空") private LocalDate transactionDate;
    @NotBlank(message = "收支类型不能为空") private String transactionType;
    @NotBlank(message = "收支类别不能为空") private String category;
    @NotNull(message = "资金账户不能为空") private Long accountId;
    @NotNull(message = "金额不能为空") @DecimalMin(value = "0.01", message = "金额必须大于0") private BigDecimal amount;
    private String counterparty;
    private String remark;
}
