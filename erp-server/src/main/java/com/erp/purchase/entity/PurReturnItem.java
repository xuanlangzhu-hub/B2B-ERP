package com.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("pur_return_item")
public class PurReturnItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long returnId;
    private Integer lineNo;
    private Long purchaseOrderItemId;
    private Long productId;
    private String productCode;
    private String productName;
    private String specification;
    private Long unitId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal outboundQuantity;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
