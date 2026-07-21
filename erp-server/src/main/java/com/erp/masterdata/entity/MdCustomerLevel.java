package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("md_customer_level")
public class MdCustomerLevel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String levelCode;
    private String levelName;
    private BigDecimal discountRate;
    private BigDecimal creditLimit;
    private Integer sortNo;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
