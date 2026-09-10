package com.nexoraone.admin.module.business.openapi.domain.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * API上架发布表单。
 */
@Data
public class OpenApiPublishForm {

    /** 开放API主键。 */
    @NotNull(message = "API主键不能为空")
    private Long openApiId;
    /** API市场标题。 */
    @NotBlank(message = "市场标题不能为空")
    @Length(max = 100, message = "市场标题最多100个字符")
    private String marketTitle;
    /** API市场简介。 */
    @NotBlank(message = "市场简介不能为空")
    @Length(max = 500, message = "市场简介最多500个字符")
    private String marketSummary;
    /** 服务等级说明。 */
    @NotBlank(message = "SLA说明不能为空")
    @Length(max = 500, message = "SLA说明最多500个字符")
    private String slaDescription;
    /** 发布可见范围：1全平台，2指定企业。 */
    @NotNull(message = "发布范围不能为空")
    @Pattern(regexp = "platform|enterprise", message = "发布范围不正确")
    private String publishScope;
}
