package com.erp.purchase.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurReturnItemRequest {
    @NotNull(message = "采购单明细不能为空")
    private Long purchaseOrderItemId;
    @NotNull(message = "退货数量不能为空")
    @DecimalMin(value = "0.0001", message = "退货数量必须大于0")
    private BigDecimal quantity;
    private String remark;
}
