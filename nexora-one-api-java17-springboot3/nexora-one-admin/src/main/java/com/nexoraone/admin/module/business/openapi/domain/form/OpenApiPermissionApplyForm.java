package com.nexoraone.admin.module.business.openapi.domain.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * API权限申请表单。
 */
@Data
public class OpenApiPermissionApplyForm {

    /** 申请应用主键。 */
    @NotNull(message = "请选择申请应用")
    private Long applicationId;
    /** 开放API主键。 */
    @NotNull(message = "请选择API")
    private Long openApiId;
    /** 申请原因。 */
    @NotBlank(message = "申请原因不能为空")
    @Length(max = 500, message = "申请原因最多500个字符")
    private String applyReason;
    /** 使用场景。 */
    @NotBlank(message = "使用场景不能为空")
    @Length(max = 500, message = "使用场景最多500个字符")
    private String useScene;
    /** 申请环境。 */
    @NotBlank(message = "请选择申请环境")
    private String applyEnvironment;
}
