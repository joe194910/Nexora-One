package com.nexoraone.admin.module.business.application.domain.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 使用一次性授权码换取登录用户信息的参数。
 */
@Data
public class ApplicationSsoTokenForm {

    /** 应用App ID。 */
    @NotBlank(message = "App ID不能为空")
    @JsonProperty("app_id")
    private String appId;
    /** 应用App Secret。 */
    @NotBlank(message = "App Secret不能为空")
    @JsonProperty("app_secret")
    private String appSecret;
    /** 平台签发的一次性授权码。 */
    @NotBlank(message = "授权码不能为空")
    private String code;
    /** 应用配置的授权回调地址。 */
    @JsonProperty("redirect_uri")
    private String redirectUri;
}
