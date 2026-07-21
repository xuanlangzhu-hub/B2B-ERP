# 成员 3 任务书：销售单、客户收款与销售报表

> 分支：`feature/sales`  
> 后端模块：`sales`、`finance/receipt`、`report/sales`  
> 前端目录：`views/sales`、`api/sales`  
> 最终责任：完成“销售单审核 → 销售出库 → 客户收款 → 销售报表”链路中的销售侧

## 1. 本人交付目标

必须完成：

- 销售单列表、详情、新增、修改、删除。
- 销售单商品明细。
- 销售单审核、取消和出库后完成。
- 给成员 5 提供待出库销售单接口。
- 客户收款单列表和新增。
- 销售单已收、未收金额展示。
- 销售明细报表和销售汇总卡片。
- 至少 3 张已完成、1 张待出库销售单及 2 张收款单演示数据。

本周不做：

- 分批出库、库存预占。
- 报价单、合同、促销和复杂折扣。
- 发票、物流、打印。
- 销售退货完整闭环。
- 一张收款单核销多张销售单的复杂页面。
- 反审核和红冲。

## 2. 数据库负责范围

沿用已有表：

| 表 | 用途 |
| --- | --- |
| `sal_order` | 销售单主表 |
| `sal_order_item` | 销售单明细 |
| `fin_receipt` | 收款单 |
| `fin_receipt_item` | 有时间再实现核销明细 |
| `fin_receivable` | 有时间再实现正式应收；本周可由销售单计算 |

不要修改 `inv_*` 表；真实库存变化由成员 5 负责。

### 销售单状态

```text
DRAFT → APPROVED → COMPLETED
  └──────────────→ CANCELLED
```

- 草稿：可编辑、删除、审核。
- 已审核：不可编辑，等待出库，可取消。
- 已完成：已出库，只读。
- 已取消：只读。
- 一周项目不处理 `PARTIALLY_OUTBOUND`；如果数据库已有该状态，不在页面中使用。

## 3. 字段约定

### 销售单主表字段

| 前端字段 | 类型 | 规则 |
| --- | --- | --- |
| `orderNo` | string | 后端生成、唯一 |
| `customerId` | string | 必填、客户必须启用 |
| `warehouseId` | string | 必填、仓库必须启用 |
| `orderDate` | string | 必填，`yyyy-MM-dd` |
| `status` | string | 后端控制 |
| `totalAmount` | decimal | 后端根据明细计算 |
| `receivedAmount` | decimal | 默认 0 |
| `remark` | string | 最长 500 |

### 明细字段

`productId`、`productCode`、`productName`、`specification`、`unitId`、`unitName`、`quantity`、`unitPrice`、`amount`、`remark`。

- `quantity > 0`。
- `unitPrice >= 0`。
- `amount = quantity × unitPrice`，后端用 BigDecimal 重新计算。
- 同一商品在一张单中可禁止重复；前端选择时提示。
- 保存商品快照字段，后续商品改名不影响历史单据。

## 4. 后端目录

```text
sales/
├─ controller/SalesOrderController.java
├─ dto/SalesOrderCreateRequest.java
├─ dto/SalesOrderUpdateRequest.java
├─ dto/SalesOrderQueryRequest.java
├─ dto/SalesOrderItemRequest.java
├─ vo/SalesOrderVO.java
├─ vo/SalesOrderDetailVO.java
├─ entity/SalOrder.java
├─ entity/SalOrderItem.java
├─ mapper/
└─ service/

finance/receipt/
├─ controller/ReceiptController.java
├─ dto/ReceiptCreateRequest.java
├─ vo/ReceiptVO.java
└─ service/

report/sales/
├─ controller/SalesReportController.java
└─ service/SalesReportService.java
```

关键写操作使用 `@Transactional`。

## 5. 公共接口约定

- 前缀 `/api/v1`。
- 返回体、分页、异常由成员 1 提供。
- 主键 JSON 使用字符串。
- 金额 `BigDecimal` / `DECIMAL(18,2)`。
- 数量 `BigDecimal` / `DECIMAL(18,4)`。
- 后端不接收前端任意传入的状态值，状态通过动作接口修改。
- 写接口必须鉴权。

## 6. 销售单接口文档

### 6.1 接口清单

| 方法 | 地址 | 功能 | 允许状态 |
| --- | --- | --- | --- |
| GET | `/api/v1/sales-orders` | 分页查询 | 全部 |
| GET | `/api/v1/sales-orders/{id}` | 详情 | 全部 |
| POST | `/api/v1/sales-orders` | 新增 | 创建 DRAFT |
| PUT | `/api/v1/sales-orders/{id}` | 修改 | DRAFT |
| DELETE | `/api/v1/sales-orders/{id}` | 删除 | DRAFT |
| POST | `/api/v1/sales-orders/{id}/approve` | 审核 | DRAFT |
| POST | `/api/v1/sales-orders/{id}/cancel` | 取消 | DRAFT/APPROVED |
| POST | `/api/v1/sales-orders/{id}/complete` | 出库完成回调 | APPROVED |
| GET | `/api/v1/sales-orders/outbound-options` | 待出库单据 | APPROVED |

列表查询参数：`orderNo`、`customerId`、`warehouseId`、`status`、`startDate`、`endDate`、`page`、`size`。

### 6.2 新增销售单

`POST /api/v1/sales-orders`

```json
{
  "customerId": "1",
  "warehouseId": "1",
  "orderDate": "2026-07-17",
  "remark": "课程演示销售单",
  "items": [
    {
      "productId": "1",
      "quantity": 10,
      "unitPrice": 23.00,
      "remark": ""
    },
    {
      "productId": "2",
      "quantity": 5,
      "unitPrice": 12.50,
      "remark": ""
    }
  ]
}
```

响应 `data`：

```json
{
  "id": "1001",
  "orderNo": "SO202607170001",
  "status": "DRAFT",
  "totalAmount": 292.50
}
```

后端处理顺序：

1. 校验客户、仓库、商品存在且启用。
2. 校验至少一条明细。
3. 读取商品快照字段。
4. 计算行金额和总金额。
5. 生成唯一销售单号。
6. 保存主表和明细。

### 6.3 销售单详情响应

```json
{
  "id": "1001",
  "orderNo": "SO202607170001",
  "customerId": "1",
  "customerName": "上海示例商贸有限公司",
  "warehouseId": "1",
  "warehouseName": "上海一号仓",
  "orderDate": "2026-07-17",
  "status": "APPROVED",
  "totalAmount": 292.50,
  "receivedAmount": 100.00,
  "unreceivedAmount": 192.50,
  "remark": "课程演示销售单",
  "items": [
    {
      "id": "1",
      "productId": "1",
      "productCode": "P0001",
      "productName": "A4 复印纸",
      "specification": "70g 500张/包",
      "unitName": "包",
      "quantity": 10,
      "unitPrice": 23.00,
      "amount": 230.00
    }
  ]
}
```

### 6.4 审核和取消

审核：`POST /api/v1/sales-orders/{id}/approve`

请求可为空，或携带：

```json
{
  "remark": "审核通过"
}
```

规则：只有 DRAFT 可审核；明细不能为空；审核时再次检查客户、仓库、商品状态。

取消：`POST /api/v1/sales-orders/{id}/cancel`

```json
{
  "reason": "客户取消采购"
}
```

已产生已确认出库单时禁止取消。

### 6.5 待出库选项

`GET /api/v1/sales-orders/outbound-options?keyword=SO2026`

响应 `data`：

```json
[
  {
    "value": "1001",
    "label": "SO202607170001 - 上海示例商贸有限公司",
    "orderNo": "SO202607170001",
    "customerName": "上海示例商贸有限公司",
    "warehouseId": "1",
    "warehouseName": "上海一号仓",
    "totalAmount": 292.50
  }
]
```

## 7. 收款接口文档

### 7.1 接口清单

| 方法 | 地址 | 功能 |
| --- | --- | --- |
| GET | `/api/v1/receipts` | 收款单分页 |
| GET | `/api/v1/receipts/{id}` | 收款详情 |
| POST | `/api/v1/receipts` | 新增并确认收款 |
| POST | `/api/v1/receipts/{id}/cancel` | 取消，时间允许再做 |
| GET | `/api/v1/sales-orders/receivable-options` | 有未收款的销售单 |

新增请求：

```json
{
  "customerId": "1",
  "salesOrderId": "1001",
  "receiptDate": "2026-07-17",
  "amount": 100.00,
  "paymentMethod": "BANK",
  "accountId": "1",
  "remark": "银行转账"
}
```

规则：

- 金额必须大于 0。
- 关联销售单时，客户必须一致。
- 本周简化为累计收款不得大于销售单总金额。
- 确认收款后更新销售单 `receivedAmount`。
- 如果现有 SQL 已启用 `fin_capital_flow`，同步写入一条 IN 流水；资金账户细节可与成员 5 协调。

## 8. 销售报表接口

| 方法 | 地址 | 功能 |
| --- | --- | --- |
| GET | `/api/v1/reports/sales-details` | 销售明细分页 |
| GET | `/api/v1/reports/sales-summary` | 销售汇总 |

明细筛选：`startDate`、`endDate`、`customerId`、`productId`、`status`、`page`、`size`。

汇总响应：

```json
{
  "orderCount": 12,
  "salesAmount": 15800.00,
  "receivedAmount": 10200.00,
  "unreceivedAmount": 5600.00,
  "salesQuantity": 358.0000
}
```

报表只统计 `APPROVED` 和 `COMPLETED`，排除草稿和已取消。

## 9. 前端页面

```text
views/sales/
├─ orders/index.vue
├─ orders/components/OrderForm.vue
├─ orders/detail.vue
├─ receipts/index.vue
└─ report/index.vue
```

### 销售单列表

- 筛选：单号、客户、仓库、状态、日期范围。
- 表格：单号、日期、客户、仓库、总额、已收、未收、状态、创建人、创建时间。
- 操作按状态显示：查看、编辑、审核、取消、删除。
- 审核、取消、删除均二次确认。

### 销售单表单

- 客户、仓库、日期、备注。
- 明细表格支持添加行和删除行。
- 商品选择使用成员 2 的 options。
- 选择商品带出规格、单位、销售价。
- 数量和单价改变时实时计算行金额和总额。
- 防止选择重复商品。
- 提交按钮有 loading。

### 收款页面

- 列表筛选：收款单号、客户、日期。
- 新增时选择客户和未结销售单。
- 显示销售总额、已收、未收。
- 收款金额超过未收金额时前端提示，后端仍必须校验。

### 销售报表

- 顶部四个汇总数字。
- 下方明细表格。
- 至少支持日期和客户筛选。
- ECharts 可选，不作为完成条件。

## 10. 七天安排

| 日期 | 必须完成 |
| --- | --- |
| 第 1 天 | 检查销售表结构；建立模块和 CRUD；创建前端路由与空页面 |
| 第 2 天 | 销售单新增、明细计算、列表、详情；下拉可先模拟 |
| 第 3 天 | 接入客户、商品、仓库 options；完成审核、取消、详情页 |
| 第 4 天 | 收款单、销售报表、待出库接口，交给成员 5 |
| 第 5 天 | 联调销售审核、生成出库、出库完成；合并代码 |
| 第 6 天 | 修复金额、状态、重复提交；准备演示销售和收款数据 |
| 第 7 天 | 页面整理、报表检查、参与完整演示排练 |

## 11. 本人代码规范

- 订单保存和明细保存必须同一事务。
- 修改订单先校验 DRAFT，再替换明细；不得修改已审核订单。
- 审核、取消、完成分别使用独立 Service 方法。
- 不允许通用 update 接口直接修改 status。
- 金额和数量只使用 BigDecimal。
- 比较 BigDecimal 使用 `compareTo`，不使用 `equals` 比较数值大小。
- 请求 DTO 不接收商品名称和总金额作为可信数据。
- 历史单据保存商品快照。
- 报表查询放 report 模块，不把复杂 SQL 写 Controller。
- 前端表单和详情建立明确 TypeScript 类型。
- 不修改成员 2 的资料表和成员 5 的库存表。
- 不修改全局菜单，由成员 1 统一合并。

## 12. Git 规范

- 分支 `feature/sales`。
- 建议提交：
  - `feat(sales): add sales order crud`
  - `feat(sales): add order approval workflow`
  - `feat(finance): add customer receipt`
  - `feat(report): add sales detail report`
  - `fix(sales): prevent editing approved order`
- 每完成一个可联调接口立即提交。
- 不提交数据库密码和本地配置。
- 不格式化采购、库存和公共模块。

## 13. 联调依赖

你依赖成员 2：客户、商品、仓库 options，第 2 天中午交付。

你向成员 5 提供：

- `GET /api/v1/sales-orders/outbound-options`。
- `GET /sales-orders/{id}` 完整商品明细。
- `POST /api/v1/sales-orders/{id}/complete`。
- 状态和值的约定。

最晚第 4 天上午完成并给成员 5 示例响应。

## 14. 测试清单

- 没有明细不能创建销售单。
- 数量为 0 或负数时报错。
- 后端金额与前端显示一致。
- 草稿可修改和删除。
- 已审核不能修改和删除。
- 重复审核返回 409，不产生额外数据。
- 已审核单出现在待出库列表。
- 出库完成后状态变 COMPLETED。
- 收款超过未收金额时报错。
- 收款后已收和未收金额正确。
- 销售报表排除已取消订单。
- 前端构建通过。

## 15. 完成定义

- 销售单全套页面和接口可演示。
- 成员 5 能使用你的销售单完成出库。
- 收款能更新销售单金额。
- 销售报表能显示真实订单。
- 演示数据已准备。
- 代码、SQL 和接口说明已合并。
