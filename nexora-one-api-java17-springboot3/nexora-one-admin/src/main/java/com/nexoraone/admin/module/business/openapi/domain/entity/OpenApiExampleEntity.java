package com.nexoraone.admin.module.business.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API request or response example.
 */
@Data
@TableName("nexora_one_open_api_example")
public class OpenApiExampleEntity {

    /** Example primary key. */
    @TableId(type = IdType.AUTO)
    private Long exampleId;
    /** API version primary key. */
    private Long versionId;
    /** Example type such as request or response. */
    private String exampleType;
    /** Example display name. */
    private String exampleName;
    /** Example content. */
    private String content;
    /** Display order. */
    private Integer sort;
    /** Creation time. */
    private LocalDateTime createTime;
    /** Update time. */
    private LocalDateTime updateTime;
}
