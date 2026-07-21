package com.erp.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
@TableName("sal_return")
public class SalReturn {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private Long storeId;
    private String returnNo;
    private LocalDate returnDate;
    private Long salesOrderId;
    private Long customerId;
    private Long warehouseId;
    private String status;
    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private BigDecimal refundAmount;
    private String returnReason;
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
    private List<SalReturnItem> items;
    @TableField(exist = false)
    private String salesOrderNo;
    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String warehouseName;
}
