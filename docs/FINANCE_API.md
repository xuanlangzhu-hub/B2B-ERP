# 资金与往来接口

所有接口前缀为 `/api/v1`，返回统一 `Result<T>`，并要求 Bearer Token。

## 账户

- `GET /accounts`：分页查询账户
- `GET /accounts/options`：启用账户选项
- `POST /accounts`：新增账户
- `PUT /accounts/{id}`：修改草稿资料，不直接修改当前余额
- `DELETE /accounts/{id}`：未发生流水时删除账户

## 应收应付

- `GET /receivables`：查询客户应收，可按客户和状态筛选
- `GET /payables`：查询供应商应付，可按供应商和状态筛选
- 销售单、采购单全部出入库完成后由系统自动生成，不提供手工新增接口。

## 收款

- `GET /receipts`：分页查询
- `POST /receipts`：保存草稿
- `POST /receipts/{id}/confirm`：确认、自动核销应收、增加账户余额并生成流水
- `POST /receipts/{id}/cancel`：取消草稿或冲销已确认收款

## 付款

- `GET /payments`：分页查询
- `POST /payments`：保存草稿
- `POST /payments/{id}/confirm`：确认、自动核销应付、减少账户余额并生成流水
- `POST /payments/{id}/cancel`：取消草稿或冲销已确认付款

收付款请求必须提供 `storeId`、往来单位、`accountId`、日期、方式和大于 0 的金额。

