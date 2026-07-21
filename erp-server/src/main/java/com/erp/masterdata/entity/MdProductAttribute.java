package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
@TableName("md_product_attribute")
public class MdProductAttribute {
    @TableId(type = IdType.AUTO) private Long id;
    private Long enterpriseId;
    private String attributeCode;
    private String attributeName;
    private String inputType;
    private String status;
    private Integer sortNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    @TableField(exist = false) private List<MdProductAttributeValue> values;
}
