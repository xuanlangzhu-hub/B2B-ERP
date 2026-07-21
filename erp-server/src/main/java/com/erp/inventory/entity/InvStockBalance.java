package com.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("inv_stock_balance")
public class InvStockBalance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private Long warehouseId;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal lockedQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal avgCostPrice;
    private BigDecimal stockAmount;
    private LocalDateTime lastMovementAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @Version
    private Integer version;

    @TableField(exist = false)
    private String productCode;
    @TableField(exist = false)
    private String productName;
    @TableField(exist = false)
    private String specification;
    @TableField(exist = false)
    private Long unitId;
    @TableField(exist = false)
    private String unitName;
    @TableField(exist = false)
    private BigDecimal minStock;
    @TableField(exist = false)
    private Boolean lowStock;
    @TableField(exist = false)
    private String warehouseName;
}
