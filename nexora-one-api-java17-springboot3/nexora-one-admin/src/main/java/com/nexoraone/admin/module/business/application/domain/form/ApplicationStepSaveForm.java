package com.nexoraone.admin.module.business.application.domain.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 保存应用接入流程步骤配置。
 */
@Data
public class ApplicationStepSaveForm {
    /** 应用主键。 */
    @NotNull(message = "应用主键不能为空")
    private Long applicationId;
    /** 流程步骤：3登录接入，4接口安全，6上架资料，7发布范围。 */
    @NotNull(message = "流程步骤不能为空")
    @Min(value = 3, message = "流程步骤不正确")
    @Max(value = 7, message = "流程步骤不正确")
    private Integer step;
    /** 该步骤结构化配置。 */
    @NotNull(message = "步骤配置不能为空")
    private Map<String, Object> data = new LinkedHashMap<>();
}
