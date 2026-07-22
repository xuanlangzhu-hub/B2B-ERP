package com.erp.system.service;

import com.erp.system.mapper.SysMenuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SysMenuServiceTest {

    @Mock
    private SysMenuMapper menuMapper;

    private SysMenuService service;

    @BeforeEach
    void setUp() {
        service = new SysMenuService();
        ReflectionTestUtils.setField(service, "baseMapper", menuMapper);
    }

    @Test
    void listUserMenusReturnsEmptyWithoutQueryingWhenPermissionsAreEmpty() {
        assertEquals(List.of(), service.listUserMenus(List.of()));
        verifyNoInteractions(menuMapper);
    }
}
