package com.erp.system.controller;

import com.erp.common.Result;
import com.erp.security.LoginUser;
import com.erp.system.entity.SysMenu;
import com.erp.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class MenuController {

    private final SysMenuService menuService;

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<List<SysMenu>> allMenus() {
        return Result.success(menuService.listAllMenus());
    }

    @GetMapping
    public Result<List<SysMenu>> userMenus(@AuthenticationPrincipal LoginUser user) {
        return Result.success(menuService.listUserMenus(user.getPermissions()));
    }
}
