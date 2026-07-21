package com.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("inv_adjustment_item")
public class InvAdjustmentItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adjustmentId;
    private Integer lineNo;
    private Long productId;
    private String productCode;
    private String productName;
    private Long unitId;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal amount;
    private String reason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String unitName;
}
