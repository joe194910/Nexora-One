package com.nexoraone.admin.module.system.menu.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 简易的菜单VO
 *
 * @Author NexoraOne: 善逸
 * @Date 2022-03-06 22:04:37
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@Data
public class MenuSimpleTreeVO {

    @Schema(description = "菜单ID")
    private Long menuId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "功能点关联菜单ID")
    private Long contextMenuId;

    @Schema(description = "父级菜单ID")
    private Long parentId;

    @Schema(description = "菜单类型")
    private Integer menuType;

    @Schema(description = "子菜单")
    private List<MenuSimpleTreeVO> children;
}
