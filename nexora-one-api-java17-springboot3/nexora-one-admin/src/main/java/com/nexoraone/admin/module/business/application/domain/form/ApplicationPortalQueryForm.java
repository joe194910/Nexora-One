package com.nexoraone.admin.module.business.application.domain.form;

import com.nexoraone.base.common.domain.PageParam;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 应用市场分页查询条件。
 */
@Data
public class ApplicationPortalQueryForm extends PageParam {

    /** 应用名称、编码或简介搜索词。 */
    @Length(max = 100, message = "搜索内容最多100个字符")
    private String searchWord;
    /** 市场分类。 */
    @Length(max = 50, message = "应用分类最多50个字符")
    private String categoryName;
    /** 是否仅查询收藏。 */
    private Boolean favoriteOnly;
    /** 是否仅查询推荐应用。 */
    private Boolean recommendOnly;
}
