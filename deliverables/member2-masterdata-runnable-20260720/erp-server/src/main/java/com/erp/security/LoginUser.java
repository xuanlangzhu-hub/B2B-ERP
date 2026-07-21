package com.erp.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class LoginUser implements UserDetails {
    private final Long userId;
    private final String username;
    private final String password;
    private final String realName;
    private final Long enterpriseId;
    private final Long defaultStoreId;
    private final List<String> permissions;
    private final String status;

    public LoginUser(Long userId, String username, String password, String realName,
                     Long enterpriseId, Long defaultStoreId, List<String> permissions, String status) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.enterpriseId = enterpriseId;
        this.defaultStoreId = defaultStoreId;
        this.permissions = permissions;
        this.status = status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return "ENABLED".equals(status); }
}
