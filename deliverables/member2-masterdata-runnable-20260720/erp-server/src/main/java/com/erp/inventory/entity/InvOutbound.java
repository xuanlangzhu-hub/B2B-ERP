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
@TableName("inv_outbound")
public class InvOutbound {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private Long storeId;
    private String outboundNo;
    private String outboundType;
    private LocalDate outboundDate;
    private Long warehouseId;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long supplierId;
    private Long customerId;
    private String status;
    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
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
    private List<InvOutboundItem> items;
    @TableField(exist = false)
    private String warehouseName;
}
