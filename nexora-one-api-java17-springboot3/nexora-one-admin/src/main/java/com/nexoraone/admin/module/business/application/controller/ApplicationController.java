package com.nexoraone.admin.module.business.application.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import com.nexoraone.admin.module.business.application.domain.form.*;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationCredentialVO;
import com.nexoraone.admin.module.business.application.service.ApplicationService;
import com.nexoraone.base.common.domain.PageResult;
import com.nexoraone.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 应用中心接入、配置、上架与审核接口。
 */
@RestController
@RequestMapping("/application")
@Tag(name = "应用中心-应用接入")
public class ApplicationController {

    @Resource
    private ApplicationService applicationService;

    /** 分页查询应用接入列表。 */
    @Operation(summary = "分页查询应用接入列表")
    @PostMapping("/query")
    @SaCheckPermission("application:query")
    public ResponseDTO<PageResult<Map<String, Object>>> query(@RequestBody @Valid ApplicationQueryForm form) {
        return applicationService.query(form);
    }

    /** 创建应用并生成访问凭证。 */
    @Operation(summary = "创建应用并生成访问凭证")
    @PostMapping("/create")
    @SaCheckPermission("application:create")
    public ResponseDTO<Map<String, Object>> create(@RequestBody @Valid ApplicationCreateForm form) {
        return applicationService.create(form);
    }

    /** 更新未锁定应用的基本信息。 */
    @Operation(summary = "更新应用基本信息")
    @PostMapping("/base/update")
    @SaCheckPermission("application:save")
    public ResponseDTO<String> updateBase(@RequestBody @Valid ApplicationBaseUpdateForm form) {
        return applicationService.updateBase(form);
    }

    /** 查询应用完整配置和进度。 */
    @Operation(summary = "查询应用完整配置和进度")
    @GetMapping("/detail/{applicationId}")
    @SaCheckPermission("application:detail")
    public ResponseDTO<Map<String, Object>> detail(@PathVariable Long applicationId) {
        return applicationService.detail(applicationId);
    }

    /** 保存流程步骤配置。 */
    @Operation(summary = "保存流程步骤配置")
    @PostMapping("/step/save")
    @SaCheckPermission("application:save")
    public ResponseDTO<String> saveStep(@RequestBody @Valid ApplicationStepSaveForm form) {
        return applicationService.saveStep(form);
    }

    /** 保存API权限申请。 */
    @Operation(summary = "保存API权限申请")
    @PostMapping("/api-permission/save")
    @SaCheckPermission("application:save")
    public ResponseDTO<String> saveApiPermissions(@RequestBody @Valid ApplicationApiPermissionForm form) {
        return applicationService.saveApiPermissions(form);
    }

    /** 查询开放API目录。 */
    @Operation(summary = "查询开放API目录")
    @GetMapping("/open-api/catalog")
    @SaCheckPermission("application:detail")
    public ResponseDTO<List<OpenApiEntity>> queryOpenApiCatalog() {
        return applicationService.queryOpenApiCatalog();
    }

    /** 重置App Secret。 */
    @Operation(summary = "重置App Secret")
    @PostMapping("/secret/reset/{applicationId}")
    @SaCheckPermission("application:secret:reset")
    public ResponseDTO<ApplicationCredentialVO> resetSecret(@PathVariable Long applicationId) {
        return applicationService.resetSecret(applicationId);
    }

    /** 提交应用上架审核。 */
    @Operation(summary = "提交应用上架审核")
    @PostMapping("/submit")
    @SaCheckPermission("application:submit")
    public ResponseDTO<String> submit(@RequestBody @Valid ApplicationSubmitForm form) {
        return applicationService.submit(form);
    }

    /** 审核应用。 */
    @Operation(summary = "审核应用")
    @PostMapping("/review")
    @SaCheckPermission("application:review")
    public ResponseDTO<String> review(@RequestBody @Valid ApplicationReviewForm form) {
        return applicationService.review(form);
    }
}
