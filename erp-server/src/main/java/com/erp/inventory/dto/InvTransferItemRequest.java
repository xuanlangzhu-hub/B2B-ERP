package com.erp.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvTransferItemRequest {
    @NotNull(message = "调拨商品不能为空")
    private Long productId;

    @NotNull(message = "调拨数量不能为空")
    @DecimalMin(value = "0.0001", message = "调拨数量必须大于0")
    private BigDecimal quantity;

    @Size(max = 500, message = "明细备注不能超过500个字符")
    private String remark;
}
