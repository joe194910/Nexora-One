-- NexoraOne开放API真实网关、调用统计与发布审核增量脚本
-- 执行日期：2026-09-10

ALTER TABLE `nexora_one_open_api`
    ADD COLUMN `call_count_date` DATE DEFAULT NULL COMMENT '今日调用量对应的统计日期' AFTER `today_call_count`;

CREATE TABLE IF NOT EXISTS `nexora_one_open_api_publish_review` (
    `review_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审核记录主键',
    `open_api_id` BIGINT NOT NULL COMMENT '开放API主键',
    `version_id` BIGINT NOT NULL COMMENT '提交审核的版本主键',
    `applicant_id` BIGINT DEFAULT NULL COMMENT '申请人主键',
    `applicant_name` VARCHAR(50) DEFAULT NULL COMMENT '申请人姓名',
    `review_status` TINYINT NOT NULL DEFAULT 1 COMMENT '审核状态：1待审核，2审核通过，3审核驳回',
    `review_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人主键',
    `reviewer_name` VARCHAR(50) DEFAULT NULL COMMENT '审核人姓名',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`review_id`),
    KEY `idx_open_api_publish_review_api` (`open_api_id`, `review_status`),
    KEY `idx_open_api_publish_review_version` (`version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NexoraOne开放API发布审核记录';

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 837, 'API发布审核', 2, 820, 35, '/open-api/publish-review', 'business/open-api/api-publish-review.vue', 1, NULL, 'open-api:publish:review', 'AuditOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 837);

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, 837, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM `t_role_menu`
    WHERE `role_id` = 1 AND `menu_id` = 837
);

-- 统一应用认证接口的标准路径，控制器继续兼容历史路径
UPDATE `nexora_one_open_api`
SET `request_path` = '/open-api/oauth/token',
    `update_time` = NOW()
WHERE `api_code` = 'application:oauth:token'
  AND `request_path` = '/open/application/oauth/token';

UPDATE `nexora_one_open_api`
SET `request_path` = '/open-api/connect/ping',
    `update_time` = NOW()
WHERE `api_code` = 'application:connect:ping'
  AND `request_path` = '/open/application/connect/ping';
