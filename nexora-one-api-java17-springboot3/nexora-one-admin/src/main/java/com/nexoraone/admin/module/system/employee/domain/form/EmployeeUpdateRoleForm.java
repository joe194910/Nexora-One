package com.nexoraone.admin.module.system.employee.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 员工更新角色
 *
 * @Author NexoraOne: 罗伊
 * @Date 2021-12-20 20:55:13
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@Data
public class EmployeeUpdateRoleForm {

    @Schema(description = "员工id")
    @NotNull(message = "员工id不能为空")
    private Long employeeId;

    @Schema(description = "角色ids")
    @Size(max = 99, message = "角色最多99")
    private List<Long> roleIdList;

}
