package com.nexoraone.admin.module.business.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/** 知识库实体，绑定解析方案、向量模型及向量存储。 */
@Data
@TableName("nexora_one_ai_knowledge_base")
public class AiKnowledgeBaseEntity {
    /** 知识库主键。 */
    @TableId(type = IdType.AUTO)
    private Long knowledgeBaseId;
    /** 名称。 */
    private String baseName;
    /** 描述。 */
    private String description;
    /** 解析方案主键。 */
    private Long parsePlanId;
    /** 向量模型主键。 */
    private Long embeddingModelId;
    /** Qdrant 实例主键。 */
    private Long vectorDatabaseId;
    /** Qdrant 集合名称。 */
    private String collectionName;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
