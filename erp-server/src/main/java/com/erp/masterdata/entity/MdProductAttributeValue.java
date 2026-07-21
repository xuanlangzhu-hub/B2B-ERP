package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("md_product_attribute_value")
public class MdProductAttributeValue {
    @TableId(type = IdType.AUTO) private Long id;
    private Long attributeId;
    private String valueCode;
    private String valueName;
    private Integer sortNo;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}
