package com.erp.finance.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.PageResult;
import com.erp.finance.entity.FinCapitalFlow;
import com.erp.finance.mapper.FinCapitalFlowMapper;
import org.springframework.stereotype.Service;

@Service
public class FinCapitalFlowService extends ServiceImpl<FinCapitalFlowMapper, FinCapitalFlow> {

    public PageResult<FinCapitalFlow> pageFlows(Long enterpriseId, Integer page, Integer size,
                                                String flowType, String startDate, String endDate) {
        LambdaQueryWrapper<FinCapitalFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinCapitalFlow::getEnterpriseId, enterpriseId)
                .eq(StrUtil.isNotBlank(flowType), FinCapitalFlow::getFlowType, flowType)
                .ge(StrUtil.isNotBlank(startDate), FinCapitalFlow::getFlowDate, startDate)
                .le(StrUtil.isNotBlank(endDate), FinCapitalFlow::getFlowDate, endDate)
                .orderByDesc(FinCapitalFlow::getFlowDate)
                .orderByDesc(FinCapitalFlow::getId);
        Page<FinCapitalFlow> result = page(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }
}
