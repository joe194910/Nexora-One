package com.nexoraone.admin.module.business.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API version definition.
 */
@Data
@TableName("nexora_one_open_api_version")
public class OpenApiVersionEntity {

    /** Version primary key. */
    @TableId(type = IdType.AUTO)
    private Long versionId;
    /** Open API primary key. */
    private Long openApiId;
    /** Semantic version number. */
    private String versionNo;
    /** HTTP request method. */
    private String requestMethod;
    /** Public gateway path. */
    private String gatewayPath;
    /** Internal forwarding path. */
    private String internalPath;
    /** Request content type. */
    private String contentType;
    /** Permission level: 1 public, 2 approval required, 3 sensitive. */
    private Integer permissionLevel;
    /** Request timeout in seconds. */
    private Integer timeoutSeconds;
    /** Version description. */
    private String description;
    /** Whether to wrap responses in the unified response structure. */
    private Boolean unifiedResponseFlag;
    /** Whether response examples and logs require masking. */
    private Boolean dataMaskingFlag;
    /** Reserved JSON security configuration. */
    private String securityConfig;
    /** Version change log. */
    private String changeLog;
    /** Version status: 1 draft, 2 pending, 3 published, 4 disabled, 5 offline. */
    private Integer status;
    /** Whether this version is immutable. */
    private Boolean lockedFlag;
    /** Creator primary key. */
    private Long createUserId;
    /** Creator name. */
    private String createUserName;
    /** Updater primary key. */
    private Long updateUserId;
    /** Updater name. */
    private String updateUserName;
    /** Creation time. */
    private LocalDateTime createTime;
    /** Update time. */
    private LocalDateTime updateTime;
}
