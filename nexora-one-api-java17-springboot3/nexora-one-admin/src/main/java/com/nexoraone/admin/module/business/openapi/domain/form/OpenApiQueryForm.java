package com.nexoraone.admin.module.business.openapi.domain.form;

import com.nexoraone.base.common.domain.PageParam;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * API管理分页查询条件。
 */
@Data
public class OpenApiQueryForm extends PageParam {

    /** API名称或编码关键词。 */
    @Length(max = 100, message = "搜索内容最多100个字符")
    private String searchWord;
    /** API分类。 */
    @Length(max = 100, message = "API分类最多100个字符")
    private String categoryName;
    /** HTTP请求方式。 */
    @Length(max = 10, message = "请求方式最多10个字符")
    private String requestMethod;
    /** API状态。 */
    private Integer status;
}
