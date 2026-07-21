package com.erp.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.finance.entity.FinAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FinAccountMapper extends BaseMapper<FinAccount> {
    @Select("SELECT * FROM fin_account WHERE id = #{id} AND enterprise_id = #{enterpriseId} AND deleted = 0 FOR UPDATE")
    FinAccount selectForUpdate(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);
}
