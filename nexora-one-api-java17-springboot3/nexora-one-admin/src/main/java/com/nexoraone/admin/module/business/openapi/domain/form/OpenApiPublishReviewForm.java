package com.nexoraone.admin.module.business.openapi.domain.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * API发布审核表单。
 */
@Data
public class OpenApiPublishReviewForm {

    /** 审核记录主键。 */
    @NotNull(message = "审核记录主键不能为空")
    private Long reviewId;
    /** 审核结果：2审核通过，3审核驳回。 */
    @NotNull(message = "审核结果不能为空")
    @Min(value = 2, message = "审核结果不正确")
    @Max(value = 3, message = "审核结果不正确")
    private Integer reviewStatus;
    /** 审核意见。 */
    @Length(max = 500, message = "审核意见最多500个字符")
    private String reviewRemark;
}
