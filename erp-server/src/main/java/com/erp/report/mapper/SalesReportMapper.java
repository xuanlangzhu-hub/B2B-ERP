package com.erp.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface SalesReportMapper {

    @Select("""
            <script>
            SELECT o.id AS orderId,
                   o.order_no AS orderNo,
                   o.order_date AS orderDate,
                   c.id AS customerId,
                   c.customer_code AS customerCode,
                   c.customer_name AS customerName,
                   p.category_id AS categoryId,
                   COALESCE(pc.category_name, '未分类') AS categoryName,
                   i.product_id AS productId,
                   i.product_code AS productCode,
                   i.product_name AS productName,
                   i.specification AS specification,
                   u.unit_name AS unitName,
                   i.outbound_quantity AS grossQuantity,
                   COALESCE(ret.returnQuantity, 0) AS returnQuantity,
                   i.outbound_quantity - COALESCE(ret.returnQuantity, 0) AS netQuantity,
                   i.amount AS salesAmount,
                   COALESCE(ret.returnAmount, 0) AS returnAmount,
                   i.amount - COALESCE(ret.returnAmount, 0) AS netSalesAmount,
                   COALESCE(cost.salesCost, 0) - COALESCE(returnCost.returnCost, 0) AS costAmount,
                   i.amount - COALESCE(ret.returnAmount, 0)
                       - (COALESCE(cost.salesCost, 0) - COALESCE(returnCost.returnCost, 0)) AS profitAmount
            FROM sal_order o
            JOIN sal_order_item i ON i.order_id = o.id
            JOIN md_customer c ON c.id = o.customer_id
            LEFT JOIN md_product p ON p.id = i.product_id
            LEFT JOIN md_product_category pc ON pc.id = p.category_id
            LEFT JOIN md_unit u ON u.id = i.unit_id
            LEFT JOIN (
                SELECT ri.sales_order_item_id,
                       SUM(ri.quantity) AS returnQuantity,
                       SUM(ri.amount) AS returnAmount
                FROM sal_return r
                JOIN sal_return_item ri ON ri.return_id = r.id
                WHERE r.status = 'COMPLETED' AND r.deleted = 0
                GROUP BY ri.sales_order_item_id
            ) ret ON ret.sales_order_item_id = i.id
            LEFT JOIN (
                SELECT source_item_id, SUM(amount) AS salesCost
                FROM inv_stock_movement
                WHERE movement_type = 'SALES_OUT' AND direction = 'OUT'
                GROUP BY source_item_id
            ) cost ON cost.source_item_id = i.id
            LEFT JOIN (
                SELECT ri.sales_order_item_id, SUM(m.amount) AS returnCost
                FROM sal_return r
                JOIN sal_return_item ri ON ri.return_id = r.id
                JOIN inv_stock_movement m ON m.source_item_id = ri.id
                    AND m.movement_type = 'SALES_RETURN_IN' AND m.direction = 'IN'
                WHERE r.status = 'COMPLETED' AND r.deleted = 0
                GROUP BY ri.sales_order_item_id
            ) returnCost ON returnCost.sales_order_item_id = i.id
            WHERE o.enterprise_id = #{enterpriseId}
              AND o.deleted = 0
              AND o.status = 'COMPLETED'
            <if test="startDate != null">AND o.order_date &gt;= #{startDate}</if>
            <if test="endDate != null">AND o.order_date &lt;= #{endDate}</if>
            <if test="customerId != null">AND o.customer_id = #{customerId}</if>
            <if test="productId != null">AND i.product_id = #{productId}</if>
            <if test="categoryId != null">AND p.category_id = #{categoryId}</if>
            ORDER BY o.order_date DESC, o.id DESC, i.line_no ASC
            </script>
            """)
    List<Map<String, Object>> selectDetails(@Param("enterpriseId") Long enterpriseId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("customerId") Long customerId,
                                             @Param("productId") Long productId,
                                             @Param("categoryId") Long categoryId);
}
