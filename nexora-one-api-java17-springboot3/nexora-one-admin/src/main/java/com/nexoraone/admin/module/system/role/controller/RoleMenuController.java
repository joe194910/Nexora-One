package com.nexoraone.admin.module.system.role.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.nexoraone.admin.constant.AdminSwaggerTagConst;
import com.nexoraone.admin.module.system.role.domain.form.RoleMenuUpdateForm;
import com.nexoraone.admin.module.system.role.domain.vo.RoleMenuTreeVO;
import com.nexoraone.admin.module.system.role.service.RoleMenuService;
import com.nexoraone.base.common.domain.ResponseDTO;
import org.springframework.web.bind.annotation.*;

/**
 * 角色的菜单
 *
 * @Author NexoraOne-主任: 卓大
 * @Date 2022-02-26 21:34:01
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_ROLE_MENU)
public class RoleMenuController {

    @Resource
    private RoleMenuService roleMenuService;

    @Operation(summary = "更新角色权限 @author 卓大")
    @PostMapping("/role/menu/updateRoleMenu")
    @SaCheckPermission("system:role:menu:update")
    public ResponseDTO<String> updateRoleMenu(@Valid @RequestBody RoleMenuUpdateForm updateDTO) {
        return roleMenuService.updateRoleMenu(updateDTO);
    }

    @Operation(summary = "获取角色关联菜单权限 @author 卓大")
    @GetMapping("/role/menu/getRoleSelectedMenu/{roleId}")
    public ResponseDTO<RoleMenuTreeVO> getRoleSelectedMenu(@PathVariable Long roleId) {
        return roleMenuService.getRoleSelectedMenu(roleId);
    }
}
