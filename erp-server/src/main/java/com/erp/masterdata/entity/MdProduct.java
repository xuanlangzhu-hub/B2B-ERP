package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("md_product")
public class MdProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String productCode;
    private String productName;
    private String barcode;
    private Long categoryId;
    private Long unitId;
    private String brand;
    private String specification;
    private String modelNo;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private BigDecimal costPrice;
    private BigDecimal minStock;
    private BigDecimal maxStock;
    private String imageUrl;
    private String status;
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
    private String categoryName;
    @TableField(exist = false)
    private String unitName;
}
