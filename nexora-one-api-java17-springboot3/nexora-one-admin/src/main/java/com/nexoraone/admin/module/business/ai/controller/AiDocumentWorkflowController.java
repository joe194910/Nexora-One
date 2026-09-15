package com.nexoraone.admin.module.business.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.nexoraone.admin.module.business.ai.domain.entity.*;
import com.nexoraone.admin.module.business.ai.domain.form.AiDocumentForm;
import com.nexoraone.admin.module.business.ai.service.AiDocumentWorkflowService;
import com.nexoraone.base.common.domain.PageResult;
import com.nexoraone.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** 解析方案、解析服务、知识库及任务业务接口。 */
@RestController
@RequestMapping("/ai/document")
@Tag(name = "AI 平台-文档解析")
public class AiDocumentWorkflowController {
    @Resource private AiDocumentWorkflowService workflow;

    /** 查询解析方案及关联知识库数量。 */
    @GetMapping("/plans") @Operation(summary = "查询解析方案")
    @SaCheckPermission(value = {"ai:parse-plan:query", "ai:knowledge:query", "ai:parse-task:query"}, mode = SaMode.OR)
    public ResponseDTO<List<Map<String, Object>>> plans() { return workflow.plans(); }

    /** 创建或编辑解析方案。 */
    @PostMapping("/plans/save") @Operation(summary = "保存解析方案")
    @SaCheckPermission("ai:parse-plan:save")
    public ResponseDTO<String> savePlan(@RequestBody @Valid AiDocumentForm.PlanSave form) { return workflow.savePlan(form); }

    /** 复制解析方案。 */
    @PostMapping("/plans/{id}/copy") @Operation(summary = "复制解析方案")
    @SaCheckPermission("ai:parse-plan:save")
    public ResponseDTO<String> copyPlan(@PathVariable Long id) { return workflow.copyPlan(id); }

    /** 删除未使用的解析方案。 */
    @PostMapping("/plans/{id}/delete") @Operation(summary = "删除解析方案")
    @SaCheckPermission("ai:parse-plan:save")
    public ResponseDTO<String> deletePlan(@PathVariable Long id) { return workflow.deletePlan(id); }

    /** 上传样本进行方案解析测试。 */
    @PostMapping("/plans/{id}/test") @Operation(summary = "解析方案测试")
    @SaCheckPermission("ai:parse-plan:test")
    public ResponseDTO<Map<String, Object>> testPlan(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return workflow.testPlan(id, file);
    }

    /** 查询解析服务的真实连接状态。 */
    @GetMapping("/services") @Operation(summary = "查询解析服务")
    @SaCheckPermission(value = {"ai:parse-service:query", "ai:parse-plan:query"}, mode = SaMode.OR)
    public ResponseDTO<List<AiParseServiceEntity>> services() { return workflow.services(); }

    /** 保存解析能力服务。 */
    @PostMapping("/services/save") @Operation(summary = "保存解析服务")
    @SaCheckPermission("ai:parse-service:save")
    public ResponseDTO<String> saveService(@RequestBody @Valid AiDocumentForm.ServiceSave form) { return workflow.saveService(form); }

    /** 检查内置实现或外部健康接口。 */
    @PostMapping("/services/{id}/test") @Operation(summary = "测试解析服务")
    @SaCheckPermission("ai:parse-service:test")
    public ResponseDTO<Map<String, Object>> testService(@PathVariable Long id) { return workflow.testService(id); }

    /** 删除未使用解析服务。 */
    @PostMapping("/services/{id}/delete") @Operation(summary = "删除解析服务")
    @SaCheckPermission("ai:parse-service:save")
    public ResponseDTO<String> deleteService(@PathVariable Long id) { return workflow.deleteService(id); }

    /** 查询知识库。 */
    @GetMapping("/knowledge-bases") @Operation(summary = "查询知识库")
    @SaCheckPermission(value = {"ai:knowledge:query", "ai:parse-task:query"}, mode = SaMode.OR)
    public ResponseDTO<List<AiKnowledgeBaseEntity>> knowledgeBases() { return workflow.knowledgeBases(); }

    /** 创建或编辑知识库，必须绑定方案及向量依赖。 */
    @PostMapping("/knowledge-bases/save") @Operation(summary = "保存知识库")
    @SaCheckPermission("ai:knowledge:save")
    public ResponseDTO<String> saveKnowledgeBase(@RequestBody @Valid AiDocumentForm.KnowledgeSave form) {
        return workflow.saveKnowledgeBase(form);
    }

    /** 文档上传后自动生成解析任务。 */
    @PostMapping("/knowledge-bases/{id}/upload") @Operation(summary = "上传知识库文档")
    @SaCheckPermission("ai:knowledge:upload")
    public ResponseDTO<AiParseTaskEntity> upload(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return workflow.upload(id, file);
    }

    /** 查询所有处理任务。 */
    @PostMapping("/tasks/query") @Operation(summary = "查询解析任务")
    @SaCheckPermission("ai:parse-task:query")
    public ResponseDTO<PageResult<AiParseTaskEntity>> tasks(@RequestBody @Valid AiDocumentForm.TaskQuery form) {
        return workflow.tasks(form);
    }

    /** 查看阶段详情与耗时原因。 */
    @GetMapping("/tasks/{id}") @Operation(summary = "查询解析任务详情")
    @SaCheckPermission("ai:parse-task:query")
    public ResponseDTO<Map<String, Object>> taskDetail(@PathVariable Long id) { return workflow.taskDetail(id); }

    /** 取消排队或运行中的任务。 */
    @PostMapping("/tasks/{id}/cancel") @Operation(summary = "取消解析任务")
    @SaCheckPermission("ai:parse-task:operate")
    public ResponseDTO<String> cancel(@PathVariable Long id) { return workflow.cancel(id); }

    /** 重试失败任务。 */
    @PostMapping("/tasks/{id}/retry") @Operation(summary = "重试解析任务")
    @SaCheckPermission("ai:parse-task:operate")
    public ResponseDTO<String> retry(@PathVariable Long id) { return workflow.retry(id); }

    /** 下载任务的源文档。 */
    @GetMapping("/tasks/{id}/download") @Operation(summary = "下载任务文档")
    @SaCheckPermission("ai:parse-task:query")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long id) {
        Path path = workflow.taskFile(id);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + path.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(new FileSystemResource(path));
    }
}
