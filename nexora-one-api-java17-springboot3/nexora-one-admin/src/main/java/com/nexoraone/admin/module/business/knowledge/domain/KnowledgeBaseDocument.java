package com.nexoraone.admin.module.business.knowledge.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 知识库与文档的多对多关系，同一文档无需重复处理。 */
@Data
@TableName("nexora_one_kb_base_document")
public class KnowledgeBaseDocument {
    /** 业务知识库主键。 */
    private Long baseId;
    /** 已向量化文档主键。 */
    private Long documentId;
}
