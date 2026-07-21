package com.erp.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.purchase.entity.PurReturn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PurReturnMapper extends BaseMapper<PurReturn> {
    @Select("SELECT * FROM pur_return WHERE id=#{id} AND enterprise_id=#{enterpriseId} AND deleted=0 FOR UPDATE")
    PurReturn selectForUpdate(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);
}
