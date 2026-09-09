package com.nexoraone.admin.module.business.openapi.domain.form;

import com.nexoraone.base.common.domain.PageParam;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * API management page query conditions.
 */
@Data
public class OpenApiQueryForm extends PageParam {

    /** API name or code keyword. */
    @Length(max = 100, message = "搜索内容最多100个字符")
    private String searchWord;
    /** API category. */
    @Length(max = 100, message = "API分类最多100个字符")
    private String categoryName;
    /** HTTP request method. */
    @Length(max = 10, message = "请求方式最多10个字符")
    private String requestMethod;
    /** API status. */
    private Integer status;
}
