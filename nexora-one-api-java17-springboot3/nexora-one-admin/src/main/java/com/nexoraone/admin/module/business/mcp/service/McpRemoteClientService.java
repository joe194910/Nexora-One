package com.nexoraone.admin.module.business.mcp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexoraone.admin.module.business.mcp.domain.McpServer;
import com.nexoraone.base.module.support.apiencrypt.service.ApiEncryptService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 使用官方 Java SDK 连接远端 Streamable HTTP MCP Server。 */
@Service
public class McpRemoteClientService {

    private static final int MAX_TOOL_PAGES = 20;
    private static final int MAX_DISCOVERED_TOOLS = 1000;
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;

    @Resource
    private ApiEncryptService apiEncryptService;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 初始化远端 Server 并分页发现全部工具。
     *
     * @param server 已保存的 MCP Server 配置
     * @return 协商信息和远端工具定义
     */
    public ProbeResult probe(McpServer server) {
        Endpoint endpoint = validateEndpoint(server.getEndpointUrl());
        McpSyncClient client = createClient(server, endpoint);
        try {
            InitializeResult initialization = client.initialize();
            List<Tool> tools = listToolsBounded(client);
            Implementation serverInfo = initialization.serverInfo();
            return new ProbeResult(
                    initialization.protocolVersion(),
                    serverInfo == null ? null : serverInfo.name(),
                    serverInfo == null ? null : serverInfo.version(),
                    tools);
        } finally {
            close(client);
        }
    }

    /**
     * 调用一个已经发现并审核启用的远端 MCP 工具。
     *
     * @param server 已保存的 MCP Server 配置
     * @param remoteToolName 远端原始工具名称
     * @param arguments 符合 inputSchema 的调用参数
     * @return 可供输出 Schema 校验和模型消费的结构化结果
     */
    public Object callTool(McpServer server, String remoteToolName,
                           Map<String, Object> arguments) {
        Endpoint endpoint = validateEndpoint(server.getEndpointUrl());
        McpSyncClient client = createClient(server, endpoint);
        try {
            client.initialize();
            CallToolResult result = client.callTool(
                    new CallToolRequest(remoteToolName, arguments));
            if (Boolean.TRUE.equals(result.isError())) {
                throw new IllegalStateException("远端 MCP 工具返回错误：" + contentText(result.content()));
            }
            if (result.structuredContent() != null) {
                return result.structuredContent();
            }
            return contentValue(result.content());
        } finally {
            close(client);
        }
    }

    /**
     * 校验 MCP endpoint，允许企业内网地址但拒绝本机、链路本地、组播和云元数据地址。
     *
     * @param endpointUrl 用户填写的标准 MCP endpoint
     * @return 拆分后的基础地址与 endpoint 路径
     */
    public Endpoint validateEndpoint(String endpointUrl) {
        try {
            URI uri = URI.create(StringUtils.trim(endpointUrl));
            if (!Set.of("http", "https").contains(StringUtils.lowerCase(uri.getScheme()))
                    || StringUtils.isBlank(uri.getHost()) || uri.getUserInfo() != null
                    || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("MCP endpoint 必须是有效的 HTTP(S) URL");
            }
            if (uri.getPort() < -1 || uri.getPort() == 0 || uri.getPort() > 65535) {
                throw new IllegalArgumentException("MCP endpoint 端口无效");
            }
            String host = StringUtils.lowerCase(uri.getHost());
            if ("localhost".equals(host) || host.endsWith(".localhost")
                    || "host.docker.internal".equals(host)
                    || "169.254.169.254".equals(host)) {
                throw new IllegalArgumentException("MCP endpoint 不能指向本机、容器宿主或云元数据地址");
            }
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                        || address.isLinkLocalAddress() || address.isMulticastAddress()) {
                    throw new IllegalArgumentException("MCP endpoint 不能解析到本机、链路本地或组播地址");
                }
            }
            String baseUrl = uri.getScheme() + "://" + uri.getRawAuthority();
            String path = StringUtils.defaultIfBlank(uri.getRawPath(), "/");
            if (StringUtils.isNotBlank(uri.getRawQuery())) {
                path += "?" + uri.getRawQuery();
            }
            return new Endpoint(baseUrl, path);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("MCP endpoint 无法解析");
        }
    }

    /** 根据超时、endpoint 和静态鉴权请求头创建同步 MCP 客户端。 */
    private McpSyncClient createClient(McpServer server, Endpoint endpoint) {
        int timeoutSeconds = Math.max(1,
                Math.min(Objects.requireNonNullElse(server.getTimeoutSeconds(), 30), 60));
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        applyAuthentication(server, requestBuilder);
        HttpClientStreamableHttpTransport transport =
                HttpClientStreamableHttpTransport.builder(endpoint.baseUrl())
                        .endpoint(endpoint.endpointPath())
                        .requestBuilder(requestBuilder)
                        .clientBuilder(HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                                .followRedirects(HttpClient.Redirect.NEVER))
                        .maxResponseSize(MAX_RESPONSE_BYTES)
                        .build();
        return McpClient.sync(transport)
                .clientInfo(new Implementation("nexora-one", "1.0.0"))
                .initializationTimeout(Duration.ofSeconds(timeoutSeconds))
                .requestTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /** 将加密保存的 Token 或 API Key 注入远端 MCP 请求头。 */
    private void applyAuthentication(McpServer server, HttpRequest.Builder requestBuilder) {
        if ("NONE".equals(server.getAuthType())) {
            return;
        }
        String secret = apiEncryptService.decrypt(server.getAuthSecretCipher());
        if (StringUtils.isBlank(secret)) {
            throw new IllegalStateException("MCP Server 鉴权凭证不存在或无法解密");
        }
        if ("BEARER".equals(server.getAuthType())) {
            requestBuilder.header("Authorization", "Bearer " + secret);
            return;
        }
        if ("API_KEY_HEADER".equals(server.getAuthType())) {
            if (StringUtils.isBlank(server.getAuthHeaderName())) {
                throw new IllegalStateException("MCP Server 未配置 API Key 请求头名称");
            }
            requestBuilder.header(server.getAuthHeaderName(), secret);
            return;
        }
        throw new IllegalStateException("不支持的 MCP Server 鉴权方式");
    }

    /** 分页发现工具，并限制页数和总量以防止异常 Server 无限返回游标。 */
    private List<Tool> listToolsBounded(McpSyncClient client) {
        List<Tool> tools = new ArrayList<>();
        String cursor = null;
        String previousCursor = null;
        for (int page = 0; page < MAX_TOOL_PAGES; page++) {
            ListToolsResult result = client.listTools(cursor);
            if (result.tools() != null) {
                tools.addAll(result.tools());
            }
            if (tools.size() > MAX_DISCOVERED_TOOLS) {
                throw new IllegalStateException("远端 MCP Server 工具数量超过 1000 个限制");
            }
            String nextCursor = result.nextCursor();
            if (StringUtils.isBlank(nextCursor)) {
                return tools;
            }
            if (Objects.equals(nextCursor, cursor) || Objects.equals(nextCursor, previousCursor)) {
                throw new IllegalStateException("远端 MCP Server 返回了重复分页游标");
            }
            previousCursor = cursor;
            cursor = nextCursor;
        }
        throw new IllegalStateException("远端 MCP Server 工具分页超过 20 页限制");
    }

    /** 将 MCP content 列表转换为结构化数据或便于模型读取的文本结果。 */
    private Object contentValue(List<Content> content) {
        if (content == null || content.isEmpty()) {
            return Map.of();
        }
        if (content.size() == 1 && content.get(0) instanceof TextContent textContent) {
            return parseTextContent(textContent.text());
        }
        List<Object> values = new ArrayList<>();
        for (Content item : content) {
            if (item instanceof TextContent textContent) {
                values.add(parseTextContent(textContent.text()));
            } else {
                values.add(objectMapper.convertValue(item, LinkedHashMap.class));
            }
        }
        return Map.of("content", values);
    }

    /** 优先把文本 content 解析为 JSON，普通文本保留为 content 字段。 */
    private Object parseTextContent(String text) {
        try {
            return objectMapper.readValue(text, Object.class);
        } catch (Exception exception) {
            return Map.of("content", StringUtils.defaultString(text));
        }
    }

    /** 汇总远端错误内容，避免把 Java record 文本直接暴露给管理员。 */
    private String contentText(List<Content> content) {
        if (content == null || content.isEmpty()) {
            return "未提供错误详情";
        }
        return content.stream()
                .map(item -> item instanceof TextContent textContent
                        ? textContent.text() : objectMapper.convertValue(item, Map.class).toString())
                .filter(StringUtils::isNotBlank)
                .reduce((left, right) -> left + "; " + right)
                .orElse("未提供错误详情");
    }

    /** 尽力关闭 MCP 会话，关闭失败不覆盖真实业务异常。 */
    private void close(McpSyncClient client) {
        if (client == null) {
            return;
        }
        try {
            client.closeGracefully();
        } catch (Exception ignored) {
            // 会话关闭失败不会改变本次探活或工具调用结果。
        }
    }

    /** 经过安全校验并拆分后的 MCP endpoint。 */
    public record Endpoint(String baseUrl, String endpointPath) {
    }

    /** MCP Server 初始化和工具发现结果。 */
    public record ProbeResult(String protocolVersion, String serverName,
                              String serverVersion, List<Tool> tools) {
    }
}
