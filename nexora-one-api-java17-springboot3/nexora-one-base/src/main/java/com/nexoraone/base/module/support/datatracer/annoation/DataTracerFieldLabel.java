package com.nexoraone.base.module.support.datatracer.annoation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段标签
 *
 * @Author NexoraOne-主任: 卓大
 * @Date 2022-07-23 19:38:52
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DataTracerFieldLabel {
    /**
     * 本属性的注释信息
     * @return
     */
    String value() default "";

}
