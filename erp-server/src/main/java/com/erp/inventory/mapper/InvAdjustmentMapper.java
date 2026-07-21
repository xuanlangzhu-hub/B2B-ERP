package com.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.inventory.entity.InvAdjustment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InvAdjustmentMapper extends BaseMapper<InvAdjustment> {
    @Select("SELECT * FROM inv_adjustment WHERE id = #{id} AND enterprise_id = #{enterpriseId} AND deleted = 0 FOR UPDATE")
    InvAdjustment selectForUpdate(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);
}
