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
@TableName("inv_count")
public class InvCount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String countNo;
    private LocalDate countDate;
    private Long warehouseId;
    private String status;
    private BigDecimal totalBookQuantity;
    private BigDecimal totalActualQuantity;
    private BigDecimal totalDiffQuantity;
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
    private List<InvCountItem> items;
}
