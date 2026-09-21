package com.partner.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 标准 MCP Streamable HTTP Server 配置。
 *
 * <p>官方 SDK 负责协议协商、会话、tools/list、tools/call 和入参 Schema 校验；
 * 业务系统只需要实现 {@link McpTool}。</p>
 */
@Configuration
@EnableConfigurationProperties(McpServerProperties.class)
public class McpServerConfiguration {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(McpServerConfiguration.class);

    /** 创建官方 Streamable HTTP Servlet 传输层。 */
    @Bean
    public HttpServletStreamableServerTransportProvider mcpTransportProvider(
            McpServerProperties properties) {
        properties.validate();
        return HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(McpJsonDefaults.getMapper())
                .mcpEndpoint(properties.getEndpoint())
                .maxRequestSize(properties.getMaxRequestSize())
                .keepAliveInterval(Duration.ofSeconds(30))
                .build();
    }

    /** 将 MCP 传输 Servlet 只映射到配置的 endpoint。 */
    @Bean
    public ServletRegistrationBean<HttpServletStreamableServerTransportProvider>
    mcpServletRegistration(
            HttpServletStreamableServerTransportProvider transportProvider,
            McpServerProperties properties) {
        ServletRegistrationBean<HttpServletStreamableServerTransportProvider>
                registration = new ServletRegistrationBean<>(
                transportProvider, properties.getEndpoint(),
                properties.getEndpoint() + "/*");
        registration.setName("nexoraMcpServlet");
        registration.setAsyncSupported(true);
        registration.setLoadOnStartup(1);
        return registration;
    }

    /** 注册只作用于 MCP endpoint 的独立静态凭证鉴权过滤器。 */
    @Bean
    public FilterRegistrationBean<McpAuthenticationFilter>
    mcpAuthenticationFilter(McpServerProperties properties) {
        FilterRegistrationBean<McpAuthenticationFilter> registration =
                new FilterRegistrationBean<>();
        registration.setFilter(new McpAuthenticationFilter(properties));
        registration.setName("nexoraMcpAuthenticationFilter");
        registration.addUrlPatterns(
                properties.getEndpoint(),
                properties.getEndpoint() + "/*");
        registration.setOrder(Integer.MIN_VALUE + 20);
        return registration;
    }

    /** 创建 MCP Server，并把全部 McpTool Spring Bean 注册为标准工具。 */
    @Bean(destroyMethod = "close")
    public McpSyncServer mcpServer(
            HttpServletStreamableServerTransportProvider transportProvider,
            McpServerProperties properties,
            List<McpTool> tools,
            ObjectMapper objectMapper) {
        validateUniqueToolNames(tools);
        List<SyncToolSpecification> specifications = tools.stream()
                .map(tool -> toSpecification(tool, objectMapper))
                .toList();
        return McpServer.sync(transportProvider)
                .serverInfo(
                        properties.getServerName(),
                        properties.getServerVersion())
                .capabilities(ServerCapabilities.builder().tools(true).build())
                .strictToolNameValidation(true)
                .validateToolInputs(true)
                .requestTimeout(Duration.ofSeconds(60))
                .tools(specifications)
                .build();
    }

    /** 将业务工具转换成官方 SDK 的工具定义与调用处理器。 */
    private SyncToolSpecification toSpecification(
            McpTool tool, ObjectMapper objectMapper) {
        Tool.Builder definition = Tool.builder(tool.getName())
                .description(tool.getDescription())
                .inputSchema(tool.getInputSchema());
        if (tool.getOutputSchema() != null
                && !tool.getOutputSchema().isEmpty()) {
            definition.outputSchema(tool.getOutputSchema());
        }
        return SyncToolSpecification.builder()
                .tool(definition.build())
                .callHandler((exchange, request) -> {
                    try {
                        Map<String, Object> arguments =
                                request.arguments() == null
                                        ? Map.of() : request.arguments();
                        Object result = tool.execute(arguments);
                        return CallToolResult.builder()
                                .structuredContent(result)
                                .addTextContent(toJson(objectMapper, result))
                                .isError(false)
                                .build();
                    } catch (IllegalArgumentException exception) {
                        return CallToolResult.builder()
                                .addTextContent(exception.getMessage())
                                .isError(true)
                                .build();
                    } catch (Exception exception) {
                        LOGGER.error(
                                "MCP 工具执行失败，工具名：{}",
                                tool.getName(), exception);
                        return CallToolResult.builder()
                                .addTextContent("工具执行失败，请联系服务提供方")
                                .isError(true)
                                .build();
                    }
                })
                .build();
    }

    /** 校验工具名全局唯一，避免覆盖已有业务能力。 */
    private void validateUniqueToolNames(List<McpTool> tools) {
        Set<String> names = new HashSet<>();
        for (McpTool tool : tools) {
            if (!names.add(tool.getName())) {
                throw new IllegalStateException(
                        "MCP 工具名重复：" + tool.getName());
            }
        }
    }

    /** 将业务对象序列化为兼容所有 MCP 客户端的文本内容。 */
    private String toJson(ObjectMapper objectMapper, Object value) {
        if (value == null) {
            return "{}";
        }
        if (value instanceof CharSequence text) {
            return text.toString();
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("MCP 工具结果无法序列化", exception);
        }
    }
}
