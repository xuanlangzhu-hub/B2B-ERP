-- B2B 云进销存 ERP 系统演示数据 V2
-- MySQL 8.0+
-- 前置条件：已执行 V1__init_schema.sql，并已选择目标数据库。
-- 说明：
-- 1. 所有演示数据均使用固定业务编码，INSERT IGNORE 保证脚本可重复执行。
-- 2. 本脚本不会保存数据库连接地址、用户名或密码。
-- 3. 演示账号使用 Spring Security DelegatingPasswordEncoder 的 {noop} 格式，
--    仅适合一周实训演示。接入登录后应立即改成 BCrypt 并修改默认密码。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;

-- =========================================================
-- 1. 企业、门店、仓库
-- =========================================================

INSERT IGNORE INTO org_enterprise
    (enterprise_code, enterprise_name, contact_name, contact_phone, address, status, remark)
VALUES
    ('DEMO001', '七尾商贸演示企业', '系统管理员', '13800000000', '上海市浦东新区演示路 88 号', 'ENABLED', '系统初始化演示账套');

SET @enterprise_id = (SELECT id FROM org_enterprise WHERE enterprise_code = 'DEMO001' LIMIT 1);

INSERT IGNORE INTO org_store
    (enterprise_id, store_code, store_name, manager_name, contact_phone, address, status, remark)
VALUES
    (@enterprise_id, 'STORE001', '上海旗舰店', '张店长', '13800000001', '上海市浦东新区演示路 88 号一层', 'ENABLED', '默认演示门店'),
    (@enterprise_id, 'STORE002', '上海二号店', '李店长', '13800000002', '上海市徐汇区示例路 66 号', 'ENABLED', '备用演示门店');

SET @store_1 = (SELECT id FROM org_store WHERE enterprise_id = @enterprise_id AND store_code = 'STORE001' LIMIT 1);
SET @store_2 = (SELECT id FROM org_store WHERE enterprise_id = @enterprise_id AND store_code = 'STORE002' LIMIT 1);

INSERT IGNORE INTO org_warehouse
    (enterprise_id, store_id, warehouse_code, warehouse_name, warehouse_type, manager_name, contact_phone, address, allow_negative, status, remark)
VALUES
    (@enterprise_id, @store_1, 'WH001', '旗舰店主仓', 'STORE', '王仓管', '13800000003', '上海市浦东新区演示路 88 号仓库', 0, 'ENABLED', '默认采购入库及销售出库仓'),
    (@enterprise_id, @store_2, 'WH002', '二号店仓库', 'STORE', '赵仓管', '13800000004', '上海市徐汇区示例路 66 号仓库', 0, 'ENABLED', '二号店仓库'),
    (@enterprise_id, NULL, 'WH-RETURN', '总部退货仓', 'RETURN', '周仓管', '13800000005', '上海市浦东新区演示路 88 号退货区', 0, 'ENABLED', '退货及残次品暂存');

SET @warehouse_1 = (SELECT id FROM org_warehouse WHERE enterprise_id = @enterprise_id AND warehouse_code = 'WH001' LIMIT 1);
SET @warehouse_2 = (SELECT id FROM org_warehouse WHERE enterprise_id = @enterprise_id AND warehouse_code = 'WH002' LIMIT 1);
SET @warehouse_return = (SELECT id FROM org_warehouse WHERE enterprise_id = @enterprise_id AND warehouse_code = 'WH-RETURN' LIMIT 1);

-- =========================================================
-- 2. 用户、角色、菜单及数据权限
-- =========================================================

INSERT IGNORE INTO sys_role
    (enterprise_id, role_code, role_name, data_scope, status, sort_no, remark)
VALUES
    (@enterprise_id, 'ADMIN', '系统管理员', 'ALL', 'ENABLED', 1, '拥有演示系统全部权限'),
    (@enterprise_id, 'SALES', '销售人员', 'STORE', 'ENABLED', 2, '销售、客户和销售报表'),
    (@enterprise_id, 'PURCHASE', '采购人员', 'STORE', 'ENABLED', 3, '采购、供应商和采购报表'),
    (@enterprise_id, 'WAREHOUSE', '仓库人员', 'WAREHOUSE', 'ENABLED', 4, '入出库、库存及盘点'),
    (@enterprise_id, 'FINANCE', '财务人员', 'ALL', 'ENABLED', 5, '收付款、往来和资金报表');

SET @demo_password_hash = '{noop}Admin@123456';

INSERT IGNORE INTO sys_user
    (enterprise_id, username, password_hash, real_name, phone, email, default_store_id, status, remark)
VALUES
    (@enterprise_id, 'admin', @demo_password_hash, '系统管理员', '13900000001', 'admin@example.com', @store_1, 'ENABLED', '默认管理员；首次登录后请修改密码'),
    (@enterprise_id, 'sales', @demo_password_hash, '销售演示员', '13900000002', 'sales@example.com', @store_1, 'ENABLED', '销售岗位演示账号'),
    (@enterprise_id, 'purchase', @demo_password_hash, '采购演示员', '13900000003', 'purchase@example.com', @store_1, 'ENABLED', '采购岗位演示账号'),
    (@enterprise_id, 'warehouse', @demo_password_hash, '仓库演示员', '13900000004', 'warehouse@example.com', @store_1, 'ENABLED', '仓库岗位演示账号'),
    (@enterprise_id, 'finance', @demo_password_hash, '财务演示员', '13900000005', 'finance@example.com', @store_1, 'ENABLED', '财务岗位演示账号');

SET @admin_id = (SELECT id FROM sys_user WHERE enterprise_id = @enterprise_id AND username = 'admin' LIMIT 1);
SET @sales_user_id = (SELECT id FROM sys_user WHERE enterprise_id = @enterprise_id AND username = 'sales' LIMIT 1);
SET @purchase_user_id = (SELECT id FROM sys_user WHERE enterprise_id = @enterprise_id AND username = 'purchase' LIMIT 1);
SET @warehouse_user_id = (SELECT id FROM sys_user WHERE enterprise_id = @enterprise_id AND username = 'warehouse' LIMIT 1);
SET @finance_user_id = (SELECT id FROM sys_user WHERE enterprise_id = @enterprise_id AND username = 'finance' LIMIT 1);

SET @role_admin = (SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'ADMIN' LIMIT 1);
SET @role_sales = (SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'SALES' LIMIT 1);
SET @role_purchase = (SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'PURCHASE' LIMIT 1);
SET @role_warehouse = (SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'WAREHOUSE' LIMIT 1);
SET @role_finance = (SELECT id FROM sys_role WHERE enterprise_id = @enterprise_id AND role_code = 'FINANCE' LIMIT 1);

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
    (@admin_id, @role_admin),
    (@sales_user_id, @role_sales),
    (@purchase_user_id, @role_purchase),
    (@warehouse_user_id, @role_warehouse),
    (@finance_user_id, @role_finance);

INSERT IGNORE INTO sys_menu
    (parent_id, menu_type, menu_name, permission_code, route_path, component_path, icon, sort_no, visible, status)
VALUES
    (0, 'MENU', '首页', 'dashboard:view', '/dashboard', 'views/dashboard/index', 'HomeFilled', 1, 1, 'ENABLED'),
    (0, 'DIRECTORY', '基础资料', 'master:view', '/master', NULL, 'Collection', 2, 1, 'ENABLED'),
    (0, 'DIRECTORY', '销售管理', 'sales:view', '/sales', NULL, 'Sell', 3, 1, 'ENABLED'),
    (0, 'DIRECTORY', '采购管理', 'purchase:view', '/purchase', NULL, 'ShoppingCart', 4, 1, 'ENABLED'),
    (0, 'DIRECTORY', '库存管理', 'inventory:view', '/inventory', NULL, 'Box', 5, 1, 'ENABLED'),
    (0, 'DIRECTORY', '资金管理', 'finance:view', '/finance', NULL, 'Wallet', 6, 1, 'ENABLED'),
    (0, 'DIRECTORY', '报表中心', 'report:view', '/reports', NULL, 'DataAnalysis', 7, 1, 'ENABLED'),
    (0, 'DIRECTORY', '系统设置', 'system:view', '/system', NULL, 'Setting', 8, 1, 'ENABLED');

SET @menu_master = (SELECT id FROM sys_menu WHERE permission_code = 'master:view' LIMIT 1);
SET @menu_sales = (SELECT id FROM sys_menu WHERE permission_code = 'sales:view' LIMIT 1);
SET @menu_purchase = (SELECT id FROM sys_menu WHERE permission_code = 'purchase:view' LIMIT 1);
SET @menu_inventory = (SELECT id FROM sys_menu WHERE permission_code = 'inventory:view' LIMIT 1);
SET @menu_finance = (SELECT id FROM sys_menu WHERE permission_code = 'finance:view' LIMIT 1);
SET @menu_report = (SELECT id FROM sys_menu WHERE permission_code = 'report:view' LIMIT 1);
SET @menu_system = (SELECT id FROM sys_menu WHERE permission_code = 'system:view' LIMIT 1);

INSERT IGNORE INTO sys_menu
    (parent_id, menu_type, menu_name, permission_code, route_path, component_path, icon, sort_no, visible, status)
VALUES
    (@menu_master, 'MENU', '商品管理', 'master:product:list', 'products', 'views/master/product/index', NULL, 1, 1, 'ENABLED'),
    (@menu_master, 'MENU', '客户管理', 'master:customer:list', 'customers', 'views/master/customer/index', NULL, 2, 1, 'ENABLED'),
    (@menu_master, 'MENU', '供应商管理', 'master:supplier:list', 'suppliers', 'views/master/supplier/index', NULL, 3, 1, 'ENABLED'),
    (@menu_master, 'MENU', '仓库与门店', 'master:warehouse:list', 'warehouses', 'views/master/warehouse/index', NULL, 4, 1, 'ENABLED'),
    (@menu_sales, 'MENU', '销售单', 'sales:order:list', 'orders', 'views/sales/order/index', NULL, 1, 1, 'ENABLED'),
    (@menu_sales, 'MENU', '销售退货', 'sales:return:list', 'returns', 'views/sales/return/index', NULL, 2, 1, 'ENABLED'),
    (@menu_purchase, 'MENU', '采购单', 'purchase:order:list', 'orders', 'views/purchase/order/index', NULL, 1, 1, 'ENABLED'),
    (@menu_purchase, 'MENU', '采购退货', 'purchase:return:list', 'returns', 'views/purchase/return/index', NULL, 2, 1, 'ENABLED'),
    (@menu_inventory, 'MENU', '库存查询', 'inventory:stock:list', 'stock', 'views/inventory/stock/index', NULL, 1, 1, 'ENABLED'),
    (@menu_inventory, 'MENU', '入库管理', 'inventory:inbound:list', 'inbounds', 'views/inventory/inbound/index', NULL, 2, 1, 'ENABLED'),
    (@menu_inventory, 'MENU', '出库管理', 'inventory:outbound:list', 'outbounds', 'views/inventory/outbound/index', NULL, 3, 1, 'ENABLED'),
    (@menu_inventory, 'MENU', '库存流水', 'inventory:movement:list', 'movements', 'views/inventory/movement/index', NULL, 4, 1, 'ENABLED'),
    (@menu_inventory, 'MENU', '库存盘点', 'inventory:count:list', 'counts', 'views/inventory/count/index', NULL, 5, 1, 'ENABLED'),
    (@menu_finance, 'MENU', '收款单', 'finance:receipt:list', 'receipts', 'views/finance/receipt/index', NULL, 1, 1, 'ENABLED'),
    (@menu_finance, 'MENU', '付款单', 'finance:payment:list', 'payments', 'views/finance/payment/index', NULL, 2, 1, 'ENABLED'),
    (@menu_finance, 'MENU', '应收应付', 'finance:accounting:list', 'accounting', 'views/finance/accounting/index', NULL, 3, 1, 'ENABLED'),
    (@menu_finance, 'MENU', '资金流水', 'finance:flow:list', 'flows', 'views/finance/flow/index', NULL, 4, 1, 'ENABLED'),
    (@menu_report, 'MENU', '销售报表', 'report:sales:view', 'sales', 'views/report/sales/index', NULL, 1, 1, 'ENABLED'),
    (@menu_report, 'MENU', '采购报表', 'report:purchase:view', 'purchase', 'views/report/purchase/index', NULL, 2, 1, 'ENABLED'),
    (@menu_report, 'MENU', '库存报表', 'report:inventory:view', 'inventory', 'views/report/inventory/index', NULL, 3, 1, 'ENABLED'),
    (@menu_report, 'MENU', '财务报表', 'report:finance:view', 'finance', 'views/report/finance/index', NULL, 4, 1, 'ENABLED'),
    (@menu_system, 'MENU', '员工管理', 'system:user:list', 'users', 'views/system/user/index', NULL, 1, 1, 'ENABLED'),
    (@menu_system, 'MENU', '角色管理', 'system:role:list', 'roles', 'views/system/role/index', NULL, 2, 1, 'ENABLED'),
    (@menu_system, 'MENU', '操作日志', 'system:log:list', 'logs', 'views/system/log/index', NULL, 3, 1, 'ENABLED');

-- 管理员拥有全部菜单；岗位角色按模块分配。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_admin, id FROM sys_menu WHERE deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_sales, id FROM sys_menu
WHERE permission_code IN ('dashboard:view', 'master:view', 'master:product:list', 'master:customer:list',
                          'sales:view', 'sales:order:list', 'sales:return:list', 'report:view', 'report:sales:view');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_purchase, id FROM sys_menu
WHERE permission_code IN ('dashboard:view', 'master:view', 'master:product:list', 'master:supplier:list',
                          'purchase:view', 'purchase:order:list', 'purchase:return:list', 'report:view', 'report:purchase:view');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_warehouse, id FROM sys_menu
WHERE permission_code IN ('dashboard:view', 'master:view', 'master:product:list', 'master:warehouse:list',
                          'inventory:view', 'inventory:stock:list', 'inventory:inbound:list', 'inventory:outbound:list',
                          'inventory:movement:list', 'inventory:count:list', 'report:view', 'report:inventory:view');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @role_finance, id FROM sys_menu
WHERE permission_code IN ('dashboard:view', 'master:view', 'master:customer:list', 'master:supplier:list',
                          'finance:view', 'finance:receipt:list', 'finance:payment:list', 'finance:accounting:list',
                          'finance:flow:list', 'report:view', 'report:finance:view');

INSERT IGNORE INTO sys_user_store (user_id, store_id) VALUES
    (@admin_id, @store_1), (@admin_id, @store_2),
    (@sales_user_id, @store_1),
    (@purchase_user_id, @store_1),
    (@warehouse_user_id, @store_1),
    (@finance_user_id, @store_1), (@finance_user_id, @store_2);

INSERT IGNORE INTO sys_user_warehouse (user_id, warehouse_id) VALUES
    (@admin_id, @warehouse_1), (@admin_id, @warehouse_2), (@admin_id, @warehouse_return),
    (@warehouse_user_id, @warehouse_1), (@warehouse_user_id, @warehouse_return),
    (@sales_user_id, @warehouse_1), (@purchase_user_id, @warehouse_1);

-- =========================================================
-- 3. 字典和单据序列
-- =========================================================

INSERT IGNORE INTO sys_dict_type (dict_code, dict_name, status, remark) VALUES
    ('common_status', '通用状态', 'ENABLED', '基础启停状态'),
    ('order_status', '业务单据状态', 'ENABLED', '采购单和销售单状态'),
    ('payment_method', '收付款方式', 'ENABLED', '资金账户收付款方式');

SET @dict_common = (SELECT id FROM sys_dict_type WHERE dict_code = 'common_status' LIMIT 1);
SET @dict_order = (SELECT id FROM sys_dict_type WHERE dict_code = 'order_status' LIMIT 1);
SET @dict_payment = (SELECT id FROM sys_dict_type WHERE dict_code = 'payment_method' LIMIT 1);

INSERT IGNORE INTO sys_dict_item (dict_type_id, item_value, item_label, sort_no, status) VALUES
    (@dict_common, 'ENABLED', '启用', 1, 'ENABLED'),
    (@dict_common, 'DISABLED', '停用', 2, 'ENABLED'),
    (@dict_order, 'DRAFT', '草稿', 1, 'ENABLED'),
    (@dict_order, 'APPROVED', '已审核', 2, 'ENABLED'),
    (@dict_order, 'PARTIALLY_INBOUND', '部分入库', 3, 'ENABLED'),
    (@dict_order, 'PARTIALLY_OUTBOUND', '部分出库', 4, 'ENABLED'),
    (@dict_order, 'COMPLETED', '已完成', 5, 'ENABLED'),
    (@dict_order, 'CANCELLED', '已取消', 6, 'ENABLED'),
    (@dict_payment, 'CASH', '现金', 1, 'ENABLED'),
    (@dict_payment, 'BANK', '银行转账', 2, 'ENABLED'),
    (@dict_payment, 'WECHAT', '微信', 3, 'ENABLED'),
    (@dict_payment, 'ALIPAY', '支付宝', 4, 'ENABLED');

INSERT IGNORE INTO sys_document_sequence
    (enterprise_id, document_type, date_part, current_value, prefix, sequence_length)
VALUES
    (@enterprise_id, 'SALES_ORDER', DATE_FORMAT(CURRENT_DATE, '%Y%m%d'), 2, 'XS', 4),
    (@enterprise_id, 'PURCHASE_ORDER', DATE_FORMAT(CURRENT_DATE, '%Y%m%d'), 2, 'CG', 4),
    (@enterprise_id, 'INBOUND', DATE_FORMAT(CURRENT_DATE, '%Y%m%d'), 1, 'RK', 4),
    (@enterprise_id, 'OUTBOUND', DATE_FORMAT(CURRENT_DATE, '%Y%m%d'), 1, 'CK', 4),
    (@enterprise_id, 'RECEIPT', DATE_FORMAT(CURRENT_DATE, '%Y%m%d'), 1, 'SK', 4),
    (@enterprise_id, 'PAYMENT', DATE_FORMAT(CURRENT_DATE, '%Y%m%d'), 1, 'FK', 4);

-- =========================================================
-- 4. 商品、客户、供应商基础资料
-- =========================================================

INSERT IGNORE INTO md_unit
    (enterprise_id, unit_code, unit_name, precision_scale, status, sort_no, created_by)
VALUES
    (@enterprise_id, 'PCS', '件', 0, 'ENABLED', 1, @admin_id),
    (@enterprise_id, 'BOX', '箱', 0, 'ENABLED', 2, @admin_id),
    (@enterprise_id, 'PACK', '包', 0, 'ENABLED', 3, @admin_id),
    (@enterprise_id, 'BOTTLE', '瓶', 0, 'ENABLED', 4, @admin_id),
    (@enterprise_id, 'KG', '千克', 2, 'ENABLED', 5, @admin_id);

SET @unit_pcs = (SELECT id FROM md_unit WHERE enterprise_id = @enterprise_id AND unit_code = 'PCS' LIMIT 1);
SET @unit_box = (SELECT id FROM md_unit WHERE enterprise_id = @enterprise_id AND unit_code = 'BOX' LIMIT 1);
SET @unit_pack = (SELECT id FROM md_unit WHERE enterprise_id = @enterprise_id AND unit_code = 'PACK' LIMIT 1);
SET @unit_bottle = (SELECT id FROM md_unit WHERE enterprise_id = @enterprise_id AND unit_code = 'BOTTLE' LIMIT 1);

INSERT IGNORE INTO md_product_category
    (enterprise_id, parent_id, category_code, category_name, category_path, sort_no, status, created_by)
VALUES
    (@enterprise_id, 0, 'OFFICE', '办公用品', '/OFFICE', 1, 'ENABLED', @admin_id),
    (@enterprise_id, 0, 'FOOD', '食品饮料', '/FOOD', 2, 'ENABLED', @admin_id),
    (@enterprise_id, 0, 'CLEANING', '清洁日化', '/CLEANING', 3, 'ENABLED', @admin_id);

SET @category_office = (SELECT id FROM md_product_category WHERE enterprise_id = @enterprise_id AND category_code = 'OFFICE' LIMIT 1);
SET @category_food = (SELECT id FROM md_product_category WHERE enterprise_id = @enterprise_id AND category_code = 'FOOD' LIMIT 1);
SET @category_cleaning = (SELECT id FROM md_product_category WHERE enterprise_id = @enterprise_id AND category_code = 'CLEANING' LIMIT 1);

INSERT IGNORE INTO md_product
    (enterprise_id, product_code, product_name, barcode, category_id, unit_id, brand, specification,
     purchase_price, sale_price, cost_price, min_stock, max_stock, status, remark, created_by)
VALUES
    (@enterprise_id, 'P0001', 'A4 复印纸', '6900000000011', @category_office, @unit_box, '晨光', '70g，5包/箱', 18.50, 23.00, 18.50, 20, 300, 'ENABLED', '核心演示商品', @admin_id),
    (@enterprise_id, 'P0002', '黑色中性笔', '6900000000028', @category_office, @unit_box, '晨光', '0.5mm，12支/盒', 6.00, 9.00, 6.00, 20, 500, 'ENABLED', '核心演示商品', @admin_id),
    (@enterprise_id, 'P0003', '办公文件夹', '6900000000035', @category_office, @unit_pcs, '得力', 'A4 蓝色', 35.00, 49.00, 35.00, 10, 200, 'ENABLED', '核心演示商品', @admin_id),
    (@enterprise_id, 'P0004', '矿泉水', '6900000000042', @category_food, @unit_box, '农夫山泉', '550ml×24瓶', 28.00, 36.00, 28.00, 30, 500, 'ENABLED', NULL, @admin_id),
    (@enterprise_id, 'P0005', '原味薯片', '6900000000059', @category_food, @unit_pack, '乐事', '70g', 4.20, 6.50, 4.20, 30, 500, 'ENABLED', NULL, @admin_id),
    (@enterprise_id, 'P0006', '洗手液', '6900000000066', @category_cleaning, @unit_bottle, '蓝月亮', '500g', 10.00, 15.90, 10.00, 15, 300, 'ENABLED', NULL, @admin_id),
    (@enterprise_id, 'P0007', '抽纸', '6900000000073', @category_cleaning, @unit_pack, '维达', '3层×100抽', 8.00, 12.50, 8.00, 20, 400, 'ENABLED', NULL, @admin_id),
    (@enterprise_id, 'P0008', '垃圾袋', '6900000000080', @category_cleaning, @unit_pack, '妙洁', '45cm×50cm', 5.50, 8.90, 5.50, 20, 300, 'ENABLED', NULL, @admin_id);

SET @product_1 = (SELECT id FROM md_product WHERE enterprise_id = @enterprise_id AND product_code = 'P0001' LIMIT 1);
SET @product_2 = (SELECT id FROM md_product WHERE enterprise_id = @enterprise_id AND product_code = 'P0002' LIMIT 1);
SET @product_3 = (SELECT id FROM md_product WHERE enterprise_id = @enterprise_id AND product_code = 'P0003' LIMIT 1);
SET @product_4 = (SELECT id FROM md_product WHERE enterprise_id = @enterprise_id AND product_code = 'P0004' LIMIT 1);
SET @product_5 = (SELECT id FROM md_product WHERE enterprise_id = @enterprise_id AND product_code = 'P0005' LIMIT 1);

INSERT IGNORE INTO md_customer_category
    (enterprise_id, category_code, category_name, sort_no, status)
VALUES
    (@enterprise_id, 'RETAIL', '零售客户', 1, 'ENABLED'),
    (@enterprise_id, 'WHOLESALE', '批发客户', 2, 'ENABLED'),
    (@enterprise_id, 'ENTERPRISE', '企业客户', 3, 'ENABLED');

INSERT IGNORE INTO md_customer_level
    (enterprise_id, level_code, level_name, discount_rate, credit_limit, sort_no, status)
VALUES
    (@enterprise_id, 'NORMAL', '普通客户', 1.0000, 5000.00, 1, 'ENABLED'),
    (@enterprise_id, 'VIP', 'VIP 客户', 0.9500, 20000.00, 2, 'ENABLED'),
    (@enterprise_id, 'STRATEGIC', '战略客户', 0.9000, 100000.00, 3, 'ENABLED');

SET @customer_category_retail = (SELECT id FROM md_customer_category WHERE enterprise_id = @enterprise_id AND category_code = 'RETAIL' LIMIT 1);
SET @customer_category_wholesale = (SELECT id FROM md_customer_category WHERE enterprise_id = @enterprise_id AND category_code = 'WHOLESALE' LIMIT 1);
SET @customer_category_enterprise = (SELECT id FROM md_customer_category WHERE enterprise_id = @enterprise_id AND category_code = 'ENTERPRISE' LIMIT 1);
SET @customer_level_normal = (SELECT id FROM md_customer_level WHERE enterprise_id = @enterprise_id AND level_code = 'NORMAL' LIMIT 1);
SET @customer_level_vip = (SELECT id FROM md_customer_level WHERE enterprise_id = @enterprise_id AND level_code = 'VIP' LIMIT 1);
SET @customer_level_strategic = (SELECT id FROM md_customer_level WHERE enterprise_id = @enterprise_id AND level_code = 'STRATEGIC' LIMIT 1);

INSERT IGNORE INTO md_customer
    (enterprise_id, customer_code, customer_name, category_id, level_id, contact_name, contact_phone, email,
     address, credit_limit, payment_days, salesperson_id, status, remark, created_by)
VALUES
    (@enterprise_id, 'C0001', '上海启航科技有限公司', @customer_category_enterprise, @customer_level_vip, '陈经理', '13700000001', 'chen@demo.example.com', '上海市浦东新区启航路 1 号', 20000, 30, @sales_user_id, 'ENABLED', '已完成销售链路客户', @admin_id),
    (@enterprise_id, 'C0002', '星河便利店', @customer_category_retail, @customer_level_normal, '刘老板', '13700000002', NULL, '上海市徐汇区星河路 2 号', 5000, 7, @sales_user_id, 'ENABLED', '待出库销售单客户', @admin_id),
    (@enterprise_id, 'C0003', '远景商贸有限公司', @customer_category_wholesale, @customer_level_vip, '吴经理', '13700000003', NULL, '杭州市西湖区远景路 3 号', 30000, 30, @sales_user_id, 'ENABLED', NULL, @admin_id),
    (@enterprise_id, 'C0004', '阳光教育培训中心', @customer_category_enterprise, @customer_level_strategic, '郑老师', '13700000004', NULL, '苏州市工业园区阳光路 4 号', 100000, 45, @sales_user_id, 'ENABLED', NULL, @admin_id),
    (@enterprise_id, 'C0005', '社区团购演示客户', @customer_category_retail, @customer_level_normal, '王女士', '13700000005', NULL, '上海市浦东新区社区路 5 号', 3000, 0, @sales_user_id, 'ENABLED', NULL, @admin_id);

SET @customer_1 = (SELECT id FROM md_customer WHERE enterprise_id = @enterprise_id AND customer_code = 'C0001' LIMIT 1);
SET @customer_2 = (SELECT id FROM md_customer WHERE enterprise_id = @enterprise_id AND customer_code = 'C0002' LIMIT 1);

INSERT IGNORE INTO md_supplier_category
    (enterprise_id, category_code, category_name, sort_no, status)
VALUES
    (@enterprise_id, 'DIRECT', '厂家直供', 1, 'ENABLED'),
    (@enterprise_id, 'DISTRIBUTOR', '经销商', 2, 'ENABLED');

SET @supplier_category_direct = (SELECT id FROM md_supplier_category WHERE enterprise_id = @enterprise_id AND category_code = 'DIRECT' LIMIT 1);
SET @supplier_category_distributor = (SELECT id FROM md_supplier_category WHERE enterprise_id = @enterprise_id AND category_code = 'DISTRIBUTOR' LIMIT 1);

INSERT IGNORE INTO md_supplier
    (enterprise_id, supplier_code, supplier_name, category_id, contact_name, contact_phone, email, address,
     payment_days, status, remark, created_by)
VALUES
    (@enterprise_id, 'S0001', '上海优品办公用品有限公司', @supplier_category_direct, '黄经理', '13600000001', 'huang@demo.example.com', '上海市嘉定区供应路 1 号', 30, 'ENABLED', '已完成采购链路供应商', @admin_id),
    (@enterprise_id, 'S0002', '华东食品经销有限公司', @supplier_category_distributor, '郭经理', '13600000002', NULL, '南京市江宁区食品路 2 号', 15, 'ENABLED', '待入库采购单供应商', @admin_id),
    (@enterprise_id, 'S0003', '洁净日化用品厂', @supplier_category_direct, '林经理', '13600000003', NULL, '苏州市吴中区日化路 3 号', 30, 'ENABLED', NULL, @admin_id),
    (@enterprise_id, 'S0004', '长三角综合贸易商行', @supplier_category_distributor, '何经理', '13600000004', NULL, '杭州市余杭区贸易路 4 号', 7, 'ENABLED', NULL, @admin_id),
    (@enterprise_id, 'S0005', '演示备用供应商', @supplier_category_distributor, '孙经理', '13600000005', NULL, '上海市青浦区备用路 5 号', 0, 'ENABLED', NULL, @admin_id);

SET @supplier_1 = (SELECT id FROM md_supplier WHERE enterprise_id = @enterprise_id AND supplier_code = 'S0001' LIMIT 1);
SET @supplier_2 = (SELECT id FROM md_supplier WHERE enterprise_id = @enterprise_id AND supplier_code = 'S0002' LIMIT 1);

-- =========================================================
-- 5. 资金账户
-- =========================================================

-- 银行账户余额 = 期初 50000 - 已付款 2000 + 已收款 200 = 48200。
INSERT IGNORE INTO fin_account
    (enterprise_id, account_code, account_name, account_type, bank_name, account_number,
     opening_balance, current_balance, status, remark, created_by)
VALUES
    (@enterprise_id, 'BANK001', '工商银行基本户', 'BANK', '中国工商银行', '6222********0001', 50000.00, 48200.00, 'ENABLED', '主要演示结算账户', @admin_id),
    (@enterprise_id, 'CASH001', '门店现金账户', 'CASH', NULL, NULL, 5000.00, 5000.00, 'ENABLED', '门店备用金', @admin_id);

SET @bank_account = (SELECT id FROM fin_account WHERE enterprise_id = @enterprise_id AND account_code = 'BANK001' LIMIT 1);

-- =========================================================
-- 6. 已完成采购：采购单 -> 入库 -> 库存 -> 应付 -> 付款
-- =========================================================

INSERT IGNORE INTO pur_order
    (enterprise_id, store_id, order_no, order_date, supplier_id, warehouse_id, purchaser_id, status,
     settlement_status, total_quantity, total_amount, discount_amount, freight_amount, payable_amount,
     paid_amount, expected_arrival_date, approved_by, approved_at, remark, created_by)
VALUES
    (@enterprise_id, @store_1, 'CG202607150001', '2026-07-15', @supplier_1, @warehouse_1, @purchase_user_id,
     'COMPLETED', 'PARTIALLY_PAID', 230.0000, 4080.00, 0.00, 0.00, 4080.00, 2000.00,
     '2026-07-16', @admin_id, '2026-07-15 10:00:00', '演示：已入库且部分付款', @purchase_user_id);

SET @purchase_order_1 = (SELECT id FROM pur_order WHERE enterprise_id = @enterprise_id AND order_no = 'CG202607150001' LIMIT 1);

INSERT IGNORE INTO pur_order_item
    (order_id, line_no, product_id, product_code, product_name, specification, unit_id, quantity,
     unit_price, amount, inbound_quantity, remark)
VALUES
    (@purchase_order_1, 1, @product_1, 'P0001', 'A4 复印纸', '70g，5包/箱', @unit_box, 100.0000, 18.50, 1850.00, 100.0000, NULL),
    (@purchase_order_1, 2, @product_2, 'P0002', '黑色中性笔', '0.5mm，12支/盒', @unit_box, 80.0000, 6.00, 480.00, 80.0000, NULL),
    (@purchase_order_1, 3, @product_3, 'P0003', '办公文件夹', 'A4 蓝色', @unit_pcs, 50.0000, 35.00, 1750.00, 50.0000, NULL);

SET @purchase_item_1 = (SELECT id FROM pur_order_item WHERE order_id = @purchase_order_1 AND line_no = 1 LIMIT 1);
SET @purchase_item_2 = (SELECT id FROM pur_order_item WHERE order_id = @purchase_order_1 AND line_no = 2 LIMIT 1);
SET @purchase_item_3 = (SELECT id FROM pur_order_item WHERE order_id = @purchase_order_1 AND line_no = 3 LIMIT 1);

INSERT IGNORE INTO inv_inbound
    (enterprise_id, store_id, inbound_no, inbound_type, inbound_date, warehouse_id, source_type, source_id,
     source_no, supplier_id, status, total_quantity, total_amount, confirmed_by, confirmed_at, remark, created_by)
VALUES
    (@enterprise_id, @store_1, 'RK202607160001', 'PURCHASE', '2026-07-16', @warehouse_1, 'PURCHASE_ORDER',
     @purchase_order_1, 'CG202607150001', @supplier_1, 'CONFIRMED', 230.0000, 4080.00,
     @warehouse_user_id, '2026-07-16 09:30:00', '演示采购入库单', @warehouse_user_id);

SET @inbound_1 = (SELECT id FROM inv_inbound WHERE enterprise_id = @enterprise_id AND inbound_no = 'RK202607160001' LIMIT 1);

INSERT IGNORE INTO inv_inbound_item
    (inbound_id, line_no, source_item_id, product_id, product_code, product_name, specification, unit_id,
     quantity, unit_cost, amount, remark)
VALUES
    (@inbound_1, 1, @purchase_item_1, @product_1, 'P0001', 'A4 复印纸', '70g，5包/箱', @unit_box, 100.0000, 18.5000, 1850.00, NULL),
    (@inbound_1, 2, @purchase_item_2, @product_2, 'P0002', '黑色中性笔', '0.5mm，12支/盒', @unit_box, 80.0000, 6.0000, 480.00, NULL),
    (@inbound_1, 3, @purchase_item_3, @product_3, 'P0003', '办公文件夹', 'A4 蓝色', @unit_pcs, 50.0000, 35.0000, 1750.00, NULL);

INSERT IGNORE INTO inv_stock_movement
    (enterprise_id, store_id, warehouse_id, product_id, movement_no, movement_type, direction, quantity,
     unit_cost, amount, before_quantity, after_quantity, source_type, source_id, source_no, source_item_id,
     business_date, operator_id, remark)
VALUES
    (@enterprise_id, @store_1, @warehouse_1, @product_1, 'LS202607160001', 'PURCHASE_IN', 'IN', 100.0000, 18.5000, 1850.00, 0.0000, 100.0000, 'INBOUND', @inbound_1, 'RK202607160001', @purchase_item_1, '2026-07-16', @warehouse_user_id, '采购入库'),
    (@enterprise_id, @store_1, @warehouse_1, @product_2, 'LS202607160002', 'PURCHASE_IN', 'IN', 80.0000, 6.0000, 480.00, 0.0000, 80.0000, 'INBOUND', @inbound_1, 'RK202607160001', @purchase_item_2, '2026-07-16', @warehouse_user_id, '采购入库'),
    (@enterprise_id, @store_1, @warehouse_1, @product_3, 'LS202607160003', 'PURCHASE_IN', 'IN', 50.0000, 35.0000, 1750.00, 0.0000, 50.0000, 'INBOUND', @inbound_1, 'RK202607160001', @purchase_item_3, '2026-07-16', @warehouse_user_id, '采购入库');

INSERT IGNORE INTO fin_payable
    (enterprise_id, store_id, payable_no, supplier_id, source_type, source_id, source_no, business_date,
     due_date, original_amount, paid_amount, outstanding_amount, status, remark, created_by)
VALUES
    (@enterprise_id, @store_1, 'YF202607160001', @supplier_1, 'PURCHASE_ORDER', @purchase_order_1,
     'CG202607150001', '2026-07-16', '2026-08-15', 4080.00, 2000.00, 2080.00,
     'PARTIALLY_SETTLED', '采购入库自动生成应付', @finance_user_id);

SET @payable_1 = (SELECT id FROM fin_payable WHERE enterprise_id = @enterprise_id AND payable_no = 'YF202607160001' LIMIT 1);

INSERT IGNORE INTO fin_payment
    (enterprise_id, store_id, payment_no, payment_date, supplier_id, account_id, payment_method,
     payment_amount, allocated_amount, unallocated_amount, status, reference_no, confirmed_by, confirmed_at,
     remark, created_by)
VALUES
    (@enterprise_id, @store_1, 'FK202607160001', '2026-07-16', @supplier_1, @bank_account, 'BANK',
     2000.00, 2000.00, 0.00, 'CONFIRMED', 'DEMO-PAY-0001', @finance_user_id, '2026-07-16 15:00:00',
     '演示部分付款', @finance_user_id);

SET @payment_1 = (SELECT id FROM fin_payment WHERE enterprise_id = @enterprise_id AND payment_no = 'FK202607160001' LIMIT 1);

INSERT IGNORE INTO fin_payment_item (payment_id, payable_id, source_no, allocated_amount)
VALUES (@payment_1, @payable_1, 'CG202607150001', 2000.00);

INSERT IGNORE INTO fin_capital_flow
    (enterprise_id, store_id, account_id, flow_no, flow_date, flow_type, direction, amount, before_balance,
     after_balance, source_type, source_id, source_no, counterparty_type, counterparty_id, counterparty_name,
     operator_id, remark)
VALUES
    (@enterprise_id, @store_1, @bank_account, 'ZJ202607160001', '2026-07-16', 'PAYMENT', 'OUT', 2000.00,
     50000.00, 48000.00, 'PAYMENT', @payment_1, 'FK202607160001', 'SUPPLIER', @supplier_1,
     '上海优品办公用品有限公司', @finance_user_id, '采购付款资金流水');

-- =========================================================
-- 7. 已完成销售：销售单 -> 出库 -> 库存 -> 应收 -> 收款
-- =========================================================

INSERT IGNORE INTO sal_order
    (enterprise_id, store_id, order_no, order_date, customer_id, warehouse_id, salesperson_id, status,
     settlement_status, total_quantity, total_amount, discount_amount, freight_amount, payable_amount,
     received_amount, delivery_address, expected_delivery_date, approved_by, approved_at, remark, created_by)
VALUES
    (@enterprise_id, @store_1, 'XS202607160001', '2026-07-16', @customer_1, @warehouse_1, @sales_user_id,
     'COMPLETED', 'PARTIALLY_PAID', 15.0000, 275.00, 0.00, 0.00, 275.00, 200.00,
     '上海市浦东新区启航路 1 号', '2026-07-17', @admin_id, '2026-07-16 16:00:00',
     '演示：已出库且部分收款', @sales_user_id);

SET @sales_order_1 = (SELECT id FROM sal_order WHERE enterprise_id = @enterprise_id AND order_no = 'XS202607160001' LIMIT 1);

INSERT IGNORE INTO sal_order_item
    (order_id, line_no, product_id, product_code, product_name, specification, unit_id, quantity,
     unit_price, amount, outbound_quantity, remark)
VALUES
    (@sales_order_1, 1, @product_1, 'P0001', 'A4 复印纸', '70g，5包/箱', @unit_box, 10.0000, 23.00, 230.00, 10.0000, NULL),
    (@sales_order_1, 2, @product_2, 'P0002', '黑色中性笔', '0.5mm，12支/盒', @unit_box, 5.0000, 9.00, 45.00, 5.0000, NULL);

SET @sales_item_1 = (SELECT id FROM sal_order_item WHERE order_id = @sales_order_1 AND line_no = 1 LIMIT 1);
SET @sales_item_2 = (SELECT id FROM sal_order_item WHERE order_id = @sales_order_1 AND line_no = 2 LIMIT 1);

INSERT IGNORE INTO inv_outbound
    (enterprise_id, store_id, outbound_no, outbound_type, outbound_date, warehouse_id, source_type, source_id,
     source_no, customer_id, status, total_quantity, total_amount, confirmed_by, confirmed_at, remark, created_by)
VALUES
    (@enterprise_id, @store_1, 'CK202607170001', 'SALES', '2026-07-17', @warehouse_1, 'SALES_ORDER',
     @sales_order_1, 'XS202607160001', @customer_1, 'CONFIRMED', 15.0000, 215.00,
     @warehouse_user_id, '2026-07-17 09:00:00', '演示销售出库单，金额按成本计', @warehouse_user_id);

SET @outbound_1 = (SELECT id FROM inv_outbound WHERE enterprise_id = @enterprise_id AND outbound_no = 'CK202607170001' LIMIT 1);

INSERT IGNORE INTO inv_outbound_item
    (outbound_id, line_no, source_item_id, product_id, product_code, product_name, specification, unit_id,
     quantity, unit_cost, amount, remark)
VALUES
    (@outbound_1, 1, @sales_item_1, @product_1, 'P0001', 'A4 复印纸', '70g，5包/箱', @unit_box, 10.0000, 18.5000, 185.00, NULL),
    (@outbound_1, 2, @sales_item_2, @product_2, 'P0002', '黑色中性笔', '0.5mm，12支/盒', @unit_box, 5.0000, 6.0000, 30.00, NULL);

INSERT IGNORE INTO inv_stock_movement
    (enterprise_id, store_id, warehouse_id, product_id, movement_no, movement_type, direction, quantity,
     unit_cost, amount, before_quantity, after_quantity, source_type, source_id, source_no, source_item_id,
     business_date, operator_id, remark)
VALUES
    (@enterprise_id, @store_1, @warehouse_1, @product_1, 'LS202607170001', 'SALES_OUT', 'OUT', 10.0000, 18.5000, 185.00, 100.0000, 90.0000, 'OUTBOUND', @outbound_1, 'CK202607170001', @sales_item_1, '2026-07-17', @warehouse_user_id, '销售出库'),
    (@enterprise_id, @store_1, @warehouse_1, @product_2, 'LS202607170002', 'SALES_OUT', 'OUT', 5.0000, 6.0000, 30.00, 80.0000, 75.0000, 'OUTBOUND', @outbound_1, 'CK202607170001', @sales_item_2, '2026-07-17', @warehouse_user_id, '销售出库');

-- 当前库存 = 采购入库 - 销售出库。
INSERT IGNORE INTO inv_stock_balance
    (enterprise_id, warehouse_id, product_id, quantity, locked_quantity, available_quantity,
     avg_cost_price, stock_amount, last_movement_at)
VALUES
    (@enterprise_id, @warehouse_1, @product_1, 90.0000, 0.0000, 90.0000, 18.5000, 1665.00, '2026-07-17 09:00:00'),
    (@enterprise_id, @warehouse_1, @product_2, 75.0000, 0.0000, 75.0000, 6.0000, 450.00, '2026-07-17 09:00:00'),
    (@enterprise_id, @warehouse_1, @product_3, 50.0000, 0.0000, 50.0000, 35.0000, 1750.00, '2026-07-16 09:30:00');

INSERT IGNORE INTO fin_receivable
    (enterprise_id, store_id, receivable_no, customer_id, source_type, source_id, source_no, business_date,
     due_date, original_amount, received_amount, outstanding_amount, status, remark, created_by)
VALUES
    (@enterprise_id, @store_1, 'YS202607170001', @customer_1, 'SALES_ORDER', @sales_order_1,
     'XS202607160001', '2026-07-17', '2026-08-16', 275.00, 200.00, 75.00,
     'PARTIALLY_SETTLED', '销售出库自动生成应收', @finance_user_id);

SET @receivable_1 = (SELECT id FROM fin_receivable WHERE enterprise_id = @enterprise_id AND receivable_no = 'YS202607170001' LIMIT 1);

INSERT IGNORE INTO fin_receipt
    (enterprise_id, store_id, receipt_no, receipt_date, customer_id, account_id, payment_method,
     receipt_amount, allocated_amount, unallocated_amount, status, reference_no, confirmed_by, confirmed_at,
     remark, created_by)
VALUES
    (@enterprise_id, @store_1, 'SK202607170001', '2026-07-17', @customer_1, @bank_account, 'BANK',
     200.00, 200.00, 0.00, 'CONFIRMED', 'DEMO-RECEIPT-0001', @finance_user_id, '2026-07-17 11:00:00',
     '演示部分收款', @finance_user_id);

SET @receipt_1 = (SELECT id FROM fin_receipt WHERE enterprise_id = @enterprise_id AND receipt_no = 'SK202607170001' LIMIT 1);

INSERT IGNORE INTO fin_receipt_item (receipt_id, receivable_id, source_no, allocated_amount)
VALUES (@receipt_1, @receivable_1, 'XS202607160001', 200.00);

INSERT IGNORE INTO fin_capital_flow
    (enterprise_id, store_id, account_id, flow_no, flow_date, flow_type, direction, amount, before_balance,
     after_balance, source_type, source_id, source_no, counterparty_type, counterparty_id, counterparty_name,
     operator_id, remark)
VALUES
    (@enterprise_id, @store_1, @bank_account, 'ZJ202607170001', '2026-07-17', 'RECEIPT', 'IN', 200.00,
     48000.00, 48200.00, 'RECEIPT', @receipt_1, 'SK202607170001', 'CUSTOMER', @customer_1,
     '上海启航科技有限公司', @finance_user_id, '销售收款资金流水');

-- =========================================================
-- 8. 待处理业务单据（便于前端演示审核/入出库按钮）
-- =========================================================

INSERT IGNORE INTO pur_order
    (enterprise_id, store_id, order_no, order_date, supplier_id, warehouse_id, purchaser_id, status,
     settlement_status, total_quantity, total_amount, payable_amount, paid_amount,
     expected_arrival_date, approved_by, approved_at, remark, created_by)
VALUES
    (@enterprise_id, @store_1, 'CG202607170002', '2026-07-17', @supplier_2, @warehouse_1, @purchase_user_id,
     'APPROVED', 'UNPAID', 50.0000, 1400.00, 1400.00, 0.00, '2026-07-19',
     @admin_id, '2026-07-17 13:00:00', '演示：等待采购入库', @purchase_user_id);

SET @purchase_order_2 = (SELECT id FROM pur_order WHERE enterprise_id = @enterprise_id AND order_no = 'CG202607170002' LIMIT 1);

INSERT IGNORE INTO pur_order_item
    (order_id, line_no, product_id, product_code, product_name, specification, unit_id,
     quantity, unit_price, amount, inbound_quantity)
VALUES
    (@purchase_order_2, 1, @product_4, 'P0004', '矿泉水', '550ml×24瓶', @unit_box, 50.0000, 28.00, 1400.00, 0.0000);

INSERT IGNORE INTO sal_order
    (enterprise_id, store_id, order_no, order_date, customer_id, warehouse_id, salesperson_id, status,
     settlement_status, total_quantity, total_amount, payable_amount, received_amount,
     delivery_address, expected_delivery_date, approved_by, approved_at, remark, created_by)
VALUES
    (@enterprise_id, @store_1, 'XS202607170002', '2026-07-17', @customer_2, @warehouse_1, @sales_user_id,
     'APPROVED', 'UNPAID', 12.0000, 78.00, 78.00, 0.00, '上海市徐汇区星河路 2 号', '2026-07-18',
     @admin_id, '2026-07-17 14:00:00', '演示：等待销售出库', @sales_user_id);

SET @sales_order_2 = (SELECT id FROM sal_order WHERE enterprise_id = @enterprise_id AND order_no = 'XS202607170002' LIMIT 1);

INSERT IGNORE INTO sal_order_item
    (order_id, line_no, product_id, product_code, product_name, specification, unit_id,
     quantity, unit_price, amount, outbound_quantity)
VALUES
    (@sales_order_2, 1, @product_5, 'P0005', '原味薯片', '70g', @unit_pack, 12.0000, 6.50, 78.00, 0.0000);

-- =========================================================
-- 9. 公告及初始化日志
-- =========================================================

INSERT INTO sys_notice
    (enterprise_id, notice_title, notice_content, notice_type, publish_status, published_at, created_by)
SELECT @enterprise_id,
       '欢迎使用七尾云进销存 ERP 演示系统',
       '当前账套已初始化商品、客户、供应商、采购、销售、库存和资金演示数据。默认账号仅用于实训，请尽快修改密码。',
       'SYSTEM', 'PUBLISHED', '2026-07-17 09:00:00', @admin_id
WHERE NOT EXISTS (
    SELECT 1 FROM sys_notice
    WHERE enterprise_id = @enterprise_id AND notice_title = '欢迎使用七尾云进销存 ERP 演示系统' AND deleted = 0
);

INSERT INTO sys_operation_log
    (enterprise_id, user_id, username, module_name, operation_type, request_method, request_uri,
     response_code, ip_address, duration_ms, success)
SELECT @enterprise_id, @admin_id, 'admin', '系统初始化', 'SEED_DATA', 'SYSTEM', '/sql/V2__seed_demo_data.sql',
       '200', '127.0.0.1', 0, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_operation_log
    WHERE enterprise_id = @enterprise_id AND operation_type = 'SEED_DATA'
);

COMMIT;
SET FOREIGN_KEY_CHECKS = 1;

-- 执行后可用以下语句快速核对：
-- SELECT username, real_name, status FROM sys_user WHERE enterprise_id = @enterprise_id;
-- SELECT p.product_code, p.product_name, b.quantity, b.available_quantity, b.stock_amount
-- FROM inv_stock_balance b JOIN md_product p ON p.id = b.product_id WHERE b.enterprise_id = @enterprise_id;
-- SELECT payable_no, original_amount, paid_amount, outstanding_amount, status FROM fin_payable WHERE enterprise_id = @enterprise_id;
-- SELECT receivable_no, original_amount, received_amount, outstanding_amount, status FROM fin_receivable WHERE enterprise_id = @enterprise_id;
