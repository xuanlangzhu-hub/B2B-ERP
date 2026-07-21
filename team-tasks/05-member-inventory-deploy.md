# 成员 5 任务书：采购入库、销售出库、库存、首页与部署

> 分支：`feature/inventory`  
> 后端模块：`inventory`、`dashboard`、`report/inventory`  
> 前端目录：`views/inventory`、`views/dashboard`、`api/inventory`  
> 最终责任：库存数据正确、主业务链闭环、系统能在服务器演示

## 1. 本人交付目标

必须完成：

- 根据已审核采购单生成采购入库单。
- 确认入库并增加库存余额、写库存流水、完成采购单。
- 根据已审核销售单生成销售出库单。
- 确认出库并校验/扣减库存、写库存流水、完成销售单。
- 入库单、出库单列表和详情。
- 库存余额查询和低库存提示。
- 库存流水查询。
- 首页核心统计卡片。
- 库存基础报表。
- 演示数据、后端打包、前端构建、服务器部署和部署说明。

本周不做：

- 分批入库、分批出库。
- 批次、序列号和保质期。
- 库存锁定。
- 盘点、调拨、调整、借入、借出完整业务。
- 多仓并发高压场景。
- Redis、消息队列和微服务。
- 自动化运维平台。

## 2. 数据库负责范围

沿用已有表：

| 表 | 用途 |
| --- | --- |
| `inv_inbound` | 入库单主表 |
| `inv_inbound_item` | 入库明细 |
| `inv_outbound` | 出库单主表 |
| `inv_outbound_item` | 出库明细 |
| `inv_stock_balance` | 仓库商品当前库存 |
| `inv_stock_movement` | 每次库存变化流水 |
| `fin_capital_flow` | 如需首页资金统计，可只读 |

不要修改销售、采购主表结构；通过成员 3、4 提供的 Service 或接口完成状态更新。

## 3. 库存核心规则

### 3.1 库存余额唯一性

`inv_stock_balance` 必须对 `(warehouse_id, product_id)` 建唯一索引。一仓一商品只能有一条余额记录。

### 3.2 入库事务

确认入库必须在一个数据库事务中：

1. 查询入库单并校验当前状态不是 CONFIRMED。
2. 校验来源采购单为 APPROVED。
3. 遍历入库明细。
4. 查询或新建对应库存余额。
5. 记录 `beforeQuantity`。
6. 增加库存余额。
7. 写入一条 `PURCHASE_IN` 库存流水。
8. 将入库单改为 CONFIRMED。
9. 将采购单改为 COMPLETED。
10. 事务提交。

任一步失败必须全部回滚。

### 3.3 出库事务

确认出库必须在一个事务中：

1. 查询出库单并校验未确认。
2. 校验来源销售单为 APPROVED。
3. 查询所有商品库存。
4. 任何一项库存不足都直接失败，不先扣其他商品。
5. 记录变化前库存。
6. 扣减库存余额。
7. 写 `SALES_OUT` 库存流水，变化数量保存负数。
8. 将出库单改为 CONFIRMED。
9. 将销售单改为 COMPLETED。
10. 事务提交。

### 3.4 防重复

- 已确认单据再次确认返回 HTTP 409、业务码 `BUSINESS_CONFLICT`。
- 确认按钮点击后立即 loading 和禁用。
- 不允许靠前端防重复，后端必须检查状态。
- 如果时间允许，可对来源单据建立唯一索引，保证一张采购/销售单只生成一张有效出入库单。

## 4. 字段约定

### 入库单

| 字段 | 规则 |
| --- | --- |
| `inboundNo` | 后端生成，如 `IN202607170001` |
| `sourceType` | `PURCHASE` 或 `OTHER`，本周只用 PURCHASE |
| `sourceOrderId` | 采购单 ID |
| `sourceOrderNo` | 采购单号快照 |
| `warehouseId` | 来源采购单仓库 |
| `businessDate` | 入库日期 |
| `status` | DRAFT、CONFIRMED、CANCELLED |
| `remark` | 备注 |

入库明细：商品 ID、商品编码、名称、规格、单位、数量、单位成本、金额。

### 出库单

主表结构对应入库单，`sourceType=SALES`；明细保存数量、销售单价、金额。

### 库存余额

`warehouseId`、`productId`、`quantity`、`lockedQuantity`、`availableQuantity`、`lastInboundAt`、`lastOutboundAt`。

本周如果不实现锁定，`lockedQuantity` 固定为 0，`availableQuantity = quantity`。

### 库存流水

`movementNo`、`warehouseId`、`productId`、`movementType`、`sourceOrderNo`、`beforeQuantity`、`changeQuantity`、`afterQuantity`、`businessDate`、`operatorId`。

## 5. 后端目录

```text
inventory/
├─ controller/InboundController.java
├─ controller/OutboundController.java
├─ controller/StockController.java
├─ controller/StockMovementController.java
├─ dto/InboundCreateRequest.java
├─ dto/OutboundCreateRequest.java
├─ dto/StockQueryRequest.java
├─ dto/MovementQueryRequest.java
├─ vo/InboundVO.java
├─ vo/InboundDetailVO.java
├─ vo/OutboundVO.java
├─ vo/OutboundDetailVO.java
├─ vo/StockBalanceVO.java
├─ vo/StockMovementVO.java
├─ entity/
├─ mapper/
└─ service/

dashboard/
├─ controller/DashboardController.java
├─ vo/DashboardSummaryVO.java
└─ service/DashboardService.java

report/inventory/
├─ controller/InventoryReportController.java
└─ service/InventoryReportService.java
```

## 6. 入库接口文档

### 6.1 接口清单

| 方法 | 地址 | 功能 |
| --- | --- | --- |
| GET | `/api/v1/inbounds` | 入库单分页 |
| GET | `/api/v1/inbounds/{id}` | 入库单详情 |
| POST | `/api/v1/inbounds/from-purchase/{purchaseOrderId}` | 根据采购单生成草稿入库单 |
| POST | `/api/v1/inbounds/{id}/confirm` | 确认入库 |
| POST | `/api/v1/inbounds/{id}/cancel` | 取消草稿，选做 |

分页参数：`inboundNo`、`sourceOrderNo`、`warehouseId`、`status`、`startDate`、`endDate`、`page`、`size`。

### 6.2 根据采购单生成入库单

`POST /api/v1/inbounds/from-purchase/2001`

请求：

```json
{
  "businessDate": "2026-07-17",
  "remark": "采购到货"
}
```

响应：

```json
{
  "id": "3001",
  "inboundNo": "IN202607170001",
  "sourceOrderId": "2001",
  "sourceOrderNo": "PO202607170001",
  "warehouseId": "1",
  "warehouseName": "上海一号仓",
  "status": "DRAFT",
  "items": [
    {
      "productId": "1",
      "productCode": "P0001",
      "productName": "A4 复印纸",
      "unitName": "包",
      "quantity": 100,
      "unitCost": 18.50,
      "amount": 1850.00
    }
  ]
}
```

规则：采购单必须 APPROVED；同一采购单不能重复生成有效入库单；商品和数量从采购单明细复制，不接受前端任意修改。

### 6.3 确认入库

`POST /api/v1/inbounds/{id}/confirm`

成功响应：

```json
{
  "inboundId": "3001",
  "inboundNo": "IN202607170001",
  "status": "CONFIRMED",
  "affectedProductCount": 1,
  "totalQuantity": 100
}
```

## 7. 出库接口文档

| 方法 | 地址 | 功能 |
| --- | --- | --- |
| GET | `/api/v1/outbounds` | 出库单分页 |
| GET | `/api/v1/outbounds/{id}` | 出库详情 |
| POST | `/api/v1/outbounds/from-sales/{salesOrderId}` | 根据销售单生成草稿出库单 |
| POST | `/api/v1/outbounds/{id}/confirm` | 确认出库 |
| POST | `/api/v1/outbounds/{id}/cancel` | 取消草稿，选做 |

### 7.1 根据销售单生成出库单

```json
{
  "businessDate": "2026-07-17",
  "remark": "销售发货"
}
```

返回中每条明细应附带 `currentStock`，前端用于提示。

### 7.2 确认出库

库存不足响应示例：

```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "商品 P0001 - A4 复印纸库存不足，当前 5，需要 10",
  "data": {
    "productId": "1",
    "availableQuantity": 5,
    "requiredQuantity": 10
  }
}
```

确认成功后销售单状态必须为 COMPLETED。

## 8. 库存余额接口

| 方法 | 地址 | 功能 |
| --- | --- | --- |
| GET | `/api/v1/stocks` | 库存分页 |
| GET | `/api/v1/stocks/{warehouseId}/{productId}` | 单商品库存 |
| GET | `/api/v1/stocks/check` | 批量检查库存，选做 |

查询参数：`warehouseId`、`productCode`、`productName`、`categoryId`、`lowStockOnly`、`page`、`size`。

响应记录：

```json
{
  "warehouseId": "1",
  "warehouseName": "上海一号仓",
  "productId": "1",
  "productCode": "P0001",
  "productName": "A4 复印纸",
  "specification": "70g 500张/包",
  "unitName": "包",
  "quantity": 90,
  "lockedQuantity": 0,
  "availableQuantity": 90,
  "minStock": 20,
  "lowStock": false
}
```

禁止提供直接修改库存数量的通用 PUT 接口。

## 9. 库存流水接口

`GET /api/v1/stock-movements`

参数：`warehouseId`、`productId`、`movementType`、`sourceOrderNo`、`startDate`、`endDate`、`page`、`size`。

响应记录：

```json
{
  "movementNo": "SM202607170001",
  "warehouseName": "上海一号仓",
  "productCode": "P0001",
  "productName": "A4 复印纸",
  "movementType": "PURCHASE_IN",
  "movementTypeName": "采购入库",
  "sourceOrderNo": "PO202607170001",
  "beforeQuantity": 0,
  "changeQuantity": 100,
  "afterQuantity": 100,
  "businessDate": "2026-07-17",
  "createdAt": "2026-07-17 12:00:00"
}
```

流水不可修改、不可删除。

## 10. 首页和库存报表接口

### 首页统计

`GET /api/v1/dashboard/summary`

```json
{
  "productCount": 15,
  "customerCount": 5,
  "supplierCount": 5,
  "salesOrderCount": 8,
  "salesAmount": 15800.00,
  "purchaseOrderCount": 6,
  "purchaseAmount": 12800.00,
  "lowStockCount": 2,
  "pendingInboundCount": 1,
  "pendingOutboundCount": 1
}
```

可选接口：

- `GET /api/v1/dashboard/sales-trend?days=7`。
- `GET /api/v1/dashboard/low-stock?limit=5`。

### 库存报表

`GET /api/v1/reports/inventory`

参数与库存查询相同，可额外返回库存总数量、预警数量和仓库数量。

## 11. 前端页面

```text
views/
├─ dashboard/index.vue
└─ inventory/
   ├─ inbounds/index.vue
   ├─ inbounds/detail.vue
   ├─ outbounds/index.vue
   ├─ outbounds/detail.vue
   ├─ stocks/index.vue
   ├─ movements/index.vue
   └─ report/index.vue
```

### 入库页面

- 查询单号、采购单号、仓库、状态、日期。
- “采购入库”按钮打开待入库采购单选择弹窗。
- 生成入库单后展示明细。
- 确认入库二次确认并显示 loading。
- 已确认单据只读。

### 出库页面

- 选择待出库销售单。
- 明细显示出库数量和当前库存。
- 库存不足行红色标记。
- 确认前提示“确认后将扣减库存且不可直接撤销”。

### 库存查询

- 筛选仓库、商品、低库存。
- 低库存用红色 Tag。
- 数量最多显示四位小数。
- 不提供编辑库存按钮。

### 库存流水

- 入库变化量绿色，出库红色。
- 类型使用中文标签。
- 来源单号可点击查看对应出入库详情，时间不足可不跳转。

### 首页

- 至少 6 个统计卡片。
- 显示低库存前 5 条。
- 一个 ECharts 趋势图选做。

## 12. 部署要求

### 后端配置

通过环境变量读取：

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
JWT_SECRET
SERVER_PORT
```

仓库只能提交示例配置，不写真实值。

### 构建

```text
后端：mvn clean package
前端：npm run build
```

### Nginx 目标

- 前端静态资源由 Nginx 提供。
- `/api/` 代理到 Spring Boot。
- Vue history 路由配置 `try_files` 回退到 `index.html`。

### 部署文档必须记录

- Java 和 Node 版本。
- 环境变量名称。
- 数据库脚本执行顺序。
- 后端启动和停止方式。
- Nginx 配置位置。
- 日志位置。
- 演示访问地址。
- 数据库备份和恢复方法。

## 13. 七天安排

| 日期 | 必须完成 |
| --- | --- |
| 第 1 天 | 检查库存表和索引；建立模块、查询接口和前端路由 |
| 第 2 天 | 库存余额、流水查询；完成入库事务主体 |
| 第 3 天 | 联调成员 4，完成采购入库和库存增加 |
| 第 4 天 | 联调成员 3，完成销售出库、库存校验和扣减；完成库存页面 |
| 第 5 天 | 首页统计、库存报表、全量合并；准备部署配置 |
| 第 6 天 | 部署测试版、准备演示数据、修复事务和重复确认问题 |
| 第 7 天 | 部署最终版、备份数据库、输出部署说明、完整演练 |

## 14. 本人代码规范

- 确认入库、出库方法必须 `@Transactional`。
- 事务方法必须由 Spring 代理调用，避免同类内部调用导致事务失效。
- 库存不足先全部校验，再开始扣减。
- 所有数量使用 BigDecimal。
- 更新库存时校验影响行数。
- 余额表唯一索引必须存在。
- 流水只新增，不修改删除。
- 不提供直接改库存接口。
- 不在 Controller 写事务逻辑。
- 不吞异常，不在 catch 中继续提交。
- 确认操作根据当前状态保证幂等或明确拒绝重复。
- 前端确认按钮 loading，成功后重新查询详情和库存。
- 部署脚本和配置不得包含真实密码。
- 不修改销售、采购的通用页面和表结构。

## 15. Git 规范

- 分支 `feature/inventory`。
- 建议提交：
  - `feat(inventory): add stock balance query`
  - `feat(inventory): implement purchase inbound transaction`
  - `feat(inventory): implement sales outbound transaction`
  - `feat(dashboard): add summary statistics`
  - `fix(inventory): prevent duplicate confirmation`
  - `docs(deploy): add deployment guide`
- 库存事务每完成一条链路立即提交。
- 不提交服务器密钥、数据库密码、日志和构建产物。

## 16. 联调依赖

依赖成员 2：商品和仓库 options。

依赖成员 3：

- 待出库销售单接口。
- 销售单详情。
- 销售完成接口或 Service。

依赖成员 4：

- 待入库采购单接口。
- 采购单详情。
- 采购完成接口或 Service。

为了避免后端内部 HTTP 调用，优先在同一个 Spring Boot 工程中注入对方 Service；如果合并前独立联调，可先调用 REST 接口。

## 17. 测试清单

- 已审核采购单生成入库单。
- 未审核采购单不能生成入库单。
- 同一采购单不能重复生成有效入库单。
- 确认入库增加正确数量。
- 入库产生正确 before/change/after 流水。
- 入库后采购单完成。
- 已审核销售单生成出库单。
- 库存不足时整张出库失败，任何商品都不扣。
- 出库后库存正确减少。
- 出库产生负数变化流水。
- 出库后销售单完成。
- 重复确认不重复变库存。
- 首页统计与业务数据基本一致。
- 后端打包成功、前端构建成功。
- 部署后刷新前端路由正常。
- 服务器重启后系统可恢复。

## 18. 完成定义

- 采购入库和销售出库两条事务链完整。
- 库存余额、流水真实可查。
- 首页能展示真实数据。
- 库存不足和重复提交得到正确处理。
- 系统部署可访问。
- 演示数据、备份和部署文档齐全。
- 真实凭据未进入 Git。

