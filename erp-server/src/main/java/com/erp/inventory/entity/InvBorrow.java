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
@TableName("inv_borrow")
public class InvBorrow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String borrowNo;
    private String borrowType;
    private LocalDate borrowDate;
    private LocalDate expectedReturnDate;
    private Long warehouseId;
    private String partnerType;
    private Long partnerId;
    private String partnerName;
    private String status;
    private BigDecimal totalQuantity;
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
    private String warehouseName;
    @TableField(exist = false)
    private Boolean overdue;
    @TableField(exist = false)
    private BigDecimal returnedQuantity;
    @TableField(exist = false)
    private BigDecimal remainingQuantity;
    @TableField(exist = false)
    private List<InvBorrowItem> items;
}
