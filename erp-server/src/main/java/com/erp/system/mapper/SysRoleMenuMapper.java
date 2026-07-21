package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    @Select("SELECT m.permission_code FROM sys_role_menu rm " +
            "JOIN sys_menu m ON m.id = rm.menu_id " +
            "WHERE rm.role_id IN (SELECT role_id FROM sys_user_role WHERE user_id = #{userId}) " +
            "AND m.permission_code IS NOT NULL AND m.deleted = 0 AND m.status = 'ENABLED'")
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);
}
