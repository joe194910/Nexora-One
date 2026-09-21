package com.partner.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 面向老项目的 MCP tools 兼容入口。
 *
 * <p>支持 initialize、ping、notifications/*、tools/list 和 tools/call。
 * 新增工具只需实现 {@link McpTool} 并标注 {@code @Component}。</p>
 */
@RestController
public class McpController {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(McpController.class);
    private static final String JSON_RPC_VERSION = "2.0";
    private static final List<String> SUPPORTED_PROTOCOL_VERSIONS =
            Arrays.asList("2025-06-18");
    private static final String BEARER_PREFIX = "Bearer ";

    private final Map<String, McpTool> toolMap;
    private final ObjectMapper objectMapper;
    private final McpProperties properties;

    /** 自动发现并校验全部 MCP 工具实现。 */
    public McpController(
            List<McpTool> tools,
            ObjectMapper objectMapper,
            McpProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        Map<String, McpTool> discovered =
                new LinkedHashMap<String, McpTool>();
        for (McpTool tool : tools) {
            validateTool(tool);
            McpTool duplicated =
                    discovered.putIfAbsent(tool.getName(), tool);
            if (duplicated != null) {
                throw new IllegalStateException(
                        "MCP 工具名重复：" + tool.getName());
            }
        }
        this.toolMap = Collections.unmodifiableMap(discovered);
        LOGGER.info("MCP 工具注册完成，共 {} 个：{}",
                toolMap.size(), toolMap.keySet());
    }

    /** 统一处理 MCP JSON-RPC 请求。 */
    @PostMapping("${nexora.mcp.endpoint:/mcp}")
    public ResponseEntity<Map<String, Object>> handle(
            @RequestBody(required = false) Map<String, Object> request,
            HttpServletRequest servletRequest) {
        if (!isAuthorized(servletRequest)) {
            LOGGER.warn("MCP 鉴权失败，来源 IP：{}",
                    servletRequest.getRemoteAddr());
            return ResponseEntity.status(401)
                    .body(error(null, -32001, "MCP 鉴权失败"));
        }
        Object id = request == null ? null : request.get("id");
        if (request == null
                || !JSON_RPC_VERSION.equals(request.get("jsonrpc"))) {
            return ResponseEntity.ok(
                    error(id, -32600, "jsonrpc 必须为 2.0"));
        }
        Object methodValue = request.get("method");
        if (!(methodValue instanceof String)
                || ((String) methodValue).trim().isEmpty()) {
            return ResponseEntity.ok(
                    error(id, -32600, "JSON-RPC 请求缺少 method"));
        }

        String method = (String) methodValue;
        try {
            if (method.startsWith("notifications/")) {
                return ResponseEntity.accepted().build();
            }
            if ("initialize".equals(method)) {
                return ResponseEntity.ok(
                        success(id, initializeResult(request)));
            }
            if ("ping".equals(method)) {
                return ResponseEntity.ok(
                        success(id, Collections.emptyMap()));
            }
            if ("tools/list".equals(method)) {
                return ResponseEntity.ok(success(id,
                        Collections.<String, Object>singletonMap(
                                "tools", toolDefinitions())));
            }
            if ("tools/call".equals(method)) {
                return ResponseEntity.ok(callTool(id, request));
            }
            return ResponseEntity.ok(
                    error(id, -32601, "不支持的 MCP 方法：" + method));
        } catch (Exception exception) {
            LOGGER.error("处理 MCP 方法异常：" + method, exception);
            return ResponseEntity.ok(
                    error(id, -32603, "MCP 服务处理异常"));
        }
    }

    /** 请求体不是合法 JSON 时返回标准 JSON-RPC 解析错误。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(
            HttpMessageNotReadableException exception) {
        LOGGER.warn("MCP 请求体解析失败", exception);
        return ResponseEntity.ok(
                error(null, -32700, "JSON-RPC 请求体解析失败"));
    }

    /** 协商受支持的协议版本并返回工具能力。 */
    private Map<String, Object> initializeResult(
            Map<String, Object> request) {
        Map<String, Object> params = asMap(request.get("params"));
        String requestedVersion = params == null
                ? null : asString(params.get("protocolVersion"));
        String selectedVersion =
                SUPPORTED_PROTOCOL_VERSIONS.contains(requestedVersion)
                        ? requestedVersion
                        : SUPPORTED_PROTOCOL_VERSIONS.get(0);

        Map<String, Object> serverInfo =
                new LinkedHashMap<String, Object>();
        serverInfo.put("name", properties.getServerName());
        serverInfo.put("version", properties.getServerVersion());

        Map<String, Object> result =
                new LinkedHashMap<String, Object>();
        result.put("protocolVersion", selectedVersion);
        result.put("capabilities",
                Collections.singletonMap(
                        "tools", Collections.emptyMap()));
        result.put("serverInfo", serverInfo);
        return result;
    }

    /** 返回全部已注册工具的标准定义。 */
    private List<Map<String, Object>> toolDefinitions() {
        List<Map<String, Object>> definitions =
                new ArrayList<Map<String, Object>>(toolMap.size());
        for (McpTool tool : toolMap.values()) {
            definitions.add(tool.getDefinition());
        }
        return definitions;
    }

    /** 校验参数后执行指定工具。 */
    private Map<String, Object> callTool(
            Object id, Map<String, Object> request) {
        Map<String, Object> params = asMap(request.get("params"));
        if (params == null) {
            return error(id, -32602, "tools/call 缺少 params");
        }
        String name = asString(params.get("name"));
        if (name == null || name.trim().isEmpty()) {
            return error(id, -32602, "tools/call 缺少工具名称");
        }
        McpTool tool = toolMap.get(name);
        if (tool == null) {
            return error(id, -32602, "未知的 MCP 工具：" + name);
        }
        Map<String, Object> arguments = asMap(params.get("arguments"));
        if (arguments == null) {
            arguments = Collections.emptyMap();
        }
        try {
            validateArguments(tool.getInputSchema(), arguments);
        } catch (IllegalArgumentException exception) {
            return success(id, toolContent(
                    "工具参数不合法：" + exception.getMessage(), true));
        }
        try {
            String text = toText(tool.execute(arguments));
            return success(id, toolContent(text, false));
        } catch (Exception exception) {
            LOGGER.error("MCP 工具执行失败：" + tool.getName(), exception);
            return success(id, toolContent(
                    "工具执行失败，请联系服务提供方", true));
        }
    }

    /** 校验工具定义中的名称、说明和输入 Schema。 */
    private void validateTool(McpTool tool) {
        if (tool == null || tool.getName() == null
                || !tool.getName().matches("[A-Za-z][A-Za-z0-9_-]{1,127}")) {
            throw new IllegalStateException("MCP 工具名称不合法");
        }
        if (tool.getDescription() == null
                || tool.getDescription().trim().isEmpty()) {
            throw new IllegalStateException(
                    "MCP 工具说明不能为空：" + tool.getName());
        }
        Map<String, Object> schema = tool.getInputSchema();
        if (schema == null || !"object".equals(schema.get("type"))) {
            throw new IllegalStateException(
                    "MCP inputSchema.type 必须为 object："
                            + tool.getName());
        }
    }

    /** 校验必填字段和常用 JSON Schema 基础类型。 */
    private void validateArguments(
            Map<String, Object> schema,
            Map<String, Object> arguments) {
        Object requiredValue = schema.get("required");
        if (requiredValue instanceof List) {
            for (Object required : (List<?>) requiredValue) {
                String field = String.valueOf(required);
                if (!arguments.containsKey(field)
                        || arguments.get(field) == null) {
                    throw new IllegalArgumentException(
                            "缺少必填字段：" + field);
                }
            }
        }
        Map<String, Object> propertiesMap =
                asMap(schema.get("properties"));
        if (propertiesMap == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            Map<String, Object> fieldSchema =
                    asMap(propertiesMap.get(entry.getKey()));
            if (fieldSchema == null || entry.getValue() == null) {
                continue;
            }
            validateValueType(
                    entry.getKey(),
                    asString(fieldSchema.get("type")),
                    entry.getValue());
        }
    }

    /** 校验字符串、数字、整数、布尔、对象和数组类型。 */
    private void validateValueType(
            String field, String type, Object value) {
        boolean valid = type == null
                || ("string".equals(type) && value instanceof String)
                || ("integer".equals(type) && value instanceof Number
                && Math.floor(((Number) value).doubleValue())
                == ((Number) value).doubleValue())
                || ("number".equals(type) && value instanceof Number)
                || ("boolean".equals(type) && value instanceof Boolean)
                || ("object".equals(type) && value instanceof Map)
                || ("array".equals(type) && value instanceof List);
        if (!valid) {
            throw new IllegalArgumentException(
                    "字段 " + field + " 类型应为 " + type);
        }
    }

    /** 按配置校验无需鉴权、Bearer Token 或 API Key。 */
    private boolean isAuthorized(HttpServletRequest request) {
        if (properties.getAuthType()
                == McpProperties.AuthType.NONE) {
            return true;
        }
        String actual;
        if (properties.getAuthType()
                == McpProperties.AuthType.BEARER) {
            String authorization =
                    request.getHeader("Authorization");
            if (authorization == null
                    || !authorization.regionMatches(
                    true, 0, BEARER_PREFIX, 0,
                    BEARER_PREFIX.length())) {
                return false;
            }
            actual = authorization
                    .substring(BEARER_PREFIX.length()).trim();
        } else {
            actual = request.getHeader(
                    properties.getAuthHeaderName());
        }
        return constantTimeEquals(
                actual, properties.getAuthSecret());
    }

    /** 使用常量时间比较，降低凭证比较时序差异。 */
    private boolean constantTimeEquals(
            String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    /** 构造 JSON-RPC 成功响应。 */
    private Map<String, Object> success(Object id, Object result) {
        Map<String, Object> response =
                new LinkedHashMap<String, Object>();
        response.put("jsonrpc", JSON_RPC_VERSION);
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    /** 构造 JSON-RPC 错误响应。 */
    private Map<String, Object> error(
            Object id, int code, String message) {
        Map<String, Object> error =
                new LinkedHashMap<String, Object>();
        error.put("code", code);
        error.put("message", message);
        Map<String, Object> response =
                new LinkedHashMap<String, Object>();
        response.put("jsonrpc", JSON_RPC_VERSION);
        response.put("id", id);
        response.put("error", error);
        return response;
    }

    /** 构造 tools/call 结果。 */
    private Map<String, Object> toolContent(
            String text, boolean isError) {
        Map<String, Object> content =
                new LinkedHashMap<String, Object>();
        content.put("type", "text");
        content.put("text", text);
        Map<String, Object> result =
                new LinkedHashMap<String, Object>();
        result.put("content", Collections.singletonList(content));
        result.put("isError", isError);
        return result;
    }

    /** 将业务结果序列化并限制最大返回长度。 */
    private String toText(Object result) {
        try {
            String text = result instanceof CharSequence
                    ? result.toString()
                    : objectMapper.writeValueAsString(
                            result == null
                                    ? Collections.emptyMap() : result);
            if (text.length() > properties.getMaxResultLength()) {
                throw new IllegalStateException(
                        "工具返回内容超过平台限制");
            }
            return text;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "工具结果无法序列化", exception);
        }
    }

    /** 将动态对象转换为 Map。 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map
                ? (Map<String, Object>) value : null;
    }

    /** 将动态对象转换为字符串。 */
    private String asString(Object value) {
        return value instanceof String ? (String) value : null;
    }
}
