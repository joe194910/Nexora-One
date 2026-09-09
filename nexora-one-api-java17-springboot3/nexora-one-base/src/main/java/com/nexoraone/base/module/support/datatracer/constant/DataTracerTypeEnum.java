package com.nexoraone.base.module.support.datatracer.constant;


import lombok.AllArgsConstructor;
import lombok.Getter;
import com.nexoraone.base.common.enumeration.BaseEnum;

/**
 * 数据业务类型
 *
 * @Author NexoraOne-主任: 卓大
 * @Date 2022-07-23 19:38:52-
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@AllArgsConstructor
@Getter
public enum DataTracerTypeEnum implements BaseEnum {

    /**
     * 商品
     */
    GOODS(1, "商品"),

    /**
     *通知公告
     */
    OA_NOTICE(2, "OA-通知公告"),

    /**
     * 企业信息
     */
    OA_ENTERPRISE(3, "OA-企业信息"),

    ;

    private final Integer value;

    private final String desc;
}
