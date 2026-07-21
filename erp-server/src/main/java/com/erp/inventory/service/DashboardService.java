package com.erp.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.PageResult;
import com.erp.inventory.entity.InvInbound;
import com.erp.inventory.entity.InvOutbound;
import com.erp.inventory.entity.InvStockBalance;
import com.erp.inventory.mapper.InvInboundMapper;
import com.erp.inventory.mapper.InvOutboundMapper;
import com.erp.masterdata.entity.MdCustomer;
import com.erp.masterdata.entity.MdProduct;
import com.erp.masterdata.entity.MdSupplier;
import com.erp.masterdata.mapper.MdCustomerMapper;
import com.erp.masterdata.mapper.MdProductMapper;
import com.erp.masterdata.mapper.MdSupplierMapper;
import com.erp.purchase.entity.PurOrder;
import com.erp.purchase.mapper.PurOrderMapper;
import com.erp.sales.entity.SalOrder;
import com.erp.sales.mapper.SalOrderMapper;
import com.erp.system.entity.OrgWarehouse;
import com.erp.system.mapper.OrgWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MdProductMapper productMapper;
    private final MdCustomerMapper customerMapper;
    private final MdSupplierMapper supplierMapper;
    private final OrgWarehouseMapper warehouseMapper;
    private final PurOrderMapper purOrderMapper;
    private final SalOrderMapper salOrderMapper;
    private final InvInboundMapper inboundMapper;
    private final InvOutboundMapper outboundMapper;
    private final InvStockService stockService;

    public Map<String, Object> summary(Long enterpriseId) {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);

        List<PurOrder> purchaseOrders = purOrderMapper.selectList(new LambdaQueryWrapper<PurOrder>()
                .select(PurOrder::getTotalAmount)
                .eq(PurOrder::getEnterpriseId, enterpriseId)
                .eq(PurOrder::getStatus, "COMPLETED")
                .ge(PurOrder::getOrderDate, monthStart)
                .lt(PurOrder::getOrderDate, nextMonthStart));
        List<SalOrder> salesOrders = salOrderMapper.selectList(new LambdaQueryWrapper<SalOrder>()
                .select(SalOrder::getTotalAmount)
                .eq(SalOrder::getEnterpriseId, enterpriseId)
                .eq(SalOrder::getStatus, "COMPLETED")
                .ge(SalOrder::getOrderDate, monthStart)
                .lt(SalOrder::getOrderDate, nextMonthStart));

        Map<String, Object> stockSummary = stockService.summaryStocks(enterpriseId, null, null, null);
        PageResult<InvStockBalance> lowStockPage = stockService.pageStocks(
                enterpriseId, 1, 5, null, null, null, true);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productCount", countProducts(enterpriseId));
        data.put("customerCount", countCustomers(enterpriseId));
        data.put("supplierCount", countSuppliers(enterpriseId));
        data.put("warehouseCount", countWarehouses(enterpriseId));
        data.put("purchaseOrderCount", purchaseOrders.size());
        data.put("purchaseAmount", sumPurchaseAmount(purchaseOrders));
        data.put("salesOrderCount", salesOrders.size());
        data.put("salesAmount", sumSalesAmount(salesOrders));
        data.put("inventoryAmount", stockSummary.get("totalStockAmount"));
        data.put("lowStockCount", stockSummary.get("lowStockCount"));
        data.put("pendingInboundCount", countPendingInbounds(enterpriseId));
        data.put("pendingOutboundCount", countPendingOutbounds(enterpriseId));
        data.put("lowStockItems", lowStockPage.getRecords());
        return data;
    }

    private long countProducts(Long enterpriseId) {
        return productMapper.selectCount(new LambdaQueryWrapper<MdProduct>()
                .eq(MdProduct::getEnterpriseId, enterpriseId));
    }

    private long countCustomers(Long enterpriseId) {
        return customerMapper.selectCount(new LambdaQueryWrapper<MdCustomer>()
                .eq(MdCustomer::getEnterpriseId, enterpriseId));
    }

    private long countSuppliers(Long enterpriseId) {
        return supplierMapper.selectCount(new LambdaQueryWrapper<MdSupplier>()
                .eq(MdSupplier::getEnterpriseId, enterpriseId));
    }

    private long countWarehouses(Long enterpriseId) {
        return warehouseMapper.selectCount(new LambdaQueryWrapper<OrgWarehouse>()
                .eq(OrgWarehouse::getEnterpriseId, enterpriseId));
    }

    private long countPendingInbounds(Long enterpriseId) {
        return inboundMapper.selectCount(new LambdaQueryWrapper<InvInbound>()
                .eq(InvInbound::getEnterpriseId, enterpriseId)
                .eq(InvInbound::getStatus, "DRAFT"));
    }

    private long countPendingOutbounds(Long enterpriseId) {
        return outboundMapper.selectCount(new LambdaQueryWrapper<InvOutbound>()
                .eq(InvOutbound::getEnterpriseId, enterpriseId)
                .eq(InvOutbound::getStatus, "DRAFT"));
    }

    private BigDecimal sumPurchaseAmount(List<PurOrder> orders) {
        return orders.stream()
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumSalesAmount(List<SalOrder> orders) {
        return orders.stream()
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
