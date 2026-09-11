package com.nexoraone.base.module.support.job.repository;

import com.nexoraone.base.module.support.job.repository.domain.NexoraJobEntity;
import com.nexoraone.base.module.support.job.repository.domain.NexoraJobLogEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * job 持久化业务
 *
 * @author huke
 * @date 2024/6/22 22:28
 */
@Service
public class NexoraJobRepository {

    @Autowired
    private NexoraJobDao jobDao;

    @Autowired
    private NexoraJobLogDao jobLogDao;

    public NexoraJobDao getJobDao() {
        return jobDao;
    }

    public NexoraJobLogDao getJobLogDao() {
        return jobLogDao;
    }

    /**
     * 保存执行记录
     *
     * @param logEntity
     * @param jobEntity
     */
    @Transactional(rollbackFor = Throwable.class)
    public void saveLog(NexoraJobLogEntity logEntity, NexoraJobEntity jobEntity) {
        jobLogDao.insert(logEntity);

        jobEntity.setLastExecuteLogId(logEntity.getLogId());
        jobDao.updateById(jobEntity);
    }
}
