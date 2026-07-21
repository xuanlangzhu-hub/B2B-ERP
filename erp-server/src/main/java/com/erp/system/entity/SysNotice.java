package com.erp.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("sys_notice")
public class SysNotice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String noticeTitle;
    private String noticeContent;
    private String noticeType;
    private String publishStatus;
    private LocalDateTime publishedAt;
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
    private Boolean read;
}
