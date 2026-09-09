package com.nexoraone.admin.module.business.application.domain.form;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新应用基本信息。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApplicationBaseUpdateForm extends ApplicationCreateForm {

    /** 应用主键。 */
    @NotNull(message = "应用主键不能为空")
    private Long applicationId;
}
