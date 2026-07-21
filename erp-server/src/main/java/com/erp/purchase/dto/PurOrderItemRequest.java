package com.erp.purchase.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurOrderItemRequest {
    @NotNull(message = "行号不能为空")
    private Integer lineNo;

    @NotNull(message = "商品不能为空")
    private Long productId;

    private String productCode;

    private String productName;

    private String specification;

    private Long unitId;

    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.0001", message = "数量必须大于0")
    private BigDecimal quantity;

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0", message = "单价不能小于0")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0", message = "折扣率不能小于0")
    @DecimalMax(value = "100", message = "折扣率不能大于100")
    private BigDecimal discountRate;

    @DecimalMin(value = "0", message = "税率不能小于0")
    @DecimalMax(value = "100", message = "税率不能大于100")
    private BigDecimal taxRate;

    private String remark;
}
