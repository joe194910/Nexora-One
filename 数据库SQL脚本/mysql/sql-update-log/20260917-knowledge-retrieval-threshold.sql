-- 本地 embedding 模型的余弦相似度通常低于通用云模型，0.65 会导致有效文档零召回。
ALTER TABLE `nexora_one_kb_assistant`
  MODIFY COLUMN `score_threshold` DECIMAL(5,4) NOT NULL DEFAULT 0.3000 COMMENT '相似度阈值';

-- 只迁移旧版默认值，保留用户主动设置的其他阈值。
UPDATE `nexora_one_kb_assistant`
SET `score_threshold` = 0.3000
WHERE `score_threshold` = 0.6500;
