package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.entity.SysMenu;
import com.erp.system.mapper.SysMenuMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysMenuService extends ServiceImpl<SysMenuMapper, SysMenu> {

    public List<SysMenu> listAllMenus() {
        return lambdaQuery().orderByAsc(SysMenu::getSortNo).list();
    }

    public List<SysMenu> listUserMenus(List<String> permissions) {
        if (permissions.contains("*:*:*")) {
            return lambdaQuery().eq(SysMenu::getVisible, 1)
                    .eq(SysMenu::getStatus, "ENABLED")
                    .in(SysMenu::getMenuType, "DIRECTORY", "MENU")
                    .orderByAsc(SysMenu::getSortNo)
                    .list();
        }
        return lambdaQuery().in(SysMenu::getPermissionCode, permissions)
                .eq(SysMenu::getVisible, 1)
                .eq(SysMenu::getStatus, "ENABLED")
                .in(SysMenu::getMenuType, "DIRECTORY", "MENU")
                .orderByAsc(SysMenu::getSortNo)
                .list();
    }
}
