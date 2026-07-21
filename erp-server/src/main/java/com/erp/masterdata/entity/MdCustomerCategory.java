package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("md_customer_category")
public class MdCustomerCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String categoryCode;
    private String categoryName;
    private Integer sortNo;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
