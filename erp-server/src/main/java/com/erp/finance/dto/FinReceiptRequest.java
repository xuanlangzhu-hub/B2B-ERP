package com.erp.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinReceiptRequest {
    @NotNull(message = "门店不能为空")
    private Long storeId;

    @NotNull(message = "收款日期不能为空")
    private LocalDate receiptDate;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "收款账户不能为空")
    private Long accountId;

    @NotNull(message = "收款方式不能为空")
    private String paymentMethod;

    @NotNull(message = "收款金额不能为空")
    private BigDecimal receiptAmount;

    private String referenceNo;

    private String remark;
}
