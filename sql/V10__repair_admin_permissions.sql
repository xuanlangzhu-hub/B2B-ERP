-- 修复本地库中“登录后权限不足、导航栏为空”的管理员权限数据。
-- 前置条件：已经按顺序执行 V1 至 V9。本脚本可重复执行。
SET NAMES utf8mb4;
START TRANSACTION;

-- 如果曾经软删除或停用了同企业的 ADMIN 角色，先恢复它，避免唯一键阻止重新创建。
UPDATE sys_role r
JOIN sys_user u ON u.enterprise_id = r.enterprise_id
SET r.status = 'ENABLED', r.deleted = 0
WHERE u.username = 'admin'
  AND u.status = 'ENABLED'
  AND u.deleted = 0
  AND r.role_code = 'ADMIN';

-- 兼容只创建了 admin 用户、但漏建 ADMIN 角色的本地演示库。
INSERT INTO sys_role
    (enterprise_id, role_code, role_name, data_scope, status, sort_no, remark)
SELECT u.enterprise_id, 'ADMIN', '系统管理员', 'ALL', 'ENABLED', 1, 'V10 自动修复'
FROM sys_user u
WHERE u.username = 'admin'
  AND u.status = 'ENABLED'
  AND u.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role r
      WHERE r.enterprise_id = u.enterprise_id
        AND r.role_code = 'ADMIN'
        AND r.deleted = 0
  );

-- 将每个企业的 admin 演示账号重新绑定到本企业 ADMIN 角色。
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r
  ON r.enterprise_id = u.enterprise_id
 AND r.role_code = 'ADMIN'
 AND r.status = 'ENABLED'
 AND r.deleted = 0
WHERE u.username = 'admin'
  AND u.status = 'ENABLED'
  AND u.deleted = 0;

-- 所有 ADMIN 角色均拥有当前全部启用菜单；以后补菜单后也可重跑本脚本。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code = 'ADMIN'
  AND r.status = 'ENABLED'
  AND r.deleted = 0
  AND m.status = 'ENABLED'
  AND m.deleted = 0;

COMMIT;

-- 结果中 role_count 和 menu_count 都应大于 0；管理员的 menu_count 应接近 sys_menu 总数。
SELECT u.id,
       u.username,
       u.enterprise_id,
       COUNT(DISTINCT ur.role_id) AS role_count,
       COUNT(DISTINCT rm.menu_id) AS menu_count
FROM sys_user u
LEFT JOIN sys_user_role ur ON ur.user_id = u.id
LEFT JOIN sys_role_menu rm ON rm.role_id = ur.role_id
WHERE u.username = 'admin' AND u.deleted = 0
GROUP BY u.id, u.username, u.enterprise_id;
