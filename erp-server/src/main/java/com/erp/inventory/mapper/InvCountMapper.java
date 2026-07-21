package com.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.inventory.entity.InvCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InvCountMapper extends BaseMapper<InvCount> {

    @Select("SELECT * FROM inv_count WHERE id = #{id} AND enterprise_id = #{enterpriseId} AND deleted = 0 FOR UPDATE")
    InvCount selectForUpdate(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);
}
