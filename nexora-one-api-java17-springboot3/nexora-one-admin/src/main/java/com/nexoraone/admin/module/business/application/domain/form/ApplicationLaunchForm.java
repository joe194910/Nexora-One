package com.nexoraone.admin.module.business.application.domain.form;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 进入应用参数。
 */
@Data
public class ApplicationLaunchForm {

    /** 应用主键。 */
    @NotNull(message = "应用主键不能为空")
    private Long applicationId;
}
