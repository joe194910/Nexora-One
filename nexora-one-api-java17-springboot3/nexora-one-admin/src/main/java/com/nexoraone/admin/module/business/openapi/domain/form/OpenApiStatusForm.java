package com.nexoraone.admin.module.business.openapi.domain.form;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * API状态更新表单。
 */
@Data
public class OpenApiStatusForm {

    /** 开放API主键。 */
    @NotNull(message = "API主键不能为空")
    private Long openApiId;
    /** 目标状态。 */
    @NotNull(message = "目标状态不能为空")
    private Integer status;
}
