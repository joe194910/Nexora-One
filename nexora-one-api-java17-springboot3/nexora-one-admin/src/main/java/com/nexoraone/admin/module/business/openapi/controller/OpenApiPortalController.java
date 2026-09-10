package com.nexoraone.admin.module.business.openapi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiDebugForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiMarketQueryForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiPermissionApplyForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiPermissionReviewForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiPublishForm;
import com.nexoraone.admin.module.business.openapi.service.OpenApiPortalService;
import com.nexoraone.base.common.domain.PageResult;
import com.nexoraone.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * API开放平台市场、文档、授权、发布和调试接口。
 */
@RestController
@RequestMapping("/open-api/portal")
@Tag(name = "API开放平台-开放服务")
public class OpenApiPortalController {

    @Resource
    private OpenApiPortalService portalService;
    /** 分页查询API市场。 */
    @Operation(summary = "分页查询API市场")
    @PostMapping("/market/query")
    @SaCheckPermission("open-api:market")
    public ResponseDTO<PageResult<OpenApiEntity>> queryMarket(
            @RequestBody @Valid OpenApiMarketQueryForm form) {
        return portalService.queryMarket(form);
    }

    /** 查询已上架API文档详情。 */
    @Operation(summary = "查询API文档详情")
    @GetMapping("/document/{openApiId}")
    @SaCheckPermission("open-api:document")
    public ResponseDTO<Map<String, Object>> document(@PathVariable Long openApiId) {
        return portalService.document(openApiId);
    }

    /** 查询当前用户可管理的应用。 */
    @Operation(summary = "查询可申请授权的应用")
    @GetMapping("/applications")
    @SaCheckPermission("open-api:grant")
    public ResponseDTO<List<Map<String, Object>>> applications() {
        return portalService.queryApplications();
    }

    /** 提交API权限申请。 */
    @Operation(summary = "提交API权限申请")
    @PostMapping("/permission/apply")
    @SaCheckPermission("open-api:grant")
    public ResponseDTO<String> applyPermission(@RequestBody @Valid OpenApiPermissionApplyForm form) {
        return portalService.applyPermission(form);
    }

    /** 查询权限申请与授权记录。 */
    @Operation(summary = "查询API权限记录")
    @GetMapping("/permission/list")
    @SaCheckPermission("open-api:grant")
    public ResponseDTO<List<Map<String, Object>>> queryPermissions(
            @RequestParam(required = false) Integer applyStatus) {
        return portalService.queryPermissions(applyStatus);
    }

    /** 管理员审核API权限申请。 */
    @Operation(summary = "审核API权限申请")
    @PostMapping("/permission/review")
    @SaCheckPermission("open-api:grant:review")
    public ResponseDTO<String> reviewPermission(@RequestBody @Valid OpenApiPermissionReviewForm form) {
        return portalService.reviewPermission(form);
    }

    /** 查询API发布检查和市场资料。 */
    @Operation(summary = "查询API发布检查")
    @GetMapping("/publish/{openApiId}")
    @SaCheckPermission("open-api:publish")
    public ResponseDTO<Map<String, Object>> publishDetail(@PathVariable Long openApiId) {
        return portalService.publishDetail(openApiId);
    }

    /** 上架并发布API。 */
    @Operation(summary = "上架发布API")
    @PostMapping("/publish")
    @SaCheckPermission("open-api:publish")
    public ResponseDTO<String> publish(@RequestBody @Valid OpenApiPublishForm form) {
        return portalService.publish(form);
    }

    /** 执行测试环境在线调试。 */
    @Operation(summary = "执行API在线调试")
    @PostMapping("/debug")
    @SaCheckPermission("open-api:debug")
    public ResponseDTO<Map<String, Object>> debug(@RequestBody @Valid OpenApiDebugForm form) {
        return portalService.debug(form);
    }

    /** 查询调用统计。 */
    @Operation(summary = "查询API调用统计")
    @GetMapping("/statistics")
    @SaCheckPermission("open-api:statistics")
    public ResponseDTO<Map<String, Object>> statistics() {
        return portalService.statistics();
    }

    /** 查询App ID和App Secret接入指南。 */
    @Operation(summary = "查询开放平台接入指南")
    @GetMapping("/guide")
    @SaCheckPermission("open-api:guide")
    public ResponseDTO<Map<String, Object>> guide() {
        return portalService.guide();
    }
}
