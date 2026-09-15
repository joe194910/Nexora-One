package com.nexoraone.admin.module.business.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档解析方案实体，沿用原解析配置表以保留已有规则。
 */
@Data
@TableName("nexora_one_ai_document_parse_config")
public class AiDocumentParseConfigEntity {

    /** 配置主键。 */
    @TableId(type = IdType.AUTO)
    private Long configId;
    /** 方案名称。 */
    private String planName;
    /** 方案描述。 */
    private String description;
    /** 是否默认方案。 */
    private Boolean defaultFlag;
    /** 是否启用。 */
    private Boolean enabledFlag;
    /** 文档解析服务主键。 */
    private Long parserServiceId;
    /** OCR 服务主键。 */
    private Long ocrServiceId;
    /** 表格解析服务主键。 */
    private Long tableServiceId;
    /** 图片理解服务主键。 */
    private Long imageServiceId;
    /** 支持的文件扩展名，使用逗号分隔。 */
    private String supportedFormats;
    /** 文档解析器类型。 */
    private String parserType;
    /** PDF 解析模式。 */
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
    private String ocrService;
    /** 切片方式。 */
    private String chunkMethod;
    /** 切片长度。 */
    private Integer chunkSize;
    /** 重叠长度。 */
    private Integer chunkOverlap;
    /** 最小切片长度。 */
    private Integer minChunkSize;
    /** 分隔符。 */
    private String separators;
    /** 是否保留标题元数据。 */
    private Boolean preserveTitleMetadata;
    /** 并发任务数。 */
    private Integer concurrency;
    /** 解析超时分钟数。 */
    private Integer timeoutMinutes;
    /** 失败重试次数。 */
    private Integer retryCount;
    /** 重复文件处理策略。 */
    private String duplicateStrategy;
    /** 异常文档是否继续。 */
    private Boolean continueOnError;
    /** 入库使用的向量模型主键。 */
    private Long embeddingModelId;
    /** 更新人。 */
    private Long updateUserId;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
