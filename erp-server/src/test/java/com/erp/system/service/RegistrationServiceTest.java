package com.erp.system.service;

import com.erp.common.BusinessException;
import com.erp.system.dto.RegisterRequest;
import com.erp.system.dto.RegisterResponse;
import com.erp.system.entity.*;
import com.erp.system.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    @Mock private OrgEnterpriseMapper enterpriseMapper;
    @Mock private OrgStoreMapper storeMapper;
    @Mock private OrgWarehouseMapper warehouseMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private SysRoleMapper roleMapper;
    @Mock private SysMenuMapper menuMapper;
    @Mock private SysUserRoleMapper userRoleMapper;
    @Mock private SysRoleMenuMapper roleMenuMapper;
    @Mock private SysUserStoreMapper userStoreMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private RegistrationService service;

    @Test
    void registerCreatesAnImmediatelyUsableEnterpriseAdministrator() {
        AtomicLong ids = new AtomicLong(10);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("StrongPass1")).thenReturn("bcrypt-value");
        doAnswer(invocation -> { invocation.<OrgEnterprise>getArgument(0).setId(ids.getAndIncrement()); return 1; }).when(enterpriseMapper).insert(any(OrgEnterprise.class));
        doAnswer(invocation -> { invocation.<OrgStore>getArgument(0).setId(ids.getAndIncrement()); return 1; }).when(storeMapper).insert(any(OrgStore.class));
        doAnswer(invocation -> { invocation.<OrgWarehouse>getArgument(0).setId(ids.getAndIncrement()); return 1; }).when(warehouseMapper).insert(any(OrgWarehouse.class));
        doAnswer(invocation -> { invocation.<SysUser>getArgument(0).setId(ids.getAndIncrement()); return 1; }).when(userMapper).insert(any(SysUser.class));
        doAnswer(invocation -> { invocation.<SysRole>getArgument(0).setId(ids.getAndIncrement()); return 1; }).when(roleMapper).insert(any(SysRole.class));
        SysMenu menu1 = new SysMenu(); menu1.setId(1L);
        SysMenu menu2 = new SysMenu(); menu2.setId(2L);
        when(menuMapper.selectList(any())).thenReturn(List.of(menu1, menu2));

        RegisterResponse result = service.register(request());

        assertEquals(10L, result.getEnterpriseId());
        assertEquals(13L, result.getUserId());
        assertEquals("owner@example.com", result.getUsername());

        ArgumentCaptor<SysUser> user = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(user.capture());
        assertEquals(10L, user.getValue().getEnterpriseId());
        assertEquals(11L, user.getValue().getDefaultStoreId());
        assertEquals("bcrypt-value", user.getValue().getPasswordHash());
        assertEquals("owner@example.com", user.getValue().getEmail());

        ArgumentCaptor<OrgWarehouse> warehouse = ArgumentCaptor.forClass(OrgWarehouse.class);
        verify(warehouseMapper).insert(warehouse.capture());
        assertEquals(11L, warehouse.getValue().getStoreId());
        assertEquals("演示门店主仓", warehouse.getValue().getWarehouseName());
        verify(roleMenuMapper, times(2)).insert(any(SysRoleMenu.class));
        verify(userRoleMapper).insert(any(SysUserRole.class));
        verify(userStoreMapper).insert(any(SysUserStore.class));
    }

    @Test
    void registerRejectsDuplicateUsernameBeforeWritingAnything() {
        when(userMapper.selectCount(any())).thenReturn(1L);
        BusinessException error = assertThrows(BusinessException.class, () -> service.register(request()));
        assertTrue(error.getMessage().contains("已注册"));
        verifyNoInteractions(enterpriseMapper, storeMapper, warehouseMapper, roleMapper, menuMapper);
        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void registerRejectsAnUninitializedMenuDatabaseBeforeWritingAnything() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(menuMapper.selectList(any())).thenReturn(List.of());

        BusinessException error = assertThrows(BusinessException.class, () -> service.register(request()));

        assertTrue(error.getMessage().contains("菜单尚未初始化"));
        verifyNoInteractions(enterpriseMapper, storeMapper, warehouseMapper, roleMapper);
        verify(userMapper, never()).insert(any(SysUser.class));
    }

    private RegisterRequest request() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(" Owner@Example.com ");
        request.setPassword("StrongPass1");
        request.setConfirmPassword("StrongPass1");
        request.setEnterpriseName("演示企业");
        request.setRealName("管理员");
        request.setPhone("13800000000");
        request.setStoreName("演示门店");
        request.setEnterpriseAddress("上海市");
        request.setStoreAddress("上海市浦东新区");
        return request;
    }
}
