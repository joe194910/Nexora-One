-- NexoraOne应用上架流程增加预发布状态。
ALTER TABLE `nexora_one_application`
    MODIFY COLUMN `listing_status` TINYINT NOT NULL DEFAULT 0
        COMMENT '上架状态：0草稿，1审核中，2已上架，3已驳回，4已下架，5已预发布';

-- API权限申请支持保留撤销记录，避免删除已审核的授权历史。
ALTER TABLE `nexora_one_application_api_permission`
    MODIFY COLUMN `apply_status` TINYINT NOT NULL DEFAULT 1
        COMMENT '申请状态：1待审核，2已授权，3已驳回，4已撤销';
