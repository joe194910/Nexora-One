package com.nexoraone.base.module.support.job.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexoraone.base.module.support.job.api.domain.NexoraJobQueryForm;
import com.nexoraone.base.module.support.job.api.domain.NexoraJobVO;
import com.nexoraone.base.module.support.job.repository.domain.NexoraJobEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时任务 dao
 *
 * @author huke
 * @date 2024/6/17 21:30
 */
@Mapper
public interface NexoraJobDao extends BaseMapper<NexoraJobEntity> {

    /**
     * 定时任务-分页查询
     *
     * @param page
     * @param queryForm
     * @return
     */
    List<NexoraJobVO> query(Page<?> page, @Param("query") NexoraJobQueryForm queryForm);

    /**
     * 假删除
     *
     * @param jobId
     * @return
     */
    void updateDeletedFlag(@Param("jobId") Integer jobId, @Param("deletedFlag") Boolean deletedFlag);

    /**
     * 根据 任务class 查找
     *
     * @param jobClass
     * @return
     */
    NexoraJobEntity selectByJobClass(@Param("jobClass") String jobClass);
}
