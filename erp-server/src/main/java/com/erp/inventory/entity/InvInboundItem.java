package com.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("inv_inbound_item")
public class InvInboundItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inboundId;
    private Integer lineNo;
    private Long sourceItemId;
    private Long productId;
    private String productCode;
    private String productName;
    private String specification;
    private Long unitId;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal amount;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
