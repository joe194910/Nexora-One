-- HTTP 方式 MCP/AI 工具第一版。
-- 工具认证复用应用 App ID、App Secret 与 Access Token，不引入独立 MCP 凭证。

CREATE TABLE IF NOT EXISTS `nexora_one_ai_tool` (
  `tool_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '工具主键',
  `tool_code` VARCHAR(100) NOT NULL COMMENT '模型调用使用的稳定编码',
  `tool_name` VARCHAR(100) NOT NULL COMMENT '工具名称',
  `description` VARCHAR(2000) NOT NULL COMMENT '给模型使用的工具说明',
  `source_type` VARCHAR(30) NOT NULL COMMENT 'PLATFORM_API EXTERNAL_HTTP STANDARD_MCP',
  `tool_type` VARCHAR(20) NOT NULL COMMENT 'QUERY ACTION',
  `open_api_id` BIGINT DEFAULT NULL COMMENT '平台 API 主键',
  `source_api_version_id` BIGINT DEFAULT NULL COMMENT '生成 Schema 的 API 版本快照',
  `application_id` BIGINT DEFAULT NULL COMMENT '第三方 HTTP 工具所属应用',
  `callback_url` VARCHAR(1000) DEFAULT NULL COMMENT '第三方回调地址',
  `http_method` VARCHAR(10) NOT NULL DEFAULT 'POST' COMMENT 'HTTP 方法',
  `content_type` VARCHAR(100) NOT NULL DEFAULT 'application/json' COMMENT '内容类型',
  `timeout_seconds` INT NOT NULL DEFAULT 30 COMMENT '调用超时',
  `risk_level` VARCHAR(20) NOT NULL DEFAULT 'LOW' COMMENT 'LOW MEDIUM HIGH',
  `confirmation_policy` VARCHAR(30) NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO REQUIRED',
  `input_schema` MEDIUMTEXT NOT NULL COMMENT 'JSON Schema',
  `output_schema` MEDIUMTEXT DEFAULT NULL COMMENT 'JSON Schema',
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'DRAFT PENDING APPROVED REJECTED',
  `audit_remark` VARCHAR(1000) DEFAULT NULL COMMENT '审核意见',
  `enabled_status` VARCHAR(20) NOT NULL DEFAULT 'DISABLED' COMMENT 'ENABLED DISABLED',
  `online_status` VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'UNKNOWN ONLINE OFFLINE ABNORMAL',
  `last_test_status` VARCHAR(20) DEFAULT NULL COMMENT 'SUCCESS FAILED',
  `last_test_message` VARCHAR(1000) DEFAULT NULL COMMENT '最近测试结果',
  `last_test_time` DATETIME DEFAULT NULL COMMENT '最近测试时间',
  `last_heartbeat_time` DATETIME DEFAULT NULL COMMENT '最近心跳时间',
  `last_call_time` DATETIME DEFAULT NULL COMMENT '最近调用时间',
  `total_call_count` BIGINT NOT NULL DEFAULT 0 COMMENT '累计调用量',
  `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_user_id` BIGINT DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`tool_id`),
  UNIQUE KEY `uk_ai_tool_code` (`tool_code`),
  UNIQUE KEY `uk_ai_tool_open_api` (`open_api_id`),
  KEY `idx_ai_tool_source_status` (`source_type`, `audit_status`, `enabled_status`),
  KEY `idx_ai_tool_application` (`application_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 工具注册表';

CREATE TABLE IF NOT EXISTS `nexora_one_ai_tool_schema_sync` (
  `sync_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '同步记录主键',
  `tool_id` BIGINT NOT NULL COMMENT '工具主键',
  `open_api_id` BIGINT NOT NULL COMMENT '平台 API 主键',
  `api_version_id` BIGINT NOT NULL COMMENT '同步的 API 版本',
  `api_version_no` VARCHAR(30) NOT NULL COMMENT 'API 版本号快照',
  `sync_type` VARCHAR(20) NOT NULL COMMENT 'CREATE MANUAL',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
  PRIMARY KEY (`sync_id`),
  KEY `idx_ai_tool_sync_tool` (`tool_id`, `create_time`),
  CONSTRAINT `fk_ai_tool_sync_tool` FOREIGN KEY (`tool_id`) REFERENCES `nexora_one_ai_tool` (`tool_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台 API 工具 Schema 同步历史';

CREATE TABLE IF NOT EXISTS `nexora_one_kb_assistant_tool` (
  `assistant_id` BIGINT NOT NULL COMMENT '智能助手',
  `tool_id` BIGINT NOT NULL COMMENT 'AI 工具',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
  PRIMARY KEY (`assistant_id`, `tool_id`),
  KEY `idx_assistant_tool_tool` (`tool_id`),
  CONSTRAINT `fk_assistant_tool_assistant` FOREIGN KEY (`assistant_id`) REFERENCES `nexora_one_kb_assistant` (`assistant_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_assistant_tool_tool` FOREIGN KEY (`tool_id`) REFERENCES `nexora_one_ai_tool` (`tool_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能助手 AI 工具关联';

CREATE TABLE IF NOT EXISTS `nexora_one_ai_tool_call_log` (
  `call_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '调用主键',
  `request_id` VARCHAR(64) NOT NULL COMMENT '请求幂等标识',
  `trace_id` VARCHAR(64) NOT NULL COMMENT '链路标识',
  `tool_id` BIGINT NOT NULL COMMENT '工具主键',
  `tool_code` VARCHAR(100) NOT NULL COMMENT '工具编码快照',
  `source_type` VARCHAR(30) NOT NULL COMMENT '来源类型快照',
  `application_id` BIGINT DEFAULT NULL COMMENT '来源应用',
  `assistant_id` BIGINT DEFAULT NULL COMMENT '智能助手',
  `conversation_id` BIGINT DEFAULT NULL COMMENT '会话',
  `user_id` BIGINT DEFAULT NULL COMMENT '调用用户',
  `status` VARCHAR(30) NOT NULL COMMENT '调用状态',
  `confirmation_required` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要确认',
  `confirmation_time` DATETIME DEFAULT NULL COMMENT '确认时间',
  `arguments_json` MEDIUMTEXT DEFAULT NULL COMMENT '模型参数',
  `result_json` MEDIUMTEXT DEFAULT NULL COMMENT '工具结果',
  `pending_context_json` MEDIUMTEXT DEFAULT NULL COMMENT '待确认模型上下文',
  `http_status` INT DEFAULT NULL COMMENT 'HTTP 状态码',
  `duration_ms` BIGINT DEFAULT NULL COMMENT '耗时',
  `error_code` VARCHAR(100) DEFAULT NULL COMMENT '错误码',
  `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`call_id`),
  UNIQUE KEY `uk_ai_tool_call_request` (`request_id`),
  KEY `idx_ai_tool_call_trace` (`trace_id`),
  KEY `idx_ai_tool_call_tool_time` (`tool_id`, `create_time`),
  KEY `idx_ai_tool_call_user_time` (`user_id`, `create_time`),
  CONSTRAINT `fk_ai_tool_call_tool` FOREIGN KEY (`tool_id`) REFERENCES `nexora_one_ai_tool` (`tool_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 工具调用与确认日志';

-- MySQL 8 不统一支持 ADD COLUMN IF NOT EXISTS，使用元数据检查保证重复执行安全。
SET @add_max_tool_calls = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'nexora_one_kb_assistant'
     AND COLUMN_NAME = 'max_tool_calls') = 0,
  'ALTER TABLE `nexora_one_kb_assistant` ADD COLUMN `max_tool_calls` INT NOT NULL DEFAULT 3 COMMENT ''单轮最大工具调用数'' AFTER `show_citations`',
  'SELECT 1'
);
PREPARE add_max_tool_calls_stmt FROM @add_max_tool_calls;
EXECUTE add_max_tool_calls_stmt;
DEALLOCATE PREPARE add_max_tool_calls_stmt;

SET @add_tool_debug_flag = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'nexora_one_kb_assistant'
     AND COLUMN_NAME = 'tool_debug_flag') = 0,
  'ALTER TABLE `nexora_one_kb_assistant` ADD COLUMN `tool_debug_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''展示工具调试信息'' AFTER `max_tool_calls`',
  'SELECT 1'
);
PREPARE add_tool_debug_flag_stmt FROM @add_tool_debug_flag;
EXECUTE add_tool_debug_flag_stmt;
DEALLOCATE PREPARE add_tool_debug_flag_stmt;

SET @add_allow_action_tool_flag = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'nexora_one_kb_assistant'
     AND COLUMN_NAME = 'allow_action_tool_flag') = 0,
  'ALTER TABLE `nexora_one_kb_assistant` ADD COLUMN `allow_action_tool_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''允许操作类工具'' AFTER `tool_debug_flag`',
  'SELECT 1'
);
PREPARE add_allow_action_tool_flag_stmt FROM @add_allow_action_tool_flag;
EXECUTE add_allow_action_tool_flag_stmt;
DEALLOCATE PREPARE add_allow_action_tool_flag_stmt;

SET @add_tool_calls_json = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'nexora_one_kb_message'
     AND COLUMN_NAME = 'tool_calls_json') = 0,
  'ALTER TABLE `nexora_one_kb_message` ADD COLUMN `tool_calls_json` MEDIUMTEXT DEFAULT NULL COMMENT ''工具调用调试信息'' AFTER `citations_json`',
  'SELECT 1'
);
PREPARE add_tool_calls_json_stmt FROM @add_tool_calls_json;
EXECUTE add_tool_calls_json_stmt;
DEALLOCATE PREPARE add_tool_calls_json_stmt;

-- MCP 工具页面与 API 管理同属 API 开放平台。这里只配置页面/按钮权限，不给接口增加权限注解。
INSERT INTO `t_menu`
(`menu_id`,`menu_name`,`menu_type`,`parent_id`,`sort`,`path`,`component`,`perms_type`,`api_perms`,`web_perms`,`icon`,`frame_flag`,`cache_flag`,`visible_flag`,`disabled_flag`,`deleted_flag`,`create_user_id`,`update_user_id`,`create_time`,`update_time`)
VALUES
(910,'MCP工具管理',2,820,25,'/open-api/mcp-tools','business/open-api/mcp-tool-management.vue',1,NULL,'mcp:tool:query','RobotOutlined',0,0,1,0,0,1,1,NOW(),NOW()),
(911,'查看MCP工具',3,910,10,NULL,NULL,2,NULL,'mcp:tool:query',NULL,0,0,0,0,0,1,1,NOW(),NOW()),
(912,'维护MCP工具',3,910,20,NULL,NULL,2,NULL,'mcp:tool:save',NULL,0,0,0,0,0,1,1,NOW(),NOW()),
(913,'测试MCP工具',3,910,30,NULL,NULL,2,NULL,'mcp:tool:test',NULL,0,0,0,0,0,1,1,NOW(),NOW()),
(914,'审核MCP工具',3,910,40,NULL,NULL,2,NULL,'mcp:tool:review',NULL,0,0,0,0,0,1,1,NOW(),NOW()),
(915,'发布API为AI工具',3,821,70,NULL,NULL,2,NULL,'open-api:ai-tool:publish',NULL,0,0,0,0,0,1,1,NOW(),NOW())
AS incoming
ON DUPLICATE KEY UPDATE
  `menu_name`=incoming.`menu_name`, `parent_id`=incoming.`parent_id`,
  `component`=incoming.`component`, `web_perms`=incoming.`web_perms`, `update_time`=NOW();

INSERT INTO `t_role_menu` (`role_id`,`menu_id`,`create_time`,`update_time`)
SELECT 1,m.`menu_id`,NOW(),NOW() FROM `t_menu` m
WHERE m.`menu_id` BETWEEN 910 AND 915
AND NOT EXISTS (
  SELECT 1 FROM `t_role_menu` x WHERE x.`role_id`=1 AND x.`menu_id`=m.`menu_id`
);
