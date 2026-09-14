package com.nexoraone.admin.module.business.application.domain.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 应用预发布参数。
 */
@Data
public class ApplicationPrePublishForm {

    /** 应用主键。 */
    @NotNull(message = "应用主键不能为空")
    private Long applicationId;

    /** 预发布补充说明。 */
    private String submitRemark;

    /** 是否确认当前配置真实有效。 */
    @NotNull(message = "请确认当前配置真实有效")
    @AssertTrue(message = "请确认当前配置真实有效")
    private Boolean confirmed;
}
