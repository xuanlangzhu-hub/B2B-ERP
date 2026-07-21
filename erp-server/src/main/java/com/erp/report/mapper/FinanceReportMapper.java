package com.erp.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface FinanceReportMapper {

    @Select("""
            <script>
            SELECT event.partnerId, c.customer_code AS partnerCode, c.customer_name AS partnerName,
                   event.businessDate, event.eventType, event.sourceNo, event.amount, event.remark
            FROM (
                SELECT o.customer_id AS partnerId, o.order_date AS businessDate, 'SALE' AS eventType,
                       o.order_no AS sourceNo, o.payable_amount AS amount, o.remark
                FROM sal_order o WHERE o.enterprise_id=#{enterpriseId} AND o.deleted=0 AND o.status='COMPLETED'
                UNION ALL
                SELECT r.customer_id, r.return_date, 'RETURN', r.return_no, r.total_amount, r.return_reason
                FROM sal_return r WHERE r.enterprise_id=#{enterpriseId} AND r.deleted=0 AND r.status='COMPLETED'
                UNION ALL
                SELECT receipt.customer_id, receipt.receipt_date, 'RECEIPT', receipt.receipt_no,
                       receipt.receipt_amount, receipt.remark
                FROM fin_receipt receipt WHERE receipt.enterprise_id=#{enterpriseId}
                    AND receipt.deleted=0 AND receipt.status='CONFIRMED'
            ) event
            JOIN md_customer c ON c.id=event.partnerId
            WHERE 1=1
            <if test="partnerId != null">AND event.partnerId=#{partnerId}</if>
            <if test="endDate != null">AND event.businessDate &lt;=#{endDate}</if>
            ORDER BY event.businessDate ASC, event.sourceNo ASC
            </script>
            """)
    List<Map<String, Object>> customerEvents(@Param("enterpriseId") Long enterpriseId,
                                              @Param("partnerId") Long partnerId,
                                              @Param("endDate") LocalDate endDate);

    @Select("""
            <script>
            SELECT event.partnerId, s.supplier_code AS partnerCode, s.supplier_name AS partnerName,
                   event.businessDate, event.eventType, event.sourceNo, event.amount, event.remark
            FROM (
                SELECT o.supplier_id AS partnerId, o.order_date AS businessDate, 'PURCHASE' AS eventType,
                       o.order_no AS sourceNo, o.payable_amount AS amount, o.remark
                FROM pur_order o WHERE o.enterprise_id=#{enterpriseId} AND o.deleted=0 AND o.status='COMPLETED'
                UNION ALL
                SELECT r.supplier_id, r.return_date, 'RETURN', r.return_no, r.total_amount, r.return_reason
                FROM pur_return r WHERE r.enterprise_id=#{enterpriseId} AND r.deleted=0 AND r.status='COMPLETED'
                UNION ALL
                SELECT payment.supplier_id, payment.payment_date, 'PAYMENT', payment.payment_no,
                       payment.payment_amount, payment.remark
                FROM fin_payment payment WHERE payment.enterprise_id=#{enterpriseId}
                    AND payment.deleted=0 AND payment.status='CONFIRMED'
            ) event
            JOIN md_supplier s ON s.id=event.partnerId
            WHERE 1=1
            <if test="partnerId != null">AND event.partnerId=#{partnerId}</if>
            <if test="endDate != null">AND event.businessDate &lt;=#{endDate}</if>
            ORDER BY event.businessDate ASC, event.sourceNo ASC
            </script>
            """)
    List<Map<String, Object>> supplierEvents(@Param("enterpriseId") Long enterpriseId,
                                              @Param("partnerId") Long partnerId,
                                              @Param("endDate") LocalDate endDate);
}
