-- NexoraOne 应用中心门户、运营管理与单点登录扩展

CREATE TABLE IF NOT EXISTS `nexora_one_application_favorite` (
    `favorite_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏记录主键',
    `application_id` BIGINT NOT NULL COMMENT '应用主键',
    `employee_id` BIGINT NOT NULL COMMENT '员工主键',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`favorite_id`),
    UNIQUE KEY `uk_application_employee` (`application_id`, `employee_id`),
    KEY `idx_favorite_employee` (`employee_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne应用收藏';

CREATE TABLE IF NOT EXISTS `nexora_one_application_visit_log` (
    `visit_log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '访问日志主键',
    `application_id` BIGINT DEFAULT NULL COMMENT '应用主键',
    `application_name` VARCHAR(100) NOT NULL COMMENT '应用名称快照',
    `employee_id` BIGINT NOT NULL COMMENT '访问员工主键',
    `employee_name` VARCHAR(50) DEFAULT NULL COMMENT '访问员工姓名',
    `department_id` BIGINT DEFAULT NULL COMMENT '部门主键',
    `department_name` VARCHAR(100) DEFAULT NULL COMMENT '部门名称',
    `launch_url` VARCHAR(1000) DEFAULT NULL COMMENT '应用跳转地址',
    `success_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否访问成功',
    `failure_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    `ip_address` VARCHAR(64) DEFAULT NULL COMMENT '访问IP',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器标识',
    `visit_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    PRIMARY KEY (`visit_log_id`),
    KEY `idx_visit_application_time` (`application_id`, `visit_time`),
    KEY `idx_visit_employee_time` (`employee_id`, `visit_time`),
    KEY `idx_visit_success_time` (`success_flag`, `visit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne应用访问日志';

CREATE TABLE IF NOT EXISTS `nexora_one_application_sso_auth_code` (
    `auth_code_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '授权码记录主键',
    `application_id` BIGINT NOT NULL COMMENT '应用主键',
    `app_id` VARCHAR(64) NOT NULL COMMENT '应用App ID',
    `code_hash` VARCHAR(64) NOT NULL COMMENT '一次性授权码SHA-256摘要',
    `employee_id` BIGINT NOT NULL COMMENT '登录员工主键',
    `user_snapshot` LONGTEXT NOT NULL COMMENT '登录用户信息快照',
    `redirect_uri` VARCHAR(500) DEFAULT NULL COMMENT '应用授权回调地址',
    `expires_time` DATETIME NOT NULL COMMENT '授权码过期时间',
    `used_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已经兑换',
    `used_time` DATETIME DEFAULT NULL COMMENT '兑换时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`auth_code_id`),
    UNIQUE KEY `uk_sso_code_hash` (`code_hash`),
    KEY `idx_sso_application_status` (`application_id`, `used_flag`, `expires_time`),
    KEY `idx_sso_employee_time` (`employee_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne应用单点登录授权码';

-- 修复历史脚本中重置密钥菜单表名拼写错误
INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 808, '重置应用密钥', 3, 801, 50, 2, 'application:secret:reset', 'application:secret:reset', 0, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 808);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 811, '应用管理', 2, 800, 20, '/application/manage', 'business/application/application-management.vue', 1, NULL, 'application:manage', 'ControlOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 811);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 812, '应用市场', 2, 800, 30, '/application/market', 'business/application/application-market.vue', 1, NULL, 'application:market', 'ShopOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 812);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 813, '我的应用', 2, 800, 40, '/application/my', 'business/application/application-my.vue', 1, NULL, 'application:portal', 'AppstoreAddOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 813);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 814, '应用审核', 2, 800, 50, '/application/review', 'business/application/application-review.vue', 1, NULL, 'application:review', 'AuditOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 814);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 815, '访问日志', 2, 800, 60, '/application/visit-log', 'business/application/application-visit-log.vue', 1, NULL, 'application:visit-log', 'HistoryOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 815);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 816, '应用上下架', 3, 811, 10, 2, 'application:status', 'application:status', 0, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 816);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 817, '应用门户操作', 3, 813, 10, 2, 'application:portal', 'application:portal', 0, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 817);

-- 管理员拥有应用中心全部功能
INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, m.menu_id, NOW(), NOW()
FROM `t_menu` m
WHERE m.menu_id BETWEEN 800 AND 817
  AND NOT EXISTS (
    SELECT 1 FROM `t_role_menu` rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );

-- 所有现有角色默认开放应用市场和个人应用门户
INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT r.role_id, m.menu_id, NOW(), NOW()
FROM `t_role` r
JOIN `t_menu` m ON m.menu_id IN (800, 812, 813, 817)
WHERE NOT EXISTS (
    SELECT 1 FROM `t_role_menu` rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );
