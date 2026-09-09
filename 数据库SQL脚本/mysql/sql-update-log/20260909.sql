-- NexoraOne 应用中心：应用接入、上架、审核与版本管理

CREATE TABLE IF NOT EXISTS `nexora_one_application` (
    `application_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '应用主键',
    `application_name` VARCHAR(50) NOT NULL COMMENT '应用名称',
    `application_code` VARCHAR(32) NOT NULL COMMENT '应用编码',
    `application_type` TINYINT NOT NULL DEFAULT 1 COMMENT '应用类型：1企业内部应用，2第三方应用',
    `enterprise_id` BIGINT DEFAULT NULL COMMENT '所属企业',
    `enterprise_name` VARCHAR(100) DEFAULT NULL COMMENT '所属企业名称快照',
    `owner_name` VARCHAR(50) NOT NULL COMMENT '负责人',
    `contact` VARCHAR(100) NOT NULL COMMENT '联系方式',
    `icon_url` VARCHAR(500) DEFAULT NULL COMMENT '应用图标',
    `summary` VARCHAR(500) NOT NULL COMMENT '应用简介',
    `home_url` VARCHAR(500) DEFAULT NULL COMMENT '应用首页地址',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `access_status` TINYINT NOT NULL DEFAULT 1 COMMENT '接入状态：1接入中，2已接入，3接入失败',
    `listing_status` TINYINT NOT NULL DEFAULT 0 COMMENT '上架状态：0未上架，1审核中，2已上架，3已驳回，4已下架',
    `workflow_step` TINYINT NOT NULL DEFAULT 1 COMMENT '当前流程步骤',
    `config_locked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '提交审核后配置是否锁定',
    `login_config` JSON DEFAULT NULL COMMENT '登录与单点跳转配置',
    `security_config` JSON DEFAULT NULL COMMENT '接口鉴权与安全配置',
    `listing_config` JSON DEFAULT NULL COMMENT '应用上架资料',
    `publish_config` JSON DEFAULT NULL COMMENT '发布范围与可见权限',
    `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人',
    `create_user_name` VARCHAR(50) DEFAULT NULL COMMENT '创建人姓名',
    `update_user_id` BIGINT DEFAULT NULL COMMENT '更新人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`application_id`),
    UNIQUE KEY `uk_application_code` (`application_code`),
    KEY `idx_application_name` (`application_name`),
    KEY `idx_application_status` (`listing_status`, `access_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne应用';

CREATE TABLE IF NOT EXISTS `nexora_one_application_credential` (
    `credential_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '凭证主键',
    `application_id` BIGINT NOT NULL COMMENT '应用主键',
    `app_id` VARCHAR(64) NOT NULL COMMENT '应用App ID',
    `secret_hash` VARCHAR(128) NOT NULL COMMENT 'App Secret摘要',
    `secret_hint` VARCHAR(16) NOT NULL COMMENT 'App Secret脱敏提示',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，0失效',
    `version_no` INT NOT NULL DEFAULT 1 COMMENT '密钥版本',
    `last_reset_time` DATETIME DEFAULT NULL COMMENT '最近重置时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`credential_id`),
    UNIQUE KEY `uk_credential_app_id` (`app_id`),
    KEY `idx_credential_application` (`application_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne应用凭证';

CREATE TABLE IF NOT EXISTS `nexora_one_open_api` (
    `open_api_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '开放API主键',
    `category_name` VARCHAR(50) NOT NULL COMMENT 'API分类',
    `api_name` VARCHAR(100) NOT NULL COMMENT 'API名称',
    `api_code` VARCHAR(100) NOT NULL COMMENT 'API编码',
    `request_method` VARCHAR(10) NOT NULL COMMENT '请求方式',
    `request_path` VARCHAR(200) NOT NULL COMMENT '请求路径',
    `api_version` VARCHAR(20) NOT NULL DEFAULT 'v1.0' COMMENT 'API版本',
    `permission_level` TINYINT NOT NULL DEFAULT 1 COMMENT '权限级别：1公开，2申请授权，3敏感审核',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '接口说明',
    `enabled_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`open_api_id`),
    UNIQUE KEY `uk_open_api_code` (`api_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne开放API目录';

CREATE TABLE IF NOT EXISTS `nexora_one_application_api_permission` (
    `permission_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请记录主键',
    `application_id` BIGINT NOT NULL COMMENT '应用主键',
    `open_api_id` BIGINT NOT NULL COMMENT '开放API主键',
    `apply_reason` VARCHAR(500) DEFAULT NULL COMMENT '申请原因',
    `apply_status` TINYINT NOT NULL DEFAULT 1 COMMENT '申请状态：1待审核，2已授权，3已驳回',
    `review_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`permission_id`),
    UNIQUE KEY `uk_application_open_api` (`application_id`, `open_api_id`),
    KEY `idx_api_permission_status` (`application_id`, `apply_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne应用API权限申请';

CREATE TABLE IF NOT EXISTS `nexora_one_application_review` (
    `review_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审核记录主键',
    `application_id` BIGINT NOT NULL COMMENT '应用主键',
    `review_stage` VARCHAR(30) NOT NULL COMMENT '审核阶段',
    `review_status` TINYINT NOT NULL COMMENT '审核状态：1待审核，2通过，3驳回',
    `review_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核说明',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`review_id`),
    KEY `idx_review_application` (`application_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne应用审核记录';

CREATE TABLE IF NOT EXISTS `nexora_one_application_version` (
    `version_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '版本主键',
    `application_id` BIGINT NOT NULL COMMENT '应用主键',
    `version_no` VARCHAR(20) NOT NULL COMMENT '版本号',
    `version_status` TINYINT NOT NULL DEFAULT 1 COMMENT '版本状态：1审核中，2已发布，3已驳回，4已下架',
    `config_snapshot` LONGTEXT NOT NULL COMMENT '提交时完整配置快照',
    `submit_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    PRIMARY KEY (`version_id`),
    UNIQUE KEY `uk_application_version` (`application_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne应用版本';

INSERT INTO `nexora_one_open_api`
(`category_name`, `api_name`, `api_code`, `request_method`, `request_path`, `api_version`, `permission_level`, `description`, `sort`)
SELECT '统一身份', '获取当前用户信息', 'identity:user:info', 'GET', '/v1/user/info', 'v1.0', 1, '获取登录用户的基本信息', 10
WHERE NOT EXISTS (SELECT 1 FROM `nexora_one_open_api` WHERE `api_code` = 'identity:user:info');
INSERT INTO `nexora_one_open_api`
(`category_name`, `api_name`, `api_code`, `request_method`, `request_path`, `api_version`, `permission_level`, `description`, `sort`)
SELECT '统一身份', '查询部门列表', 'identity:department:list', 'GET', '/v1/departments', 'v1.0', 2, '根据条件查询部门列表', 20
WHERE NOT EXISTS (SELECT 1 FROM `nexora_one_open_api` WHERE `api_code` = 'identity:department:list');
INSERT INTO `nexora_one_open_api`
(`category_name`, `api_name`, `api_code`, `request_method`, `request_path`, `api_version`, `permission_level`, `description`, `sort`)
SELECT '消息服务', '发送站内消息', 'message:send', 'POST', '/v1/messages', 'v1.0', 2, '向指定用户发送站内消息', 30
WHERE NOT EXISTS (SELECT 1 FROM `nexora_one_open_api` WHERE `api_code` = 'message:send');
INSERT INTO `nexora_one_open_api`
(`category_name`, `api_name`, `api_code`, `request_method`, `request_path`, `api_version`, `permission_level`, `description`, `sort`)
SELECT '文件服务', '上传文件', 'file:upload', 'POST', '/v1/files/upload', 'v1.0', 3, '上传文件到企业文件库', 40
WHERE NOT EXISTS (SELECT 1 FROM `nexora_one_open_api` WHERE `api_code` = 'file:upload');
INSERT INTO `nexora_one_open_api`
(`category_name`, `api_name`, `api_code`, `request_method`, `request_path`, `api_version`, `permission_level`, `description`, `sort`)
SELECT '企业信息', '查询企业信息', 'enterprise:info', 'GET', '/v1/company/info', 'v1.0', 1, '获取企业基本信息', 50
WHERE NOT EXISTS (SELECT 1 FROM `nexora_one_open_api` WHERE `api_code` = 'enterprise:info');
INSERT INTO `nexora_one_open_api`
(`category_name`, `api_name`, `api_code`, `request_method`, `request_path`, `api_version`, `permission_level`, `description`, `sort`)
SELECT '统一身份', '创建用户', 'identity:user:create', 'POST', '/v1/users', 'v1.0', 3, '在指定部门下创建用户', 60
WHERE NOT EXISTS (SELECT 1 FROM `nexora_one_open_api` WHERE `api_code` = 'identity:user:create');
INSERT INTO `nexora_one_open_api`
(`category_name`, `api_name`, `api_code`, `request_method`, `request_path`, `api_version`, `permission_level`, `description`, `sort`)
SELECT '统一身份', '获取角色列表', 'identity:role:list', 'GET', '/v1/roles', 'v1.0', 2, '查询系统角色列表', 70
WHERE NOT EXISTS (SELECT 1 FROM `nexora_one_open_api` WHERE `api_code` = 'identity:role:list');
INSERT INTO `nexora_one_open_api`
(`category_name`, `api_name`, `api_code`, `request_method`, `request_path`, `api_version`, `permission_level`, `description`, `sort`)
SELECT '消息服务', '发送邮件', 'message:email:send', 'POST', '/v1/emails', 'v1.0', 2, '发送企业邮件', 80
WHERE NOT EXISTS (SELECT 1 FROM `nexora_one_open_api` WHERE `api_code` = 'message:email:send');
INSERT INTO `nexora_one_open_api`
(`category_name`, `api_name`, `api_code`, `request_method`, `request_path`, `api_version`, `permission_level`, `description`, `sort`)
SELECT '应用认证', '换取Access Token', 'application:oauth:token', 'POST', '/open/application/oauth/token', 'v1.0', 1, '使用App ID和App Secret通过client_credentials模式换取Access Token', 1
WHERE NOT EXISTS (SELECT 1 FROM `nexora_one_open_api` WHERE `api_code` = 'application:oauth:token');
INSERT INTO `nexora_one_open_api`
(`category_name`, `api_name`, `api_code`, `request_method`, `request_path`, `api_version`, `permission_level`, `description`, `sort`)
SELECT '应用认证', '平台连通性检查', 'application:connect:ping', 'GET', '/open/application/connect/ping', 'v1.0', 1, '携带Bearer Access Token验证应用与NexoraOne平台的接入链路', 2
WHERE NOT EXISTS (SELECT 1 FROM `nexora_one_open_api` WHERE `api_code` = 'application:connect:ping');

-- 应用中心菜单及按钮权限
INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 800, '应用中心', 1, 0, 40, '/application', NULL, 1, NULL, NULL, 'AppstoreOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 800);
INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 801, '应用接入', 2, 800, 10, '/application/access', 'business/application/application-list.vue', 1, NULL, 'application:query', 'ApiOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 801);
INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 802, '创建与配置应用', 2, 800, 20, '/application/onboarding', 'business/application/application-onboarding.vue', 1, NULL, 'application:create', 'PlusSquareOutlined', 0, 0, 0, 0, 0, 1, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 802);
INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 803, '应用详情', 2, 800, 30, '/application/detail', 'business/application/application-detail.vue', 1, NULL, 'application:detail', 'ProfileOutlined', 0, 0, 0, 0, 0, 1, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 803);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 804, '查询应用', 3, 801, 10, 2, 'application:query', 'application:query', 0, 0, 0, 1, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 804);
INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 805, '创建应用', 3, 801, 20, 2, 'application:create', 'application:create', 0, 0, 0, 1, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 805);
INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 806, '应用详情', 3, 801, 30, 2, 'application:detail', 'application:detail', 0, 0, 0, 1, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 806);
INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 807, '保存配置', 3, 801, 40, 2, 'application:save', 'application:save', 0, 0, 0, 1, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 807);
INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 808, '重置密钥', 3, 801, 50, 2, 'application:secret:reset', 'application:secret:reset', 0, 0, 0, 1, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 808);
INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 809, '提交审核', 3, 801, 60, 2, 'application:submit', 'application:submit', 0, 0, 0, 1, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 809);
INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 810, '审核应用', 3, 801, 70, 2, 'application:review', 'application:review', 0, 0, 0, 1, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 810);

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, m.menu_id, NOW(), NOW()
FROM `t_menu` m
WHERE m.menu_id BETWEEN 800 AND 810
  AND NOT EXISTS (
    SELECT 1 FROM `t_role_menu` rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
);
