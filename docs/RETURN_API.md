# 退货业务接口

所有接口以 `/api/v1` 为前缀，请求头携带 `Authorization: Bearer <token>`。

## 销售退货

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/sales-returns` | 分页查询；支持 `page`、`size`、`returnNo`、`customerId`、`status` |
| `GET` | `/sales-returns/{id}` | 查询退货单和明细 |
| `POST` | `/sales-returns` | 从已完成销售单创建草稿 |
| `POST` | `/sales-returns/{id}/approve` | 审核草稿 |
| `POST` | `/sales-returns/{id}/cancel` | 取消草稿 |
| `POST` | `/inbounds/from-sales-return/{id}` | 从已审核退货单生成退货入库单 |
| `POST` | `/inbounds/{id}/confirm` | 确认退货入库，完成库存与应收冲减 |

创建请求示例：

```json
{
  "salesOrderId": 1001,
  "returnDate": "2026-07-20",
  "warehouseId": 1,
  "returnReason": "客户拒收",
  "remark": "外包装破损",
  "items": [
    {
      "salesOrderItemId": 2001,
      "quantity": 1
    }
  ]
}
```

## 采购退货

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/purchase-returns` | 分页查询；支持 `page`、`size`、`returnNo`、`supplierId`、`status` |
| `GET` | `/purchase-returns/{id}` | 查询退货单和明细 |
| `POST` | `/purchase-returns` | 从已完成采购单创建草稿 |
| `POST` | `/purchase-returns/{id}/approve` | 审核草稿 |
| `POST` | `/purchase-returns/{id}/cancel` | 取消草稿 |
| `POST` | `/outbounds/from-purchase-return/{id}` | 从已审核退货单生成退货出库单 |
| `POST` | `/outbounds/{id}/confirm` | 确认退货出库，完成库存与应付冲减 |

创建请求示例：

```json
{
  "purchaseOrderId": 3001,
  "returnDate": "2026-07-20",
  "warehouseId": 1,
  "returnReason": "质量不合格",
  "remark": "供应商同意退回",
  "items": [
    {
      "purchaseOrderItemId": 4001,
      "quantity": 1
    }
  ]
}
```

## 状态与限制

- 状态流转：`DRAFT -> APPROVED -> COMPLETED`，草稿也可变为 `CANCELLED`。
- 退货数量不得超过原订单的剩余可退数量。
- 当前版本退货金额不得超过原订单未收或未付余额，退款型退货暂不自动处理。
- 退货入库或出库确认与库存、订单退货数量、应收应付冲减在同一数据库事务中完成。
