package com.nexoraone.admin.module.business.knowledge.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户收藏的已上架智能助手。 */
@Data
@TableName("nexora_one_kb_assistant_favorite")
public class KnowledgeAssistantFavorite {
    private Long assistantId;
    private Long ownerId;
    private LocalDateTime createTime;
}
