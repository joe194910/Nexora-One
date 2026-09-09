package com.nexoraone.admin.module.business.application.domain.form;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 保存应用API权限申请。
 */
@Data
public class ApplicationApiPermissionForm {
    /** 应用主键。 */
    @NotNull(message = "应用主键不能为空")
    private Long applicationId;
    /** 申请的开放API主键集合。 */
    private List<Long> openApiIdList = new ArrayList<>();
    /** API使用场景和申请原因。 */
    private String applyReason;
}
