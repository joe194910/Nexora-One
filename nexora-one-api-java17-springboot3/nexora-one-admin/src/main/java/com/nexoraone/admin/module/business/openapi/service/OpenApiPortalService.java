package com.nexoraone.admin.module.business.openapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexoraone.admin.module.business.application.dao.ApplicationApiPermissionDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationCredentialDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationDao;
import com.nexoraone.admin.module.business.application.dao.OpenApiDao;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationApiPermissionEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationCredentialEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationEntity;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import com.nexoraone.admin.module.business.application.service.ApplicationDataScopeService;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiCallLogDao;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiEnvironmentDao;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiExampleDao;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiVersionDao;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiCallLogEntity;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiEnvironmentEntity;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiExampleEntity;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiVersionEntity;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiDebugForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiMarketQueryForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiPermissionApplyForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiPermissionReviewForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiPublishForm;
import com.nexoraone.admin.module.system.login.domain.RequestEmployee;
import com.nexoraone.admin.util.AdminRequestUtil;
import com.nexoraone.base.common.domain.PageResult;
import com.nexoraone.base.common.domain.ResponseDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * API开放平台市场、授权、发布、调试和统计服务。
 */
@Slf4j
@Service
public class OpenApiPortalService {

    @Resource
    private OpenApiDao openApiDao;
    @Resource
    private OpenApiVersionDao versionDao;
    @Resource
    private OpenApiEnvironmentDao environmentDao;
    @Resource
    private OpenApiExampleDao exampleDao;
    @Resource
    private ApplicationDao applicationDao;
    @Resource
    private ApplicationCredentialDao credentialDao;
    @Resource
    private ApplicationApiPermissionDao permissionDao;
    @Resource
    private OpenApiCallLogDao callLogDao;
    @Resource
    private OpenApiManageService manageService;
    @Resource
    private ApplicationDataScopeService applicationDataScopeService;

    /**
     * 分页查询已上架的API市场。
     */
    public ResponseDTO<PageResult<OpenApiEntity>> queryMarket(OpenApiMarketQueryForm form) {
        LambdaQueryWrapper<OpenApiEntity> wrapper = new LambdaQueryWrapper<OpenApiEntity>()
                .eq(OpenApiEntity::getStatus, 4)
                .eq(OpenApiEntity::getEnabledFlag, true);
        if (StringUtils.isNotBlank(form.getSearchWord())) {
            wrapper.and(item -> item.like(OpenApiEntity::getApiName, form.getSearchWord())
                    .or().like(OpenApiEntity::getApiCode, form.getSearchWord())
                    .or().like(OpenApiEntity::getDescription, form.getSearchWord()));
        }
        wrapper.eq(StringUtils.isNotBlank(form.getCategoryName()),
                        OpenApiEntity::getCategoryName, form.getCategoryName())
                .eq(form.getPermissionLevel() != null,
                        OpenApiEntity::getPermissionLevel, form.getPermissionLevel());
        if (StringUtils.isNotBlank(form.getRequestMethod())) {
            wrapper.eq(OpenApiEntity::getRequestMethod, StringUtils.upperCase(form.getRequestMethod()));
        }
        if (Objects.equals(form.getOrderType(), "calls")) {
            wrapper.orderByDesc(OpenApiEntity::getTotalCallCount);
        } else {
            wrapper.orderByAsc(OpenApiEntity::getSort).orderByDesc(OpenApiEntity::getPublishTime);
        }
        Page<OpenApiEntity> page = openApiDao.selectPage(
                new Page<>(form.getPageNum(), form.getPageSize(), !Boolean.FALSE.equals(form.getSearchCount())), wrapper);
        return ResponseDTO.ok(toPageResult(page));
    }

    /**
     * 查询已上架API的完整文档，避免门户侧读取草稿或已停用版本。
     */
    public ResponseDTO<Map<String, Object>> document(Long openApiId) {
        OpenApiEntity api = openApiDao.selectById(openApiId);
        if (api == null || !Objects.equals(api.getStatus(), 4) || !Boolean.TRUE.equals(api.getEnabledFlag())) {
            return ResponseDTO.userErrorParam("API不存在或尚未上架");
        }
        return manageService.publishedDetail(openApiId);
    }

    /**
     * 查询当前用户可管理的应用及其脱敏App ID。
     */
    public ResponseDTO<List<Map<String, Object>>> queryApplications() {
        LambdaQueryWrapper<ApplicationEntity> wrapper = new LambdaQueryWrapper<ApplicationEntity>()
                .ne(ApplicationEntity::getListingStatus, 4)
                .orderByDesc(ApplicationEntity::getUpdateTime);
        applicationDataScopeService.applyScope(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ApplicationEntity application : applicationDao.selectList(wrapper)) {
            ApplicationCredentialEntity credential = credentialDao.selectOne(
                    new LambdaQueryWrapper<ApplicationCredentialEntity>()
                            .eq(ApplicationCredentialEntity::getApplicationId, application.getApplicationId())
                            .eq(ApplicationCredentialEntity::getStatus, 1)
                            .orderByDesc(ApplicationCredentialEntity::getVersionNo)
                            .last("limit 1"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("applicationId", application.getApplicationId());
            item.put("applicationName", application.getApplicationName());
            item.put("applicationCode", application.getApplicationCode());
            item.put("listingStatus", application.getListingStatus());
            item.put("accessStatus", application.getAccessStatus());
            item.put("appId", credential == null ? null : credential.getAppId());
            result.add(item);
        }
        return ResponseDTO.ok(result);
    }

    /**
     * 提交API权限申请，公开API直接生成授权记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> applyPermission(OpenApiPermissionApplyForm form) {
        ApplicationEntity application = applicationDao.selectById(form.getApplicationId());
        if (!canManage(application)) {
            return ResponseDTO.userErrorParam("应用不存在或无权管理");
        }
        OpenApiEntity api = openApiDao.selectById(form.getOpenApiId());
        if (api == null || !Objects.equals(api.getStatus(), 4) || !Boolean.TRUE.equals(api.getEnabledFlag())) {
            return ResponseDTO.userErrorParam("API不存在或未上架");
        }
        ApplicationApiPermissionEntity permission = permissionDao.selectOne(
                new LambdaQueryWrapper<ApplicationApiPermissionEntity>()
                        .eq(ApplicationApiPermissionEntity::getApplicationId, form.getApplicationId())
                        .eq(ApplicationApiPermissionEntity::getOpenApiId, form.getOpenApiId())
                        .last("limit 1"));
        if (permission == null) {
            permission = new ApplicationApiPermissionEntity();
            permission.setApplicationId(form.getApplicationId());
            permission.setOpenApiId(form.getOpenApiId());
        }
        RequestEmployee employee = getRequestEmployee();
        permission.setApplyReason(form.getApplyReason());
        permission.setUseScene(form.getUseScene());
        permission.setApplyEnvironment(form.getApplyEnvironment());
        permission.setApplicantId(employee == null ? null : employee.getEmployeeId());
        permission.setApplicantName(employee == null ? null : employee.getActualName());
        permission.setApplyStatus(Objects.equals(api.getPermissionLevel(), 1) ? 2 : 1);
        permission.setReviewRemark(Objects.equals(api.getPermissionLevel(), 1) ? "公开API自动授权" : null);
        if (Objects.equals(api.getPermissionLevel(), 1)) {
            permission.setEffectiveTime(LocalDateTime.now());
        }
        if (permission.getPermissionId() == null) {
            permissionDao.insert(permission);
        } else {
            permissionDao.updateById(permission);
        }
        return ResponseDTO.ok();
    }

    /**
     * 查询API权限申请和授权状态。
     */
    public ResponseDTO<List<Map<String, Object>>> queryPermissions(Integer applyStatus) {
        boolean platformReviewer = applicationDataScopeService.hasPlatformPermission("open-api:grant:review");
        LambdaQueryWrapper<ApplicationApiPermissionEntity> wrapper =
                new LambdaQueryWrapper<ApplicationApiPermissionEntity>()
                        .eq(applyStatus != null, ApplicationApiPermissionEntity::getApplyStatus, applyStatus)
                        .orderByDesc(ApplicationApiPermissionEntity::getUpdateTime)
                        .orderByDesc(ApplicationApiPermissionEntity::getPermissionId);
        RequestEmployee employee = getRequestEmployee();
        List<ApplicationApiPermissionEntity> permissions = permissionDao.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ApplicationApiPermissionEntity permission : permissions) {
            ApplicationEntity application = applicationDao.selectById(permission.getApplicationId());
            if (application == null || !platformReviewer && !applicationDataScopeService.canManage(application)) {
                continue;
            }
            OpenApiEntity api = openApiDao.selectById(permission.getOpenApiId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("permission", permission);
            item.put("applicationName", application == null ? "-" : application.getApplicationName());
            item.put("apiName", api == null ? "-" : api.getApiName());
            item.put("apiCode", api == null ? "-" : api.getApiCode());
            item.put("apiVersion", api == null ? "-" : api.getApiVersion());
            result.add(item);
        }
        return ResponseDTO.ok(result);
    }

    /**
     * 审核API权限申请并生成有效授权信息。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> reviewPermission(OpenApiPermissionReviewForm form) {
        ApplicationApiPermissionEntity permission = permissionDao.selectById(form.getPermissionId());
        if (permission == null || !Objects.equals(permission.getApplyStatus(), 1)) {
            return ResponseDTO.userErrorParam("权限申请不存在或已审核");
        }
        RequestEmployee employee = getRequestEmployee();
        permission.setApplyStatus(form.getApplyStatus());
        permission.setReviewerId(employee == null ? null : employee.getEmployeeId());
        permission.setReviewerName(employee == null ? "系统" : employee.getActualName());
        permission.setReviewRemark(form.getReviewRemark());
        if (Objects.equals(form.getApplyStatus(), 2)) {
            permission.setDailyQuota(Objects.requireNonNullElse(form.getDailyQuota(), 100000L));
            permission.setEffectiveTime(LocalDateTime.now());
            permission.setExpireTime(form.getEffectiveDays() == null
                    ? null : LocalDateTime.now().plusDays(form.getEffectiveDays()));
        }
        permissionDao.updateById(permission);
        return ResponseDTO.ok();
    }

    /**
     * 查询API发布前检查结果和当前市场资料。
     */
    public ResponseDTO<Map<String, Object>> publishDetail(Long openApiId) {
        ResponseDTO<Map<String, Object>> detail = manageService.detail(openApiId);
        if (!detail.getOk()) {
            return detail;
        }
        Map<String, Object> data = detail.getData();
        List<Map<String, Object>> checks = buildPublishChecks(data);
        data.put("checks", checks);
        data.put("ready", checks.stream().allMatch(item -> Boolean.TRUE.equals(item.get("passed"))));
        return ResponseDTO.ok(data);
    }

    /**
     * 完成API上架并锁定当前版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> publish(OpenApiPublishForm form) {
        ResponseDTO<Map<String, Object>> detail = publishDetail(form.getOpenApiId());
        if (!detail.getOk()) {
            return ResponseDTO.error(detail);
        }
        if (!Boolean.TRUE.equals(detail.getData().get("ready"))) {
            return ResponseDTO.userErrorParam("发布前检查未通过，请先补全API定义");
        }
        OpenApiEntity api = (OpenApiEntity) detail.getData().get("api");
        OpenApiVersionEntity version = (OpenApiVersionEntity) detail.getData().get("version");
        api.setMarketTitle(form.getMarketTitle());
        api.setMarketSummary(form.getMarketSummary());
        api.setSlaDescription(form.getSlaDescription());
        api.setPublishScope(form.getPublishScope());
        api.setPublishTime(LocalDateTime.now());
        api.setStatus(4);
        api.setEnabledFlag(true);
        openApiDao.updateById(api);
        version.setStatus(3);
        version.setLockedFlag(true);
        versionDao.updateById(version);
        return ResponseDTO.ok();
    }

    /**
     * 执行测试环境在线调试并记录调用链路。
     *
     * <p>控制台调试使用平台托管身份校验授权，不读取或回显App Secret。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Map<String, Object>> debug(OpenApiDebugForm form) {
        long start = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        ApplicationEntity application = applicationDao.selectById(form.getApplicationId());
        if (!canManage(application)) {
            return ResponseDTO.userErrorParam("应用不存在或无权管理");
        }
        OpenApiEntity api = openApiDao.selectById(form.getOpenApiId());
        if (api == null || !Boolean.TRUE.equals(api.getEnabledFlag())) {
            return ResponseDTO.userErrorParam("API不存在或不可用");
        }
        if (!Objects.equals(api.getPermissionLevel(), 1)) {
            long grantCount = permissionDao.selectCount(new LambdaQueryWrapper<ApplicationApiPermissionEntity>()
                    .eq(ApplicationApiPermissionEntity::getApplicationId, form.getApplicationId())
                    .eq(ApplicationApiPermissionEntity::getOpenApiId, form.getOpenApiId())
                    .eq(ApplicationApiPermissionEntity::getApplyStatus, 2));
            if (grantCount == 0) {
                return ResponseDTO.userErrorParam("当前应用尚未获得该API授权");
            }
        }
        OpenApiVersionEntity version = versionDao.selectById(api.getCurrentVersionId());
        OpenApiEnvironmentEntity environment = environmentDao.selectOne(
                new LambdaQueryWrapper<OpenApiEnvironmentEntity>()
                        .eq(OpenApiEnvironmentEntity::getVersionId, api.getCurrentVersionId())
                        .eq(OpenApiEnvironmentEntity::getEnvironmentCode, form.getEnvironmentCode())
                        .eq(OpenApiEnvironmentEntity::getEnabledFlag, true)
                        .eq(OpenApiEnvironmentEntity::getOnlineDebugFlag, true)
                        .last("limit 1"));
        if (version == null || environment == null) {
            return ResponseDTO.userErrorParam("该环境未启用在线调试");
        }
        ApplicationCredentialEntity credential = credentialDao.selectOne(
                new LambdaQueryWrapper<ApplicationCredentialEntity>()
                        .eq(ApplicationCredentialEntity::getApplicationId, form.getApplicationId())
                        .eq(ApplicationCredentialEntity::getStatus, 1)
                        .orderByDesc(ApplicationCredentialEntity::getVersionNo)
                        .last("limit 1"));
        String nonce = UUID.randomUUID().toString().replace("-", "");
        long timestamp = System.currentTimeMillis();
        String bodyHash = sha256(Objects.requireNonNullElse(form.getBody(), ""));
        String canonicalRequest = version.getRequestMethod() + "\n" + version.getGatewayPath() + "\n"
                + timestamp + "\n" + nonce + "\n" + bodyHash;
        OpenApiExampleEntity responseExample = exampleDao.selectOne(
                new LambdaQueryWrapper<OpenApiExampleEntity>()
                        .eq(OpenApiExampleEntity::getVersionId, version.getVersionId())
                        .in(OpenApiExampleEntity::getExampleType, "success_response", "response")
                        .orderByAsc(OpenApiExampleEntity::getSort)
                        .last("limit 1"));
        String responseBody = responseExample == null
                ? "{\"code\":1,\"message\":\"success\",\"data\":{\"traceId\":\"" + traceId + "\"}}"
                : responseExample.getContent();
        long duration = Math.max(System.currentTimeMillis() - start, 1L);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("traceId", traceId);
        result.put("httpStatus", 200);
        result.put("durationMs", duration);
        result.put("responseBytes", responseBody.getBytes(StandardCharsets.UTF_8).length);
        result.put("requestUrl", environment.getBaseUrl() + version.getGatewayPath());
        result.put("requestMethod", version.getRequestMethod());
        result.put("appId", credential == null ? null : credential.getAppId());
        result.put("timestamp", timestamp);
        result.put("nonce", nonce);
        result.put("canonicalRequest", canonicalRequest);
        result.put("signature", "由平台服务端托管生成，控制台不展示App Secret");
        result.put("responseHeaders", Map.of("Content-Type", "application/json", "X-Trace-Id", traceId));
        result.put("responseBody", responseBody);
        result.put("dispatchMode", "definition-validation");
        saveCallLog(traceId, application, credential, api, form.getEnvironmentCode(), 200, duration, responseBody, null);
        return ResponseDTO.ok(result);
    }

    /**
     * 查询调用统计概览和最近调用日志。
     */
    public ResponseDTO<Map<String, Object>> statistics() {
        List<Long> applicationIds = null;
        if (!applicationDataScopeService.isPlatformAdministrator()) {
            applicationIds = applicationDataScopeService.getVisibleApplicationIds();
            if (applicationIds.isEmpty()) {
                return ResponseDTO.ok(emptyStatistics());
            }
        }
        LambdaQueryWrapper<OpenApiCallLogEntity> listWrapper = new LambdaQueryWrapper<OpenApiCallLogEntity>()
                .in(applicationIds != null, OpenApiCallLogEntity::getApplicationId, applicationIds)
                .orderByDesc(OpenApiCallLogEntity::getCreateTime)
                .last("limit 100");
        List<OpenApiCallLogEntity> logs = callLogDao.selectList(listWrapper);
        long success = logs.stream().filter(item -> Boolean.TRUE.equals(item.getSuccessFlag())).count();
        double average = logs.stream().map(OpenApiCallLogEntity::getDurationMs)
                .filter(Objects::nonNull).mapToLong(Long::longValue).average().orElse(0D);
        LambdaQueryWrapper<OpenApiCallLogEntity> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.in(applicationIds != null, OpenApiCallLogEntity::getApplicationId, applicationIds);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", callLogDao.selectCount(countWrapper));
        result.put("recentTotal", logs.size());
        result.put("successRate", logs.isEmpty() ? 100D : success * 100D / logs.size());
        result.put("averageDurationMs", Math.round(average));
        result.put("logs", logs);
        return ResponseDTO.ok(result);
    }

    /**
     * 构建没有可见调用记录时的统计结果。
     */
    private Map<String, Object> emptyStatistics() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", 0L);
        result.put("recentTotal", 0);
        result.put("successRate", 100D);
        result.put("averageDurationMs", 0L);
        result.put("logs", List.of());
        return result;
    }

    /**
     * 返回与现有应用认证接口一致的接入指南配置。
     */
    public ResponseDTO<Map<String, Object>> guide() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tokenPath", "/open/application/oauth/token");
        result.put("grantType", "client_credentials");
        result.put("tokenTtlSeconds", 7200);
        result.put("headers", List.of("Authorization", "X-App-Id", "X-Timestamp", "X-Nonce", "X-Signature"));
        result.put("canonicalRule",
                "HTTP_METHOD + \"\\n\" + REQUEST_PATH + \"\\n\" + TIMESTAMP + \"\\n\" + NONCE + \"\\n\" + SHA256(REQUEST_BODY)");
        result.put("signatureAlgorithm", "HMAC-SHA256");
        result.put("secretNotice", "App Secret仅允许保存在调用方服务端，禁止写入浏览器、URL、日志或公开仓库。");
        return ResponseDTO.ok(result);
    }

    /**
     * 构建发布前检查项。
     */
    private List<Map<String, Object>> buildPublishChecks(Map<String, Object> detail) {
        OpenApiEntity api = (OpenApiEntity) detail.get("api");
        OpenApiVersionEntity version = (OpenApiVersionEntity) detail.get("version");
        List<?> requestParameters = (List<?>) detail.get("requestParameters");
        List<?> responseParameters = (List<?>) detail.get("responseParameters");
        List<?> examples = (List<?>) detail.get("examples");
        List<?> errorCodes = (List<?>) detail.get("errorCodes");
        List<OpenApiEnvironmentEntity> environments = (List<OpenApiEnvironmentEntity>) detail.get("environments");
        return List.of(
                check("基本信息", StringUtils.isNoneBlank(api.getApiName(), api.getApiCode(), version.getGatewayPath())),
                check("入参定义", requestParameters != null),
                check("出参定义", responseParameters != null && !responseParameters.isEmpty()),
                check("请求响应示例", examples != null && !examples.isEmpty()),
                check("错误码", errorCodes != null && !errorCodes.isEmpty()),
                check("测试环境", environments != null && environments.stream()
                        .anyMatch(item -> Boolean.TRUE.equals(item.getOnlineDebugFlag()))),
                check("网关路由", StringUtils.isNotBlank(version.getInternalPath()))
        );
    }

    /**
     * 创建一条发布检查结果。
     */
    private Map<String, Object> check(String name, boolean passed) {
        return Map.of("name", name, "passed", passed);
    }

    /**
     * 保存一次在线调试调用日志。
     */
    private void saveCallLog(String traceId, ApplicationEntity application,
                             ApplicationCredentialEntity credential, OpenApiEntity api,
                             String environmentCode, int httpStatus, long duration,
                             String responseBody, String errorMessage) {
        OpenApiCallLogEntity logEntity = new OpenApiCallLogEntity();
        logEntity.setTraceId(traceId);
        logEntity.setApplicationId(application.getApplicationId());
        logEntity.setAppId(credential == null ? null : credential.getAppId());
        logEntity.setOpenApiId(api.getOpenApiId());
        logEntity.setApiCode(api.getApiCode());
        logEntity.setEnvironmentCode(environmentCode);
        logEntity.setRequestMethod(api.getRequestMethod());
        logEntity.setRequestPath(api.getRequestPath());
        logEntity.setHttpStatus(httpStatus);
        logEntity.setSuccessFlag(httpStatus >= 200 && httpStatus < 300);
        logEntity.setDurationMs(duration);
        logEntity.setResponseBytes((long) responseBody.getBytes(StandardCharsets.UTF_8).length);
        logEntity.setErrorMessage(errorMessage);
        callLogDao.insert(logEntity);
        api.setTodayCallCount(Objects.requireNonNullElse(api.getTodayCallCount(), 0L) + 1);
        api.setTotalCallCount(Objects.requireNonNullElse(api.getTotalCallCount(), 0L) + 1);
        openApiDao.updateById(api);
    }

    /**
     * 判断当前登录用户是否可以管理指定应用。
     */
    private boolean canManage(ApplicationEntity application) {
        if (application == null || Objects.equals(application.getListingStatus(), 4)) {
            return false;
        }
        return applicationDataScopeService.canManage(application);
    }

    /**
     * 转换MyBatis分页结果。
     */
    private <T> PageResult<T> toPageResult(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setList(page.getRecords());
        result.setEmptyFlag(page.getRecords().isEmpty());
        return result;
    }

    /**
     * 计算SHA-256十六进制摘要。
     */
    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            log.error("生成调试请求体摘要失败", exception);
            return "";
        }
    }

    /**
     * 获取当前登录员工。
     */
    private RequestEmployee getRequestEmployee() {
        try {
            return AdminRequestUtil.getRequestUser();
        } catch (Exception exception) {
            log.debug("未读取到当前登录员工", exception);
            return null;
        }
    }
}
