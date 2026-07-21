-- 门店及基础分类资料菜单权限。
-- 前置条件：已依次执行 V1 至 V6。

SET NAMES utf8mb4;
START TRANSACTION;

SET @enterprise_id = (SELECT id FROM org_enterprise WHERE enterprise_code = 'DEMO001' LIMIT 1);
SET @master_menu_id = (SELECT id FROM sys_menu WHERE permission_code = 'master:view' AND deleted = 0 LIMIT 1);
SET @role_admin = (SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'ADMIN' LIMIT 1);
SET @role_sales = (SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'SALES' LIMIT 1);
SET @role_purchase = (SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'PURCHASE' LIMIT 1);
SET @role_warehouse = (SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'WAREHOUSE' LIMIT 1);

INSERT IGNORE INTO sys_menu
    (parent_id, menu_type, menu_name, permission_code, route_path, component_path, sort_no, visible, status)
VALUES
    (@master_menu_id, 'MENU', '门店管理', 'system:store:list', 'stores', 'views/master/store/index', 5, 1, 'ENABLED'),
    (@master_menu_id, 'MENU', '商品分类', 'md:category:page', 'product-categories', 'views/master/config/index', 6, 1, 'ENABLED'),
    (@master_menu_id, 'MENU', '商品单位', 'md:unit:page', 'units', 'views/master/config/index', 7, 1, 'ENABLED'),
    (@master_menu_id, 'MENU', '客户分类', 'md:customer-category:list', 'customer-categories', 'views/master/config/index', 8, 1, 'ENABLED'),
    (@master_menu_id, 'MENU', '客户等级', 'md:customer-level:list', 'customer-levels', 'views/master/config/index', 9, 1, 'ENABLED'),
    (@master_menu_id, 'MENU', '供应商分类', 'md:supplier-category:list', 'supplier-categories', 'views/master/config/index', 10, 1, 'ENABLED');

UPDATE sys_menu SET menu_name='门店管理', route_path='stores', component_path='views/master/store/index', sort_no=5, visible=1, status='ENABLED'
WHERE permission_code='system:store:list' AND deleted=0;
UPDATE sys_menu SET menu_name='商品分类', route_path='product-categories', component_path='views/master/config/index', sort_no=6, visible=1, status='ENABLED'
WHERE permission_code='md:category:page' AND deleted=0;
UPDATE sys_menu SET menu_name='商品单位', route_path='units', component_path='views/master/config/index', sort_no=7, visible=1, status='ENABLED'
WHERE permission_code='md:unit:page' AND deleted=0;
UPDATE sys_menu SET menu_name='客户分类', route_path='customer-categories', component_path='views/master/config/index', sort_no=8, visible=1, status='ENABLED'
WHERE permission_code='md:customer-category:list' AND deleted=0;
UPDATE sys_menu SET menu_name='客户等级', route_path='customer-levels', component_path='views/master/config/index', sort_no=9, visible=1, status='ENABLED'
WHERE permission_code='md:customer-level:list' AND deleted=0;
UPDATE sys_menu SET menu_name='供应商分类', route_path='supplier-categories', component_path='views/master/config/index', sort_no=10, visible=1, status='ENABLED'
WHERE permission_code='md:supplier-category:list' AND deleted=0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_admin, id FROM sys_menu WHERE permission_code IN (
    'system:store:list','md:category:page','md:unit:page',
    'md:customer-category:list','md:customer-level:list','md:supplier-category:list') AND deleted=0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_sales, id FROM sys_menu WHERE permission_code IN (
    'md:customer-category:list','md:customer-level:list') AND deleted=0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_purchase, id FROM sys_menu WHERE permission_code='md:supplier-category:list' AND deleted=0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_warehouse, id FROM sys_menu WHERE permission_code IN (
    'system:store:list','md:category:page','md:unit:page') AND deleted=0;

COMMIT;
