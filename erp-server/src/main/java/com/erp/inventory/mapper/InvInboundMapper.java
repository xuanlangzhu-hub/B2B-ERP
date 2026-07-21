package com.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.inventory.entity.InvInbound;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InvInboundMapper extends BaseMapper<InvInbound> {
    @Select("SELECT * FROM inv_inbound WHERE id = #{id} AND enterprise_id = #{enterpriseId} FOR UPDATE")
    InvInbound selectForUpdate(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);
}
