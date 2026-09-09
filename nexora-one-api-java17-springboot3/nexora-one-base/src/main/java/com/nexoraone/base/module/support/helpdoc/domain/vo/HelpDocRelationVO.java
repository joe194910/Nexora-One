package com.nexoraone.base.module.support.helpdoc.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 帮助文档 关联项目
 *
 * @Author NexoraOne-主任: 卓大
 * @Date 2022-08-20 23:11:42
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@Data
public class HelpDocRelationVO {

    @Schema(description = "关联名称")
    private String relationName;

    @Schema(description = "关联id")
    private Long relationId;
}
