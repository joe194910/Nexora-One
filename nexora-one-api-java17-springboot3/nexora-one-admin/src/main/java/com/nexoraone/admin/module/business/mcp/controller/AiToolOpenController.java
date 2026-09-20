package com.nexoraone.admin.module.business.mcp.controller;

import com.nexoraone.admin.module.business.application.domain.vo.ApplicationConnectVO;
import com.nexoraone.admin.module.business.application.service.ApplicationOpenAuthService;
import com.nexoraone.admin.module.business.mcp.domain.AiToolForms;
import com.nexoraone.admin.module.business.mcp.service.AiToolService;
import com.nexoraone.base.common.annoation.NoNeedLogin;
import com.nexoraone.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 第三方应用复用 Access Token 登记和维护 HTTP 工具。 */
@RestController
@RequestMapping("/open-api/v1/ai-tools")
@Tag(name = "开放平台-AI 工具接入")
public class AiToolOpenController {

    @Resource
    private ApplicationOpenAuthService openAuthService;
    @Resource
    private AiToolService toolService;

    /** 使用应用 Access Token 登记一个第三方 HTTP 工具。 */
    @NoNeedLogin
    @Operation(summary = "登记一个第三方 HTTP 工具")
    @PostMapping
    public ResponseDTO<Map<String, Object>> create(
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Valid AiToolForms.ExternalSave form) {
        ResponseDTO<ApplicationConnectVO> access = authorize(authorization);
        if (!access.getOk()) {
            return ResponseDTO.error(access);
        }
        return ResponseDTO.ok(toolService.registerExternal(
                access.getData().getApplicationId(), form));
    }

    /** 使用应用 Access Token 批量登记第三方 HTTP 工具。 */
    @NoNeedLogin
    @Operation(summary = "批量登记第三方 HTTP 工具")
    @PostMapping("/batch")
    public ResponseDTO<List<Map<String, Object>>> createBatch(
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Valid AiToolForms.ExternalBatch form) {
        ResponseDTO<ApplicationConnectVO> access = authorize(authorization);
        if (!access.getOk()) {
            return ResponseDTO.error(access);
        }
        return ResponseDTO.ok(toolService.registerExternalBatch(
                access.getData().getApplicationId(), form));
    }

    /** 更新当前应用名下的第三方 HTTP 工具，更新后重新进入待审核状态。 */
    @NoNeedLogin
    @Operation(summary = "更新第三方 HTTP 工具")
    @PutMapping("/{toolCode}")
    public ResponseDTO<Map<String, Object>> update(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String toolCode,
            @RequestBody @Valid AiToolForms.ExternalSave form) {
        ResponseDTO<ApplicationConnectVO> access = authorize(authorization);
        if (!access.getOk()) {
            return ResponseDTO.error(access);
        }
        return ResponseDTO.ok(toolService.updateExternal(
                access.getData().getApplicationId(), toolCode, form));
    }

    /** 查询当前应用登记的全部第三方 HTTP 工具。 */
    @NoNeedLogin
    @Operation(summary = "查询当前应用的第三方 HTTP 工具")
    @GetMapping
    public ResponseDTO<List<Map<String, Object>>> list(
            @RequestHeader("Authorization") String authorization) {
        ResponseDTO<ApplicationConnectVO> access = authorize(authorization);
        if (!access.getOk()) {
            return ResponseDTO.error(access);
        }
        return ResponseDTO.ok(toolService.listExternal(
                access.getData().getApplicationId()));
    }

    /** 查询当前应用名下指定第三方 HTTP 工具的详情。 */
    @NoNeedLogin
    @Operation(summary = "查询第三方 HTTP 工具详情")
    @GetMapping("/{toolCode}")
    public ResponseDTO<Map<String, Object>> detail(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String toolCode) {
        ResponseDTO<ApplicationConnectVO> access = authorize(authorization);
        if (!access.getOk()) {
            return ResponseDTO.error(access);
        }
        return ResponseDTO.ok(toolService.externalDetail(
                access.getData().getApplicationId(), toolCode));
    }

    /** 上线审核通过的第三方 HTTP 工具。 */
    @NoNeedLogin
    @Operation(summary = "上线第三方 HTTP 工具")
    @PostMapping("/{toolCode}/online")
    public ResponseDTO<Map<String, Object>> online(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String toolCode) {
        ResponseDTO<ApplicationConnectVO> access = authorize(authorization);
        if (!access.getOk()) {
            return ResponseDTO.error(access);
        }
        return ResponseDTO.ok(toolService.externalStatus(
                access.getData().getApplicationId(), toolCode, true));
    }

    /** 下线当前应用名下的第三方 HTTP 工具。 */
    @NoNeedLogin
    @Operation(summary = "下线第三方 HTTP 工具")
    @PostMapping("/{toolCode}/offline")
    public ResponseDTO<Map<String, Object>> offline(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String toolCode) {
        ResponseDTO<ApplicationConnectVO> access = authorize(authorization);
        if (!access.getOk()) {
            return ResponseDTO.error(access);
        }
        return ResponseDTO.ok(toolService.externalStatus(
                access.getData().getApplicationId(), toolCode, false));
    }

    /** 删除未被智能助手引用的第三方 HTTP 工具。 */
    @NoNeedLogin
    @Operation(summary = "删除第三方 HTTP 工具")
    @DeleteMapping("/{toolCode}")
    public ResponseDTO<String> delete(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String toolCode) {
        ResponseDTO<ApplicationConnectVO> access = authorize(authorization);
        if (!access.getOk()) {
            return ResponseDTO.error(access);
        }
        toolService.deleteExternal(access.getData().getApplicationId(), toolCode);
        return ResponseDTO.ok();
    }

    /** 批量上报当前应用名下第三方 HTTP 工具的在线状态。 */
    @NoNeedLogin
    @Operation(summary = "上报第三方 HTTP 工具心跳")
    @PostMapping("/heartbeat")
    public ResponseDTO<Map<String, Integer>> heartbeat(
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Valid AiToolForms.Heartbeat form) {
        ResponseDTO<ApplicationConnectVO> access = authorize(authorization);
        if (!access.getOk()) {
            return ResponseDTO.error(access);
        }
        int updated = toolService.heartbeat(
                access.getData().getApplicationId(), form);
        return ResponseDTO.ok(Map.of("updated", updated));
    }

    /** 复用开放平台现有 Access Token 完成应用身份校验。 */
    private ResponseDTO<ApplicationConnectVO> authorize(String authorization) {
        return openAuthService.authorize(authorization, null, false);
    }
}
