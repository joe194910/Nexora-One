package com.nexoraone.admin.module.business.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.nexoraone.admin.module.business.ai.domain.entity.AiModelServiceEntity;
import com.nexoraone.admin.module.business.ai.domain.form.AiPlatformForm;
import com.nexoraone.admin.module.business.ai.service.AiPlatformService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 模型服务管理接口。
 */
@RestController
@RequestMapping("/ai/model-service")
@Tag(name = "AI 平台-模型服务")
public class AiModelServiceController {

    @Resource
    private AiPlatformService aiPlatformService;

    /**
     * 分页查询模型服务。
     */
    @PostMapping("/query")
    @Operation(summary = "分页查询模型服务")
    @SaCheckPermission("ai:model-service:query")
    public ResponseDTO<PageResult<AiModelServiceEntity>> query(@RequestBody @Valid AiPlatformForm.PageQuery form) {
        return aiPlatformService.queryModelServices(form);
    }

    /**
     * 查询模型服务概览。
     */
    @GetMapping("/summary")
    @Operation(summary = "查询模型服务概览")
    @SaCheckPermission("ai:model-service:query")
    public ResponseDTO<Map<String, Object>> summary() {
        return aiPlatformService.modelServiceSummary();
    }

    /**
     * 查询模型服务下拉选项。
     */
    @GetMapping("/options")
    @Operation(summary = "查询模型服务选项")
    @SaCheckPermission(value = {"ai:model-service:query", "ai:model:query"}, mode = SaMode.OR)
    public ResponseDTO<List<AiModelServiceEntity>> options() {
        return aiPlatformService.modelServiceOptions();
    }

    /**
     * 保存模型服务。
     */
    @PostMapping("/save")
    @Operation(summary = "保存模型服务")
    @SaCheckPermission("ai:model-service:save")
    public ResponseDTO<String> save(@RequestBody @Valid AiPlatformForm.ModelServiceSave form) {
        return aiPlatformService.saveModelService(form);
    }

    /**
     * 测试模型服务连接。
     */
    @PostMapping("/test/{serviceId}")
    @Operation(summary = "测试模型服务连接")
    @SaCheckPermission("ai:model-service:test")
    public ResponseDTO<Map<String, Object>> test(@PathVariable Long serviceId) {
        return aiPlatformService.testModelService(serviceId);
    }

    /**
     * 删除模型服务。
     */
    @PostMapping("/delete/{serviceId}")
    @Operation(summary = "删除模型服务")
    @SaCheckPermission("ai:model-service:delete")
    public ResponseDTO<String> delete(@PathVariable Long serviceId) {
        return aiPlatformService.deleteModelService(serviceId);
    }
}
