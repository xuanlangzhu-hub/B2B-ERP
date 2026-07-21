package com.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.inventory.entity.InvOutbound;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InvOutboundMapper extends BaseMapper<InvOutbound> {
    @Select("SELECT * FROM inv_outbound WHERE id = #{id} AND enterprise_id = #{enterpriseId} FOR UPDATE")
    InvOutbound selectForUpdate(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);
}
