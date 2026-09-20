package com.nexoraone.admin.module.business.mcp.controller;

import com.nexoraone.admin.module.business.mcp.domain.AiToolForms;
import com.nexoraone.admin.module.business.mcp.service.AiToolInvocationService;
import com.nexoraone.admin.module.business.mcp.service.AiToolService;
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
 * MCP 工具管理接口。
 *
 * <p>权限仅由菜单和页面按钮控制；接口不增加权限注解，但仍受统一登录拦截、
 * API/应用/助手归属校验和工具状态机约束。</p>
 */
@RestController
@RequestMapping("/mcp/tools")
@Tag(name = "MCP 工具管理")
public class AiToolManageController {

    @Resource
    private AiToolService toolService;
    @Resource
    private AiToolInvocationService invocationService;

    /** 分页查询当前用户可管理的 MCP 工具。 */
    @Operation(summary = "分页查询 MCP 工具")
    @PostMapping("/query")
    public ResponseDTO<PageResult<Map<String, Object>>> query(
            @RequestBody @Valid AiToolForms.Query form) {
        return ResponseDTO.ok(toolService.query(form));
    }

    /** 统计当前用户可管理的工具数量及来源分布。 */
    @Operation(summary = "查询 MCP 工具统计")
    @GetMapping("/summary")
    public ResponseDTO<Map<String, Long>> summary() {
        return ResponseDTO.ok(toolService.summary());
    }

    /** 查询当前用户可以选择的应用。 */
    @Operation(summary = "查询可管理应用")
    @GetMapping("/applications")
    public ResponseDTO<List<Map<String, Object>>> applications() {
        return ResponseDTO.ok(toolService.manageableApplications());
    }

    /** 查询指定 MCP 工具的完整详情。 */
    @Operation(summary = "查询 MCP 工具详情")
    @GetMapping("/detail/{toolId}")
    public ResponseDTO<Map<String, Object>> detail(@PathVariable Long toolId) {
        return ResponseDTO.ok(toolService.detail(toolId));
    }

    /** 查询平台 API 与 AI 工具的关联、版本及同步历史。 */
    @Operation(summary = "查询 API 关联的 AI 工具")
    @GetMapping("/platform/{openApiId}")
    public ResponseDTO<Map<String, Object>> platformRelation(@PathVariable Long openApiId) {
        return ResponseDTO.ok(toolService.platformRelation(openApiId));
    }

    /** 预览平台 API 当前线上版本自动生成的输入和输出 Schema。 */
    @Operation(summary = "预览平台 API 自动生成的工具 Schema")
    @GetMapping("/platform/{openApiId}/preview")
    public ResponseDTO<Map<String, Object>> platformPreview(@PathVariable Long openApiId) {
        return ResponseDTO.ok(toolService.platformPreview(openApiId));
    }

    /** 将已稳定上架的平台 API 发布为 AI 工具。 */
    @Operation(summary = "发布平台 API 为 AI 工具")
    @PostMapping("/platform/publish")
    public ResponseDTO<Map<String, Object>> publishPlatform(
            @RequestBody @Valid AiToolForms.PlatformPublish form) {
        return ResponseDTO.ok(toolService.publishPlatform(form));
    }

    /** 手动将工具 Schema 同步到平台 API 的最新已发布版本。 */
    @Operation(summary = "同步平台 API 新版本 Schema")
    @PostMapping("/{toolId}/sync")
    public ResponseDTO<Map<String, Object>> syncPlatform(@PathVariable Long toolId) {
        return ResponseDTO.ok(toolService.syncPlatform(toolId));
    }

    /** 由管理端为指定应用手工登记第三方 HTTP 工具。 */
    @Operation(summary = "管理端手工登记第三方 HTTP 工具")
    @PostMapping("/external/save")
    public ResponseDTO<Map<String, Object>> saveExternal(
            @RequestBody @Valid AiToolForms.AdminExternalSave form) {
        return ResponseDTO.ok(toolService.saveExternal(form));
    }

    /** 使用真实 HTTP 请求测试工具参数、连通性和响应 Schema。 */
    @Operation(summary = "在线测试工具")
    @PostMapping("/test")
    public ResponseDTO<AiToolInvocationService.InvocationResult> test(
            @RequestBody @Valid AiToolForms.TestCall form) {
        return ResponseDTO.ok(invocationService.test(form.getToolId(), form.getArguments()));
    }

    /** 审核工具并确定最终风险等级与确认策略。 */
    @Operation(summary = "审核工具")
    @PostMapping("/review")
    public ResponseDTO<Map<String, Object>> review(
            @RequestBody @Valid AiToolForms.Review form) {
        return ResponseDTO.ok(toolService.review(form));
    }

    /** 启用或停用已经审核通过的工具。 */
    @Operation(summary = "启用或停用工具")
    @PostMapping("/status")
    public ResponseDTO<Map<String, Object>> updateStatus(
            @RequestBody @Valid AiToolForms.StatusUpdate form) {
        return ResponseDTO.ok(toolService.updateStatus(form));
    }

    /** 查询当前用户可配置到智能助手的可用工具。 */
    @Operation(summary = "查询助手可选工具")
    @GetMapping("/assistant/available")
    public ResponseDTO<List<Map<String, Object>>> availableForAssistant() {
        return ResponseDTO.ok(toolService.availableForAssistant());
    }

    /** 查询指定智能助手当前关联的工具。 */
    @Operation(summary = "查询助手已关联工具")
    @GetMapping("/assistant/{assistantId}")
    public ResponseDTO<List<Map<String, Object>>> assistantTools(
            @PathVariable Long assistantId) {
        return ResponseDTO.ok(toolService.assistantTools(assistantId));
    }

    /** 保存智能助手的工具关联和单轮调用安全配置。 */
    @Operation(summary = "保存助手工具关联与安全设置")
    @PostMapping("/assistant/bind")
    public ResponseDTO<List<Map<String, Object>>> bindAssistant(
            @RequestBody @Valid AiToolForms.AssistantBind form) {
        return ResponseDTO.ok(toolService.bindAssistant(form));
    }

    /** 处理本人发起且等待确认的工具调用。 */
    @Operation(summary = "确认或拒绝工具调用")
    @PostMapping("/calls/confirm")
    public ResponseDTO<AiToolInvocationService.InvocationResult> confirm(
            @RequestBody @Valid AiToolForms.Confirm form) {
        return ResponseDTO.ok(invocationService.confirm(
                form.getRequestId(), form.getApproved()));
    }

    /** 查询本人发起的工具调用结果。 */
    @Operation(summary = "查询工具调用结果")
    @GetMapping("/calls/{requestId}")
    public ResponseDTO<AiToolInvocationService.InvocationResult> callResult(
            @PathVariable String requestId) {
        return ResponseDTO.ok(invocationService.callResult(requestId));
    }
}
