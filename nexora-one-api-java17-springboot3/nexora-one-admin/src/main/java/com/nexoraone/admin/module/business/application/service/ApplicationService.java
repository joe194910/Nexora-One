package com.nexoraone.admin.module.business.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexoraone.admin.module.business.application.dao.*;
import com.nexoraone.admin.module.business.application.domain.entity.*;
import com.nexoraone.admin.module.business.application.domain.form.*;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationCredentialVO;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationSecurityConfigVO;
import com.nexoraone.admin.module.business.application.manager.ApplicationCredentialManager;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 应用中心业务服务。
 */
@Slf4j
@Service
public class ApplicationService {

    /** 草稿状态。 */
    private static final int LISTING_STATUS_DRAFT = 0;
    /** 正式上架审核中。 */
    private static final int LISTING_STATUS_REVIEWING = 1;
    /** 已上架。 */
    private static final int LISTING_STATUS_LISTED = 2;
    /** 正式上架审核已驳回。 */
    private static final int LISTING_STATUS_REJECTED = 3;
    /** 已下架。 */
    private static final int LISTING_STATUS_UNLISTED = 4;
    /** 已预发布，允许使用应用凭证进行接入验证。 */
    private static final int LISTING_STATUS_PRE_PUBLISHED = 5;
    /** 应用版本草稿。 */
    private static final int VERSION_STATUS_DRAFT = 0;
    /** 应用版本审核中。 */
    private static final int VERSION_STATUS_REVIEWING = 1;
    /** 应用版本已发布。 */
    private static final int VERSION_STATUS_PUBLISHED = 2;
    /** 应用版本审核已驳回。 */
    private static final int VERSION_STATUS_REJECTED = 3;
    /** 应用版本已下架。 */
    private static final int VERSION_STATUS_UNLISTED = 4;
    /** 应用版本已预发布。 */
    private static final int VERSION_STATUS_PRE_PUBLISHED = 5;

    @Resource
    private ApplicationDao applicationDao;
    @Resource
    private ApplicationCredentialDao credentialDao;
    @Resource
    private OpenApiDao openApiDao;
    @Resource
    private ApplicationApiPermissionDao permissionDao;
    @Resource
    private ApplicationReviewDao reviewDao;
    @Resource
    private ApplicationVersionDao versionDao;
    @Resource
    private ApplicationCredentialManager credentialManager;
    @Resource
    private ApplicationDataScopeService applicationDataScopeService;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 分页查询应用接入列表。
     */
    public ResponseDTO<PageResult<Map<String, Object>>> query(ApplicationQueryForm form) {
        LambdaQueryWrapper<ApplicationEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(form.getSearchWord())) {
            List<Long> credentialApplicationIds = credentialDao.selectList(
                            new LambdaQueryWrapper<ApplicationCredentialEntity>()
                                    .like(ApplicationCredentialEntity::getAppId, form.getSearchWord())
                                    .eq(ApplicationCredentialEntity::getStatus, 1))
                    .stream().map(ApplicationCredentialEntity::getApplicationId).toList();
            wrapper.and(query -> query
                    .like(ApplicationEntity::getApplicationName, form.getSearchWord())
                    .or().like(ApplicationEntity::getApplicationCode, form.getSearchWord())
                    .or(!credentialApplicationIds.isEmpty(),
                            nested -> nested.in(ApplicationEntity::getApplicationId, credentialApplicationIds)));
        }
        wrapper.eq(form.getApplicationType() != null, ApplicationEntity::getApplicationType, form.getApplicationType())
                .eq(form.getAccessStatus() != null, ApplicationEntity::getAccessStatus, form.getAccessStatus());
        if (Objects.equals(form.getListingStatus(), LISTING_STATUS_LISTED)) {
            wrapper.and(query -> query
                    .eq(ApplicationEntity::getOnlineStatus, LISTING_STATUS_LISTED)
                    .or(legacy -> legacy.isNull(ApplicationEntity::getOnlineStatus)
                            .eq(ApplicationEntity::getListingStatus, LISTING_STATUS_LISTED)));
        } else if (Objects.equals(form.getListingStatus(), LISTING_STATUS_UNLISTED)) {
            wrapper.and(query -> query
                    .eq(ApplicationEntity::getOnlineStatus, LISTING_STATUS_UNLISTED)
                    .or(legacy -> legacy.isNull(ApplicationEntity::getOnlineStatus)
                            .eq(ApplicationEntity::getListingStatus, LISTING_STATUS_UNLISTED)));
        } else if (Objects.equals(form.getListingStatus(), LISTING_STATUS_DRAFT)) {
            wrapper.eq(ApplicationEntity::getListingStatus, LISTING_STATUS_DRAFT)
                    .isNull(ApplicationEntity::getPublishedVersionId);
        } else {
            wrapper.eq(form.getListingStatus() != null,
                    ApplicationEntity::getListingStatus, form.getListingStatus());
        }
        applicationDataScopeService.applyScope(wrapper, "application:review");
        wrapper.orderByDesc(ApplicationEntity::getCreateTime);
        Page<ApplicationEntity> page = applicationDao.selectPage(
                new Page<>(form.getPageNum(), form.getPageSize(), !Boolean.FALSE.equals(form.getSearchCount())), wrapper);

        Map<Long, ApplicationCredentialEntity> credentialMap = queryActiveCredentials(
                page.getRecords().stream().map(ApplicationEntity::getApplicationId).toList());
        List<Map<String, Object>> list = page.getRecords().stream()
                .map(application -> toApplicationMap(application, credentialMap.get(application.getApplicationId())))
                .toList();
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setList(list);
        result.setEmptyFlag(list.isEmpty());
        return ResponseDTO.ok(result);
    }

    /**
     * 创建应用并一次性返回完整App Secret。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Map<String, Object>> create(ApplicationCreateForm form) {
        if (!form.getApplicationCode().matches("^[a-z][a-z0-9-]{3,31}$")) {
            return ResponseDTO.userErrorParam("应用编码需以小写字母开头，仅可包含小写字母、数字和短横线");
        }
        Long count = applicationDao.selectCount(new LambdaQueryWrapper<ApplicationEntity>()
                .eq(ApplicationEntity::getApplicationCode, form.getApplicationCode()));
        if (count > 0) {
            return ResponseDTO.userErrorParam("应用编码已存在");
        }
        RequestEmployee employee = applicationDataScopeService.requireEmployee();
        if (!applicationDataScopeService.canAssignEnterprise(form.getEnterpriseId())) {
            return ResponseDTO.userErrorParam("无权为该企业创建应用");
        }
        ApplicationEntity entity = new ApplicationEntity();
        entity.setApplicationName(form.getApplicationName());
        entity.setApplicationCode(form.getApplicationCode());
        entity.setApplicationType(form.getApplicationType());
        entity.setEnterpriseId(form.getEnterpriseId());
        entity.setEnterpriseName(form.getEnterpriseName());
        entity.setOwnerName(form.getOwnerName());
        entity.setContact(form.getContact());
        entity.setIconUrl(form.getIconUrl());
        entity.setSummary(form.getSummary());
        entity.setHomeUrl(form.getHomeUrl());
        entity.setRemark(form.getRemark());
        entity.setAccessStatus(1);
        entity.setListingStatus(0);
        entity.setOnlineStatus(0);
        entity.setWorkflowStep(2);
        entity.setConfigLocked(false);
        entity.setCreateUserId(employee.getEmployeeId());
        entity.setCreateUserName(employee.getActualName());
        entity.setUpdateUserId(employee.getEmployeeId());
        applicationDao.insert(entity);

        ApplicationCredentialVO credential = credentialManager.createCredential(entity.getApplicationId());
        Map<String, Object> result = toApplicationMap(entity, null);
        result.put("credential", credential);
        return ResponseDTO.ok(result);
    }

    /**
     * 更新应用基本信息，应用编码在创建后保持不变。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> updateBase(ApplicationBaseUpdateForm form) {
        ApplicationEntity entity = applicationDao.selectById(form.getApplicationId());
        ResponseDTO<String> editableResult = checkEditable(entity);
        if (!editableResult.getOk()) {
            return editableResult;
        }
        if (!Objects.equals(entity.getApplicationCode(), form.getApplicationCode())) {
            return ResponseDTO.userErrorParam("应用编码创建后不可修改");
        }
        if (!applicationDataScopeService.canAssignEnterprise(form.getEnterpriseId())) {
            return ResponseDTO.userErrorParam("无权将应用归属到该企业");
        }
        entity.setApplicationName(form.getApplicationName());
        entity.setApplicationType(form.getApplicationType());
        entity.setEnterpriseId(form.getEnterpriseId());
        entity.setEnterpriseName(form.getEnterpriseName());
        entity.setOwnerName(form.getOwnerName());
        entity.setContact(form.getContact());
        entity.setIconUrl(form.getIconUrl());
        entity.setSummary(form.getSummary());
        entity.setHomeUrl(form.getHomeUrl());
        entity.setRemark(form.getRemark());
        entity.setAccessStatus(1);
        RequestEmployee employee = getRequestEmployee();
        entity.setUpdateUserId(employee == null ? null : employee.getEmployeeId());
        applicationDao.updateById(entity);
        syncCurrentVersionSnapshot(entity);
        return ResponseDTO.ok();
    }

    /**
     * 查询应用完整配置、审核记录和版本记录。
     */
    public ResponseDTO<Map<String, Object>> detail(Long applicationId) {
        ApplicationEntity entity = applicationDao.selectById(applicationId);
        if (entity == null) {
            return ResponseDTO.userErrorParam("应用不存在");
        }
        if (!applicationDataScopeService.canManage(entity)
                && !applicationDataScopeService.hasPlatformPermission("application:review")) {
            return ResponseDTO.userErrorParam("应用不存在或无权访问");
        }
        Map<String, Object> result = toApplicationMap(entity, null);
        result.put("credential", credentialManager.getMaskedCredential(applicationId));
        result.put("loginConfig", resolveLoginConfig(entity));
        result.put("securityConfig", readJson(entity.getSecurityConfig()));
        result.put("listingConfig", resolveListingConfig(entity));
        result.put("publishConfig", readJson(entity.getPublishConfig()));
        result.put("currentVersionId", entity.getCurrentVersionId());
        result.put("publishedVersionId", entity.getPublishedVersionId());
        result.put("currentVersion", getVersion(entity.getCurrentVersionId()));
        result.put("publishedVersion", getVersion(entity.getPublishedVersionId()));
        result.put("apiPermissions", getApiPermissions(applicationId));
        result.put("reviews", reviewDao.selectList(new LambdaQueryWrapper<ApplicationReviewEntity>()
                .eq(ApplicationReviewEntity::getApplicationId, applicationId)
                .orderByAsc(ApplicationReviewEntity::getCreateTime)));
        result.put("versions", versionDao.selectList(new LambdaQueryWrapper<ApplicationVersionEntity>()
                .eq(ApplicationVersionEntity::getApplicationId, applicationId)
                .orderByDesc(ApplicationVersionEntity::getVersionId)));
        result.put("prePublishCompletion", calculatePrePublishCompletion(entity, applicationId));
        result.put("completion", calculateCompletion(entity, applicationId));
        return ResponseDTO.ok(result);
    }

    /**
     * 基于当前线上版本创建新的可编辑版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Map<String, Long>> createVersion(ApplicationVersionCreateForm form) {
        ApplicationEntity entity = applicationDao.selectById(form.getApplicationId());
        ResponseDTO<String> editableResult = checkEditable(entity);
        if (!editableResult.getOk()) {
            return ResponseDTO.userErrorParam(editableResult.getMsg());
        }
        if ((!Objects.equals(entity.getListingStatus(), LISTING_STATUS_LISTED)
                && !Objects.equals(entity.getListingStatus(), LISTING_STATUS_UNLISTED))) {
            return ResponseDTO.userErrorParam("只有已发布或已下架的应用可以创建新版本");
        }
        ensurePublishedVersion(entity);
        if (!Objects.equals(entity.getCurrentVersionId(), entity.getPublishedVersionId())) {
            return ResponseDTO.userErrorParam("当前已有未完成的新版本，请先继续配置或完成审核");
        }
        String versionNo = StringUtils.trim(form.getVersionNo());
        Long versionCount = versionDao.selectCount(new LambdaQueryWrapper<ApplicationVersionEntity>()
                .eq(ApplicationVersionEntity::getApplicationId, entity.getApplicationId())
                .eq(ApplicationVersionEntity::getVersionNo, versionNo));
        if (versionCount > 0) {
            return ResponseDTO.userErrorParam("该版本号已存在");
        }

        Map<String, Object> listingConfig = readJson(entity.getListingConfig());
        listingConfig.put("versionNo", versionNo);
        entity.setListingConfig(writeJson(listingConfig));
        entity.setListingStatus(LISTING_STATUS_DRAFT);
        entity.setAccessStatus(1);
        entity.setWorkflowStep(2);
        entity.setConfigLocked(false);

        ApplicationVersionEntity version = new ApplicationVersionEntity();
        version.setApplicationId(entity.getApplicationId());
        version.setVersionNo(versionNo);
        version.setVersionStatus(VERSION_STATUS_DRAFT);
        version.setConfigSnapshot(writeJson(buildSnapshot(entity)));
        versionDao.insert(version);

        entity.setCurrentVersionId(version.getVersionId());
        applicationDao.updateById(entity);
        addReview(entity.getApplicationId(), "创建新版本", VERSION_STATUS_DRAFT,
                "已基于线上版本创建 " + versionNo + " 草稿");
        return ResponseDTO.ok(Map.of("versionId", version.getVersionId()));
    }

    /**
     * 保存登录接入、接口安全、上架资料或发布范围配置。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> saveStep(ApplicationStepSaveForm form) {
        ApplicationEntity entity = applicationDao.selectById(form.getApplicationId());
        ResponseDTO<String> editableResult = checkEditable(entity);
        if (!editableResult.getOk()) {
            return editableResult;
        }
        Object stepData = form.getData();
        String validationMessage = "";
        if (Objects.equals(form.getStep(), 3)) {
            validationMessage = validateLoginConfig(form.getData());
        } else if (Objects.equals(form.getStep(), 4)) {
            ApplicationSecurityConfigVO securityConfig = objectMapper.convertValue(
                    form.getData(), ApplicationSecurityConfigVO.class);
            validationMessage = securityConfig.validate();
            stepData = securityConfig.normalize();
        } else if (Objects.equals(form.getStep(), 6)) {
            validationMessage = validateListingConfig(form.getData());
        } else if (Objects.equals(form.getStep(), 7)) {
            validationMessage = validatePublishConfig(form.getData());
        }
        if (StringUtils.isNotBlank(validationMessage)) {
            return ResponseDTO.userErrorParam(validationMessage);
        }
        String json = writeJson(stepData);
        switch (form.getStep()) {
            case 3 -> {
                entity.setLoginConfig(json);
                entity.setAccessStatus(1);
            }
            case 4 -> {
                entity.setSecurityConfig(json);
                entity.setAccessStatus(1);
            }
            case 6 -> entity.setListingConfig(json);
            case 7 -> entity.setPublishConfig(json);
            default -> {
                return ResponseDTO.userErrorParam("不支持的流程步骤");
            }
        }
        entity.setWorkflowStep(Math.max(Objects.requireNonNullElse(entity.getWorkflowStep(), 1), form.getStep()));
        RequestEmployee employee = getRequestEmployee();
        entity.setUpdateUserId(employee == null ? null : employee.getEmployeeId());
        applicationDao.updateById(entity);
        syncCurrentVersionSnapshot(entity);
        return ResponseDTO.ok();
    }

    /**
     * 保存API权限申请，保存动作与最终授权状态相互独立。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> saveApiPermissions(ApplicationApiPermissionForm form) {
        ApplicationEntity entity = applicationDao.selectById(form.getApplicationId());
        ResponseDTO<String> editableResult = checkEditable(entity);
        if (!editableResult.getOk()) {
            return editableResult;
        }
        List<Long> distinctIds = Objects.requireNonNullElse(form.getOpenApiIdList(), List.<Long>of())
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!distinctIds.isEmpty() && StringUtils.isAnyBlank(
                form.getApplyReason(), form.getUseScene(), form.getApplyEnvironment())) {
            return ResponseDTO.userErrorParam("请选择需要申请的API并完整填写使用场景、申请环境和申请原因");
        }
        if (StringUtils.length(form.getApplyReason()) > 500) {
            return ResponseDTO.userErrorParam("API申请原因不能超过500个字符");
        }
        if (StringUtils.length(form.getUseScene()) > 500) {
            return ResponseDTO.userErrorParam("API使用场景不能超过500个字符");
        }
        if (!distinctIds.isEmpty()
                && !Objects.equals(form.getApplyEnvironment(), "test")
                && !Objects.equals(form.getApplyEnvironment(), "prod")) {
            return ResponseDTO.userErrorParam("API申请环境只能选择测试环境或生产环境");
        }
        String applyReason = StringUtils.trimToEmpty(form.getApplyReason());
        String useScene = StringUtils.trimToEmpty(form.getUseScene());
        String applyEnvironment = StringUtils.trimToEmpty(form.getApplyEnvironment());
        Map<Long, OpenApiEntity> apiMap = Map.of();
        if (!distinctIds.isEmpty()) {
            List<OpenApiEntity> availableApis = openApiDao.selectList(new LambdaQueryWrapper<OpenApiEntity>()
                    .in(OpenApiEntity::getOpenApiId, distinctIds)
                    .eq(OpenApiEntity::getStatus, 4)
                    .eq(OpenApiEntity::getEnabledFlag, true));
            if (availableApis.size() != distinctIds.size()) {
                return ResponseDTO.userErrorParam("申请的API中包含不存在、未上架或已停用的接口");
            }
            apiMap = availableApis.stream()
                    .collect(Collectors.toMap(OpenApiEntity::getOpenApiId, Function.identity()));
        }
        Map<Long, ApplicationApiPermissionEntity> existingMap = permissionDao.selectList(
                        new LambdaQueryWrapper<ApplicationApiPermissionEntity>()
                                .eq(ApplicationApiPermissionEntity::getApplicationId, form.getApplicationId()))
                .stream()
                .collect(Collectors.toMap(ApplicationApiPermissionEntity::getOpenApiId,
                        Function.identity(), (left, right) -> left));
        List<Long> removedIds = existingMap.keySet().stream()
                .filter(openApiId -> !distinctIds.contains(openApiId))
                .toList();
        if (!removedIds.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (Long removedId : removedIds) {
                ApplicationApiPermissionEntity removedPermission = existingMap.get(removedId);
                if (Objects.equals(removedPermission.getApplyStatus(), 4)) {
                    continue;
                }
                boolean granted = Objects.equals(removedPermission.getApplyStatus(), 2);
                removedPermission.setApplyStatus(4);
                removedPermission.setReviewRemark("应用负责人已撤销该API权限申请");
                removedPermission.setDailyQuota(null);
                removedPermission.setEffectiveTime(null);
                removedPermission.setExpireTime(granted ? now : null);
                permissionDao.updateById(removedPermission);
            }
        }
        RequestEmployee employee = getRequestEmployee();
        for (Long openApiId : distinctIds) {
            OpenApiEntity api = apiMap.get(openApiId);
            ApplicationApiPermissionEntity permission = existingMap.get(openApiId);
            boolean insert = permission == null;
            if (insert) {
                permission = new ApplicationApiPermissionEntity();
                permission.setApplicationId(form.getApplicationId());
                permission.setOpenApiId(openApiId);
                permission.setApplicantId(employee == null ? null : employee.getEmployeeId());
                permission.setApplicantName(employee == null ? null : employee.getActualName());
            }
            boolean applicationContentChanged = !Objects.equals(permission.getApplyReason(), applyReason)
                    || !Objects.equals(permission.getUseScene(), useScene)
                    || !Objects.equals(permission.getApplyEnvironment(), applyEnvironment);
            permission.setApplyReason(applyReason);
            permission.setUseScene(useScene);
            permission.setApplyEnvironment(applyEnvironment);
            if (Objects.equals(api.getPermissionLevel(), 1)) {
                permission.setApplyStatus(2);
                permission.setReviewerId(null);
                permission.setReviewerName(null);
                permission.setReviewRemark("公开API自动授权");
                permission.setDailyQuota(null);
                permission.setEffectiveTime(
                        permission.getEffectiveTime() == null ? LocalDateTime.now() : permission.getEffectiveTime());
                permission.setExpireTime(null);
            } else if (insert
                    || Objects.equals(permission.getApplyStatus(), 3)
                    || Objects.equals(permission.getApplyStatus(), 4)
                    || (Objects.equals(permission.getApplyStatus(), 2) && applicationContentChanged)
                    || Objects.equals(permission.getReviewRemark(), "公开API自动授权")) {
                permission.setApplyStatus(1);
                permission.setApplicantId(employee == null ? null : employee.getEmployeeId());
                permission.setApplicantName(employee == null ? null : employee.getActualName());
                permission.setReviewerId(null);
                permission.setReviewerName(null);
                permission.setReviewRemark(null);
                permission.setDailyQuota(null);
                permission.setEffectiveTime(null);
                permission.setExpireTime(null);
            }
            if (insert) {
                permissionDao.insert(permission);
            } else {
                permissionDao.updateById(permission);
            }
        }
        entity.setWorkflowStep(Math.max(Objects.requireNonNullElse(entity.getWorkflowStep(), 1), 5));
        applicationDao.updateById(entity);
        syncCurrentVersionSnapshot(entity);
        return ResponseDTO.ok();
    }

    /**
     * 查询可申请的开放API目录。
     */
    public ResponseDTO<List<OpenApiEntity>> queryOpenApiCatalog() {
        return ResponseDTO.ok(openApiDao.selectList(new LambdaQueryWrapper<OpenApiEntity>()
                .eq(OpenApiEntity::getStatus, 4)
                .eq(OpenApiEntity::getEnabledFlag, true)
                .orderByAsc(OpenApiEntity::getCategoryName)
                .orderByAsc(OpenApiEntity::getSort)));
    }

    /**
     * 重置App Secret，完整新密钥仅在本次响应中返回。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<ApplicationCredentialVO> resetSecret(Long applicationId) {
        ApplicationEntity entity = applicationDao.selectById(applicationId);
        ResponseDTO<String> editableResult = checkEditable(entity);
        if (!editableResult.getOk()) {
            return ResponseDTO.userErrorParam(editableResult.getMsg());
        }
        ApplicationCredentialVO credential = credentialManager.resetSecret(applicationId);
        entity.setAccessStatus(1);
        applicationDao.updateById(entity);
        return ResponseDTO.ok(credential);
    }

    /**
     * 提交应用预发布。
     *
     * <p>预发布后才开放 App ID 和 App Secret 的 Token 换取与接入验证能力。
     * 接入验证本身不作为预发布的前置条件，预发布也不会锁定应用配置。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> prePublish(ApplicationPrePublishForm form) {
        ApplicationEntity entity = applicationDao.selectById(form.getApplicationId());
        ResponseDTO<String> editableResult = checkEditable(entity);
        if (!editableResult.getOk()) {
            return editableResult;
        }
        if (!Objects.equals(entity.getListingStatus(), LISTING_STATUS_DRAFT)
                && !Objects.equals(entity.getListingStatus(), LISTING_STATUS_REJECTED)) {
            return ResponseDTO.userErrorParam("当前应用状态不允许重复提交预发布");
        }
        List<String> missingItems = calculatePrePublishCompletion(entity, form.getApplicationId()).entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(Map.Entry::getKey)
                .toList();
        if (!missingItems.isEmpty()) {
            return ResponseDTO.userErrorParam("以下配置尚未完成：" + String.join("、", missingItems));
        }

        entity.setLoginConfig(writeJson(resolveLoginConfig(entity)));
        entity.setListingConfig(writeJson(resolveListingConfig(entity)));
        entity.setListingStatus(LISTING_STATUS_PRE_PUBLISHED);
        entity.setWorkflowStep(8);
        entity.setConfigLocked(false);
        applicationDao.updateById(entity);
        updateCurrentVersion(entity, VERSION_STATUS_PRE_PUBLISHED, null);
        addReview(entity.getApplicationId(), "提交预发布", 2,
                StringUtils.defaultIfBlank(form.getSubmitRemark(),
                        "应用已完成预发布，等待使用 App ID 和 App Secret 完成接入验证"));
        return ResponseDTO.okMsg("应用已预发布，请使用 App ID 和 App Secret 换取 Access Token 并完成接入验证");
    }

    /**
     * 完整性校验通过后提交正式上架审核并生成不可变版本快照。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> submit(ApplicationSubmitForm form) {
        ApplicationEntity entity = applicationDao.selectById(form.getApplicationId());
        ResponseDTO<String> manageableResult = checkManageable(entity);
        if (!manageableResult.getOk()) {
            return manageableResult;
        }
        if (!Objects.equals(entity.getListingStatus(), LISTING_STATUS_PRE_PUBLISHED)) {
            return ResponseDTO.userErrorParam("请先提交预发布，并完成 App ID 和 App Secret 接入验证");
        }
        List<String> missingItems = calculateCompletion(entity, form.getApplicationId()).entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(Map.Entry::getKey)
                .toList();
        if (!missingItems.isEmpty()) {
            return ResponseDTO.userErrorParam("以下配置尚未完成：" + String.join("、", missingItems));
        }
        String versionNo = StringUtils.defaultIfBlank(form.getVersionNo(), resolveVersionNo(entity));
        ApplicationVersionEntity version = getVersion(entity.getCurrentVersionId());
        if (version != null && !Objects.equals(version.getApplicationId(), entity.getApplicationId())) {
            return ResponseDTO.userErrorParam("当前应用版本数据异常");
        }
        if (version != null && !Objects.equals(version.getVersionNo(), versionNo)) {
            return ResponseDTO.userErrorParam("提交版本号与当前编辑版本不一致");
        }
        if (version == null) {
            version = versionDao.selectOne(new LambdaQueryWrapper<ApplicationVersionEntity>()
                    .eq(ApplicationVersionEntity::getApplicationId, entity.getApplicationId())
                    .eq(ApplicationVersionEntity::getVersionNo, versionNo)
                    .last("limit 1"));
        }
        if (version != null
                && !Objects.equals(version.getVersionStatus(), VERSION_STATUS_DRAFT)
                && !Objects.equals(version.getVersionStatus(), VERSION_STATUS_PRE_PUBLISHED)
                && !Objects.equals(version.getVersionStatus(), VERSION_STATUS_REJECTED)) {
            return ResponseDTO.userErrorParam("该版本已提交，请勿重复操作");
        }

        entity.setListingStatus(LISTING_STATUS_REVIEWING);
        entity.setWorkflowStep(8);
        entity.setConfigLocked(true);

        if (version == null) {
            version = new ApplicationVersionEntity();
            version.setApplicationId(entity.getApplicationId());
            version.setVersionNo(versionNo);
        }
        version.setVersionStatus(VERSION_STATUS_REVIEWING);
        version.setConfigSnapshot(writeJson(buildSnapshot(entity)));
        version.setSubmitTime(LocalDateTime.now());
        version.setPublishTime(null);
        if (version.getVersionId() == null) {
            versionDao.insert(version);
        } else {
            versionDao.updateById(version);
        }
        entity.setCurrentVersionId(version.getVersionId());
        applicationDao.updateById(entity);

        addReview(entity.getApplicationId(), "提交审核", 1,
                StringUtils.defaultIfBlank(form.getSubmitRemark(), "应用配置已完成，等待平台审核"));
        return ResponseDTO.okMsg("应用已提交审核");
    }

    /**
     * 平台管理员处理应用审核，并同步应用和版本状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> review(ApplicationReviewForm form) {
        if (!applicationDataScopeService.hasPlatformPermission("application:review")) {
            return ResponseDTO.userErrorParam("无权审核应用");
        }
        if (!Objects.equals(form.getReviewStatus(), 2) && !Objects.equals(form.getReviewStatus(), 3)) {
            return ResponseDTO.userErrorParam("审核结果只能为通过或驳回");
        }
        ApplicationEntity entity = applicationDao.selectById(form.getApplicationId());
        if (entity == null) {
            return ResponseDTO.userErrorParam("应用不存在");
        }
        if (!Objects.equals(entity.getListingStatus(), LISTING_STATUS_REVIEWING)) {
            return ResponseDTO.userErrorParam("当前应用不在审核中");
        }
        boolean approved = Objects.equals(form.getReviewStatus(), 2);
        ApplicationVersionEntity version = getVersion(entity.getCurrentVersionId());
        if (version == null
                || !Objects.equals(version.getApplicationId(), entity.getApplicationId())
                || !Objects.equals(version.getVersionStatus(), VERSION_STATUS_REVIEWING)) {
            return ResponseDTO.userErrorParam("未找到当前待审核的应用版本");
        }
        ApplicationVersionEntity previousPublishedVersion = approved
                && entity.getPublishedVersionId() != null
                && !Objects.equals(entity.getPublishedVersionId(), version.getVersionId())
                ? getVersion(entity.getPublishedVersionId()) : null;
        entity.setListingStatus(approved ? LISTING_STATUS_LISTED : LISTING_STATUS_REJECTED);
        entity.setConfigLocked(approved);
        if (approved) {
            if (previousPublishedVersion != null
                    && Objects.equals(previousPublishedVersion.getApplicationId(), entity.getApplicationId())) {
                previousPublishedVersion.setVersionStatus(VERSION_STATUS_UNLISTED);
                versionDao.updateById(previousPublishedVersion);
            }
            entity.setPublishedVersionId(version.getVersionId());
            entity.setOnlineStatus(LISTING_STATUS_LISTED);
        }
        version.setVersionStatus(approved ? VERSION_STATUS_PUBLISHED : VERSION_STATUS_REJECTED);
        version.setPublishTime(approved ? LocalDateTime.now() : null);
        if (approved) {
            version.setConfigSnapshot(writeJson(buildSnapshot(entity)));
        }
        versionDao.updateById(version);
        applicationDao.updateById(entity);
        addReview(entity.getApplicationId(), approved ? "审核通过并发布" : "审核驳回",
                form.getReviewStatus(), form.getReviewRemark());
        return ResponseDTO.okMsg(approved ? "审核通过，应用已发布" : "应用已驳回，可修改后重新提交");
    }

    /**
     * 检查应用是否允许修改。
     */
    private ResponseDTO<String> checkEditable(ApplicationEntity entity) {
        ResponseDTO<String> manageableResult = checkManageable(entity);
        if (!manageableResult.getOk()) {
            return manageableResult;
        }
        if (isConfigurationReadOnly(entity)) {
            return ResponseDTO.userErrorParam("应用已接入或已提交上架，当前配置仅支持查看");
        }
        return ResponseDTO.ok();
    }

    /**
     * 检查当前用户是否可以管理指定应用。
     */
    private ResponseDTO<String> checkManageable(ApplicationEntity entity) {
        if (entity == null) {
            return ResponseDTO.userErrorParam("应用不存在");
        }
        if (!canManageApplication(entity)) {
            return ResponseDTO.userErrorParam("应用不存在或无权访问");
        }
        return ResponseDTO.ok();
    }

    /**
     * 已完成接入或已进入正式上架流程的当前版本不可再直接修改。
     */
    private boolean isConfigurationReadOnly(ApplicationEntity entity) {
        return Objects.equals(entity.getAccessStatus(), 2)
                || Boolean.TRUE.equals(entity.getConfigLocked())
                || Objects.equals(entity.getListingStatus(), LISTING_STATUS_PRE_PUBLISHED)
                || Objects.equals(entity.getListingStatus(), LISTING_STATUS_REVIEWING)
                || Objects.equals(entity.getListingStatus(), LISTING_STATUS_LISTED)
                || Objects.equals(entity.getListingStatus(), LISTING_STATUS_UNLISTED);
    }

    /**
     * 判断当前用户是否具备跨应用审核和修正权限。
     */
    private boolean canAdministerApplication() {
        return applicationDataScopeService.hasPlatformPermission("application:review");
    }

    /**
     * 判断当前用户是否可以管理指定应用。
     */
    private boolean canManageApplication(ApplicationEntity entity) {
        return applicationDataScopeService.canManage(entity) || canAdministerApplication();
    }

    /**
     * 计算提交前各业务步骤的完成情况。
     */
    private LinkedHashMap<String, Boolean> calculateCompletion(ApplicationEntity entity, Long applicationId) {
        LinkedHashMap<String, Boolean> completion = calculatePrePublishCompletion(entity, applicationId);
        LinkedHashMap<String, Boolean> result = new LinkedHashMap<>();
        completion.forEach((name, completed) -> {
            result.put(name, completed);
            if ("应用凭证".equals(name)) {
                result.put("接入验证", Objects.equals(entity.getAccessStatus(), 2));
            }
        });
        return result;
    }

    /**
     * 计算预发布前的配置完成情况。
     *
     * <p>接入验证只能在预发布后执行，因此不能作为预发布的前置条件。</p>
     */
    private LinkedHashMap<String, Boolean> calculatePrePublishCompletion(
            ApplicationEntity entity, Long applicationId) {
        LinkedHashMap<String, Boolean> completion = new LinkedHashMap<>();
        completion.put("基本信息", StringUtils.isNoneBlank(entity.getApplicationName(), entity.getApplicationCode(),
                entity.getOwnerName(), entity.getContact(), entity.getSummary()));
        completion.put("应用凭证", credentialManager.getMaskedCredential(applicationId) != null);
        completion.put("登录接入", StringUtils.isBlank(validateLoginConfig(resolveLoginConfig(entity))));
        completion.put("接口安全", isSecurityConfigComplete(entity.getSecurityConfig()));
        completion.put("API权限", entity.getWorkflowStep() != null && entity.getWorkflowStep() >= 5);
        completion.put("上架资料", StringUtils.isBlank(validateListingConfig(resolveListingConfig(entity))));
        completion.put("发布范围", StringUtils.isBlank(validatePublishConfig(readJson(entity.getPublishConfig()))));
        return completion;
    }

    /**
     * 为升级前尚未保存登录配置的应用补齐可用默认值。
     */
    private Map<String, Object> resolveLoginConfig(ApplicationEntity entity) {
        Map<String, Object> config = readJson(entity.getLoginConfig());
        putDefault(config, "protocol", "DIRECT");
        putDefault(config, "homeUrl", entity.getHomeUrl());
        config.putIfAbsent("callbackUrls", List.of());
        putDefault(config, "tokenTtl", 7200);
        putDefault(config, "codeTtl", 60);
        config.putIfAbsent("singleLogout", true);
        config.putIfAbsent("autoCreateUser", true);
        return config;
    }

    /**
     * 为升级前尚未保存上架资料的应用补齐基础展示信息。
     */
    private Map<String, Object> resolveListingConfig(ApplicationEntity entity) {
        Map<String, Object> config = readJson(entity.getListingConfig());
        putDefault(config, "marketName", entity.getApplicationName());
        putDefault(config, "subtitle", entity.getSummary());
        putDefault(config, "category", "企业服务");
        config.putIfAbsent("tags", List.of());
        putDefault(config, "versionNo", "v1.0.0");
        putDefault(config, "releaseNotes", "首次发布");
        putDefault(config, "description", entity.getSummary());
        putDefault(config, "providerName",
                StringUtils.defaultIfBlank(entity.getEnterpriseName(), entity.getOwnerName()));
        putDefault(config, "bannerUrl", entity.getIconUrl());
        config.putIfAbsent("screenshotUrls", List.of());
        return config;
    }

    /**
     * 仅在配置项为空时写入兼容默认值。
     */
    private void putDefault(Map<String, Object> config, String key, Object value) {
        if (StringUtils.isBlank(configString(config, key)) && value != null) {
            config.put(key, value);
        }
    }

    /**
     * 校验登录与单点跳转配置。
     *
     * @param config 登录接入配置
     * @return 校验通过返回空字符串，否则返回错误说明
     */
    private String validateLoginConfig(Map<String, Object> config) {
        String protocol = configString(config, "protocol").toUpperCase(Locale.ROOT);
        if (!Set.of("OIDC", "AUTH_CODE", "DIRECT").contains(protocol)) {
            return "请选择正确的应用接入协议";
        }
        if (!isHttpUrl(configString(config, "homeUrl"))) {
            return "应用首页地址必须是有效的HTTP或HTTPS地址";
        }
        String logoutCallback = configString(config, "logoutCallback");
        if (StringUtils.isNotBlank(logoutCallback) && !isHttpUrl(logoutCallback)) {
            return "单点退出回调地址必须是有效的HTTP或HTTPS地址";
        }
        if (!"DIRECT".equals(protocol)) {
            List<String> callbackUrls = configStringList(config, "callbackUrls");
            if (callbackUrls.isEmpty() || callbackUrls.stream().anyMatch(url -> !isHttpUrl(url))) {
                return "OIDC或授权码接入必须配置有效的授权回调地址";
            }
            Integer codeTtl = configInteger(config, "codeTtl");
            if (codeTtl == null || codeTtl < 30 || codeTtl > 600) {
                return "授权码有效期必须在30到600秒之间";
            }
        }
        Integer tokenTtl = configInteger(config, "tokenTtl");
        if (tokenTtl == null || tokenTtl < 60 || tokenTtl > 86400) {
            return "Token有效期必须在60到86400秒之间";
        }
        return "";
    }

    /**
     * 校验应用市场上架资料。
     *
     * @param config 上架资料配置
     * @return 校验通过返回空字符串，否则返回错误说明
     */
    private String validateListingConfig(Map<String, Object> config) {
        if (StringUtils.isAnyBlank(
                configString(config, "marketName"),
                configString(config, "category"),
                configString(config, "versionNo"),
                configString(config, "description"),
                configString(config, "providerName"))) {
            return "请完善必填的应用上架资料";
        }
        String contactEmail = configString(config, "contactEmail");
        if (StringUtils.isNotBlank(contactEmail)
                && !contactEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return "联系邮箱格式不正确";
        }
        String privacyUrl = configString(config, "privacyUrl");
        if (StringUtils.isNotBlank(privacyUrl) && !isHttpUrl(privacyUrl)) {
            return "隐私政策地址必须是有效的HTTP或HTTPS地址";
        }
        String termsUrl = configString(config, "termsUrl");
        if (StringUtils.isNotBlank(termsUrl) && !isHttpUrl(termsUrl)) {
            return "用户协议地址必须是有效的HTTP或HTTPS地址";
        }
        String helpUrl = configString(config, "helpUrl");
        if (StringUtils.isNotBlank(helpUrl) && !isHttpUrl(helpUrl)) {
            return "帮助文档地址必须是有效的HTTP或HTTPS地址";
        }
        return "";
    }

    /**
     * 校验应用发布范围和门户配置。
     *
     * @param config 发布范围配置
     * @return 校验通过返回空字符串，否则返回错误说明
     */
    private String validatePublishConfig(Map<String, Object> config) {
        String scopeType = configString(config, "scopeType").toUpperCase(Locale.ROOT);
        if (!Set.of("ENTERPRISE", "SELECTED", "PUBLIC").contains(scopeType)) {
            return "请选择正确的应用发布范围";
        }
        if ("SELECTED".equals(scopeType) && configStringList(config, "organizationNames").isEmpty()) {
            return "指定范围发布时至少需要选择一个企业或组织";
        }
        String openMode = configString(config, "openMode").toUpperCase(Locale.ROOT);
        if (!Set.of("NEW_TAB", "CURRENT").contains(openMode)) {
            return "请选择正确的应用打开方式";
        }
        Integer sort = configInteger(config, "sort");
        if (sort == null || sort < 0 || sort > 999999) {
            return "门户排序号必须在0到999999之间";
        }
        return "";
    }

    /**
     * 判断接口安全配置是否完整且可执行。
     */
    private boolean isSecurityConfigComplete(String json) {
        if (StringUtils.isBlank(json)) {
            return false;
        }
        try {
            ApplicationSecurityConfigVO config = objectMapper.convertValue(
                    readJson(json), ApplicationSecurityConfigVO.class);
            return StringUtils.isBlank(config.validate());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * 从配置中读取并清理字符串值。
     */
    private String configString(Map<String, Object> config, String key) {
        Object value = config == null ? null : config.get(key);
        return value == null ? "" : value.toString().trim();
    }

    /**
     * 从配置中读取非空字符串列表。
     */
    private List<String> configStringList(Map<String, Object> config, String key) {
        Object value = config == null ? null : config.get(key);
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    /**
     * 从配置中读取整数值。
     */
    private Integer configInteger(Map<String, Object> config, String key) {
        Object value = config == null ? null : config.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 判断地址是否为带主机名的HTTP或HTTPS地址。
     */
    private boolean isHttpUrl(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            String scheme = StringUtils.lowerCase(uri.getScheme());
            return ("http".equals(scheme) || "https".equals(scheme))
                    && StringUtils.isNotBlank(uri.getHost());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * 查询应用已申请的API并合并目录信息。
     */
    private List<Map<String, Object>> getApiPermissions(Long applicationId) {
        List<ApplicationApiPermissionEntity> permissions = permissionDao.selectList(
                new LambdaQueryWrapper<ApplicationApiPermissionEntity>()
                        .eq(ApplicationApiPermissionEntity::getApplicationId, applicationId)
                        .ne(ApplicationApiPermissionEntity::getApplyStatus, 4));
        if (permissions.isEmpty()) {
            return List.of();
        }
        Map<Long, OpenApiEntity> apiMap = openApiDao.selectBatchIds(
                        permissions.stream().map(ApplicationApiPermissionEntity::getOpenApiId).toList())
                .stream().collect(Collectors.toMap(OpenApiEntity::getOpenApiId, Function.identity()));
        return permissions.stream().map(permission -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("permissionId", permission.getPermissionId());
            item.put("openApiId", permission.getOpenApiId());
            item.put("applyReason", permission.getApplyReason());
            item.put("useScene", permission.getUseScene());
            item.put("applyEnvironment", permission.getApplyEnvironment());
            item.put("applyStatus", permission.getApplyStatus());
            item.put("reviewRemark", permission.getReviewRemark());
            item.put("api", apiMap.get(permission.getOpenApiId()));
            return item;
        }).toList();
    }

    /**
     * 批量查询应用当前有效凭证。
     */
    private Map<Long, ApplicationCredentialEntity> queryActiveCredentials(List<Long> applicationIds) {
        if (applicationIds.isEmpty()) {
            return Map.of();
        }
        return credentialDao.selectList(new LambdaQueryWrapper<ApplicationCredentialEntity>()
                        .in(ApplicationCredentialEntity::getApplicationId, applicationIds)
                        .eq(ApplicationCredentialEntity::getStatus, 1))
                .stream().collect(Collectors.toMap(ApplicationCredentialEntity::getApplicationId,
                        Function.identity(), (left, right) -> left.getVersionNo() >= right.getVersionNo() ? left : right));
    }

    /**
     * 组装应用列表展示数据。
     */
    private Map<String, Object> toApplicationMap(ApplicationEntity entity, ApplicationCredentialEntity credential) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("applicationId", entity.getApplicationId());
        item.put("applicationName", entity.getApplicationName());
        item.put("applicationCode", entity.getApplicationCode());
        item.put("applicationType", entity.getApplicationType());
        item.put("enterpriseId", entity.getEnterpriseId());
        item.put("enterpriseName", entity.getEnterpriseName());
        item.put("ownerName", entity.getOwnerName());
        item.put("contact", entity.getContact());
        item.put("iconUrl", entity.getIconUrl());
        item.put("summary", entity.getSummary());
        item.put("homeUrl", entity.getHomeUrl());
        item.put("remark", entity.getRemark());
        item.put("accessStatus", entity.getAccessStatus());
        item.put("listingStatus", entity.getListingStatus());
        item.put("onlineStatus", resolveOnlineStatus(entity));
        item.put("workflowStep", entity.getWorkflowStep());
        item.put("configLocked", entity.getConfigLocked());
        boolean administrator = canAdministerApplication();
        boolean manageable = applicationDataScopeService.canManage(entity) || administrator;
        boolean configurationReadOnly = isConfigurationReadOnly(entity);
        item.put("editable", manageable && !configurationReadOnly);
        item.put("secretResettable", manageable && !configurationReadOnly);
        item.put("currentVersionId", entity.getCurrentVersionId());
        item.put("publishedVersionId", entity.getPublishedVersionId());
        item.put("onlineFlag", Objects.equals(resolveOnlineStatus(entity), LISTING_STATUS_LISTED));
        item.put("appId", credential == null ? null : credential.getAppId());
        item.put("createUserId", entity.getCreateUserId());
        item.put("createUserName", entity.getCreateUserName());
        item.put("createTime", entity.getCreateTime());
        item.put("updateTime", entity.getUpdateTime());
        return item;
    }

    /**
     * 构建版本配置快照。
     */
    private Map<String, Object> buildSnapshot(ApplicationEntity entity) {
        Map<String, Object> snapshot = toApplicationMap(entity, null);
        snapshot.remove("editable");
        snapshot.remove("secretResettable");
        snapshot.put("credential", credentialManager.getMaskedCredential(entity.getApplicationId()));
        snapshot.put("loginConfig", readJson(entity.getLoginConfig()));
        snapshot.put("securityConfig", readJson(entity.getSecurityConfig()));
        snapshot.put("listingConfig", readJson(entity.getListingConfig()));
        snapshot.put("publishConfig", readJson(entity.getPublishConfig()));
        snapshot.put("apiPermissions", getApiPermissions(entity.getApplicationId()));
        return snapshot;
    }

    /**
     * 获取指定应用版本。
     */
    private ApplicationVersionEntity getVersion(Long versionId) {
        return versionId == null ? null : versionDao.selectById(versionId);
    }

    /**
     * 为升级前已上架或已下架的历史应用补齐线上版本基线。
     */
    private void ensurePublishedVersion(ApplicationEntity entity) {
        if (entity.getPublishedVersionId() != null) {
            return;
        }
        ApplicationVersionEntity version = versionDao.selectOne(
                new LambdaQueryWrapper<ApplicationVersionEntity>()
                        .eq(ApplicationVersionEntity::getApplicationId, entity.getApplicationId())
                        .in(ApplicationVersionEntity::getVersionStatus,
                                VERSION_STATUS_PUBLISHED, VERSION_STATUS_UNLISTED)
                        .orderByDesc(ApplicationVersionEntity::getVersionId)
                        .last("limit 1"));
        if (version == null) {
            version = new ApplicationVersionEntity();
            version.setApplicationId(entity.getApplicationId());
            version.setVersionNo(resolveVersionNo(entity));
            version.setVersionStatus(Objects.equals(entity.getListingStatus(), LISTING_STATUS_UNLISTED)
                    ? VERSION_STATUS_UNLISTED : VERSION_STATUS_PUBLISHED);
            version.setConfigSnapshot(writeJson(buildSnapshot(entity)));
            version.setPublishTime(entity.getUpdateTime() == null ? LocalDateTime.now() : entity.getUpdateTime());
            versionDao.insert(version);
        }
        entity.setCurrentVersionId(version.getVersionId());
        entity.setPublishedVersionId(version.getVersionId());
        entity.setOnlineStatus(Objects.equals(version.getVersionStatus(), VERSION_STATUS_UNLISTED)
                ? LISTING_STATUS_UNLISTED : LISTING_STATUS_LISTED);
        applicationDao.updateById(entity);
    }

    /**
     * 获取独立线上状态，并兼容尚未执行升级脚本的历史数据。
     */
    private Integer resolveOnlineStatus(ApplicationEntity entity) {
        if (entity.getOnlineStatus() != null) {
            return entity.getOnlineStatus();
        }
        if (entity.getPublishedVersionId() != null
                || Objects.equals(entity.getListingStatus(), LISTING_STATUS_LISTED)) {
            return Objects.equals(entity.getListingStatus(), LISTING_STATUS_UNLISTED)
                    ? LISTING_STATUS_UNLISTED : LISTING_STATUS_LISTED;
        }
        return 0;
    }

    /**
     * 将当前可编辑配置同步到新版本快照，线上版本保持不变。
     */
    private void syncCurrentVersionSnapshot(ApplicationEntity entity) {
        if (entity.getCurrentVersionId() == null
                || Objects.equals(entity.getCurrentVersionId(), entity.getPublishedVersionId())) {
            return;
        }
        ApplicationVersionEntity version = getVersion(entity.getCurrentVersionId());
        if (version == null || !Objects.equals(version.getApplicationId(), entity.getApplicationId())) {
            throw new IllegalStateException("当前应用版本不存在或不属于该应用");
        }
        version.setConfigSnapshot(writeJson(buildSnapshot(entity)));
        versionDao.updateById(version);
    }

    /**
     * 更新当前版本状态及其完整配置快照。
     */
    private void updateCurrentVersion(ApplicationEntity entity, int versionStatus, LocalDateTime submitTime) {
        if (entity.getCurrentVersionId() == null) {
            return;
        }
        ApplicationVersionEntity version = getVersion(entity.getCurrentVersionId());
        if (version == null || !Objects.equals(version.getApplicationId(), entity.getApplicationId())) {
            throw new IllegalStateException("当前应用版本不存在或不属于该应用");
        }
        version.setVersionStatus(versionStatus);
        version.setConfigSnapshot(writeJson(buildSnapshot(entity)));
        if (submitTime != null) {
            version.setSubmitTime(submitTime);
        }
        versionDao.updateById(version);
    }

    /**
     * 根据上架资料生成默认版本号。
     */
    private String resolveVersionNo(ApplicationEntity entity) {
        Object value = readJson(entity.getListingConfig()).get("versionNo");
        return value == null || StringUtils.isBlank(value.toString()) ? "v1.0.0" : value.toString();
    }

    /**
     * 新增审核时间线记录。
     */
    private void addReview(Long applicationId, String stage, Integer status, String remark) {
        RequestEmployee employee = getRequestEmployee();
        ApplicationReviewEntity review = new ApplicationReviewEntity();
        review.setApplicationId(applicationId);
        review.setReviewStage(stage);
        review.setReviewStatus(status);
        review.setReviewRemark(remark);
        review.setOperatorId(employee == null ? null : employee.getEmployeeId());
        review.setOperatorName(employee == null ? "系统" : employee.getActualName());
        reviewDao.insert(review);
    }

    /**
     * 获取当前登录员工；无会话场景下返回空，便于任务和测试调用。
     */
    private RequestEmployee getRequestEmployee() {
        try {
            return AdminRequestUtil.getRequestUser();
        } catch (Exception exception) {
            log.debug("未获取到当前登录员工", exception);
            return null;
        }
    }

    /**
     * 将配置对象序列化为JSON。
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("应用配置格式不正确", exception);
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
            log.warn("读取应用配置失败: {}", json, exception);
            return new LinkedHashMap<>();
        }
    }
}
