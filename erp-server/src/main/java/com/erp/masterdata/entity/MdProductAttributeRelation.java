package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("md_product_attribute_relation")
public class MdProductAttributeRelation {
    @TableId(type = IdType.AUTO) private Long id;
    private Long productId;
    private Long attributeId;
    private Long attributeValueId;
    private String customValue;
    private LocalDateTime createdAt;
}
