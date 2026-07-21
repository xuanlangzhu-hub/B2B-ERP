package com.erp.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface PurchaseReportMapper {

    @Select("""
            <script>
            SELECT o.id AS orderId,
                   o.order_no AS orderNo,
                   o.order_date AS orderDate,
                   s.id AS supplierId,
                   s.supplier_code AS supplierCode,
                   s.supplier_name AS supplierName,
                   p.category_id AS categoryId,
                   COALESCE(pc.category_name, '未分类') AS categoryName,
                   i.product_id AS productId,
                   i.product_code AS productCode,
                   i.product_name AS productName,
                   i.specification AS specification,
                   u.unit_name AS unitName,
                   i.inbound_quantity AS grossQuantity,
                   COALESCE(ret.returnQuantity, 0) AS returnQuantity,
                   i.inbound_quantity - COALESCE(ret.returnQuantity, 0) AS netQuantity,
                   i.amount AS purchaseAmount,
                   COALESCE(ret.returnAmount, 0) AS returnAmount,
                   i.amount - COALESCE(ret.returnAmount, 0) AS netPurchaseAmount
            FROM pur_order o
            JOIN pur_order_item i ON i.order_id = o.id
            JOIN md_supplier s ON s.id = o.supplier_id
            LEFT JOIN md_product p ON p.id = i.product_id
            LEFT JOIN md_product_category pc ON pc.id = p.category_id
            LEFT JOIN md_unit u ON u.id = i.unit_id
            LEFT JOIN (
                SELECT ri.purchase_order_item_id,
                       SUM(ri.quantity) AS returnQuantity,
                       SUM(ri.amount) AS returnAmount
                FROM pur_return r
                JOIN pur_return_item ri ON ri.return_id = r.id
                WHERE r.status = 'COMPLETED' AND r.deleted = 0
                GROUP BY ri.purchase_order_item_id
            ) ret ON ret.purchase_order_item_id = i.id
            WHERE o.enterprise_id = #{enterpriseId}
              AND o.deleted = 0
              AND o.status = 'COMPLETED'
            <if test="startDate != null">AND o.order_date &gt;= #{startDate}</if>
            <if test="endDate != null">AND o.order_date &lt;= #{endDate}</if>
            <if test="supplierId != null">AND o.supplier_id = #{supplierId}</if>
            <if test="productId != null">AND i.product_id = #{productId}</if>
            <if test="categoryId != null">AND p.category_id = #{categoryId}</if>
            ORDER BY o.order_date DESC, o.id DESC, i.line_no ASC
            </script>
            """)
    List<Map<String, Object>> selectDetails(@Param("enterpriseId") Long enterpriseId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("supplierId") Long supplierId,
                                             @Param("productId") Long productId,
                                             @Param("categoryId") Long categoryId);
}
