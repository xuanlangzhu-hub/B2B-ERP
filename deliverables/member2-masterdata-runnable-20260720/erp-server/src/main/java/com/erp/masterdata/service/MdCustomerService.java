package com.erp.masterdata.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.masterdata.entity.MdCustomer;
import com.erp.masterdata.mapper.MdCustomerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MdCustomerService extends ServiceImpl<MdCustomerMapper, MdCustomer> {

    public PageResult<MdCustomer> pageQuery(Integer page, Integer size, String customerCode, String customerName,
                                             String contactPhone, String status) {
        LambdaQueryWrapper<MdCustomer> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(customerCode), MdCustomer::getCustomerCode, customerCode)
                .like(StrUtil.isNotBlank(customerName), MdCustomer::getCustomerName, customerName)
                .like(StrUtil.isNotBlank(contactPhone), MdCustomer::getContactPhone, contactPhone)
                .eq(StrUtil.isNotBlank(status), MdCustomer::getStatus, status)
                .orderByDesc(MdCustomer::getCreatedAt);
        Page<MdCustomer> result = page(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public MdCustomer create(MdCustomer customer, Long enterpriseId, Long operatorId) {
        if (lambdaQuery().eq(MdCustomer::getCustomerCode, customer.getCustomerCode())
                .eq(MdCustomer::getEnterpriseId, enterpriseId).count() > 0) {
            throw new BusinessException("客户编码已存在");
        }
        customer.setEnterpriseId(enterpriseId);
        customer.setCreatedBy(operatorId);
        save(customer);
        return customer;
    }

    @Transactional
    public void update(MdCustomer customer) {
        MdCustomer existing = getById(customer.getId());
        if (existing == null) {
            throw new BusinessException("客户不存在");
        }
        if (lambdaQuery().eq(MdCustomer::getCustomerCode, customer.getCustomerCode())
                .eq(MdCustomer::getEnterpriseId, existing.getEnterpriseId())
                .ne(MdCustomer::getId, customer.getId()).count() > 0) {
            throw new BusinessException("客户编码已存在");
        }
        updateById(customer);
    }

    @Transactional
    public void delete(Long id) {
        MdCustomer customer = getById(id);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        removeById(id);
    }

    public List<Map<String, Object>> options(Long enterpriseId) {
        List<MdCustomer> list = lambdaQuery()
                .eq(MdCustomer::getEnterpriseId, enterpriseId)
                .eq(MdCustomer::getStatus, "ENABLED")
                .orderByAsc(MdCustomer::getCustomerCode)
                .list();
        return list.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("value", c.getId());
            map.put("label", c.getCustomerCode() + " - " + c.getCustomerName());
            return map;
        }).collect(Collectors.toList());
    }
}
