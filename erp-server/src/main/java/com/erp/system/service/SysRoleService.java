package com.erp.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.system.dto.RoleRequest;
import com.erp.system.entity.SysRole;
import com.erp.system.entity.SysRoleMenu;
import com.erp.system.entity.SysUserRole;
import com.erp.system.mapper.SysRoleMapper;
import com.erp.system.mapper.SysRoleMenuMapper;
import com.erp.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleService extends ServiceImpl<SysRoleMapper, SysRole> {
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    public List<SysRole> listRoles(Long enterpriseId) {
        return list(new LambdaQueryWrapper<SysRole>().eq(SysRole::getEnterpriseId, enterpriseId)
                .orderByAsc(SysRole::getSortNo));
    }

    public SysRole getRole(Long id, Long enterpriseId) {
        SysRole role = lambdaQuery().eq(SysRole::getId, id).eq(SysRole::getEnterpriseId, enterpriseId).one();
        if (role == null) throw new BusinessException("角色不存在");
        return role;
    }

    @Transactional
    public SysRole createRole(RoleRequest request, Long enterpriseId, Long operatorId) {
        if (lambdaQuery().eq(SysRole::getRoleCode, request.getRoleCode())
                .eq(SysRole::getEnterpriseId, enterpriseId).count() > 0) throw new BusinessException("角色编码已存在");
        SysRole role = new SysRole();
        role.setEnterpriseId(enterpriseId); role.setRoleCode(request.getRoleCode()); role.setRoleName(request.getRoleName());
        role.setDataScope(request.getDataScope() != null ? request.getDataScope() : "SELF");
        role.setSortNo(request.getSortNo() != null ? request.getSortNo() : 0); role.setStatus("ENABLED");
        role.setRemark(request.getRemark()); role.setCreatedBy(operatorId); save(role); return role;
    }

    @Transactional
    public void updateRole(Long id, RoleRequest request, Long enterpriseId) {
        SysRole role = getRole(id, enterpriseId);
        role.setRoleName(request.getRoleName());
        role.setDataScope(request.getDataScope());
        role.setSortNo(request.getSortNo());
        role.setRemark(request.getRemark());
        updateById(role);
    }

    @Transactional
    public void deleteRole(Long id, Long enterpriseId) {
        getRole(id, enterpriseId);
        if (userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id)) > 0)
            throw new BusinessException("角色下存在用户，不能删除");
        removeById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds, Long enterpriseId) {
        getRole(roleId, enterpriseId);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds != null) menuIds.stream().distinct().forEach(menuId -> {
            SysRoleMenu relation = new SysRoleMenu(); relation.setRoleId(roleId); relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        });
    }

    public List<Long> getRoleMenus(Long roleId, Long enterpriseId) {
        getRole(roleId, enterpriseId);
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }
}
