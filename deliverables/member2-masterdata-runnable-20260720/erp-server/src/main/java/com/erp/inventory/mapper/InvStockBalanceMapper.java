package com.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.inventory.entity.InvStockBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InvStockBalanceMapper extends BaseMapper<InvStockBalance> {

    @Select("SELECT * FROM inv_stock_balance WHERE enterprise_id = #{enterpriseId} AND warehouse_id = #{warehouseId} AND product_id = #{productId} FOR UPDATE")
    InvStockBalance selectForUpdate(@Param("enterpriseId") Long enterpriseId, @Param("warehouseId") Long warehouseId, @Param("productId") Long productId);
}
