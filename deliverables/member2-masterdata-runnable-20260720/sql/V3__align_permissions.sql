-- B2B 云进销存 ERP 系统权限编码修正 V3
-- 前置条件：已依次执行 V1__init_schema.sql、V2__seed_demo_data.sql。
-- 用途：将 V2 演示菜单权限与 Spring Boot Controller 的 @PreAuthorize 编码对齐。

SET NAMES utf8mb4;
START TRANSACTION;

SET @enterprise_id = (
    SELECT id FROM org_enterprise WHERE enterprise_code = 'DEMO001' LIMIT 1
);

-- 修正 V2 中已经存在的菜单权限编码，关联表使用菜单 ID，因此无需重建角色关系。
UPDATE sys_menu SET permission_code = 'dashboard:summary'
WHERE permission_code = 'dashboard:view';

UPDATE sys_menu SET permission_code = 'md:product:list'
WHERE permission_code = 'master:product:list';

UPDATE sys_menu SET permission_code = 'md:customer:list'
WHERE permission_code = 'master:customer:list';

UPDATE sys_menu SET permission_code = 'md:supplier:list'
WHERE permission_code = 'master:supplier:list';

UPDATE sys_menu SET permission_code = 'system:warehouse:list'
WHERE permission_code = 'master:warehouse:list';

SET @product_menu_id = (
    SELECT id FROM sys_menu WHERE permission_code = 'md:product:list' LIMIT 1
);
SET @customer_menu_id = (
    SELECT id FROM sys_menu WHERE permission_code = 'md:customer:list' LIMIT 1
);
SET @supplier_menu_id = (
    SELECT id FROM sys_menu WHERE permission_code = 'md:supplier:list' LIMIT 1
);
SET @warehouse_menu_id = (
    SELECT id FROM sys_menu WHERE permission_code = 'system:warehouse:list' LIMIT 1
);

-- CRUD 按钮权限。visible=0 表示不作为左侧菜单展示。
INSERT IGNORE INTO sys_menu
    (parent_id, menu_type, menu_name, permission_code, sort_no, visible, status)
VALUES
    (@product_menu_id, 'BUTTON', '新增商品', 'md:product:create', 101, 0, 'ENABLED'),
    (@product_menu_id, 'BUTTON', '修改商品', 'md:product:update', 102, 0, 'ENABLED'),
    (@product_menu_id, 'BUTTON', '删除商品', 'md:product:delete', 103, 0, 'ENABLED'),
    (@product_menu_id, 'BUTTON', '查看商品分类', 'md:category:list', 111, 0, 'ENABLED'),
    (@product_menu_id, 'BUTTON', '新增商品分类', 'md:category:create', 112, 0, 'ENABLED'),
    (@product_menu_id, 'BUTTON', '修改商品分类', 'md:category:update', 113, 0, 'ENABLED'),
    (@product_menu_id, 'BUTTON', '删除商品分类', 'md:category:delete', 114, 0, 'ENABLED'),
    (@product_menu_id, 'BUTTON', '查看商品单位', 'md:unit:list', 121, 0, 'ENABLED'),
    (@product_menu_id, 'BUTTON', '新增商品单位', 'md:unit:create', 122, 0, 'ENABLED'),
    (@product_menu_id, 'BUTTON', '修改商品单位', 'md:unit:update', 123, 0, 'ENABLED'),
    (@product_menu_id, 'BUTTON', '删除商品单位', 'md:unit:delete', 124, 0, 'ENABLED'),
    (@customer_menu_id, 'BUTTON', '新增客户', 'md:customer:create', 101, 0, 'ENABLED'),
    (@customer_menu_id, 'BUTTON', '修改客户', 'md:customer:update', 102, 0, 'ENABLED'),
    (@customer_menu_id, 'BUTTON', '删除客户', 'md:customer:delete', 103, 0, 'ENABLED'),
    (@supplier_menu_id, 'BUTTON', '新增供应商', 'md:supplier:create', 101, 0, 'ENABLED'),
    (@supplier_menu_id, 'BUTTON', '修改供应商', 'md:supplier:update', 102, 0, 'ENABLED'),
    (@supplier_menu_id, 'BUTTON', '删除供应商', 'md:supplier:delete', 103, 0, 'ENABLED'),
    (@warehouse_menu_id, 'BUTTON', '新增仓库', 'system:warehouse:create', 101, 0, 'ENABLED'),
    (@warehouse_menu_id, 'BUTTON', '修改仓库', 'system:warehouse:update', 102, 0, 'ENABLED'),
    (@warehouse_menu_id, 'BUTTON', '删除仓库', 'system:warehouse:delete', 103, 0, 'ENABLED');

SET @role_admin = (
    SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'ADMIN' LIMIT 1
);
SET @role_sales = (
    SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'SALES' LIMIT 1
);
SET @role_purchase = (
    SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'PURCHASE' LIMIT 1
);
SET @role_warehouse = (
    SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'WAREHOUSE' LIMIT 1
);
SET @role_finance = (
    SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'FINANCE' LIMIT 1
);

-- 管理员拥有当前全部菜单和按钮权限。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_admin, id FROM sys_menu WHERE deleted = 0;

-- 销售角色：商品只读、客户维护、销售和收款。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_sales, id FROM sys_menu
WHERE permission_code IN (
    'md:product:list', 'md:category:list', 'md:unit:list',
    'md:customer:list', 'md:customer:create', 'md:customer:update', 'md:customer:delete',
    'finance:receipt:list'
);

-- 采购角色：商品只读、供应商维护、采购和付款。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_purchase, id FROM sys_menu
WHERE permission_code IN (
    'md:product:list', 'md:category:list', 'md:unit:list',
    'md:supplier:list', 'md:supplier:create', 'md:supplier:update', 'md:supplier:delete',
    'finance:payment:list'
);

-- 仓库角色：商品只读、仓库维护和库存业务。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_warehouse, id FROM sys_menu
WHERE permission_code IN (
    'md:product:list', 'md:category:list', 'md:unit:list',
    'system:warehouse:list', 'system:warehouse:create',
    'system:warehouse:update', 'system:warehouse:delete'
);

-- 财务角色继续使用 V2 中已有的 finance:* 菜单权限。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_finance, id FROM sys_menu
WHERE permission_code IN (
    'finance:receipt:list', 'finance:payment:list',
    'finance:accounting:list', 'finance:flow:list'
);

COMMIT;

-- 快速核对：
-- SELECT r.role_code, COUNT(*) permission_count
-- FROM sys_role r JOIN sys_role_menu rm ON rm.role_id = r.id
-- WHERE r.enterprise_id = @enterprise_id GROUP BY r.role_code;
