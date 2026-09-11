package com.nexoraone.admin.module.business.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API请求或响应参数定义。
 */
@Data
@TableName("nexora_one_open_api_parameter")
public class OpenApiParameterEntity {

    /** 参数主键。 */
    @TableId(type = IdType.AUTO)
    private Long parameterId;
    /** API版本主键。 */
    private Long versionId;
    /** 参数方向：1请求，2响应。 */
    private Integer direction;
    /** 参数位置，例如请求头、路径、查询参数、请求体或响应体。 */
    private String location;
    /** 嵌套结构的父参数主键。 */
    private Long parentId;
    /** 参数名称。 */
    private String parameterName;
    /** 中文显示名称。 */
    private String chineseName;
    /** 数据类型。 */
    private String dataType;
    /** 参数是否必填。 */
    private Boolean requiredFlag;
    /** 响应字段是否允许为空。 */
    private Boolean nullableFlag;
    /** 默认值。 */
    private String defaultValue;
    /** 示例值。 */
    private String exampleValue;
    /** 校验规则。 */
    private String validationRule;
    /** 参数说明。 */
    private String description;
    /** 字段是否需要脱敏。 */
    private Boolean maskingFlag;
    /** 显示顺序。 */
    private Integer sort;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
