package com.nexoraone.admin.module.business.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexoraone.admin.module.business.application.dao.ApplicationCredentialDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationFavoriteDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationApiPermissionDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationSsoAuthCodeDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationVersionDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationVisitLogDao;
import com.nexoraone.admin.module.business.application.dao.OpenApiDao;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationApiPermissionEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationCredentialEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationFavoriteEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationSsoAuthCodeEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationVersionEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationVisitLogEntity;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationFavoriteForm;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationLaunchForm;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationPortalQueryForm;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationSsoTokenForm;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationStatusUpdateForm;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationVisitLogQueryForm;
import com.nexoraone.admin.module.business.application.manager.ApplicationCredentialManager;
import com.nexoraone.admin.module.system.login.domain.RequestEmployee;
import com.nexoraone.admin.module.system.role.dao.RoleEmployeeDao;
import com.nexoraone.admin.module.system.role.domain.vo.RoleVO;
import com.nexoraone.admin.util.AdminRequestUtil;
import com.nexoraone.base.common.domain.PageResult;
import com.nexoraone.base.common.domain.ResponseDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 应用市场、用户应用门户、单点进入和运营管理服务。
 */
@Slf4j
@Service
public class ApplicationPortalService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Resource
    private ApplicationDao applicationDao;
    @Resource
    private OpenApiDao openApiDao;
    @Resource
    private ApplicationApiPermissionDao applicationApiPermissionDao;
    @Resource
    private ApplicationCredentialDao credentialDao;
    @Resource
    private ApplicationFavoriteDao favoriteDao;
    @Resource
    private ApplicationVisitLogDao visitLogDao;
    @Resource
    private ApplicationSsoAuthCodeDao authCodeDao;
    @Resource
    private ApplicationVersionDao versionDao;
    @Resource
    private RoleEmployeeDao roleEmployeeDao;
    @Resource
    private ApplicationCredentialManager credentialManager;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private ApplicationDataScopeService applicationDataScopeService;

    /**
     * 分页查询当前用户可见的已上架应用。
     */
    public ResponseDTO<PageResult<Map<String, Object>>> queryMarket(ApplicationPortalQueryForm form) {
        RequestEmployee employee = getRequestEmployee();
        Set<Long> favoriteIds = queryFavoriteIds(employee);
        LambdaQueryWrapper<ApplicationEntity> wrapper = new LambdaQueryWrapper<ApplicationEntity>()
                .eq(ApplicationEntity::getListingStatus, 2)
                .eq(ApplicationEntity::getAccessStatus, 2)
                .orderByDesc(ApplicationEntity::getUpdateTime);
        if (StringUtils.isNotBlank(form.getSearchWord())) {
            wrapper.and(item -> item.like(ApplicationEntity::getApplicationName, form.getSearchWord())
                    .or().like(ApplicationEntity::getApplicationCode, form.getSearchWord())
                    .or().like(ApplicationEntity::getSummary, form.getSearchWord()));
        }
        List<ApplicationEntity> visible = applicationDao.selectList(wrapper).stream()
                .filter(item -> canView(item, employee))
                .filter(item -> matchesPortalFilter(item, form, favoriteIds))
                .toList();
        long total = visible.size();
        int pageNum = Math.toIntExact(Math.max(form.getPageNum(), 1));
        int pageSize = Math.toIntExact(Math.max(form.getPageSize(), 1));
        int from = Math.min((pageNum - 1) * pageSize, visible.size());
        int to = Math.min(from + pageSize, visible.size());
        List<Map<String, Object>> records = visible.subList(from, to).stream()
                .map(item -> toPortalMap(item, favoriteIds.contains(item.getApplicationId()), employee))
                .toList();
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setPageNum((long) pageNum);
        result.setPageSize((long) pageSize);
        result.setTotal(total);
        result.setPages((total + pageSize - 1) / pageSize);
        result.setList(records);
        result.setEmptyFlag(records.isEmpty());
        return ResponseDTO.ok(result);
    }

    /**
     * 查询当前已上架应用的市场分类。
     */
    public ResponseDTO<List<String>> categories() {
        RequestEmployee employee = getRequestEmployee();
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        applicationDao.selectList(new LambdaQueryWrapper<ApplicationEntity>()
                        .eq(ApplicationEntity::getListingStatus, 2)
                        .eq(ApplicationEntity::getAccessStatus, 2)
                        .orderByDesc(ApplicationEntity::getUpdateTime))
                .stream()
                .filter(item -> canView(item, employee))
                .map(item -> Objects.toString(readJson(item.getListingConfig()).get("category"), "未分类"))
                .forEach(categories::add);
        return ResponseDTO.ok(new ArrayList<>(categories));
    }

    /**
     * 查询当前用户可使用、已收藏和最近访问的应用。
     */
    public ResponseDTO<Map<String, Object>> myApplications() {
        RequestEmployee employee = requireEmployee();
        Set<Long> favoriteIds = queryFavoriteIds(employee);
        List<Map<String, Object>> applications = applicationDao.selectList(
                        new LambdaQueryWrapper<ApplicationEntity>()
                                .eq(ApplicationEntity::getListingStatus, 2)
                                .eq(ApplicationEntity::getAccessStatus, 2)
                                .orderByDesc(ApplicationEntity::getUpdateTime))
                .stream()
                .filter(item -> canView(item, employee))
                .map(item -> toPortalMap(item, favoriteIds.contains(item.getApplicationId()), employee))
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applications", applications);
        result.put("favorites", applications.stream()
                .filter(item -> Boolean.TRUE.equals(item.get("favoriteFlag"))).toList());
        result.put("recent", recentInternal(employee, 12));
        return ResponseDTO.ok(result);
    }

    /**
     * 查询首页应用与开放平台统计数据。
     */
    public ResponseDTO<Map<String, Long>> homeOverview() {
        RequestEmployee employee = requireEmployee();
        long listedApplications = applicationDao.selectList(
                        new LambdaQueryWrapper<ApplicationEntity>()
                                .eq(ApplicationEntity::getListingStatus, 2)
                                .eq(ApplicationEntity::getAccessStatus, 2))
                .stream()
                .filter(item -> canView(item, employee))
                .count();
        Long publishedApis = openApiDao.selectCount(new LambdaQueryWrapper<OpenApiEntity>()
                .eq(OpenApiEntity::getStatus, 4)
                .eq(OpenApiEntity::getEnabledFlag, true));

        LambdaQueryWrapper<ApplicationEntity> applicationReviewWrapper =
                new LambdaQueryWrapper<ApplicationEntity>().eq(ApplicationEntity::getListingStatus, 1);
        applicationDataScopeService.applyScope(applicationReviewWrapper, "application:review");
        long pendingApplicationReviews = applicationDao.selectCount(applicationReviewWrapper);

        LambdaQueryWrapper<ApplicationApiPermissionEntity> apiReviewWrapper =
                new LambdaQueryWrapper<ApplicationApiPermissionEntity>()
                        .eq(ApplicationApiPermissionEntity::getApplyStatus, 1);
        if (!applicationDataScopeService.hasPlatformPermission("open-api:grant:review")) {
            List<Long> visibleApplicationIds = applicationDataScopeService.getVisibleApplicationIds();
            if (visibleApplicationIds.isEmpty()) {
                apiReviewWrapper.eq(ApplicationApiPermissionEntity::getApplicationId, -1L);
            } else {
                apiReviewWrapper.in(ApplicationApiPermissionEntity::getApplicationId, visibleApplicationIds);
            }
        }
        long pendingApiReviews = applicationApiPermissionDao.selectCount(apiReviewWrapper);

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("listedApplications", listedApplications);
        result.put("publishedApis", publishedApis);
        result.put("mcpOnlineServices", 0L);
        result.put("pendingReviews", pendingApplicationReviews + pendingApiReviews);
        return ResponseDTO.ok(result);
    }

    /**
     * 收藏或取消收藏应用。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> favorite(ApplicationFavoriteForm form) {
        RequestEmployee employee = requireEmployee();
        ApplicationEntity application = applicationDao.selectById(form.getApplicationId());
        if (!canView(application, employee)) {
            return ResponseDTO.userErrorParam("应用不存在或当前用户无权访问");
        }
        if (!Boolean.TRUE.equals(readBoolean(readJson(application.getPublishConfig()), "allowFavorite", true))) {
            return ResponseDTO.userErrorParam("该应用未开放收藏");
        }
        LambdaQueryWrapper<ApplicationFavoriteEntity> wrapper = new LambdaQueryWrapper<ApplicationFavoriteEntity>()
                .eq(ApplicationFavoriteEntity::getApplicationId, form.getApplicationId())
                .eq(ApplicationFavoriteEntity::getEmployeeId, employee.getEmployeeId());
        ApplicationFavoriteEntity existing = favoriteDao.selectOne(wrapper);
        if (Boolean.TRUE.equals(form.getFavoriteFlag()) && existing == null) {
            ApplicationFavoriteEntity entity = new ApplicationFavoriteEntity();
            entity.setApplicationId(form.getApplicationId());
            entity.setEmployeeId(employee.getEmployeeId());
            favoriteDao.insert(entity);
        } else if (Boolean.FALSE.equals(form.getFavoriteFlag()) && existing != null) {
            favoriteDao.deleteById(existing.getFavoriteId());
        }
        return ResponseDTO.ok();
    }

    /**
     * 查询当前用户最近访问的应用。
     */
    public ResponseDTO<List<Map<String, Object>>> recent() {
        return ResponseDTO.ok(recentInternal(requireEmployee(), 20));
    }

    /**
     * 校验可见权限并签发一次性单点登录授权码。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Map<String, Object>> launch(ApplicationLaunchForm form) {
        RequestEmployee employee = requireEmployee();
        ApplicationEntity application = applicationDao.selectById(form.getApplicationId());
        if (!canView(application, employee)) {
            saveVisit(application, employee, null, false, "应用不存在、未上架或当前用户无访问权限");
            return ResponseDTO.userErrorParam("应用不存在、未上架或当前用户无访问权限");
        }
        if (StringUtils.isBlank(application.getHomeUrl())) {
            saveVisit(application, employee, null, false, "应用首页地址未配置");
            return ResponseDTO.userErrorParam("应用首页地址未配置");
        }
        ApplicationCredentialEntity credential = credentialManager.getCurrentCredential(application.getApplicationId());
        if (credential == null) {
            saveVisit(application, employee, null, false, "应用凭证不可用");
            return ResponseDTO.userErrorParam("应用凭证不可用");
        }
        String code = generateCode();
        String state = generateCode();
        Map<String, Object> loginConfig = readJson(application.getLoginConfig());
        int ttl = Math.min(Math.max(readInt(loginConfig, "codeTtl", 300), 60), 600);
        String redirectUri = firstCallback(loginConfig);
        ApplicationSsoAuthCodeEntity authCode = new ApplicationSsoAuthCodeEntity();
        authCode.setApplicationId(application.getApplicationId());
        authCode.setAppId(credential.getAppId());
        authCode.setCodeHash(sha256(code));
        authCode.setEmployeeId(employee.getEmployeeId());
        authCode.setUserSnapshot(writeJson(buildUserSnapshot(employee)));
        authCode.setRedirectUri(redirectUri);
        authCode.setExpiresTime(LocalDateTime.now().plusSeconds(ttl));
        authCode.setUsedFlag(false);
        authCodeDao.insert(authCode);
        String launchUrl = UriComponentsBuilder.fromUriString(application.getHomeUrl())
                .queryParam("nexora_code", code)
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
        saveVisit(application, employee, launchUrl, true, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("launchUrl", launchUrl);
        result.put("openMode", Objects.toString(readJson(application.getPublishConfig()).get("openMode"), "NEW_TAB"));
        result.put("expiresIn", ttl);
        return ResponseDTO.ok(result);
    }

    /**
     * 第三方应用使用App ID、App Secret和一次性授权码换取用户信息。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Map<String, Object>> exchangeSsoCode(ApplicationSsoTokenForm form) {
        ApplicationCredentialEntity credential = credentialManager.verifyCredential(form.getAppId(), form.getAppSecret());
        if (credential == null) {
            return ResponseDTO.userErrorParam("App ID或App Secret错误");
        }
        ApplicationSsoAuthCodeEntity authCode = authCodeDao.selectOne(
                new LambdaQueryWrapper<ApplicationSsoAuthCodeEntity>()
                        .eq(ApplicationSsoAuthCodeEntity::getAppId, form.getAppId())
                        .eq(ApplicationSsoAuthCodeEntity::getCodeHash, sha256(form.getCode()))
                        .last("limit 1"));
        if (authCode == null || !Objects.equals(authCode.getApplicationId(), credential.getApplicationId())) {
            return ResponseDTO.userErrorParam("授权码无效");
        }
        if (Boolean.TRUE.equals(authCode.getUsedFlag())) {
            return ResponseDTO.userErrorParam("授权码已经使用");
        }
        if (authCode.getExpiresTime() == null || authCode.getExpiresTime().isBefore(LocalDateTime.now())) {
            return ResponseDTO.userErrorParam("授权码已经过期");
        }
        if (StringUtils.isNotBlank(form.getRedirectUri())
                && StringUtils.isNotBlank(authCode.getRedirectUri())
                && !Objects.equals(form.getRedirectUri(), authCode.getRedirectUri())) {
            return ResponseDTO.userErrorParam("redirect_uri与应用配置不一致");
        }
        authCode.setUsedFlag(true);
        authCode.setUsedTime(LocalDateTime.now());
        authCodeDao.updateById(authCode);
        Map<String, Object> result = readJson(authCode.getUserSnapshot());
        result.put("appId", authCode.getAppId());
        result.put("issuedAt", authCode.getCreateTime());
        return ResponseDTO.ok(result);
    }

    /**
     * 查询应用运营概览。
     */
    public ResponseDTO<Map<String, Object>> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", applicationDao.selectCount(null));
        result.put("listed", countByStatus(2));
        result.put("reviewing", countByStatus(1));
        result.put("unlisted", countByStatus(0) + countByStatus(4));
        result.put("rejected", countByStatus(3));
        result.put("visits", visitLogDao.selectCount(null));
        result.put("favorites", favoriteDao.selectCount(null));
        return ResponseDTO.ok(result);
    }

    /**
     * 分页查询应用访问日志。
     */
    public ResponseDTO<PageResult<ApplicationVisitLogEntity>> queryVisitLogs(ApplicationVisitLogQueryForm form) {
        LambdaQueryWrapper<ApplicationVisitLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(form.getSearchWord())) {
            wrapper.and(item -> item.like(ApplicationVisitLogEntity::getApplicationName, form.getSearchWord())
                    .or().like(ApplicationVisitLogEntity::getEmployeeName, form.getSearchWord())
                    .or().like(ApplicationVisitLogEntity::getDepartmentName, form.getSearchWord()));
        }
        wrapper.eq(form.getApplicationId() != null, ApplicationVisitLogEntity::getApplicationId, form.getApplicationId())
                .eq(form.getSuccessFlag() != null, ApplicationVisitLogEntity::getSuccessFlag, form.getSuccessFlag())
                .orderByDesc(ApplicationVisitLogEntity::getVisitTime);
        Page<ApplicationVisitLogEntity> page = visitLogDao.selectPage(
                new Page<>(form.getPageNum(), form.getPageSize(), !Boolean.FALSE.equals(form.getSearchCount())), wrapper);
        return ResponseDTO.ok(toPageResult(page));
    }

    /**
     * 管理员执行应用上架或下架。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> updateStatus(ApplicationStatusUpdateForm form) {
        if (!Objects.equals(form.getListingStatus(), 2) && !Objects.equals(form.getListingStatus(), 4)) {
            return ResponseDTO.userErrorParam("目标状态只支持上架或下架");
        }
        ApplicationEntity application = applicationDao.selectById(form.getApplicationId());
        if (application == null) {
            return ResponseDTO.userErrorParam("应用不存在");
        }
        if (Objects.equals(form.getListingStatus(), 2)
                && !List.of(2, 4).contains(application.getListingStatus())) {
            return ResponseDTO.userErrorParam("只有已发布或已下架的应用可以重新上架");
        }
        application.setListingStatus(form.getListingStatus());
        application.setConfigLocked(true);
        applicationDao.updateById(application);
        ApplicationVersionEntity version = versionDao.selectOne(new LambdaQueryWrapper<ApplicationVersionEntity>()
                .eq(ApplicationVersionEntity::getApplicationId, application.getApplicationId())
                .orderByDesc(ApplicationVersionEntity::getSubmitTime)
                .last("limit 1"));
        if (version != null) {
            version.setVersionStatus(Objects.equals(form.getListingStatus(), 2) ? 2 : 4);
            versionDao.updateById(version);
        }
        return ResponseDTO.okMsg(Objects.equals(form.getListingStatus(), 2) ? "应用已上架" : "应用已下架");
    }

    /**
     * 查询当前用户最近访问的去重应用。
     */
    private List<Map<String, Object>> recentInternal(RequestEmployee employee, int limit) {
        Set<Long> favoriteIds = queryFavoriteIds(employee);
        LinkedHashSet<Long> applicationIds = new LinkedHashSet<>();
        visitLogDao.selectList(new LambdaQueryWrapper<ApplicationVisitLogEntity>()
                        .eq(ApplicationVisitLogEntity::getEmployeeId, employee.getEmployeeId())
                        .eq(ApplicationVisitLogEntity::getSuccessFlag, true)
                        .orderByDesc(ApplicationVisitLogEntity::getVisitTime)
                        .last("limit 100"))
                .forEach(item -> applicationIds.add(item.getApplicationId()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long applicationId : applicationIds) {
            ApplicationEntity application = applicationDao.selectById(applicationId);
            if (canView(application, employee)) {
                result.add(toPortalMap(application, favoriteIds.contains(applicationId), employee));
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 判断应用是否满足市场查询筛选条件。
     */
    private boolean matchesPortalFilter(ApplicationEntity application, ApplicationPortalQueryForm form,
                                        Set<Long> favoriteIds) {
        Map<String, Object> listing = readJson(application.getListingConfig());
        Map<String, Object> publish = readJson(application.getPublishConfig());
        if (!readBoolean(publish, "searchable", true)) {
            return false;
        }
        if (StringUtils.isNotBlank(form.getCategoryName())
                && !Objects.equals(form.getCategoryName(), Objects.toString(listing.get("category"), "未分类"))) {
            return false;
        }
        if (Boolean.TRUE.equals(form.getFavoriteOnly()) && !favoriteIds.contains(application.getApplicationId())) {
            return false;
        }
        return !Boolean.TRUE.equals(form.getRecommendOnly())
                || StringUtils.isNotBlank(Objects.toString(publish.get("recommendTag"), ""));
    }

    /**
     * 判断当前用户是否具备应用可见权限。
     */
    private boolean canView(ApplicationEntity application, RequestEmployee employee) {
        if (application == null || !Objects.equals(application.getListingStatus(), 2)
                || !Objects.equals(application.getAccessStatus(), 2)) {
            return false;
        }
        Map<String, Object> publish = readJson(application.getPublishConfig());
        if (!readBoolean(publish, "portalVisible", true)) {
            return false;
        }
        if (employee != null && Boolean.TRUE.equals(employee.getAdministratorFlag())) {
            return true;
        }
        String scopeType = Objects.toString(publish.get("scopeType"), "ENTERPRISE");
        if (Objects.equals(scopeType, "PUBLIC")) {
            return true;
        }
        if (Objects.equals(scopeType, "ENTERPRISE")) {
            return employee != null && applicationDataScopeService.canManage(application);
        }
        if (!Objects.equals(scopeType, "SELECTED") || employee == null) {
            return false;
        }
        Set<String> organizationNames = stringSet(publish.get("organizationNames"));
        if (organizationNames.contains(employee.getDepartmentName())) {
            return true;
        }
        Set<String> allowedRoles = stringSet(publish.get("roleNames"));
        return roleEmployeeDao.selectRoleByEmployeeId(employee.getEmployeeId()).stream()
                .map(RoleVO::getRoleName)
                .anyMatch(allowedRoles::contains);
    }

    /**
     * 将应用主数据与市场、发布配置组合为门户展示对象。
     */
    private Map<String, Object> toPortalMap(ApplicationEntity application, boolean favorite,
                                            RequestEmployee employee) {
        Map<String, Object> listing = readJson(application.getListingConfig());
        Map<String, Object> publish = readJson(application.getPublishConfig());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("applicationId", application.getApplicationId());
        item.put("applicationName", application.getApplicationName());
        item.put("applicationCode", application.getApplicationCode());
        item.put("applicationType", application.getApplicationType());
        item.put("enterpriseName", application.getEnterpriseName());
        item.put("summary", application.getSummary());
        item.put("iconUrl", application.getIconUrl());
        item.put("homeUrl", application.getHomeUrl());
        item.put("listingStatus", application.getListingStatus());
        item.put("marketName", Objects.toString(listing.get("marketName"), application.getApplicationName()));
        item.put("subtitle", Objects.toString(listing.get("subtitle"), application.getSummary()));
        item.put("category", Objects.toString(listing.get("category"), "未分类"));
        item.put("tags", listing.getOrDefault("tags", List.of()));
        item.put("versionNo", Objects.toString(listing.get("versionNo"), "v1.0.0"));
        item.put("description", listing.get("description"));
        item.put("bannerUrl", listing.get("bannerUrl"));
        item.put("recommendTag", publish.get("recommendTag"));
        item.put("allowFavorite", readBoolean(publish, "allowFavorite", true));
        item.put("favoriteFlag", favorite);
        item.put("openMode", Objects.toString(publish.get("openMode"), "NEW_TAB"));
        item.put("visitCount", visitLogDao.selectCount(new LambdaQueryWrapper<ApplicationVisitLogEntity>()
                .eq(ApplicationVisitLogEntity::getApplicationId, application.getApplicationId())
                .eq(ApplicationVisitLogEntity::getSuccessFlag, true)));
        if (employee != null) {
            ApplicationVisitLogEntity latest = visitLogDao.selectOne(
                    new LambdaQueryWrapper<ApplicationVisitLogEntity>()
                            .eq(ApplicationVisitLogEntity::getApplicationId, application.getApplicationId())
                            .eq(ApplicationVisitLogEntity::getEmployeeId, employee.getEmployeeId())
                            .eq(ApplicationVisitLogEntity::getSuccessFlag, true)
                            .orderByDesc(ApplicationVisitLogEntity::getVisitTime)
                            .last("limit 1"));
            item.put("lastVisitTime", latest == null ? null : latest.getVisitTime());
        }
        return item;
    }

    /**
     * 保存用户进入应用的成功或失败日志。
     */
    private void saveVisit(ApplicationEntity application, RequestEmployee employee, String launchUrl,
                           boolean success, String failureReason) {
        ApplicationVisitLogEntity logEntity = new ApplicationVisitLogEntity();
        logEntity.setApplicationId(application == null ? null : application.getApplicationId());
        logEntity.setApplicationName(application == null ? "未知应用" : application.getApplicationName());
        logEntity.setEmployeeId(employee.getEmployeeId());
        logEntity.setEmployeeName(employee.getActualName());
        logEntity.setDepartmentId(employee.getDepartmentId());
        logEntity.setDepartmentName(employee.getDepartmentName());
        logEntity.setLaunchUrl(launchUrl);
        logEntity.setSuccessFlag(success);
        logEntity.setFailureReason(failureReason);
        logEntity.setIpAddress(employee.getIp());
        logEntity.setUserAgent(StringUtils.abbreviate(employee.getUserAgent(), 500));
        logEntity.setVisitTime(LocalDateTime.now());
        visitLogDao.insert(logEntity);
    }

    /**
     * 创建单点登录用户信息快照。
     */
    private Map<String, Object> buildUserSnapshot(RequestEmployee employee) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employeeId", employee.getEmployeeId());
        result.put("loginName", employee.getLoginName());
        result.put("name", employee.getActualName());
        result.put("avatar", employee.getAvatar());
        result.put("phone", employee.getPhone());
        result.put("email", employee.getEmail());
        result.put("departmentId", employee.getDepartmentId());
        result.put("departmentName", employee.getDepartmentName());
        result.put("positionId", employee.getPositionId());
        result.put("roles", roleEmployeeDao.selectRoleByEmployeeId(employee.getEmployeeId()));
        return result;
    }

    /**
     * 查询当前用户收藏的应用主键集合。
     */
    private Set<Long> queryFavoriteIds(RequestEmployee employee) {
        if (employee == null) {
            return Set.of();
        }
        return new HashSet<>(favoriteDao.selectList(new LambdaQueryWrapper<ApplicationFavoriteEntity>()
                        .eq(ApplicationFavoriteEntity::getEmployeeId, employee.getEmployeeId()))
                .stream().map(ApplicationFavoriteEntity::getApplicationId).toList());
    }

    /**
     * 读取登录配置中的第一个授权回调地址。
     */
    private String firstCallback(Map<String, Object> loginConfig) {
        Object value = loginConfig.get("callbackUrls");
        if (value instanceof List<?> list && !list.isEmpty()) {
            return Objects.toString(list.get(0), null);
        }
        return null;
    }

    /**
     * 查询指定上架状态的应用数量。
     */
    private long countByStatus(Integer status) {
        return applicationDao.selectCount(new LambdaQueryWrapper<ApplicationEntity>()
                .eq(ApplicationEntity::getListingStatus, status));
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
     * 生成高熵、URL安全的一次性授权码。
     */
    private String generateCode() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 计算SHA-256摘要。
     */
    private String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("当前运行环境不支持SHA-256", exception);
        }
    }

    /**
     * 将对象序列化为JSON。
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("用户快照序列化失败", exception);
        }
    }

    /**
     * 将JSON配置转换为Map。
     */
    private Map<String, Object> readJson(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            log.warn("读取应用JSON配置失败", exception);
            return new LinkedHashMap<>();
        }
    }

    /**
     * 将配置值转换为字符串集合。
     */
    private Set<String> stringSet(Object value) {
        if (!(value instanceof List<?> list)) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        list.stream().filter(Objects::nonNull).map(Object::toString).forEach(result::add);
        return result;
    }

    /**
     * 读取布尔配置并提供默认值。
     */
    private boolean readBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        return value instanceof Boolean booleanValue ? booleanValue : defaultValue;
    }

    /**
     * 读取整数配置并提供默认值。
     */
    private int readInt(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
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

    /**
     * 获取当前登录员工，无会话时终止当前业务操作。
     */
    private RequestEmployee requireEmployee() {
        RequestEmployee employee = getRequestEmployee();
        if (employee == null || employee.getEmployeeId() == null) {
            throw new IllegalStateException("当前登录状态已失效");
        }
        return employee;
    }
}
