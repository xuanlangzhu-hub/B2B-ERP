package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("md_supplier")
public class MdSupplier {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String supplierCode;
    private String supplierName;
    private Long categoryId;
    private String contactName;
    private String contactPhone;
    private String email;
    private String address;
    private String taxNo;
    private String bankName;
    private String bankAccount;
    private Integer paymentDays;
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
}
