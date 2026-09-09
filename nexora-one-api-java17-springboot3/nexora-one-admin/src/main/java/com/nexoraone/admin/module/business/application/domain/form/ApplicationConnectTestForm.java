package com.nexoraone.admin.module.business.application.domain.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 控制台验证应用凭证和平台连通性的请求参数。
 */
@Data
public class ApplicationConnectTestForm {

    /** 应用主键。 */
    @NotNull(message = "应用主键不能为空")
    private Long applicationId;

    /** App ID。 */
    @NotBlank(message = "App ID不能为空")
    private String appId;

    /** App Secret。 */
    @NotBlank(message = "App Secret不能为空")
    private String appSecret;
}
