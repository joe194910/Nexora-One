package com.nexoraone.admin.module.business.mcp.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 平台 API、标准 MCP Server 与第三方 HTTP 适配工具的统一 AI 工具定义。 */
@Data
@TableName("nexora_one_ai_tool")
public class AiTool {
    /** 工具主键。 */
    @TableId(type = IdType.AUTO)
    private Long toolId;
    /** 提供给模型调用的唯一工具编码。 */
    private String toolCode;
    /** 工具展示名称。 */
    private String toolName;
    /** 面向模型的工具能力说明。 */
    private String description;
    /** 工具来源：平台 API、标准 MCP Server 或第三方 HTTP。 */
    private String sourceType;
    /** 工具类型：查询工具或操作工具。 */
    private String toolType;
    /** 平台 API 主键，第三方 HTTP 工具为空。 */
    private Long openApiId;
    /** 当前 Schema 关联的平台 API 版本主键。 */
    private Long sourceApiVersionId;
    /** 标准 MCP 或第三方 HTTP 工具所属应用主键。 */
    private Long applicationId;
    /** 标准 MCP Server 主键，非标准 MCP 工具为空。 */
    private Long mcpServerId;
    /** 远端 MCP Server 声明的原始工具名称。 */
    private String remoteToolName;
    /** 第三方 HTTP 工具回调地址。 */
    private String callbackUrl;
    /** 工具调用使用的 HTTP 方法。 */
    private String httpMethod;
    /** 请求内容类型。 */
    private String contentType;
    /** 单次调用超时时间，单位为秒。 */
    private Integer timeoutSeconds;
    /** 风险等级：低、中、高。 */
    private String riskLevel;
    /** 调用确认策略：自动执行或执行前确认。 */
    private String confirmationPolicy;
    /** 模型调用工具时使用的输入 JSON Schema。 */
    private String inputSchema;
    /** 校验工具响应时使用的输出 JSON Schema。 */
    private String outputSchema;
    /** 审核状态。 */
    private String auditStatus;
    /** 审核意见。 */
    private String auditRemark;
    /** 工具启停状态。 */
    private String enabledStatus;
    /** 工具来源服务的在线状态。 */
    private String onlineStatus;
    /** 最近一次测试状态。 */
    private String lastTestStatus;
    /** 最近一次测试说明。 */
    private String lastTestMessage;
    /** 远端工具 Schema 是否发生变化并等待管理员显式同步。 */
    private Boolean schemaSyncRequired;
    /** 最近一次测试时间。 */
    private LocalDateTime lastTestTime;
    /** 第三方工具最近一次心跳时间。 */
    private LocalDateTime lastHeartbeatTime;
    /** 最近一次成功调用时间。 */
    private LocalDateTime lastCallTime;
    /** 累计成功调用次数。 */
    private Long totalCallCount;
    /** 创建人主键。 */
    private Long createUserId;
    /** 最后更新人主键。 */
    private Long updateUserId;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
