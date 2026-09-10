package com.nexoraone.admin.module.business.openapi.domain.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * API在线调试请求表单。
 */
@Data
public class OpenApiDebugForm {

    /** 调试应用主键。 */
    @NotNull(message = "请选择应用")
    private Long applicationId;
    /** 调试API主键。 */
    @NotNull(message = "请选择API")
    private Long openApiId;
    /** 调试环境编码。 */
    @NotBlank(message = "请选择调试环境")
    private String environmentCode;
    /** 请求参数。 */
    private Map<String, Object> params;
    /** 自定义请求头，不允许传递App Secret。 */
    private Map<String, String> headers;
    /** 请求体。 */
    private String body;
}
