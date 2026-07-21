package com.erp.security;

import com.erp.system.entity.SysUser;
import com.erp.system.mapper.SysRoleMenuMapper;
import com.erp.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));
        return toLoginUser(user, "用户名: " + username);
    }

    public LoginUser loadUserById(Long userId) throws UsernameNotFoundException {
        SysUser user = userMapper.selectById(userId);
        return toLoginUser(user, "用户ID: " + userId);
    }

    private LoginUser toLoginUser(SysUser user, String identity) {
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + identity);
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw new UsernameNotFoundException("用户已被禁用: " + identity);
        }
        List<String> permissions = roleMenuMapper.selectPermissionsByUserId(user.getId());
        return new LoginUser(
                user.getId(), user.getUsername(), user.getPasswordHash(), user.getRealName(),
                user.getEnterpriseId(), user.getDefaultStoreId(), permissions, user.getStatus());
    }
}
