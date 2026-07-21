package com.erp.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.finance.entity.FinPayment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FinPaymentMapper extends BaseMapper<FinPayment> {
    @Select("SELECT * FROM fin_payment WHERE id = #{id} AND enterprise_id = #{enterpriseId} AND deleted = 0 FOR UPDATE")
    FinPayment selectForUpdate(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);
}
