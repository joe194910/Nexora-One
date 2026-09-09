package com.nexoraone.admin.module.business.application.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用访问凭证。
 */
@Data
@TableName("nexora_one_application_credential")
public class ApplicationCredentialEntity {
    /** 凭证主键。 */
    @TableId(type = IdType.AUTO)
    private Long credentialId;
    /** 应用主键。 */
    private Long applicationId;
    /** 对外使用的App ID。 */
    private String appId;
    /** App Secret的SHA-256摘要。 */
    private String secretHash;
    /** 密钥脱敏提示。 */
    private String secretHint;
    /** 状态：1正常，0失效。 */
    private Integer status;
    /** 密钥版本。 */
    private Integer versionNo;
    /** 最近重置时间。 */
    private LocalDateTime lastResetTime;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
