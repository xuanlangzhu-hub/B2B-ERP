package com.erp.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinAccountRequest {
    @NotBlank(message = "账户编码不能为空")
    private String accountCode;
    @NotBlank(message = "账户名称不能为空")
    private String accountName;
    @NotBlank(message = "账户类型不能为空")
    private String accountType;
    private String bankName;
    private String accountNumber;
    @NotNull(message = "期初余额不能为空")
    private BigDecimal openingBalance;
    private String status;
    private String remark;
}
