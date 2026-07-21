package com.erp.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private Long userId;
    private String username;
    private String moduleName;
    private String operationType;
    private String requestMethod;
    private String requestUri;
    private String requestParams;
    private String responseCode;
    private String ipAddress;
    private Long durationMs;
    private Integer success;
    private String errorMessage;
    private LocalDateTime createdAt;
}
