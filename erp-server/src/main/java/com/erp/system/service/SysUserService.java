package com.erp.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.security.JwtUtils;
import com.erp.security.LoginUser;
import com.erp.system.dto.*;
import com.erp.system.entity.*;
import com.erp.system.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = jwtUtils.generateToken(
                loginUser.getUserId(), loginUser.getUsername(), loginUser.getEnterpriseId());
        return LoginResponse.of(token, loginUser.getUserId(), loginUser.getUsername(),
                loginUser.getRealName(), loginUser.getEnterpriseId());
    }

    public PageResult<SysUser> pageUsers(Integer page, Integer size, String username,
                                          String realName, String status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(username), SysUser::getUsername, username)
                .like(StrUtil.isNotBlank(realName), SysUser::getRealName, realName)
                .eq(StrUtil.isNotBlank(status), SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreatedAt);
        Page<SysUser> result = page(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPasswordHash(null));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public SysUser createUser(UserRequest request, Long enterpriseId, Long operatorId) {
        if (lambdaQuery().eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getEnterpriseId, enterpriseId).count() > 0) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser()
                .setEnterpriseId(enterpriseId)
                .setUsername(request.getUsername())
                .setPasswordHash(passwordEncoder.encode("123456"))
                .setRealName(request.getRealName())
                .setPhone(request.getPhone())
                .setEmail(request.getEmail())
                .setDefaultStoreId(request.getDefaultStoreId())
                .setStatus("ENABLED")
                .setRemark(request.getRemark())
                .setCreatedBy(operatorId);
        save(user);
        return user;
    }

    @Transactional
    public void updateUser(Long id, UserRequest request) {
        SysUser user = getById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setRealName(request.getRealName())
                .setPhone(request.getPhone())
                .setEmail(request.getEmail())
                .setDefaultStoreId(request.getDefaultStoreId())
                .setRemark(request.getRemark());
        updateById(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        SysUser user = getById(id);
        if (user == null) throw new BusinessException("用户不存在");
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException("不能删除管理员账号");
        }
        removeById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    @Transactional
    public void resetPassword(Long id) {
        SysUser user = getById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setPasswordHash(passwordEncoder.encode("123456"));
        updateById(user);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        SysUser user = getById(id);
        if (user == null) throw new BusinessException("用户不存在");
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException("不能禁用管理员账号");
        }
        user.setStatus(status);
        updateById(user);
    }

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    public List<Long> getUserRoles(Long userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }

    public List<String> getUserPermissions(Long userId) {
        return roleMenuMapper.selectPermissionsByUserId(userId);
    }
}
