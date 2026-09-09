package com.nexoraone.admin.module.business.openapi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiBasicSaveForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiExampleSaveForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiParameterSaveForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiQueryForm;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiStatusForm;
import com.nexoraone.admin.module.business.openapi.service.OpenApiManageService;
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
 * API open platform management endpoints.
 */
@RestController
@RequestMapping("/open-api/manage")
@Tag(name = "API开放平台-API管理")
public class OpenApiManageController {

    @Resource
    private OpenApiManageService openApiManageService;

    /**
     * Query the API management list by page.
     */
    @Operation(summary = "分页查询API管理列表")
    @PostMapping("/query")
    @SaCheckPermission("open-api:query")
    public ResponseDTO<PageResult<OpenApiEntity>> query(@RequestBody @Valid OpenApiQueryForm form) {
        return openApiManageService.query(form);
    }

    /**
     * Query management dashboard statistics.
     */
    @Operation(summary = "查询API统计")
    @GetMapping("/summary")
    @SaCheckPermission("open-api:query")
    public ResponseDTO<Map<String, Long>> summary() {
        return openApiManageService.summary();
    }

    /**
     * Query existing API categories.
     */
    @Operation(summary = "查询API分类")
    @GetMapping("/categories")
    @SaCheckPermission("open-api:query")
    public ResponseDTO<List<String>> categories() {
        return openApiManageService.categories();
    }

    /**
     * Check whether an API code is available.
     */
    @Operation(summary = "校验API编码")
    @GetMapping("/code/check")
    @SaCheckPermission("open-api:query")
    public ResponseDTO<Boolean> checkCode(
            @RequestParam String apiCode,
            @RequestParam(required = false) Long openApiId) {
        return openApiManageService.checkCode(apiCode, openApiId);
    }

    /**
     * Create an API draft and first version.
     */
    @Operation(summary = "创建API")
    @PostMapping("/create")
    @SaCheckPermission("open-api:add")
    public ResponseDTO<Map<String, Long>> create(@RequestBody @Valid OpenApiBasicSaveForm form) {
        return openApiManageService.create(form);
    }

    /**
     * Update basic information of an editable API version.
     */
    @Operation(summary = "更新API基本信息")
    @PostMapping("/basic/update")
    @SaCheckPermission("open-api:save")
    public ResponseDTO<String> updateBasic(@RequestBody @Valid OpenApiBasicSaveForm form) {
        return openApiManageService.updateBasic(form);
    }

    /**
     * Save request or response parameter definitions.
     */
    @Operation(summary = "保存API参数定义")
    @PostMapping("/parameters/save")
    @SaCheckPermission("open-api:save")
    public ResponseDTO<String> saveParameters(@RequestBody @Valid OpenApiParameterSaveForm form) {
        return openApiManageService.saveParameters(form);
    }

    /**
     * Save examples, error codes and version change log.
     */
    @Operation(summary = "保存API示例和错误码")
    @PostMapping("/examples/save")
    @SaCheckPermission("open-api:save")
    public ResponseDTO<String> saveExamples(@RequestBody @Valid OpenApiExampleSaveForm form) {
        return openApiManageService.saveExamples(form);
    }

    /**
     * Query one API and the complete configuration of its current version.
     */
    @Operation(summary = "查询API详情")
    @GetMapping("/detail/{openApiId}")
    @SaCheckPermission("open-api:detail")
    public ResponseDTO<Map<String, Object>> detail(@PathVariable Long openApiId) {
        return openApiManageService.detail(openApiId);
    }

    /**
     * Enable or disable an API.
     */
    @Operation(summary = "变更API状态")
    @PostMapping("/status/update")
    @SaCheckPermission("open-api:status")
    public ResponseDTO<String> updateStatus(@RequestBody @Valid OpenApiStatusForm form) {
        return openApiManageService.updateStatus(form);
    }
}
