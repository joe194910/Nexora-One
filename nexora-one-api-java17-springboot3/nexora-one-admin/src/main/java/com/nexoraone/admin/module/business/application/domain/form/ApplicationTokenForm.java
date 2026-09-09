package com.nexoraone.admin.module.business.application.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * App ID与App Secret换取Access Token的请求参数。
 */
@Data
@Schema(description = "应用客户端凭证换取Access Token参数")
public class ApplicationTokenForm {

    /** App ID。 */
    @NotBlank(message = "App ID不能为空")
    @Schema(description = "应用App ID", example = "app_nxo_20260909_abcdef123456")
    private String appId;

    /** App Secret。 */
    @NotBlank(message = "App Secret不能为空")
    @Schema(description = "应用App Secret")
    private String appSecret;

    /** OAuth 2.0授权类型，当前仅支持client_credentials。 */
    @NotBlank(message = "grantType不能为空")
    @Schema(description = "授权类型", example = "client_credentials")
    private String grantType = "client_credentials";
}
