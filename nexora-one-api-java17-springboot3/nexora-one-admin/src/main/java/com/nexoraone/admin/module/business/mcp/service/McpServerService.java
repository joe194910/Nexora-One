package com.nexoraone.admin.module.business.mcp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexoraone.admin.module.business.application.dao.ApplicationDao;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationEntity;
import com.nexoraone.admin.module.business.application.service.ApplicationDataScopeService;
import com.nexoraone.admin.module.business.mcp.dao.AiToolMappers;
import com.nexoraone.admin.module.business.mcp.domain.AiTool;
import com.nexoraone.admin.module.business.mcp.domain.AiToolForms;
import com.nexoraone.admin.module.business.mcp.domain.McpServer;
import com.nexoraone.admin.module.system.login.domain.RequestEmployee;
import com.nexoraone.base.common.domain.PageResult;
import com.nexoraone.base.module.support.apiencrypt.service.ApiEncryptService;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 标准 MCP Server 注册、探活、工具发现和安全同步服务。 */
@Service
public class McpServerService {

    private static final String STANDARD_MCP = "STANDARD_MCP";

    @Resource
    private AiToolMappers.McpServerDao serverDao;
    @Resource
    private AiToolMappers.ToolDao toolDao;
    @Resource
    private ApplicationDao applicationDao;
    @Resource
    private ApplicationDataScopeService applicationDataScopeService;
    @Resource
    private McpRemoteClientService remoteClientService;
    @Resource
    private ApiEncryptService apiEncryptService;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 分页查询当前员工有权管理的标准 MCP Server。
     *
     * @param form 查询条件
     * @return MCP Server 分页数据
     */
    public PageResult<Map<String, Object>> query(AiToolForms.McpServerQuery form) {
        LambdaQueryWrapper<McpServer> wrapper = new LambdaQueryWrapper<McpServer>()
                .eq(StringUtils.isNotBlank(form.getOnlineStatus()),
                        McpServer::getOnlineStatus, form.getOnlineStatus())
                .eq(form.getEnabledFlag() != null,
                        McpServer::getEnabledFlag, form.getEnabledFlag())
                .eq(form.getApplicationId() != null,
                        McpServer::getApplicationId, form.getApplicationId())
                .and(StringUtils.isNotBlank(form.getSearchWord()), query -> query
                        .like(McpServer::getServerName, form.getSearchWord())
                        .or().like(McpServer::getServerCode, form.getSearchWord()))
                .orderByDesc(McpServer::getUpdateTime)
                .orderByDesc(McpServer::getServerId);
        List<McpServer> visible = serverDao.selectList(wrapper).stream()
                .filter(this::canManage)
                .toList();
        int pageNum = Math.toIntExact(form.getPageNum());
        int pageSize = Math.toIntExact(form.getPageSize());
        int from = Math.min((pageNum - 1) * pageSize, visible.size());
        int to = Math.min(from + pageSize, visible.size());
        List<Map<String, Object>> records = visible.subList(from, to).stream()
                .map(this::toView)
                .toList();
        return pageResult(form.getPageNum(), form.getPageSize(), visible.size(), records);
    }

    /** 统计当前员工可管理的标准 MCP Server 数量和在线情况。 */
    public Map<String, Long> summary() {
        List<McpServer> visible = serverDao.selectList(new LambdaQueryWrapper<McpServer>())
                .stream().filter(this::canManage).toList();
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("total", (long) visible.size());
        result.put("online", visible.stream()
                .filter(item -> "ONLINE".equals(item.getOnlineStatus())).count());
        result.put("abnormal", visible.stream()
                .filter(item -> Set.of("OFFLINE", "ABNORMAL")
                        .contains(item.getOnlineStatus())).count());
        result.put("tools", visible.stream()
                .map(McpServer::getDiscoveredToolCount)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue).sum());
        return result;
    }

    /** 查询单个标准 MCP Server 的脱敏配置和已发现工具。 */
    public Map<String, Object> detail(Long serverId) {
        McpServer server = requireManageable(serverId);
        Map<String, Object> result = toView(server);
        result.put("tools", toolDao.selectList(new LambdaQueryWrapper<AiTool>()
                        .eq(AiTool::getMcpServerId, serverId)
                        .orderByAsc(AiTool::getToolName))
                .stream().map(this::toolView).toList());
        return result;
    }

    /**
     * 新增或更新标准 MCP Server。
     *
     * <p>更新时凭证留空表示保留原值；endpoint、鉴权或凭证变化后必须重新探活。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> save(AiToolForms.McpServerSave form) {
        ApplicationEntity application = requireManageableApplication(form.getApplicationId());
        McpServer server = form.getServerId() == null
                ? new McpServer() : requireManageable(form.getServerId());
        if (server.getServerId() != null
                && !Objects.equals(server.getApplicationId(), form.getApplicationId())) {
            throw new IllegalArgumentException("MCP Server 创建后不能修改所属应用");
        }
        if (server.getServerId() != null
                && !Objects.equals(server.getServerCode(), form.getServerCode())) {
            throw new IllegalArgumentException("MCP Server 创建后不能修改稳定编码");
        }
        assertCodeAvailable(form.getServerCode(), form.getServerId());
        remoteClientService.validateEndpoint(form.getEndpointUrl());
        String authSecret = normalizeAuthSecret(form.getAuthType(), form.getAuthSecret());
        validateAuthentication(form, server, authSecret);

        boolean newRecord = server.getServerId() == null;
        boolean enabled = !Boolean.FALSE.equals(form.getEnabledFlag());
        String authHeaderName = "API_KEY_HEADER".equals(form.getAuthType())
                ? StringUtils.trim(form.getAuthHeaderName()) : null;
        int timeoutSeconds = Objects.requireNonNullElse(form.getTimeoutSeconds(), 30);
        boolean enabledChanged = !newRecord
                && !Objects.equals(server.getEnabledFlag(), enabled);
        boolean connectionChanged = newRecord
                || !Objects.equals(server.getEndpointUrl(), StringUtils.trim(form.getEndpointUrl()))
                || !Objects.equals(server.getAuthType(), form.getAuthType())
                || !Objects.equals(server.getAuthHeaderName(), authHeaderName)
                || !Objects.equals(server.getTimeoutSeconds(), timeoutSeconds)
                || StringUtils.isNotBlank(authSecret);
        RequestEmployee employee = applicationDataScopeService.requireEmployee();
        server.setApplicationId(application.getApplicationId());
        server.setServerCode(form.getServerCode());
        server.setServerName(form.getServerName());
        server.setDescription(form.getDescription());
        server.setEndpointUrl(StringUtils.trim(form.getEndpointUrl()));
        server.setTransportType("STREAMABLE_HTTP");
        server.setAuthType(form.getAuthType());
        server.setAuthHeaderName(authHeaderName);
        if ("NONE".equals(form.getAuthType())) {
            server.setAuthSecretCipher(null);
        } else if (StringUtils.isNotBlank(authSecret)) {
            String cipher = apiEncryptService.encrypt(authSecret);
            if (StringUtils.isBlank(cipher)) {
                throw new IllegalStateException("MCP Server 鉴权凭证加密失败");
            }
            server.setAuthSecretCipher(cipher);
        }
        server.setTimeoutSeconds(timeoutSeconds);
        server.setEnabledFlag(enabled);
        if (connectionChanged) {
            server.setProtocolVersion(null);
            server.setServerVersion(null);
        }
        if (!enabled) {
            server.setOnlineStatus("OFFLINE");
            server.setLastProbeMessage("Server 已停用");
        } else if (connectionChanged || enabledChanged) {
            server.setOnlineStatus("UNKNOWN");
            server.setLastProbeMessage(connectionChanged
                    ? "连接配置已变更，请重新探活并同步工具"
                    : "Server 已启用，请重新探活并同步工具");
        }
        server.setUpdateUserId(employee.getEmployeeId());
        server.setUpdateTime(LocalDateTime.now());
        if (newRecord) {
            server.setDiscoveredToolCount(0);
            server.setCreateUserId(employee.getEmployeeId());
            server.setCreateTime(LocalDateTime.now());
            serverDao.insert(server);
        } else {
            serverDao.updateById(server);
        }
        if (connectionChanged) {
            markToolsUnavailable(server.getServerId(), "OFFLINE", true, true);
        } else if (!enabled || enabledChanged) {
            markToolsUnavailable(server.getServerId(), "OFFLINE", true, false);
        }
        return toView(server);
    }

    /** 启用或停用标准 MCP Server；停用时同步停止其全部工具。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateStatus(AiToolForms.McpServerStatus form) {
        McpServer server = requireManageable(form.getServerId());
        server.setEnabledFlag(form.getEnabledFlag());
        server.setOnlineStatus(Boolean.TRUE.equals(form.getEnabledFlag())
                ? "UNKNOWN" : "OFFLINE");
        server.setLastProbeMessage(Boolean.TRUE.equals(form.getEnabledFlag())
                ? "Server 已启用，请执行探活并同步工具" : "Server 已停用");
        server.setUpdateUserId(applicationDataScopeService.requireEmployee().getEmployeeId());
        server.setUpdateTime(LocalDateTime.now());
        serverDao.updateById(server);
        markToolsUnavailable(server.getServerId(), "OFFLINE", true, false);
        return toView(server);
    }

    /**
     * 连接远端 MCP Server，执行 initialize 和 tools/list，并导入新发现的工具。
     *
     * <p>已存在工具的 Schema 不会被静默覆盖，避免正在使用的助手能力发生变化。</p>
     */
    public Map<String, Object> probeAndSync(Long serverId) {
        McpServer server = requireManageable(serverId);
        if (!Boolean.TRUE.equals(server.getEnabledFlag())) {
            throw new IllegalArgumentException("MCP Server 已停用，不能执行探活");
        }
        try {
            McpRemoteClientService.ProbeResult probe = remoteClientService.probe(server);
            return Objects.requireNonNull(transactionTemplate.execute(
                    status -> persistProbeSuccess(server, probe)));
        } catch (RuntimeException exception) {
            recordProbeFailure(server, exception);
            throw exception;
        }
    }

    /**
     * 明确同步单个远端 MCP 工具的最新 Schema。
     *
     * <p>同步后保留智能助手绑定关系，但工具会回到待测试、待审核和停用状态，
     * 防止远端契约变化直接影响正在使用的助手。</p>
     */
    public Map<String, Object> syncToolSchema(Long serverId, Long toolId) {
        McpServer server = requireManageable(serverId);
        if (!Boolean.TRUE.equals(server.getEnabledFlag())
                || !"ONLINE".equals(server.getOnlineStatus())) {
            throw new IllegalStateException("MCP Server 未启用或不在线，请先探活");
        }
        AiTool tool = toolDao.selectById(toolId);
        if (tool == null || !Objects.equals(tool.getMcpServerId(), serverId)
                || !STANDARD_MCP.equals(tool.getSourceType())) {
            throw new IllegalArgumentException("MCP 工具不存在或不属于当前 Server");
        }

        Tool remoteTool = remoteClientService.probe(server).tools().stream()
                .filter(item -> Objects.equals(item.name(), tool.getRemoteToolName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("远端 MCP Server 未返回该工具"));
        String inputSchema = schemaJson(remoteTool.inputSchema(), true);
        String outputSchema = schemaJson(remoteTool.outputSchema(), false);
        if (schemaEquals(tool.getInputSchema(), inputSchema)
                && schemaEquals(tool.getOutputSchema(), outputSchema)) {
            throw new IllegalArgumentException("远端工具 Schema 没有变化");
        }

        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            McpServer currentServer = serverDao.selectById(serverId);
            if (currentServer == null || !Boolean.TRUE.equals(currentServer.getEnabledFlag())
                    || !"ONLINE".equals(currentServer.getOnlineStatus())
                    || !sameConnection(server, currentServer)) {
                throw new IllegalStateException("MCP Server 配置或状态已变化，请重新探活后再同步");
            }
            AiTool current = toolDao.selectById(toolId);
            if (current == null || !Objects.equals(current.getMcpServerId(), serverId)
                    || !STANDARD_MCP.equals(current.getSourceType())) {
                throw new IllegalArgumentException("MCP 工具不存在或不属于当前 Server");
            }
            current.setInputSchema(inputSchema);
            current.setOutputSchema(outputSchema);
            current.setDescription(StringUtils.firstNonBlank(
                    remoteTool.description(), current.getDescription()));
            current.setAuditStatus("PENDING");
            current.setAuditRemark(null);
            current.setEnabledStatus("DISABLED");
            current.setOnlineStatus("ONLINE");
            current.setLastTestStatus(null);
            current.setLastTestMessage("Schema 已同步，请重新测试并审核后启用");
            current.setLastTestTime(null);
            current.setSchemaSyncRequired(false);
            current.setLastHeartbeatTime(LocalDateTime.now());
            current.setUpdateUserId(applicationDataScopeService.requireEmployee().getEmployeeId());
            current.setUpdateTime(LocalDateTime.now());
            toolDao.updateById(current);
            return toolView(current);
        }));
    }

    /** 读取当前员工可维护的标准 MCP Server。 */
    public McpServer requireManageable(Long serverId) {
        McpServer server = serverDao.selectById(serverId);
        if (server == null || !canManage(server)) {
            throw new IllegalArgumentException("MCP Server 不存在或无权管理");
        }
        return server;
    }

    /** 读取当前可调用的标准 MCP Server，供助手运行时最终校验。 */
    public McpServer requireCallable(Long serverId) {
        McpServer server = serverDao.selectById(serverId);
        if (server == null || !Boolean.TRUE.equals(server.getEnabledFlag())
                || !"ONLINE".equals(server.getOnlineStatus())) {
            throw new IllegalStateException("MCP Server 未启用或不在线");
        }
        return server;
    }

    /** 在短事务内原子写入探活成功状态和本次工具发现结果。 */
    private Map<String, Object> persistProbeSuccess(
            McpServer server, McpRemoteClientService.ProbeResult probe) {
        McpServer current = serverDao.selectById(server.getServerId());
        if (current == null || !Boolean.TRUE.equals(current.getEnabledFlag())
                || !canManage(current) || !sameConnection(server, current)) {
            throw new IllegalStateException("MCP Server 配置或状态已变化，本次探活结果已丢弃");
        }
        SyncStatistics statistics = synchronizeTools(current, probe.tools());
        long discoveredToolCount = probe.tools().stream()
                .map(Tool::name)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .count();
        current.setOnlineStatus("ONLINE");
        current.setProtocolVersion(probe.protocolVersion());
        current.setServerVersion(probe.serverVersion());
        current.setDiscoveredToolCount(Math.toIntExact(discoveredToolCount));
        current.setLastProbeTime(LocalDateTime.now());
        current.setLastProbeMessage("连接成功，发现 " + discoveredToolCount + " 个工具");
        current.setUpdateUserId(applicationDataScopeService.requireEmployee().getEmployeeId());
        current.setUpdateTime(LocalDateTime.now());
        serverDao.updateById(current);

        Map<String, Object> result = toView(current);
        result.put("remoteServerName", probe.serverName());
        result.put("createdToolCount", statistics.created());
        result.put("unchangedToolCount", statistics.unchanged());
        result.put("schemaChangedToolCount", statistics.schemaChanged());
        result.put("offlineToolCount", statistics.offline());
        return result;
    }

    /** 探活或同步失败后单独记录异常，且不让记录失败覆盖原始异常。 */
    private void recordProbeFailure(McpServer server, RuntimeException cause) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                McpServer current = serverDao.selectById(server.getServerId());
                if (current == null || !Boolean.TRUE.equals(current.getEnabledFlag())
                        || !sameConnection(server, current)) {
                    return;
                }
                current.setOnlineStatus("ABNORMAL");
                current.setLastProbeTime(LocalDateTime.now());
                current.setLastProbeMessage(StringUtils.abbreviate(
                        StringUtils.defaultIfBlank(cause.getMessage(), "MCP Server 探活失败"), 1000));
                current.setUpdateUserId(applicationDataScopeService.requireEmployee().getEmployeeId());
                current.setUpdateTime(LocalDateTime.now());
                serverDao.updateById(current);
                markToolsUnavailable(current.getServerId(), "ABNORMAL", false, false);
            });
        } catch (RuntimeException recordException) {
            cause.addSuppressed(recordException);
        }
    }

    /** 把远端工具定义导入统一工具注册表，并标记本次未发现的旧工具离线。 */
    private SyncStatistics synchronizeTools(McpServer server, List<Tool> remoteTools) {
        List<AiTool> existingTools = toolDao.selectList(new LambdaQueryWrapper<AiTool>()
                .eq(AiTool::getMcpServerId, server.getServerId()));
        Map<String, AiTool> existingByName = new LinkedHashMap<>();
        existingTools.forEach(tool -> existingByName.put(tool.getRemoteToolName(), tool));
        Set<String> discoveredNames = new HashSet<>();
        int created = 0;
        int unchanged = 0;
        int schemaChanged = 0;

        for (Tool remoteTool : remoteTools) {
            if (StringUtils.isBlank(remoteTool.name())
                    || !discoveredNames.add(remoteTool.name())) {
                continue;
            }
            String inputSchema = schemaJson(remoteTool.inputSchema(), true);
            String outputSchema = schemaJson(remoteTool.outputSchema(), false);
            AiTool existing = existingByName.get(remoteTool.name());
            if (existing == null) {
                createDiscoveredTool(server, remoteTool, inputSchema, outputSchema);
                created++;
                continue;
            }
            if (!schemaEquals(existing.getInputSchema(), inputSchema)
                    || !schemaEquals(existing.getOutputSchema(), outputSchema)) {
                schemaChanged++;
                existing.setLastTestMessage("远端 Schema 已变化，请审核后手工决定是否更新工具定义");
                existing.setLastTestStatus(null);
                existing.setLastTestTime(null);
                existing.setSchemaSyncRequired(true);
                existing.setEnabledStatus("DISABLED");
            } else {
                unchanged++;
                if (Boolean.TRUE.equals(existing.getSchemaSyncRequired())) {
                    existing.setSchemaSyncRequired(false);
                    existing.setLastTestStatus(null);
                    existing.setLastTestTime(null);
                    existing.setLastTestMessage("远端 Schema 已恢复为当前版本，请重新测试后启用");
                }
            }
            existing.setOnlineStatus("ONLINE");
            existing.setLastHeartbeatTime(LocalDateTime.now());
            existing.setUpdateTime(LocalDateTime.now());
            toolDao.updateById(existing);
        }

        int offline = 0;
        for (AiTool existing : existingTools) {
            if (!discoveredNames.contains(existing.getRemoteToolName())) {
                existing.setOnlineStatus("OFFLINE");
                existing.setEnabledStatus("DISABLED");
                existing.setLastTestStatus(null);
                existing.setLastTestTime(null);
                existing.setLastTestMessage("远端 MCP Server 本次未返回该工具");
                existing.setUpdateTime(LocalDateTime.now());
                toolDao.updateById(existing);
                offline++;
            }
        }
        return new SyncStatistics(created, unchanged, schemaChanged, offline);
    }

    /** 为首次发现的远端工具创建待测试、待审核的统一工具记录。 */
    private void createDiscoveredTool(McpServer server, Tool remoteTool,
                                      String inputSchema, String outputSchema) {
        RequestEmployee employee = applicationDataScopeService.requireEmployee();
        AiTool tool = new AiTool();
        tool.setToolCode(generateToolCode(server.getServerCode(), remoteTool.name()));
        tool.setToolName(StringUtils.firstNonBlank(remoteTool.title(), remoteTool.name()));
        tool.setDescription(StringUtils.firstNonBlank(remoteTool.description(),
                "来自 " + server.getServerName() + " 的 MCP 工具 " + remoteTool.name()));
        tool.setSourceType(STANDARD_MCP);
        tool.setToolType("QUERY");
        tool.setApplicationId(server.getApplicationId());
        tool.setMcpServerId(server.getServerId());
        tool.setRemoteToolName(remoteTool.name());
        tool.setHttpMethod("POST");
        tool.setContentType("application/json");
        tool.setTimeoutSeconds(server.getTimeoutSeconds());
        tool.setRiskLevel("LOW");
        tool.setConfirmationPolicy("AUTO");
        tool.setInputSchema(inputSchema);
        tool.setOutputSchema(outputSchema);
        tool.setAuditStatus("PENDING");
        tool.setEnabledStatus("DISABLED");
        tool.setOnlineStatus("ONLINE");
        tool.setSchemaSyncRequired(false);
        tool.setLastHeartbeatTime(LocalDateTime.now());
        tool.setTotalCallCount(0L);
        tool.setCreateUserId(employee.getEmployeeId());
        tool.setUpdateUserId(employee.getEmployeeId());
        tool.setCreateTime(LocalDateTime.now());
        tool.setUpdateTime(LocalDateTime.now());
        toolDao.insert(tool);
    }

    /** 生成符合模型工具命名约束且全局唯一的平台工具编码。 */
    private String generateToolCode(String serverCode, String remoteName) {
        String base = (serverCode + "_" + remoteName)
                .replaceAll("[^A-Za-z0-9_]", "_")
                .replaceAll("_+", "_");
        if (!base.matches("^[A-Za-z].*")) {
            base = "mcp_" + base;
        }
        base = StringUtils.substring(base, 0, 90);
        String candidate = base;
        int suffix = 2;
        while (toolDao.selectCount(new LambdaQueryWrapper<AiTool>()
                .eq(AiTool::getToolCode, candidate)) > 0) {
            candidate = StringUtils.substring(base, 0, 94) + "_" + suffix++;
        }
        return candidate;
    }

    /** 把远端 SDK Schema 对象规范化为数据库 JSON 文本。 */
    private String schemaJson(Map<String, Object> schema, boolean input) {
        if (!input && (schema == null || schema.isEmpty())) {
            return null;
        }
        Map<String, Object> value = schema == null || schema.isEmpty()
                ? new LinkedHashMap<>() : new LinkedHashMap<>(schema);
        value.putIfAbsent("type", "object");
        if (input) {
            value.putIfAbsent("properties", Map.of());
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("远端 MCP 工具 Schema 无法序列化");
        }
    }

    /** 按 JSON 结构比较 Schema，忽略空白和属性书写顺序。 */
    private boolean schemaEquals(String left, String right) {
        if (StringUtils.isBlank(left) && StringUtils.isBlank(right)) {
            return true;
        }
        try {
            return Objects.equals(objectMapper.readTree(left), objectMapper.readTree(right));
        } catch (Exception exception) {
            return Objects.equals(left, right);
        }
    }

    /** 校验鉴权配置，并保证新建配置具备真实可用的凭证。 */
    private void validateAuthentication(
            AiToolForms.McpServerSave form, McpServer server, String authSecret) {
        if ("NONE".equals(form.getAuthType())) {
            return;
        }
        if ("API_KEY_HEADER".equals(form.getAuthType())
                && StringUtils.isBlank(form.getAuthHeaderName())) {
            throw new IllegalArgumentException("API Key 鉴权必须填写请求头名称");
        }
        boolean sameAuthType = Objects.equals(server.getAuthType(), form.getAuthType());
        if (StringUtils.isBlank(authSecret)
                && (server.getServerId() == null || !sameAuthType
                || StringUtils.isBlank(server.getAuthSecretCipher()))) {
            throw new IllegalArgumentException("请填写 MCP Server 鉴权凭证");
        }
    }

    /** 清理鉴权凭证并兼容直接粘贴完整 Bearer 请求头值。 */
    private String normalizeAuthSecret(String authType, String authSecret) {
        if (StringUtils.isBlank(authSecret)) {
            return authSecret;
        }
        String normalized = StringUtils.trim(authSecret);
        if ("BEARER".equals(authType)
                && StringUtils.startsWithIgnoreCase(normalized, "Bearer ")) {
            normalized = StringUtils.trim(normalized.substring("Bearer ".length()));
        }
        if (StringUtils.containsAny(normalized, '\r', '\n')) {
            throw new IllegalArgumentException("鉴权凭证不能包含换行字符");
        }
        return normalized;
    }

    /** 校验 Server 编码在平台范围内唯一。 */
    private void assertCodeAvailable(String serverCode, Long excludedServerId) {
        long count = serverDao.selectCount(new LambdaQueryWrapper<McpServer>()
                .eq(McpServer::getServerCode, serverCode)
                .ne(excludedServerId != null, McpServer::getServerId, excludedServerId));
        if (count > 0) {
            throw new IllegalArgumentException("MCP Server 编码已存在");
        }
    }

    /** 读取当前员工有权维护的应用。 */
    private ApplicationEntity requireManageableApplication(Long applicationId) {
        ApplicationEntity application = applicationDao.selectById(applicationId);
        if (application == null || !applicationDataScopeService.canManage(application)) {
            throw new IllegalArgumentException("应用不存在或无权管理");
        }
        return application;
    }

    /** 判断当前员工是否可以管理指定 MCP Server。 */
    private boolean canManage(McpServer server) {
        if (applicationDataScopeService.isPlatformAdministrator()) {
            return true;
        }
        ApplicationEntity application = applicationDao.selectById(server.getApplicationId());
        return application != null
                && applicationDataScopeService.canManage(application);
    }

    /** 判断探活开始前后的连接配置是否一致，避免旧请求结果覆盖管理员刚保存的新配置。 */
    private boolean sameConnection(McpServer expected, McpServer current) {
        return Objects.equals(expected.getApplicationId(), current.getApplicationId())
                && Objects.equals(expected.getEndpointUrl(), current.getEndpointUrl())
                && Objects.equals(expected.getTransportType(), current.getTransportType())
                && Objects.equals(expected.getAuthType(), current.getAuthType())
                && Objects.equals(expected.getAuthHeaderName(), current.getAuthHeaderName())
                && Objects.equals(expected.getAuthSecretCipher(), current.getAuthSecretCipher())
                && Objects.equals(expected.getTimeoutSeconds(), current.getTimeoutSeconds());
    }

    /** 将 Server 实体转换为页面视图，并移除加密凭证。 */
    private Map<String, Object> toView(McpServer server) {
        Map<String, Object> result = objectMapper.convertValue(server, LinkedHashMap.class);
        result.remove("authSecretCipher");
        result.put("credentialConfigured", StringUtils.isNotBlank(server.getAuthSecretCipher()));
        ApplicationEntity application = applicationDao.selectById(server.getApplicationId());
        result.put("applicationName", application == null ? null : application.getApplicationName());
        result.put("applicationCode", application == null ? null : application.getApplicationCode());
        return result;
    }

    /** 将 Server 详情中的工具压缩为必要的审核和运行状态字段。 */
    private Map<String, Object> toolView(AiTool tool) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("toolId", tool.getToolId());
        result.put("toolName", tool.getToolName());
        result.put("toolCode", tool.getToolCode());
        result.put("remoteToolName", tool.getRemoteToolName());
        result.put("auditStatus", tool.getAuditStatus());
        result.put("enabledStatus", tool.getEnabledStatus());
        result.put("onlineStatus", tool.getOnlineStatus());
        result.put("lastTestMessage", tool.getLastTestMessage());
        result.put("schemaSyncRequired", Boolean.TRUE.equals(tool.getSchemaSyncRequired()));
        return result;
    }

    /** 批量刷新 Server 关联工具的在线和启停状态。 */
    private void markToolsUnavailable(Long serverId, String onlineStatus,
                                      boolean disableTool, boolean clearTestResult) {
        List<AiTool> tools = toolDao.selectList(new LambdaQueryWrapper<AiTool>()
                .eq(AiTool::getMcpServerId, serverId));
        for (AiTool tool : tools) {
            tool.setOnlineStatus(onlineStatus);
            if (disableTool) {
                tool.setEnabledStatus("DISABLED");
            }
            if (clearTestResult) {
                tool.setLastTestStatus(null);
                tool.setLastTestTime(null);
                if (!Boolean.TRUE.equals(tool.getSchemaSyncRequired())) {
                    tool.setLastTestMessage("MCP Server 连接配置已变更，请重新探活并测试工具");
                }
            }
            tool.setUpdateTime(LocalDateTime.now());
            toolDao.updateById(tool);
        }
    }

    /** 按项目统一 PageResult 结构组装内存分页结果。 */
    private <T> PageResult<T> pageResult(Long pageNum, Long pageSize,
                                         long total, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setPages(total == 0 ? 0 : (total + pageSize - 1) / pageSize);
        result.setList(new ArrayList<>(list));
        result.setEmptyFlag(list.isEmpty());
        return result;
    }

    /** 单次工具发现同步统计。 */
    private record SyncStatistics(int created, int unchanged,
                                  int schemaChanged, int offline) {
    }
}
