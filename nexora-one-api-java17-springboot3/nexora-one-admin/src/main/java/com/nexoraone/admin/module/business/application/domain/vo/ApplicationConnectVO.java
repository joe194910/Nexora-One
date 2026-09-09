package com.nexoraone.admin.module.business.application.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 应用对接平台后的连通性检查结果。
 */
@Data
public class ApplicationConnectVO {

    /** 是否已经通过真实Token请求完成接入验证。 */
    private Boolean connected;

    /** 应用主键。 */
    private Long applicationId;

    /** 应用名称。 */
    private String applicationName;

    /** App ID。 */
    private String appId;

    /** 当前密钥版本。 */
    private Integer credentialVersion;

    /** Access Token剩余有效期，单位秒。 */
    private Long expiresIn;

    /** 当前Token包含的API授权范围。 */
    private List<String> scopes;

    /** 接入状态：1接入中，2已接入，3接入失败。 */
    private Integer accessStatus;
}
