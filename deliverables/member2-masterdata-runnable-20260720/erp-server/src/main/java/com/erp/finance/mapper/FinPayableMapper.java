package com.erp.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.finance.entity.FinPayable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FinPayableMapper extends BaseMapper<FinPayable> {
    @Select("SELECT * FROM fin_payable WHERE id = #{id} AND enterprise_id = #{enterpriseId} AND deleted = 0 FOR UPDATE")
    FinPayable selectForUpdate(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);
}
