package com.nexoraone.admin.module.business.ai.domain.form;

import com.nexoraone.base.common.domain.PageParam;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import java.util.List;

/** 文档解析与知识库接口表单集合。 */
public final class AiDocumentForm {
    private AiDocumentForm() {
    }

    /** 解析方案新增或编辑表单。 */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PlanSave extends AiPlatformForm.DocumentConfigSave {
        /** 方案主键；新增时为空。 */
        private Long configId;
        /** 名称。 */
        @NotBlank @Length(max = 100)
        private String planName;
        /** 描述。 */
        @Length(max = 200)
        private String description;
        /** 是否默认。 */
        private Boolean defaultFlag;
        /** 是否启用。 */
        private Boolean enabledFlag;
        /** 文档解析服务主键。 */
        @NotNull
        private Long parserServiceId;
        /** OCR 服务主键。 */
        private Long ocrServiceId;
        /** 表格解析服务主键。 */
        private Long tableServiceId;
        /** 图片理解服务主键。 */
        private Long imageServiceId;
    }

    /** 解析服务配置表单。 */
    @Data
    public static class ServiceSave {
        /** 服务主键。 */
        private Long parseServiceId;
        /** 名称。 */
        @NotBlank @Length(max = 100)
        private String serviceName;
        /** 服务类型。 */
        @NotBlank
        private String serviceType;
        /** 部署方式。 */
        @NotBlank
        private String deploymentType;
        /** 内置实现代码。 */
        private String implementation;
        /** HTTP API 地址。 */
        @Length(max = 500)
        private String endpoint;
        /** HTTP API Key，掩码表示保持原值。 */
        @Length(max = 1000)
        private String apiKey;
        /** 支持的格式。 */
        @Size(min = 1, max = 20)
        private List<String> supportedFormats;
        /** 超时秒数。 */
        @NotNull @Min(1) @Max(600)
        private Integer timeoutSeconds;
        /** 是否默认。 */
        private Boolean defaultFlag;
        /** 是否启用。 */
        private Boolean enabledFlag;
    }

    /** 知识库创建与编辑表单。 */
    @Data
    public static class KnowledgeSave {
        /** 主键。 */
        private Long knowledgeBaseId;
        /** 名称。 */
        @NotBlank @Length(max = 100)
        private String baseName;
        /** 描述。 */
        @Length(max = 500)
        private String description;
        /** 所属解析方案。 */
        @NotNull
        private Long parsePlanId;
        /** 向量模型。 */
        @NotNull
        private Long embeddingModelId;
        /** 向量存储。 */
        @NotNull
        private Long vectorDatabaseId;
    }

    /** 解析任务分页筛选表单。 */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TaskQuery extends PageParam {
        /** 任务号或文件名。 */
        @Length(max = 100)
        private String keyword;
        /** 知识库。 */
        private Long knowledgeBaseId;
        /** 方案。 */
        private Long parsePlanId;
        /** 状态。 */
        private String status;
        /** 阶段。 */
        private String currentStage;
    }
}
