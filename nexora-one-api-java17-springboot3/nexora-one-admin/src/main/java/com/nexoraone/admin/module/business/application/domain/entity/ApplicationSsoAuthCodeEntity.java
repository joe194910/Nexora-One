package com.nexoraone.admin.module.business.application.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用单点登录一次性授权码。
 */
@Data
@TableName("nexora_one_application_sso_auth_code")
public class ApplicationSsoAuthCodeEntity {

    /** 授权码记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long authCodeId;
    /** 应用主键。 */
    private Long applicationId;
    /** App ID快照。 */
    private String appId;
    /** 授权码SHA-256摘要。 */
    private String codeHash;
    /** 当前登录员工主键。 */
    private Long employeeId;
    /** 用户信息JSON快照。 */
    private String userSnapshot;
    /** 授权回调地址。 */
    private String redirectUri;
    /** 过期时间。 */
    private LocalDateTime expiresTime;
    /** 是否已经使用。 */
    private Boolean usedFlag;
    /** 使用时间。 */
    private LocalDateTime usedTime;
    /** 创建时间。 */
    private LocalDateTime createTime;
}
