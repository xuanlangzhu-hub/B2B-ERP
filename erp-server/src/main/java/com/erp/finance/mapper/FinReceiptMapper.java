package com.erp.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.finance.entity.FinReceipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FinReceiptMapper extends BaseMapper<FinReceipt> {
    @Select("SELECT * FROM fin_receipt WHERE id = #{id} AND enterprise_id = #{enterpriseId} AND deleted = 0 FOR UPDATE")
    FinReceipt selectForUpdate(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);
}
