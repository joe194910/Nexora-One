-- 标准 MCP Server 接入。
-- 本脚本只增加页面/按钮权限，不给后端接口增加权限注解。

CREATE TABLE IF NOT EXISTS `nexora_one_mcp_server` (
  `server_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'MCP Server 主键',
  `application_id` BIGINT NOT NULL COMMENT '所属应用主键',
  `server_code` VARCHAR(100) NOT NULL COMMENT '平台内唯一 Server 编码',
  `server_name` VARCHAR(100) NOT NULL COMMENT 'Server 展示名称',
  `description` VARCHAR(2000) DEFAULT NULL COMMENT 'Server 能力与使用范围说明',
  `endpoint_url` VARCHAR(1000) NOT NULL COMMENT '标准 MCP Streamable HTTP endpoint',
  `transport_type` VARCHAR(30) NOT NULL DEFAULT 'STREAMABLE_HTTP' COMMENT '传输协议',
  `auth_type` VARCHAR(30) NOT NULL DEFAULT 'NONE' COMMENT 'NONE BEARER API_KEY_HEADER',
  `auth_header_name` VARCHAR(100) DEFAULT NULL COMMENT 'API Key 请求头名称',
  `auth_secret_cipher` TEXT DEFAULT NULL COMMENT '加密保存的 Token 或 API Key',
  `timeout_seconds` INT NOT NULL DEFAULT 30 COMMENT '初始化、发现和调用超时秒数',
  `enabled_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许探活和调用',
  `online_status` VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'UNKNOWN ONLINE OFFLINE ABNORMAL',
  `protocol_version` VARCHAR(50) DEFAULT NULL COMMENT '协商得到的 MCP 协议版本',
  `server_version` VARCHAR(100) DEFAULT NULL COMMENT '远端 Server 版本',
  `discovered_tool_count` INT NOT NULL DEFAULT 0 COMMENT '最近发现工具数',
  `last_probe_time` DATETIME DEFAULT NULL COMMENT '最近探活时间',
  `last_probe_message` VARCHAR(1000) DEFAULT NULL COMMENT '最近探活结果',
  `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_user_id` BIGINT DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`server_id`),
  UNIQUE KEY `uk_mcp_server_code` (`server_code`),
  KEY `idx_mcp_server_application` (`application_id`, `update_time`),
  KEY `idx_mcp_server_status` (`enabled_flag`, `online_status`),
  CONSTRAINT `fk_mcp_server_application`
    FOREIGN KEY (`application_id`) REFERENCES `nexora_one_application` (`application_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准 MCP Server 注册表';

-- MySQL 8 不统一支持 ADD COLUMN IF NOT EXISTS，使用元数据检查保证重复执行安全。
SET @widen_mcp_auth_secret = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'nexora_one_mcp_server'
     AND COLUMN_NAME = 'auth_secret_cipher'
     AND CHARACTER_MAXIMUM_LENGTH < 65535) > 0,
  'ALTER TABLE `nexora_one_mcp_server` MODIFY COLUMN `auth_secret_cipher` TEXT DEFAULT NULL COMMENT ''加密保存的 Token 或 API Key''',
  'SELECT 1'
);
PREPARE widen_mcp_auth_secret_stmt FROM @widen_mcp_auth_secret;
EXECUTE widen_mcp_auth_secret_stmt;
DEALLOCATE PREPARE widen_mcp_auth_secret_stmt;

SET @add_ai_tool_mcp_server_id = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'nexora_one_ai_tool'
     AND COLUMN_NAME = 'mcp_server_id') = 0,
  'ALTER TABLE `nexora_one_ai_tool` ADD COLUMN `mcp_server_id` BIGINT DEFAULT NULL COMMENT ''标准 MCP Server 主键'' AFTER `application_id`',
  'SELECT 1'
);
PREPARE add_ai_tool_mcp_server_id_stmt FROM @add_ai_tool_mcp_server_id;
EXECUTE add_ai_tool_mcp_server_id_stmt;
DEALLOCATE PREPARE add_ai_tool_mcp_server_id_stmt;

SET @add_ai_tool_remote_name = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'nexora_one_ai_tool'
     AND COLUMN_NAME = 'remote_tool_name') = 0,
  'ALTER TABLE `nexora_one_ai_tool` ADD COLUMN `remote_tool_name` VARCHAR(255) DEFAULT NULL COMMENT ''远端 MCP 原始工具名称'' AFTER `mcp_server_id`',
  'SELECT 1'
);
PREPARE add_ai_tool_remote_name_stmt FROM @add_ai_tool_remote_name;
EXECUTE add_ai_tool_remote_name_stmt;
DEALLOCATE PREPARE add_ai_tool_remote_name_stmt;

SET @add_ai_tool_schema_sync_required = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'nexora_one_ai_tool'
     AND COLUMN_NAME = 'schema_sync_required') = 0,
  'ALTER TABLE `nexora_one_ai_tool` ADD COLUMN `schema_sync_required` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''远端工具 Schema 是否等待显式同步'' AFTER `last_test_message`',
  'SELECT 1'
);
PREPARE add_ai_tool_schema_sync_required_stmt FROM @add_ai_tool_schema_sync_required;
EXECUTE add_ai_tool_schema_sync_required_stmt;
DEALLOCATE PREPARE add_ai_tool_schema_sync_required_stmt;

SET @add_ai_tool_mcp_unique = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'nexora_one_ai_tool'
     AND INDEX_NAME = 'uk_ai_tool_mcp_remote') = 0,
  'ALTER TABLE `nexora_one_ai_tool` ADD UNIQUE KEY `uk_ai_tool_mcp_remote` (`mcp_server_id`, `remote_tool_name`)',
  'SELECT 1'
);
PREPARE add_ai_tool_mcp_unique_stmt FROM @add_ai_tool_mcp_unique;
EXECUTE add_ai_tool_mcp_unique_stmt;
DEALLOCATE PREPARE add_ai_tool_mcp_unique_stmt;

SET @add_ai_tool_mcp_foreign_key = IF(
  (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
   WHERE CONSTRAINT_SCHEMA = DATABASE()
     AND CONSTRAINT_NAME = 'fk_ai_tool_mcp_server') = 0,
  'ALTER TABLE `nexora_one_ai_tool` ADD CONSTRAINT `fk_ai_tool_mcp_server` FOREIGN KEY (`mcp_server_id`) REFERENCES `nexora_one_mcp_server` (`server_id`) ON DELETE RESTRICT',
  'SELECT 1'
);
PREPARE add_ai_tool_mcp_foreign_key_stmt FROM @add_ai_tool_mcp_foreign_key;
EXECUTE add_ai_tool_mcp_foreign_key_stmt;
DEALLOCATE PREPARE add_ai_tool_mcp_foreign_key_stmt;

-- 标准 MCP Server 的按钮权限仅用于页面展示。
INSERT INTO `t_menu`
(`menu_id`,`menu_name`,`menu_type`,`parent_id`,`sort`,`path`,`component`,`perms_type`,`api_perms`,`web_perms`,`icon`,`frame_flag`,`cache_flag`,`visible_flag`,`disabled_flag`,`deleted_flag`,`create_user_id`,`update_user_id`,`create_time`,`update_time`)
VALUES
(916,'维护MCP Server',3,910,50,NULL,NULL,2,NULL,'mcp:server:save',NULL,0,0,0,0,0,1,1,NOW(),NOW()),
(917,'探活MCP Server',3,910,60,NULL,NULL,2,NULL,'mcp:server:probe',NULL,0,0,0,0,0,1,1,NOW(),NOW())
AS incoming
ON DUPLICATE KEY UPDATE
  `menu_name`=incoming.`menu_name`,
  `parent_id`=incoming.`parent_id`,
  `web_perms`=incoming.`web_perms`,
  `update_time`=NOW();

INSERT INTO `t_role_menu` (`role_id`,`menu_id`,`create_time`,`update_time`)
SELECT 1,m.`menu_id`,NOW(),NOW() FROM `t_menu` m
WHERE m.`menu_id` BETWEEN 916 AND 917
AND NOT EXISTS (
  SELECT 1 FROM `t_role_menu` x WHERE x.`role_id`=1 AND x.`menu_id`=m.`menu_id`
);
