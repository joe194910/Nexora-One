package com.nexoraone.admin.module.business.openapi.domain.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * API新版本创建表单。
 */
@Data
public class OpenApiVersionCreateForm {

    /** 开放API主键。 */
    @NotNull(message = "API主键不能为空")
    private Long openApiId;

    /** 新版本号。 */
    @NotBlank(message = "新版本号不能为空")
    @Length(max = 20, message = "新版本号最多20个字符")
    private String versionNo;
}
