package com.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.inventory.entity.InvTransfer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InvTransferMapper extends BaseMapper<InvTransfer> {
    @Select("SELECT * FROM inv_transfer WHERE id = #{id} AND enterprise_id = #{enterpriseId} AND deleted = 0 FOR UPDATE")
    InvTransfer selectForUpdate(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);
}
