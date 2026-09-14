-- 应用中心增加当前编辑版本与线上发布版本隔离

ALTER TABLE `nexora_one_application`
    ADD COLUMN `current_version_id` BIGINT DEFAULT NULL
        COMMENT '当前正在编辑或审核的应用版本主键' AFTER `publish_config`,
    ADD COLUMN `published_version_id` BIGINT DEFAULT NULL
        COMMENT '当前对外提供服务的已发布版本主键' AFTER `current_version_id`;

-- 将已有已发布版本回填为应用的当前版本和线上版本
UPDATE `nexora_one_application` a
JOIN (
    SELECT v.application_id, MAX(v.version_id) AS version_id
    FROM `nexora_one_application_version` v
    WHERE v.version_status IN (2, 4)
    GROUP BY v.application_id
) latest ON latest.application_id = a.application_id
SET a.current_version_id = latest.version_id,
    a.published_version_id = latest.version_id
WHERE a.current_version_id IS NULL
   OR a.published_version_id IS NULL;
