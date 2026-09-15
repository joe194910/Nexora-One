-- 文档解析三模块及知识库最小闭环；须在 20260915-ai-platform.sql 后执行。
-- 已部署旧解析配置的环境保留原表和已有配置，并迁移为默认解析方案。

ALTER TABLE `nexora_one_ai_document_parse_config`
  ADD COLUMN `plan_name` VARCHAR(100) DEFAULT NULL COMMENT '解析方案名称',
  ADD COLUMN `description` VARCHAR(200) DEFAULT NULL COMMENT '方案描述',
  ADD COLUMN `default_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认方案',
  ADD COLUMN `enabled_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  ADD COLUMN `parser_service_id` BIGINT DEFAULT NULL COMMENT '文档解析服务主键',
  ADD COLUMN `ocr_service_id` BIGINT DEFAULT NULL COMMENT 'OCR 服务主键',
  ADD COLUMN `table_service_id` BIGINT DEFAULT NULL COMMENT '表格解析服务主键',
  ADD COLUMN `image_service_id` BIGINT DEFAULT NULL COMMENT '图片理解服务主键';

CREATE TABLE IF NOT EXISTS `nexora_one_ai_parse_service` (
  `parse_service_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `service_name` VARCHAR(100) NOT NULL COMMENT '名称',
  `service_type` VARCHAR(20) NOT NULL COMMENT 'TIKA OCR TABLE IMAGE',
  `deployment_type` VARCHAR(20) NOT NULL COMMENT 'BUILTIN HTTP',
  `implementation` VARCHAR(50) DEFAULT NULL COMMENT '内置实现',
  `endpoint` VARCHAR(500) DEFAULT NULL COMMENT 'HTTP API 地址',
  `api_key_cipher` VARCHAR(2000) DEFAULT NULL COMMENT '加密密钥',
  `supported_formats` VARCHAR(300) NOT NULL COMMENT '支持格式',
  `timeout_seconds` INT NOT NULL DEFAULT 60 COMMENT '超时秒数',
  `enabled_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用',
  `default_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '默认',
  `connection_status` VARCHAR(20) NOT NULL DEFAULT 'UNTESTED' COMMENT '连接状态',
  `last_error` VARCHAR(1000) DEFAULT NULL COMMENT '最近错误',
  `last_test_time` DATETIME DEFAULT NULL COMMENT '最近检测时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`parse_service_id`),
  UNIQUE KEY `uk_parse_service_name` (`service_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档解析服务';

CREATE TABLE IF NOT EXISTS `nexora_one_ai_knowledge_base` (
  `knowledge_base_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `base_name` VARCHAR(100) NOT NULL COMMENT '名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `parse_plan_id` BIGINT NOT NULL COMMENT '解析方案',
  `embedding_model_id` BIGINT NOT NULL COMMENT '向量模型',
  `vector_database_id` BIGINT NOT NULL COMMENT 'Qdrant 实例',
  `collection_name` VARCHAR(150) DEFAULT NULL COMMENT '向量集合',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`knowledge_base_id`),
  UNIQUE KEY `uk_ai_kb_name` (`base_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库';

CREATE TABLE IF NOT EXISTS `nexora_one_ai_parse_task` (
  `task_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务主键',
  `knowledge_base_id` BIGINT NOT NULL COMMENT '知识库',
  `parse_plan_id` BIGINT NOT NULL COMMENT '解析方案快照',
  `file_name` VARCHAR(255) NOT NULL COMMENT '原文件名',
  `file_path` VARCHAR(1000) NOT NULL COMMENT '受控存储路径',
  `file_hash` CHAR(64) NOT NULL COMMENT 'SHA256',
  `file_size` BIGINT NOT NULL COMMENT '字节数',
  `status` VARCHAR(20) NOT NULL COMMENT '任务状态',
  `current_stage` VARCHAR(20) NOT NULL COMMENT '当前阶段',
  `chunk_count` INT NOT NULL DEFAULT 0 COMMENT '切片数',
  `indexed_count` INT NOT NULL DEFAULT 0 COMMENT '入库数',
  `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
  `create_user_id` BIGINT DEFAULT NULL COMMENT '上传人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `finish_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_parse_task_queue` (`status`, `create_time`),
  KEY `idx_parse_task_base` (`knowledge_base_id`, `create_time`),
  KEY `idx_parse_task_hash` (`knowledge_base_id`, `file_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档解析任务';

CREATE TABLE IF NOT EXISTS `nexora_one_ai_parse_step` (
  `step_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '阶段记录主键',
  `task_id` BIGINT NOT NULL COMMENT '任务',
  `stage` VARCHAR(20) NOT NULL COMMENT '阶段',
  `status` VARCHAR(20) NOT NULL COMMENT '结果状态',
  `result_summary` VARCHAR(1000) DEFAULT NULL COMMENT '执行结果',
  `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
  `duration_ms` BIGINT NOT NULL DEFAULT 0 COMMENT '耗时毫秒',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `finish_time` DATETIME NOT NULL COMMENT '结束时间',
  PRIMARY KEY (`step_id`),
  KEY `idx_parse_step_task` (`task_id`, `step_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解析阶段执行记录';

INSERT INTO `nexora_one_ai_parse_service`
(`service_name`, `service_type`, `deployment_type`, `implementation`, `supported_formats`,
 `timeout_seconds`, `enabled_flag`, `default_flag`, `connection_status`)
SELECT '内置 Apache Tika', 'TIKA', 'BUILTIN', 'APACHE_TIKA',
       'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,csv', 60, 1, 1, 'CONNECTED'
WHERE NOT EXISTS (SELECT 1 FROM `nexora_one_ai_parse_service` WHERE `service_type` = 'TIKA');

UPDATE `nexora_one_ai_document_parse_config` p
SET p.`plan_name` = COALESCE(p.`plan_name`, CONCAT('旧版解析方案 ', p.`config_id`)),
    p.`default_flag` = CASE WHEN p.`config_id` = (SELECT MIN(x.`config_id`) FROM
      (SELECT `config_id` FROM `nexora_one_ai_document_parse_config`) x) THEN 1 ELSE p.`default_flag` END,
    p.`parser_service_id` = COALESCE(p.`parser_service_id`,
      (SELECT MIN(s.`parse_service_id`) FROM `nexora_one_ai_parse_service` s WHERE s.`service_type` = 'TIKA'));

-- 旧默认配置曾宣称支持图片但未配置 OCR，迁移后避免生成必然失败的图片任务。
UPDATE `nexora_one_ai_document_parse_config`
SET `supported_formats` = 'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,csv'
WHERE `supported_formats` = 'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,csv,png,jpg,jpeg'
  AND `ocr_enabled` = 0 AND `extract_image` = 0;

UPDATE `t_menu` SET `menu_name` = '文档解析', `menu_type` = 1, `path` = '/ai-platform/document',
  `component` = NULL, `web_perms` = NULL, `icon` = 'FileSearchOutlined', `sort` = 40
WHERE `menu_id` = 843;
UPDATE `t_menu` SET `menu_name` = '解析方案', `menu_type` = 2, `perms_type` = 1,
  `api_perms` = NULL, `parent_id` = 843, `sort` = 10,
  `path` = '/ai-platform/document/plans', `component` = '/business/ai/parse-plans.vue',
  `web_perms` = 'ai:parse-plan:query', `icon` = 'FileTextOutlined', `visible_flag` = 1
WHERE `menu_id` = 858;
UPDATE `t_menu` SET `menu_name` = '解析服务', `menu_type` = 2, `perms_type` = 1,
  `api_perms` = NULL, `parent_id` = 843, `sort` = 20,
  `path` = '/ai-platform/document/services', `component` = '/business/ai/parse-services.vue',
  `web_perms` = 'ai:parse-service:query', `icon` = 'ApiOutlined', `visible_flag` = 1
WHERE `menu_id` = 859;
UPDATE `t_menu` SET `menu_name` = '解析任务', `menu_type` = 2, `perms_type` = 1,
  `api_perms` = NULL, `parent_id` = 843, `sort` = 30,
  `path` = '/ai-platform/document/tasks', `component` = '/business/ai/parse-tasks.vue',
  `web_perms` = 'ai:parse-task:query', `icon` = 'ProfileOutlined', `visible_flag` = 1
WHERE `menu_id` = 860;

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`,
 `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`,
 `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
VALUES
(865, '知识库', 2, 839, 35, '/ai-platform/knowledge-bases', '/business/ai/knowledge-bases.vue',
  1, NULL, 'ai:knowledge:query', 'BookOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `component`=VALUES(`component`), `menu_name`=VALUES(`menu_name`);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`,
 `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
VALUES
(866, '维护解析方案', 3, 858, 10, 2, 'ai:parse-plan:save', 'ai:parse-plan:save', 0, 0, 0, 1, 1, NOW(), NOW()),
(867, '测试解析方案', 3, 858, 20, 2, 'ai:parse-plan:test', 'ai:parse-plan:test', 0, 0, 0, 1, 1, NOW(), NOW()),
(868, '查询解析服务', 3, 859, 10, 2, 'ai:parse-service:query', 'ai:parse-service:query', 0, 0, 0, 1, 1, NOW(), NOW()),
(869, '维护解析服务', 3, 859, 20, 2, 'ai:parse-service:save', 'ai:parse-service:save', 0, 0, 0, 1, 1, NOW(), NOW()),
(870, '测试解析服务', 3, 859, 30, 2, 'ai:parse-service:test', 'ai:parse-service:test', 0, 0, 0, 1, 1, NOW(), NOW()),
(871, '查询解析任务', 3, 860, 10, 2, 'ai:parse-task:query', 'ai:parse-task:query', 0, 0, 0, 1, 1, NOW(), NOW()),
(872, '操作解析任务', 3, 860, 20, 2, 'ai:parse-task:operate', 'ai:parse-task:operate', 0, 0, 0, 1, 1, NOW(), NOW()),
(873, '查询知识库', 3, 865, 10, 2, 'ai:knowledge:query', 'ai:knowledge:query', 0, 0, 0, 1, 1, NOW(), NOW()),
(874, '维护知识库', 3, 865, 20, 2, 'ai:knowledge:save', 'ai:knowledge:save', 0, 0, 0, 1, 1, NOW(), NOW()),
(875, '上传文档', 3, 865, 30, 2, 'ai:knowledge:upload', 'ai:knowledge:upload', 0, 0, 0, 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `api_perms`=VALUES(`api_perms`), `web_perms`=VALUES(`web_perms`);

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, m.`menu_id`, NOW(), NOW() FROM `t_menu` m
WHERE m.`menu_id` BETWEEN 865 AND 875
AND NOT EXISTS (SELECT 1 FROM `t_role_menu` r WHERE r.`role_id`=1 AND r.`menu_id`=m.`menu_id`);
