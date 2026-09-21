package com.nexoraone.admin.module.business.mcp.domain;

import com.nexoraone.base.common.domain.PageParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** AI 工具管理、开放注册和调用表单。 */
public final class AiToolForms {
    private AiToolForms() {}

    /** 工具管理列表分页查询条件。 */
    @Data
    public static class Query extends PageParam {
        /** 工具名称或编码关键词。 */
        @Size(max = 100)
        private String searchWord;
        /** 工具来源。 */
        private String sourceType;
        /** 风险等级。 */
        private String riskLevel;
        /** 审核状态。 */
        private String auditStatus;
        /** 启停状态。 */
        private String enabledStatus;
        /** 所属应用主键。 */
        private Long applicationId;
    }

    /** 标准 MCP Server 管理列表分页查询条件。 */
    @Data
    public static class McpServerQuery extends PageParam {
        /** Server 名称或编码关键词。 */
        @Size(max = 100)
        private String searchWord;
        /** 在线状态。 */
        @Pattern(regexp = "UNKNOWN|ONLINE|OFFLINE|ABNORMAL")
        private String onlineStatus;
        /** 启用状态。 */
        private Boolean enabledFlag;
        /** 所属应用主键。 */
        private Long applicationId;
    }

    /** 标准 MCP Server 新增或更新表单。 */
    @Data
    public static class McpServerSave {
        /** Server 主键；新增时为空。 */
        private Long serverId;
        /** Server 所属应用主键。 */
        @NotNull
        private Long applicationId;
        /** 平台内唯一且稳定的 Server 编码。 */
        @NotBlank
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{2,99}$")
        private String serverCode;
        /** Server 展示名称。 */
        @NotBlank
        @Size(max = 100)
        private String serverName;
        /** Server 能力与使用范围说明。 */
        @Size(max = 2000)
        private String description;
        /** 标准 MCP Streamable HTTP 服务地址。 */
        @NotBlank
        @Size(max = 1000)
        private String endpointUrl;
        /** 鉴权方式。 */
        @NotBlank
        @Pattern(regexp = "NONE|BEARER|API_KEY_HEADER")
        private String authType;
        /** API Key 模式使用的请求头名称。 */
        @Size(max = 100)
        @Pattern(regexp = "^[!#$%&'*+.^_`|~0-9A-Za-z-]+$")
        private String authHeaderName;
        /** Bearer Token 或 API Key；更新时留空表示保留原值。 */
        @Size(max = 4000)
        private String authSecret;
        /** 初始化、发现和调用超时时间，单位为秒。 */
        @Min(1)
        @Max(60)
        private Integer timeoutSeconds;
        /** 是否允许平台探活和调用。 */
        private Boolean enabledFlag;
    }

    /** 标准 MCP Server 启停表单。 */
    @Data
    public static class McpServerStatus {
        /** Server 主键。 */
        @NotNull
        private Long serverId;
        /** 目标启用状态。 */
        @NotNull
        private Boolean enabledFlag;
    }

    /** 将已发布平台 API 转换为 AI 工具的表单。 */
    @Data
    public static class PlatformPublish {
        /** 来源平台 API 主键。 */
        @NotNull
        private Long openApiId;
        /** 工具展示名称。 */
        @NotBlank
        @Size(max = 100)
        private String toolName;
        /** 模型调用使用的唯一工具编码。 */
        @NotBlank
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{2,99}$")
        private String toolCode;
        /** 面向模型的工具能力说明。 */
        @NotBlank
        @Size(max = 2000)
        private String description;
        /** 工具类型：查询或操作。 */
        @NotBlank
        @Pattern(regexp = "QUERY|ACTION")
        private String toolType;
        /** 风险等级。 */
        @NotBlank
        @Pattern(regexp = "LOW|MEDIUM|HIGH")
        private String riskLevel;
        /** 调用确认策略。 */
        @NotBlank
        @Pattern(regexp = "AUTO|REQUIRED")
        private String confirmationPolicy;
        /** 是否发布后立即提交审核。 */
        private Boolean submitReview;
    }

    /** 第三方 HTTP 工具登记或更新表单。 */
    @Data
    public static class ExternalSave {
        /** 模型调用使用的唯一工具编码。 */
        @NotBlank
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{2,99}$")
        private String toolCode;
        /** 工具展示名称。 */
        @NotBlank
        @Size(max = 100)
        private String toolName;
        /** 面向模型的工具能力说明。 */
        @NotBlank
        @Size(max = 2000)
        private String description;
        /** 工具类型：查询或操作。 */
        @NotBlank
        @Pattern(regexp = "QUERY|ACTION")
        private String toolType;
        /** 第三方服务回调地址。 */
        @NotBlank
        @Size(max = 1000)
        private String callbackUrl;
        /** 回调使用的 HTTP 方法。 */
        @NotBlank
        @Pattern(regexp = "GET|POST|PUT|DELETE|PATCH")
        private String httpMethod;
        /** 请求内容类型。 */
        @Size(max = 100)
        private String contentType;
        /** 单次调用超时时间，单位为秒。 */
        @Min(1)
        @Max(60)
        private Integer timeoutSeconds;
        /** 风险等级。 */
        @NotBlank
        @Pattern(regexp = "LOW|MEDIUM|HIGH")
        private String riskLevel;
        /** 是否强制在执行前由用户确认。 */
        private Boolean requireConfirmation;
        /** 输入 JSON Schema。 */
        @NotBlank
        private String inputSchema;
        /** 输出 JSON Schema。 */
        private String outputSchema;
    }

    /** 第三方 HTTP 工具批量登记表单。 */
    @Data
    public static class ExternalBatch {
        /** 待登记的工具列表。 */
        @NotEmpty
        @Size(max = 100)
        @Valid
        private List<ExternalSave> tools;
    }

    /** 管理端手工登记第三方 HTTP 工具表单。 */
    @Data
    public static class AdminExternalSave extends ExternalSave {
        /** 工具所属应用主键。 */
        @NotNull
        private Long applicationId;
    }

    /** 工具审核表单。 */
    @Data
    public static class Review {
        /** 工具主键。 */
        @NotNull
        private Long toolId;
        /** 审核结果。 */
        @NotBlank
        @Pattern(regexp = "APPROVED|REJECTED")
        private String auditStatus;
        /** 审核确定的最终工具类型。 */
        @NotBlank
        @Pattern(regexp = "QUERY|ACTION")
        private String toolType;
        /** 审核确定的最终风险等级。 */
        @NotBlank
        @Pattern(regexp = "LOW|MEDIUM|HIGH")
        private String riskLevel;
        /** 审核确定的最终确认策略。 */
        @NotBlank
        @Pattern(regexp = "AUTO|REQUIRED")
        private String confirmationPolicy;
        /** 审核意见。 */
        @Size(max = 1000)
        private String remark;
    }

    /** 工具在线测试表单。 */
    @Data
    public static class TestCall {
        /** 待测试工具主键。 */
        @NotNull
        private Long toolId;
        /** 符合输入 Schema 的测试参数。 */
        @NotNull
        private Map<String, Object> arguments;
    }

    /** 智能助手绑定工具及安全设置表单。 */
    @Data
    public static class AssistantBind {
        /** 智能助手主键。 */
        @NotNull
        private Long assistantId;
        /** 需要关联的工具主键列表。 */
        @NotNull
        private List<Long> toolIds;
        /** 单轮对话允许执行的最大工具次数。 */
        @Min(1)
        @Max(5)
        private Integer maxToolCalls;
        /** 是否向前端展示工具调试参数和结果。 */
        private Boolean toolDebugFlag;
        /** 是否允许助手使用操作类工具。 */
        private Boolean allowActionToolFlag;
    }

    /** 第三方 HTTP 工具心跳表单。 */
    @Data
    public static class Heartbeat {
        /** 本次心跳覆盖的工具编码；为空时覆盖应用下全部工具。 */
        private List<String> toolCodes;
        /** 上报的在线状态。 */
        private String status;
        /** 心跳附带的状态说明。 */
        @Size(max = 1000)
        private String message;
    }

    /** 用户确认或拒绝工具调用表单。 */
    @Data
    public static class Confirm {
        /** 待处理的工具调用请求编号。 */
        @NotBlank
        private String requestId;
        /** 是否同意执行。 */
        @NotNull
        private Boolean approved;
    }

    /** 工具启停状态更新表单。 */
    @Data
    public static class StatusUpdate {
        /** 工具主键。 */
        @NotNull
        private Long toolId;
        /** 目标启停状态。 */
        @NotBlank
        @Pattern(regexp = "ENABLED|DISABLED")
        private String enabledStatus;
    }
}
