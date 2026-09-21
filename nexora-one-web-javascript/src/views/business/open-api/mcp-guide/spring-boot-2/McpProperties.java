package com.partner.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 老项目 MCP 兼容 endpoint 配置。
 */
@Component
@ConfigurationProperties(prefix = "nexora.mcp")
public class McpProperties {

    private String endpoint = "/mcp";
    private String serverName = "partner-business-mcp";
    private String serverVersion = "1.0.0";
    private AuthType authType = AuthType.BEARER;
    private String authHeaderName = "X-MCP-API-Key";
    private String authSecret;
    private int maxResultLength = 1024 * 1024;

    /** 启动时校验关键配置，避免空凭证误上生产。 */
    @PostConstruct
    public void validate() {
        if (endpoint == null || !endpoint.startsWith("/")
                || endpoint.endsWith("/") || endpoint.contains("?")
                || endpoint.contains("#")) {
            throw new IllegalStateException("nexora.mcp.endpoint 配置不合法");
        }
        if (serverName == null || serverName.trim().isEmpty()) {
            throw new IllegalStateException("nexora.mcp.server-name 不能为空");
        }
        if (serverVersion == null || serverVersion.trim().isEmpty()) {
            throw new IllegalStateException("nexora.mcp.server-version 不能为空");
        }
        if (authType != AuthType.NONE
                && (authSecret == null || authSecret.trim().isEmpty())) {
            throw new IllegalStateException(
                    "启用 MCP 鉴权时必须配置 nexora.mcp.auth-secret");
        }
        if (authType == AuthType.API_KEY
                && (authHeaderName == null
                || authHeaderName.trim().isEmpty())) {
            throw new IllegalStateException(
                    "API Key 鉴权必须配置 nexora.mcp.auth-header-name");
        }
        if (maxResultLength < 1024
                || maxResultLength > 4 * 1024 * 1024) {
            throw new IllegalStateException(
                    "nexora.mcp.max-result-length 必须在 1KB 到 4MB 之间");
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

    public int getMaxResultLength() {
        return maxResultLength;
    }

    public void setMaxResultLength(int maxResultLength) {
        this.maxResultLength = maxResultLength;
    }
}
