package com.nexoraone.admin.module.business.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API business error code definition.
 */
@Data
@TableName("nexora_one_open_api_error_code")
public class OpenApiErrorCodeEntity {

    /** Error code primary key. */
    @TableId(type = IdType.AUTO)
    private Long errorCodeId;
    /** API version primary key. */
    private Long versionId;
    /** HTTP status code. */
    private Integer httpStatus;
    /** Business error code. */
    private String businessCode;
    /** Error message. */
    private String errorMessage;
    /** Trigger condition. */
    private String triggerCondition;
    /** Recommended handling. */
    private String handlingAdvice;
    /** Display order. */
    private Integer sort;
    /** Creation time. */
    private LocalDateTime createTime;
    /** Update time. */
    private LocalDateTime updateTime;
}
