package com.nexoraone.admin.module.business.application.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * Redis中保存的应用Access Token安全上下文。
 */
@Data
public class ApplicationAccessContextVO {

    /** 应用主键。 */
    private Long applicationId;

    /** 应用名称。 */
    private String applicationName;

    /** App ID。 */
    private String appId;

    /** 签发Token时使用的密钥版本。 */
    private Integer credentialVersion;

    /** 已授权API编码。 */
    private List<String> scopes;

    /** Token签发时间戳，单位毫秒。 */
    private Long issuedAt;

    /** Token过期时间戳，单位毫秒。 */
    private Long expiresAt;
}
