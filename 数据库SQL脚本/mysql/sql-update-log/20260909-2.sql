-- NexoraOne API 开放平台第一阶段增量脚本
-- 执行日期：2026-09-09

ALTER TABLE `nexora_one_open_api`
    ADD COLUMN `service_name` VARCHAR(100) DEFAULT NULL COMMENT '所属后端服务' AFTER `description`,
    ADD COLUMN `owner_name` VARCHAR(50) DEFAULT NULL COMMENT '接口负责人' AFTER `service_name`,
    ADD COLUMN `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签，英文逗号分隔' AFTER `owner_name`,
    ADD COLUMN `current_version_id` BIGINT DEFAULT NULL COMMENT '当前版本主键' AFTER `tags`,
    ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1草稿，2配置中，3待发布，4已上架，5已停用，6已下线' AFTER `current_version_id`,
    ADD COLUMN `workflow_step` TINYINT NOT NULL DEFAULT 1 COMMENT '当前编辑步骤' AFTER `status`,
    ADD COLUMN `today_call_count` BIGINT NOT NULL DEFAULT 0 COMMENT '今日调用量' AFTER `workflow_step`,
    ADD COLUMN `total_call_count` BIGINT NOT NULL DEFAULT 0 COMMENT '累计调用量' AFTER `today_call_count`,
    ADD COLUMN `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人主键' AFTER `sort`,
    ADD COLUMN `create_user_name` VARCHAR(50) DEFAULT NULL COMMENT '创建人姓名' AFTER `create_user_id`,
    ADD COLUMN `update_user_id` BIGINT DEFAULT NULL COMMENT '更新人主键' AFTER `create_user_name`,
    ADD COLUMN `update_user_name` VARCHAR(50) DEFAULT NULL COMMENT '更新人姓名' AFTER `update_user_id`;

CREATE TABLE IF NOT EXISTS `nexora_one_open_api_version` (
    `version_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'API版本主键',
    `open_api_id` BIGINT NOT NULL COMMENT '开放API主键',
    `version_no` VARCHAR(20) NOT NULL COMMENT '版本号',
    `request_method` VARCHAR(10) NOT NULL COMMENT '请求方式',
    `gateway_path` VARCHAR(300) NOT NULL COMMENT '网关路径',
    `internal_path` VARCHAR(300) DEFAULT NULL COMMENT '内部转发地址',
    `content_type` VARCHAR(50) NOT NULL DEFAULT 'application/json' COMMENT '请求内容类型',
    `permission_level` TINYINT NOT NULL DEFAULT 2 COMMENT '权限级别：1公开，2申请授权，3敏感审核',
    `timeout_seconds` INT NOT NULL DEFAULT 10 COMMENT '请求超时秒数',
    `description` VARCHAR(1000) DEFAULT NULL COMMENT '版本接口说明',
    `unified_response_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用统一响应结构',
    `data_masking_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用数据脱敏',
    `security_config` JSON DEFAULT NULL COMMENT '安全策略预留配置',
    `change_log` VARCHAR(500) DEFAULT NULL COMMENT '版本更新说明',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '版本状态：1草稿，2待发布，3已发布，4已停用，5已下线',
    `locked_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '版本是否锁定',
    `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人主键',
    `create_user_name` VARCHAR(50) DEFAULT NULL COMMENT '创建人姓名',
    `update_user_id` BIGINT DEFAULT NULL COMMENT '更新人主键',
    `update_user_name` VARCHAR(50) DEFAULT NULL COMMENT '更新人姓名',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`version_id`),
    UNIQUE KEY `uk_open_api_version` (`open_api_id`, `version_no`),
    UNIQUE KEY `uk_gateway_path_version` (`gateway_path`, `version_no`),
    KEY `idx_open_api_version_status` (`open_api_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne开放API版本';

CREATE TABLE IF NOT EXISTS `nexora_one_open_api_environment` (
    `environment_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '发布环境主键',
    `version_id` BIGINT NOT NULL COMMENT 'API版本主键',
    `environment_code` VARCHAR(30) NOT NULL COMMENT '环境编码',
    `environment_name` VARCHAR(50) NOT NULL COMMENT '环境名称',
    `base_url` VARCHAR(300) NOT NULL COMMENT '环境基础地址',
    `enabled_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `online_debug_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许在线调试',
    `description` VARCHAR(300) DEFAULT NULL COMMENT '环境说明',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`environment_id`),
    UNIQUE KEY `uk_version_environment` (`version_id`, `environment_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne开放API发布环境';

CREATE TABLE IF NOT EXISTS `nexora_one_open_api_parameter` (
    `parameter_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '参数主键',
    `version_id` BIGINT NOT NULL COMMENT 'API版本主键',
    `direction` TINYINT NOT NULL COMMENT '参数方向：1请求，2响应',
    `location` VARCHAR(20) NOT NULL COMMENT '参数位置',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父参数主键',
    `parameter_name` VARCHAR(100) NOT NULL COMMENT '参数名称',
    `chinese_name` VARCHAR(100) DEFAULT NULL COMMENT '中文名称',
    `data_type` VARCHAR(30) NOT NULL COMMENT '数据类型',
    `required_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必填',
    `nullable_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可空',
    `default_value` VARCHAR(500) DEFAULT NULL COMMENT '默认值',
    `example_value` VARCHAR(1000) DEFAULT NULL COMMENT '示例值',
    `validation_rule` VARCHAR(500) DEFAULT NULL COMMENT '校验规则',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '参数说明',
    `masking_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否脱敏',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`parameter_id`),
    KEY `idx_api_parameter_version` (`version_id`, `direction`, `location`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne开放API参数定义';

CREATE TABLE IF NOT EXISTS `nexora_one_open_api_example` (
    `example_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '示例主键',
    `version_id` BIGINT NOT NULL COMMENT 'API版本主键',
    `example_type` VARCHAR(30) NOT NULL COMMENT '示例类型',
    `example_name` VARCHAR(100) NOT NULL COMMENT '示例名称',
    `content` LONGTEXT NOT NULL COMMENT '示例内容',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`example_id`),
    KEY `idx_api_example_version` (`version_id`, `example_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne开放API请求响应示例';

CREATE TABLE IF NOT EXISTS `nexora_one_open_api_error_code` (
    `error_code_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '错误码主键',
    `version_id` BIGINT NOT NULL COMMENT 'API版本主键',
    `http_status` INT NOT NULL COMMENT 'HTTP状态码',
    `business_code` VARCHAR(50) NOT NULL COMMENT '业务错误码',
    `error_message` VARCHAR(200) NOT NULL COMMENT '错误信息',
    `trigger_condition` VARCHAR(500) DEFAULT NULL COMMENT '触发条件',
    `handling_advice` VARCHAR(500) DEFAULT NULL COMMENT '处理建议',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`error_code_id`),
    UNIQUE KEY `uk_version_business_code` (`version_id`, `business_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne开放API错误码';

-- 为既有 API 目录补充首个版本，保证应用中心历史授权关系继续有效。
INSERT INTO `nexora_one_open_api_version`
(`open_api_id`, `version_no`, `request_method`, `gateway_path`, `content_type`, `permission_level`,
 `timeout_seconds`, `description`, `status`, `locked_flag`)
SELECT a.`open_api_id`, a.`api_version`, a.`request_method`, a.`request_path`, 'application/json',
       a.`permission_level`, 10, a.`description`, IF(a.`enabled_flag` = 1, 3, 4), a.`enabled_flag`
FROM `nexora_one_open_api` a
WHERE NOT EXISTS (
    SELECT 1 FROM `nexora_one_open_api_version` v
    WHERE v.`open_api_id` = a.`open_api_id` AND v.`version_no` = a.`api_version`
);

UPDATE `nexora_one_open_api` a
JOIN `nexora_one_open_api_version` v
  ON v.`open_api_id` = a.`open_api_id` AND v.`version_no` = a.`api_version`
SET a.`current_version_id` = v.`version_id`,
    a.`status` = IF(a.`enabled_flag` = 1, 4, 5),
    a.`workflow_step` = 4
WHERE a.`current_version_id` IS NULL;

INSERT INTO `nexora_one_open_api_environment`
(`version_id`, `environment_code`, `environment_name`, `base_url`, `enabled_flag`, `online_debug_flag`, `description`)
SELECT v.`version_id`, 'test', '测试环境', 'https://api-test.nexoraone.com', 1, 1, '用于功能测试和联调'
FROM `nexora_one_open_api_version` v
WHERE NOT EXISTS (
    SELECT 1 FROM `nexora_one_open_api_environment` e
    WHERE e.`version_id` = v.`version_id` AND e.`environment_code` = 'test'
);

INSERT INTO `nexora_one_open_api_environment`
(`version_id`, `environment_code`, `environment_name`, `base_url`, `enabled_flag`, `online_debug_flag`, `description`)
SELECT v.`version_id`, 'prod', '生产环境', 'https://api.nexoraone.com', 1, 0, '正式生产环境'
FROM `nexora_one_open_api_version` v
WHERE NOT EXISTS (
    SELECT 1 FROM `nexora_one_open_api_environment` e
    WHERE e.`version_id` = v.`version_id` AND e.`environment_code` = 'prod'
);

-- API 开放平台菜单及按钮权限。
INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 820, 'API开放平台', 1, 0, 45, '/open-api', NULL, 1, NULL, NULL, 'ApiOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 820);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 821, 'API管理', 2, 820, 10, '/open-api/manage', 'business/open-api/api-management.vue', 1, NULL, 'open-api:query', 'DatabaseOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 821);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 822, '创建与配置API', 2, 820, 20, '/open-api/editor', 'business/open-api/api-editor.vue', 1, NULL, 'open-api:add', 'PlusSquareOutlined', 0, 0, 0, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 822);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 823, '查询API', 3, 821, 10, 2, 'open-api:query', 'open-api:query', 0, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 823);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 824, '创建API', 3, 821, 20, 2, 'open-api:add', 'open-api:add', 0, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 824);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 825, '查看API详情', 3, 821, 30, 2, 'open-api:detail', 'open-api:detail', 0, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 825);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 826, '保存API配置', 3, 821, 40, 2, 'open-api:save', 'open-api:save', 0, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 826);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 827, '变更API状态', 3, 821, 50, 2, 'open-api:status', 'open-api:status', 0, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 827);

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, m.`menu_id`, NOW(), NOW()
FROM `t_menu` m
WHERE m.`menu_id` BETWEEN 820 AND 827
  AND NOT EXISTS (
      SELECT 1 FROM `t_role_menu` rm
      WHERE rm.`role_id` = 1 AND rm.`menu_id` = m.`menu_id`
  );
