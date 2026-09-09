package com.nexoraone.admin.module.business.application.domain.form;

import com.nexoraone.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 应用分页查询条件。
 */
@Data
public class ApplicationQueryForm extends PageParam {
    /** 应用名称、编码或App ID搜索词。 */
    @Schema(description = "应用名称、编码或App ID")
    @Length(max = 64, message = "搜索词最多64个字符")
    private String searchWord;
    /** 应用类型。 */
    private Integer applicationType;
    /** 接入状态。 */
    private Integer accessStatus;
    /** 上架状态。 */
    private Integer listingStatus;
}
