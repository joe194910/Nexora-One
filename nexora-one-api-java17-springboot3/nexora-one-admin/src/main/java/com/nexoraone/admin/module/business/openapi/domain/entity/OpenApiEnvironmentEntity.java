package com.nexoraone.admin.module.business.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API发布环境。
 */
@Data
@TableName("nexora_one_open_api_environment")
public class OpenApiEnvironmentEntity {

    /** 环境主键。 */
    @TableId(type = IdType.AUTO)
    private Long environmentId;
    /** API版本主键。 */
    private Long versionId;
    /** 环境编码。 */
    private String environmentCode;
    /** 环境显示名称。 */
    private String environmentName;
    /** 环境基础地址。 */
    private String baseUrl;
    /** 是否启用环境。 */
    private Boolean enabledFlag;
    /** 是否允许在线调试。 */
    private Boolean onlineDebugFlag;
    /** 环境说明。 */
    private String description;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
