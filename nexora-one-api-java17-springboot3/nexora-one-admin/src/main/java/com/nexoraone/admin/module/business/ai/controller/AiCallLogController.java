package com.nexoraone.admin.module.business.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexoraone.admin.module.business.ai.domain.entity.AiCallLogEntity;
import com.nexoraone.admin.module.business.ai.domain.form.AiPlatformForm;
import com.nexoraone.admin.module.business.ai.service.AiPlatformService;
import com.nexoraone.base.common.domain.PageResult;
import com.nexoraone.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * AI 调用日志接口。
 */
@RestController
@RequestMapping("/ai/call-log")
@Tag(name = "AI 平台-调用日志")
public class AiCallLogController {

    @Resource
    private AiPlatformService aiPlatformService;

    /**
     * 分页查询调用日志。
     */
    @PostMapping("/query")
    @Operation(summary = "分页查询调用日志")
    @SaCheckPermission("ai:call-log:query")
    public ResponseDTO<PageResult<AiCallLogEntity>> query(@RequestBody @Valid AiPlatformForm.CallLogQuery form) {
        return aiPlatformService.queryCallLogs(form);
    }

    /**
     * 查询调用日志摘要。
     */
    @PostMapping("/summary")
    @Operation(summary = "查询调用日志摘要")
    @SaCheckPermission("ai:call-log:query")
    public ResponseDTO<Map<String, Object>> summary(@RequestBody @Valid AiPlatformForm.CallLogQuery form) {
        return aiPlatformService.callLogSummary(form);
    }

    /**
     * 查询调用日志详情。
     */
    @GetMapping("/detail/{callLogId}")
    @Operation(summary = "查询调用日志详情")
    @SaCheckPermission("ai:call-log:detail")
    public ResponseDTO<AiCallLogEntity> detail(@PathVariable Long callLogId) {
        return aiPlatformService.callLogDetail(callLogId);
    }

    /**
     * 导出调用日志 CSV 文件。
     */
    @PostMapping("/export")
    @Operation(summary = "导出调用日志")
    @SaCheckPermission("ai:call-log:export")
    public void export(@RequestBody @Valid AiPlatformForm.CallLogQuery form, HttpServletResponse response)
            throws IOException {
        String fileName = URLEncoder.encode("AI调用日志.csv", StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
        response.getOutputStream().write(aiPlatformService.exportCallLogs(form));
    }
}
