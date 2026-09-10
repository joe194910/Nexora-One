package com.nexoraone.admin.module.business.openapi.domain.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * API权限审核表单。
 */
@Data
public class OpenApiPermissionReviewForm {

    /** 权限申请主键。 */
    @NotNull(message = "权限申请主键不能为空")
    private Long permissionId;
    /** 审核结果：2通过，3驳回。 */
    @NotNull(message = "审核结果不能为空")
    @Min(value = 2, message = "审核结果不正确")
    @Max(value = 3, message = "审核结果不正确")
    private Integer applyStatus;
    /** 审核意见。 */
    @Length(max = 500, message = "审核意见最多500个字符")
    private String reviewRemark;
    /** 每日调用额度。 */
    @Min(value = 1, message = "每日调用额度不能小于1")
    private Long dailyQuota;
    /** 授权有效天数。 */
    @Min(value = 1, message = "授权有效天数不能小于1")
    private Integer effectiveDays;
}
