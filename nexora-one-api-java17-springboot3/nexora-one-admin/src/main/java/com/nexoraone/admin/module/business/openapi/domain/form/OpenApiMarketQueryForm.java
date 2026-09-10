package com.nexoraone.admin.module.business.openapi.domain.form;

import com.nexoraone.base.common.domain.PageParam;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * API市场分页查询条件。
 */
@Data
public class OpenApiMarketQueryForm extends PageParam {

    /** API名称、编码或能力搜索词。 */
    @Length(max = 100, message = "搜索内容最多100个字符")
    private String searchWord;
    /** API分类。 */
    @Length(max = 100, message = "API分类最多100个字符")
    private String categoryName;
    /** HTTP请求方式。 */
    @Length(max = 10, message = "请求方式最多10个字符")
    private String requestMethod;
    /** 权限级别。 */
    private Integer permissionLevel;
    /** 排序方式：recommend推荐，calls调用量。 */
    private String orderType;
}
