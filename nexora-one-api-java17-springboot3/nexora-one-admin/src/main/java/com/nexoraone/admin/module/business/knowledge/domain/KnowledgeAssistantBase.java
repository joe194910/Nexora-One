package com.nexoraone.admin.module.business.knowledge.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 助手与用户知识库的多对多绑定。 */
@Data
@TableName("nexora_one_kb_assistant_base")
public class KnowledgeAssistantBase {
    /** 助手主键。 */
    private Long assistantId;
    /** 用户知识库主键。 */
    private Long baseId;
}
