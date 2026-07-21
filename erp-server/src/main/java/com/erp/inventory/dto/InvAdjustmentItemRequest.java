package com.erp.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvAdjustmentItemRequest {
    @NotNull(message = "调整商品不能为空")
    private Long productId;

    @NotNull(message = "调整数量不能为空")
    @DecimalMin(value = "0.0001", message = "调整数量必须大于0")
    private BigDecimal quantity;

    @DecimalMin(value = "0", message = "单位成本不能小于0")
    private BigDecimal unitCost;

    @Size(max = 500, message = "明细原因不能超过500个字符")
    private String reason;
}
