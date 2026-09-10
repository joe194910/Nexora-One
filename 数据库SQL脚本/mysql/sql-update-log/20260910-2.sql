-- NexoraOne API 开放平台门户、授权、发布、调试与统计增量脚本
-- 执行日期：2026-09-10

ALTER TABLE `nexora_one_open_api`
    ADD COLUMN `market_title` VARCHAR(100) DEFAULT NULL COMMENT 'API市场展示标题' AFTER `tags`,
    ADD COLUMN `market_summary` VARCHAR(500) DEFAULT NULL COMMENT 'API市场展示简介' AFTER `market_title`,
    ADD COLUMN `sla_description` VARCHAR(300) DEFAULT NULL COMMENT '服务等级说明' AFTER `market_summary`,
    ADD COLUMN `publish_scope` VARCHAR(30) NOT NULL DEFAULT 'platform' COMMENT '发布范围：platform全平台，enterprise指定企业' AFTER `sla_description`,
    ADD COLUMN `publish_time` DATETIME DEFAULT NULL COMMENT '最近发布时间' AFTER `publish_scope`;

ALTER TABLE `nexora_one_application_api_permission`
    ADD COLUMN `use_scene` VARCHAR(300) DEFAULT NULL COMMENT 'API使用场景' AFTER `apply_reason`,
    ADD COLUMN `apply_environment` VARCHAR(30) NOT NULL DEFAULT 'test' COMMENT '申请环境：test测试，prod生产' AFTER `use_scene`,
    ADD COLUMN `applicant_id` BIGINT DEFAULT NULL COMMENT '申请人主键' AFTER `apply_environment`,
    ADD COLUMN `applicant_name` VARCHAR(50) DEFAULT NULL COMMENT '申请人姓名' AFTER `applicant_id`,
    ADD COLUMN `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人主键' AFTER `apply_status`,
    ADD COLUMN `reviewer_name` VARCHAR(50) DEFAULT NULL COMMENT '审核人姓名' AFTER `reviewer_id`,
    ADD COLUMN `daily_quota` BIGINT DEFAULT NULL COMMENT '每日调用额度' AFTER `review_remark`,
    ADD COLUMN `effective_time` DATETIME DEFAULT NULL COMMENT '授权生效时间' AFTER `daily_quota`,
    ADD COLUMN `expire_time` DATETIME DEFAULT NULL COMMENT '授权失效时间' AFTER `effective_time`;

CREATE TABLE IF NOT EXISTS `nexora_one_open_api_call_log` (
    `call_log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '调用日志主键',
    `trace_id` VARCHAR(64) NOT NULL COMMENT '调用链追踪标识',
    `application_id` BIGINT NOT NULL COMMENT '应用主键',
    `app_id` VARCHAR(100) DEFAULT NULL COMMENT 'App ID快照',
    `open_api_id` BIGINT NOT NULL COMMENT '开放API主键',
    `api_code` VARCHAR(100) NOT NULL COMMENT 'API编码快照',
    `environment_code` VARCHAR(30) NOT NULL COMMENT '调用环境编码',
    `request_method` VARCHAR(10) NOT NULL COMMENT '请求方法',
    `request_path` VARCHAR(500) NOT NULL COMMENT '请求路径',
    `http_status` INT NOT NULL COMMENT 'HTTP状态码',
    `success_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否调用成功',
    `duration_ms` BIGINT NOT NULL DEFAULT 0 COMMENT '调用耗时毫秒数',
    `response_bytes` BIGINT NOT NULL DEFAULT 0 COMMENT '响应字节数',
    `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`call_log_id`),
    UNIQUE KEY `uk_open_api_call_trace` (`trace_id`),
    KEY `idx_open_api_call_application` (`application_id`, `create_time`),
    KEY `idx_open_api_call_api` (`open_api_id`, `create_time`),
    KEY `idx_open_api_call_status` (`success_flag`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne开放API调用日志';

-- 补充 API 开放平台页面路由与按钮权限。
INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 829, 'API市场', 2, 820, 40, '/open-api/market', 'business/open-api/api-market.vue', 1, NULL, 'open-api:market', 'ShopOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 829);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 830, '我的授权', 2, 820, 50, '/open-api/applications', 'business/open-api/api-applications.vue', 1, NULL, 'open-api:grant', 'SafetyCertificateOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 830);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 831, '在线调试', 2, 820, 60, '/open-api/debugger', 'business/open-api/api-debugger.vue', 1, NULL, 'open-api:debug', 'CodeOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 831);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 832, '接入指南', 2, 820, 70, '/open-api/guide', 'business/open-api/api-guide.vue', 1, NULL, 'open-api:guide', 'BookOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 832);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 833, '调用统计', 2, 820, 80, '/open-api/statistics', 'business/open-api/api-statistics.vue', 1, NULL, 'open-api:statistics', 'BarChartOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 833);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 834, 'API文档', 2, 820, 90, '/open-api/document', 'business/open-api/api-documentation.vue', 1, NULL, 'open-api:document', 'FileTextOutlined', 0, 0, 0, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 834);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 835, 'API发布', 2, 820, 100, '/open-api/publish', 'business/open-api/api-publish.vue', 1, NULL, 'open-api:publish', 'CloudUploadOutlined', 0, 0, 0, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 835);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 836, '审核API授权', 3, 830, 10, 2, 'open-api:grant:review', 'open-api:grant:review', 0, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 836);

-- 所有现有角色可访问市场、文档、授权、调试、指南和自己的调用统计。
INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT r.`role_id`, m.`menu_id`, NOW(), NOW()
FROM `t_role` r
JOIN `t_menu` m ON m.`menu_id` IN (820, 829, 830, 831, 832, 833, 834)
WHERE NOT EXISTS (
    SELECT 1 FROM `t_role_menu` rm
    WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`
);

-- 管理员保留 API 创建、维护、发布和授权审核能力。
INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, m.`menu_id`, NOW(), NOW()
FROM `t_menu` m
WHERE m.`menu_id` BETWEEN 820 AND 836
  AND NOT EXISTS (
      SELECT 1 FROM `t_role_menu` rm
      WHERE rm.`role_id` = 1 AND rm.`menu_id` = m.`menu_id`
  );
