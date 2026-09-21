package com.nexoraone.admin.module.business.mcp.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 远端标准 MCP Server 的注册、鉴权、探活和工具发现配置。 */
@Data
@TableName("nexora_one_mcp_server")
public class McpServer {
    /** MCP Server 主键。 */
    @TableId(type = IdType.AUTO)
    private Long serverId;
    /** Server 所属应用主键。 */
    private Long applicationId;
    /** 平台内唯一且稳定的 Server 编码。 */
    private String serverCode;
    /** Server 展示名称。 */
    private String serverName;
    /** Server 能力与使用范围说明。 */
    private String description;
    /** 标准 MCP Streamable HTTP 服务地址。 */
    private String endpointUrl;
    /** 传输协议，第一版固定为 STREAMABLE_HTTP。 */
    private String transportType;
    /** 鉴权方式：NONE、BEARER 或 API_KEY_HEADER。 */
    private String authType;
    /** API Key 模式使用的请求头名称。 */
    private String authHeaderName;
    /** 加密保存的 Bearer Token 或 API Key。 */
    private String authSecretCipher;
    /** 初始化、发现和调用的超时时间，单位为秒。 */
    private Integer timeoutSeconds;
    /** 是否允许平台探活和调用该 Server。 */
    private Boolean enabledFlag;
    /** 在线状态：UNKNOWN、ONLINE、OFFLINE、ABNORMAL。 */
    private String onlineStatus;
    /** 最近一次协商成功的 MCP 协议版本。 */
    private String protocolVersion;
    /** 远端 Server 上报的实现版本。 */
    private String serverVersion;
    /** 最近一次成功发现的工具数量。 */
    private Integer discoveredToolCount;
    /** 最近一次探活时间。 */
    private LocalDateTime lastProbeTime;
    /** 最近一次探活结果说明。 */
    private String lastProbeMessage;
    /** 创建人主键。 */
    private Long createUserId;
    /** 最后更新人主键。 */
    private Long updateUserId;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
