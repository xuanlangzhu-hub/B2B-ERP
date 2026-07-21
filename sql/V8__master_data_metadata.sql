-- 商品属性、商品标签、客户标签菜单权限。
-- 前置条件：已依次执行 V1 至 V7。

SET NAMES utf8mb4;
START TRANSACTION;

SET @enterprise_id = (SELECT id FROM org_enterprise WHERE enterprise_code = 'DEMO001' LIMIT 1);
SET @master_menu_id = (SELECT id FROM sys_menu WHERE permission_code = 'master:view' AND deleted = 0 LIMIT 1);
SET @role_admin = (SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'ADMIN' LIMIT 1);
SET @role_sales = (SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'SALES' LIMIT 1);
SET @role_warehouse = (SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'WAREHOUSE' LIMIT 1);

INSERT IGNORE INTO sys_menu
    (parent_id, menu_type, menu_name, permission_code, route_path, component_path, sort_no, visible, status)
VALUES
    (@master_menu_id, 'MENU', '商品属性', 'md:attribute:list', 'product-attributes', 'views/master/metadata/index', 11, 1, 'ENABLED'),
    (@master_menu_id, 'MENU', '商品标签', 'md:product-tag:list', 'product-tags', 'views/master/metadata/index', 12, 1, 'ENABLED'),
    (@master_menu_id, 'MENU', '客户标签', 'md:customer-tag:list', 'customer-tags', 'views/master/metadata/index', 13, 1, 'ENABLED');

UPDATE sys_menu SET menu_name='商品属性', route_path='product-attributes', component_path='views/master/metadata/index', sort_no=11, visible=1, status='ENABLED'
WHERE permission_code='md:attribute:list' AND deleted=0;
UPDATE sys_menu SET menu_name='商品标签', route_path='product-tags', component_path='views/master/metadata/index', sort_no=12, visible=1, status='ENABLED'
WHERE permission_code='md:product-tag:list' AND deleted=0;
UPDATE sys_menu SET menu_name='客户标签', route_path='customer-tags', component_path='views/master/metadata/index', sort_no=13, visible=1, status='ENABLED'
WHERE permission_code='md:customer-tag:list' AND deleted=0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_admin, id FROM sys_menu WHERE permission_code IN (
    'md:attribute:list','md:product-tag:list','md:customer-tag:list') AND deleted=0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_sales, id FROM sys_menu WHERE permission_code='md:customer-tag:list' AND deleted=0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_warehouse, id FROM sys_menu WHERE permission_code IN (
    'md:attribute:list','md:product-tag:list') AND deleted=0;

COMMIT;
