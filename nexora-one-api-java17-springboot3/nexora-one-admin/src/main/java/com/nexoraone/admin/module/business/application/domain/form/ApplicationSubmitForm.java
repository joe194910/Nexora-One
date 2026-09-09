package com.nexoraone.admin.module.business.application.domain.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 应用提交审核参数。
 */
@Data
public class ApplicationSubmitForm {
    /** 应用主键。 */
    @NotNull(message = "应用主键不能为空")
    private Long applicationId;
    /** 应用版本号。 */
    private String versionNo;
    /** 提交补充说明。 */
    private String submitRemark;
    /** 是否确认信息真实有效并同意锁定配置。 */
    @AssertTrue(message = "请确认信息真实有效并同意锁定配置")
    private Boolean confirmed;
}
