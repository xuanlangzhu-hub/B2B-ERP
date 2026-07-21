package com.erp.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvCountItemRequest {
    @NotNull(message = "盘点明细ID不能为空")
    private Long id;

    @NotNull(message = "实盘数量不能为空")
    @DecimalMin(value = "0", message = "实盘数量不能小于0")
    private BigDecimal actualQuantity;

    @Size(max = 500, message = "差异原因不能超过500个字符")
    private String reason;
}
