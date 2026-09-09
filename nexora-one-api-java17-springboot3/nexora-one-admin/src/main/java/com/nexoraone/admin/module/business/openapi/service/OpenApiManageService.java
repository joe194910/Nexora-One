package com.nexoraone.admin.module.business.openapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexoraone.admin.module.business.application.dao.OpenApiDao;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * API open platform management service.
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

    /**
     * Query the API management list by page.
     */
    public ResponseDTO<PageResult<OpenApiEntity>> query(OpenApiQueryForm form) {
        LambdaQueryWrapper<OpenApiEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(form.getSearchWord())) {
            wrapper.and(query -> query.like(OpenApiEntity::getApiName, form.getSearchWord())
                    .or().like(OpenApiEntity::getApiCode, form.getSearchWord()));
        }
        wrapper.eq(StringUtils.isNotBlank(form.getCategoryName()),
                        OpenApiEntity::getCategoryName, form.getCategoryName())
                .eq(StringUtils.isNotBlank(form.getRequestMethod()),
                        OpenApiEntity::getRequestMethod, form.getRequestMethod().toUpperCase())
                .eq(form.getStatus() != null, OpenApiEntity::getStatus, form.getStatus())
                .orderByDesc(OpenApiEntity::getUpdateTime)
                .orderByDesc(OpenApiEntity::getOpenApiId);
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
     * Query API count summary for the management dashboard.
     */
    public ResponseDTO<Map<String, Long>> summary() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("total", openApiDao.selectCount(null));
        result.put("published", countByStatus(4));
        result.put("draft", countByStatus(1));
        result.put("disabled", countByStatus(5));
        return ResponseDTO.ok(result);
    }

    /**
     * Query distinct API categories used by existing records.
     */
    public ResponseDTO<List<String>> categories() {
        List<String> categories = openApiDao.selectList(new LambdaQueryWrapper<OpenApiEntity>()
                        .select(OpenApiEntity::getCategoryName)
                        .isNotNull(OpenApiEntity::getCategoryName)
                        .orderByAsc(OpenApiEntity::getCategoryName))
                .stream()
                .map(OpenApiEntity::getCategoryName)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        return ResponseDTO.ok(categories);
    }

    /**
     * Check whether an API code is available.
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
     * Create an API and its first editable version.
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Map<String, Long>> create(OpenApiBasicSaveForm form) {
        ResponseDTO<String> validation = validateBasic(form, null);
        if (!validation.getOk()) {
            return ResponseDTO.userErrorParam(validation.getMsg());
        }
        RequestEmployee employee = getRequestEmployee();
        OpenApiEntity api = new OpenApiEntity();
        copyMaster(form, api);
        api.setStatus(1);
        api.setWorkflowStep(1);
        api.setTodayCallCount(0L);
        api.setTotalCallCount(0L);
        api.setEnabledFlag(false);
        api.setSort(0);
        api.setCreateUserId(employee == null ? null : employee.getEmployeeId());
        api.setCreateUserName(employee == null ? null : employee.getActualName());
        api.setUpdateUserId(employee == null ? null : employee.getEmployeeId());
        api.setUpdateUserName(employee == null ? null : employee.getActualName());
        openApiDao.insert(api);

        OpenApiVersionEntity version = new OpenApiVersionEntity();
        copyVersion(form, version);
        version.setOpenApiId(api.getOpenApiId());
        version.setUnifiedResponseFlag(true);
        version.setDataMaskingFlag(true);
        version.setStatus(1);
        version.setLockedFlag(false);
        version.setCreateUserId(employee == null ? null : employee.getEmployeeId());
        version.setCreateUserName(employee == null ? null : employee.getActualName());
        version.setUpdateUserId(employee == null ? null : employee.getEmployeeId());
        version.setUpdateUserName(employee == null ? null : employee.getActualName());
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
     * Update API basic information and the current editable version.
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
     * Replace all request or response parameters for one API version.
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
     * Replace API request examples, response examples and business error codes.
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
     * Query an API and all configuration of its current version.
     */
    public ResponseDTO<Map<String, Object>> detail(Long openApiId) {
        OpenApiEntity api = openApiDao.selectById(openApiId);
        if (api == null) {
            return ResponseDTO.userErrorParam("API不存在");
        }
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
     * Enable or disable an existing API without changing its stable primary key.
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> updateStatus(OpenApiStatusForm form) {
        if (!Set.of(4, 5).contains(form.getStatus())) {
            return ResponseDTO.userErrorParam("当前阶段仅支持启用或停用API");
        }
        OpenApiEntity api = openApiDao.selectById(form.getOpenApiId());
        if (api == null) {
            return ResponseDTO.userErrorParam("API不存在");
        }
        OpenApiVersionEntity version = resolveCurrentVersion(api);
        if (version == null) {
            return ResponseDTO.userErrorParam("API版本不存在");
        }
        if (Objects.equals(form.getStatus(), 4) && api.getWorkflowStep() < 4) {
            return ResponseDTO.userErrorParam("请先完成请求参数、响应参数和示例错误码配置");
        }
        boolean enabled = Objects.equals(form.getStatus(), 4);
        api.setStatus(form.getStatus());
        api.setEnabledFlag(enabled);
        openApiDao.updateById(api);
        version.setStatus(enabled ? 3 : 4);
        version.setLockedFlag(enabled);
        versionDao.updateById(version);
        return ResponseDTO.ok();
    }

    /**
     * Count APIs by one status value.
     */
    private long countByStatus(Integer status) {
        return openApiDao.selectCount(new LambdaQueryWrapper<OpenApiEntity>()
                .eq(OpenApiEntity::getStatus, status));
    }

    /**
     * Validate API code, method, path and uniqueness constraints.
     */
    private ResponseDTO<String> validateBasic(OpenApiBasicSaveForm form, OpenApiEntity existing) {
        if (!isValidApiCode(form.getApiCode())) {
            return ResponseDTO.userErrorParam("API编码需以字母开头，仅支持字母、数字、点、短横线、下划线和冒号");
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
        long codeCount = openApiDao.selectCount(new LambdaQueryWrapper<OpenApiEntity>()
                .eq(OpenApiEntity::getApiCode, form.getApiCode())
                .ne(existing != null, OpenApiEntity::getOpenApiId, existing == null ? null : existing.getOpenApiId()));
        if (codeCount > 0) {
            return ResponseDTO.userErrorParam("API编码已存在");
        }
        long pathCount = versionDao.selectCount(new LambdaQueryWrapper<OpenApiVersionEntity>()
                .eq(OpenApiVersionEntity::getGatewayPath, form.getGatewayPath())
                .eq(OpenApiVersionEntity::getVersionNo, form.getVersionNo())
                .ne(form.getVersionId() != null, OpenApiVersionEntity::getVersionId, form.getVersionId()));
        if (pathCount > 0) {
            return ResponseDTO.userErrorParam("该网关路径和版本已存在");
        }
        return ResponseDTO.ok();
    }

    /**
     * Check whether the current version can be edited.
     */
    private ResponseDTO<String> checkEditable(OpenApiEntity api, OpenApiVersionEntity version) {
        if (api == null || version == null || !Objects.equals(version.getOpenApiId(), api.getOpenApiId())) {
            return ResponseDTO.userErrorParam("API或版本不存在");
        }
        if (Boolean.TRUE.equals(version.getLockedFlag()) || !EDITABLE_STATUS.contains(api.getStatus())) {
            return ResponseDTO.userErrorParam("当前API版本已锁定，不允许直接修改");
        }
        return ResponseDTO.ok();
    }

    /**
     * Copy form values to the stable API master record.
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
     * Copy form values to an API version record.
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
     * Replace all release environments of one API version.
     */
    private void replaceEnvironments(Long versionId, List<OpenApiBasicSaveForm.EnvironmentItem> items) {
        environmentDao.delete(new LambdaQueryWrapper<OpenApiEnvironmentEntity>()
                .eq(OpenApiEnvironmentEntity::getVersionId, versionId));
        List<OpenApiBasicSaveForm.EnvironmentItem> environments = items == null ? new ArrayList<>() : items;
        for (OpenApiBasicSaveForm.EnvironmentItem item : environments) {
            OpenApiEnvironmentEntity entity = new OpenApiEnvironmentEntity();
            entity.setVersionId(versionId);
            entity.setEnvironmentCode(item.getEnvironmentCode());
            entity.setEnvironmentName(item.getEnvironmentName());
            entity.setBaseUrl(item.getBaseUrl());
            entity.setEnabledFlag(!Boolean.FALSE.equals(item.getEnabledFlag()));
            entity.setOnlineDebugFlag(Boolean.TRUE.equals(item.getOnlineDebugFlag()));
            entity.setDescription(item.getDescription());
            environmentDao.insert(entity);
        }
    }

    /**
     * Resolve the current API version, with a fallback for migrated historical data.
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
     * Validate the stable API code format.
     */
    private boolean isValidApiCode(String apiCode) {
        return StringUtils.isNotBlank(apiCode)
                && apiCode.matches("^[A-Za-z][A-Za-z0-9._:-]{2,99}$");
    }

    /**
     * Read the current employee for audit fields.
     */
    private RequestEmployee getRequestEmployee() {
        try {
            return AdminRequestUtil.getRequestUser();
        } catch (Exception exception) {
            log.debug("No logged-in employee was found for API audit fields", exception);
            return null;
        }
    }
}
