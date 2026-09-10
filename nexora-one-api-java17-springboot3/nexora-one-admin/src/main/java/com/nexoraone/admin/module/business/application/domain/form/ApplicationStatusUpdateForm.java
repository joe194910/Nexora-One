package com.nexoraone.admin.module.business.application.domain.form;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 应用上架状态更新参数。
 */
@Data
public class ApplicationStatusUpdateForm {

    /** 应用主键。 */
    @NotNull(message = "应用主键不能为空")
    private Long applicationId;
    /** 目标状态：2上架，4下架。 */
    @NotNull(message = "目标状态不能为空")
    private Integer listingStatus;
    /** 操作原因。 */
    @Length(max = 500, message = "操作原因最多500个字符")
    private String reason;
}
