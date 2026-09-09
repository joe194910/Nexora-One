package com.nexoraone.admin.module.business.application.domain.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 应用审核处理参数。
 */
@Data
public class ApplicationReviewForm {
    /** 应用主键。 */
    @NotNull(message = "应用主键不能为空")
    private Long applicationId;
    /** 审核结果：2通过，3驳回。 */
    @NotNull(message = "审核结果不能为空")
    private Integer reviewStatus;
    /** 审核说明。 */
    @NotBlank(message = "审核说明不能为空")
    private String reviewRemark;
}
