package com.erp.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.finance.entity.FinReceivable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FinReceivableMapper extends BaseMapper<FinReceivable> {
    @Select("SELECT * FROM fin_receivable WHERE id = #{id} AND enterprise_id = #{enterpriseId} AND deleted = 0 FOR UPDATE")
    FinReceivable selectForUpdate(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);
}
