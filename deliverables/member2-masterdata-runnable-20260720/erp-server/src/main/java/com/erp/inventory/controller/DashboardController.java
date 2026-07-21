package com.erp.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.Result;
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
import com.erp.security.LoginUser;
import com.erp.system.entity.OrgWarehouse;
import com.erp.system.mapper.OrgWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final MdProductMapper productMapper;
    private final MdCustomerMapper customerMapper;
    private final MdSupplierMapper supplierMapper;
    private final OrgWarehouseMapper warehouseMapper;
    private final PurOrderMapper purOrderMapper;
    private final SalOrderMapper salOrderMapper;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('dashboard:summary')")
    public Result<Map<String, Object>> summary(@AuthenticationPrincipal LoginUser user) {
        Map<String, Object> data = new HashMap<>();
        Long enterpriseId = user.getEnterpriseId();
        data.put("productCount", productMapper.selectCount(new LambdaQueryWrapper<MdProduct>()
                .eq(MdProduct::getEnterpriseId, enterpriseId)));
        data.put("customerCount", customerMapper.selectCount(new LambdaQueryWrapper<MdCustomer>()
                .eq(MdCustomer::getEnterpriseId, enterpriseId)));
        data.put("supplierCount", supplierMapper.selectCount(new LambdaQueryWrapper<MdSupplier>()
                .eq(MdSupplier::getEnterpriseId, enterpriseId)));
        data.put("warehouseCount", warehouseMapper.selectCount(new LambdaQueryWrapper<OrgWarehouse>()
                .eq(OrgWarehouse::getEnterpriseId, enterpriseId)));

        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);

        BigDecimal purMonthAmount = purOrderMapper.selectList(new LambdaQueryWrapper<PurOrder>()
                        .select(PurOrder::getTotalAmount)
                        .eq(PurOrder::getEnterpriseId, enterpriseId)
                        .eq(PurOrder::getStatus, "COMPLETED")
                        .ge(PurOrder::getOrderDate, monthStart)
                        .lt(PurOrder::getOrderDate, nextMonthStart))
                .stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal salMonthAmount = salOrderMapper.selectList(new LambdaQueryWrapper<SalOrder>()
                        .select(SalOrder::getTotalAmount)
                        .eq(SalOrder::getEnterpriseId, enterpriseId)
                        .eq(SalOrder::getStatus, "COMPLETED")
                        .ge(SalOrder::getOrderDate, monthStart)
                        .lt(SalOrder::getOrderDate, nextMonthStart))
                .stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        data.put("purchaseAmount", purMonthAmount);
        data.put("salesAmount", salMonthAmount);

        return Result.success(data);
    }
}
