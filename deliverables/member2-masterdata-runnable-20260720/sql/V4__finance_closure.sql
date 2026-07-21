-- 资金闭环约束：同一业务单据的同一流水类型只能记账一次。
ALTER TABLE fin_capital_flow
    ADD UNIQUE KEY uk_fin_capital_flow_source_type
        (enterprise_id, source_type, source_id, flow_type);

