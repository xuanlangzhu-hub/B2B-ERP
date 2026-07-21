package com.erp.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("org_enterprise")
public class OrgEnterprise {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String enterpriseCode;
    private String enterpriseName;
    private String contactName;
    private String contactPhone;
    private String address;
    private String logoUrl;
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
}
