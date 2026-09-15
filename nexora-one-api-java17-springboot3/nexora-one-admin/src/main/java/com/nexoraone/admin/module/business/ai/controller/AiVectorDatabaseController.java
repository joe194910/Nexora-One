package com.nexoraone.admin.module.business.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.nexoraone.admin.module.business.ai.domain.entity.AiVectorDatabaseEntity;
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

import java.util.Map;

/**
 * 向量数据库管理接口。
 */
@RestController
@RequestMapping("/ai/vector-database")
@Tag(name = "AI 平台-向量数据库")
public class AiVectorDatabaseController {

    @Resource
    private AiPlatformService aiPlatformService;

    /**
     * 查询向量数据库概览。
     */
    @GetMapping("/summary")
    @Operation(summary = "查询向量数据库概览")
    @SaCheckPermission("ai:vector:query")
    public ResponseDTO<Map<String, Object>> summary() {
        return aiPlatformService.vectorDatabaseSummary();
    }

    /**
     * 分页查询向量数据库。
     */
    @PostMapping("/query")
    @Operation(summary = "分页查询向量数据库")
    @SaCheckPermission(value = {"ai:vector:query", "ai:knowledge:query"}, mode = SaMode.OR)
    public ResponseDTO<PageResult<AiVectorDatabaseEntity>> query(@RequestBody @Valid AiPlatformForm.PageQuery form) {
        return aiPlatformService.queryVectorDatabases(form);
    }

    /**
     * 保存向量数据库。
     */
    @PostMapping("/save")
    @Operation(summary = "保存向量数据库")
    @SaCheckPermission("ai:vector:save")
    public ResponseDTO<String> save(@RequestBody @Valid AiPlatformForm.VectorDatabaseSave form) {
        return aiPlatformService.saveVectorDatabase(form);
    }

    /**
     * 测试连接并同步集合。
     */
    @PostMapping("/sync/{vectorDatabaseId}")
    @Operation(summary = "测试连接并同步集合")
    @SaCheckPermission("ai:vector:test")
    public ResponseDTO<Map<String, Object>> sync(@PathVariable Long vectorDatabaseId) {
        return aiPlatformService.syncVectorDatabase(vectorDatabaseId);
    }

    /**
     * 删除向量数据库。
     */
    @PostMapping("/delete/{vectorDatabaseId}")
    @Operation(summary = "删除向量数据库")
    @SaCheckPermission("ai:vector:delete")
    public ResponseDTO<String> delete(@PathVariable Long vectorDatabaseId) {
        return aiPlatformService.deleteVectorDatabase(vectorDatabaseId);
    }
}
