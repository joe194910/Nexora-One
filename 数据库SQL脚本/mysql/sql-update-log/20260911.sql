-- 将旧版 SmartJob 定时任务表迁移为 NexoraJob 命名。
RENAME TABLE `t_smart_job` TO `t_nexora_job`;
RENAME TABLE `t_smart_job_log` TO `t_nexora_job_log`;

-- 更新数据库中保存的示例任务实现类名称。
UPDATE `t_nexora_job`
SET `job_class` = REPLACE(`job_class`, 'SmartJob', 'NexoraJob')
WHERE `job_class` LIKE '%SmartJob%';
