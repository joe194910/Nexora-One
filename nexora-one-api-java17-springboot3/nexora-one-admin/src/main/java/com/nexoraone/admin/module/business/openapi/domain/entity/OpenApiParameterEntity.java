package com.nexoraone.admin.module.business.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API request or response parameter definition.
 */
@Data
@TableName("nexora_one_open_api_parameter")
public class OpenApiParameterEntity {

    /** Parameter primary key. */
    @TableId(type = IdType.AUTO)
    private Long parameterId;
    /** API version primary key. */
    private Long versionId;
    /** Direction: 1 request, 2 response. */
    private Integer direction;
    /** Parameter location such as header, path, query, body or response. */
    private String location;
    /** Parent parameter primary key for nested structures. */
    private Long parentId;
    /** Parameter name. */
    private String parameterName;
    /** Chinese display name. */
    private String chineseName;
    /** Data type. */
    private String dataType;
    /** Whether the parameter is required. */
    private Boolean requiredFlag;
    /** Whether the response field may be null. */
    private Boolean nullableFlag;
    /** Default value. */
    private String defaultValue;
    /** Example value. */
    private String exampleValue;
    /** Validation rule. */
    private String validationRule;
    /** Parameter description. */
    private String description;
    /** Whether the field requires masking. */
    private Boolean maskingFlag;
    /** Display order. */
    private Integer sort;
    /** Creation time. */
    private LocalDateTime createTime;
    /** Update time. */
    private LocalDateTime updateTime;
}
