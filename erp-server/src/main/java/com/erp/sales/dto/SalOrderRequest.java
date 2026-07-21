package com.erp.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SalOrderRequest {
    @NotNull(message = "门店不能为空")
    private Long storeId;

    @NotNull(message = "单据日期不能为空")
    private LocalDate orderDate;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    private Long salespersonId;

    private BigDecimal freightAmount;

    private String deliveryAddress;

    private LocalDate expectedDeliveryDate;

    private String remark;

    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<SalOrderItemRequest> items;
}
