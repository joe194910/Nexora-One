package com.nexoraone.admin.module.business.openapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexoraone.admin.module.business.application.dao.OpenApiDao;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import com.nexoraone.admin.module.business.application.service.ApplicationDataScopeService;
import com.nexoraone.admin.module.business.mcp.dao.AiToolMappers;
import com.nexoraone.admin.module.business.mcp.domain.AiTool;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiEnvironmentDao;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiErrorCodeDao;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiExampleDao;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiParameterDao;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiVersionDao;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiEnvironmentEntity;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiErrorCodeEntity;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiExampleEntity;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiParameterEntity;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiVersionEntity;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiBasicSaveForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiExampleSaveForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiParameterSaveForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiQueryForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiStatusForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiVersionCreateForm;
import com.nexoraone.admin.module.business.openapi.domain.vo.OpenApiVersionVO;
import com.nexoraone.admin.module.system.login.domain.RequestEmployee;
import com.nexoraone.admin.util.AdminRequestUtil;
import com.nexoraone.base.common.domain.PageResult;
import com.nexoraone.base.common.domain.ResponseDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * API开放平台管理服务。
 */
@Slf4j
@Service
public class OpenApiManageService {

    private static final Set<String> HTTP_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH");
    @Resource
    private OpenApiDao openApiDao;
    @Resource
    private OpenApiVersionDao versionDao;
    @Resource
    private OpenApiEnvironmentDao environmentDao;
    @Resource
    private OpenApiParameterDao parameterDao;
    @Resource
    private OpenApiExampleDao exampleDao;
    @Resource
    private OpenApiErrorCodeDao errorCodeDao;
    @Resource
    private ApplicationDataScopeService applicationDataScopeService;
    @Resource
    private AiToolMappers.ToolDao aiToolDao;

    /**
     * 分页查询API管理列表。
     */
    public ResponseDTO<PageResult<OpenApiEntity>> query(OpenApiQueryForm form) {
        LambdaQueryWrapper<OpenApiEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(form.getSearchWord())) {
            wrapper.and(query -> query.like(OpenApiEntity::getApiName, form.getSearchWord())
                    .or().like(OpenApiEntity::getApiCode, form.getSearchWord()));
        }
        wrapper.eq(StringUtils.isNotBlank(form.getCategoryName()),
                        OpenApiEntity::getCategoryName, form.getCategoryName())
                .eq(form.getStatus() != null, OpenApiEntity::getStatus, form.getStatus())
                .orderByDesc(OpenApiEntity::getUpdateTime)
                .orderByDesc(OpenApiEntity::getOpenApiId);
        if (StringUtils.isNotBlank(form.getRequestMethod())) {
            wrapper.eq(OpenApiEntity::getRequestMethod, StringUtils.upperCase(form.getRequestMethod()));
        }
        applyAiToolStatusFilter(wrapper, form.getAiToolStatus());
        applyCreatorScope(wrapper);
        Page<OpenApiEntity> page = openApiDao.selectPage(
                new Page<>(form.getPageNum(), form.getPageSize(), !Boolean.FALSE.equals(form.getSearchCount())), wrapper);
        for (OpenApiEntity api : page.getRecords()) {
            OpenApiVersionEntity currentVersion = resolveCurrentVersion(api);
            api.setCurrentVersionStatus(currentVersion == null ? null : currentVersion.getStatus());
            api.setCurrentVersionNo(currentVersion == null ? null : currentVersion.getVersionNo());
        }
        enrichAiToolRelations(page.getRecords());
        PageResult<OpenApiEntity> result = new PageResult<>();
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setList(page.getRecords());
        result.setEmptyFlag(page.getRecords().isEmpty());
        return ResponseDTO.ok(result);
    }

    /**
     * API列表按关联工具状态过滤。未发布表示尚未建立平台API工具记录。
     */
    private void applyAiToolStatusFilter(LambdaQueryWrapper<OpenApiEntity> wrapper, String status) {
        if (StringUtils.isBlank(status)) {
            return;
        }
        List<AiTool> platformTools = aiToolDao.selectList(new LambdaQueryWrapper<AiTool>()
                .eq(AiTool::getSourceType, "PLATFORM_API"));
        Set<Long> allLinkedIds = platformTools.stream()
                .map(AiTool::getOpenApiId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if ("UNPUBLISHED".equals(status)) {
            if (!allLinkedIds.isEmpty()) {
                wrapper.notIn(OpenApiEntity::getOpenApiId, allLinkedIds);
            }
            return;
        }
        Set<Long> matchedIds = platformTools.stream()
                .filter(tool -> status.equals(tool.getAuditStatus()))
                .map(AiTool::getOpenApiId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (matchedIds.isEmpty()) {
            wrapper.apply("1 = 0");
        } else {
            wrapper.in(OpenApiEntity::getOpenApiId, matchedIds);
        }
    }

    /**
     * 补充列表所需的轻量 AI 工具关系，不改变现有 API 分页结构。
     */
    private void enrichAiToolRelations(List<OpenApiEntity> apiList) {
        if (apiList.isEmpty()) {
            return;
        }
        Map<Long, AiTool> tools = aiToolDao.selectList(new LambdaQueryWrapper<AiTool>()
                        .eq(AiTool::getSourceType, "PLATFORM_API")
                        .in(AiTool::getOpenApiId, apiList.stream()
                                .map(OpenApiEntity::getOpenApiId).toList()))
                .stream()
                .collect(Collectors.toMap(AiTool::getOpenApiId, Function.identity(),
                        (left, right) -> left));
        for (OpenApiEntity api : apiList) {
            AiTool tool = tools.get(api.getOpenApiId());
            if (tool == null) {
                api.setAiToolStatus("UNPUBLISHED");
                api.setAiToolSyncAvailable(false);
                continue;
            }
            api.setAiToolId(tool.getToolId());
            api.setAiToolCode(tool.getToolCode());
            api.setAiToolStatus(tool.getAuditStatus());
            OpenApiVersionEntity publishedVersion = resolvePublishedVersion(api);
            api.setAiToolSyncAvailable(publishedVersion != null
                    && !Objects.equals(publishedVersion.getVersionId(), tool.getSourceApiVersionId()));
        }
    }

    /**
     * 查询API管理看板数量汇总。
     */
    public ResponseDTO<Map<String, Long>> summary() {
        LambdaQueryWrapper<OpenApiEntity> wrapper = new LambdaQueryWrapper<>();
        applyCreatorScope(wrapper);
        List<OpenApiEntity> apiList = openApiDao.selectList(wrapper);

        long publishedCount = 0L;
        long draftCount = 0L;
        long disabledCount = 0L;
        for (OpenApiEntity api : apiList) {
            if (Objects.equals(api.getStatus(), 4)) {
                publishedCount++;
            }
            if (Objects.equals(api.getStatus(), 5)) {
                disabledCount++;
            }
            OpenApiVersionEntity currentVersion = resolveCurrentVersion(api);
            if (currentVersion != null && Objects.equals(currentVersion.getStatus(), 1)) {
                draftCount++;
            }
        }

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("total", (long) apiList.size());
        result.put("published", publishedCount);
        result.put("draft", draftCount);
        result.put("disabled", disabledCount);
        return ResponseDTO.ok(result);
    }

    /**
     * 查询现有记录使用的API分类。
     */
    public ResponseDTO<List<String>> categories() {
        LambdaQueryWrapper<OpenApiEntity> wrapper = new LambdaQueryWrapper<OpenApiEntity>()
                .select(OpenApiEntity::getCategoryName)
                .isNotNull(OpenApiEntity::getCategoryName);
        applyCreatorScope(wrapper);
        wrapper.orderByAsc(OpenApiEntity::getCategoryName);
        List<String> categories = openApiDao.selectList(wrapper)
                .stream()
                .map(OpenApiEntity::getCategoryName)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        return ResponseDTO.ok(categories);
    }

    /**
     * 校验API编码是否可用。
     */
    public ResponseDTO<Boolean> checkCode(String apiCode, Long openApiId) {
        if (!isValidApiCode(apiCode)) {
            return ResponseDTO.ok(false);
        }
        long count = openApiDao.selectCount(new LambdaQueryWrapper<OpenApiEntity>()
                .eq(OpenApiEntity::getApiCode, apiCode)
                .ne(openApiId != null, OpenApiEntity::getOpenApiId, openApiId));
        return ResponseDTO.ok(count == 0);
    }

    /**
     * 创建API及其首个可编辑版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Map<String, Long>> create(OpenApiBasicSaveForm form) {
        ResponseDTO<String> validation = validateBasic(form, null);
        if (!validation.getOk()) {
            return ResponseDTO.userErrorParam(validation.getMsg());
        }
        RequestEmployee employee = applicationDataScopeService.requireEmployee();
        OpenApiEntity api = new OpenApiEntity();
        copyMaster(form, api);
        api.setStatus(1);
        api.setWorkflowStep(1);
        api.setTodayCallCount(0L);
        api.setTotalCallCount(0L);
        api.setEnabledFlag(false);
        api.setSort(0);
        api.setCreateUserId(employee.getEmployeeId());
        api.setCreateUserName(employee.getActualName());
        api.setUpdateUserId(employee.getEmployeeId());
        api.setUpdateUserName(employee.getActualName());
        openApiDao.insert(api);

        OpenApiVersionEntity version = new OpenApiVersionEntity();
        copyVersion(form, version);
        version.setOpenApiId(api.getOpenApiId());
        version.setUnifiedResponseFlag(true);
        version.setDataMaskingFlag(true);
        version.setStatus(1);
        version.setLockedFlag(false);
        version.setCreateUserId(employee.getEmployeeId());
        version.setCreateUserName(employee.getActualName());
        version.setUpdateUserId(employee.getEmployeeId());
        version.setUpdateUserName(employee.getActualName());
        versionDao.insert(version);

        api.setCurrentVersionId(version.getVersionId());
        openApiDao.updateById(api);
        replaceEnvironments(version.getVersionId(), form.getEnvironments());

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("openApiId", api.getOpenApiId());
        result.put("versionId", version.getVersionId());
        return ResponseDTO.ok(result);
    }

    /**
     * 基于当前线上版本创建一个内容完整、互不影响的新草稿版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Map<String, Long>> createVersion(OpenApiVersionCreateForm form) {
        OpenApiEntity api = openApiDao.selectById(form.getOpenApiId());
        if (api == null || !canManage(api)) {
            return ResponseDTO.userErrorParam("API不存在或无权访问");
        }
        if (!Objects.equals(api.getStatus(), 4) && !Objects.equals(api.getStatus(), 5)) {
            return ResponseDTO.userErrorParam("只有已上架或已停用的API可以创建新版本");
        }
        OpenApiVersionEntity publishedVersion = resolvePublishedVersion(api);
        if (publishedVersion == null) {
            return ResponseDTO.userErrorParam("API线上版本不存在，无法创建新版本");
        }
        if (!Objects.equals(api.getCurrentVersionId(), publishedVersion.getVersionId())) {
            return ResponseDTO.userErrorParam("当前API已有待编辑或待审核的新版本，请先处理现有版本");
        }
        String versionNo = StringUtils.trim(form.getVersionNo());
        if (StringUtils.isBlank(versionNo) || !versionNo.matches("^v?\\d+\\.\\d+\\.\\d+$")) {
            return ResponseDTO.userErrorParam("接口版本需使用v1.0.0格式");
        }
        long versionCount = versionDao.selectCount(new LambdaQueryWrapper<OpenApiVersionEntity>()
                .eq(OpenApiVersionEntity::getOpenApiId, api.getOpenApiId())
                .eq(OpenApiVersionEntity::getVersionNo, versionNo));
        if (versionCount > 0) {
            return ResponseDTO.userErrorParam("该API版本号已存在");
        }
        long routeCount = versionDao.selectCount(new LambdaQueryWrapper<OpenApiVersionEntity>()
                .eq(OpenApiVersionEntity::getRequestMethod, publishedVersion.getRequestMethod())
                .eq(OpenApiVersionEntity::getGatewayPath, publishedVersion.getGatewayPath())
                .eq(OpenApiVersionEntity::getVersionNo, versionNo));
        if (routeCount > 0) {
            return ResponseDTO.userErrorParam("该网关路径和版本已存在");
        }

        RequestEmployee employee = applicationDataScopeService.requireEmployee();
        OpenApiVersionEntity newVersion = copyNewVersion(
                publishedVersion, versionNo, employee);
        versionDao.insert(newVersion);
        copyVersionDetails(publishedVersion.getVersionId(), newVersion.getVersionId());

        api.setCurrentVersionId(newVersion.getVersionId());
        api.setWorkflowStep(Objects.requireNonNullElse(api.getWorkflowStep(), 4));
        api.setUpdateUserId(employee.getEmployeeId());
        api.setUpdateUserName(employee.getActualName());
        openApiDao.updateById(api);

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("openApiId", api.getOpenApiId());
        result.put("versionId", newVersion.getVersionId());
        return ResponseDTO.ok(result);
    }

    /**
     * 查询当前用户有权管理的API版本记录。
     */
    public ResponseDTO<List<OpenApiVersionVO>> versionList(Long openApiId) {
        OpenApiEntity api = openApiDao.selectById(openApiId);
        if (api == null || !canManage(api)) {
            return ResponseDTO.userErrorParam("API不存在或无权访问");
        }
        List<OpenApiVersionVO> result = versionDao.selectList(new LambdaQueryWrapper<OpenApiVersionEntity>()
                        .eq(OpenApiVersionEntity::getOpenApiId, openApiId)
                        .orderByDesc(OpenApiVersionEntity::getVersionId))
                .stream()
                .map(version -> buildVersionVO(api, version))
                .toList();
        return ResponseDTO.ok(result);
    }

    /**
     * 查询当前用户有权管理的指定API版本配置。
     */
    public ResponseDTO<Map<String, Object>> versionDetail(Long openApiId, Long versionId) {
        OpenApiEntity api = openApiDao.selectById(openApiId);
        if (api == null || !canManage(api)) {
            return ResponseDTO.userErrorParam("API不存在或无权访问");
        }
        OpenApiVersionEntity version = versionDao.selectById(versionId);
        if (version == null || !Objects.equals(version.getOpenApiId(), openApiId)) {
            return ResponseDTO.userErrorParam("API版本不存在");
        }
        return buildDetail(api, version);
    }

    /**
     * 更新API基本信息和当前可编辑版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> updateBasic(OpenApiBasicSaveForm form) {
        if (form.getOpenApiId() == null || form.getVersionId() == null) {
            return ResponseDTO.userErrorParam("API主键和版本主键不能为空");
        }
        OpenApiEntity api = openApiDao.selectById(form.getOpenApiId());
        OpenApiVersionEntity version = versionDao.selectById(form.getVersionId());
        ResponseDTO<String> editable = checkEditable(api, version);
        if (!editable.getOk()) {
            return editable;
        }
        ResponseDTO<String> validation = validateBasic(form, api);
        if (!validation.getOk()) {
            return validation;
        }
        copyMaster(form, api);
        copyVersion(form, version);
        if (api.getPublishedVersionId() != null) {
            syncPublishedFields(api, resolvePublishedVersion(api));
        }
        RequestEmployee employee = getRequestEmployee();
        api.setUpdateUserId(employee == null ? null : employee.getEmployeeId());
        api.setUpdateUserName(employee == null ? null : employee.getActualName());
        version.setUpdateUserId(employee == null ? null : employee.getEmployeeId());
        version.setUpdateUserName(employee == null ? null : employee.getActualName());
        openApiDao.updateById(api);
        versionDao.updateById(version);
        replaceEnvironments(version.getVersionId(), form.getEnvironments());
        return ResponseDTO.ok();
    }

    /**
     * 替换指定API版本的全部请求或响应参数。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> saveParameters(OpenApiParameterSaveForm form) {
        OpenApiEntity api = openApiDao.selectById(form.getOpenApiId());
        OpenApiVersionEntity version = versionDao.selectById(form.getVersionId());
        ResponseDTO<String> editable = checkEditable(api, version);
        if (!editable.getOk()) {
            return editable;
        }
        parameterDao.delete(new LambdaQueryWrapper<OpenApiParameterEntity>()
                .eq(OpenApiParameterEntity::getVersionId, form.getVersionId())
                .eq(OpenApiParameterEntity::getDirection, form.getDirection()));
        Map<String, Long> rowIdMap = new LinkedHashMap<>();
        List<OpenApiParameterSaveForm.ParameterItem> rows =
                form.getParameters() == null ? List.of() : form.getParameters();
        for (int index = 0; index < rows.size(); index++) {
            OpenApiParameterSaveForm.ParameterItem item = rows.get(index);
            if (StringUtils.isNotBlank(item.getParentRowKey())
                    && !rowIdMap.containsKey(item.getParentRowKey())) {
                return ResponseDTO.userErrorParam("父参数必须位于子参数之前");
            }
            OpenApiParameterEntity parameter = new OpenApiParameterEntity();
            parameter.setVersionId(form.getVersionId());
            parameter.setDirection(form.getDirection());
            parameter.setLocation(item.getLocation().toLowerCase());
            parameter.setParentId(rowIdMap.get(item.getParentRowKey()));
            parameter.setParameterName(item.getParameterName());
            parameter.setChineseName(item.getChineseName());
            parameter.setDataType(item.getDataType());
            parameter.setRequiredFlag(Boolean.TRUE.equals(item.getRequiredFlag()));
            parameter.setNullableFlag(!Boolean.FALSE.equals(item.getNullableFlag()));
            parameter.setDefaultValue(item.getDefaultValue());
            parameter.setExampleValue(item.getExampleValue());
            parameter.setValidationRule(item.getValidationRule());
            parameter.setDescription(item.getDescription());
            parameter.setMaskingFlag(Boolean.TRUE.equals(item.getMaskingFlag()));
            parameter.setSort(item.getSort() == null ? index : item.getSort());
            parameterDao.insert(parameter);
            if (StringUtils.isNotBlank(item.getRowKey())) {
                rowIdMap.put(item.getRowKey(), parameter.getParameterId());
            }
        }
        if (form.getUnifiedResponseFlag() != null) {
            version.setUnifiedResponseFlag(form.getUnifiedResponseFlag());
        }
        if (form.getDataMaskingFlag() != null) {
            version.setDataMaskingFlag(form.getDataMaskingFlag());
        }
        versionDao.updateById(version);
        api.setWorkflowStep(Math.max(Objects.requireNonNullElse(api.getWorkflowStep(), 1),
                Objects.equals(form.getDirection(), 1) ? 2 : 3));
        openApiDao.updateById(api);
        return ResponseDTO.ok();
    }

    /**
     * 替换API请求示例、响应示例和业务错误码。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> saveExamples(OpenApiExampleSaveForm form) {
        OpenApiEntity api = openApiDao.selectById(form.getOpenApiId());
        OpenApiVersionEntity version = versionDao.selectById(form.getVersionId());
        ResponseDTO<String> editable = checkEditable(api, version);
        if (!editable.getOk()) {
            return editable;
        }
        exampleDao.delete(new LambdaQueryWrapper<OpenApiExampleEntity>()
                .eq(OpenApiExampleEntity::getVersionId, form.getVersionId()));
        errorCodeDao.delete(new LambdaQueryWrapper<OpenApiErrorCodeEntity>()
                .eq(OpenApiErrorCodeEntity::getVersionId, form.getVersionId()));

        List<OpenApiExampleSaveForm.ExampleItem> examples =
                form.getExamples() == null ? List.of() : form.getExamples();
        for (int index = 0; index < examples.size(); index++) {
            OpenApiExampleSaveForm.ExampleItem item = examples.get(index);
            OpenApiExampleEntity entity = new OpenApiExampleEntity();
            entity.setVersionId(form.getVersionId());
            entity.setExampleType(item.getExampleType());
            entity.setExampleName(item.getExampleName());
            entity.setContent(item.getContent());
            entity.setSort(item.getSort() == null ? index : item.getSort());
            exampleDao.insert(entity);
        }
        List<OpenApiExampleSaveForm.ErrorCodeItem> errorCodes =
                form.getErrorCodes() == null ? List.of() : form.getErrorCodes();
        for (int index = 0; index < errorCodes.size(); index++) {
            OpenApiExampleSaveForm.ErrorCodeItem item = errorCodes.get(index);
            OpenApiErrorCodeEntity entity = new OpenApiErrorCodeEntity();
            entity.setVersionId(form.getVersionId());
            entity.setHttpStatus(item.getHttpStatus());
            entity.setBusinessCode(item.getBusinessCode());
            entity.setErrorMessage(item.getErrorMessage());
            entity.setTriggerCondition(item.getTriggerCondition());
            entity.setHandlingAdvice(item.getHandlingAdvice());
            entity.setSort(item.getSort() == null ? index : item.getSort());
            errorCodeDao.insert(entity);
        }
        version.setChangeLog(form.getChangeLog());
        versionDao.updateById(version);
        api.setWorkflowStep(Math.max(Objects.requireNonNullElse(api.getWorkflowStep(), 1), 4));
        openApiDao.updateById(api);
        return ResponseDTO.ok();
    }

    /**
     * 查询API及当前版本的全部配置。
     */
    public ResponseDTO<Map<String, Object>> detail(Long openApiId) {
        OpenApiEntity api = openApiDao.selectById(openApiId);
        if (api == null) {
            return ResponseDTO.userErrorParam("API不存在");
        }
        if (!canManage(api)) {
            return ResponseDTO.userErrorParam("API不存在或无权访问");
        }
        return buildDetail(api);
    }

    /**
     * 查询已发布API的市场文档信息。
     */
    public ResponseDTO<Map<String, Object>> publishedDetail(Long openApiId) {
        OpenApiEntity api = openApiDao.selectById(openApiId);
        if (api == null || !Objects.equals(api.getStatus(), 4) || !Boolean.TRUE.equals(api.getEnabledFlag())) {
            return ResponseDTO.userErrorParam("API不存在或尚未上架");
        }
        OpenApiVersionEntity version = resolvePublishedVersion(api);
        if (version == null || !Objects.equals(version.getStatus(), 3)) {
            return ResponseDTO.userErrorParam("API线上版本不存在");
        }
        return buildDetail(api, version);
    }

    /**
     * 平台发布审核人查询指定提交版本的完整配置。
     */
    public ResponseDTO<Map<String, Object>> publishReviewDetail(Long openApiId, Long versionId) {
        if (!applicationDataScopeService.hasPlatformPermission("open-api:publish:review")) {
            return ResponseDTO.userErrorParam("无权查看API发布审核详情");
        }
        OpenApiEntity api = openApiDao.selectById(openApiId);
        OpenApiVersionEntity version = versionDao.selectById(versionId);
        if (api == null || version == null || !Objects.equals(version.getOpenApiId(), openApiId)) {
            return ResponseDTO.userErrorParam("API或提交版本不存在");
        }
        return buildDetail(api, version);
    }

    /**
     * 在数据权限校验通过后构建API详情。
     */
    private ResponseDTO<Map<String, Object>> buildDetail(OpenApiEntity api) {
        OpenApiVersionEntity version = resolveCurrentVersion(api);
        if (version == null) {
            return ResponseDTO.userErrorParam("API版本不存在");
        }
        return buildDetail(api, version);
    }

    /**
     * 构建指定API版本的完整配置。
     */
    private ResponseDTO<Map<String, Object>> buildDetail(OpenApiEntity api, OpenApiVersionEntity version) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("api", api);
        result.put("version", version);
        result.put("environments", environmentDao.selectList(new LambdaQueryWrapper<OpenApiEnvironmentEntity>()
                .eq(OpenApiEnvironmentEntity::getVersionId, version.getVersionId())
                .orderByAsc(OpenApiEnvironmentEntity::getEnvironmentId)));
        List<OpenApiParameterEntity> parameters = parameterDao.selectList(
                new LambdaQueryWrapper<OpenApiParameterEntity>()
                        .eq(OpenApiParameterEntity::getVersionId, version.getVersionId())
                        .orderByAsc(OpenApiParameterEntity::getDirection)
                        .orderByAsc(OpenApiParameterEntity::getSort)
                        .orderByAsc(OpenApiParameterEntity::getParameterId));
        result.put("requestParameters", parameters.stream()
                .filter(item -> Objects.equals(item.getDirection(), 1)).toList());
        result.put("responseParameters", parameters.stream()
                .filter(item -> Objects.equals(item.getDirection(), 2)).toList());
        result.put("examples", exampleDao.selectList(new LambdaQueryWrapper<OpenApiExampleEntity>()
                .eq(OpenApiExampleEntity::getVersionId, version.getVersionId())
                .orderByAsc(OpenApiExampleEntity::getSort)));
        result.put("errorCodes", errorCodeDao.selectList(new LambdaQueryWrapper<OpenApiErrorCodeEntity>()
                .eq(OpenApiErrorCodeEntity::getVersionId, version.getVersionId())
                .orderByAsc(OpenApiErrorCodeEntity::getSort)));
        return ResponseDTO.ok(result);
    }

    /**
     * 停用已上架API，不改变其稳定主键。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> updateStatus(OpenApiStatusForm form) {
        if (!Objects.equals(form.getStatus(), 5)) {
            return ResponseDTO.userErrorParam("API上架必须提交平台审核，当前接口仅支持停用");
        }
        OpenApiEntity api = openApiDao.selectById(form.getOpenApiId());
        if (api == null) {
            return ResponseDTO.userErrorParam("API不存在");
        }
        if (!canManage(api)) {
            return ResponseDTO.userErrorParam("API不存在或无权访问");
        }
        OpenApiVersionEntity version = resolvePublishedVersion(api);
        if (version == null) {
            return ResponseDTO.userErrorParam("API线上版本不存在");
        }
        if (!Objects.equals(api.getStatus(), 4)) {
            return ResponseDTO.userErrorParam("只有已上架API可以停用");
        }
        api.setStatus(form.getStatus());
        api.setEnabledFlag(false);
        openApiDao.updateById(api);
        version.setStatus(4);
        version.setLockedFlag(true);
        versionDao.updateById(version);
        return ResponseDTO.ok();
    }

    /**
     * 校验API编码、请求方式、路径、发布环境和唯一性约束。
     */
    private ResponseDTO<String> validateBasic(OpenApiBasicSaveForm form, OpenApiEntity existing) {
        if (!isValidApiCode(form.getApiCode())) {
            return ResponseDTO.userErrorParam("API编码需以字母开头，仅支持字母、数字、点、短横线、下划线和冒号");
        }
        if (StringUtils.isBlank(form.getRequestMethod())) {
            return ResponseDTO.userErrorParam("请求方式不能为空");
        }
        String method = StringUtils.upperCase(StringUtils.trim(form.getRequestMethod()));
        if (!HTTP_METHODS.contains(method)) {
            return ResponseDTO.userErrorParam("不支持的HTTP请求方式");
        }
        if (!form.getGatewayPath().startsWith("/")) {
            return ResponseDTO.userErrorParam("网关路径必须以/开头");
        }
        if (!form.getVersionNo().matches("^v?\\d+\\.\\d+\\.\\d+$")) {
            return ResponseDTO.userErrorParam("接口版本需使用v1.0.0格式");
        }
        ResponseDTO<String> environmentValidation = validateEnvironments(form.getEnvironments());
        if (!environmentValidation.getOk()) {
            return environmentValidation;
        }
        long codeCount = openApiDao.selectCount(new LambdaQueryWrapper<OpenApiEntity>()
                .eq(OpenApiEntity::getApiCode, form.getApiCode())
                .ne(existing != null, OpenApiEntity::getOpenApiId, existing == null ? null : existing.getOpenApiId()));
        if (codeCount > 0) {
            return ResponseDTO.userErrorParam("API编码已存在");
        }
        long pathCount = versionDao.selectCount(new LambdaQueryWrapper<OpenApiVersionEntity>()
                .eq(OpenApiVersionEntity::getRequestMethod, method)
                .eq(OpenApiVersionEntity::getGatewayPath, form.getGatewayPath())
                .eq(OpenApiVersionEntity::getVersionNo, form.getVersionNo())
                .ne(form.getVersionId() != null, OpenApiVersionEntity::getVersionId, form.getVersionId()));
        if (pathCount > 0) {
            return ResponseDTO.userErrorParam("该网关路径和版本已存在");
        }
        return ResponseDTO.ok();
    }

    /**
     * 校验发布环境编码和服务基础地址。
     */
    private ResponseDTO<String> validateEnvironments(List<OpenApiBasicSaveForm.EnvironmentItem> environments) {
        if (environments == null || environments.isEmpty()) {
            return ResponseDTO.userErrorParam("请至少配置一个API发布环境");
        }
        Set<String> environmentCodes = new HashSet<>();
        for (OpenApiBasicSaveForm.EnvironmentItem item : environments) {
            String environmentCode = StringUtils.trim(item.getEnvironmentCode());
            if (StringUtils.isBlank(environmentCode) || StringUtils.isBlank(item.getEnvironmentName())) {
                return ResponseDTO.userErrorParam("环境编码和环境名称不能为空");
            }
            if (!environmentCodes.add(StringUtils.lowerCase(environmentCode))) {
                return ResponseDTO.userErrorParam("环境编码不能重复");
            }
            if (!isValidBaseUrl(item.getBaseUrl())) {
                return ResponseDTO.userErrorParam("环境基础地址必须是有效的HTTP或HTTPS服务地址，且不能包含账号、查询参数或锚点");
            }
        }
        return ResponseDTO.ok();
    }

    /**
     * 判断服务基础地址是否可用于网关转发。
     */
    private boolean isValidBaseUrl(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)) {
            return false;
        }
        try {
            URI uri = new URI(StringUtils.trim(baseUrl));
            boolean supportedScheme = "http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme());
            return supportedScheme
                    && StringUtils.isNotBlank(uri.getHost())
                    && uri.getUserInfo() == null
                    && uri.getQuery() == null
                    && uri.getFragment() == null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    /**
     * 规范化服务基础地址，避免与内部路径拼接时产生重复斜杠。
     */
    private String normalizeBaseUrl(String baseUrl) {
        return StringUtils.removeEnd(StringUtils.trim(baseUrl), "/");
    }

    /**
     * 检查当前版本是否允许编辑。
     */
    private ResponseDTO<String> checkEditable(OpenApiEntity api, OpenApiVersionEntity version) {
        if (api == null || version == null || !Objects.equals(version.getOpenApiId(), api.getOpenApiId())) {
            return ResponseDTO.userErrorParam("API或版本不存在");
        }
        if (!canManage(api)) {
            return ResponseDTO.userErrorParam("API不存在或无权访问");
        }
        if (!Objects.equals(api.getCurrentVersionId(), version.getVersionId())
                || Boolean.TRUE.equals(version.getLockedFlag())
                || !Objects.equals(version.getStatus(), 1)) {
            return ResponseDTO.userErrorParam("当前API版本已锁定，不允许直接修改");
        }
        return ResponseDTO.ok();
    }

    /**
     * 将表单数据复制到稳定的API主记录。
     */
    private void copyMaster(OpenApiBasicSaveForm form, OpenApiEntity api) {
        api.setApiName(form.getApiName());
        api.setApiCode(form.getApiCode());
        api.setCategoryName(form.getCategoryName());
        api.setRequestMethod(StringUtils.upperCase(StringUtils.trim(form.getRequestMethod())));
        api.setRequestPath(form.getGatewayPath());
        api.setApiVersion(form.getVersionNo());
        api.setPermissionLevel(form.getPermissionLevel());
        api.setDescription(form.getDescription());
        api.setServiceName(form.getServiceName());
        api.setOwnerName(form.getOwnerName());
        api.setTags(form.getTags());
    }

    /**
     * 将表单数据复制到API版本记录。
     */
    private void copyVersion(OpenApiBasicSaveForm form, OpenApiVersionEntity version) {
        version.setVersionNo(form.getVersionNo());
        version.setRequestMethod(StringUtils.upperCase(StringUtils.trim(form.getRequestMethod())));
        version.setGatewayPath(form.getGatewayPath());
        version.setInternalPath(form.getInternalPath());
        version.setContentType(form.getContentType());
        version.setPermissionLevel(form.getPermissionLevel());
        version.setTimeoutSeconds(form.getTimeoutSeconds());
        version.setDescription(form.getDescription());
    }

    /**
     * 替换指定API版本的全部发布环境。
     */
    private void replaceEnvironments(Long versionId, List<OpenApiBasicSaveForm.EnvironmentItem> items) {
        environmentDao.delete(new LambdaQueryWrapper<OpenApiEnvironmentEntity>()
                .eq(OpenApiEnvironmentEntity::getVersionId, versionId));
        List<OpenApiBasicSaveForm.EnvironmentItem> environments = items == null ? new ArrayList<>() : items;
        for (OpenApiBasicSaveForm.EnvironmentItem item : environments) {
            OpenApiEnvironmentEntity entity = new OpenApiEnvironmentEntity();
            entity.setVersionId(versionId);
            entity.setEnvironmentCode(StringUtils.lowerCase(StringUtils.trim(item.getEnvironmentCode())));
            entity.setEnvironmentName(StringUtils.trim(item.getEnvironmentName()));
            entity.setBaseUrl(normalizeBaseUrl(item.getBaseUrl()));
            entity.setEnabledFlag(!Boolean.FALSE.equals(item.getEnabledFlag()));
            entity.setOnlineDebugFlag(Boolean.TRUE.equals(item.getOnlineDebugFlag()));
            entity.setDescription(item.getDescription());
            environmentDao.insert(entity);
        }
    }

    /**
     * 复制线上版本主记录并初始化新版本的审计信息。
     */
    private OpenApiVersionEntity copyNewVersion(OpenApiVersionEntity source, String versionNo,
                                                RequestEmployee employee) {
        OpenApiVersionEntity target = new OpenApiVersionEntity();
        target.setOpenApiId(source.getOpenApiId());
        target.setVersionNo(versionNo);
        target.setRequestMethod(source.getRequestMethod());
        target.setGatewayPath(source.getGatewayPath());
        target.setInternalPath(source.getInternalPath());
        target.setContentType(source.getContentType());
        target.setPermissionLevel(source.getPermissionLevel());
        target.setTimeoutSeconds(source.getTimeoutSeconds());
        target.setDescription(source.getDescription());
        target.setUnifiedResponseFlag(source.getUnifiedResponseFlag());
        target.setDataMaskingFlag(source.getDataMaskingFlag());
        target.setSecurityConfig(source.getSecurityConfig());
        target.setChangeLog(null);
        target.setStatus(1);
        target.setLockedFlag(false);
        target.setCreateUserId(employee.getEmployeeId());
        target.setCreateUserName(employee.getActualName());
        target.setUpdateUserId(employee.getEmployeeId());
        target.setUpdateUserName(employee.getActualName());
        return target;
    }

    /**
     * 复制版本关联的环境、参数、示例和错误码配置。
     */
    private void copyVersionDetails(Long sourceVersionId, Long targetVersionId) {
        for (OpenApiEnvironmentEntity source : environmentDao.selectList(
                new LambdaQueryWrapper<OpenApiEnvironmentEntity>()
                        .eq(OpenApiEnvironmentEntity::getVersionId, sourceVersionId)
                        .orderByAsc(OpenApiEnvironmentEntity::getEnvironmentId))) {
            OpenApiEnvironmentEntity target = new OpenApiEnvironmentEntity();
            target.setVersionId(targetVersionId);
            target.setEnvironmentCode(source.getEnvironmentCode());
            target.setEnvironmentName(source.getEnvironmentName());
            target.setBaseUrl(source.getBaseUrl());
            target.setEnabledFlag(source.getEnabledFlag());
            target.setOnlineDebugFlag(source.getOnlineDebugFlag());
            target.setDescription(source.getDescription());
            environmentDao.insert(target);
        }

        List<OpenApiParameterEntity> sourceParameters = parameterDao.selectList(
                new LambdaQueryWrapper<OpenApiParameterEntity>()
                        .eq(OpenApiParameterEntity::getVersionId, sourceVersionId)
                        .orderByAsc(OpenApiParameterEntity::getDirection)
                        .orderByAsc(OpenApiParameterEntity::getSort)
                        .orderByAsc(OpenApiParameterEntity::getParameterId));
        Map<Long, Long> parameterIdMap = new LinkedHashMap<>();
        for (OpenApiParameterEntity source : sourceParameters) {
            OpenApiParameterEntity target = copyParameter(source, targetVersionId);
            target.setParentId(null);
            parameterDao.insert(target);
            parameterIdMap.put(source.getParameterId(), target.getParameterId());
        }
        for (int index = 0; index < sourceParameters.size(); index++) {
            OpenApiParameterEntity source = sourceParameters.get(index);
            if (source.getParentId() != null) {
                OpenApiParameterEntity target = new OpenApiParameterEntity();
                target.setParameterId(parameterIdMap.get(source.getParameterId()));
                target.setParentId(parameterIdMap.get(source.getParentId()));
                parameterDao.updateById(target);
            }
        }

        for (OpenApiExampleEntity source : exampleDao.selectList(
                new LambdaQueryWrapper<OpenApiExampleEntity>()
                        .eq(OpenApiExampleEntity::getVersionId, sourceVersionId)
                        .orderByAsc(OpenApiExampleEntity::getSort))) {
            OpenApiExampleEntity target = new OpenApiExampleEntity();
            target.setVersionId(targetVersionId);
            target.setExampleType(source.getExampleType());
            target.setExampleName(source.getExampleName());
            target.setContent(source.getContent());
            target.setSort(source.getSort());
            exampleDao.insert(target);
        }

        for (OpenApiErrorCodeEntity source : errorCodeDao.selectList(
                new LambdaQueryWrapper<OpenApiErrorCodeEntity>()
                        .eq(OpenApiErrorCodeEntity::getVersionId, sourceVersionId)
                        .orderByAsc(OpenApiErrorCodeEntity::getSort))) {
            OpenApiErrorCodeEntity target = new OpenApiErrorCodeEntity();
            target.setVersionId(targetVersionId);
            target.setHttpStatus(source.getHttpStatus());
            target.setBusinessCode(source.getBusinessCode());
            target.setErrorMessage(source.getErrorMessage());
            target.setTriggerCondition(source.getTriggerCondition());
            target.setHandlingAdvice(source.getHandlingAdvice());
            target.setSort(source.getSort());
            errorCodeDao.insert(target);
        }
    }

    /**
     * 复制一个API参数定义，主键和父参数由调用方重新设置。
     */
    private OpenApiParameterEntity copyParameter(OpenApiParameterEntity source, Long targetVersionId) {
        OpenApiParameterEntity target = new OpenApiParameterEntity();
        target.setVersionId(targetVersionId);
        target.setDirection(source.getDirection());
        target.setLocation(source.getLocation());
        target.setParameterName(source.getParameterName());
        target.setChineseName(source.getChineseName());
        target.setDataType(source.getDataType());
        target.setRequiredFlag(source.getRequiredFlag());
        target.setNullableFlag(source.getNullableFlag());
        target.setDefaultValue(source.getDefaultValue());
        target.setExampleValue(source.getExampleValue());
        target.setValidationRule(source.getValidationRule());
        target.setDescription(source.getDescription());
        target.setMaskingFlag(source.getMaskingFlag());
        target.setSort(source.getSort());
        return target;
    }

    /**
     * 将API版本实体转换为版本记录展示对象。
     */
    private OpenApiVersionVO buildVersionVO(OpenApiEntity api, OpenApiVersionEntity version) {
        OpenApiVersionVO result = new OpenApiVersionVO();
        result.setVersionId(version.getVersionId());
        result.setVersionNo(version.getVersionNo());
        result.setRequestMethod(version.getRequestMethod());
        result.setGatewayPath(version.getGatewayPath());
        result.setStatus(version.getStatus());
        result.setCurrentFlag(Objects.equals(api.getCurrentVersionId(), version.getVersionId()));
        result.setPublishedFlag(Objects.equals(api.getPublishedVersionId(), version.getVersionId()));
        result.setChangeLog(version.getChangeLog());
        result.setCreateUserName(version.getCreateUserName());
        result.setCreateTime(version.getCreateTime());
        result.setUpdateTime(version.getUpdateTime());
        return result;
    }

    /**
     * 解析当前API版本，并兼容迁移后的历史数据。
     */
    private OpenApiVersionEntity resolveCurrentVersion(OpenApiEntity api) {
        if (api.getCurrentVersionId() != null) {
            OpenApiVersionEntity version = versionDao.selectById(api.getCurrentVersionId());
            if (version != null) {
                return version;
            }
        }
        return versionDao.selectOne(new LambdaQueryWrapper<OpenApiVersionEntity>()
                .eq(OpenApiVersionEntity::getOpenApiId, api.getOpenApiId())
                .orderByDesc(OpenApiVersionEntity::getVersionId)
                .last("limit 1"));
    }

    /**
     * 解析当前线上发布版本，并兼容尚未补充线上版本指针的历史数据。
     */
    public OpenApiVersionEntity resolvePublishedVersion(OpenApiEntity api) {
        if (api.getPublishedVersionId() != null) {
            OpenApiVersionEntity version = versionDao.selectById(api.getPublishedVersionId());
            if (version != null) {
                return version;
            }
        }
        if (api.getCurrentVersionId() != null) {
            OpenApiVersionEntity current = versionDao.selectById(api.getCurrentVersionId());
            if (current != null
                    && (Objects.equals(current.getStatus(), 3)
                    || Objects.equals(current.getStatus(), 4)
                    || Objects.equals(current.getStatus(), 5))) {
                return current;
            }
        }
        return versionDao.selectOne(new LambdaQueryWrapper<OpenApiVersionEntity>()
                .eq(OpenApiVersionEntity::getOpenApiId, api.getOpenApiId())
                .in(OpenApiVersionEntity::getStatus, 3, 4, 5)
                .orderByDesc(OpenApiVersionEntity::getVersionId)
                .last("limit 1"));
    }

    /**
     * 将API主记录中的线上路由字段同步为审核通过的版本内容。
     */
    public void syncPublishedFields(OpenApiEntity api, OpenApiVersionEntity version) {
        if (api == null || version == null) {
            return;
        }
        api.setRequestMethod(version.getRequestMethod());
        api.setRequestPath(version.getGatewayPath());
        api.setApiVersion(version.getVersionNo());
        api.setPermissionLevel(version.getPermissionLevel());
        api.setDescription(version.getDescription());
    }

    /**
     * 校验稳定API编码格式。
     */
    private boolean isValidApiCode(String apiCode) {
        return StringUtils.isNotBlank(apiCode)
                && apiCode.matches("^[A-Za-z][A-Za-z0-9._:-]{2,99}$");
    }

    /**
     * 非平台管理员仅可查询本人创建的API管理记录。
     */
    private void applyCreatorScope(LambdaQueryWrapper<OpenApiEntity> wrapper) {
        RequestEmployee employee = applicationDataScopeService.requireEmployee();
        if (!applicationDataScopeService.isPlatformAdministrator()) {
            wrapper.eq(OpenApiEntity::getCreateUserId, employee.getEmployeeId());
        }
    }

    /**
     * 判断当前员工是否可以管理指定API定义。
     */
    private boolean canManage(OpenApiEntity api) {
        RequestEmployee employee = applicationDataScopeService.requireEmployee();
        return applicationDataScopeService.isPlatformAdministrator()
                || Objects.equals(api.getCreateUserId(), employee.getEmployeeId());
    }

    /**
     * 获取当前员工并用于审计字段。
     */
    private RequestEmployee getRequestEmployee() {
        try {
            return AdminRequestUtil.getRequestUser();
        } catch (Exception exception) {
            log.debug("未获取到用于记录API审计字段的登录员工", exception);
            return null;
        }
    }
}
