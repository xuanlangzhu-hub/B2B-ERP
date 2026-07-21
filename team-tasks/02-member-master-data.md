# 成员 2 任务书：商品、客户、供应商与仓库基础资料

> 分支：`feature/masterdata`  
> 后端模块：`masterdata`、组织资料中的 `warehouse`  
> 前端目录：`views/master`、`api/masterdata`  
> 最终责任：给销售、采购、库存提供稳定的基础数据和 options 接口

## 1. 本人交付目标

你负责：

- 商品分类、单位、商品。
- 客户。
- 供应商。
- 仓库。
- 以上资源的列表、详情、新增、修改、删除、状态维护。
- 给其他模块使用的下拉选项接口。
- 至少 10 个商品、5 个客户、5 个供应商、2 个仓库的演示数据。

本周可以不做：

- 商品多规格 SKU。
- 商品属性组合、标签关系的完整页面。
- 客户等级、客户标签和供应商分类的复杂维护。
- Excel 导入导出。
- 图片上传。
- 物理删除和复杂引用检查。

## 2. 数据库负责范围

沿用现有表：

| 表 | 本周实现程度 |
| --- | --- |
| `md_unit` | 完整 CRUD |
| `md_product_category` | 完整 CRUD，最多两级 |
| `md_product` | 完整 CRUD |
| `md_customer` | 完整 CRUD |
| `md_supplier` | 完整 CRUD |
| `org_warehouse` | 完整 CRUD |
| 其他 `md_*` | 保留表，不要求页面 |

### 关键字段约定

#### 商品

| 字段 | 前端字段 | 规则 |
| --- | --- | --- |
| 商品编码 | `productCode` | 必填、唯一、最长 50 |
| 商品名称 | `productName` | 必填、最长 100 |
| 分类 ID | `categoryId` | 必填 |
| 单位 ID | `unitId` | 必填 |
| 规格型号 | `specification` | 可空 |
| 默认采购价 | `purchasePrice` | `BigDecimal`，不得小于 0 |
| 默认销售价 | `salePrice` | `BigDecimal`，不得小于 0 |
| 最低库存 | `minStock` | `BigDecimal`，不得小于 0 |
| 状态 | `status` | 1 启用、0 停用 |
| 备注 | `remark` | 可空，最长 500 |

#### 客户

`customerCode`、`customerName`、`contactName`、`phone`、`address`、`creditLimit`、`status`、`remark`。

#### 供应商

`supplierCode`、`supplierName`、`contactName`、`phone`、`address`、`status`、`remark`。

#### 仓库

`warehouseCode`、`warehouseName`、`storeId`、`managerName`、`address`、`status`、`remark`。

## 3. 后端目录

```text
masterdata/
├─ controller/
│  ├─ ProductController.java
│  ├─ ProductCategoryController.java
│  ├─ UnitController.java
│  ├─ CustomerController.java
│  ├─ SupplierController.java
│  └─ WarehouseController.java
├─ dto/
├─ vo/
├─ entity/
├─ mapper/
└─ service/
```

每类资源至少包含 CreateRequest、UpdateRequest、QueryRequest、VO，不直接暴露 Entity。

## 4. 公共接口约定

- 基础路径 `/api/v1`。
- 返回格式由成员 1 提供。
- 分页使用 `page`、`size`。
- 主键在 JSON 中按字符串处理，避免前端精度问题。
- 时间格式 `yyyy-MM-dd HH:mm:ss`。
- 删除默认逻辑删除。
- 被停用的数据不出现在 options 接口中。

## 5. 商品接口文档

### 5.1 接口列表

| 方法 | 地址 | 功能 |
| --- | --- | --- |
| GET | `/api/v1/products` | 商品分页 |
| GET | `/api/v1/products/{id}` | 商品详情 |
| POST | `/api/v1/products` | 新增商品 |
| PUT | `/api/v1/products/{id}` | 修改商品 |
| DELETE | `/api/v1/products/{id}` | 删除商品 |
| PUT | `/api/v1/products/{id}/status` | 启用或停用 |
| GET | `/api/v1/products/options` | 商品下拉选项 |

分页查询参数：`productCode`、`productName`、`categoryId`、`status`、`page`、`size`。

新增请求：

```json
{
  "productCode": "P0001",
  "productName": "A4 复印纸",
  "categoryId": "1",
  "unitId": "1",
  "specification": "70g 500张/包",
  "purchasePrice": 18.50,
  "salePrice": 23.00,
  "minStock": 20,
  "status": 1,
  "remark": "演示商品"
}
```

详情响应 `data`：

```json
{
  "id": "1",
  "productCode": "P0001",
  "productName": "A4 复印纸",
  "categoryId": "1",
  "categoryName": "办公用纸",
  "unitId": "1",
  "unitName": "包",
  "specification": "70g 500张/包",
  "purchasePrice": 18.50,
  "salePrice": 23.00,
  "minStock": 20,
  "status": 1,
  "remark": "演示商品"
}
```

options 响应：

```json
[
  {
    "value": "1",
    "label": "P0001 - A4 复印纸",
    "productCode": "P0001",
    "productName": "A4 复印纸",
    "specification": "70g 500张/包",
    "unitId": "1",
    "unitName": "包",
    "purchasePrice": 18.50,
    "salePrice": 23.00
  }
]
```

## 6. 分类和单位接口

| 资源 | 列表 | 新增 | 修改 | 删除 | options |
| --- | --- | --- | --- | --- | --- |
| 分类 | `GET /product-categories` | `POST /product-categories` | `PUT /product-categories/{id}` | `DELETE /product-categories/{id}` | `GET /product-categories/options` |
| 单位 | `GET /units` | `POST /units` | `PUT /units/{id}` | `DELETE /units/{id}` | `GET /units/options` |

分类请求字段：`categoryCode`、`categoryName`、`parentId`、`sortOrder`、`status`。

单位请求字段：`unitCode`、`unitName`、`status`、`remark`。

## 7. 客户接口文档

| 方法 | 地址 | 功能 |
| --- | --- | --- |
| GET | `/api/v1/customers` | 客户分页 |
| GET | `/api/v1/customers/{id}` | 客户详情 |
| POST | `/api/v1/customers` | 新增客户 |
| PUT | `/api/v1/customers/{id}` | 修改客户 |
| DELETE | `/api/v1/customers/{id}` | 删除客户 |
| PUT | `/api/v1/customers/{id}/status` | 启停 |
| GET | `/api/v1/customers/options` | 客户下拉 |

查询：`customerCode`、`customerName`、`contactName`、`phone`、`status`。

新增请求：

```json
{
  "customerCode": "C0001",
  "customerName": "上海示例商贸有限公司",
  "contactName": "张经理",
  "phone": "13800000000",
  "address": "上海市",
  "creditLimit": 50000.00,
  "status": 1,
  "remark": "重点客户"
}
```

客户 options 至少返回 `value`、`label`、`customerCode`、`customerName`、`creditLimit`。

## 8. 供应商接口文档

接口结构与客户一致，资源地址使用 `/api/v1/suppliers`。

新增请求：

```json
{
  "supplierCode": "S0001",
  "supplierName": "华东办公用品供应商",
  "contactName": "李经理",
  "phone": "13900000000",
  "address": "江苏省苏州市",
  "status": 1,
  "remark": "长期合作"
}
```

供应商 options 至少返回 `value`、`label`、`supplierCode`、`supplierName`。

## 9. 仓库接口文档

| 方法 | 地址 | 功能 |
| --- | --- | --- |
| GET | `/api/v1/warehouses` | 仓库分页 |
| GET | `/api/v1/warehouses/{id}` | 仓库详情 |
| POST | `/api/v1/warehouses` | 新增仓库 |
| PUT | `/api/v1/warehouses/{id}` | 修改仓库 |
| DELETE | `/api/v1/warehouses/{id}` | 删除仓库 |
| GET | `/api/v1/warehouses/options` | 启用仓库下拉 |

options 至少返回 `value`、`label`、`warehouseCode`、`warehouseName`、`storeId`。

## 10. 前端页面

```text
views/master/
├─ products/index.vue
├─ product-categories/index.vue
├─ units/index.vue
├─ customers/index.vue
├─ suppliers/index.vue
└─ warehouses/index.vue
```

### 商品页面

- 查询区：编码、名称、分类、状态。
- 表格：编码、名称、分类、规格、单位、采购价、销售价、最低库存、状态。
- 新增/编辑：Element Plus Dialog 或 Drawer。
- 金额统一保留两位小数，数量最多四位小数。
- 状态使用 Tag。
- 删除前确认。

### 客户、供应商页面

- 查询：编码、名称、联系人、状态。
- 表格：编码、名称、联系人、电话、地址、状态。
- 新增和编辑弹窗。
- 电话只做长度校验，不做复杂正则。

### 分类、单位、仓库页面

- 普通 CRUD 即可。
- 分类最多两级；来不及时做平铺列表。

## 11. 七天安排

| 日期 | 必须完成 |
| --- | --- |
| 第 1 天 | 检查已有表，建立后端模块、Entity、Mapper、Service、Controller 框架 |
| 第 2 天上午 | 商品、客户、供应商、仓库 options 接口及演示数据，交给成员 3/4/5 |
| 第 2 天下午 | 商品、分类、单位完整页面 |
| 第 3 天 | 客户、供应商、仓库完整页面 |
| 第 4 天 | 分页筛选、唯一校验、状态、表单校验，与其他成员联调下拉 |
| 第 5 天 | 合并，修复字段和路由冲突，检查商品名称回显 |
| 第 6 天 | 补充演示数据，协助销售采购库存修复关联问题 |
| 第 7 天 | 页面细节、空状态、模块说明、最终演示检查 |

## 12. 本人代码规范

- 遵循成员 1 提供的返回体、异常、分页和鉴权。
- Controller 不写业务逻辑。
- 编码唯一性在 Service 校验，数据库同时保留唯一索引。
- 金额使用 `BigDecimal`，禁止 `double`。
- 状态统一使用整数 1/0；业务单据状态才使用字符串枚举。
- options 接口只返回启用且未删除的数据。
- Entity、DTO、VO 分离。
- MapStruct 可选；一周项目可以手工转换，但不能把密码等无关字段暴露。
- 前端类型集中在模块 `types.ts`。
- 页面请求统一放 `api/masterdata`。
- 不修改全局路由和菜单，提供路由片段给成员 1 合并。
- 不修改销售、采购和库存的表结构。

## 13. Git 规范

- 分支：`feature/masterdata`。
- 建议提交：
  - `feat(masterdata): add product management`
  - `feat(masterdata): add customer and supplier management`
  - `feat(masterdata): add warehouse options api`
  - `fix(masterdata): validate duplicate product code`
- 不提交本地数据库配置。
- 不大范围格式化公共文件。
- 第 2 天上午 options 接口完成后立即提交，不等待全部页面。

## 14. 联调依赖

你向其他人提供：

| 使用者 | 接口 | 最晚时间 |
| --- | --- | --- |
| 成员 3 | 客户、商品、仓库 options | 第 2 天中午 |
| 成员 4 | 供应商、商品、仓库 options | 第 2 天中午 |
| 成员 5 | 商品、仓库 options 和详情 | 第 2 天中午 |

如果正式接口未完成，必须先提供固定 JSON 响应，不能让三个人停工。

## 15. 测试清单

- 商品编码重复时拒绝新增。
- 客户和供应商编码重复时拒绝新增。
- 必填字段为空时返回 400。
- 停用商品不出现在 options。
- 商品列表筛选和分页正确。
- 编辑商品后详情回显正确。
- 删除使用逻辑删除。
- 销售页面可以选择客户和商品。
- 采购页面可以选择供应商和商品。
- 库存页面可以选择仓库和商品。
- 前端构建通过。

## 16. 完成定义

- 六类基础资料至少四类完整可用，商品、客户、供应商、仓库不得删减。
- options 接口被销售、采购、库存真实使用。
- 演示数据足够支撑完整业务链。
- 页面、接口和 SQL 已合并。
- 无真实凭据进入代码或文档。

