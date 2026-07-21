package com.erp.inventory.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
@TableName("inv_transfer")
public class InvTransfer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String transferNo;
    private LocalDate transferDate;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private String status;
    private BigDecimal totalQuantity;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime completedAt;
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
    private String fromWarehouseName;
    @TableField(exist = false)
    private String toWarehouseName;
    @TableField(exist = false)
    private List<InvTransferItem> items;
}
