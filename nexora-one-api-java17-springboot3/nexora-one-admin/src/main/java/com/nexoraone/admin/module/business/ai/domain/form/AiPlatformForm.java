package com.nexoraone.admin.module.business.ai.domain.form;

import com.nexoraone.base.common.domain.PageParam;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 平台请求表单集合。
 */
public final class AiPlatformForm {

    private AiPlatformForm() {
    }

    /**
     * 通用分页查询表单。
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PageQuery extends PageParam {
        /** 名称或编码关键字。 */
        @Length(max = 100, message = "搜索关键字最多 100 个字符")
        private String keyword;
        /** 类型筛选。 */
        @Length(max = 30, message = "类型最多 30 个字符")
        private String type;
        /** 状态筛选。 */
        private Boolean enabledFlag;
        /** 所属服务主键。 */
        private Long serviceId;
    }

    /**
     * 模型服务保存表单。
     */
    @Data
    public static class ModelServiceSave {
        /** 模型服务主键，新增时为空。 */
        private Long serviceId;
        /** 服务名称。 */
        @NotBlank(message = "服务名称不能为空")
        @Length(max = 100, message = "服务名称最多 100 个字符")
        private String serviceName;
        /** 服务商类型。 */
        @NotBlank(message = "服务商类型不能为空")
        private String providerType;
        /** 接口协议。 */
        @NotBlank(message = "接口协议不能为空")
        private String protocolType;
        /** 服务基础地址。 */
        @NotBlank(message = "Base URL 不能为空")
        @Length(max = 500, message = "Base URL 最多 500 个字符")
        private String baseUrl;
        /** API Key，编辑时传掩码表示保持不变。 */
        @Length(max = 1000, message = "API Key 长度超出限制")
        private String apiKey;
        /** 组织 ID。 */
        @Length(max = 100, message = "组织 ID 最多 100 个字符")
        private String organizationId;
        /** 请求超时秒数。 */
        @NotNull(message = "请求超时不能为空")
        @Min(value = 1, message = "请求超时不能小于 1 秒")
        @Max(value = 600, message = "请求超时不能大于 600 秒")
        private Integer requestTimeoutSeconds;
        /** 支持能力。 */
        @Size(max = 10, message = "支持能力最多选择 10 项")
        private List<String> capabilities;
        /** 是否为默认服务。 */
        private Boolean defaultFlag;
        /** 是否启用。 */
        private Boolean enabledFlag;
    }

    /**
     * 模型保存表单。
     */
    @Data
    public static class ModelSave {
        /** 模型主键，新增时为空。 */
        private Long modelId;
        /** 模型名称。 */
        @NotBlank(message = "模型名称不能为空")
        @Length(max = 100, message = "模型名称最多 100 个字符")
        private String modelName;
        /** 模型编码。 */
        @NotBlank(message = "模型编码不能为空")
        @Length(max = 150, message = "模型编码最多 150 个字符")
        private String modelCode;
        /** 模型类型。 */
        @NotBlank(message = "模型类型不能为空")
        private String modelType;
        /** 所属服务主键。 */
        @NotNull(message = "所属模型服务不能为空")
        private Long serviceId;
        /** 支持能力。 */
        private List<String> capabilities;
        /** 上下文长度。 */
        @Min(value = 1, message = "上下文长度必须大于 0")
        private Integer contextLength;
        /** 向量维度。 */
        @Min(value = 1, message = "向量维度必须大于 0")
        private Integer embeddingDimension;
        /** 默认温度。 */
        @DecimalMin(value = "0", message = "温度不能小于 0")
        @DecimalMax(value = "2", message = "温度不能大于 2")
        private BigDecimal temperature;
        /** 最大输出 Token。 */
        @Min(value = 1, message = "最大输出 Token 必须大于 0")
        private Integer maxOutputTokens;
        /** 每百万输入 Token 价格。 */
        @DecimalMin(value = "0", message = "输入价格不能小于 0")
        private BigDecimal inputPrice;
        /** 每百万输出 Token 价格。 */
        @DecimalMin(value = "0", message = "输出价格不能小于 0")
        private BigDecimal outputPrice;
        /** 是否为默认模型。 */
        private Boolean defaultFlag;
        /** 是否启用。 */
        private Boolean enabledFlag;
    }

    /**
     * 向量数据库保存表单。
     */
    @Data
    public static class VectorDatabaseSave {
        /** 向量数据库主键，新增时为空。 */
        private Long vectorDatabaseId;
        /** 实例名称。 */
        @NotBlank(message = "实例名称不能为空")
        private String instanceName;
        /** 数据库类型。 */
        @NotBlank(message = "数据库类型不能为空")
        private String databaseType;
        /** 服务地址。 */
        @NotBlank(message = "服务地址不能为空")
        private String serviceUrl;
        /** API Key。 */
        private String apiKey;
        /** 集合前缀。 */
        @NotBlank(message = "集合前缀不能为空")
        private String collectionPrefix;
        /** 请求超时秒数。 */
        @NotNull(message = "请求超时不能为空")
        @Min(value = 1, message = "请求超时不能小于 1 秒")
        @Max(value = 600, message = "请求超时不能大于 600 秒")
        private Integer requestTimeoutSeconds;
        /** 是否使用 TLS。 */
        private Boolean tlsEnabled;
        /** 是否为默认实例。 */
        private Boolean defaultFlag;
        /** 是否启用。 */
        private Boolean enabledFlag;
    }

    /**
     * 文档解析配置保存表单。
     */
    @Data
    public static class DocumentConfigSave {
        /** 支持的文件格式。 */
        @NotNull(message = "支持的文件格式不能为空")
        @Size(min = 1, max = 20, message = "至少选择一种文件格式")
        private List<String> supportedFormats;
        /** 解析器类型。 */
        @NotBlank(message = "解析器类型不能为空")
        private String parserType;
        /** PDF 解析模式。 */
        @NotBlank(message = "PDF 解析模式不能为空")
        private String pdfMode;
        /** 是否提取表格。 */
        private Boolean extractTable;
        /** 是否保留标题层级。 */
        private Boolean preserveHeading;
        /** 是否提取图片。 */
        private Boolean extractImage;
        /** 是否启用 OCR。 */
        private Boolean ocrEnabled;
        /** OCR HTTP 接口地址。 */
        @Length(max = 500, message = "OCR 接口地址最多 500 个字符")
        private String ocrService;
        /** 切片方式。 */
        @NotBlank(message = "切片方式不能为空")
        private String chunkMethod;
        /** 切片长度。 */
        @NotNull(message = "切片长度不能为空")
        @Min(value = 100, message = "切片长度不能小于 100")
        @Max(value = 10000, message = "切片长度不能大于 10000")
        private Integer chunkSize;
        /** 重叠长度。 */
        @NotNull(message = "重叠长度不能为空")
        @Min(value = 0, message = "重叠长度不能小于 0")
        private Integer chunkOverlap;
        /** 最小切片长度。 */
        @NotNull(message = "最小切片长度不能为空")
        @Min(value = 1, message = "最小切片长度必须大于 0")
        private Integer minChunkSize;
        /** 分隔符。 */
        private String separators;
        /** 是否保留标题元数据。 */
        private Boolean preserveTitleMetadata;
        /** 并发任务数。 */
        @NotNull(message = "并发任务数不能为空")
        @Min(value = 1, message = "并发任务数不能小于 1")
        @Max(value = 20, message = "并发任务数不能大于 20")
        private Integer concurrency;
        /** 解析超时分钟数。 */
        @NotNull(message = "解析超时不能为空")
        @Min(value = 1, message = "解析超时不能小于 1 分钟")
        private Integer timeoutMinutes;
        /** 失败重试次数。 */
        @NotNull(message = "失败重试次数不能为空")
        @Min(value = 0, message = "失败重试次数不能小于 0")
        @Max(value = 10, message = "失败重试次数不能大于 10")
        private Integer retryCount;
        /** 重复文件策略。 */
        @NotBlank(message = "重复文件策略不能为空")
        private String duplicateStrategy;
        /** 异常文档是否继续。 */
        private Boolean continueOnError;
        /** 向量模型主键。 */
        private Long embeddingModelId;
    }

    /**
     * 模型调试表单。
     */
    @Data
    public static class ModelDebug {
        /** 模型主键。 */
        @NotNull(message = "模型主键不能为空")
        private Long modelId;
        /** 调试输入内容。 */
        @NotBlank(message = "调试内容不能为空")
        @Length(max = 20000, message = "调试内容最多 20000 个字符")
        private String prompt;
        /** 系统提示词。 */
        @Length(max = 5000, message = "系统提示词最多 5000 个字符")
        private String systemPrompt;
        /** 调用来源。 */
        private String sourceName;
    }

    /**
     * 调用日志分页查询表单。
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CallLogQuery extends PageParam {
        /** 查询开始时间。 */
        private LocalDateTime beginTime;
        /** 查询结束时间。 */
        private LocalDateTime endTime;
        /** 用户名称。 */
        private String userName;
        /** 模型主键。 */
        private Long modelId;
        /** 调用类型。 */
        private String callType;
        /** 来源类型。 */
        private String sourceType;
        /** 调用是否成功。 */
        private Boolean successFlag;
        /** Trace ID。 */
        private String traceId;
    }

    /**
     * 用量统计查询表单。
     */
    @Data
    public static class StatisticsQuery {
        /** 统计开始时间。 */
        @NotNull(message = "统计开始时间不能为空")
        private LocalDateTime beginTime;
        /** 统计结束时间。 */
        @NotNull(message = "统计结束时间不能为空")
        private LocalDateTime endTime;
        /** 服务主键。 */
        private Long serviceId;
        /** 模型主键。 */
        private Long modelId;
        /** 调用类型。 */
        private String callType;
    }
}
