package com.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.inventory.entity.InvBorrow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InvBorrowMapper extends BaseMapper<InvBorrow> {
    @Select("SELECT * FROM inv_borrow WHERE id = #{id} AND enterprise_id = #{enterpriseId} AND deleted = 0 FOR UPDATE")
    InvBorrow selectForUpdate(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);
}
