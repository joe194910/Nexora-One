-- AI 平台管理：模型服务、模型、向量数据库、文档解析、调用日志与用量统计
-- 执行日期：2026-09-15

CREATE TABLE IF NOT EXISTS `nexora_one_ai_model_service` (
    `service_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模型服务主键',
    `service_name` VARCHAR(100) NOT NULL COMMENT '服务名称',
    `provider_type` VARCHAR(30) NOT NULL COMMENT '服务商类型',
    `protocol_type` VARCHAR(30) NOT NULL DEFAULT 'OPENAI' COMMENT '接口协议',
    `base_url` VARCHAR(500) NOT NULL COMMENT '服务基础地址',
    `api_key_cipher` VARCHAR(2000) DEFAULT NULL COMMENT '加密后的 API Key',
    `organization_id` VARCHAR(100) DEFAULT NULL COMMENT '组织 ID',
    `request_timeout_seconds` INT NOT NULL DEFAULT 60 COMMENT '请求超时秒数',
    `capabilities` VARCHAR(500) DEFAULT NULL COMMENT '支持能力',
    `default_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认服务',
    `enabled_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `connection_status` VARCHAR(20) NOT NULL DEFAULT 'UNTESTED' COMMENT '连接状态',
    `last_test_time` DATETIME DEFAULT NULL COMMENT '最近测试时间',
    `last_error` VARCHAR(1000) DEFAULT NULL COMMENT '最近错误信息',
    `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_user_id` BIGINT DEFAULT NULL COMMENT '更新人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`service_id`),
    UNIQUE KEY `uk_ai_model_service_name` (`service_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型服务配置';

CREATE TABLE IF NOT EXISTS `nexora_one_ai_model` (
    `model_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模型主键',
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `model_code` VARCHAR(150) NOT NULL COMMENT '服务商模型编码',
    `model_type` VARCHAR(30) NOT NULL COMMENT '模型类型',
    `service_id` BIGINT NOT NULL COMMENT '所属模型服务主键',
    `capabilities` VARCHAR(500) DEFAULT NULL COMMENT '支持能力',
    `context_length` INT DEFAULT NULL COMMENT '上下文长度',
    `embedding_dimension` INT DEFAULT NULL COMMENT '向量维度',
    `temperature` DECIMAL(4,2) DEFAULT 0.70 COMMENT '默认温度',
    `max_output_tokens` INT DEFAULT 2048 COMMENT '最大输出 Token',
    `input_price` DECIMAL(14,6) NOT NULL DEFAULT 0 COMMENT '每百万输入 Token 价格',
    `output_price` DECIMAL(14,6) NOT NULL DEFAULT 0 COMMENT '每百万输出 Token 价格',
    `default_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认模型',
    `enabled_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_user_id` BIGINT DEFAULT NULL COMMENT '更新人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`model_id`),
    UNIQUE KEY `uk_ai_model_service_code` (`service_id`, `model_code`),
    KEY `idx_ai_model_type_status` (`model_type`, `enabled_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型配置';

CREATE TABLE IF NOT EXISTS `nexora_one_ai_vector_database` (
    `vector_database_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '向量数据库主键',
    `instance_name` VARCHAR(100) NOT NULL COMMENT '实例名称',
    `database_type` VARCHAR(30) NOT NULL COMMENT '数据库类型',
    `service_url` VARCHAR(500) NOT NULL COMMENT '服务地址',
    `api_key_cipher` VARCHAR(2000) DEFAULT NULL COMMENT '加密后的 API Key',
    `collection_prefix` VARCHAR(100) NOT NULL DEFAULT 'nexora_kb_' COMMENT '集合前缀',
    `request_timeout_seconds` INT NOT NULL DEFAULT 30 COMMENT '请求超时秒数',
    `tls_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否使用 TLS',
    `default_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认实例',
    `enabled_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `connection_status` VARCHAR(20) NOT NULL DEFAULT 'UNTESTED' COMMENT '连接状态',
    `collection_count` INT NOT NULL DEFAULT 0 COMMENT '集合数量',
    `vector_count` BIGINT NOT NULL DEFAULT 0 COMMENT '向量总数',
    `last_test_time` DATETIME DEFAULT NULL COMMENT '最近测试时间',
    `last_error` VARCHAR(1000) DEFAULT NULL COMMENT '最近错误信息',
    `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_user_id` BIGINT DEFAULT NULL COMMENT '更新人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`vector_database_id`),
    UNIQUE KEY `uk_ai_vector_instance_name` (`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 向量数据库配置';

CREATE TABLE IF NOT EXISTS `nexora_one_ai_document_parse_config` (
    `config_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置主键',
    `supported_formats` VARCHAR(300) NOT NULL COMMENT '支持的文件格式',
    `parser_type` VARCHAR(30) NOT NULL DEFAULT 'TIKA' COMMENT '解析器类型',
    `pdf_mode` VARCHAR(30) NOT NULL DEFAULT 'PAGE_PARAGRAPH' COMMENT 'PDF 解析模式',
    `extract_table` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否提取表格',
    `preserve_heading` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否保留标题层级',
    `extract_image` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否提取图片',
    `ocr_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用 OCR',
    `ocr_service` VARCHAR(500) DEFAULT NULL COMMENT 'OCR HTTP 接口地址',
    `chunk_method` VARCHAR(30) NOT NULL DEFAULT 'RECURSIVE' COMMENT '切片方式',
    `chunk_size` INT NOT NULL DEFAULT 800 COMMENT '切片长度',
    `chunk_overlap` INT NOT NULL DEFAULT 100 COMMENT '重叠长度',
    `min_chunk_size` INT NOT NULL DEFAULT 100 COMMENT '最小切片长度',
    `separators` VARCHAR(300) DEFAULT NULL COMMENT '分隔符',
    `preserve_title_metadata` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否保留标题元数据',
    `concurrency` INT NOT NULL DEFAULT 3 COMMENT '并发任务数',
    `timeout_minutes` INT NOT NULL DEFAULT 10 COMMENT '解析超时分钟数',
    `retry_count` INT NOT NULL DEFAULT 2 COMMENT '失败重试次数',
    `duplicate_strategy` VARCHAR(30) NOT NULL DEFAULT 'SKIP' COMMENT '重复文件策略',
    `continue_on_error` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '异常文档是否继续',
    `embedding_model_id` BIGINT DEFAULT NULL COMMENT '向量模型主键',
    `update_user_id` BIGINT DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 文档解析配置';

CREATE TABLE IF NOT EXISTS `nexora_one_ai_call_log` (
    `call_log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '调用日志主键',
    `trace_id` VARCHAR(64) NOT NULL COMMENT '链路标识',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户主键',
    `user_name` VARCHAR(100) DEFAULT NULL COMMENT '用户名称',
    `user_ip` VARCHAR(64) DEFAULT NULL COMMENT '用户 IP',
    `service_id` BIGINT DEFAULT NULL COMMENT '模型服务主键',
    `provider_name` VARCHAR(100) DEFAULT NULL COMMENT '服务商名称快照',
    `model_id` BIGINT DEFAULT NULL COMMENT '模型主键',
    `model_code` VARCHAR(150) DEFAULT NULL COMMENT '模型编码快照',
    `call_type` VARCHAR(30) NOT NULL COMMENT '调用类型',
    `source_type` VARCHAR(30) DEFAULT NULL COMMENT '来源类型',
    `source_name` VARCHAR(100) DEFAULT NULL COMMENT '来源名称',
    `assistant_id` BIGINT DEFAULT NULL COMMENT '助手主键',
    `knowledge_base_id` BIGINT DEFAULT NULL COMMENT '知识库主键',
    `input_tokens` INT NOT NULL DEFAULT 0 COMMENT '输入 Token',
    `output_tokens` INT NOT NULL DEFAULT 0 COMMENT '输出 Token',
    `total_tokens` INT NOT NULL DEFAULT 0 COMMENT '总 Token',
    `queue_duration_ms` BIGINT DEFAULT 0 COMMENT '排队耗时毫秒',
    `connect_duration_ms` BIGINT DEFAULT 0 COMMENT '连接耗时毫秒',
    `model_duration_ms` BIGINT DEFAULT 0 COMMENT '模型耗时毫秒',
    `total_duration_ms` BIGINT DEFAULT 0 COMMENT '总耗时毫秒',
    `success_flag` TINYINT(1) NOT NULL COMMENT '是否成功',
    `error_code` VARCHAR(100) DEFAULT NULL COMMENT '错误码',
    `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    `request_summary` TEXT DEFAULT NULL COMMENT '请求摘要',
    `response_summary` TEXT DEFAULT NULL COMMENT '响应摘要',
    `estimated_cost` DECIMAL(14,6) NOT NULL DEFAULT 0 COMMENT '预估费用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`call_log_id`),
    UNIQUE KEY `uk_ai_call_trace` (`trace_id`),
    KEY `idx_ai_call_time` (`create_time`),
    KEY `idx_ai_call_model_time` (`model_id`, `create_time`),
    KEY `idx_ai_call_user_time` (`user_id`, `create_time`),
    KEY `idx_ai_call_status_time` (`success_flag`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 调用审计日志';

INSERT INTO `nexora_one_ai_document_parse_config`
(`supported_formats`, `parser_type`, `pdf_mode`, `extract_table`, `preserve_heading`, `extract_image`,
 `ocr_enabled`, `chunk_method`, `chunk_size`, `chunk_overlap`, `min_chunk_size`, `separators`,
 `preserve_title_metadata`, `concurrency`, `timeout_minutes`, `retry_count`, `duplicate_strategy`,
 `continue_on_error`, `update_time`)
SELECT 'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,csv,png,jpg,jpeg', 'TIKA', 'PAGE_PARAGRAPH', 1, 1, 0,
       0, 'RECURSIVE', 800, 100, 100, '\n\n,\n,。！？；', 1, 3, 10, 2, 'SKIP', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `nexora_one_ai_document_parse_config`);

-- AI 平台菜单
INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
SELECT 839, 'AI平台管理', 1, 0, 35, '/ai-platform', NULL, 1, NULL, NULL, 'RobotOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_id` = 839);

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
VALUES
(840, '模型服务', 2, 839, 10, '/ai-platform/model-service', '/business/ai/model-service.vue', 1, NULL, 'ai:model-service:query', 'ApiOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()),
(841, '模型管理', 2, 839, 20, '/ai-platform/model-management', '/business/ai/model-management.vue', 1, NULL, 'ai:model:query', 'DeploymentUnitOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()),
(842, '向量数据库', 2, 839, 30, '/ai-platform/vector-database', '/business/ai/vector-database.vue', 1, NULL, 'ai:vector:query', 'DatabaseOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()),
(843, '文档解析配置', 2, 839, 40, '/ai-platform/document-parse-config', '/business/ai/document-parse-config.vue', 1, NULL, 'ai:document:query', 'FileSearchOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()),
(844, '调用日志', 2, 839, 50, '/ai-platform/call-log', '/business/ai/call-log.vue', 1, NULL, 'ai:call-log:query', 'ProfileOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW()),
(845, '用量统计', 2, 839, 60, '/ai-platform/usage-statistics', '/business/ai/usage-statistics.vue', 1, NULL, 'ai:statistics:query', 'BarChartOutlined', 0, 0, 1, 0, 0, 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`), `component` = VALUES(`component`), `update_time` = NOW();

INSERT INTO `t_menu`
(`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `update_user_id`, `create_time`, `update_time`)
VALUES
(846, '查询模型服务', 3, 840, 10, 2, 'ai:model-service:query', 'ai:model-service:query', 0, 0, 0, 1, 1, NOW(), NOW()),
(847, '维护模型服务', 3, 840, 20, 2, 'ai:model-service:save', 'ai:model-service:save', 0, 0, 0, 1, 1, NOW(), NOW()),
(848, '测试模型服务', 3, 840, 30, 2, 'ai:model-service:test', 'ai:model-service:test', 0, 0, 0, 1, 1, NOW(), NOW()),
(849, '删除模型服务', 3, 840, 40, 2, 'ai:model-service:delete', 'ai:model-service:delete', 0, 0, 0, 1, 1, NOW(), NOW()),
(850, '查询模型', 3, 841, 10, 2, 'ai:model:query', 'ai:model:query', 0, 0, 0, 1, 1, NOW(), NOW()),
(851, '维护模型', 3, 841, 20, 2, 'ai:model:save', 'ai:model:save', 0, 0, 0, 1, 1, NOW(), NOW()),
(852, '调试模型', 3, 841, 30, 2, 'ai:model:debug', 'ai:model:debug', 0, 0, 0, 1, 1, NOW(), NOW()),
(853, '删除模型', 3, 841, 40, 2, 'ai:model:delete', 'ai:model:delete', 0, 0, 0, 1, 1, NOW(), NOW()),
(854, '查询向量数据库', 3, 842, 10, 2, 'ai:vector:query', 'ai:vector:query', 0, 0, 0, 1, 1, NOW(), NOW()),
(855, '维护向量数据库', 3, 842, 20, 2, 'ai:vector:save', 'ai:vector:save', 0, 0, 0, 1, 1, NOW(), NOW()),
(856, '测试向量数据库', 3, 842, 30, 2, 'ai:vector:test', 'ai:vector:test', 0, 0, 0, 1, 1, NOW(), NOW()),
(857, '删除向量数据库', 3, 842, 40, 2, 'ai:vector:delete', 'ai:vector:delete', 0, 0, 0, 1, 1, NOW(), NOW()),
(858, '查询解析配置', 3, 843, 10, 2, 'ai:document:query', 'ai:document:query', 0, 0, 0, 1, 1, NOW(), NOW()),
(859, '保存解析配置', 3, 843, 20, 2, 'ai:document:save', 'ai:document:save', 0, 0, 0, 1, 1, NOW(), NOW()),
(860, '测试文档解析', 3, 843, 30, 2, 'ai:document:test', 'ai:document:test', 0, 0, 0, 1, 1, NOW(), NOW()),
(861, '查询调用日志', 3, 844, 10, 2, 'ai:call-log:query', 'ai:call-log:query', 0, 0, 0, 1, 1, NOW(), NOW()),
(862, '查看调用详情', 3, 844, 20, 2, 'ai:call-log:detail', 'ai:call-log:detail', 0, 0, 0, 1, 1, NOW(), NOW()),
(863, '导出调用日志', 3, 844, 30, 2, 'ai:call-log:export', 'ai:call-log:export', 0, 0, 0, 1, 1, NOW(), NOW()),
(864, '查询用量统计', 3, 845, 10, 2, 'ai:statistics:query', 'ai:statistics:query', 0, 0, 0, 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`), `api_perms` = VALUES(`api_perms`), `web_perms` = VALUES(`web_perms`), `update_time` = NOW();

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, m.`menu_id`, NOW(), NOW()
FROM `t_menu` m
WHERE m.`menu_id` BETWEEN 839 AND 864
  AND NOT EXISTS (
      SELECT 1 FROM `t_role_menu` rm
      WHERE rm.`role_id` = 1 AND rm.`menu_id` = m.`menu_id`
  );
