package com.nexoraone.admin.module.business.mcp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexoraone.admin.module.business.application.dao.ApplicationDao;
import com.nexoraone.admin.module.business.application.dao.OpenApiDao;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationEntity;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import com.nexoraone.admin.module.business.application.service.ApplicationDataScopeService;
import com.nexoraone.admin.module.business.knowledge.dao.KnowledgeMappers;
import com.nexoraone.admin.module.business.knowledge.domain.KnowledgeAssistant;
import com.nexoraone.admin.module.business.mcp.dao.AiToolMappers;
import com.nexoraone.admin.module.business.mcp.domain.AiTool;
import com.nexoraone.admin.module.business.mcp.domain.AiToolAssistant;
import com.nexoraone.admin.module.business.mcp.domain.AiToolForms;
import com.nexoraone.admin.module.business.mcp.domain.AiToolSchemaSync;
import com.nexoraone.admin.module.business.mcp.domain.McpServer;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiVersionDao;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiVersionEntity;
import com.nexoraone.admin.module.business.openapi.service.OpenApiManageService;
import com.nexoraone.admin.module.system.login.domain.RequestEmployee;
import com.nexoraone.base.common.domain.PageResult;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** MCP 工具管理、开放注册、审核和助手关联的业务服务。 */
@Service
public class AiToolService {

    public static final String PLATFORM_API = "PLATFORM_API";
    public static final String EXTERNAL_HTTP = "EXTERNAL_HTTP";
    public static final String STANDARD_MCP = "STANDARD_MCP";
    public static final String APPROVED = "APPROVED";
    public static final String ENABLED = "ENABLED";

    @Resource
    private AiToolMappers.ToolDao toolDao;
    @Resource
    private AiToolMappers.SchemaSyncDao schemaSyncDao;
    @Resource
    private AiToolMappers.AssistantToolDao assistantToolDao;
    @Resource
    private AiToolMappers.McpServerDao mcpServerDao;
    @Resource
    private OpenApiDao openApiDao;
    @Resource
    private OpenApiVersionDao versionDao;
    @Resource
    private ApplicationDao applicationDao;
    @Resource
    private KnowledgeMappers.AssistantDao assistantDao;
    @Resource
    /** 应用与开放 API 共用的数据范围服务。 */
    private ApplicationDataScopeService applicationDataScopeService;
    @Resource
    private OpenApiManageService openApiManageService;
    @Resource
    private AiToolSchemaService schemaService;
    @Resource
    private ObjectMapper objectMapper;

    /** 查询当前员工管理范围内的工具。 */
    public PageResult<Map<String, Object>> query(AiToolForms.Query form) {
        LambdaQueryWrapper<AiTool> wrapper = new LambdaQueryWrapper<AiTool>()
                .eq(StringUtils.isNotBlank(form.getSourceType()), AiTool::getSourceType, form.getSourceType())
                .eq(StringUtils.isNotBlank(form.getRiskLevel()), AiTool::getRiskLevel, form.getRiskLevel())
                .eq(StringUtils.isNotBlank(form.getAuditStatus()), AiTool::getAuditStatus, form.getAuditStatus())
                .eq(StringUtils.isNotBlank(form.getEnabledStatus()), AiTool::getEnabledStatus, form.getEnabledStatus())
                .eq(form.getApplicationId() != null, AiTool::getApplicationId, form.getApplicationId())
                .and(StringUtils.isNotBlank(form.getSearchWord()), query -> query
                        .like(AiTool::getToolName, form.getSearchWord())
                        .or().like(AiTool::getToolCode, form.getSearchWord()))
                .orderByDesc(AiTool::getUpdateTime)
                .orderByDesc(AiTool::getToolId);
        List<AiTool> visible = toolDao.selectList(wrapper).stream()
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

    /** MCP 工具页面统计。 */
    public Map<String, Long> summary() {
        List<AiTool> visible = toolDao.selectList(new LambdaQueryWrapper<AiTool>())
                .stream().filter(this::canManage).toList();
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("total", (long) visible.size());
        result.put("platformApi", count(visible, PLATFORM_API));
        result.put("standardMcp", count(visible, STANDARD_MCP));
        result.put("externalHttp", count(visible, EXTERNAL_HTTP));
        result.put("pending", visible.stream()
                .filter(item -> "PENDING".equals(item.getAuditStatus())).count());
        return result;
    }

    /** 查询当前员工可管理的应用，供第三方 HTTP 工具登记时选择。 */
    public List<Map<String, Object>> manageableApplications() {
        return applicationDao.selectList(new LambdaQueryWrapper<ApplicationEntity>()
                        .orderByDesc(ApplicationEntity::getUpdateTime))
                .stream()
                .filter(applicationDataScopeService::canManage)
                .map(application -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("applicationId", application.getApplicationId());
                    item.put("applicationName", application.getApplicationName());
                    item.put("applicationCode", application.getApplicationCode());
                    item.put("listingStatus", application.getListingStatus());
                    item.put("onlineStatus", application.getOnlineStatus());
                    return item;
                })
                .toList();
    }

    /** 查询工具详情。 */
    public Map<String, Object> detail(Long toolId) {
        return toView(requireManageable(toolId));
    }

    /** 查询 API 详情页中的 AI 工具关系、版本和同步记录。 */
    public Map<String, Object> platformRelation(Long openApiId) {
        OpenApiEntity api = requireManageableApi(openApiId);
        AiTool tool = toolDao.selectOne(new LambdaQueryWrapper<AiTool>()
                .eq(AiTool::getOpenApiId, openApiId).last("limit 1"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("openApiId", api.getOpenApiId());
        result.put("apiName", api.getApiName());
        result.put("apiCode", api.getApiCode());
        result.put("apiStatus", api.getStatus());
        result.put("publishable", Objects.equals(api.getStatus(), 4)
                && openApiManageService.resolvePublishedVersion(api) != null);
        result.put("tool", tool == null ? null : toView(tool));
        if (tool == null) {
            result.put("versions", List.of());
            result.put("syncHistory", List.of());
            result.put("assistants", List.of());
            return result;
        }
        List<OpenApiVersionEntity> versions = versionDao.selectList(
                new LambdaQueryWrapper<OpenApiVersionEntity>()
                        .eq(OpenApiVersionEntity::getOpenApiId, openApiId)
                        .eq(OpenApiVersionEntity::getStatus, 3)
                        .orderByDesc(OpenApiVersionEntity::getVersionId));
        result.put("versions", versions.stream().map(version -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("versionId", version.getVersionId());
            row.put("versionNo", version.getVersionNo());
            row.put("associated", Objects.equals(version.getVersionId(), tool.getSourceApiVersionId()));
            row.put("publishTime", version.getUpdateTime());
            return row;
        }).toList());
        result.put("syncHistory", schemaSyncDao.selectList(
                new LambdaQueryWrapper<AiToolSchemaSync>()
                        .eq(AiToolSchemaSync::getToolId, tool.getToolId())
                        .orderByDesc(AiToolSchemaSync::getCreateTime)));
        result.put("assistants", assistantViews(tool.getToolId()));
        return result;
    }

    /** 发布抽屉预览线上 API 自动生成的工具 Schema。 */
    public Map<String, Object> platformPreview(Long openApiId) {
        OpenApiEntity api = requireManageableApi(openApiId);
        if (!Objects.equals(api.getStatus(), 4)) {
            throw new IllegalArgumentException("只有已上架的稳定 API 才能发布为 AI 工具");
        }
        OpenApiVersionEntity version = openApiManageService.resolvePublishedVersion(api);
        if (version == null) {
            throw new IllegalArgumentException("API 线上版本不存在");
        }
        AiToolSchemaService.SchemaPair schemas = schemaService.generate(version.getVersionId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("versionId", version.getVersionId());
        result.put("versionNo", version.getVersionNo());
        result.put("inputSchema", schemas.inputSchema());
        result.put("outputSchema", schemas.outputSchema());
        return result;
    }

    /** 将一个已稳定上架的平台 API 发布为 AI 工具。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publishPlatform(AiToolForms.PlatformPublish form) {
        OpenApiEntity api = requireManageableApi(form.getOpenApiId());
        if (!Objects.equals(api.getStatus(), 4)) {
            throw new IllegalArgumentException("只有已上架的稳定 API 才能发布为 AI 工具");
        }
        OpenApiVersionEntity version = openApiManageService.resolvePublishedVersion(api);
        if (version == null) {
            throw new IllegalArgumentException("API 线上版本不存在");
        }
        if (toolDao.selectCount(new LambdaQueryWrapper<AiTool>()
                .eq(AiTool::getOpenApiId, api.getOpenApiId())) > 0) {
            throw new IllegalArgumentException("该 API 已经发布为 AI 工具");
        }
        assertCodeAvailable(form.getToolCode(), null);
        AiToolSchemaService.SchemaPair schemas = schemaService.generate(version.getVersionId());
        RequestEmployee employee = applicationDataScopeService.requireEmployee();
        AiTool tool = new AiTool();
        tool.setToolCode(form.getToolCode());
        tool.setToolName(form.getToolName());
        tool.setDescription(form.getDescription());
        tool.setSourceType(PLATFORM_API);
        tool.setToolType(form.getToolType());
        tool.setOpenApiId(api.getOpenApiId());
        tool.setSourceApiVersionId(version.getVersionId());
        tool.setHttpMethod(version.getRequestMethod());
        tool.setContentType(StringUtils.defaultIfBlank(version.getContentType(), "application/json"));
        tool.setTimeoutSeconds(Objects.requireNonNullElse(version.getTimeoutSeconds(), 30));
        tool.setRiskLevel(form.getRiskLevel());
        tool.setConfirmationPolicy(resolveConfirmation(form.getToolType(), form.getConfirmationPolicy()));
        tool.setInputSchema(schemas.inputSchema());
        tool.setOutputSchema(schemas.outputSchema());
        tool.setAuditStatus(Boolean.TRUE.equals(form.getSubmitReview()) ? "PENDING" : "DRAFT");
        tool.setEnabledStatus("DISABLED");
        tool.setOnlineStatus("ONLINE");
        tool.setSchemaSyncRequired(false);
        tool.setTotalCallCount(0L);
        tool.setCreateUserId(employee.getEmployeeId());
        tool.setUpdateUserId(employee.getEmployeeId());
        tool.setCreateTime(LocalDateTime.now());
        tool.setUpdateTime(LocalDateTime.now());
        toolDao.insert(tool);
        recordSync(tool, version, "CREATE", employee.getEmployeeId());
        return toView(tool);
    }

    /** 手动同步新的 API 已发布版本；不改变任何助手绑定。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncPlatform(Long toolId) {
        AiTool tool = requireManageable(toolId);
        if (!PLATFORM_API.equals(tool.getSourceType())) {
            throw new IllegalArgumentException("只有平台 API 工具支持 Schema 同步");
        }
        OpenApiEntity api = requireManageableApi(tool.getOpenApiId());
        OpenApiVersionEntity version = openApiManageService.resolvePublishedVersion(api);
        if (version == null) {
            throw new IllegalArgumentException("API 当前没有已发布版本");
        }
        if (Objects.equals(version.getVersionId(), tool.getSourceApiVersionId())) {
            return toView(tool);
        }
        AiToolSchemaService.SchemaPair schemas = schemaService.generate(version.getVersionId());
        tool.setSourceApiVersionId(version.getVersionId());
        tool.setHttpMethod(version.getRequestMethod());
        tool.setContentType(StringUtils.defaultIfBlank(version.getContentType(), "application/json"));
        tool.setTimeoutSeconds(Objects.requireNonNullElse(version.getTimeoutSeconds(), 30));
        tool.setInputSchema(schemas.inputSchema());
        tool.setOutputSchema(schemas.outputSchema());
        tool.setSchemaSyncRequired(false);
        tool.setAuditStatus("PENDING");
        tool.setAuditRemark(null);
        tool.setEnabledStatus("DISABLED");
        tool.setLastTestStatus(null);
        tool.setLastTestMessage("API 版本已同步，请重新测试并审核后启用");
        tool.setLastTestTime(null);
        tool.setUpdateUserId(applicationDataScopeService.requireEmployee().getEmployeeId());
        tool.setUpdateTime(LocalDateTime.now());
        toolDao.updateById(tool);
        recordSync(tool, version, "MANUAL", tool.getUpdateUserId());
        return toView(tool);
    }

    /** 管理端手工登记第三方 HTTP 工具。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveExternal(AiToolForms.AdminExternalSave form) {
        ApplicationEntity application = requireManageableApplication(form.getApplicationId());
        return toView(createExternal(application.getApplicationId(), form));
    }

    /** 开放接口按 Access Token 所属应用登记第三方 HTTP 工具。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> registerExternal(Long applicationId, AiToolForms.ExternalSave form) {
        requireApplication(applicationId);
        return toView(createExternal(applicationId, form));
    }

    /** 开放接口批量登记第三方 HTTP 工具。 */
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> registerExternalBatch(
            Long applicationId, AiToolForms.ExternalBatch form) {
        requireApplication(applicationId);
        Set<String> codes = new LinkedHashSet<>();
        for (AiToolForms.ExternalSave item : form.getTools()) {
            if (!codes.add(item.getToolCode())) {
                throw new IllegalArgumentException("批量请求中工具编码重复：" + item.getToolCode());
            }
            assertCodeAvailable(item.getToolCode(), null);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiToolForms.ExternalSave item : form.getTools()) {
            result.add(toView(createExternal(applicationId, item)));
        }
        return result;
    }

    /** 第三方应用更新自己的工具，更新后重新进入待审核状态。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateExternal(
            Long applicationId, String toolCode, AiToolForms.ExternalSave form) {
        AiTool tool = requireExternal(applicationId, toolCode);
        assertCodeAvailable(form.getToolCode(), tool.getToolId());
        applyExternal(form, tool);
        tool.setAuditStatus("PENDING");
        tool.setAuditRemark(null);
        tool.setEnabledStatus("DISABLED");
        tool.setOnlineStatus("UNKNOWN");
        tool.setLastTestStatus(null);
        tool.setLastTestMessage(null);
        tool.setLastTestTime(null);
        tool.setUpdateTime(LocalDateTime.now());
        toolDao.updateById(tool);
        return toView(tool);
    }

    /** 查询应用登记的第三方工具。 */
    public List<Map<String, Object>> listExternal(Long applicationId) {
        return toolDao.selectList(new LambdaQueryWrapper<AiTool>()
                        .eq(AiTool::getApplicationId, applicationId)
                        .eq(AiTool::getSourceType, EXTERNAL_HTTP)
                        .orderByDesc(AiTool::getUpdateTime))
                .stream().map(this::toView).toList();
    }

    /** 查询应用名下指定第三方 HTTP 工具的详情。 */
    public Map<String, Object> externalDetail(Long applicationId, String toolCode) {
        return toView(requireExternal(applicationId, toolCode));
    }

    /** 删除未被智能助手引用的第三方 HTTP 工具。 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteExternal(Long applicationId, String toolCode) {
        AiTool tool = requireExternal(applicationId, toolCode);
        if (assistantToolDao.selectCount(new LambdaQueryWrapper<AiToolAssistant>()
                .eq(AiToolAssistant::getToolId, tool.getToolId())) > 0) {
            throw new IllegalArgumentException("工具已关联智能助手，请先解除关联");
        }
        toolDao.deleteById(tool.getToolId());
    }

    /** 上线或下线应用名下已经审核通过的第三方 HTTP 工具。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> externalStatus(Long applicationId, String toolCode, boolean online) {
        AiTool tool = requireExternal(applicationId, toolCode);
        if (online && !APPROVED.equals(tool.getAuditStatus())) {
            throw new IllegalArgumentException("工具审核通过后才能上线");
        }
        tool.setEnabledStatus(online ? ENABLED : "DISABLED");
        tool.setOnlineStatus(online ? "ONLINE" : "OFFLINE");
        tool.setUpdateTime(LocalDateTime.now());
        toolDao.updateById(tool);
        return toView(tool);
    }

    /** 第三方应用批量上报工具在线状态。 */
    @Transactional(rollbackFor = Exception.class)
    public int heartbeat(Long applicationId, AiToolForms.Heartbeat form) {
        String status = StringUtils.defaultIfBlank(form.getStatus(), "ONLINE");
        if (!Set.of("ONLINE", "OFFLINE", "ABNORMAL").contains(status)) {
            throw new IllegalArgumentException("心跳状态不支持");
        }
        LambdaQueryWrapper<AiTool> wrapper = new LambdaQueryWrapper<AiTool>()
                .eq(AiTool::getApplicationId, applicationId)
                .eq(AiTool::getSourceType, EXTERNAL_HTTP);
        if (form.getToolCodes() != null && !form.getToolCodes().isEmpty()) {
            wrapper.in(AiTool::getToolCode, form.getToolCodes());
        }
        List<AiTool> tools = toolDao.selectList(wrapper);
        for (AiTool tool : tools) {
            tool.setOnlineStatus(status);
            tool.setLastHeartbeatTime(LocalDateTime.now());
            if (StringUtils.isNotBlank(form.getMessage())) {
                tool.setLastTestMessage(form.getMessage());
            }
            tool.setUpdateTime(LocalDateTime.now());
            toolDao.updateById(tool);
        }
        return tools.size();
    }

    /** 审核工具；通过前必须完成一次成功测试，通过后仍需管理员显式启用。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> review(AiToolForms.Review form) {
        AiTool tool = requireManageable(form.getToolId());
        if ("APPROVED".equals(form.getAuditStatus())
                && Boolean.TRUE.equals(tool.getSchemaSyncRequired())) {
            throw new IllegalArgumentException("远端 Schema 已变化，请先同步工具定义");
        }
        if ("APPROVED".equals(form.getAuditStatus())
                && !"SUCCESS".equals(tool.getLastTestStatus())) {
            throw new IllegalArgumentException("审核通过前请先完成一次成功的工具测试");
        }
        if ("APPROVED".equals(form.getAuditStatus()) && !sourceAvailable(tool)) {
            throw new IllegalArgumentException("工具来源当前不可用，不能审核通过");
        }
        tool.setAuditStatus(form.getAuditStatus());
        tool.setAuditRemark(form.getRemark());
        tool.setToolType(form.getToolType());
        tool.setRiskLevel("ACTION".equals(form.getToolType())
                && "LOW".equals(form.getRiskLevel()) ? "MEDIUM" : form.getRiskLevel());
        tool.setConfirmationPolicy(resolveConfirmation(
                form.getToolType(), form.getConfirmationPolicy()));
        tool.setEnabledStatus("DISABLED");
        if ("APPROVED".equals(form.getAuditStatus()) && PLATFORM_API.equals(tool.getSourceType())) {
            tool.setOnlineStatus("ONLINE");
        }
        tool.setUpdateUserId(applicationDataScopeService.requireEmployee().getEmployeeId());
        tool.setUpdateTime(LocalDateTime.now());
        toolDao.updateById(tool);
        return toView(tool);
    }

    /** 启停已审核工具。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateStatus(AiToolForms.StatusUpdate form) {
        AiTool tool = requireManageable(form.getToolId());
        if (ENABLED.equals(form.getEnabledStatus()) && !APPROVED.equals(tool.getAuditStatus())) {
            throw new IllegalArgumentException("只有审核通过的工具可以启用");
        }
        if (ENABLED.equals(form.getEnabledStatus())
                && !"SUCCESS".equals(tool.getLastTestStatus())) {
            throw new IllegalArgumentException("启用工具前请先完成一次成功测试");
        }
        if (ENABLED.equals(form.getEnabledStatus()) && !sourceAvailable(tool)) {
            throw new IllegalArgumentException("工具来源当前不可用，不能启用");
        }
        tool.setEnabledStatus(form.getEnabledStatus());
        tool.setUpdateUserId(applicationDataScopeService.requireEmployee().getEmployeeId());
        tool.setUpdateTime(LocalDateTime.now());
        toolDao.updateById(tool);
        return toView(tool);
    }

    /** 查询助手配置页可选择的工具。 */
    public List<Map<String, Object>> availableForAssistant() {
        return toolDao.selectList(new LambdaQueryWrapper<AiTool>()
                        .eq(AiTool::getAuditStatus, APPROVED)
                        .eq(AiTool::getEnabledStatus, ENABLED)
                        .in(AiTool::getOnlineStatus, "ONLINE", "UNKNOWN")
                        .orderByAsc(AiTool::getToolName))
                .stream()
                .filter(this::canManage)
                .filter(this::sourceAvailable)
                .map(this::toView)
                .toList();
    }

    /** 查询助手当前关联工具。 */
    public List<Map<String, Object>> assistantTools(Long assistantId) {
        requireManageableAssistant(assistantId);
        return linkedToolViews(assistantId);
    }

    /** 读取助手关联工具摘要，由已经完成助手访问校验的内部业务使用。 */
    public List<Map<String, Object>> linkedToolViews(Long assistantId) {
        List<Long> ids = assistantToolDao.selectList(new LambdaQueryWrapper<AiToolAssistant>()
                        .eq(AiToolAssistant::getAssistantId, assistantId))
                .stream().map(AiToolAssistant::getToolId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return toolDao.selectBatchIds(ids).stream()
                .sorted(Comparator.comparing(AiTool::getToolName))
                .map(this::toView).toList();
    }

    /** 保存助手工具及调用安全设置。 */
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> bindAssistant(AiToolForms.AssistantBind form) {
        KnowledgeAssistant assistant = requireManageableAssistant(form.getAssistantId());
        List<Long> distinctIds = form.getToolIds().stream().distinct().toList();
        List<AiTool> tools = distinctIds.isEmpty() ? List.of() : toolDao.selectBatchIds(distinctIds);
        if (tools.size() != distinctIds.size()) {
            throw new IllegalArgumentException("部分工具不存在");
        }
        for (AiTool tool : tools) {
            if (!APPROVED.equals(tool.getAuditStatus())
                    || !ENABLED.equals(tool.getEnabledStatus())
                    || !Set.of("ONLINE", "UNKNOWN").contains(tool.getOnlineStatus())
                    || !sourceAvailable(tool)) {
                throw new IllegalArgumentException("只能关联已审核、已启用且在线的工具");
            }
            if ("ACTION".equals(tool.getToolType())
                    && !Boolean.TRUE.equals(form.getAllowActionToolFlag())) {
                throw new IllegalArgumentException("关联操作工具时必须开启“允许操作类工具”");
            }
        }
        assistantToolDao.delete(new LambdaQueryWrapper<AiToolAssistant>()
                .eq(AiToolAssistant::getAssistantId, assistant.getAssistantId()));
        for (Long toolId : distinctIds) {
            AiToolAssistant relation = new AiToolAssistant();
            relation.setAssistantId(assistant.getAssistantId());
            relation.setToolId(toolId);
            relation.setCreateTime(LocalDateTime.now());
            assistantToolDao.insert(relation);
        }
        assistant.setMaxToolCalls(Objects.requireNonNullElse(form.getMaxToolCalls(), 3));
        assistant.setToolDebugFlag(Boolean.TRUE.equals(form.getToolDebugFlag()));
        assistant.setAllowActionToolFlag(Boolean.TRUE.equals(form.getAllowActionToolFlag()));
        assistant.setUpdateTime(LocalDateTime.now());
        assistantDao.updateById(assistant);
        return tools.stream().map(this::toView).toList();
    }

    /** 模型调用前读取助手已经获准使用的工具。 */
    public List<AiTool> enabledToolsForAssistant(Long assistantId) {
        List<Long> ids = assistantToolDao.selectList(new LambdaQueryWrapper<AiToolAssistant>()
                        .eq(AiToolAssistant::getAssistantId, assistantId))
                .stream().map(AiToolAssistant::getToolId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return toolDao.selectList(new LambdaQueryWrapper<AiTool>()
                .in(AiTool::getToolId, ids)
                .eq(AiTool::getAuditStatus, APPROVED)
                .eq(AiTool::getEnabledStatus, ENABLED)
                .in(AiTool::getOnlineStatus, "ONLINE", "UNKNOWN"))
                .stream().filter(this::sourceAvailable).toList();
    }

    /** 读取当前员工有权管理的工具，不满足归属条件时拒绝访问。 */
    public AiTool requireManageable(Long toolId) {
        AiTool tool = toolDao.selectById(toolId);
        if (tool == null || !canManage(tool)) {
            throw new IllegalArgumentException("工具不存在或无权管理");
        }
        return tool;
    }

    /** 读取审核通过、已启用且在线的工具，供实际调用前做最终状态校验。 */
    public AiTool requireEnabled(Long toolId) {
        AiTool tool = toolDao.selectById(toolId);
        if (tool == null || !APPROVED.equals(tool.getAuditStatus())
                || !ENABLED.equals(tool.getEnabledStatus())
                || !Set.of("ONLINE", "UNKNOWN").contains(tool.getOnlineStatus())
                || !sourceAvailable(tool)) {
            throw new IllegalArgumentException("工具未审核、未启用或不在线");
        }
        return tool;
    }

    /** 读取助手仍然获准使用的可调用工具，防止解绑或停用后的并发调用。 */
    public AiTool requireEnabledForAssistant(Long toolId, Long assistantId) {
        AiTool tool = requireEnabled(toolId);
        if (assistantId == null || assistantToolDao.selectCount(
                new LambdaQueryWrapper<AiToolAssistant>()
                        .eq(AiToolAssistant::getAssistantId, assistantId)
                        .eq(AiToolAssistant::getToolId, toolId)) == 0) {
            throw new IllegalArgumentException("工具已不再关联当前智能助手");
        }
        return tool;
    }

    /** 测试完成后刷新工具测试和在线状态。 */
    @Transactional(rollbackFor = Exception.class)
    public void recordTest(Long toolId, boolean success, String message) {
        AiTool tool = toolDao.selectById(toolId);
        if (tool == null) {
            return;
        }
        tool.setLastTestStatus(success ? "SUCCESS" : "FAILED");
        tool.setLastTestMessage(StringUtils.abbreviate(message, 1000));
        tool.setLastTestTime(LocalDateTime.now());
        tool.setOnlineStatus(success ? "ONLINE" : "ABNORMAL");
        tool.setUpdateTime(LocalDateTime.now());
        toolDao.updateById(tool);
    }

    /** 创建一条第三方 HTTP 工具记录并初始化审核、启停和在线状态。 */
    private AiTool createExternal(Long applicationId, AiToolForms.ExternalSave form) {
        assertCodeAvailable(form.getToolCode(), null);
        AiTool tool = new AiTool();
        tool.setApplicationId(applicationId);
        tool.setSourceType(EXTERNAL_HTTP);
        applyExternal(form, tool);
        tool.setAuditStatus("PENDING");
        tool.setEnabledStatus("DISABLED");
        tool.setOnlineStatus("UNKNOWN");
        tool.setSchemaSyncRequired(false);
        tool.setTotalCallCount(0L);
        tool.setCreateTime(LocalDateTime.now());
        tool.setUpdateTime(LocalDateTime.now());
        toolDao.insert(tool);
        return tool;
    }

    /** 将第三方登记表单中的可编辑配置写入工具实体。 */
    private void applyExternal(AiToolForms.ExternalSave form, AiTool tool) {
        tool.setToolCode(form.getToolCode());
        tool.setToolName(form.getToolName());
        tool.setDescription(form.getDescription());
        tool.setToolType(form.getToolType());
        tool.setCallbackUrl(form.getCallbackUrl());
        tool.setHttpMethod(form.getHttpMethod());
        tool.setContentType(StringUtils.defaultIfBlank(form.getContentType(), "application/json"));
        tool.setTimeoutSeconds(Objects.requireNonNullElse(form.getTimeoutSeconds(), 30));
        tool.setRiskLevel(form.getRiskLevel());
        tool.setConfirmationPolicy(Boolean.TRUE.equals(form.getRequireConfirmation())
                || "ACTION".equals(form.getToolType()) ? "REQUIRED" : "AUTO");
        tool.setInputSchema(schemaService.normalizeSchema(form.getInputSchema(), true));
        tool.setOutputSchema(schemaService.normalizeSchema(form.getOutputSchema(), false));
    }

    /** 校验工具编码在平台范围内唯一，更新时排除当前工具。 */
    private void assertCodeAvailable(String toolCode, Long excludedToolId) {
        long count = toolDao.selectCount(new LambdaQueryWrapper<AiTool>()
                .eq(AiTool::getToolCode, toolCode)
                .ne(excludedToolId != null, AiTool::getToolId, excludedToolId));
        if (count > 0) {
            throw new IllegalArgumentException("工具编码已存在");
        }
    }

    /** 按应用和工具编码读取第三方工具，防止跨应用访问。 */
    private AiTool requireExternal(Long applicationId, String toolCode) {
        AiTool tool = toolDao.selectOne(new LambdaQueryWrapper<AiTool>()
                .eq(AiTool::getApplicationId, applicationId)
                .eq(AiTool::getSourceType, EXTERNAL_HTTP)
                .eq(AiTool::getToolCode, toolCode)
                .last("limit 1"));
        if (tool == null) {
            throw new IllegalArgumentException("工具不存在或不属于当前应用");
        }
        return tool;
    }

    /** 读取当前员工有权维护的平台 API。 */
    private OpenApiEntity requireManageableApi(Long openApiId) {
        OpenApiEntity api = openApiDao.selectById(openApiId);
        RequestEmployee employee = applicationDataScopeService.requireEmployee();
        if (api == null || !applicationDataScopeService.isPlatformAdministrator()
                && !Objects.equals(api.getCreateUserId(), employee.getEmployeeId())) {
            throw new IllegalArgumentException("API 不存在或无权管理");
        }
        return api;
    }

    /** 读取当前员工有权维护的应用。 */
    private ApplicationEntity requireManageableApplication(Long applicationId) {
        ApplicationEntity application = requireApplication(applicationId);
        if (!applicationDataScopeService.canManage(application)) {
            throw new IllegalArgumentException("应用不存在或无权管理");
        }
        return application;
    }

    /** 读取应用基础信息，不附加管理范围判断。 */
    private ApplicationEntity requireApplication(Long applicationId) {
        ApplicationEntity application = applicationDao.selectById(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("应用不存在");
        }
        return application;
    }

    /** 读取当前员工有权维护的智能助手。 */
    private KnowledgeAssistant requireManageableAssistant(Long assistantId) {
        KnowledgeAssistant assistant = assistantDao.selectById(assistantId);
        RequestEmployee employee = applicationDataScopeService.requireEmployee();
        if (assistant == null || !applicationDataScopeService.isPlatformAdministrator()
                && !Objects.equals(assistant.getOwnerId(), employee.getEmployeeId())) {
            throw new IllegalArgumentException("智能助手不存在或无权管理");
        }
        return assistant;
    }

    /** 判断当前员工是否可以管理指定工具及其来源对象。 */
    private boolean canManage(AiTool tool) {
        if (applicationDataScopeService.isPlatformAdministrator()) {
            return true;
        }
        RequestEmployee employee = applicationDataScopeService.requireEmployee();
        if (PLATFORM_API.equals(tool.getSourceType())) {
            OpenApiEntity api = openApiDao.selectById(tool.getOpenApiId());
            return api != null && Objects.equals(api.getCreateUserId(), employee.getEmployeeId());
        }
        ApplicationEntity application = applicationDao.selectById(tool.getApplicationId());
        return application != null && Objects.equals(
                application.getCreateUserId(), employee.getEmployeeId());
    }

    /** 判断工具对应的平台 API、标准 MCP Server 或第三方应用是否仍然可用。 */
    private boolean sourceAvailable(AiTool tool) {
        if (Boolean.TRUE.equals(tool.getSchemaSyncRequired())) {
            return false;
        }
        if (PLATFORM_API.equals(tool.getSourceType())) {
            OpenApiEntity api = openApiDao.selectById(tool.getOpenApiId());
            return api != null && Objects.equals(api.getStatus(), 4);
        }
        if (STANDARD_MCP.equals(tool.getSourceType())) {
            McpServer server = mcpServerDao.selectById(tool.getMcpServerId());
            return server != null
                    && Boolean.TRUE.equals(server.getEnabledFlag())
                    && "ONLINE".equals(server.getOnlineStatus());
        }
        ApplicationEntity application = applicationDao.selectById(tool.getApplicationId());
        return application != null && (Objects.equals(application.getOnlineStatus(), 2)
                || application.getOnlineStatus() == null
                && Objects.equals(application.getListingStatus(), 2));
    }

    /** 将工具实体转换为页面视图，并补充来源、版本和助手数量信息。 */
    private Map<String, Object> toView(AiTool tool) {
        Map<String, Object> result = objectMapper.convertValue(tool, LinkedHashMap.class);
        if (PLATFORM_API.equals(tool.getSourceType())) {
            OpenApiEntity api = openApiDao.selectById(tool.getOpenApiId());
            OpenApiVersionEntity sourceVersion = versionDao.selectById(tool.getSourceApiVersionId());
            OpenApiVersionEntity latestVersion = api == null ? null
                    : openApiManageService.resolvePublishedVersion(api);
            result.put("sourceName", api == null ? null : api.getApiName());
            result.put("sourceCode", api == null ? null : api.getApiCode());
            result.put("sourceVersion", sourceVersion == null ? null : sourceVersion.getVersionNo());
            result.put("latestVersion", latestVersion == null ? null : latestVersion.getVersionNo());
            result.put("syncAvailable", latestVersion != null
                    && !Objects.equals(latestVersion.getVersionId(), tool.getSourceApiVersionId()));
        } else if (STANDARD_MCP.equals(tool.getSourceType())) {
            McpServer server = mcpServerDao.selectById(tool.getMcpServerId());
            result.put("sourceName", server == null ? null : server.getServerName());
            result.put("sourceCode", server == null ? null : server.getServerCode());
            result.put("sourceVersion", server == null ? null : server.getServerVersion());
            result.put("latestVersion", null);
            result.put("syncAvailable", false);
            result.put("endpointUrl", server == null ? null : server.getEndpointUrl());
            result.put("authType", server == null ? null : server.getAuthType());
            result.put("protocolVersion", server == null ? null : server.getProtocolVersion());
        } else {
            ApplicationEntity application = applicationDao.selectById(tool.getApplicationId());
            result.put("sourceName", application == null ? null : application.getApplicationName());
            result.put("sourceCode", application == null ? null : application.getApplicationCode());
            result.put("sourceVersion", null);
            result.put("latestVersion", null);
            result.put("syncAvailable", false);
        }
        result.put("assistantCount", assistantToolDao.selectCount(
                new LambdaQueryWrapper<AiToolAssistant>()
                        .eq(AiToolAssistant::getToolId, tool.getToolId())));
        return result;
    }

    /** 查询指定工具已经关联的智能助手摘要。 */
    private List<Map<String, Object>> assistantViews(Long toolId) {
        List<Long> ids = assistantToolDao.selectList(new LambdaQueryWrapper<AiToolAssistant>()
                        .eq(AiToolAssistant::getToolId, toolId))
                .stream().map(AiToolAssistant::getAssistantId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return assistantDao.selectBatchIds(ids).stream().map(assistant -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("assistantId", assistant.getAssistantId());
            item.put("assistantName", assistant.getAssistantName());
            item.put("enabledFlag", assistant.getEnabledFlag());
            return item;
        }).toList();
    }

    /** 记录平台 API 版本与工具 Schema 的显式同步历史。 */
    private void recordSync(AiTool tool, OpenApiVersionEntity version,
                            String syncType, Long operatorId) {
        AiToolSchemaSync record = new AiToolSchemaSync();
        record.setToolId(tool.getToolId());
        record.setOpenApiId(tool.getOpenApiId());
        record.setApiVersionId(version.getVersionId());
        record.setApiVersionNo(version.getVersionNo());
        record.setSyncType(syncType);
        record.setOperatorId(operatorId);
        record.setCreateTime(LocalDateTime.now());
        schemaSyncDao.insert(record);
    }

    /** 操作类工具始终强制确认，查询类工具沿用页面选择的策略。 */
    private String resolveConfirmation(String toolType, String policy) {
        return "ACTION".equals(toolType) ? "REQUIRED" : policy;
    }

    /** 统计指定来源的工具数量。 */
    private long count(List<AiTool> tools, String sourceType) {
        return tools.stream().filter(item -> sourceType.equals(item.getSourceType())).count();
    }

    /** 按项目统一 PageResult 结构组装内存分页结果。 */
    private <T> PageResult<T> pageResult(Long pageNum, Long pageSize, long total, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setPages(total == 0 ? 0 : (total + pageSize - 1) / pageSize);
        result.setList(list);
        result.setEmptyFlag(list.isEmpty());
        return result;
    }
}
