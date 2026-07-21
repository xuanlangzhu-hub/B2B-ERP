package com.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("inv_stock_movement")
public class InvStockMovement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private Long storeId;
    private Long warehouseId;
    private Long productId;
    private String movementNo;
    private String movementType;
    private String direction;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal amount;
    private BigDecimal beforeQuantity;
    private BigDecimal afterQuantity;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long sourceItemId;
    private LocalDate businessDate;
    private Long operatorId;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String productCode;
    @TableField(exist = false)
    private String productName;
    @TableField(exist = false)
    private String warehouseName;
}
