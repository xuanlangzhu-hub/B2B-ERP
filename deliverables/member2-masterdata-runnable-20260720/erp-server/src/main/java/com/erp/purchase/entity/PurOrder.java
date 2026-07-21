package com.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
@TableName("pur_order")
public class PurOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private Long storeId;
    private String orderNo;
    private LocalDate orderDate;
    private Long supplierId;
    private Long warehouseId;
    private Long purchaserId;
    private String status;
    private String settlementStatus;
    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal freightAmount;
    private BigDecimal payableAmount;
    private BigDecimal paidAmount;
    private LocalDate expectedArrivalDate;
    private Long approvedBy;
    private LocalDateTime approvedAt;
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
    private List<PurOrderItem> items;
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String warehouseName;
}
