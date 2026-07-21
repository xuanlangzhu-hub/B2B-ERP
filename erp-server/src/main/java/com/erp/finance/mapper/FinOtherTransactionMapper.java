package com.erp.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.finance.entity.FinOtherTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FinOtherTransactionMapper extends BaseMapper<FinOtherTransaction> {
    @Select("SELECT * FROM fin_other_transaction WHERE id=#{id} AND enterprise_id=#{enterpriseId} AND deleted=0 FOR UPDATE")
    FinOtherTransaction selectForUpdate(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);
}
