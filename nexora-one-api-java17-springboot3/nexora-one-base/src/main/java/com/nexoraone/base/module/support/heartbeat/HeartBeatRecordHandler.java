package com.nexoraone.base.module.support.heartbeat;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.nexoraone.base.common.util.SmartBeanUtil;
import com.nexoraone.base.module.support.heartbeat.core.HeartBeatRecord;
import com.nexoraone.base.module.support.heartbeat.core.IHeartBeatRecordHandler;
import com.nexoraone.base.module.support.heartbeat.domain.HeartBeatRecordEntity;
import org.springframework.stereotype.Service;

/**
 * 心跳记录
 *
 * @Author NexoraOne-主任: 卓大
 * @Date 2022-01-09 20:57:24
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@Slf4j
@Service
public class HeartBeatRecordHandler implements IHeartBeatRecordHandler {

    @Resource
    private HeartBeatRecordDao heartBeatRecordDao;

    /**
     * 心跳日志处理方法
     * @param heartBeatRecord
     */
    @Override
    public void handler(HeartBeatRecord heartBeatRecord) {
        HeartBeatRecordEntity heartBeatRecordEntity = SmartBeanUtil.copy(heartBeatRecord, HeartBeatRecordEntity.class);
        HeartBeatRecordEntity heartBeatRecordOld = heartBeatRecordDao.query(heartBeatRecordEntity);
        if (heartBeatRecordOld == null) {
            heartBeatRecordDao.insert(heartBeatRecordEntity);
        } else {
            heartBeatRecordDao.updateHeartBeatTimeById(heartBeatRecordOld.getHeartBeatRecordId(), heartBeatRecordEntity.getHeartBeatTime());
        }
    }

}
