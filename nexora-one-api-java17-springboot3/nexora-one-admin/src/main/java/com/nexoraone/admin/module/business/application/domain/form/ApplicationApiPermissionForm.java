package com.nexoraone.admin.module.business.application.domain.form;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

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
    /** API申请原因。 */
    @Length(max = 500, message = "申请原因最多500个字符")
    private String applyReason;
    /** API使用场景。 */
    @Length(max = 500, message = "使用场景最多500个字符")
    private String useScene;
    /** 申请环境：test测试，prod生产。 */
    private String applyEnvironment;
}
