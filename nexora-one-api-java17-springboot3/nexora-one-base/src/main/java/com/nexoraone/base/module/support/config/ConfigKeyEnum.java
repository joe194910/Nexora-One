package com.nexoraone.base.module.support.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.nexoraone.base.common.enumeration.BaseEnum;

/**
 * 系统配置常量类
 *
 * @Author NexoraOne-主任: 卓大
 * @Date 2022-03-14 20:46:27
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@Getter
@AllArgsConstructor
public enum ConfigKeyEnum implements BaseEnum {

    /**
     * 万能密码
     */
    SUPER_PASSWORD("super_password", "万能密码"),

    LEVEL3_PROTECT_CONFIG("level3_protect_config", "三级等保配置"),
    ;

    private final String value;

    private final String desc;
}
