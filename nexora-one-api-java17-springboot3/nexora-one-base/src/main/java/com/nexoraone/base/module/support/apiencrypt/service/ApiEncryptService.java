package com.nexoraone.base.module.support.apiencrypt.service;

/**
 * 接口加密、解密 Service
 *
 * @Author NexoraOne-主任:卓大
 * @Date 2023/10/21 11:41:46
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */

public interface ApiEncryptService {

    /**
     * 解密
     * @param data
     * @return
     */
    String decrypt(String data);

    /**
     * 加密
     *
     * @param data
     * @return
     */
    String encrypt(String data);

}
