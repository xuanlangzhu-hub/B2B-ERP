package com.erp.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinPaymentRequest {
    @NotNull(message = "门店不能为空")
    private Long storeId;

    @NotNull(message = "付款日期不能为空")
    private LocalDate paymentDate;

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @NotNull(message = "付款账户不能为空")
    private Long accountId;

    @NotNull(message = "付款方式不能为空")
    private String paymentMethod;

    @NotNull(message = "付款金额不能为空")
    private BigDecimal paymentAmount;

    private String referenceNo;

    private String remark;
}
