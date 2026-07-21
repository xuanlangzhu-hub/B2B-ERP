package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.system.entity.SysOperationLog;
import com.erp.system.mapper.SysOperationLogMapper;
import com.erp.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/operation-logs")
@RequiredArgsConstructor
public class LogController {

    private final SysOperationLogMapper logMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('system:log:list')")
    public Result<PageResult<SysOperationLog>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String moduleName,
            @AuthenticationPrincipal LoginUser loginUser) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperationLog::getEnterpriseId, loginUser.getEnterpriseId())
                .eq(username != null, SysOperationLog::getUsername, username)
                .eq(moduleName != null, SysOperationLog::getModuleName, moduleName)
                .orderByDesc(SysOperationLog::getCreatedAt);
        Page<SysOperationLog> result = logMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }
}
