package com.nexoraone.base.module.support.mail.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.nexoraone.base.common.enumeration.BaseEnum;

/**
 * 邮件模板类型
 *
 * @Author NexoraOne-创始人兼主任:卓大
 * @Date 2024/8/5
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright <a href="#">NexoraOne</a> ，Since 2012
 */

@Getter
@AllArgsConstructor
public enum MailTemplateTypeEnum implements BaseEnum {

    STRING("string", "字符串替代器"),

    FREEMARKER("freemarker", "freemarker模板引擎");

    private String value;

    private String desc;


}