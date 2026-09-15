package com.nexoraone.admin.module.business.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 调用审计日志实体。
 */
@Data
@TableName("nexora_one_ai_call_log")
public class AiCallLogEntity {

    /** 调用日志主键。 */
    @TableId(type = IdType.AUTO)
    private Long callLogId;
    /** 调用链路标识。 */
    private String traceId;
    /** 调用用户主键。 */
    private Long userId;
    /** 调用用户名称。 */
    private String userName;
    /** 用户 IP。 */
    private String userIp;
    /** 模型服务主键。 */
    private Long serviceId;
    /** 服务商名称快照。 */
    private String providerName;
    /** 模型主键。 */
    private Long modelId;
    /** 模型编码快照。 */
    private String modelCode;
    /** 调用类型。 */
    private String callType;
    /** 调用来源类型。 */
    private String sourceType;
    /** 调用来源名称。 */
    private String sourceName;
    /** 助手主键，预留给智能助手模块。 */
    private Long assistantId;
    /** 知识库主键，预留给知识库模块。 */
    private Long knowledgeBaseId;
    /** 输入 Token 数。 */
    private Integer inputTokens;
    /** 输出 Token 数。 */
    private Integer outputTokens;
    /** Token 总数。 */
    private Integer totalTokens;
    /** 排队耗时毫秒。 */
    private Long queueDurationMs;
    /** 连接耗时毫秒。 */
    private Long connectDurationMs;
    /** 模型生成耗时毫秒。 */
    private Long modelDurationMs;
    /** 总耗时毫秒。 */
    private Long totalDurationMs;
    /** 是否调用成功。 */
    private Boolean successFlag;
    /** 错误码。 */
    private String errorCode;
    /** 错误信息。 */
    private String errorMessage;
    /** 请求摘要。 */
    private String requestSummary;
    /** 响应摘要。 */
    private String responseSummary;
    /** 预估费用。 */
    private BigDecimal estimatedCost;
    /** 创建时间。 */
    private LocalDateTime createTime;
}
