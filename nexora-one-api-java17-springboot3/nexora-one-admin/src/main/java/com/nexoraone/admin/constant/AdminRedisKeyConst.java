package com.nexoraone.admin.constant;

import com.nexoraone.base.constant.RedisKeyConst;

/**
 * redis key 常量类
 *
 * @Author NexoraOne-主任:卓大
 * @Date 2022-01-07 18:59:22
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
public class AdminRedisKeyConst extends RedisKeyConst {

    /** 第三方应用Access Token缓存前缀，实际键使用Token摘要，避免明文Token出现在Redis键中。 */
    public static final String APPLICATION_ACCESS_TOKEN = "application:access-token:";
}
