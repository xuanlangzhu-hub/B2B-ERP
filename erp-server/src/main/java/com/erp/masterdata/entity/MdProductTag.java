package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("md_product_tag")
public class MdProductTag {
    @TableId(type = IdType.AUTO) private Long id;
    private Long enterpriseId;
    private String tagName;
    private String tagColor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}
