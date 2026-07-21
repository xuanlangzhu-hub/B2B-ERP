package com.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
@TableName("inv_adjustment")
public class InvAdjustment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String adjustmentNo;
    private LocalDate adjustmentDate;
    private Long warehouseId;
    private String adjustmentType;
    private String sourceType;
    private Long sourceId;
    private String status;
    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String reason;
    private String remark;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long updatedBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
    @Version
    private Integer version;

    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private List<InvAdjustmentItem> items;
}
