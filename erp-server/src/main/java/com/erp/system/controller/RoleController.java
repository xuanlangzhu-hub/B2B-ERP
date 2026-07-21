package com.erp.system.controller;

import com.erp.common.Result;
import com.erp.security.LoginUser;
import com.erp.system.dto.RoleRequest;
import com.erp.system.entity.SysRole;
import com.erp.system.service.SysRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<List<SysRole>> list(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(roleService.listRoles(loginUser.getEnterpriseId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<SysRole> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(roleService.getRole(id, loginUser.getEnterpriseId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<SysRole> create(@Valid @RequestBody RoleRequest request,
                                   @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(roleService.createRole(request, loginUser.getEnterpriseId(), loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleRequest request,
                               @AuthenticationPrincipal LoginUser loginUser) {
        roleService.updateRole(id, request, loginUser.getEnterpriseId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        roleService.deleteRole(id, loginUser.getEnterpriseId());
        return Result.success();
    }

    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        roleService.assignMenus(id, menuIds, loginUser.getEnterpriseId());
        return Result.success();
    }

    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<List<Long>> getRoleMenus(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(roleService.getRoleMenus(id, loginUser.getEnterpriseId()));
    }
}
