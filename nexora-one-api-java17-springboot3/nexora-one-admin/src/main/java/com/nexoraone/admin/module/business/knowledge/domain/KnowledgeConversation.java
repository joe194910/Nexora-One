package com.nexoraone.admin.module.business.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/** 用户的助手会话，所有历史读取均检查所有权。 */
@Data
@TableName("nexora_one_kb_conversation")
public class KnowledgeConversation {
    /** 会话主键。 */
    @TableId(type = IdType.AUTO) private Long conversationId;
    /** 所属助手。 */
    private Long assistantId;
    /** 所属用户。 */
    private Long ownerId;
    /** 会话标题。 */
    private String title;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
