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
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserStoreMapper userStoreMapper;
    private final OrgStoreMapper storeMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = jwtUtils.generateToken(loginUser.getUserId(), loginUser.getUsername(), loginUser.getEnterpriseId());
        return LoginResponse.of(token, loginUser.getUserId(), loginUser.getUsername(),
                loginUser.getRealName(), loginUser.getEnterpriseId());
    }

    public PageResult<SysUser> pageUsers(Long enterpriseId, Integer page, Integer size, String username,
                                         String realName, String status) {
        Page<SysUser> result = page(new Page<>(page, size), new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEnterpriseId, enterpriseId)
                .like(StrUtil.isNotBlank(username), SysUser::getUsername, username)
                .like(StrUtil.isNotBlank(realName), SysUser::getRealName, realName)
                .eq(StrUtil.isNotBlank(status), SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreatedAt));
        result.getRecords().forEach(u -> u.setPasswordHash(null));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public SysUser getUser(Long id, Long enterpriseId) {
        SysUser user = lambdaQuery().eq(SysUser::getId, id).eq(SysUser::getEnterpriseId, enterpriseId).one();
        if (user == null) throw new BusinessException("用户不存在");
        user.setPasswordHash(null);
        return user;
    }

    @Transactional
    public SysUser createUser(UserRequest request, Long enterpriseId, Long operatorId) {
        if (lambdaQuery().eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getEnterpriseId, enterpriseId).count() > 0) {
            throw new BusinessException("用户名已存在");
        }
        validateStore(request.getDefaultStoreId(), enterpriseId);
        SysUser user = new SysUser().setEnterpriseId(enterpriseId).setUsername(request.getUsername().trim())
                .setPasswordHash(passwordEncoder.encode("123456")).setRealName(request.getRealName().trim())
                .setPhone(request.getPhone()).setEmail(request.getEmail()).setDefaultStoreId(request.getDefaultStoreId())
                .setStatus("ENABLED").setRemark(request.getRemark()).setCreatedBy(operatorId);
        save(user);
        if (request.getDefaultStoreId() != null) addUserStore(user.getId(), request.getDefaultStoreId());
        user.setPasswordHash(null);
        return user;
    }

    @Transactional
    public void updateUser(Long id, UserRequest request, Long enterpriseId) {
        SysUser user = requireUser(id, enterpriseId);
        validateStore(request.getDefaultStoreId(), enterpriseId);
        user.setRealName(request.getRealName().trim()).setPhone(request.getPhone()).setEmail(request.getEmail())
                .setDefaultStoreId(request.getDefaultStoreId()).setRemark(request.getRemark());
        updateById(user);
        if (request.getDefaultStoreId() != null) addUserStore(id, request.getDefaultStoreId());
    }

    @Transactional
    public void deleteUser(Long id, Long enterpriseId) {
        SysUser user = requireUser(id, enterpriseId);
        if ("admin".equals(user.getUsername())) throw new BusinessException("不能删除管理员账号");
        removeById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        userStoreMapper.delete(new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getUserId, id));
    }

    @Transactional
    public void resetPassword(Long id, Long enterpriseId) {
        SysUser user = requireUser(id, enterpriseId);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        updateById(user);
    }

    @Transactional
    public void updateStatus(Long id, String status, Long enterpriseId) {
        if (!List.of("ENABLED", "DISABLED").contains(status)) throw new BusinessException("用户状态不正确");
        SysUser user = requireUser(id, enterpriseId);
        if ("admin".equals(user.getUsername())) throw new BusinessException("不能禁用管理员账号");
        user.setStatus(status);
        updateById(user);
    }

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds, Long enterpriseId) {
        requireUser(userId, enterpriseId);
        if (roleIds != null && !roleIds.isEmpty()) {
            long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getEnterpriseId, enterpriseId).in(SysRole::getId, roleIds));
            if (count != roleIds.stream().distinct().count()) throw new BusinessException("包含无权分配的角色");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds != null) roleIds.stream().distinct().forEach(roleId -> {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        });
    }

    public List<Long> getUserRoles(Long userId, Long enterpriseId) {
        requireUser(userId, enterpriseId);
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }

    public List<String> getUserPermissions(Long userId) {
        return roleMenuMapper.selectPermissionsByUserId(userId);
    }

    public SysUser profile(Long userId, Long enterpriseId) {
        return getUser(userId, enterpriseId);
    }

    @Transactional
    public void updateProfile(Long userId, Long enterpriseId, ProfileRequest request) {
        SysUser user = requireUser(userId, enterpriseId);
        user.setRealName(request.getRealName().trim()).setPhone(request.getPhone())
                .setEmail(request.getEmail()).setAvatarUrl(request.getAvatarUrl());
        updateById(user);
    }

    @Transactional
    public void changePassword(Long userId, Long enterpriseId, ChangePasswordRequest request) {
        SysUser user = requireUser(userId, enterpriseId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("当前密码不正确");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("新密码不能与当前密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        updateById(user);
    }

    public List<OrgStore> accessibleStores(Long userId, Long enterpriseId) {
        SysUser user = requireUser(userId, enterpriseId);
        List<Long> ids = userStoreMapper.selectList(new LambdaQueryWrapper<SysUserStore>()
                .eq(SysUserStore::getUserId, userId)).stream().map(SysUserStore::getStoreId).toList();
        LambdaQueryWrapper<OrgStore> wrapper = new LambdaQueryWrapper<OrgStore>()
                .eq(OrgStore::getEnterpriseId, enterpriseId).eq(OrgStore::getStatus, "ENABLED")
                .orderByAsc(OrgStore::getStoreCode);
        if (!ids.isEmpty()) {
            wrapper.in(OrgStore::getId, ids);
        } else if (user.getDefaultStoreId() != null) {
            wrapper.eq(OrgStore::getId, user.getDefaultStoreId());
        } else {
            return List.of();
        }
        return storeMapper.selectList(wrapper);
    }

    @Transactional
    public void changeDefaultStore(Long userId, Long enterpriseId, Long storeId) {
        if (accessibleStores(userId, enterpriseId).stream().noneMatch(store -> store.getId().equals(storeId))) {
            throw new BusinessException("无权切换到该门店");
        }
        SysUser user = requireUser(userId, enterpriseId);
        user.setDefaultStoreId(storeId);
        updateById(user);
    }

    private SysUser requireUser(Long id, Long enterpriseId) {
        SysUser user = lambdaQuery().eq(SysUser::getId, id).eq(SysUser::getEnterpriseId, enterpriseId).one();
        if (user == null) throw new BusinessException("用户不存在");
        return user;
    }

    private void validateStore(Long storeId, Long enterpriseId) {
        if (storeId == null) return;
        if (storeMapper.selectCount(new LambdaQueryWrapper<OrgStore>().eq(OrgStore::getId, storeId)
                .eq(OrgStore::getEnterpriseId, enterpriseId).eq(OrgStore::getStatus, "ENABLED")) == 0) {
            throw new BusinessException("默认门店不存在或已停用");
        }
    }

    private void addUserStore(Long userId, Long storeId) {
        if (userStoreMapper.selectCount(new LambdaQueryWrapper<SysUserStore>()
                .eq(SysUserStore::getUserId, userId).eq(SysUserStore::getStoreId, storeId)) == 0) {
            SysUserStore relation = new SysUserStore();
            relation.setUserId(userId);
            relation.setStoreId(storeId);
            userStoreMapper.insert(relation);
        }
    }
}
