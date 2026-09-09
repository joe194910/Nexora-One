package com.nexoraone.admin.module.business.openapi.domain.form;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * API status update form.
 */
@Data
public class OpenApiStatusForm {

    /** Open API primary key. */
    @NotNull(message = "API主键不能为空")
    private Long openApiId;
    /** Target status. */
    @NotNull(message = "目标状态不能为空")
    private Integer status;
}
