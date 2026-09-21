package com.partner.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP Server 配置。
 *
 * <p>生产凭证必须通过环境变量或密钥中心注入，不能提交到代码仓库。</p>
 */
@ConfigurationProperties(prefix = "nexora.mcp")
public class McpServerProperties {

    /** 标准 Streamable HTTP endpoint。 */
    private String endpoint = "/mcp";
    /** MCP Server 对外名称。 */
    private String serverName = "partner-business-mcp";
    /** MCP Server 对外版本。 */
    private String serverVersion = "1.0.0";
    /** endpoint 的鉴权方式。 */
    private AuthType authType = AuthType.BEARER;
    /** API Key 模式使用的请求头名称。 */
    private String authHeaderName = "X-MCP-API-Key";
    /** Bearer Token 或 API Key 的真实值。 */
    private String authSecret;
    /** 单次 MCP 请求允许的最大字节数。 */
    private int maxRequestSize = 2 * 1024 * 1024;

    /** 校验配置，避免带着空凭证启动到生产环境。 */
    public void validate() {
        if (endpoint == null || !endpoint.startsWith("/")
                || endpoint.length() > 200 || endpoint.contains("?")
                || endpoint.contains("#") || endpoint.endsWith("/")) {
            throw new IllegalStateException(
                    "nexora.mcp.endpoint 必须是以 / 开头且不以 / 结尾的路径");
        }
        if (serverName == null || serverName.isBlank()) {
            throw new IllegalStateException("nexora.mcp.server-name 不能为空");
        }
        if (serverVersion == null || serverVersion.isBlank()) {
            throw new IllegalStateException("nexora.mcp.server-version 不能为空");
        }
        if (maxRequestSize < 1024 || maxRequestSize > 16 * 1024 * 1024) {
            throw new IllegalStateException(
                    "nexora.mcp.max-request-size 必须在 1KB 到 16MB 之间");
        }
        if (authType != AuthType.NONE
                && (authSecret == null || authSecret.isBlank())) {
            throw new IllegalStateException(
                    "启用 MCP 鉴权时必须配置 nexora.mcp.auth-secret");
        }
        if (authType == AuthType.API_KEY
                && (authHeaderName == null || authHeaderName.isBlank())) {
            throw new IllegalStateException(
                    "API Key 鉴权必须配置 nexora.mcp.auth-header-name");
        }
    }

    /** MCP endpoint 支持的鉴权方式。 */
    public enum AuthType {
        NONE,
        BEARER,
        API_KEY
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public String getAuthHeaderName() {
        return authHeaderName;
    }

    public void setAuthHeaderName(String authHeaderName) {
        this.authHeaderName = authHeaderName;
    }

    public String getAuthSecret() {
        return authSecret;
    }

    public void setAuthSecret(String authSecret) {
        this.authSecret = authSecret;
    }

    public int getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(int maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }
}
