package com.nexoraone.admin.module.system.role.service;

import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexoraone.admin.module.system.menu.constant.MenuTypeEnum;
import com.nexoraone.admin.module.system.menu.dao.MenuDao;
import com.nexoraone.admin.module.system.menu.domain.entity.MenuEntity;
import com.nexoraone.admin.module.system.menu.domain.vo.MenuSimpleTreeVO;
import com.nexoraone.admin.module.system.menu.domain.vo.MenuVO;
import com.nexoraone.admin.module.system.login.manager.UserPermissionCacheManager;
import com.nexoraone.admin.module.system.role.dao.RoleDao;
import com.nexoraone.admin.module.system.role.dao.RoleEmployeeDao;
import com.nexoraone.admin.module.system.role.dao.RoleMenuDao;
import com.nexoraone.admin.module.system.role.domain.entity.RoleEntity;
import com.nexoraone.admin.module.system.role.domain.entity.RoleMenuEntity;
import com.nexoraone.admin.module.system.role.domain.form.RoleMenuUpdateForm;
import com.nexoraone.admin.module.system.role.domain.vo.RoleMenuTreeVO;
import com.nexoraone.admin.module.system.role.manager.RoleMenuManager;
import com.nexoraone.base.common.code.UserErrorCode;
import com.nexoraone.base.common.domain.ResponseDTO;
import com.nexoraone.base.common.util.SmartBeanUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色-菜单
 *
 * @Author NexoraOne: 善逸
 * @Date 2021-10-22 23:17:47
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@Service
public class RoleMenuService {

    @Resource
    private RoleDao roleDao;
    @Resource
    private RoleMenuDao roleMenuDao;
    @Resource
    private RoleMenuManager roleMenuManager;
    @Resource
    private MenuDao menuDao;
    @Resource
    private RoleEmployeeDao roleEmployeeDao;
    @Resource
    private UserPermissionCacheManager userPermissionCacheManager;

    /**
     * 更新角色权限
     *
     */
    public ResponseDTO<String> updateRoleMenu(RoleMenuUpdateForm roleMenuUpdateForm) {
        //查询角色是否存在
        Long roleId = roleMenuUpdateForm.getRoleId();
        RoleEntity roleEntity = roleDao.selectById(roleId);
        if (null == roleEntity) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        Set<Long> grantedMenuIds = appendMenuPoints(roleMenuUpdateForm.getMenuIdList());
        List<RoleMenuEntity> roleMenuEntityList = Lists.newArrayList();
        RoleMenuEntity roleMenuEntity;
        for (Long menuId : grantedMenuIds) {
            roleMenuEntity = new RoleMenuEntity();
            roleMenuEntity.setRoleId(roleId);
            roleMenuEntity.setMenuId(menuId);
            roleMenuEntityList.add(roleMenuEntity);
        }
        roleMenuManager.updateRoleMenu(roleMenuUpdateForm.getRoleId(), roleMenuEntityList);
        userPermissionCacheManager.clear(roleEmployeeDao.selectEmployeeIdByRoleIdList(Lists.newArrayList(roleId)));
        return ResponseDTO.ok();
    }

    /**
     * 勾选页面菜单时，自动授权该页面下的所有有效功能点。
     */
    private Set<Long> appendMenuPoints(List<Long> selectedMenuIds) {
        Set<Long> grantedMenuIds = new LinkedHashSet<>();
        if (CollectionUtils.isEmpty(selectedMenuIds)) {
            return grantedMenuIds;
        }
        List<MenuEntity> selectedMenus = menuDao.selectList(new LambdaQueryWrapper<MenuEntity>()
                .in(MenuEntity::getMenuId, selectedMenuIds)
                .eq(MenuEntity::getDeletedFlag, false)
                .eq(MenuEntity::getDisabledFlag, false));
        grantedMenuIds.addAll(selectedMenus.stream()
                .map(MenuEntity::getMenuId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        if (CollectionUtils.isEmpty(grantedMenuIds)) {
            return grantedMenuIds;
        }
        grantedMenuIds.addAll(menuDao.selectList(new LambdaQueryWrapper<MenuEntity>()
                        .in(MenuEntity::getParentId, grantedMenuIds)
                        .eq(MenuEntity::getMenuType, MenuTypeEnum.POINTS.getValue())
                        .eq(MenuEntity::getDeletedFlag, false)
                        .eq(MenuEntity::getDisabledFlag, false))
                .stream()
                .map(MenuEntity::getMenuId)
                .toList());
        return grantedMenuIds;
    }

    /**
     * 根据角色id集合，查询其所有的菜单权限
     *
     */
    public List<MenuVO> getMenuList(List<Long> roleIdList, Boolean administratorFlag) {
        //管理员返回所有菜单
        if(administratorFlag){
            List<MenuEntity> menuEntityList = roleMenuDao.selectMenuListByRoleIdList(Lists.newArrayList(), false);
            return SmartBeanUtil.copyList(menuEntityList, MenuVO.class);
        }
        //非管理员 无角色 返回空菜单
        if (CollectionUtils.isEmpty(roleIdList)) {
            return new ArrayList<>();
        }
        List<MenuEntity> menuEntityList = roleMenuDao.selectMenuListByRoleIdList(roleIdList, false);
        Set<Long> effectiveMenuIds = appendMenuPoints(menuEntityList.stream()
                .map(MenuEntity::getMenuId)
                .toList());
        if (CollectionUtils.isEmpty(effectiveMenuIds)) {
            return new ArrayList<>();
        }
        List<MenuEntity> effectiveMenuList = menuDao.selectList(new LambdaQueryWrapper<MenuEntity>()
                .in(MenuEntity::getMenuId, effectiveMenuIds)
                .eq(MenuEntity::getDeletedFlag, false)
                .eq(MenuEntity::getDisabledFlag, false)
                .orderByAsc(MenuEntity::getSort));
        return SmartBeanUtil.copyList(effectiveMenuList, MenuVO.class);
    }


    /**
     * 获取角色关联菜单权限
     *
     */
    public ResponseDTO<RoleMenuTreeVO> getRoleSelectedMenu(Long roleId) {
        RoleMenuTreeVO res = new RoleMenuTreeVO();
        res.setRoleId(roleId);
        //查询角色ID选择的菜单权限
        List<Long> selectedMenuId = roleMenuDao.queryMenuIdByRoleId(roleId);
        res.setSelectedMenuId(selectedMenuId);
        //查询完整菜单权限树
        List<MenuVO> menuVOList = menuDao.queryMenuList(Boolean.FALSE, Boolean.FALSE, null);
        Map<Long, List<MenuVO>> parentMap = menuVOList.stream().collect(Collectors.groupingBy(MenuVO::getParentId, Collectors.toList()));
        List<MenuSimpleTreeVO> menuTreeList = this.buildMenuTree(parentMap, NumberUtils.LONG_ZERO);
        res.setMenuTreeList(menuTreeList);
        return ResponseDTO.ok(res);
    }

    /**
     * 构建菜单树
     *
     */
    private List<MenuSimpleTreeVO> buildMenuTree(Map<Long, List<MenuVO>> parentMap, Long parentId) {
        // 获取本级菜单树List
        List<MenuSimpleTreeVO> res = parentMap.getOrDefault(parentId, Lists.newArrayList()).stream()
                .map(e -> SmartBeanUtil.copy(e, MenuSimpleTreeVO.class)).collect(Collectors.toList());
        // 循环遍历下级菜单
        res.forEach(e -> {
            e.setChildren(this.buildMenuTree(parentMap, e.getMenuId()));
        });
        return res;
    }
}
