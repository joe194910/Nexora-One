package com.nexoraone.base.module.support.heartbeat.core;

/**
 * 心跳处理接口
 *
 * @Author NexoraOne-主任: 卓大
 * @Date 2022-01-09 20:57:24
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
public interface IHeartBeatRecordHandler {

    /**
     * 心跳日志处理方法
     *
     * @param heartBeatRecord
     */
    void handler(HeartBeatRecord heartBeatRecord);
}
