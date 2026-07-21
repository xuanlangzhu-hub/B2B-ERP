package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
@TableName("md_customer")
public class MdCustomer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String customerCode;
    private String customerName;
    private Long categoryId;
    private Long levelId;
    private String contactName;
    private String contactPhone;
    private String email;
    private String address;
    private String taxNo;
    private String bankName;
    private String bankAccount;
    private BigDecimal creditLimit;
    private Integer paymentDays;
    private Long salespersonId;
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
    private String levelName;
    @TableField(exist = false)
    private List<Long> tagIds;
    @TableField(exist = false)
    private List<String> tagNames;
}
