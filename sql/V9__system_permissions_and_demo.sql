-- 第七阶段：系统设置、权限和演示数据（可重复执行）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_notice_read (
    id BIGINT NOT NULL AUTO_INCREMENT,
    notice_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_notice_read (notice_id, user_id),
    KEY idx_sys_notice_read_user (user_id, read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知阅读记录';

SET @enterprise_id = (SELECT id FROM org_enterprise WHERE deleted = 0 ORDER BY id LIMIT 1);
SET @menu_finance = (SELECT id FROM sys_menu WHERE permission_code = 'finance:view' AND deleted = 0 LIMIT 1);
SET @menu_system = (SELECT id FROM sys_menu WHERE permission_code = 'system:view' AND deleted = 0 LIMIT 1);

INSERT INTO sys_menu (parent_id, menu_type, menu_name, permission_code, route_path, component_path, icon, sort_no, visible, status)
SELECT @menu_finance, 'MENU', '其他收支', 'finance:other:list', 'other-transactions', 'views/finance/other/index', 'Coin', 5, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'finance:other:list');

INSERT INTO sys_menu (parent_id, menu_type, menu_name, permission_code, route_path, component_path, icon, sort_no, visible, status)
SELECT @menu_system, 'MENU', '企业信息', 'system:enterprise:view', 'enterprise', 'views/system/enterprise/index', 'OfficeBuilding', 4, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'system:enterprise:view');

INSERT INTO sys_menu (parent_id, menu_type, menu_name, permission_code, route_path, component_path, icon, sort_no, visible, status)
SELECT @menu_system, 'MENU', '消息通知', 'system:notice:list', 'notices', 'views/system/notice/index', 'Bell', 5, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission_code = 'system:notice:list');

-- 主动校正已存在记录，兼容旧环境或错误终端编码造成的中文问号。
UPDATE sys_menu SET menu_name='其他收支', parent_id=@menu_finance, route_path='other-transactions',
                    component_path='views/finance/other/index', icon='Coin', sort_no=5
WHERE permission_code='finance:other:list';
UPDATE sys_menu SET menu_name='企业信息', parent_id=@menu_system, route_path='enterprise',
                    component_path='views/system/enterprise/index', icon='OfficeBuilding', sort_no=4
WHERE permission_code='system:enterprise:view';
UPDATE sys_menu SET menu_name='消息通知', parent_id=@menu_system, route_path='notices',
                    component_path='views/system/notice/index', icon='Bell', sort_no=5
WHERE permission_code='system:notice:list';

SET @role_admin = (SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'ADMIN' AND deleted = 0 LIMIT 1);
SET @role_finance = (SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'FINANCE' AND deleted = 0 LIMIT 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_admin, id FROM sys_menu
WHERE permission_code IN ('finance:other:list', 'system:enterprise:view', 'system:notice:list');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_finance, id FROM sys_menu WHERE permission_code = 'finance:other:list';

INSERT INTO sys_notice (enterprise_id, notice_title, notice_content, notice_type, publish_status, published_at, created_by, updated_by)
SELECT @enterprise_id, '欢迎使用 B2B 云进销存 ERP', '系统已准备好销售、采购、库存、资金和报表演示数据。建议从首页进入各业务模块体验完整流程。', 'SYSTEM', 'PUBLISHED', NOW(),
       (SELECT id FROM sys_user WHERE enterprise_id=@enterprise_id AND username='admin' AND deleted=0 LIMIT 1),
       (SELECT id FROM sys_user WHERE enterprise_id=@enterprise_id AND username='admin' AND deleted=0 LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM sys_notice WHERE enterprise_id=@enterprise_id AND notice_title='欢迎使用 B2B 云进销存 ERP' AND deleted=0);

INSERT INTO sys_notice (enterprise_id, notice_title, notice_content, notice_type, publish_status, published_at, created_by, updated_by)
SELECT @enterprise_id, '演示提示：单据状态会影响库存和资金', '销售出库、采购入库、退货、收付款和其他收支确认后会产生对应库存或资金流水，取消时系统会自动回冲。', 'BUSINESS', 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 1 DAY),
       (SELECT id FROM sys_user WHERE enterprise_id=@enterprise_id AND username='admin' AND deleted=0 LIMIT 1),
       (SELECT id FROM sys_user WHERE enterprise_id=@enterprise_id AND username='admin' AND deleted=0 LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM sys_notice WHERE enterprise_id=@enterprise_id AND notice_title='演示提示：单据状态会影响库存和资金' AND deleted=0);

-- 让管理员可访问全部门店，便于顶栏切换演示。
INSERT IGNORE INTO sys_user_store (user_id, store_id)
SELECT u.id, s.id FROM sys_user u JOIN org_store s ON s.enterprise_id=u.enterprise_id AND s.deleted=0
WHERE u.enterprise_id=@enterprise_id AND u.username='admin' AND u.deleted=0;
