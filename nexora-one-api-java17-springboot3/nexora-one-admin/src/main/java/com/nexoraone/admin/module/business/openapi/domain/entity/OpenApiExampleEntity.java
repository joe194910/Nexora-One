package com.nexoraone.admin.module.business.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API请求或响应示例。
 */
@Data
@TableName("nexora_one_open_api_example")
public class OpenApiExampleEntity {

    /** 示例主键。 */
    @TableId(type = IdType.AUTO)
    private Long exampleId;
    /** API版本主键。 */
    private Long versionId;
    /** 示例类型，例如请求或响应。 */
    private String exampleType;
    /** 示例显示名称。 */
    private String exampleName;
    /** 示例内容。 */
    private String content;
    /** 显示顺序。 */
    private Integer sort;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
