package com.nexoraone.admin.module.business.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexoraone.admin.module.business.ai.domain.form.AiPlatformForm;
import com.nexoraone.admin.module.business.ai.service.AiPlatformService;
import com.nexoraone.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 用量统计接口。
 */
@RestController
@RequestMapping("/ai/usage-statistics")
@Tag(name = "AI 平台-用量统计")
public class AiUsageStatisticsController {

    @Resource
    private AiPlatformService aiPlatformService;

    /**
     * 查询用量统计数据。
     */
    @PostMapping("/query")
    @Operation(summary = "查询用量统计数据")
    @SaCheckPermission("ai:statistics:query")
    public ResponseDTO<Map<String, Object>> query(@RequestBody @Valid AiPlatformForm.StatisticsQuery form) {
        return aiPlatformService.statistics(form);
    }
}
