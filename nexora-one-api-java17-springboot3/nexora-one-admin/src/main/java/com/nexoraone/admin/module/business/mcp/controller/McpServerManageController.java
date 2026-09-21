package com.nexoraone.admin.module.business.mcp.controller;

import com.nexoraone.admin.module.business.mcp.domain.AiToolForms;
import com.nexoraone.admin.module.business.mcp.service.McpServerService;
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
 * 标准 MCP Server 管理接口。
 *
 * <p>权限仅由菜单和页面按钮控制；接口不增加权限注解，但仍执行登录态和应用归属校验。</p>
 */
@RestController
@RequestMapping("/mcp/servers")
@Tag(name = "标准 MCP Server 管理")
public class McpServerManageController {

    @Resource
    private McpServerService serverService;

    /** 分页查询当前员工可管理的 MCP Server。 */
    @Operation(summary = "分页查询 MCP Server")
    @PostMapping("/query")
    public ResponseDTO<PageResult<Map<String, Object>>> query(
            @RequestBody @Valid AiToolForms.McpServerQuery form) {
        return ResponseDTO.ok(serverService.query(form));
    }

    /** 查询当前员工可管理的 MCP Server 统计。 */
    @Operation(summary = "查询 MCP Server 统计")
    @GetMapping("/summary")
    public ResponseDTO<Map<String, Long>> summary() {
        return ResponseDTO.ok(serverService.summary());
    }

    /** 查询单个 MCP Server 的脱敏配置和已发现工具。 */
    @Operation(summary = "查询 MCP Server 详情")
    @GetMapping("/detail/{serverId}")
    public ResponseDTO<Map<String, Object>> detail(@PathVariable Long serverId) {
        return ResponseDTO.ok(serverService.detail(serverId));
    }

    /** 新增或更新 MCP Server 配置。 */
    @Operation(summary = "保存 MCP Server")
    @PostMapping("/save")
    public ResponseDTO<Map<String, Object>> save(
            @RequestBody @Valid AiToolForms.McpServerSave form) {
        return ResponseDTO.ok(serverService.save(form));
    }

    /** 执行 initialize、tools/list 探活并同步首次发现的工具。 */
    @Operation(summary = "探活并同步 MCP 工具")
    @PostMapping("/{serverId}/probe-sync")
    public ResponseDTO<Map<String, Object>> probeAndSync(@PathVariable Long serverId) {
        return ResponseDTO.ok(serverService.probeAndSync(serverId));
    }

    /** 明确同步单个远端工具的最新 Schema，并让工具重新进入测试审核流程。 */
    @Operation(summary = "同步远端 MCP 工具 Schema")
    @PostMapping("/{serverId}/tools/{toolId}/sync-schema")
    public ResponseDTO<Map<String, Object>> syncToolSchema(
            @PathVariable Long serverId, @PathVariable Long toolId) {
        return ResponseDTO.ok(serverService.syncToolSchema(serverId, toolId));
    }

    /** 启用或停用 MCP Server。 */
    @Operation(summary = "启用或停用 MCP Server")
    @PostMapping("/status")
    public ResponseDTO<Map<String, Object>> updateStatus(
            @RequestBody @Valid AiToolForms.McpServerStatus form) {
        return ResponseDTO.ok(serverService.updateStatus(form));
    }
}
