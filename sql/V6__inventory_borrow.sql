-- 借出、借入和归还菜单权限补充。
-- 前置条件：已依次执行 V1 至 V5。

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
    (@inventory_menu_id, 'MENU', '借出管理', 'inventory:borrow-out:list',
     'borrow-outs', 'views/inventory/borrow/index', 8, 1, 'ENABLED'),
    (@inventory_menu_id, 'MENU', '借入管理', 'inventory:borrow-in:list',
     'borrow-ins', 'views/inventory/borrow/index', 9, 1, 'ENABLED');

-- 保证脚本可重复执行，并修复客户端字符集设置错误留下的问号菜单名。
UPDATE sys_menu
SET menu_name = '借出管理', route_path = 'borrow-outs',
    component_path = 'views/inventory/borrow/index', sort_no = 8, visible = 1, status = 'ENABLED'
WHERE permission_code = 'inventory:borrow-out:list' AND deleted = 0;

UPDATE sys_menu
SET menu_name = '借入管理', route_path = 'borrow-ins',
    component_path = 'views/inventory/borrow/index', sort_no = 9, visible = 1, status = 'ENABLED'
WHERE permission_code = 'inventory:borrow-in:list' AND deleted = 0;

SET @role_admin = (
    SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'ADMIN' LIMIT 1
);
SET @role_warehouse = (
    SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'WAREHOUSE' LIMIT 1
);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_admin, id FROM sys_menu
WHERE permission_code IN ('inventory:borrow-out:list', 'inventory:borrow-in:list') AND deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_warehouse, id FROM sys_menu
WHERE permission_code IN ('inventory:borrow-out:list', 'inventory:borrow-in:list') AND deleted = 0;

COMMIT;
