package com.nexoraone.admin.module.business.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/** 一条对话消息；助手回答及其真实引用分别持久化。 */
@Data
@TableName("nexora_one_kb_message")
public class KnowledgeMessage {
    /** 消息主键。 */
    @TableId(type = IdType.AUTO) private Long messageId;
    /** 会话主键。 */
    private Long conversationId;
    /** user 或 assistant。 */
    private String role;
    /** 消息正文。 */
    private String content;
    /** 引用的 JSON 数组。 */
    private String citationsJson;
    /** 创建时间。 */
    private LocalDateTime createTime;
}
