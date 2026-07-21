# 成员 4 任务书：采购单、供应商付款与采购报表

> 分支：`feature/purchase`  
> 后端模块：`purchase`、`finance/payment`、`report/purchase`  
> 前端目录：`views/purchase`、`api/purchase`  
> 最终责任：完成“采购单审核 → 采购入库 → 供应商付款 → 采购报表”链路中的采购侧

## 1. 本人交付目标

必须完成：

- 采购单列表、详情、新增、修改、删除。
- 采购商品明细。
- 采购单审核、取消和入库后完成。
- 给成员 5 提供待入库采购单接口。
- 供应商付款单列表和新增。
- 采购单已付、未付金额展示。
- 采购明细报表和采购汇总。
- 至少 3 张已完成、1 张待入库采购单及 2 张付款单演示数据。

本周不做：

- 采购申请、询价、报价比价和审批流。
- 分批到货和质检。
- 采购退货完整闭环。
- 发票和税率处理。
- 一张付款单核销多个采购单的复杂逻辑。
- 反审核和红冲。

## 2. 数据库负责范围

| 表 | 用途 |
| --- | --- |
| `pur_order` | 采购单主表 |
| `pur_order_item` | 采购单明细 |
| `fin_payment` | 付款单 |
| `fin_payment_item` | 时间允许再做核销明细 |
| `fin_payable` | 时间允许再做正式应付；本周可由采购单计算 |

不要直接修改 `inv_*` 表，库存增加由成员 5 负责。

### 采购单状态

```text
DRAFT → APPROVED → COMPLETED
  └──────────────→ CANCELLED
```

- 草稿可编辑、删除和审核。
- 已审核不可编辑，等待入库，可取消。
- 已完成和已取消只读。
- 本周不使用 `PARTIALLY_INBOUND`。

## 3. 字段约定

### 主表

| 前端字段 | 类型 | 规则 |
| --- | --- | --- |
| `orderNo` | string | 后端生成、唯一 |
| `supplierId` | string | 必填、供应商启用 |
| `warehouseId` | string | 必填、仓库启用 |
| `orderDate` | string | 必填 |
| `status` | string | 动作接口控制 |
| `totalAmount` | decimal | 后端计算 |
| `paidAmount` | decimal | 默认 0 |
| `remark` | string | 最长 500 |

### 明细

`productId`、`productCode`、`productName`、`specification`、`unitId`、`unitName`、`quantity`、`unitCost`、`amount`、`remark`。

- 数量大于 0。
- 采购单价不得小于 0。
- 金额由后端 BigDecimal 计算。
- 保存商品快照。

## 4. 后端目录

```text
purchase/
├─ controller/PurchaseOrderController.java
├─ dto/PurchaseOrderCreateRequest.java
├─ dto/PurchaseOrderUpdateRequest.java
├─ dto/PurchaseOrderQueryRequest.java
├─ dto/PurchaseOrderItemRequest.java
├─ vo/PurchaseOrderVO.java
├─ vo/PurchaseOrderDetailVO.java
├─ entity/PurOrder.java
├─ entity/PurOrderItem.java
├─ mapper/
└─ service/

finance/payment/
├─ controller/PaymentController.java
├─ dto/PaymentCreateRequest.java
├─ vo/PaymentVO.java
└─ service/

report/purchase/
├─ controller/PurchaseReportController.java
└─ service/PurchaseReportService.java
```

## 5. 采购单接口文档

### 5.1 接口清单

| 方法 | 地址 | 功能 | 允许状态 |
| --- | --- | --- | --- |
| GET | `/api/v1/purchase-orders` | 分页查询 | 全部 |
| GET | `/api/v1/purchase-orders/{id}` | 详情 | 全部 |
| POST | `/api/v1/purchase-orders` | 新增 | 创建 DRAFT |
| PUT | `/api/v1/purchase-orders/{id}` | 修改 | DRAFT |
| DELETE | `/api/v1/purchase-orders/{id}` | 删除 | DRAFT |
| POST | `/api/v1/purchase-orders/{id}/approve` | 审核 | DRAFT |
| POST | `/api/v1/purchase-orders/{id}/cancel` | 取消 | DRAFT/APPROVED |
| POST | `/api/v1/purchase-orders/{id}/complete` | 入库完成回调 | APPROVED |
| GET | `/api/v1/purchase-orders/inbound-options` | 待入库单据 | APPROVED |

列表查询：`orderNo`、`supplierId`、`warehouseId`、`status`、`startDate`、`endDate`、`page`、`size`。

### 5.2 新增采购单

```json
{
  "supplierId": "1",
  "warehouseId": "1",
  "orderDate": "2026-07-17",
  "remark": "课程演示采购单",
  "items": [
    {
      "productId": "1",
      "quantity": 100,
      "unitCost": 18.50,
      "remark": ""
    }
  ]
}
```

响应：

```json
{
  "id": "2001",
  "orderNo": "PO202607170001",
  "status": "DRAFT",
  "totalAmount": 1850.00
}
```

后端顺序：校验供应商、仓库、商品 → 读取快照 → 计算金额 → 生成单号 → 保存主表和明细。

### 5.3 详情响应

```json
{
  "id": "2001",
  "orderNo": "PO202607170001",
  "supplierId": "1",
  "supplierName": "华东办公用品供应商",
  "warehouseId": "1",
  "warehouseName": "上海一号仓",
  "orderDate": "2026-07-17",
  "status": "APPROVED",
  "totalAmount": 1850.00,
  "paidAmount": 500.00,
  "unpaidAmount": 1350.00,
  "items": [
    {
      "productId": "1",
      "productCode": "P0001",
      "productName": "A4 复印纸",
      "specification": "70g 500张/包",
      "unitName": "包",
      "quantity": 100,
      "unitCost": 18.50,
      "amount": 1850.00
    }
  ]
}
```

### 5.4 审核与取消

审核：`POST /api/v1/purchase-orders/{id}/approve`。

只有 DRAFT 可审核；审核时再次校验供应商、仓库和商品状态。

取消：`POST /api/v1/purchase-orders/{id}/cancel`。

```json
{
  "reason": "采购计划取消"
}
```

已存在确认入库单时禁止取消。

### 5.5 待入库选项

`GET /api/v1/purchase-orders/inbound-options?keyword=PO2026`

```json
[
  {
    "value": "2001",
    "label": "PO202607170001 - 华东办公用品供应商",
    "orderNo": "PO202607170001",
    "supplierName": "华东办公用品供应商",
    "warehouseId": "1",
    "warehouseName": "上海一号仓",
    "totalAmount": 1850.00
  }
]
```

## 6. 付款接口文档

| 方法 | 地址 | 功能 |
| --- | --- | --- |
| GET | `/api/v1/payments` | 付款单分页 |
| GET | `/api/v1/payments/{id}` | 付款详情 |
| POST | `/api/v1/payments` | 新增并确认付款 |
| POST | `/api/v1/payments/{id}/cancel` | 取消，选做 |
| GET | `/api/v1/purchase-orders/payable-options` | 有未付款的采购单 |

新增请求：

```json
{
  "supplierId": "1",
  "purchaseOrderId": "2001",
  "paymentDate": "2026-07-17",
  "amount": 500.00,
  "paymentMethod": "BANK",
  "accountId": "1",
  "remark": "首付款"
}
```

规则：

- 金额大于 0。
- 供应商必须和采购单一致。
- 累计付款不得大于采购单总额。
- 更新采购单 `paidAmount`。
- 如启用 `fin_capital_flow`，同步写 OUT 流水。

## 7. 采购报表接口

| 方法 | 地址 | 功能 |
| --- | --- | --- |
| GET | `/api/v1/reports/purchase-details` | 采购明细分页 |
| GET | `/api/v1/reports/purchase-summary` | 采购汇总 |

筛选：`startDate`、`endDate`、`supplierId`、`productId`、`status`、`page`、`size`。

汇总响应：

```json
{
  "orderCount": 10,
  "purchaseAmount": 12800.00,
  "paidAmount": 8000.00,
  "unpaidAmount": 4800.00,
  "purchaseQuantity": 520.0000
}
```

只统计 APPROVED 和 COMPLETED。

## 8. 前端页面

```text
views/purchase/
├─ orders/index.vue
├─ orders/components/OrderForm.vue
├─ orders/detail.vue
├─ payments/index.vue
└─ report/index.vue
```

### 采购单列表

- 筛选：单号、供应商、仓库、状态、日期范围。
- 列：单号、日期、供应商、仓库、总额、已付、未付、状态、创建时间。
- 状态决定操作按钮。

### 采购单表单

- 供应商、仓库、日期和备注。
- 商品明细动态增删。
- 选择商品带出默认采购价、单位、规格。
- 自动计算行金额和总金额。
- 至少一条明细才能提交。

### 付款页面

- 列表筛选：付款单号、供应商、日期。
- 新增选择供应商和未结采购单。
- 显示总额、已付、未付。
- 金额超限前端提示，后端兜底。

### 采购报表

- 顶部四个汇总数据。
- 明细表格支持日期、供应商筛选。
- 图表选做。

## 9. 七天安排

| 日期 | 必须完成 |
| --- | --- |
| 第 1 天 | 检查采购表；建立模块、CRUD、前端路由和空页面 |
| 第 2 天 | 采购新增、明细计算、列表、详情；可先模拟下拉 |
| 第 3 天 | 接入供应商、商品、仓库 options；审核、取消、详情 |
| 第 4 天 | 付款、采购报表、待入库接口，交给成员 5 |
| 第 5 天 | 联调采购审核、生成入库、入库完成；合并代码 |
| 第 6 天 | 修复状态、金额和重复提交；准备采购和付款演示数据 |
| 第 7 天 | 页面整理、报表检查、完整演示排练 |

## 10. 本人代码规范

- 主表明细保存同一事务。
- 已审核单据不允许通用更新。
- 状态只由专用动作接口改变。
- 金额、数量使用 BigDecimal。
- 后端重新计算金额。
- 保存商品快照。
- 单号由后端生成，不能由前端指定。
- 报表 SQL 放 report 模块。
- 前端使用 `<script setup lang="ts">`。
- 不修改成员 2 的资料表或成员 5 的库存表。
- 与成员 3 同用 finance 时，你只修改 payment 子包，避免冲突。

## 11. Git 规范

- 分支 `feature/purchase`。
- 建议提交：
  - `feat(purchase): add purchase order crud`
  - `feat(purchase): add approval workflow`
  - `feat(finance): add supplier payment`
  - `feat(report): add purchase detail report`
  - `fix(purchase): prevent overpayment`
- 不提交本地配置和密码。
- 不格式化销售、库存和公共模块。

## 12. 联调依赖

依赖成员 2：供应商、商品、仓库 options。

向成员 5 提供：

- `GET /api/v1/purchase-orders/inbound-options`。
- `GET /purchase-orders/{id}` 完整明细。
- `POST /api/v1/purchase-orders/{id}/complete`。

最晚第 4 天上午交付。

## 13. 测试清单

- 无明细不能创建采购单。
- 数量和单价校验有效。
- 后端金额计算正确。
- 草稿可编辑删除，已审核不可编辑删除。
- 重复审核返回 409。
- 已审核单出现在待入库列表。
- 入库后状态为 COMPLETED。
- 付款超过未付金额时报错。
- 付款后已付、未付正确。
- 报表排除取消和草稿。
- 前端构建通过。

## 14. 完成定义

- 采购单页面和接口完整可演示。
- 成员 5 能根据你的采购单完成入库。
- 付款能正确更新已付金额。
- 报表使用真实采购数据。
- 演示数据、代码、SQL 和接口说明已合并。
