package com.nexoraone.admin.module.business.knowledge.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        /** 单轮最多调用工具数量。 */
        @Min(1) @Max(5) private Integer maxToolCalls;
        /** 显示工具调试信息。 */
        private Boolean toolDebugFlag;
        /** 允许操作类工具。 */
        private Boolean allowActionToolFlag;
        /** 启用。 */
        private Boolean enabledFlag;
        /** 关联的知识库。 */
        @NotNull private List<Long> baseIds;
        /** 关联的 AI 工具。 */
        private List<Long> toolIds;
    }

    /** 用户助手问答。 */
    @Data public static class Chat {
        /** 已存在的会话或空值。 */
        private Long conversationId;
        /** 用户问题。 */
        @NotBlank @Size(max = 4000) private String question;
    }

    /** 从智能助手商店创建免登录分享链接。 */
    @Data public static class AssistantShareCreate {
        /** 分享链接失效时间。 */
        @NotNull @Future private LocalDateTime expireTime;
        /** 当前管理端访问源，例如 https://example.com。 */
        @NotBlank @Size(max = 500) private String accessOrigin;
    }

    /** 免登录分享页面发起的助手问答。 */
    @Data public static class PublicChat {
        /** 当前用户问题。 */
        @NotBlank @Size(max = 4000) private String question;
        /** 浏览器本地保存的最近对话上下文。 */
        @Size(max = 12) private List<@Valid PublicMessage> history;
    }

    /** 公开问答携带的单条本地会话消息。 */
    @Data public static class PublicMessage {
        /** 消息角色，只允许用户或助手。 */
        @NotBlank
        @Pattern(regexp = "user|assistant", message = "消息角色仅支持 user 或 assistant")
        private String role;
        /** 消息正文。 */
        @NotBlank @Size(max = 8000) private String content;
    }
}
