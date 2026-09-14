package com.nexoraone.admin.module.business.application.domain.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 应用新版本创建表单。
 */
@Data
public class ApplicationVersionCreateForm {

    /** 应用主键。 */
    @NotNull(message = "应用主键不能为空")
    private Long applicationId;

    /** 新版本号。 */
    @NotBlank(message = "新版本号不能为空")
    @Length(max = 20, message = "版本号最多20个字符")
    @Pattern(regexp = "^v\\d+\\.\\d+\\.\\d+$", message = "版本号需使用v1.0.1格式")
    private String versionNo;
}
