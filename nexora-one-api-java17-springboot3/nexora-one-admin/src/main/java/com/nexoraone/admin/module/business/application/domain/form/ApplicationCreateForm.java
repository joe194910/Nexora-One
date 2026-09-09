package com.nexoraone.admin.module.business.application.domain.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 创建应用基本信息。
 */
@Data
public class ApplicationCreateForm {
    /** 应用名称。 */
    @NotBlank(message = "应用名称不能为空")
    @Length(max = 50, message = "应用名称最多50个字符")
    private String applicationName;
    /** 应用编码，创建后不可修改。 */
    @NotBlank(message = "应用编码不能为空")
    @Length(min = 4, max = 32, message = "应用编码长度为4到32个字符")
    private String applicationCode;
    /** 应用类型：1企业内部应用，2第三方应用。 */
    @NotNull(message = "应用类型不能为空")
    private Integer applicationType;
    /** 所属企业主键。 */
    private Long enterpriseId;
    /** 所属企业名称。 */
    private String enterpriseName;
    /** 应用负责人。 */
    @NotBlank(message = "负责人不能为空")
    @Length(max = 50, message = "负责人最多50个字符")
    private String ownerName;
    /** 负责人联系方式。 */
    @NotBlank(message = "联系方式不能为空")
    @Length(max = 100, message = "联系方式最多100个字符")
    private String contact;
    /** 应用图标地址。 */
    private String iconUrl;
    /** 应用简介。 */
    @NotBlank(message = "应用简介不能为空")
    @Length(max = 500, message = "应用简介最多500个字符")
    private String summary;
    /** 应用首页地址。 */
    private String homeUrl;
    /** 备注。 */
    @Length(max = 200, message = "备注最多200个字符")
    private String remark;
}
