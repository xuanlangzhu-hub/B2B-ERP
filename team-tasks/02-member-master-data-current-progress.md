# 成员 2 当前进度参考版：商品、客户、供应商与仓库基础资料

> 本文件是 `02-member-master-data.md` 的当前进度副本，生成日期：2026-07-20。  
> 原任务书保留不动；本文件用于继续开发和验收，不代表代码已经全部完成。  
> 建议分支：`feature/masterdata`，提交前先同步当前 `main`。

## 1. 当前结论

- 按原任务书逐项严格验收，当前约完成 **68%**。
- 如果只看“能否支撑采购、销售、库存演示”的主功能，约完成 **80%**。
- 商品、客户、供应商、仓库四个主页面已经可以进行基础增删改查。
- 商品分类、计量单位已有后端 CRUD 和 options，但没有独立前端维护界面。
- options 已被销售、采购、库存、资金和报表模块真实调用，是当前完成度最高的部分。
- 当前主要缺口不是继续堆框架，而是补齐企业隔离、后端参数校验、分类/单位页面、options 字段和专项测试。

本次估算口径：

| 验收项 | 权重 | 当前得分 | 说明 |
| --- | ---: | ---: | --- |
| 六类后端 CRUD 与 options | 35 | 28 | 接口齐全，但企业隔离、DTO 和校验不足 |
| 六类前端维护页面 | 25 | 18 | 四个主页面完成，分类和单位页面缺失 |
| 跨模块联调与演示数据 | 20 | 18 | options 已广泛使用，商品数据还差 2 条 |
| 安全、规范和自动化测试 | 20 | 4 | 有重复编码校验和逻辑删除，但缺企业隔离与专项测试 |
| **合计** | **100** | **68** | 按原任务书严格验收 |

## 2. 逐项完成情况

| 模块 | 后端 | 前端 | options | 演示数据 | 当前评价 |
| --- | --- | --- | --- | --- | --- |
| 商品 | 基础 CRUD 已有 | 页面已完成 | 已有并被订单使用 | 8 条，目标 10 条 | 基本可用，仍需后端校验和企业隔离 |
| 商品分类 | 基础 CRUD 已有 | 缺少维护页面 | 已有 | 3 条 | 完成一半 |
| 计量单位 | 基础 CRUD 已有 | 缺少维护页面 | 已有 | 5 条 | 完成一半 |
| 客户 | 基础 CRUD 已有 | 页面已完成 | 已有并被销售、收款使用 | 5 条，已达标 | 基本可用，options 字段不足 |
| 供应商 | 基础 CRUD 已有 | 页面已完成 | 已有并被采购、付款使用 | 5 条，已达标 | 基本可用，options 字段不足 |
| 仓库 | 基础 CRUD 已有 | 页面已完成 | 已有并被库存、订单、报表使用 | 3 条，已超过目标 | 基本可用，仍需企业隔离 |

### 已经完成

- 六类资源的 Entity、Mapper、Service、Controller 框架。
- 六类资源的列表、详情、新增、修改、逻辑删除接口。
- 六类资源的启用数据 options 接口。
- 商品、客户、供应商、仓库的 Vue 页面。
- 商品、客户、供应商、仓库页面的分页、筛选、新增、编辑、删除确认和状态展示。
- 商品页面可以选择商品分类和计量单位。
- 商品、客户、供应商编码新增和修改时的重复校验。
- 分类和单位编码重复校验。
- 仓库编码重复校验。
- 分类存在子分类时禁止删除。
- V2 演示数据：8 个商品、5 个客户、5 个供应商、3 个仓库、3 个分类、5 个单位。
- 商品、客户、供应商、仓库 options 已接入采购、销售、库存、资金、退货和报表页面。
- 权限码及角色授权已在 `sql/V3__align_permissions.sql` 中对齐。

### 部分完成

- 状态维护：可以通过编辑弹窗修改 `status`，但没有原任务书约定的独立 `/status` 接口。
- 商品详情：后端详情接口存在，前端编辑直接使用列表行数据，没有重新请求详情。
- 页面校验：前端已有部分必填和数值校验，后端没有使用 DTO + `@Valid` 做可靠校验。
- 分类层级：数据库和后端支持 `parentId`，但前端还没有两级分类维护页面。
- options：可以联调，但返回字段没有完全达到原任务书约定。

### 尚未完成或需要修复

- 六类资源的列表、详情、修改、删除没有全部按当前登录人的 `enterpriseId` 隔离。
- Controller 直接接收并返回 Entity，尚未按任务书拆分 CreateRequest、UpdateRequest、VO。
- 新增和修改接口缺少 `@Valid`，必填、长度、金额非负等规则主要依赖前端和数据库。
- 商品分类、计量单位缺少前端维护入口。
- 商品演示数据还差 2 条。
- 商品 options 缺少 `specification`、`unitName` 等字段。
- 客户 options 缺少 `customerCode`、`customerName`、`creditLimit`。
- 供应商 options 缺少 `supplierCode`、`supplierName`。
- 仓库 options 缺少 `warehouseCode`、`warehouseName`。
- 没有基础资料模块专项自动化测试。
- 删除前没有完整检查商品、客户、供应商、仓库是否已被业务单据引用。

## 3. 当前真实目录

```text
erp-server/src/main/java/com/erp/masterdata/
├─ controller/
│  ├─ ProductController.java
│  ├─ ProductCategoryController.java
│  ├─ UnitController.java
│  ├─ CustomerController.java
│  └─ SupplierController.java
├─ entity/
├─ mapper/
└─ service/

erp-server/src/main/java/com/erp/system/
├─ controller/WarehouseController.java
├─ entity/OrgWarehouse.java
├─ mapper/OrgWarehouseMapper.java
└─ service/OrgWarehouseService.java

erp-web/src/views/master/
├─ product/index.vue
├─ customer/index.vue
├─ supplier/index.vue
└─ warehouse/index.vue

erp-web/src/api/masterdata.ts
```

说明：当前前端目录使用单数 `product/customer/supplier/warehouse`，不要再创建一套复数目录。

## 4. 当前接口基线

### 商品

| 方法 | 地址 | 当前状态 |
| --- | --- | --- |
| `GET` | `/api/v1/products` | 已有 |
| `GET` | `/api/v1/products/{id}` | 已有 |
| `POST` | `/api/v1/products` | 已有 |
| `PUT` | `/api/v1/products/{id}` | 已有 |
| `DELETE` | `/api/v1/products/{id}` | 已有 |
| `GET` | `/api/v1/products/options` | 已有，需要补字段 |

### 分类和单位

| 资源 | 列表 | 详情 | 新增 | 修改 | 删除 | options |
| --- | --- | --- | --- | --- | --- | --- |
| 分类 | `GET /product-categories` | `GET /product-categories/{id}` | `POST /product-categories` | `PUT /product-categories/{id}` | `DELETE /product-categories/{id}` | `GET /product-categories/options` |
| 单位 | `GET /units` | `GET /units/{id}` | `POST /units` | `PUT /units/{id}` | `DELETE /units/{id}` | `GET /units/options` |

### 客户、供应商和仓库

三类资源均已有列表、详情、新增、修改、删除和 options：

- `/api/v1/customers`
- `/api/v1/suppliers`
- `/api/v1/warehouses`

当前状态值统一使用字符串：

- `ENABLED`：启用
- `DISABLED`：停用

不要再按旧任务书使用整数 `1/0`，否则会与数据库和现有页面不兼容。

当前真实字段名：

- 联系电话：`contactPhone`，不是 `phone`。
- 分类排序：`sortNo`，不是 `sortOrder`。
- 商品最低库存：`minStock`。
- 商品采购价、销售价：`purchasePrice`、`salePrice`。

## 5. 接下来按优先级完成

## P0：必须先修，避免联调数据串企业

### 5.1 补齐企业隔离

对商品、分类、单位、客户、供应商、仓库六类资源统一处理：

1. 列表方法增加 `enterpriseId` 参数，并在查询条件中添加：

```java
wrapper.eq(Entity::getEnterpriseId, enterpriseId);
```

2. 详情不能直接 `getById(id)`，必须同时匹配 `id + enterpriseId`。
3. 修改、删除前查询旧记录时必须匹配 `id + enterpriseId`。
4. Controller 从 `@AuthenticationPrincipal LoginUser` 取得 `enterpriseId` 并传给 Service。
5. options 已经按企业过滤，保持现状。

验收方式：企业 A 的账号传入企业 B 的 ID 时，应返回“不存在”，不能读取或修改数据。

### 5.2 补后端 DTO 和校验

一周版本不必一次建立大量类，至少为以下四个主资源建立请求 DTO：

- `ProductSaveRequest`
- `CustomerSaveRequest`
- `SupplierSaveRequest`
- `WarehouseSaveRequest`

Controller 使用 `@Valid @RequestBody`。最低校验规则：

- 编码、名称必填。
- 编码最长 50，名称最长 200。
- 商品 `categoryId`、`unitId` 必填。
- `purchasePrice`、`salePrice`、`minStock` 不得小于 0。
- 电话只校验长度，不写复杂正则。
- 备注最长 500。
- `status` 只能是 `ENABLED` 或 `DISABLED`。

若时间不足，VO 可以暂缓，但不能继续只靠前端校验。

## P1：补齐原任务书页面

### 5.3 分类和单位维护界面

优先在现有 `product/index.vue` 中增加三个标签页：

1. 商品列表
2. 商品分类
3. 计量单位

这样不需要新增菜单和修改动态路由。分类、单位页面至少具备：

- 分页列表。
- 编码、名称、状态查询。
- 新增、编辑、删除。
- 分类可选择父分类，最多两级。
- 删除前确认。
- 状态 Tag。

不要另外复制商品页面形成第二套商品管理。

### 5.4 补齐 options 返回字段

商品 options 每项至少返回：

```json
{
  "value": 1,
  "label": "P0001 - A4 复印纸",
  "productCode": "P0001",
  "productName": "A4 复印纸",
  "specification": "70g，5包/箱",
  "unitId": 2,
  "unitName": "箱",
  "purchasePrice": 18.50,
  "salePrice": 23.00
}
```

客户 options 增加：`customerCode`、`customerName`、`creditLimit`。  
供应商 options 增加：`supplierCode`、`supplierName`。  
仓库 options 增加：`warehouseCode`、`warehouseName`、`storeId`。

保持 `value` 为数字。Vue 和后端当前都使用 `number/Long`，不必为了旧文档强行转字符串。

### 5.5 补演示数据

在 `sql/V2__seed_demo_data.sql` 中新增两件不同分类的商品，使商品达到 10 条：

- `P0009`
- `P0010`

使用 `INSERT IGNORE`，不要修改现有商品编码和已有订单引用。

## P2：质量和验收

### 5.6 增加专项测试

至少完成以下后端测试：

- 商品编码重复时拒绝新增。
- 客户编码重复时拒绝新增。
- 供应商编码重复时拒绝新增。
- 停用商品不出现在 options。
- 企业 A 无法访问企业 B 的基础资料。

如果来不及做完整 SpringBoot 集成测试，可先写 Service 测试，但企业隔离用例必须覆盖。

### 5.7 删除引用保护

最低要求：被采购单、销售单、库存余额引用的商品不允许删除；被业务单据引用的客户、供应商和仓库不允许删除。返回明确业务提示，不要直接抛数据库异常。

## 6. 不要重复修改的部分

- 不修改采购、销售、库存、退货和财务表结构。
- 不改现有 `/api/v1` 路径。
- 不把状态从 `ENABLED/DISABLED` 改回 `1/0`。
- 不重命名现有前端目录。
- 不修改 options 中现有字段，只能向后兼容地增加字段。
- 不删除现有 V2 演示商品和业务单据。
- 不提交本地数据库密码、服务器密码或 SSH 私钥。

## 7. 建议提交顺序

```text
fix(masterdata): scope queries by enterprise
fix(masterdata): validate save requests
feat(masterdata): add category and unit maintenance
feat(masterdata): enrich options payloads
test(masterdata): cover duplicate codes and tenant isolation
docs(seed): add remaining master data demos
```

每完成一组就提交，不要把所有修改堆在一个提交中。

## 8. 验收清单

- [ ] 六类资源列表仅返回当前企业数据。
- [ ] 六类资源详情、修改、删除均校验当前企业。
- [ ] 商品、客户、供应商、仓库新增接口具有后端必填校验。
- [ ] 商品金额和库存下限不能为负数。
- [ ] 商品编码重复时返回明确提示。
- [ ] 客户、供应商和仓库编码重复时返回明确提示。
- [ ] 停用资源不出现在 options。
- [ ] 商品、分类、单位均可在前端维护。
- [ ] 商品 options 回显规格、单位和价格正确。
- [ ] 销售单可以选择客户、商品、仓库。
- [ ] 采购单可以选择供应商、商品、仓库。
- [ ] 库存和报表页面可以选择仓库。
- [ ] 演示数据达到 10 个商品、5 个客户、5 个供应商、至少 2 个仓库。
- [ ] `mvn test` 通过。
- [ ] `npm run build` 通过。

## 9. 完成定义

满足以下条件后，本成员任务可以认定为完成：

1. 商品、分类、单位、客户、供应商、仓库六类资料均有可用的前后端维护能力。
2. 所有接口具有当前企业隔离，不存在跨企业读写。
3. options 字段足够支撑销售、采购、库存、资金和报表回显。
4. 演示数据达到任务书数量。
5. 至少覆盖编码重复、停用 options、企业隔离三类自动化测试。
6. 后端测试和前端生产构建通过。
