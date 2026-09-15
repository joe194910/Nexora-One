-- 用户知识库模块；先完成现有 AI 平台与文档解析迁移，再执行本文件。
CREATE TABLE IF NOT EXISTS `nexora_one_kb_ingest_context` (
  `context_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '入库上下文',
  `owner_id` BIGINT NOT NULL COMMENT '所属用户',
  `parse_plan_id` BIGINT NOT NULL COMMENT '解析方案',
  `embedding_model_id` BIGINT NOT NULL COMMENT '向量模型',
  `vector_database_id` BIGINT NOT NULL COMMENT 'Qdrant 实例',
  `ingest_base_id` BIGINT NOT NULL COMMENT '原解析任务使用的内部知识库',
  PRIMARY KEY (`context_id`),
  UNIQUE KEY `uk_kb_context` (`owner_id`, `parse_plan_id`, `embedding_model_id`, `vector_database_id`),
  UNIQUE KEY `uk_kb_ingest_base` (`ingest_base_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户向量入库上下文';

CREATE TABLE IF NOT EXISTS `nexora_one_kb_document` (
  `document_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档主键',
  `owner_id` BIGINT NOT NULL COMMENT '所属用户',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
  `file_type` VARCHAR(20) NOT NULL COMMENT '扩展名',
  `file_size` BIGINT NOT NULL COMMENT '大小（字节）',
  `file_hash` CHAR(64) NOT NULL COMMENT 'SHA256',
  `object_key` VARCHAR(500) NOT NULL COMMENT 'MinIO 对象键',
  `parse_plan_id` BIGINT NOT NULL COMMENT '解析方案',
  `embedding_model_id` BIGINT NOT NULL COMMENT '向量模型',
  `vector_database_id` BIGINT NOT NULL COMMENT 'Qdrant 实例',
  `task_id` BIGINT DEFAULT NULL COMMENT '解析任务',
  `ingest_base_id` BIGINT NOT NULL COMMENT '内部入库知识库',
  `status` VARCHAR(20) NOT NULL COMMENT 'PROCESSING READY FAILED',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`document_id`),
  UNIQUE KEY `uk_kb_doc_version` (`owner_id`, `file_hash`, `parse_plan_id`, `embedding_model_id`, `vector_database_id`),
  KEY `idx_kb_doc_owner` (`owner_id`, `create_time`),
  KEY `idx_kb_doc_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='我的文档';

CREATE TABLE IF NOT EXISTS `nexora_one_kb_base` (
  `base_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '知识库主键',
  `owner_id` BIGINT NOT NULL COMMENT '所属用户',
  `base_name` VARCHAR(100) NOT NULL COMMENT '名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `enabled_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`base_id`),
  UNIQUE KEY `uk_kb_base_name` (`owner_id`, `base_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户知识库';

CREATE TABLE IF NOT EXISTS `nexora_one_kb_base_document` (
  `base_id` BIGINT NOT NULL COMMENT '知识库',
  `document_id` BIGINT NOT NULL COMMENT '已就绪文档',
  PRIMARY KEY (`base_id`, `document_id`),
  KEY `idx_kb_document_bases` (`document_id`),
  CONSTRAINT `fk_kb_base_document_base` FOREIGN KEY (`base_id`) REFERENCES `nexora_one_kb_base` (`base_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_kb_base_document_doc` FOREIGN KEY (`document_id`) REFERENCES `nexora_one_kb_document` (`document_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档关联';

CREATE TABLE IF NOT EXISTS `nexora_one_kb_assistant` (
  `assistant_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '助手主键',
  `owner_id` BIGINT NOT NULL COMMENT '所属用户',
  `assistant_name` VARCHAR(100) NOT NULL COMMENT '助手名称',
  `system_prompt` TEXT COMMENT '系统提示词',
  `model_id` BIGINT NOT NULL COMMENT '对话模型',
  `top_k` INT NOT NULL DEFAULT 5 COMMENT '检索数量',
  `score_threshold` DECIMAL(5,4) NOT NULL DEFAULT 0.6500 COMMENT '相似度阈值',
  `show_citations` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '展示引用',
  `enabled_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`assistant_id`),
  KEY `idx_kb_assistant_owner` (`owner_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能助手';

CREATE TABLE IF NOT EXISTS `nexora_one_kb_assistant_base` (
  `assistant_id` BIGINT NOT NULL COMMENT '助手',
  `base_id` BIGINT NOT NULL COMMENT '业务知识库',
  PRIMARY KEY (`assistant_id`, `base_id`),
  KEY `idx_kb_assistant_base` (`base_id`),
  CONSTRAINT `fk_kb_assistant_base_assistant` FOREIGN KEY (`assistant_id`) REFERENCES `nexora_one_kb_assistant` (`assistant_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_kb_assistant_base_base` FOREIGN KEY (`base_id`) REFERENCES `nexora_one_kb_base` (`base_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助手知识库关联';

CREATE TABLE IF NOT EXISTS `nexora_one_kb_conversation` (
  `conversation_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话主键',
  `assistant_id` BIGINT NOT NULL COMMENT '助手',
  `owner_id` BIGINT NOT NULL COMMENT '所属用户',
  `title` VARCHAR(120) NOT NULL COMMENT '标题',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`conversation_id`),
  KEY `idx_kb_conversation` (`owner_id`, `assistant_id`, `update_time`),
  CONSTRAINT `fk_kb_conversation_assistant` FOREIGN KEY (`assistant_id`) REFERENCES `nexora_one_kb_assistant` (`assistant_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助手会话';

CREATE TABLE IF NOT EXISTS `nexora_one_kb_message` (
  `message_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息主键',
  `conversation_id` BIGINT NOT NULL COMMENT '会话',
  `role` VARCHAR(20) NOT NULL COMMENT 'user assistant',
  `content` MEDIUMTEXT NOT NULL COMMENT '消息正文',
  `citations_json` MEDIUMTEXT DEFAULT NULL COMMENT '真实引用 JSON',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_kb_message_conversation` (`conversation_id`, `message_id`),
  CONSTRAINT `fk_kb_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `nexora_one_kb_conversation` (`conversation_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息';

-- 菜单与 AI 平台管理平级；900-908 为本次新增菜单 ID，旧菜单不变。
INSERT INTO `t_menu`
(`menu_id`,`menu_name`,`menu_type`,`parent_id`,`sort`,`path`,`component`,`perms_type`,`api_perms`,`web_perms`,`icon`,`frame_flag`,`cache_flag`,`visible_flag`,`disabled_flag`,`deleted_flag`,`create_user_id`,`update_user_id`,`create_time`,`update_time`)
VALUES
(900,'知识库',1,0,45,'/knowledge',NULL,1,NULL,NULL,'BookOutlined',0,0,1,0,0,1,1,NOW(),NOW()),
(901,'我的文档',2,900,10,'/knowledge/documents','/business/knowledge/my-documents.vue',1,NULL,'knowledge:document:query','FileTextOutlined',0,0,1,0,0,1,1,NOW(),NOW()),
(902,'知识库管理',2,900,20,'/knowledge/bases','/business/knowledge/bases.vue',1,NULL,'knowledge:base:query','ReadOutlined',0,0,1,0,0,1,1,NOW(),NOW()),
(903,'智能助手',2,900,30,'/knowledge/assistants','/business/knowledge/assistants.vue',1,NULL,'knowledge:assistant:query','RobotOutlined',0,0,1,0,0,1,1,NOW(),NOW()),
(904,'管理我的文档',3,901,10,NULL,NULL,2,'knowledge:document:write','knowledge:document:write',NULL,0,0,0,0,0,1,1,NOW(),NOW()),
(905,'管理知识库',3,902,10,NULL,NULL,2,'knowledge:base:write','knowledge:base:write',NULL,0,0,0,0,0,1,1,NOW(),NOW()),
(906,'管理智能助手',3,903,10,NULL,NULL,2,'knowledge:assistant:write','knowledge:assistant:write',NULL,0,0,0,0,0,1,1,NOW(),NOW()),
(907,'助手问答',3,903,20,NULL,NULL,2,'knowledge:assistant:chat','knowledge:assistant:chat',NULL,0,0,0,0,0,1,1,NOW(),NOW()) AS incoming
ON DUPLICATE KEY UPDATE `menu_id`=incoming.`menu_id`;

-- 现有角色均可使用自己的文档与知识库；数据隔离由服务端 owner_id 保证。
INSERT INTO `t_role_menu` (`role_id`,`menu_id`,`create_time`,`update_time`)
SELECT r.`role_id`,m.`menu_id`,NOW(),NOW() FROM `t_role` r
JOIN `t_menu` m ON m.`menu_id` BETWEEN 900 AND 907
WHERE NOT EXISTS (SELECT 1 FROM `t_role_menu` x WHERE x.`role_id`=r.`role_id` AND x.`menu_id`=m.`menu_id`);
