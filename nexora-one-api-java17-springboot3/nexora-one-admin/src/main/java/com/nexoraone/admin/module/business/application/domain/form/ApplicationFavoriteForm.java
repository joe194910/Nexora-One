package com.nexoraone.admin.module.business.application.domain.form;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 应用收藏操作参数。
 */
@Data
public class ApplicationFavoriteForm {

    /** 应用主键。 */
    @NotNull(message = "应用主键不能为空")
    private Long applicationId;
    /** true收藏，false取消收藏。 */
    @NotNull(message = "收藏状态不能为空")
    private Boolean favoriteFlag;
}
