package com.nexoraone.admin.module.business.mcp.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 工具执行、用户确认、结果与错误的完整审计记录。 */
@Data
@TableName("nexora_one_ai_tool_call_log")
public class AiToolCallLog {
    /** 调用日志主键。 */
    @TableId(type = IdType.AUTO)
    private Long callId;
    /** 幂等请求编号。 */
    private String requestId;
    /** 全链路追踪编号。 */
    private String traceId;
    /** AI 工具主键。 */
    private Long toolId;
    /** 调用时的工具编码快照。 */
    private String toolCode;
    /** 调用时的工具来源快照。 */
    private String sourceType;
    /** 第三方工具所属应用主键。 */
    private Long applicationId;
    /** 发起调用的智能助手主键。 */
    private Long assistantId;
    /** 发起调用的会话主键。 */
    private Long conversationId;
    /** 发起调用的用户主键。 */
    private Long userId;
    /** 调用状态。 */
    private String status;
    /** 是否需要用户确认。 */
    private Boolean confirmationRequired;
    /** 用户确认或拒绝时间。 */
    private LocalDateTime confirmationTime;
    /** 调用参数 JSON。 */
    private String argumentsJson;
    /** 调用结果 JSON。 */
    private String resultJson;
    /** 等待确认时暂存的执行上下文 JSON。 */
    private String pendingContextJson;
    /** 目标服务返回的 HTTP 状态码。 */
    private Integer httpStatus;
    /** 调用耗时，单位为毫秒。 */
    private Long durationMs;
    /** 失败错误码。 */
    private String errorCode;
    /** 失败错误信息。 */
    private String errorMessage;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
