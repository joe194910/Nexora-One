package com.nexoraone.base.module.support.loginlog;

import com.nexoraone.base.common.enumeration.BaseEnum;

/**
 * 登录类型
 *
 * @Author NexoraOne-主任: 卓大
 * @Date 2022/07/22 19:46:23
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
public enum LoginLogResultEnum implements BaseEnum {
    
    LOGIN_SUCCESS(0, "登录成功"),
    LOGIN_FAIL(1, "登录失败"),
    LOGIN_OUT(2, "退出登录");

    private Integer type;
    private String desc;

    LoginLogResultEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return type;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
