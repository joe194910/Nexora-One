package com.nexoraone.admin.module.business.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.nexoraone.admin.module.business.ai.domain.entity.AiModelEntity;
import com.nexoraone.admin.module.business.ai.domain.form.AiPlatformForm;
import com.nexoraone.admin.module.business.ai.service.AiPlatformService;
import com.nexoraone.admin.module.business.ai.service.AiRuntimeService;
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

import java.util.Map;

/**
 * 模型管理接口。
 */
@RestController
@RequestMapping("/ai/model")
@Tag(name = "AI 平台-模型管理")
public class AiModelController {

    @Resource
    private AiPlatformService aiPlatformService;
    @Resource
    private AiRuntimeService aiRuntimeService;

    /**
     * 分页查询模型。
     */
    @PostMapping("/query")
    @Operation(summary = "分页查询模型")
    @SaCheckPermission(value = {"ai:model:query", "ai:knowledge:query"}, mode = SaMode.OR)
    public ResponseDTO<PageResult<AiModelEntity>> query(@RequestBody @Valid AiPlatformForm.PageQuery form) {
        return aiPlatformService.queryModels(form);
    }

    /**
     * 查询模型概览。
     */
    @GetMapping("/summary")
    @Operation(summary = "查询模型概览")
    @SaCheckPermission("ai:model:query")
    public ResponseDTO<Map<String, Object>> summary() {
        return aiPlatformService.modelSummary();
    }

    /**
     * 保存模型。
     */
    @PostMapping("/save")
    @Operation(summary = "保存模型")
    @SaCheckPermission("ai:model:save")
    public ResponseDTO<String> save(@RequestBody @Valid AiPlatformForm.ModelSave form) {
        return aiPlatformService.saveModel(form);
    }

    /**
     * 调试模型。
     */
    @PostMapping("/debug")
    @Operation(summary = "调试模型")
    @SaCheckPermission("ai:model:debug")
    public ResponseDTO<Map<String, Object>> debug(@RequestBody @Valid AiPlatformForm.ModelDebug form) {
        return aiRuntimeService.debug(form);
    }

    /**
     * 删除模型。
     */
    @PostMapping("/delete/{modelId}")
    @Operation(summary = "删除模型")
    @SaCheckPermission("ai:model:delete")
    public ResponseDTO<String> delete(@PathVariable Long modelId) {
        return aiPlatformService.deleteModel(modelId);
    }
}
