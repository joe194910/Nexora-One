package com.nexoraone.admin.module.business.application.domain.form;

import com.nexoraone.base.common.domain.PageParam;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 应用访问日志分页查询条件。
 */
@Data
public class ApplicationVisitLogQueryForm extends PageParam {

    /** 应用名称或访问用户搜索词。 */
    @Length(max = 100, message = "搜索内容最多100个字符")
    private String searchWord;
    /** 应用主键。 */
    private Long applicationId;
    /** 是否成功。 */
    private Boolean successFlag;
}
