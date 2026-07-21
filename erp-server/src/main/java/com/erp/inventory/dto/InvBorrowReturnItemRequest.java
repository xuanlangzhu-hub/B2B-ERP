package com.erp.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvBorrowReturnItemRequest {
    @NotNull(message = "借用明细不能为空")
    private Long itemId;

    @NotNull(message = "归还数量不能为空")
    @DecimalMin(value = "0.0001", message = "归还数量必须大于0")
    private BigDecimal quantity;
}
