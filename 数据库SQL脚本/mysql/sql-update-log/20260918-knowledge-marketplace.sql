-- 智能助手商店：助手上架、收藏和公共问答。
-- 本脚本兼容曾经执行过“知识库商店”旧版本的数据库。

SET @assistant_published_flag_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'nexora_one_kb_assistant'
    AND COLUMN_NAME = 'published_flag'
);
SET @sql = IF(@assistant_published_flag_exists = 0,
  'ALTER TABLE `nexora_one_kb_assistant` ADD COLUMN `published_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否上架'' AFTER `enabled_flag`',
  'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @assistant_published_time_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'nexora_one_kb_assistant'
    AND COLUMN_NAME = 'published_time'
);
SET @sql = IF(@assistant_published_time_exists = 0,
  'ALTER TABLE `nexora_one_kb_assistant` ADD COLUMN `published_time` DATETIME DEFAULT NULL COMMENT ''上架时间'' AFTER `published_flag`',
  'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

CREATE TABLE IF NOT EXISTS `nexora_one_kb_assistant_favorite` (
  `assistant_id` BIGINT NOT NULL COMMENT '智能助手',
  `owner_id` BIGINT NOT NULL COMMENT '收藏用户',
  `create_time` DATETIME NOT NULL COMMENT '收藏时间',
  PRIMARY KEY (`assistant_id`, `owner_id`),
  KEY `idx_kb_assistant_favorite_owner` (`owner_id`, `create_time`),
  CONSTRAINT `fk_kb_assistant_favorite_assistant`
    FOREIGN KEY (`assistant_id`) REFERENCES `nexora_one_kb_assistant` (`assistant_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能助手收藏';

-- 旧版中已经上架的知识库，其关联助手继续保持上架。
SET @base_published_flag_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'nexora_one_kb_base'
    AND COLUMN_NAME = 'published_flag'
);
SET @base_published_time_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'nexora_one_kb_base'
    AND COLUMN_NAME = 'published_time'
);
SET @sql = IF(@base_published_flag_exists > 0 AND @base_published_time_exists > 0,
  'UPDATE `nexora_one_kb_assistant` a
     JOIN `nexora_one_kb_assistant_base` ab ON ab.`assistant_id` = a.`assistant_id`
     JOIN `nexora_one_kb_base` b ON b.`base_id` = ab.`base_id`
   SET a.`published_flag` = 1,
       a.`published_time` = COALESCE(a.`published_time`, b.`published_time`, NOW())
   WHERE b.`published_flag` = 1 AND a.`enabled_flag` = 1',
  IF(@base_published_flag_exists > 0,
    'UPDATE `nexora_one_kb_assistant` a
       JOIN `nexora_one_kb_assistant_base` ab ON ab.`assistant_id` = a.`assistant_id`
       JOIN `nexora_one_kb_base` b ON b.`base_id` = ab.`base_id`
     SET a.`published_flag` = 1,
         a.`published_time` = COALESCE(a.`published_time`, NOW())
     WHERE b.`published_flag` = 1 AND a.`enabled_flag` = 1',
    'SELECT 1'));
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

-- 将旧知识库收藏迁移为其关联助手收藏。
SET @base_favorite_exists = (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'nexora_one_kb_base_favorite'
);
SET @sql = IF(@base_favorite_exists > 0,
  'INSERT IGNORE INTO `nexora_one_kb_assistant_favorite`
     (`assistant_id`, `owner_id`, `create_time`)
   SELECT ab.`assistant_id`, bf.`owner_id`, bf.`create_time`
   FROM `nexora_one_kb_base_favorite` bf
   JOIN `nexora_one_kb_assistant_base` ab ON ab.`base_id` = bf.`base_id`
   JOIN `nexora_one_kb_assistant` a ON a.`assistant_id` = ab.`assistant_id`
   WHERE a.`owner_id` <> bf.`owner_id`',
  'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @sql = IF(@base_favorite_exists > 0,
  'DROP TABLE `nexora_one_kb_base_favorite`',
  'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @sql = IF(@base_published_time_exists > 0,
  'ALTER TABLE `nexora_one_kb_base` DROP COLUMN `published_time`',
  'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @sql = IF(@base_published_flag_exists > 0,
  'ALTER TABLE `nexora_one_kb_base` DROP COLUMN `published_flag`',
  'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

INSERT INTO `t_menu`
(`menu_id`,`menu_name`,`menu_type`,`parent_id`,`sort`,`path`,`component`,`perms_type`,`api_perms`,`web_perms`,`icon`,`frame_flag`,`cache_flag`,`visible_flag`,`disabled_flag`,`deleted_flag`,`create_user_id`,`update_user_id`,`create_time`,`update_time`)
VALUES
(908,'智能助手商店',2,900,25,'/knowledge/assistant-store','/business/knowledge/assistant-store.vue',1,'knowledge:assistant-store:query','knowledge:assistant-store:query','ShopOutlined',0,0,1,0,0,1,1,NOW(),NOW()) AS incoming
ON DUPLICATE KEY UPDATE
`menu_name`=incoming.`menu_name`,
`parent_id`=incoming.`parent_id`,
`sort`=incoming.`sort`,
`path`=incoming.`path`,
`component`=incoming.`component`,
`api_perms`=incoming.`api_perms`,
`web_perms`=incoming.`web_perms`,
`icon`=incoming.`icon`,
`visible_flag`=1,
`disabled_flag`=0,
`deleted_flag`=0,
`update_time`=NOW();

INSERT INTO `t_role_menu` (`role_id`,`menu_id`,`create_time`,`update_time`)
SELECT r.`role_id`,908,NOW(),NOW()
FROM `t_role` r
WHERE NOT EXISTS (
  SELECT 1 FROM `t_role_menu` x WHERE x.`role_id`=r.`role_id` AND x.`menu_id`=908
);
