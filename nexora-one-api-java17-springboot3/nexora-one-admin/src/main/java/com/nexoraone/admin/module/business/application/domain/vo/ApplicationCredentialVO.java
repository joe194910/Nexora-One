package com.nexoraone.admin.module.business.application.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用凭证展示对象。
 */
@Data
public class ApplicationCredentialVO {
    /** App ID。 */
    private String appId;
    /** 仅创建或重置时返回的完整App Secret。 */
    private String appSecret;
    /** 日常详情中返回的脱敏App Secret。 */
    private String maskedSecret;
    /** 密钥版本。 */
    private Integer versionNo;
    /** 凭证创建时间。 */
    private LocalDateTime createTime;
    /** 最近重置时间。 */
    private LocalDateTime lastResetTime;
}
