package com.erp.system.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.security.LoginUser;
import com.erp.system.dto.UserRequest;
import com.erp.system.entity.SysUser;
import com.erp.system.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(userService.pageUsers(loginUser.getEnterpriseId(), page, size, username, realName, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<SysUser> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(userService.getUser(id, loginUser.getEnterpriseId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<SysUser> create(@Valid @RequestBody UserRequest request,
                                   @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(userService.createUser(request, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserRequest request,
                               @AuthenticationPrincipal LoginUser loginUser) {
        userService.updateUser(id, request, loginUser.getEnterpriseId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        userService.deleteUser(id, loginUser.getEnterpriseId());
        return Result.success();
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Void> resetPassword(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        userService.resetPassword(id, loginUser.getEnterpriseId());
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status,
                                     @AuthenticationPrincipal LoginUser loginUser) {
        userService.updateStatus(id, status, loginUser.getEnterpriseId());
        return Result.success();
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        userService.assignRoles(id, roleIds, loginUser.getEnterpriseId());
        return Result.success();
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<List<Long>> getUserRoles(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(userService.getUserRoles(id, loginUser.getEnterpriseId()));
    }
}
