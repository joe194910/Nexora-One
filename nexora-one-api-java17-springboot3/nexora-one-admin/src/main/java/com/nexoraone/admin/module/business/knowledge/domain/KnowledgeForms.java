package com.nexoraone.admin.module.business.knowledge.domain;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/** 用户知识库的输入表单；身份信息一律从登录态取得。 */
public final class KnowledgeForms {
    private KnowledgeForms() {}

    /** 知识库创建、编辑和多文档绑定。 */
    @Data public static class BaseSave {
        /** 编辑时的主键。 */
        private Long baseId;
        /** 名称。 */
        @NotBlank @Size(max = 100) private String baseName;
        /** 描述。 */
        @Size(max = 500) private String description;
        /** 是否启用。 */
        private Boolean enabledFlag;
        /** 已就绪文档主键集合。 */
        @NotNull private List<Long> documentIds;
    }

    /** 助手配置及多知识库绑定。 */
    @Data public static class AssistantSave {
        /** 编辑时的主键。 */
        private Long assistantId;
        /** 名称。 */
        @NotBlank @Size(max = 100) private String assistantName;
        /** 系统提示词。 */
        @Size(max = 8000) private String systemPrompt;
        /** 对话模型。 */
        @NotNull private Long modelId;
        /** 检索 TopK。 */
        @NotNull @Min(1) @Max(20) private Integer topK;
        /** 相似度阈值。 */
        @NotNull @DecimalMin("0") @DecimalMax("1") private BigDecimal scoreThreshold;
        /** 展示真实引用。 */
        private Boolean showCitations;
        /** 启用。 */
        private Boolean enabledFlag;
        /** 关联的知识库。 */
        @NotNull private List<Long> baseIds;
    }

    /** 用户助手问答。 */
    @Data public static class Chat {
        /** 已存在的会话或空值。 */
        private Long conversationId;
        /** 用户问题。 */
        @NotBlank @Size(max = 4000) private String question;
    }
}
