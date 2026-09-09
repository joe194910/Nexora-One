package com.nexoraone.admin.module.business.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexoraone.admin.module.business.application.dao.*;
import com.nexoraone.admin.module.business.application.domain.entity.*;
import com.nexoraone.admin.module.business.application.domain.form.*;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationCredentialVO;
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
                .eq(form.getAccessStatus() != null, ApplicationEntity::getAccessStatus, form.getAccessStatus())
                .eq(form.getListingStatus() != null, ApplicationEntity::getListingStatus, form.getListingStatus())
                .orderByDesc(ApplicationEntity::getCreateTime);
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
        RequestEmployee employee = getRequestEmployee();
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
        entity.setWorkflowStep(2);
        entity.setConfigLocked(false);
        entity.setCreateUserId(employee == null ? null : employee.getEmployeeId());
        entity.setCreateUserName(employee == null ? null : employee.getActualName());
        entity.setUpdateUserId(employee == null ? null : employee.getEmployeeId());
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
        RequestEmployee employee = getRequestEmployee();
        entity.setUpdateUserId(employee == null ? null : employee.getEmployeeId());
        applicationDao.updateById(entity);
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
        Map<String, Object> result = toApplicationMap(entity, null);
        result.put("credential", credentialManager.getMaskedCredential(applicationId));
        result.put("loginConfig", readJson(entity.getLoginConfig()));
        result.put("securityConfig", readJson(entity.getSecurityConfig()));
        result.put("listingConfig", readJson(entity.getListingConfig()));
        result.put("publishConfig", readJson(entity.getPublishConfig()));
        result.put("apiPermissions", getApiPermissions(applicationId));
        result.put("reviews", reviewDao.selectList(new LambdaQueryWrapper<ApplicationReviewEntity>()
                .eq(ApplicationReviewEntity::getApplicationId, applicationId)
                .orderByAsc(ApplicationReviewEntity::getCreateTime)));
        result.put("versions", versionDao.selectList(new LambdaQueryWrapper<ApplicationVersionEntity>()
                .eq(ApplicationVersionEntity::getApplicationId, applicationId)
                .orderByDesc(ApplicationVersionEntity::getSubmitTime)));
        result.put("completion", calculateCompletion(entity, applicationId));
        return ResponseDTO.ok(result);
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
        String json = writeJson(form.getData());
        switch (form.getStep()) {
            case 3 -> entity.setLoginConfig(json);
            case 4 -> entity.setSecurityConfig(json);
            case 6 -> entity.setListingConfig(json);
            case 7 -> entity.setPublishConfig(json);
            default -> {
                return ResponseDTO.userErrorParam("不支持的流程步骤");
            }
        }
        entity.setWorkflowStep(Math.max(entity.getWorkflowStep(), form.getStep()));
        RequestEmployee employee = getRequestEmployee();
        entity.setUpdateUserId(employee == null ? null : employee.getEmployeeId());
        applicationDao.updateById(entity);
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
        List<Long> distinctIds = form.getOpenApiIdList().stream().filter(Objects::nonNull).distinct().toList();
        if (!distinctIds.isEmpty()) {
            Long enabledCount = openApiDao.selectCount(new LambdaQueryWrapper<OpenApiEntity>()
                    .in(OpenApiEntity::getOpenApiId, distinctIds)
                    .eq(OpenApiEntity::getEnabledFlag, true));
            if (enabledCount != distinctIds.size()) {
                return ResponseDTO.userErrorParam("申请的API中包含不存在或已停用的接口");
            }
        }
        permissionDao.delete(new LambdaQueryWrapper<ApplicationApiPermissionEntity>()
                .eq(ApplicationApiPermissionEntity::getApplicationId, form.getApplicationId()));
        for (Long openApiId : distinctIds) {
            ApplicationApiPermissionEntity permission = new ApplicationApiPermissionEntity();
            permission.setApplicationId(form.getApplicationId());
            permission.setOpenApiId(openApiId);
            permission.setApplyReason(form.getApplyReason());
            permission.setApplyStatus(1);
            permissionDao.insert(permission);
        }
        entity.setWorkflowStep(Math.max(entity.getWorkflowStep(), 5));
        applicationDao.updateById(entity);
        return ResponseDTO.ok();
    }

    /**
     * 查询可申请的开放API目录。
     */
    public ResponseDTO<List<OpenApiEntity>> queryOpenApiCatalog() {
        return ResponseDTO.ok(openApiDao.selectList(new LambdaQueryWrapper<OpenApiEntity>()
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
        return ResponseDTO.ok(credentialManager.resetSecret(applicationId));
    }

    /**
     * 完整性校验通过后提交审核并生成不可变版本快照。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> submit(ApplicationSubmitForm form) {
        ApplicationEntity entity = applicationDao.selectById(form.getApplicationId());
        ResponseDTO<String> editableResult = checkEditable(entity);
        if (!editableResult.getOk()) {
            return editableResult;
        }
        List<String> missingItems = calculateCompletion(entity, form.getApplicationId()).entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(Map.Entry::getKey)
                .toList();
        if (!missingItems.isEmpty()) {
            return ResponseDTO.userErrorParam("以下配置尚未完成：" + String.join("、", missingItems));
        }
        String versionNo = StringUtils.defaultIfBlank(form.getVersionNo(), resolveVersionNo(entity));
        Long versionCount = versionDao.selectCount(new LambdaQueryWrapper<ApplicationVersionEntity>()
                .eq(ApplicationVersionEntity::getApplicationId, entity.getApplicationId())
                .eq(ApplicationVersionEntity::getVersionNo, versionNo));
        if (versionCount > 0) {
            return ResponseDTO.userErrorParam("该版本已提交，请勿重复操作");
        }

        entity.setListingStatus(1);
        entity.setWorkflowStep(8);
        entity.setConfigLocked(true);
        applicationDao.updateById(entity);

        ApplicationVersionEntity version = new ApplicationVersionEntity();
        version.setApplicationId(entity.getApplicationId());
        version.setVersionNo(versionNo);
        version.setVersionStatus(1);
        version.setConfigSnapshot(writeJson(buildSnapshot(entity)));
        versionDao.insert(version);

        addReview(entity.getApplicationId(), "提交审核", 1,
                StringUtils.defaultIfBlank(form.getSubmitRemark(), "应用配置已完成，等待平台审核"));
        return ResponseDTO.okMsg("应用已提交审核");
    }

    /**
     * 平台管理员处理应用审核，并同步应用和版本状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> review(ApplicationReviewForm form) {
        if (!Objects.equals(form.getReviewStatus(), 2) && !Objects.equals(form.getReviewStatus(), 3)) {
            return ResponseDTO.userErrorParam("审核结果只能为通过或驳回");
        }
        ApplicationEntity entity = applicationDao.selectById(form.getApplicationId());
        if (entity == null) {
            return ResponseDTO.userErrorParam("应用不存在");
        }
        if (!Objects.equals(entity.getListingStatus(), 1)) {
            return ResponseDTO.userErrorParam("当前应用不在审核中");
        }
        boolean approved = Objects.equals(form.getReviewStatus(), 2);
        entity.setListingStatus(approved ? 2 : 3);
        entity.setConfigLocked(approved);
        applicationDao.updateById(entity);

        ApplicationVersionEntity version = versionDao.selectOne(new LambdaQueryWrapper<ApplicationVersionEntity>()
                .eq(ApplicationVersionEntity::getApplicationId, entity.getApplicationId())
                .eq(ApplicationVersionEntity::getVersionStatus, 1)
                .orderByDesc(ApplicationVersionEntity::getSubmitTime)
                .last("limit 1"));
        if (version != null) {
            version.setVersionStatus(approved ? 2 : 3);
            version.setPublishTime(approved ? LocalDateTime.now() : null);
            versionDao.updateById(version);
        }
        permissionDao.update(null, new LambdaUpdateWrapper<ApplicationApiPermissionEntity>()
                .eq(ApplicationApiPermissionEntity::getApplicationId, entity.getApplicationId())
                .set(ApplicationApiPermissionEntity::getApplyStatus, approved ? 2 : 3)
                .set(ApplicationApiPermissionEntity::getReviewRemark, form.getReviewRemark()));
        addReview(entity.getApplicationId(), approved ? "审核通过并发布" : "审核驳回",
                form.getReviewStatus(), form.getReviewRemark());
        return ResponseDTO.okMsg(approved ? "审核通过，应用已发布" : "应用已驳回，可修改后重新提交");
    }

    /**
     * 检查应用是否允许修改。
     */
    private ResponseDTO<String> checkEditable(ApplicationEntity entity) {
        if (entity == null) {
            return ResponseDTO.userErrorParam("应用不存在");
        }
        if (Boolean.TRUE.equals(entity.getConfigLocked())) {
            return ResponseDTO.userErrorParam("应用配置已锁定，当前状态不可修改");
        }
        return ResponseDTO.ok();
    }

    /**
     * 计算提交前各业务步骤的完成情况。
     */
    private LinkedHashMap<String, Boolean> calculateCompletion(ApplicationEntity entity, Long applicationId) {
        LinkedHashMap<String, Boolean> completion = new LinkedHashMap<>();
        completion.put("基本信息", StringUtils.isNoneBlank(entity.getApplicationName(), entity.getApplicationCode(),
                entity.getOwnerName(), entity.getContact(), entity.getSummary()));
        completion.put("应用凭证", credentialManager.getMaskedCredential(applicationId) != null);
        completion.put("接入验证", Objects.equals(entity.getAccessStatus(), 2));
        completion.put("登录接入", StringUtils.isNotBlank(entity.getLoginConfig()));
        completion.put("接口安全", StringUtils.isNotBlank(entity.getSecurityConfig()));
        completion.put("API权限", permissionDao.selectCount(new LambdaQueryWrapper<ApplicationApiPermissionEntity>()
                .eq(ApplicationApiPermissionEntity::getApplicationId, applicationId)) > 0);
        completion.put("上架资料", StringUtils.isNotBlank(entity.getListingConfig()));
        completion.put("发布范围", StringUtils.isNotBlank(entity.getPublishConfig()));
        return completion;
    }

    /**
     * 查询应用已申请的API并合并目录信息。
     */
    private List<Map<String, Object>> getApiPermissions(Long applicationId) {
        List<ApplicationApiPermissionEntity> permissions = permissionDao.selectList(
                new LambdaQueryWrapper<ApplicationApiPermissionEntity>()
                        .eq(ApplicationApiPermissionEntity::getApplicationId, applicationId));
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
        item.put("workflowStep", entity.getWorkflowStep());
        item.put("configLocked", entity.getConfigLocked());
        item.put("appId", credential == null ? null : credential.getAppId());
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
        snapshot.put("credential", credentialManager.getMaskedCredential(entity.getApplicationId()));
        snapshot.put("loginConfig", readJson(entity.getLoginConfig()));
        snapshot.put("securityConfig", readJson(entity.getSecurityConfig()));
        snapshot.put("listingConfig", readJson(entity.getListingConfig()));
        snapshot.put("publishConfig", readJson(entity.getPublishConfig()));
        snapshot.put("apiPermissions", getApiPermissions(entity.getApplicationId()));
        return snapshot;
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
