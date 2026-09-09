package com.nexoraone.base.module.support.helpdoc.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 帮助文档 目录
 *
 * @Author NexoraOne-主任: 卓大
 * @Date 2022-08-20 23:11:42
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@Data
public class HelpDocCatalogUpdateForm extends HelpDocCatalogAddForm {

    @Schema(description = "id")
    @NotNull(message = "id")
    private Long helpDocCatalogId;
}
