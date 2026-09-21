package com.nexoraone.admin.module.business.mcp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexoraone.admin.module.business.application.dao.ApplicationDao;
import com.nexoraone.admin.module.business.application.dao.OpenApiDao;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationCredentialEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationEntity;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import com.nexoraone.admin.module.business.application.manager.ApplicationCredentialManager;
import com.nexoraone.admin.module.business.application.service.ApplicationDataScopeService;
import com.nexoraone.admin.module.business.mcp.dao.AiToolMappers;
import com.nexoraone.admin.module.business.mcp.domain.AiTool;
import com.nexoraone.admin.module.business.mcp.domain.AiToolCallLog;
import com.nexoraone.admin.module.business.mcp.domain.McpServer;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiEnvironmentDao;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiParameterDao;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiVersionDao;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiEnvironmentEntity;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiParameterEntity;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiVersionEntity;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** AI 工具的实际 HTTP 调用、确认、签名、SSRF 防护和审计日志。 */
@Service
public class AiToolInvocationService {

    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    private static final int MAX_CALLS_PER_MINUTE = 30;

    @Resource
    private AiToolMappers.ToolDao toolDao;
    @Resource
    private AiToolMappers.CallLogDao callLogDao;
    @Resource
    private OpenApiDao openApiDao;
    @Resource
    private OpenApiVersionDao versionDao;
    @Resource
    private OpenApiEnvironmentDao environmentDao;
    @Resource
    private OpenApiParameterDao parameterDao;
    @Resource
    private ApplicationDao applicationDao;
    @Resource
    private ApplicationCredentialManager credentialManager;
    @Resource
    /** 应用与开放 API 共用的数据范围服务。 */
    private ApplicationDataScopeService applicationDataScopeService;
    @Resource
    private AiToolService toolService;
    @Resource
    private AiToolSchemaService schemaService;
    @Resource
    private McpServerService mcpServerService;
    @Resource
    private McpRemoteClientService mcpRemoteClientService;
    @Resource
    private ObjectMapper objectMapper;

    /** 管理端在线测试，操作类工具在明确点击测试后视为已确认。 */
    public InvocationResult test(Long toolId, Map<String, Object> arguments) {
        AiTool tool = toolService.requireManageable(toolId);
        if (Boolean.TRUE.equals(tool.getSchemaSyncRequired())) {
            throw new IllegalArgumentException("远端 Schema 已变化，请先同步工具定义再测试");
        }
        ExecutionContext context = new ExecutionContext(
                null, applicationDataScopeService.requireEmployee().getEmployeeId(),
                null, null, null);
        try {
            InvocationResult result = invoke(tool, arguments, context, true, null);
            toolService.recordTest(toolId, "SUCCESS".equals(result.status()),
                    result.message());
            return result;
        } catch (RuntimeException exception) {
            toolService.recordTest(toolId, false, exception.getMessage());
            throw exception;
        }
    }

    /**
     * 智能助手运行时调用工具，并在真正执行前重新校验工具的审核、启用和来源状态。
     *
     * @param tool 模型选择工具时读取的工具快照
     * @param arguments 模型生成且符合输入 Schema 的参数
     * @param context 服务端构造的可信用户、助手和会话上下文
     * @return 实际调用结果或等待确认结果
     */
    public InvocationResult invokeForAssistant(
            AiTool tool, Map<String, Object> arguments, ExecutionContext context) {
        if (context.assistantId() == null) {
            throw new IllegalArgumentException("助手工具调用缺少助手上下文");
        }
        AiTool callable = toolService.requireEnabledForAssistant(
                tool.getToolId(), context.assistantId());
        return invoke(callable, arguments, context, false, null);
    }

    /** 助手运行时调用工具；操作类或显式配置的工具先返回确认请求。 */
    private InvocationResult invoke(AiTool tool, Map<String, Object> arguments,
                                    ExecutionContext context, boolean confirmed,
                                    String suppliedRequestId) {
        schemaService.validateArguments(tool.getInputSchema(), arguments);
        enforceRateLimit(tool.getToolId(), context.userId());
        String requestId = StringUtils.defaultIfBlank(suppliedRequestId, newId());
        AiToolCallLog existing = callLogDao.selectOne(new LambdaQueryWrapper<AiToolCallLog>()
                .eq(AiToolCallLog::getRequestId, requestId).last("limit 1"));
        if (existing != null) {
            if (!Objects.equals(existing.getToolId(), tool.getToolId())
                    || !Objects.equals(existing.getUserId(), context.userId())
                    || !Objects.equals(existing.getAssistantId(), context.assistantId())) {
                throw new IllegalArgumentException("请求编号已被其他工具调用占用");
            }
            return resultFromLog(existing);
        }

        AiToolCallLog log = new AiToolCallLog();
        log.setRequestId(requestId);
        log.setTraceId(newId());
        log.setToolId(tool.getToolId());
        log.setToolCode(tool.getToolCode());
        log.setSourceType(tool.getSourceType());
        log.setApplicationId(tool.getApplicationId());
        log.setAssistantId(context.assistantId());
        log.setConversationId(context.conversationId());
        log.setUserId(context.userId());
        log.setArgumentsJson(writeJson(arguments));
        log.setConfirmationRequired(requiresConfirmation(tool));
        log.setCreateTime(LocalDateTime.now());
        log.setUpdateTime(LocalDateTime.now());

        if (requiresConfirmation(tool) && !confirmed) {
            log.setStatus("WAITING_CONFIRMATION");
            log.setPendingContextJson(writeJson(context));
            callLogDao.insert(log);
            return new InvocationResult(requestId, log.getTraceId(), log.getStatus(),
                    null, "该操作需要用户确认后执行", null, null, true);
        }
        log.setStatus("CREATED");
        callLogDao.insert(log);
        return execute(log, tool, arguments, context);
    }

    /** 用户确认或拒绝一笔待执行操作。 */
    public InvocationResult confirm(String requestId, boolean approved) {
        AiToolCallLog log = callLogDao.selectOne(new LambdaQueryWrapper<AiToolCallLog>()
                .eq(AiToolCallLog::getRequestId, requestId).last("limit 1"));
        if (log == null || !"WAITING_CONFIRMATION".equals(log.getStatus())) {
            throw new IllegalArgumentException("待确认的工具调用不存在或已经处理");
        }
        Long currentUserId = applicationDataScopeService.requireEmployee().getEmployeeId();
        if (!Objects.equals(log.getUserId(), currentUserId)
                && !applicationDataScopeService.isPlatformAdministrator()) {
            throw new IllegalArgumentException("无权处理该工具调用");
        }
        if (log.getCreateTime() != null
                && log.getCreateTime().isBefore(LocalDateTime.now().minusMinutes(30))) {
            callLogDao.update(null, new LambdaUpdateWrapper<AiToolCallLog>()
                    .eq(AiToolCallLog::getCallId, log.getCallId())
                    .eq(AiToolCallLog::getStatus, "WAITING_CONFIRMATION")
                    .set(AiToolCallLog::getStatus, "CONFIRMATION_EXPIRED")
                    .set(AiToolCallLog::getErrorMessage, "工具调用确认已超过 30 分钟")
                    .set(AiToolCallLog::getUpdateTime, LocalDateTime.now()));
            throw new IllegalArgumentException("工具调用确认已过期，请重新发起");
        }
        LocalDateTime confirmationTime = LocalDateTime.now();
        if (!approved) {
            int updated = callLogDao.update(null, new LambdaUpdateWrapper<AiToolCallLog>()
                    .eq(AiToolCallLog::getCallId, log.getCallId())
                    .eq(AiToolCallLog::getStatus, "WAITING_CONFIRMATION")
                    .set(AiToolCallLog::getStatus, "USER_REJECTED")
                    .set(AiToolCallLog::getConfirmationTime, confirmationTime)
                    .set(AiToolCallLog::getUpdateTime, confirmationTime));
            if (updated == 0) {
                throw new IllegalArgumentException("该工具调用已经处理");
            }
            log.setStatus("USER_REJECTED");
            log.setConfirmationTime(confirmationTime);
            log.setUpdateTime(confirmationTime);
            return resultFromLog(log);
        }
        int updated = callLogDao.update(null, new LambdaUpdateWrapper<AiToolCallLog>()
                .eq(AiToolCallLog::getCallId, log.getCallId())
                .eq(AiToolCallLog::getStatus, "WAITING_CONFIRMATION")
                .set(AiToolCallLog::getStatus, "CONFIRMED")
                .set(AiToolCallLog::getConfirmationTime, confirmationTime)
                .set(AiToolCallLog::getUpdateTime, confirmationTime));
        if (updated == 0) {
            throw new IllegalArgumentException("该工具调用已经处理");
        }
        log.setStatus("CONFIRMED");
        log.setConfirmationTime(confirmationTime);
        log.setUpdateTime(confirmationTime);
        long preparationStart = System.currentTimeMillis();
        AiTool tool;
        Map<String, Object> arguments;
        ExecutionContext context;
        try {
            arguments = readMap(log.getArgumentsJson());
            context = readContext(log.getPendingContextJson());
            if (!Objects.equals(log.getAssistantId(), context.assistantId())
                    || !Objects.equals(log.getUserId(), context.userId())) {
                throw new IllegalStateException("工具调用上下文与待确认记录不一致");
            }
            tool = toolService.requireEnabledForAssistant(
                    log.getToolId(), context.assistantId());
        } catch (RuntimeException exception) {
            fail(log, "FAILED", "PRECONDITION_FAILED",
                    StringUtils.defaultIfBlank(exception.getMessage(), "确认后执行条件已变化"),
                    preparationStart);
            throw exception;
        }
        return execute(log, tool, arguments, context);
    }

    /** 查询本人发起的调用详情，供确认弹窗和调试面板刷新。 */
    public InvocationResult callResult(String requestId) {
        AiToolCallLog log = callLogDao.selectOne(new LambdaQueryWrapper<AiToolCallLog>()
                .eq(AiToolCallLog::getRequestId, requestId).last("limit 1"));
        if (log == null) {
            throw new IllegalArgumentException("工具调用记录不存在");
        }
        Long currentUserId = applicationDataScopeService.requireEmployee().getEmployeeId();
        if (!Objects.equals(log.getUserId(), currentUserId)
                && !applicationDataScopeService.isPlatformAdministrator()) {
            throw new IllegalArgumentException("无权查看该工具调用");
        }
        return resultFromLog(log);
    }

    /** 执行平台 API、标准 MCP 或第三方 HTTP 工具，并完整记录成功或失败结果。 */
    private InvocationResult execute(AiToolCallLog log, AiTool tool,
                                     Map<String, Object> arguments,
                                     ExecutionContext context) {
        long start = System.currentTimeMillis();
        log.setStatus("EXECUTING");
        log.setUpdateTime(LocalDateTime.now());
        callLogDao.updateById(log);
        try {
            Object result;
            Integer httpStatus;
            if (AiToolService.PLATFORM_API.equals(tool.getSourceType())) {
                HttpOutcome outcome = invokePlatform(tool, arguments, context, log);
                result = parseBody(outcome.body());
                httpStatus = outcome.statusCode();
            } else if (AiToolService.STANDARD_MCP.equals(tool.getSourceType())) {
                result = invokeStandardMcp(tool, arguments);
                httpStatus = null;
            } else {
                HttpOutcome outcome = invokeExternal(tool, arguments, context, log);
                result = parseBody(outcome.body());
                httpStatus = outcome.statusCode();
            }
            Object schemaResult = result;
            try {
                schemaService.validateResult(tool.getOutputSchema(), schemaResult);
            } catch (IllegalArgumentException validation) {
                if (schemaResult instanceof Map<?, ?> map && map.containsKey("data")) {
                    schemaResult = map.get("data");
                    schemaService.validateResult(tool.getOutputSchema(), schemaResult);
                } else {
                    throw validation;
                }
            }
            log.setStatus("SUCCESS");
            log.setResultJson(writeJson(schemaResult));
            log.setHttpStatus(httpStatus);
            log.setDurationMs(Math.max(System.currentTimeMillis() - start, 1));
            log.setErrorCode(null);
            log.setErrorMessage(null);
            log.setPendingContextJson(null);
            log.setUpdateTime(LocalDateTime.now());
            callLogDao.updateById(log);
            incrementToolCall(tool);
            return new InvocationResult(log.getRequestId(), log.getTraceId(), log.getStatus(),
                    schemaResult, "工具调用成功", httpStatus,
                    log.getDurationMs(), false);
        } catch (IllegalArgumentException exception) {
            fail(log, "SCHEMA_ERROR", "SCHEMA_ERROR", exception.getMessage(), start);
            throw exception;
        } catch (java.net.http.HttpTimeoutException exception) {
            fail(log, "TIMEOUT", "TIMEOUT", "工具调用超时", start);
            throw new IllegalStateException("工具调用超时");
        } catch (Exception exception) {
            fail(log, "FAILED",
                    AiToolService.STANDARD_MCP.equals(tool.getSourceType())
                            ? "MCP_CALL_FAILED" : "HTTP_CALL_FAILED",
                    StringUtils.defaultIfBlank(exception.getMessage(), "工具调用失败"), start);
            throw new IllegalStateException("工具调用失败：" + exception.getMessage());
        }
    }

    /** 根据平台 API 版本、生产环境和参数位置组装内部 HTTP 请求。 */
    private HttpOutcome invokePlatform(AiTool tool, Map<String, Object> arguments,
                                       ExecutionContext context, AiToolCallLog log) throws Exception {
        OpenApiEntity api = openApiDao.selectById(tool.getOpenApiId());
        OpenApiVersionEntity version = versionDao.selectById(tool.getSourceApiVersionId());
        if (api == null || version == null || !Objects.equals(api.getStatus(), 4)
                || !Objects.equals(version.getStatus(), 3)) {
            throw new IllegalStateException("来源 API 或关联版本已不可用");
        }
        OpenApiEnvironmentEntity environment = resolveProductionEnvironment(version.getVersionId());
        if (environment == null) {
            throw new IllegalStateException("来源 API 未配置可用的生产环境");
        }
        List<OpenApiParameterEntity> parameters = parameterDao.selectList(
                new LambdaQueryWrapper<OpenApiParameterEntity>()
                        .eq(OpenApiParameterEntity::getVersionId, version.getVersionId())
                        .eq(OpenApiParameterEntity::getDirection, 1)
                        .isNull(OpenApiParameterEntity::getParentId)
                        .orderByAsc(OpenApiParameterEntity::getSort));
        RequestParts parts = splitArguments(arguments, parameters);
        String path = fillPath(version.getInternalPath(), parts.path());
        String url = buildUrl(environment.getBaseUrl(), path, parts.query());
        byte[] body = buildBody(parts.body());
        Map<String, String> headers = contextHeaders(context, log);
        parts.headers().forEach((name, value) -> headers.put(name, Objects.toString(value, "")));
        return send(version.getRequestMethod(), url,
                StringUtils.defaultIfBlank(version.getContentType(), "application/json"),
                Objects.requireNonNullElse(version.getTimeoutSeconds(), tool.getTimeoutSeconds()),
                headers, body);
    }

    /** 调用第三方 HTTP 工具，并附加应用身份、时间戳、随机数和平台签名。 */
    private HttpOutcome invokeExternal(AiTool tool, Map<String, Object> arguments,
                                       ExecutionContext context, AiToolCallLog log) throws Exception {
        ApplicationEntity application = applicationDao.selectById(tool.getApplicationId());
        if (application == null || !isApplicationOnline(application)) {
            throw new IllegalStateException("工具所属应用未上架或已停用");
        }
        URI uri = validateExternalTarget(tool.getCallbackUrl());
        ApplicationCredentialEntity credential =
                credentialManager.getCurrentCredential(application.getApplicationId());
        if (credential == null) {
            throw new IllegalStateException("工具所属应用没有有效凭证");
        }
        String method = StringUtils.upperCase(tool.getHttpMethod());
        String url = uri.toString();
        byte[] body = new byte[0];
        if (Set.of("GET", "DELETE").contains(method)) {
            url = appendQuery(url, arguments);
        } else {
            Map<String, Object> callbackPayload = new LinkedHashMap<>();
            callbackPayload.put("toolCode", tool.getToolCode());
            callbackPayload.put("arguments", arguments);
            callbackPayload.put("context", executionContextPayload(context));
            body = writeJson(callbackPayload).getBytes(StandardCharsets.UTF_8);
        }
        long timestamp = System.currentTimeMillis();
        String nonce = newId();
        URI requestUri = URI.create(url);
        String requestTarget = requestUri.getRawPath()
                + (StringUtils.isBlank(requestUri.getRawQuery()) ? "" : "?" + requestUri.getRawQuery());
        String canonical = canonicalRequest(method, requestTarget,
                String.valueOf(timestamp), nonce, body);
        Map<String, String> headers = contextHeaders(context, log);
        headers.put("X-Nexora-App-Id", credential.getAppId());
        headers.put("X-Nexora-Timestamp", String.valueOf(timestamp));
        headers.put("X-Nexora-Nonce", nonce);
        headers.put("X-Nexora-Signature",
                credentialManager.signForPlatform(credential, canonical));
        return send(method, url, tool.getContentType(), tool.getTimeoutSeconds(), headers, body);
    }

    /** 通过标准 MCP 客户端调用远端 Server 暴露的原始工具。 */
    private Object invokeStandardMcp(AiTool tool, Map<String, Object> arguments) {
        if (tool.getMcpServerId() == null || StringUtils.isBlank(tool.getRemoteToolName())) {
            throw new IllegalStateException("标准 MCP 工具缺少 Server 或远端工具名称");
        }
        McpServer server = mcpServerService.requireCallable(tool.getMcpServerId());
        if (!Objects.equals(server.getApplicationId(), tool.getApplicationId())) {
            throw new IllegalStateException("标准 MCP 工具与 Server 所属应用不一致");
        }
        return mcpRemoteClientService.callTool(server, tool.getRemoteToolName(), arguments);
    }

    /** 将服务端可信执行上下文转换为第三方回调载荷。 */
    private Map<String, Object> executionContextPayload(ExecutionContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putValue(payload, "tenantId", context.tenantId());
        putValue(payload, "userId", context.userId());
        putValue(payload, "employeeId", context.employeeId());
        putValue(payload, "assistantId", context.assistantId());
        putValue(payload, "conversationId", context.conversationId());
        return payload;
    }

    /** 仅写入非空上下文字段，避免产生歧义值。 */
    private void putValue(Map<String, Object> payload, String name, Long value) {
        if (value != null) {
            payload.put(name, value);
        }
    }

    /** 使用 JDK HTTP 客户端发送请求，并限制超时、重定向和响应大小。 */
    private HttpOutcome send(String method, String url, String contentType,
                             Integer timeoutSeconds, Map<String, String> headers,
                             byte[] body) throws Exception {
        int timeout = Math.max(1, Math.min(Objects.requireNonNullElse(timeoutSeconds, 30), 60));
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(timeout));
        headers.forEach(builder::header);
        if (StringUtils.isNotBlank(contentType)) {
            builder.header("Content-Type", contentType);
        }
        builder.method(HttpMethod.valueOf(method).name(),
                body.length == 0 ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body));
        HttpResponse<byte[]> response = client.send(
                builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.body().length > MAX_RESPONSE_BYTES) {
            throw new IllegalStateException("工具响应超过 1 MB 限制");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("目标服务 HTTP " + response.statusCode()
                    + "：" + abbreviate(new String(response.body(), StandardCharsets.UTF_8), 500));
        }
        return new HttpOutcome(response.statusCode(), response.body());
    }

    /** 按 API 参数位置拆分路径、查询、请求头和请求体参数。 */
    private RequestParts splitArguments(Map<String, Object> arguments,
                                        List<OpenApiParameterEntity> parameters) {
        Map<String, Object> path = new LinkedHashMap<>();
        Map<String, Object> query = new LinkedHashMap<>();
        Map<String, Object> headers = new LinkedHashMap<>();
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, String> locations = parameters.stream().collect(
                java.util.stream.Collectors.toMap(
                        OpenApiParameterEntity::getParameterName,
                        item -> StringUtils.lowerCase(item.getLocation()),
                        (left, right) -> left));
        arguments.forEach((name, value) -> {
            String location = locations.getOrDefault(name, "body");
            if (location.contains("path") || location.contains("路径")) {
                path.put(name, value);
            } else if (location.contains("query") || location.contains("查询")) {
                query.put(name, value);
            } else if (location.contains("header") || location.contains("请求头")) {
                headers.put(name, value);
            } else {
                body.put(name, value);
            }
        });
        return new RequestParts(path, query, headers, body);
    }

    /** 优先选择生产环境，未标注生产环境时兼容使用首个启用环境。 */
    private OpenApiEnvironmentEntity resolveProductionEnvironment(Long versionId) {
        List<OpenApiEnvironmentEntity> environments = environmentDao.selectList(
                new LambdaQueryWrapper<OpenApiEnvironmentEntity>()
                        .eq(OpenApiEnvironmentEntity::getVersionId, versionId)
                        .eq(OpenApiEnvironmentEntity::getEnabledFlag, true)
                        .orderByAsc(OpenApiEnvironmentEntity::getEnvironmentId));
        return environments.stream()
                .filter(item -> Set.of("prod", "production", "pro")
                        .contains(StringUtils.lowerCase(item.getEnvironmentCode())))
                .findFirst().orElse(environments.isEmpty() ? null : environments.get(0));
    }

    /** 校验第三方回调 URL，并阻止本机、内网、链路本地和云元数据地址。 */
    private URI validateExternalTarget(String callbackUrl) {
        try {
            URI uri = URI.create(callbackUrl);
            if (!Set.of("http", "https").contains(StringUtils.lowerCase(uri.getScheme()))
                    || StringUtils.isBlank(uri.getHost()) || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("回调地址必须是有效的 HTTP(S) URL");
            }
            String host = StringUtils.lowerCase(uri.getHost());
            if ("localhost".equals(host) || host.endsWith(".localhost")
                    || "host.docker.internal".equals(host)
                    || "169.254.169.254".equals(host)) {
                throw new IllegalArgumentException("回调地址不能指向本机、容器宿主或云元数据地址");
            }
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                        || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new IllegalArgumentException("回调地址不能解析到内网或本机地址");
                }
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("回调地址无法解析");
        }
    }

    /** 构造平台可信链路请求头，不接受模型覆盖身份字段。 */
    private Map<String, String> contextHeaders(ExecutionContext context, AiToolCallLog log) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Nexora-Trace-Id", log.getTraceId());
        headers.put("X-Nexora-Request-Id", log.getRequestId());
        putHeader(headers, "X-Nexora-Tenant-Id", context.tenantId());
        putHeader(headers, "X-Nexora-User-Id", context.userId());
        putHeader(headers, "X-Nexora-Employee-Id", context.employeeId());
        putHeader(headers, "X-Nexora-Assistant-Id", context.assistantId());
        putHeader(headers, "X-Nexora-Conversation-Id", context.conversationId());
        return headers;
    }

    /** 向请求头写入非空数值。 */
    private void putHeader(Map<String, String> headers, String name, Long value) {
        if (value != null) {
            headers.put(name, String.valueOf(value));
        }
    }

    /** 限制同一用户对同一工具每分钟的调用次数。 */
    private void enforceRateLimit(Long toolId, Long userId) {
        if (userId == null) {
            return;
        }
        long count = callLogDao.selectCount(new LambdaQueryWrapper<AiToolCallLog>()
                .eq(AiToolCallLog::getToolId, toolId)
                .eq(AiToolCallLog::getUserId, userId)
                .ge(AiToolCallLog::getCreateTime, LocalDateTime.now().minusMinutes(1)));
        if (count >= MAX_CALLS_PER_MINUTE) {
            throw new IllegalArgumentException("工具调用过于频繁，请稍后再试");
        }
    }

    /** 更新工具累计调用次数和最近成功调用时间。 */
    private void incrementToolCall(AiTool tool) {
        tool.setTotalCallCount(Objects.requireNonNullElse(tool.getTotalCallCount(), 0L) + 1);
        tool.setLastCallTime(LocalDateTime.now());
        tool.setUpdateTime(LocalDateTime.now());
        toolDao.updateById(tool);
    }

    /** 统一落库失败状态、错误信息和调用耗时。 */
    private void fail(AiToolCallLog log, String status, String code,
                      String message, long start) {
        log.setStatus(status);
        log.setErrorCode(code);
        log.setErrorMessage(abbreviate(message, 1000));
        log.setDurationMs(Math.max(System.currentTimeMillis() - start, 1));
        log.setPendingContextJson(null);
        log.setUpdateTime(LocalDateTime.now());
        callLogDao.updateById(log);
    }

    /** 判断工具是否必须经过用户确认后才能执行。 */
    private boolean requiresConfirmation(AiTool tool) {
        return "ACTION".equals(tool.getToolType())
                || "REQUIRED".equals(tool.getConfirmationPolicy());
    }

    /** 将非空请求体序列化为 UTF-8 JSON 字节。 */
    private byte[] buildBody(Map<String, Object> body) {
        return body.isEmpty() ? new byte[0] : writeJson(body).getBytes(StandardCharsets.UTF_8);
    }

    /** 替换并编码路径参数，缺少必要参数时拒绝调用。 */
    private String fillPath(String path, Map<String, Object> arguments) {
        String result = StringUtils.defaultIfBlank(path, "/");
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}",
                    URLEncoder.encode(Objects.toString(entry.getValue(), ""),
                            StandardCharsets.UTF_8).replace("+", "%20"));
        }
        if (result.matches(".*\\{[^}]+}.*")) {
            throw new IllegalArgumentException("路径参数不完整");
        }
        return result;
    }

    /** 合并环境基础地址、接口路径和查询参数。 */
    private String buildUrl(String baseUrl, String path, Map<String, Object> query) {
        String url = StringUtils.removeEnd(baseUrl, "/") + "/"
                + StringUtils.removeStart(path, "/");
        return appendQuery(url, query);
    }

    /** 使用 URI 构建器安全追加查询参数。 */
    private String appendQuery(String url, Map<String, Object> query) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        query.forEach((name, value) -> {
            if (value instanceof Iterable<?> iterable) {
                iterable.forEach(item -> builder.queryParam(name, item));
            } else if (value != null) {
                builder.queryParam(name, value);
            }
        });
        return builder.build().encode().toUriString();
    }

    /** 优先解析 JSON 响应，非 JSON 内容按文本返回。 */
    private Object parseBody(byte[] body) {
        if (body == null || body.length == 0) {
            return Map.of();
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            return objectMapper.convertValue(node, Object.class);
        } catch (Exception exception) {
            return Map.of("content", new String(body, StandardCharsets.UTF_8));
        }
    }

    /** 按开放平台约定生成第三方回调签名原文。 */
    private String canonicalRequest(String method, String requestTarget,
                                    String timestamp, String nonce, byte[] body) {
        return StringUtils.upperCase(StringUtils.trimToEmpty(method)) + "\n"
                + Objects.requireNonNullElse(requestTarget, "") + "\n"
                + timestamp + "\n" + nonce + "\n" + sha256(body);
    }

    /** 计算请求体 SHA-256 摘要。 */
    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception exception) {
            throw new IllegalStateException("计算请求摘要失败", exception);
        }
    }

    /** 判断第三方工具所属应用是否处于可调用状态。 */
    private boolean isApplicationOnline(ApplicationEntity application) {
        return Objects.equals(application.getOnlineStatus(), 2)
                || application.getOnlineStatus() == null
                && Objects.equals(application.getListingStatus(), 2);
    }

    /** 将数据库调用日志转换为前端可展示的统一调用结果。 */
    private InvocationResult resultFromLog(AiToolCallLog log) {
        Object result = StringUtils.isBlank(log.getResultJson())
                ? null : readObject(log.getResultJson());
        return new InvocationResult(log.getRequestId(), log.getTraceId(), log.getStatus(),
                result, StringUtils.firstNonBlank(log.getErrorMessage(),
                statusMessage(log.getStatus())), log.getHttpStatus(),
                log.getDurationMs(), "WAITING_CONFIRMATION".equals(log.getStatus()));
    }

    /** 将内部调用状态转换为用户可读说明。 */
    private String statusMessage(String status) {
        return switch (StringUtils.defaultString(status)) {
            case "WAITING_CONFIRMATION" -> "等待用户确认";
            case "USER_REJECTED" -> "用户已拒绝执行";
            case "CONFIRMATION_EXPIRED" -> "工具调用确认已过期";
            case "SUCCESS" -> "工具调用成功";
            case "TIMEOUT" -> "工具调用超时";
            default -> "工具调用处理中";
        };
    }

    /** 将调用参数、上下文或结果序列化为 JSON。 */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("工具调用参数无法序列化");
        }
    }

    /** 从调用日志恢复工具参数。 */
    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("工具调用参数记录损坏", exception);
        }
    }

    /** 从待确认日志恢复服务端可信执行上下文。 */
    private ExecutionContext readContext(String json) {
        try {
            return objectMapper.readValue(json, ExecutionContext.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("工具调用上下文记录损坏", exception);
        }
    }

    /** 从调用日志恢复任意 JSON 结果，损坏时返回空结果。 */
    private Object readObject(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    /** 截断外部服务错误内容，避免日志和响应过大。 */
    private String abbreviate(String value, int length) {
        return StringUtils.abbreviate(StringUtils.defaultString(value), length);
    }

    /** 生成无分隔符的请求或追踪编号。 */
    private String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 工具调用期间由服务端注入的可信身份与会话上下文。 */
    public record ExecutionContext(Long tenantId, Long userId, Long employeeId,
                                   Long assistantId, Long conversationId) {
    }

    /** 返回给管理端或助手页面的统一工具调用结果。 */
    public record InvocationResult(String requestId, String traceId, String status,
                                   Object result, String message, Integer httpStatus,
                                   Long durationMs, boolean confirmationRequired) {
    }

    /** HTTP 状态码与原始响应体。 */
    private record HttpOutcome(int statusCode, byte[] body) {
    }

    /** 平台 API 参数按 HTTP 位置拆分后的结果。 */
    private record RequestParts(Map<String, Object> path, Map<String, Object> query,
                                Map<String, Object> headers, Map<String, Object> body) {
    }
}
