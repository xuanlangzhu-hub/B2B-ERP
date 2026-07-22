package com.erp.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.BusinessException;
import com.erp.system.dto.RegisterRequest;
import com.erp.system.dto.RegisterResponse;
import com.erp.system.entity.*;
import com.erp.system.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final OrgEnterpriseMapper enterpriseMapper;
    private final OrgStoreMapper storeMapper;
    private final OrgWarehouseMapper warehouseMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserStoreMapper userStoreMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0) {
            throw new BusinessException("该邮箱已注册，请直接登录");
        }

        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, "ENABLED"));
        if (menus.isEmpty()) {
            throw new BusinessException("系统菜单尚未初始化，请先按顺序执行 sql/V1 至 V10 脚本");
        }

        String suffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
        OrgEnterprise enterprise = new OrgEnterprise()
                .setEnterpriseCode("ENT" + suffix)
                .setEnterpriseName(request.getEnterpriseName().trim())
                .setContactName(request.getRealName().trim())
                .setContactPhone(request.getPhone().trim())
                .setAddress(trimToNull(request.getEnterpriseAddress()))
                .setStatus("ENABLED")
                .setRemark("用户注册创建");
        enterpriseMapper.insert(enterprise);

        OrgStore store = new OrgStore()
                .setEnterpriseId(enterprise.getId())
                .setStoreCode("STORE001")
                .setStoreName(request.getStoreName().trim())
                .setManagerName(request.getRealName().trim())
                .setContactPhone(request.getPhone().trim())
                .setAddress(trimToNull(request.getStoreAddress()))
                .setStatus("ENABLED")
                .setRemark("注册时创建的默认门店");
        storeMapper.insert(store);

        OrgWarehouse warehouse = new OrgWarehouse()
                .setEnterpriseId(enterprise.getId())
                .setStoreId(store.getId())
                .setWarehouseCode("WH001")
                .setWarehouseName(store.getStoreName() + "主仓")
                .setWarehouseType("STORE")
                .setManagerName(request.getRealName().trim())
                .setContactPhone(request.getPhone().trim())
                .setAddress(trimToNull(request.getStoreAddress()))
                .setAllowNegative(false)
                .setStatus("ENABLED")
                .setRemark("注册时创建的默认仓库");
        warehouseMapper.insert(warehouse);

        SysUser user = new SysUser()
                .setEnterpriseId(enterprise.getId())
                .setUsername(username)
                .setPasswordHash(passwordEncoder.encode(request.getPassword()))
                .setRealName(request.getRealName().trim())
                .setPhone(request.getPhone().trim())
                .setEmail(username)
                .setDefaultStoreId(store.getId())
                .setStatus("ENABLED")
                .setRemark("企业注册管理员");
        userMapper.insert(user);

        SysRole adminRole = new SysRole();
        adminRole.setEnterpriseId(enterprise.getId());
        adminRole.setRoleCode("ADMIN");
        adminRole.setRoleName("系统管理员");
        adminRole.setDataScope("ALL");
        adminRole.setStatus("ENABLED");
        adminRole.setSortNo(1);
        adminRole.setCreatedBy(user.getId());
        adminRole.setRemark("注册企业默认管理员角色");
        roleMapper.insert(adminRole);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(adminRole.getId());
        userRoleMapper.insert(userRole);

        for (SysMenu menu : menus) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(adminRole.getId());
            relation.setMenuId(menu.getId());
            roleMenuMapper.insert(relation);
        }

        SysUserStore userStore = new SysUserStore();
        userStore.setUserId(user.getId());
        userStore.setStoreId(store.getId());
        userStoreMapper.insert(userStore);

        enterprise.setCreatedBy(user.getId()).setUpdatedBy(user.getId());
        store.setCreatedBy(user.getId()).setUpdatedBy(user.getId());
        warehouse.setCreatedBy(user.getId()).setUpdatedBy(user.getId());
        enterpriseMapper.updateById(enterprise);
        storeMapper.updateById(store);
        warehouseMapper.updateById(warehouse);

        return new RegisterResponse(enterprise.getId(), user.getId(), username);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}
