package com.erp.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.sales.entity.SalReturn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SalReturnMapper extends BaseMapper<SalReturn> {
    @Select("SELECT * FROM sal_return WHERE id=#{id} AND enterprise_id=#{enterpriseId} AND deleted=0 FOR UPDATE")
    SalReturn selectForUpdate(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);
}
