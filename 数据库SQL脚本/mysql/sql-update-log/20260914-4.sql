-- 将应用当前流程状态与线上运行状态分离，避免新版本审核影响旧版本服务
ALTER TABLE `nexora_one_application`
    ADD COLUMN `online_status` TINYINT NOT NULL DEFAULT 0
        COMMENT '线上状态：0未发布，2线上运行，4已下架' AFTER `listing_status`,
    ADD KEY `idx_application_online_status` (`online_status`);

-- 已发布版本优先决定线上状态，兼容升级前的历史应用
UPDATE `nexora_one_application` a
LEFT JOIN `nexora_one_application_version` v
    ON v.version_id = a.published_version_id
SET a.online_status = CASE
    WHEN v.version_status = 2 THEN 2
    WHEN v.version_status = 4 THEN 4
    WHEN a.listing_status = 2 THEN 2
    WHEN a.listing_status = 4 AND a.published_version_id IS NOT NULL THEN 4
    ELSE 0
END;
