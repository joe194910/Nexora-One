package com.nexoraone.admin.module.business.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API业务错误码定义。
 */
@Data
@TableName("nexora_one_open_api_error_code")
public class OpenApiErrorCodeEntity {

    /** 错误码主键。 */
    @TableId(type = IdType.AUTO)
    private Long errorCodeId;
    /** API版本主键。 */
    private Long versionId;
    /** HTTP状态码。 */
    private Integer httpStatus;
    /** 业务错误码。 */
    private String businessCode;
    /** 错误信息。 */
    private String errorMessage;
    /** 触发条件。 */
    private String triggerCondition;
    /** 处理建议。 */
    private String handlingAdvice;
    /** 显示顺序。 */
    private Integer sort;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
