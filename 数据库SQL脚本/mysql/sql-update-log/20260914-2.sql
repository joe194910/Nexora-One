-- API版本管理区分当前编辑版本和当前线上发布版本。
ALTER TABLE `nexora_one_open_api`
    ADD COLUMN `published_version_id` BIGINT DEFAULT NULL
        COMMENT '当前线上发布版本主键' AFTER `current_version_id`;

-- 兼容已上架、已停用和已下线API的历史数据。
UPDATE `nexora_one_open_api`
SET `published_version_id` = `current_version_id`
WHERE `published_version_id` IS NULL
  AND `current_version_id` IS NOT NULL
  AND `status` IN (4, 5, 6);
