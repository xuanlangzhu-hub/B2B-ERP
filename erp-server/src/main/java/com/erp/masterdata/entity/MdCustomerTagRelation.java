package com.erp.masterdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("md_customer_tag_relation")
public class MdCustomerTagRelation {
    @TableId(type = IdType.AUTO) private Long id;
    private Long customerId;
    private Long tagId;
    private LocalDateTime createdAt;
}
