package com.nexoraone.admin.module.business.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 用户智能助手，检索范围限定在已授权知识库文档。 */
@Data
@TableName("nexora_one_kb_assistant")
public class KnowledgeAssistant {
    /** 助手主键。 */
    @TableId(type = IdType.AUTO) private Long assistantId;
    /** 所属用户。 */
    private Long ownerId;
    /** 助手名称。 */
    private String assistantName;
    /** 系统提示词。 */
    private String systemPrompt;
    /** 已启用对话模型主键。 */
    private Long modelId;
    /** 单次检索结果数量。 */
    private Integer topK;
    /** Cosine 相似度阈值。 */
    private BigDecimal scoreThreshold;
    /** 是否展示真实引用来源。 */
    private Boolean showCitations;
    /** 是否启用。 */
    private Boolean enabledFlag;
    /** 是否已上架到智能助手商店。 */
    private Boolean publishedFlag;
    /** 最近一次上架时间。 */
    private LocalDateTime publishedTime;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
