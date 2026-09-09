package com.nexoraone.admin.util;

import com.nexoraone.admin.module.system.login.domain.RequestEmployee;
import com.nexoraone.base.common.domain.RequestUser;
import com.nexoraone.base.common.util.SmartRequestUtil;

/**
 * admin 端的请求工具类
 *
 * @Author NexoraOne-主任:卓大
 * @Date 2023/7/28 19:39:21
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>，Since 2012
 */
public final class AdminRequestUtil {


    public static RequestEmployee getRequestUser() {
        return (RequestEmployee) SmartRequestUtil.getRequestUser();
    }

    public static Long getRequestUserId() {
        RequestUser requestUser = getRequestUser();
        return null == requestUser ? null : requestUser.getUserId();
    }


}
