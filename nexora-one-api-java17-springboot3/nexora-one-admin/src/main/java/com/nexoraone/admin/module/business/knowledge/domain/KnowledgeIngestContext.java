package com.nexoraone.admin.module.business.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 用户和底层配置的入库上下文；多个业务知识库共享同一批文档向量。 */
@Data
@TableName("nexora_one_kb_ingest_context")
public class KnowledgeIngestContext {
    /** 上下文主键。 */
    @TableId(type = IdType.AUTO) private Long contextId;
    /** 所属用户。 */
    private Long ownerId;
    /** 解析方案。 */
    private Long parsePlanId;
    /** 向量模型。 */
    private Long embeddingModelId;
    /** Qdrant 实例。 */
    private Long vectorDatabaseId;
    /** 底层任务使用的入库主键。 */
    private Long ingestBaseId;
}
