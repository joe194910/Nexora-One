package com.nexoraone.base.common.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * sa-token 所需的权限信息
 *
 * @Author NexoraOne-主任:卓大
 * @Date 2023/8/26 15:23:10
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>，Since 2012
 */

@Data
public class UserPermission implements Serializable {

    /**
     * 权限列表
     */
    private List<String> permissionList;

    /**
     * 角色列表
     */
    private List<String> roleList;


}
