package com.nexoraone.admin.module.business.openapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexoraone.admin.module.business.application.dao.OpenApiDao;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import com.nexoraone.admin.module.business.application.service.ApplicationDataScopeService;
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

/**
 * API开放平台管理服务。
 */
@Slf4j
@Service
public class OpenApiManageService {

    private static final Set<String> HTTP_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH");
    private static final Set<Integer> EDITABLE_STATUS = Set.of(1, 2, 3);

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
        applyCreatorScope(wrapper);
        Page<OpenApiEntity> page = openApiDao.selectPage(
                new Page<>(form.getPageNum(), form.getPageSize(), !Boolean.FALSE.equals(form.getSearchCount())), wrapper);
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
     * 查询API管理看板数量汇总。
     */
    public ResponseDTO<Map<String, Long>> summary() {
        Map<String, Long> result = new LinkedHashMap<>();
        LambdaQueryWrapper<OpenApiEntity> totalWrapper = new LambdaQueryWrapper<>();
        applyCreatorScope(totalWrapper);
        result.put("total", openApiDao.selectCount(totalWrapper));
        result.put("published", countByStatus(4));
        result.put("draft", countByStatus(1));
        result.put("disabled", countByStatus(5));
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
        return buildDetail(api);
    }

    /**
     * 在数据权限校验通过后构建API详情。
     */
    private ResponseDTO<Map<String, Object>> buildDetail(OpenApiEntity api) {
        OpenApiVersionEntity version = resolveCurrentVersion(api);
        if (version == null) {
            return ResponseDTO.userErrorParam("API版本不存在");
        }
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
        OpenApiVersionEntity version = resolveCurrentVersion(api);
        if (version == null) {
            return ResponseDTO.userErrorParam("API版本不存在");
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
     * 按状态统计API数量。
     */
    private long countByStatus(Integer status) {
        LambdaQueryWrapper<OpenApiEntity> wrapper = new LambdaQueryWrapper<OpenApiEntity>()
                .eq(OpenApiEntity::getStatus, status);
        applyCreatorScope(wrapper);
        return openApiDao.selectCount(wrapper);
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
        String method = form.getRequestMethod().toUpperCase();
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
        if (Boolean.TRUE.equals(version.getLockedFlag()) || !EDITABLE_STATUS.contains(api.getStatus())) {
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
        api.setRequestMethod(form.getRequestMethod().toUpperCase());
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
        version.setRequestMethod(form.getRequestMethod().toUpperCase());
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
