-- B2B 云进销存 ERP 系统数据库结构 V1
-- MySQL 8.0+
-- 说明：本脚本不包含 CREATE DATABASE / USE，请先选择目标数据库再执行。
-- 设计策略：使用逻辑外键，暂不创建物理外键，便于一周内并行开发和后续调整。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 1. 组织与系统权限
-- =========================================================

CREATE TABLE org_enterprise (
    id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '企业ID',
    enterprise_code VARCHAR(50) NOT NULL COMMENT '企业编码',
    enterprise_name VARCHAR(100) NOT NULL COMMENT '企业名称',
    contact_name    VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    contact_phone   VARCHAR(30) DEFAULT NULL COMMENT '联系电话',
    address         VARCHAR(255) DEFAULT NULL COMMENT '企业地址',
    logo_url        VARCHAR(500) DEFAULT NULL COMMENT 'Logo地址',
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_enterprise_code (enterprise_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业信息';

CREATE TABLE org_store (
    id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '门店ID',
    enterprise_id   BIGINT NOT NULL COMMENT '企业ID',
    store_code      VARCHAR(50) NOT NULL COMMENT '门店编码',
    store_name      VARCHAR(100) NOT NULL COMMENT '门店名称',
    manager_name    VARCHAR(50) DEFAULT NULL COMMENT '负责人',
    contact_phone   VARCHAR(30) DEFAULT NULL COMMENT '联系电话',
    address         VARCHAR(255) DEFAULT NULL COMMENT '地址',
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_store_code (enterprise_id, store_code),
    KEY idx_org_store_enterprise (enterprise_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门店';

CREATE TABLE org_warehouse (
    id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '仓库ID',
    enterprise_id   BIGINT NOT NULL COMMENT '企业ID',
    store_id        BIGINT DEFAULT NULL COMMENT '所属门店ID，空表示总部仓',
    warehouse_code  VARCHAR(50) NOT NULL COMMENT '仓库编码',
    warehouse_name  VARCHAR(100) NOT NULL COMMENT '仓库名称',
    warehouse_type  VARCHAR(30) NOT NULL DEFAULT 'NORMAL' COMMENT '类型：NORMAL/STORE/RETURN/DEFECTIVE',
    manager_name    VARCHAR(50) DEFAULT NULL COMMENT '负责人',
    contact_phone   VARCHAR(30) DEFAULT NULL,
    address         VARCHAR(255) DEFAULT NULL,
    allow_negative  TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许负库存',
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_warehouse_code (enterprise_id, warehouse_code),
    KEY idx_org_warehouse_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仓库';

CREATE TABLE sys_user (
    id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    enterprise_id   BIGINT NOT NULL COMMENT '企业ID',
    username        VARCHAR(50) NOT NULL COMMENT '登录名',
    password_hash   VARCHAR(255) NOT NULL COMMENT '密码哈希',
    real_name       VARCHAR(50) NOT NULL COMMENT '真实姓名',
    phone           VARCHAR(30) DEFAULT NULL,
    email           VARCHAR(100) DEFAULT NULL,
    avatar_url      VARCHAR(500) DEFAULT NULL,
    default_store_id BIGINT DEFAULT NULL COMMENT '默认门店',
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    last_login_at   DATETIME DEFAULT NULL,
    last_login_ip   VARCHAR(64) DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (enterprise_id, username),
    KEY idx_sys_user_phone (phone),
    KEY idx_sys_user_store (default_store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户';

CREATE TABLE sys_role (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    role_code       VARCHAR(50) NOT NULL,
    role_name       VARCHAR(100) NOT NULL,
    data_scope      VARCHAR(30) NOT NULL DEFAULT 'SELF' COMMENT 'ALL/STORE/WAREHOUSE/SELF',
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    sort_no         INT NOT NULL DEFAULT 0,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (enterprise_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色';

CREATE TABLE sys_menu (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    parent_id       BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    menu_type       VARCHAR(20) NOT NULL COMMENT 'DIRECTORY/MENU/BUTTON',
    menu_name       VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    route_path      VARCHAR(255) DEFAULT NULL,
    component_path  VARCHAR(255) DEFAULT NULL,
    icon            VARCHAR(100) DEFAULT NULL,
    sort_no         INT NOT NULL DEFAULT 0,
    visible         TINYINT NOT NULL DEFAULT 1,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_menu_permission (permission_code),
    KEY idx_sys_menu_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单及按钮权限';

CREATE TABLE sys_user_role (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_role (user_id, role_id),
    KEY idx_sys_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联';

CREATE TABLE sys_role_menu (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    role_id         BIGINT NOT NULL,
    menu_id         BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_menu (role_id, menu_id),
    KEY idx_sys_role_menu_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联';

CREATE TABLE sys_user_store (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    store_id        BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_store (user_id, store_id),
    KEY idx_sys_user_store_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户门店数据权限';

CREATE TABLE sys_user_warehouse (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    warehouse_id    BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_warehouse (user_id, warehouse_id),
    KEY idx_sys_user_warehouse_warehouse (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户仓库数据权限';

CREATE TABLE sys_operation_log (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    user_id         BIGINT DEFAULT NULL,
    username        VARCHAR(50) DEFAULT NULL,
    module_name     VARCHAR(100) DEFAULT NULL,
    operation_type  VARCHAR(50) DEFAULT NULL,
    request_method  VARCHAR(10) DEFAULT NULL,
    request_uri     VARCHAR(500) DEFAULT NULL,
    request_params  TEXT NULL,
    response_code   VARCHAR(50) DEFAULT NULL,
    ip_address      VARCHAR(64) DEFAULT NULL,
    duration_ms     BIGINT DEFAULT NULL,
    success         TINYINT NOT NULL DEFAULT 1,
    error_message   TEXT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sys_operation_user_time (user_id, created_at),
    KEY idx_sys_operation_module_time (module_name, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志';

CREATE TABLE sys_notice (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    notice_title    VARCHAR(200) NOT NULL,
    notice_content  TEXT NOT NULL,
    notice_type     VARCHAR(30) NOT NULL DEFAULT 'SYSTEM',
    publish_status  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at    DATETIME DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_sys_notice_status_time (publish_status, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息通知';

CREATE TABLE sys_document_sequence (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    document_type   VARCHAR(50) NOT NULL COMMENT '单据类型',
    date_part       VARCHAR(8) NOT NULL COMMENT '日期部分yyyyMMdd',
    current_value   BIGINT NOT NULL DEFAULT 0,
    prefix          VARCHAR(20) NOT NULL,
    sequence_length INT NOT NULL DEFAULT 4,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_document_sequence (enterprise_id, document_type, date_part)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务单号序列';

CREATE TABLE sys_dict_type (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    dict_code       VARCHAR(100) NOT NULL,
    dict_name       VARCHAR(100) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    remark          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_dict_type_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型';

CREATE TABLE sys_dict_item (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    dict_type_id    BIGINT NOT NULL,
    item_value      VARCHAR(100) NOT NULL,
    item_label      VARCHAR(100) NOT NULL,
    sort_no         INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    remark          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_dict_item (dict_type_id, item_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典项';

-- =========================================================
-- 2. 基础资料
-- =========================================================

CREATE TABLE md_unit (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    unit_code       VARCHAR(50) NOT NULL,
    unit_name       VARCHAR(50) NOT NULL,
    precision_scale INT NOT NULL DEFAULT 0 COMMENT '数量小数位',
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    sort_no         INT NOT NULL DEFAULT 0,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_unit_code (enterprise_id, unit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品单位';

CREATE TABLE md_product_category (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    parent_id       BIGINT NOT NULL DEFAULT 0,
    category_code   VARCHAR(50) NOT NULL,
    category_name   VARCHAR(100) NOT NULL,
    category_path   VARCHAR(500) DEFAULT NULL COMMENT '层级路径',
    sort_no         INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_product_category_code (enterprise_id, category_code),
    KEY idx_md_product_category_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类';

CREATE TABLE md_product (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    product_code    VARCHAR(50) NOT NULL COMMENT '商品编码/SKU',
    product_name    VARCHAR(200) NOT NULL,
    barcode         VARCHAR(100) DEFAULT NULL,
    category_id     BIGINT NOT NULL,
    unit_id         BIGINT NOT NULL,
    brand           VARCHAR(100) DEFAULT NULL,
    specification   VARCHAR(200) DEFAULT NULL,
    model_no        VARCHAR(100) DEFAULT NULL,
    purchase_price  DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    sale_price      DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    cost_price      DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    min_stock       DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    max_stock       DECIMAL(18,4) DEFAULT NULL,
    image_url       VARCHAR(500) DEFAULT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_product_code (enterprise_id, product_code),
    KEY idx_md_product_barcode (barcode),
    KEY idx_md_product_category (category_id),
    KEY idx_md_product_name (product_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品';

CREATE TABLE md_product_attribute (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    attribute_code  VARCHAR(50) NOT NULL,
    attribute_name  VARCHAR(100) NOT NULL,
    input_type      VARCHAR(20) NOT NULL DEFAULT 'SELECT' COMMENT 'SELECT/TEXT/NUMBER',
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    sort_no         INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_product_attribute_code (enterprise_id, attribute_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品属性';

CREATE TABLE md_product_attribute_value (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    attribute_id    BIGINT NOT NULL,
    value_code      VARCHAR(50) NOT NULL,
    value_name      VARCHAR(100) NOT NULL,
    sort_no         INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_attribute_value (attribute_id, value_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品属性值';

CREATE TABLE md_product_attribute_relation (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    product_id      BIGINT NOT NULL,
    attribute_id    BIGINT NOT NULL,
    attribute_value_id BIGINT DEFAULT NULL,
    custom_value    VARCHAR(255) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_product_attribute_relation (product_id, attribute_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品属性关联';

CREATE TABLE md_product_tag (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    tag_name        VARCHAR(50) NOT NULL,
    tag_color       VARCHAR(20) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_product_tag_name (enterprise_id, tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品标签';

CREATE TABLE md_product_tag_relation (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    product_id      BIGINT NOT NULL,
    tag_id          BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_product_tag_relation (product_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品标签关联';

CREATE TABLE md_customer_category (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    category_code   VARCHAR(50) NOT NULL,
    category_name   VARCHAR(100) NOT NULL,
    sort_no         INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_customer_category_code (enterprise_id, category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户分类';

CREATE TABLE md_customer_level (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    level_code      VARCHAR(50) NOT NULL,
    level_name      VARCHAR(100) NOT NULL,
    discount_rate   DECIMAL(8,4) NOT NULL DEFAULT 1.0000 COMMENT '折扣率',
    credit_limit    DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    sort_no         INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_customer_level_code (enterprise_id, level_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户等级';

CREATE TABLE md_customer (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    customer_code   VARCHAR(50) NOT NULL,
    customer_name   VARCHAR(200) NOT NULL,
    category_id     BIGINT DEFAULT NULL,
    level_id        BIGINT DEFAULT NULL,
    contact_name    VARCHAR(50) DEFAULT NULL,
    contact_phone   VARCHAR(30) DEFAULT NULL,
    email           VARCHAR(100) DEFAULT NULL,
    address         VARCHAR(255) DEFAULT NULL,
    tax_no          VARCHAR(100) DEFAULT NULL,
    bank_name       VARCHAR(100) DEFAULT NULL,
    bank_account    VARCHAR(100) DEFAULT NULL,
    credit_limit    DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    payment_days    INT NOT NULL DEFAULT 0 COMMENT '账期天数',
    salesperson_id  BIGINT DEFAULT NULL COMMENT '负责员工',
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_customer_code (enterprise_id, customer_code),
    KEY idx_md_customer_name (customer_name),
    KEY idx_md_customer_category (category_id),
    KEY idx_md_customer_level (level_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户';

CREATE TABLE md_customer_tag (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    tag_name        VARCHAR(50) NOT NULL,
    tag_color       VARCHAR(20) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_customer_tag_name (enterprise_id, tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户标签';

CREATE TABLE md_customer_tag_relation (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    customer_id     BIGINT NOT NULL,
    tag_id          BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_customer_tag_relation (customer_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户标签关联';

CREATE TABLE md_supplier_category (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    category_code   VARCHAR(50) NOT NULL,
    category_name   VARCHAR(100) NOT NULL,
    sort_no         INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_supplier_category_code (enterprise_id, category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='供应商分类';

CREATE TABLE md_supplier (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    supplier_code   VARCHAR(50) NOT NULL,
    supplier_name   VARCHAR(200) NOT NULL,
    category_id     BIGINT DEFAULT NULL,
    contact_name    VARCHAR(50) DEFAULT NULL,
    contact_phone   VARCHAR(30) DEFAULT NULL,
    email           VARCHAR(100) DEFAULT NULL,
    address         VARCHAR(255) DEFAULT NULL,
    tax_no          VARCHAR(100) DEFAULT NULL,
    bank_name       VARCHAR(100) DEFAULT NULL,
    bank_account    VARCHAR(100) DEFAULT NULL,
    payment_days    INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_md_supplier_code (enterprise_id, supplier_code),
    KEY idx_md_supplier_name (supplier_name),
    KEY idx_md_supplier_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='供应商';

-- =========================================================
-- 3. 销售业务
-- =========================================================

CREATE TABLE sal_order (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    store_id        BIGINT NOT NULL,
    order_no        VARCHAR(50) NOT NULL COMMENT '销售单号',
    order_date      DATE NOT NULL,
    customer_id     BIGINT NOT NULL,
    warehouse_id    BIGINT NOT NULL COMMENT '默认出库仓库',
    salesperson_id  BIGINT DEFAULT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/APPROVED/PARTIALLY_OUTBOUND/COMPLETED/CANCELLED',
    settlement_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PARTIALLY_PAID/PAID',
    total_quantity  DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    total_amount    DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    freight_amount  DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    payable_amount  DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '应收金额',
    received_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '已收金额',
    delivery_address VARCHAR(255) DEFAULT NULL,
    expected_delivery_date DATE DEFAULT NULL,
    approved_by     BIGINT DEFAULT NULL,
    approved_at     DATETIME DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sal_order_no (enterprise_id, order_no),
    KEY idx_sal_order_customer_date (customer_id, order_date),
    KEY idx_sal_order_store_status (store_id, status),
    KEY idx_sal_order_warehouse (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售单';

CREATE TABLE sal_order_item (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    order_id        BIGINT NOT NULL,
    line_no         INT NOT NULL,
    product_id      BIGINT NOT NULL,
    product_code    VARCHAR(50) NOT NULL COMMENT '商品编码快照',
    product_name    VARCHAR(200) NOT NULL COMMENT '商品名称快照',
    specification   VARCHAR(200) DEFAULT NULL,
    unit_id         BIGINT NOT NULL,
    quantity        DECIMAL(18,4) NOT NULL,
    unit_price      DECIMAL(18,2) NOT NULL,
    discount_rate   DECIMAL(8,4) NOT NULL DEFAULT 1.0000,
    tax_rate        DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
    amount          DECIMAL(18,2) NOT NULL,
    outbound_quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    returned_quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    remark          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sal_order_item_line (order_id, line_no),
    KEY idx_sal_order_item_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售单明细';

CREATE TABLE sal_return (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    store_id        BIGINT NOT NULL,
    return_no       VARCHAR(50) NOT NULL,
    return_date     DATE NOT NULL,
    sales_order_id  BIGINT DEFAULT NULL COMMENT '原销售单ID',
    customer_id     BIGINT NOT NULL,
    warehouse_id    BIGINT NOT NULL COMMENT '退货入库仓库',
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/APPROVED/COMPLETED/CANCELLED',
    total_quantity  DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    total_amount    DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    refund_amount   DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    return_reason   VARCHAR(500) DEFAULT NULL,
    approved_by     BIGINT DEFAULT NULL,
    approved_at     DATETIME DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sal_return_no (enterprise_id, return_no),
    KEY idx_sal_return_order (sales_order_id),
    KEY idx_sal_return_customer_date (customer_id, return_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售退货申请单';

CREATE TABLE sal_return_item (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    return_id       BIGINT NOT NULL,
    line_no         INT NOT NULL,
    sales_order_item_id BIGINT DEFAULT NULL,
    product_id      BIGINT NOT NULL,
    product_code    VARCHAR(50) NOT NULL,
    product_name    VARCHAR(200) NOT NULL,
    specification   VARCHAR(200) DEFAULT NULL,
    unit_id         BIGINT NOT NULL,
    quantity        DECIMAL(18,4) NOT NULL,
    unit_price      DECIMAL(18,2) NOT NULL,
    amount          DECIMAL(18,2) NOT NULL,
    inbound_quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    remark          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sal_return_item_line (return_id, line_no),
    KEY idx_sal_return_item_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售退货明细';

-- =========================================================
-- 4. 采购业务
-- =========================================================

CREATE TABLE pur_order (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    store_id        BIGINT NOT NULL,
    order_no        VARCHAR(50) NOT NULL COMMENT '采购单号',
    order_date      DATE NOT NULL,
    supplier_id     BIGINT NOT NULL,
    warehouse_id    BIGINT NOT NULL COMMENT '默认入库仓库',
    purchaser_id    BIGINT DEFAULT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/APPROVED/PARTIALLY_INBOUND/COMPLETED/CANCELLED',
    settlement_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    total_quantity  DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    total_amount    DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    freight_amount  DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    payable_amount  DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '应付金额',
    paid_amount     DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '已付金额',
    expected_arrival_date DATE DEFAULT NULL,
    approved_by     BIGINT DEFAULT NULL,
    approved_at     DATETIME DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pur_order_no (enterprise_id, order_no),
    KEY idx_pur_order_supplier_date (supplier_id, order_date),
    KEY idx_pur_order_store_status (store_id, status),
    KEY idx_pur_order_warehouse (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购单';

CREATE TABLE pur_order_item (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    order_id        BIGINT NOT NULL,
    line_no         INT NOT NULL,
    product_id      BIGINT NOT NULL,
    product_code    VARCHAR(50) NOT NULL,
    product_name    VARCHAR(200) NOT NULL,
    specification   VARCHAR(200) DEFAULT NULL,
    unit_id         BIGINT NOT NULL,
    quantity        DECIMAL(18,4) NOT NULL,
    unit_price      DECIMAL(18,2) NOT NULL,
    discount_rate   DECIMAL(8,4) NOT NULL DEFAULT 1.0000,
    tax_rate        DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
    amount          DECIMAL(18,2) NOT NULL,
    inbound_quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    returned_quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    remark          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pur_order_item_line (order_id, line_no),
    KEY idx_pur_order_item_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购单明细';

CREATE TABLE pur_return (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    store_id        BIGINT NOT NULL,
    return_no       VARCHAR(50) NOT NULL,
    return_date     DATE NOT NULL,
    purchase_order_id BIGINT DEFAULT NULL,
    supplier_id     BIGINT NOT NULL,
    warehouse_id    BIGINT NOT NULL COMMENT '退货出库仓库',
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    total_quantity  DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    total_amount    DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    refund_amount   DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    return_reason   VARCHAR(500) DEFAULT NULL,
    approved_by     BIGINT DEFAULT NULL,
    approved_at     DATETIME DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pur_return_no (enterprise_id, return_no),
    KEY idx_pur_return_order (purchase_order_id),
    KEY idx_pur_return_supplier_date (supplier_id, return_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购退货申请单';

CREATE TABLE pur_return_item (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    return_id       BIGINT NOT NULL,
    line_no         INT NOT NULL,
    purchase_order_item_id BIGINT DEFAULT NULL,
    product_id      BIGINT NOT NULL,
    product_code    VARCHAR(50) NOT NULL,
    product_name    VARCHAR(200) NOT NULL,
    specification   VARCHAR(200) DEFAULT NULL,
    unit_id         BIGINT NOT NULL,
    quantity        DECIMAL(18,4) NOT NULL,
    unit_price      DECIMAL(18,2) NOT NULL,
    amount          DECIMAL(18,2) NOT NULL,
    outbound_quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    remark          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pur_return_item_line (return_id, line_no),
    KEY idx_pur_return_item_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购退货明细';

-- =========================================================
-- 5. 库存业务
-- =========================================================

CREATE TABLE inv_stock_balance (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    warehouse_id    BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    quantity        DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '实际库存',
    locked_quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '锁定库存',
    available_quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '可用库存',
    avg_cost_price  DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '移动平均成本',
    stock_amount    DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    last_movement_at DATETIME DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_stock_balance (enterprise_id, warehouse_id, product_id),
    KEY idx_inv_stock_balance_product (product_id),
    KEY idx_inv_stock_balance_available (warehouse_id, available_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存余额';

CREATE TABLE inv_stock_movement (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    store_id        BIGINT DEFAULT NULL,
    warehouse_id    BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    movement_no     VARCHAR(50) NOT NULL COMMENT '流水号',
    movement_type   VARCHAR(50) NOT NULL COMMENT 'PURCHASE_IN/SALES_OUT/SALES_RETURN_IN/PURCHASE_RETURN_OUT/TRANSFER_IN/TRANSFER_OUT/COUNT/ADJUST/BORROW_IN/BORROW_OUT',
    direction       VARCHAR(10) NOT NULL COMMENT 'IN/OUT',
    quantity        DECIMAL(18,4) NOT NULL COMMENT '正数数量',
    unit_cost       DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    amount          DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    before_quantity DECIMAL(18,4) NOT NULL,
    after_quantity  DECIMAL(18,4) NOT NULL,
    source_type     VARCHAR(50) NOT NULL COMMENT '来源单据类型',
    source_id       BIGINT NOT NULL COMMENT '来源单据ID',
    source_no       VARCHAR(50) NOT NULL COMMENT '来源单号',
    source_item_id  BIGINT DEFAULT NULL,
    business_date   DATE NOT NULL,
    operator_id     BIGINT DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_stock_movement_no (enterprise_id, movement_no),
    KEY idx_inv_movement_product_time (product_id, created_at),
    KEY idx_inv_movement_warehouse_time (warehouse_id, created_at),
    KEY idx_inv_movement_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存流水';

CREATE TABLE inv_inbound (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    store_id        BIGINT DEFAULT NULL,
    inbound_no      VARCHAR(50) NOT NULL,
    inbound_type    VARCHAR(50) NOT NULL COMMENT 'PURCHASE/SALES_RETURN/TRANSFER/BORROW/ADJUST/OTHER',
    inbound_date    DATE NOT NULL,
    warehouse_id    BIGINT NOT NULL,
    source_type     VARCHAR(50) DEFAULT NULL,
    source_id       BIGINT DEFAULT NULL,
    source_no       VARCHAR(50) DEFAULT NULL,
    supplier_id     BIGINT DEFAULT NULL,
    customer_id     BIGINT DEFAULT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CANCELLED',
    total_quantity  DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    total_amount    DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    confirmed_by    BIGINT DEFAULT NULL,
    confirmed_at    DATETIME DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_inbound_no (enterprise_id, inbound_no),
    KEY idx_inv_inbound_warehouse_date (warehouse_id, inbound_date),
    KEY idx_inv_inbound_source (source_type, source_id),
    KEY idx_inv_inbound_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='入库单';

CREATE TABLE inv_inbound_item (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    inbound_id      BIGINT NOT NULL,
    line_no         INT NOT NULL,
    source_item_id  BIGINT DEFAULT NULL,
    product_id      BIGINT NOT NULL,
    product_code    VARCHAR(50) NOT NULL,
    product_name    VARCHAR(200) NOT NULL,
    specification   VARCHAR(200) DEFAULT NULL,
    unit_id         BIGINT NOT NULL,
    quantity        DECIMAL(18,4) NOT NULL,
    unit_cost       DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    amount          DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    batch_no        VARCHAR(100) DEFAULT NULL,
    production_date DATE DEFAULT NULL,
    expiry_date     DATE DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_inbound_item_line (inbound_id, line_no),
    KEY idx_inv_inbound_item_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='入库单明细';

CREATE TABLE inv_outbound (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    store_id        BIGINT DEFAULT NULL,
    outbound_no     VARCHAR(50) NOT NULL,
    outbound_type   VARCHAR(50) NOT NULL COMMENT 'SALES/PURCHASE_RETURN/TRANSFER/BORROW/ADJUST/OTHER',
    outbound_date   DATE NOT NULL,
    warehouse_id    BIGINT NOT NULL,
    source_type     VARCHAR(50) DEFAULT NULL,
    source_id       BIGINT DEFAULT NULL,
    source_no       VARCHAR(50) DEFAULT NULL,
    supplier_id     BIGINT DEFAULT NULL,
    customer_id     BIGINT DEFAULT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    total_quantity  DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    total_amount    DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    confirmed_by    BIGINT DEFAULT NULL,
    confirmed_at    DATETIME DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_outbound_no (enterprise_id, outbound_no),
    KEY idx_inv_outbound_warehouse_date (warehouse_id, outbound_date),
    KEY idx_inv_outbound_source (source_type, source_id),
    KEY idx_inv_outbound_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出库单';

CREATE TABLE inv_outbound_item (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    outbound_id     BIGINT NOT NULL,
    line_no         INT NOT NULL,
    source_item_id  BIGINT DEFAULT NULL,
    product_id      BIGINT NOT NULL,
    product_code    VARCHAR(50) NOT NULL,
    product_name    VARCHAR(200) NOT NULL,
    specification   VARCHAR(200) DEFAULT NULL,
    unit_id         BIGINT NOT NULL,
    quantity        DECIMAL(18,4) NOT NULL,
    unit_cost       DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    amount          DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    batch_no        VARCHAR(100) DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_outbound_item_line (outbound_id, line_no),
    KEY idx_inv_outbound_item_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出库单明细';

CREATE TABLE inv_transfer (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    transfer_no     VARCHAR(50) NOT NULL,
    transfer_date   DATE NOT NULL,
    from_warehouse_id BIGINT NOT NULL,
    to_warehouse_id BIGINT NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/APPROVED/OUTBOUND/COMPLETED/CANCELLED',
    total_quantity  DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    approved_by     BIGINT DEFAULT NULL,
    approved_at     DATETIME DEFAULT NULL,
    completed_at    DATETIME DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_transfer_no (enterprise_id, transfer_no),
    KEY idx_inv_transfer_from_date (from_warehouse_id, transfer_date),
    KEY idx_inv_transfer_to_date (to_warehouse_id, transfer_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存调拨单';

CREATE TABLE inv_transfer_item (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    transfer_id     BIGINT NOT NULL,
    line_no         INT NOT NULL,
    product_id      BIGINT NOT NULL,
    product_code    VARCHAR(50) NOT NULL,
    product_name    VARCHAR(200) NOT NULL,
    unit_id         BIGINT NOT NULL,
    quantity        DECIMAL(18,4) NOT NULL,
    outbound_quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    inbound_quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    remark          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_transfer_item_line (transfer_id, line_no),
    KEY idx_inv_transfer_item_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存调拨明细';

CREATE TABLE inv_count (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    count_no        VARCHAR(50) NOT NULL,
    count_date      DATE NOT NULL,
    warehouse_id    BIGINT NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/COUNTING/APPROVED/COMPLETED/CANCELLED',
    total_book_quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    total_actual_quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    total_diff_quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    approved_by     BIGINT DEFAULT NULL,
    approved_at     DATETIME DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_count_no (enterprise_id, count_no),
    KEY idx_inv_count_warehouse_date (warehouse_id, count_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存盘点单';

CREATE TABLE inv_count_item (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    count_id        BIGINT NOT NULL,
    line_no         INT NOT NULL,
    product_id      BIGINT NOT NULL,
    product_code    VARCHAR(50) NOT NULL,
    product_name    VARCHAR(200) NOT NULL,
    unit_id         BIGINT NOT NULL,
    book_quantity   DECIMAL(18,4) NOT NULL,
    actual_quantity DECIMAL(18,4) NOT NULL,
    diff_quantity   DECIMAL(18,4) NOT NULL,
    unit_cost       DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    diff_amount     DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    reason          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_count_item_line (count_id, line_no),
    KEY idx_inv_count_item_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存盘点明细';

CREATE TABLE inv_adjustment (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    adjustment_no   VARCHAR(50) NOT NULL,
    adjustment_date DATE NOT NULL,
    warehouse_id    BIGINT NOT NULL,
    adjustment_type VARCHAR(20) NOT NULL COMMENT 'INCREASE/DECREASE',
    source_type     VARCHAR(50) DEFAULT NULL COMMENT 'COUNT/MANUAL/OTHER',
    source_id       BIGINT DEFAULT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    total_quantity  DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    total_amount    DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    approved_by     BIGINT DEFAULT NULL,
    approved_at     DATETIME DEFAULT NULL,
    reason          VARCHAR(500) DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_adjustment_no (enterprise_id, adjustment_no),
    KEY idx_inv_adjustment_warehouse_date (warehouse_id, adjustment_date),
    KEY idx_inv_adjustment_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存调整单';

CREATE TABLE inv_adjustment_item (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    adjustment_id   BIGINT NOT NULL,
    line_no         INT NOT NULL,
    product_id      BIGINT NOT NULL,
    product_code    VARCHAR(50) NOT NULL,
    product_name    VARCHAR(200) NOT NULL,
    unit_id         BIGINT NOT NULL,
    quantity        DECIMAL(18,4) NOT NULL,
    unit_cost       DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    amount          DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    reason          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_adjustment_item_line (adjustment_id, line_no),
    KEY idx_inv_adjustment_item_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存调整明细';

CREATE TABLE inv_borrow (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    borrow_no       VARCHAR(50) NOT NULL,
    borrow_type     VARCHAR(20) NOT NULL COMMENT 'BORROW_IN/BORROW_OUT',
    borrow_date     DATE NOT NULL,
    expected_return_date DATE DEFAULT NULL,
    warehouse_id    BIGINT NOT NULL,
    partner_type    VARCHAR(20) NOT NULL COMMENT 'CUSTOMER/SUPPLIER/OTHER',
    partner_id      BIGINT DEFAULT NULL,
    partner_name    VARCHAR(200) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/APPROVED/PARTIALLY_RETURNED/COMPLETED/CANCELLED',
    total_quantity  DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    approved_by     BIGINT DEFAULT NULL,
    approved_at     DATETIME DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_borrow_no (enterprise_id, borrow_no),
    KEY idx_inv_borrow_warehouse_date (warehouse_id, borrow_date),
    KEY idx_inv_borrow_partner (partner_type, partner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='借入借出单';

CREATE TABLE inv_borrow_item (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    borrow_id       BIGINT NOT NULL,
    line_no         INT NOT NULL,
    product_id      BIGINT NOT NULL,
    product_code    VARCHAR(50) NOT NULL,
    product_name    VARCHAR(200) NOT NULL,
    unit_id         BIGINT NOT NULL,
    quantity        DECIMAL(18,4) NOT NULL,
    returned_quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    unit_cost       DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    remark          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_borrow_item_line (borrow_id, line_no),
    KEY idx_inv_borrow_item_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='借入借出明细';

-- =========================================================
-- 6. 资金与往来
-- =========================================================

CREATE TABLE fin_account (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    account_code    VARCHAR(50) NOT NULL,
    account_name    VARCHAR(100) NOT NULL,
    account_type    VARCHAR(30) NOT NULL COMMENT 'CASH/BANK/ONLINE/OTHER',
    bank_name       VARCHAR(100) DEFAULT NULL,
    account_number  VARCHAR(100) DEFAULT NULL,
    opening_balance DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    current_balance DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fin_account_code (enterprise_id, account_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资金账户';

CREATE TABLE fin_receivable (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    store_id        BIGINT NOT NULL,
    receivable_no   VARCHAR(50) NOT NULL,
    customer_id     BIGINT NOT NULL,
    source_type     VARCHAR(50) NOT NULL COMMENT 'SALES_ORDER/SALES_RETURN/OTHER',
    source_id       BIGINT NOT NULL,
    source_no       VARCHAR(50) NOT NULL,
    business_date   DATE NOT NULL,
    due_date        DATE DEFAULT NULL,
    original_amount DECIMAL(18,2) NOT NULL COMMENT '原始应收，可为负数',
    received_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    write_off_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    outstanding_amount DECIMAL(18,2) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'UNSETTLED' COMMENT 'UNSETTLED/PARTIALLY_SETTLED/SETTLED/CANCELLED',
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fin_receivable_no (enterprise_id, receivable_no),
    UNIQUE KEY uk_fin_receivable_source (enterprise_id, source_type, source_id),
    KEY idx_fin_receivable_customer_status (customer_id, status),
    KEY idx_fin_receivable_due (due_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应收款';

CREATE TABLE fin_receipt (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    store_id        BIGINT NOT NULL,
    receipt_no      VARCHAR(50) NOT NULL,
    receipt_date    DATE NOT NULL,
    customer_id     BIGINT NOT NULL,
    account_id      BIGINT NOT NULL,
    payment_method  VARCHAR(30) NOT NULL COMMENT 'CASH/BANK/WECHAT/ALIPAY/OTHER',
    receipt_amount  DECIMAL(18,2) NOT NULL,
    allocated_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    unallocated_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CANCELLED',
    reference_no    VARCHAR(100) DEFAULT NULL COMMENT '外部流水号',
    confirmed_by    BIGINT DEFAULT NULL,
    confirmed_at    DATETIME DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fin_receipt_no (enterprise_id, receipt_no),
    KEY idx_fin_receipt_customer_date (customer_id, receipt_date),
    KEY idx_fin_receipt_account_date (account_id, receipt_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收款单';

CREATE TABLE fin_receipt_item (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id      BIGINT NOT NULL,
    receivable_id   BIGINT NOT NULL,
    source_no       VARCHAR(50) NOT NULL COMMENT '业务单号快照',
    allocated_amount DECIMAL(18,2) NOT NULL COMMENT '本次核销金额',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fin_receipt_item (receipt_id, receivable_id),
    KEY idx_fin_receipt_item_receivable (receivable_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收款核销明细';

CREATE TABLE fin_payable (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    store_id        BIGINT NOT NULL,
    payable_no      VARCHAR(50) NOT NULL,
    supplier_id     BIGINT NOT NULL,
    source_type     VARCHAR(50) NOT NULL COMMENT 'PURCHASE_ORDER/PURCHASE_RETURN/OTHER',
    source_id       BIGINT NOT NULL,
    source_no       VARCHAR(50) NOT NULL,
    business_date   DATE NOT NULL,
    due_date        DATE DEFAULT NULL,
    original_amount DECIMAL(18,2) NOT NULL,
    paid_amount     DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    write_off_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    outstanding_amount DECIMAL(18,2) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'UNSETTLED',
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fin_payable_no (enterprise_id, payable_no),
    UNIQUE KEY uk_fin_payable_source (enterprise_id, source_type, source_id),
    KEY idx_fin_payable_supplier_status (supplier_id, status),
    KEY idx_fin_payable_due (due_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应付款';

CREATE TABLE fin_payment (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    store_id        BIGINT NOT NULL,
    payment_no      VARCHAR(50) NOT NULL,
    payment_date    DATE NOT NULL,
    supplier_id     BIGINT NOT NULL,
    account_id      BIGINT NOT NULL,
    payment_method  VARCHAR(30) NOT NULL,
    payment_amount  DECIMAL(18,2) NOT NULL,
    allocated_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    unallocated_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    reference_no    VARCHAR(100) DEFAULT NULL,
    confirmed_by    BIGINT DEFAULT NULL,
    confirmed_at    DATETIME DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fin_payment_no (enterprise_id, payment_no),
    KEY idx_fin_payment_supplier_date (supplier_id, payment_date),
    KEY idx_fin_payment_account_date (account_id, payment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='付款单';

CREATE TABLE fin_payment_item (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    payment_id      BIGINT NOT NULL,
    payable_id      BIGINT NOT NULL,
    source_no       VARCHAR(50) NOT NULL,
    allocated_amount DECIMAL(18,2) NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fin_payment_item (payment_id, payable_id),
    KEY idx_fin_payment_item_payable (payable_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='付款核销明细';

CREATE TABLE fin_other_transaction (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    store_id        BIGINT DEFAULT NULL,
    transaction_no  VARCHAR(50) NOT NULL,
    transaction_date DATE NOT NULL,
    transaction_type VARCHAR(20) NOT NULL COMMENT 'INCOME/EXPENSE',
    category        VARCHAR(100) NOT NULL COMMENT '收支类别',
    account_id      BIGINT NOT NULL,
    amount          DECIMAL(18,2) NOT NULL,
    counterparty    VARCHAR(200) DEFAULT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    confirmed_by    BIGINT DEFAULT NULL,
    confirmed_at    DATETIME DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_by      BIGINT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fin_other_transaction_no (enterprise_id, transaction_no),
    KEY idx_fin_other_transaction_date (transaction_date, transaction_type),
    KEY idx_fin_other_transaction_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='其他收支';

CREATE TABLE fin_capital_flow (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT NOT NULL,
    store_id        BIGINT DEFAULT NULL,
    account_id      BIGINT NOT NULL,
    flow_no         VARCHAR(50) NOT NULL,
    flow_date       DATE NOT NULL,
    flow_type       VARCHAR(50) NOT NULL COMMENT 'RECEIPT/PAYMENT/OTHER_INCOME/OTHER_EXPENSE/OPENING',
    direction       VARCHAR(10) NOT NULL COMMENT 'IN/OUT',
    amount          DECIMAL(18,2) NOT NULL,
    before_balance  DECIMAL(18,2) NOT NULL,
    after_balance   DECIMAL(18,2) NOT NULL,
    source_type     VARCHAR(50) NOT NULL,
    source_id       BIGINT NOT NULL,
    source_no       VARCHAR(50) NOT NULL,
    counterparty_type VARCHAR(20) DEFAULT NULL COMMENT 'CUSTOMER/SUPPLIER/OTHER',
    counterparty_id BIGINT DEFAULT NULL,
    counterparty_name VARCHAR(200) DEFAULT NULL,
    operator_id     BIGINT DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fin_capital_flow_no (enterprise_id, flow_no),
    KEY idx_fin_capital_flow_account_date (account_id, flow_date),
    KEY idx_fin_capital_flow_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资金流水';

SET FOREIGN_KEY_CHECKS = 1;
