package com.nexoraone.admin.module.business.application.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 应用客户端凭证模式签发的Access Token。
 */
@Data
@Schema(description = "应用Access Token")
public class ApplicationAccessTokenVO {

    /** Access Token明文，仅在本次签发响应中返回。 */
    @JsonProperty("access_token")
    private String accessToken;

    /** Token类型。 */
    @JsonProperty("token_type")
    private String tokenType;

    /** Token有效期，单位秒。 */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /** OAuth 2.0空格分隔的授权范围。 */
    private String scope;

    /** 便于客户端读取的授权范围列表。 */
    private List<String> scopes;

    /** 应用主键。 */
    private Long applicationId;

    /** 应用名称。 */
    private String applicationName;
}
