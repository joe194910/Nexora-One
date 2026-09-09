package com.nexoraone.admin.module.business.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API release environment.
 */
@Data
@TableName("nexora_one_open_api_environment")
public class OpenApiEnvironmentEntity {

    /** Environment primary key. */
    @TableId(type = IdType.AUTO)
    private Long environmentId;
    /** API version primary key. */
    private Long versionId;
    /** Environment code. */
    private String environmentCode;
    /** Environment display name. */
    private String environmentName;
    /** Environment base URL. */
    private String baseUrl;
    /** Whether the environment is enabled. */
    private Boolean enabledFlag;
    /** Whether online debugging is allowed. */
    private Boolean onlineDebugFlag;
    /** Environment description. */
    private String description;
    /** Creation time. */
    private LocalDateTime createTime;
    /** Update time. */
    private LocalDateTime updateTime;
}
