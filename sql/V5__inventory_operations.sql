-- 库存盘点、调拨和调整菜单权限补充。
-- 前置条件：已执行 V1、V2、V3。

SET NAMES utf8mb4;
START TRANSACTION;

SET @enterprise_id = (
    SELECT id FROM org_enterprise WHERE enterprise_code = 'DEMO001' LIMIT 1
);
SET @inventory_menu_id = (
    SELECT id FROM sys_menu WHERE permission_code = 'inventory:view' AND deleted = 0 LIMIT 1
);

INSERT IGNORE INTO sys_menu
    (parent_id, menu_type, menu_name, permission_code, route_path, component_path, sort_no, visible, status)
VALUES
    (@inventory_menu_id, 'MENU', '库存调拨', 'inventory:transfer:list',
     'transfers', 'views/inventory/transfer/index', 6, 1, 'ENABLED'),
    (@inventory_menu_id, 'MENU', '商品报损与调整', 'inventory:adjustment:list',
     'adjustments', 'views/inventory/adjustment/index', 7, 1, 'ENABLED');

-- INSERT IGNORE 不会修复已经存在的菜单；显式回写也可以修复曾因客户端字符集错误写成问号的名称。
UPDATE sys_menu
SET menu_name = '库存调拨',
    route_path = 'transfers',
    component_path = 'views/inventory/transfer/index',
    sort_no = 6,
    visible = 1,
    status = 'ENABLED'
WHERE permission_code = 'inventory:transfer:list' AND deleted = 0;

UPDATE sys_menu
SET menu_name = '商品报损与调整',
    route_path = 'adjustments',
    component_path = 'views/inventory/adjustment/index',
    sort_no = 7,
    visible = 1,
    status = 'ENABLED'
WHERE permission_code = 'inventory:adjustment:list' AND deleted = 0;

SET @role_admin = (
    SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'ADMIN' LIMIT 1
);
SET @role_warehouse = (
    SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'WAREHOUSE' LIMIT 1
);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_admin, id FROM sys_menu
WHERE permission_code IN ('inventory:transfer:list', 'inventory:adjustment:list') AND deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_warehouse, id FROM sys_menu
WHERE permission_code IN ('inventory:transfer:list', 'inventory:adjustment:list') AND deleted = 0;

COMMIT;
