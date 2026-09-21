package com.partner.mcp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * MCP endpoint 独立鉴权过滤器。
 *
 * <p>该过滤器不复用业务系统登录态，因此 NexoraOne 可以使用静态 Bearer Token
 * 或 API Key 直接调用 MCP Server。</p>
 */
public class McpAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final McpServerProperties properties;

    public McpAuthenticationFilter(McpServerProperties properties) {
        this.properties = properties;
    }

    /** 对 MCP endpoint 执行独立鉴权，通过后继续交给官方 SDK Servlet。 */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (properties.getAuthType() == McpServerProperties.AuthType.NONE) {
            filterChain.doFilter(request, response);
            return;
        }

        String actualSecret = readSecret(request);
        if (!constantTimeEquals(actualSecret, properties.getAuthSecret())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"jsonrpc\":\"2.0\",\"id\":null,"
                            + "\"error\":{\"code\":-32001,"
                            + "\"message\":\"MCP 鉴权失败\"}}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** 根据配置的鉴权方式读取请求中的真实凭证。 */
    private String readSecret(HttpServletRequest request) {
        if (properties.getAuthType() == McpServerProperties.AuthType.BEARER) {
            String authorization = request.getHeader("Authorization");
            if (authorization == null
                    || !authorization.regionMatches(
                    true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
                return null;
            }
            return authorization.substring(BEARER_PREFIX.length()).trim();
        }
        return request.getHeader(properties.getAuthHeaderName());
    }

    /** 使用常量时间比较，降低凭证比较时序差异。 */
    private boolean constantTimeEquals(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
