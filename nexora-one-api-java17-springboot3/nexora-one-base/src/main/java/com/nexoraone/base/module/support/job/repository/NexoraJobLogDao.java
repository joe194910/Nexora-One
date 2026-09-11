package com.nexoraone.base.module.support.job.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexoraone.base.module.support.job.api.domain.NexoraJobLogQueryForm;
import com.nexoraone.base.module.support.job.api.domain.NexoraJobLogVO;
import com.nexoraone.base.module.support.job.repository.domain.NexoraJobLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时任务-执行记录 dao
 *
 * @author huke
 * @date 2024/6/17 21:30
 */
@Mapper
public interface NexoraJobLogDao extends BaseMapper<NexoraJobLogEntity> {

    /**
     * 定时任务-执行记录-分页查询
     *
     * @param page
     * @param queryForm
     * @return
     */
    List<NexoraJobLogVO> query(Page<?> page, @Param("query") NexoraJobLogQueryForm queryForm);
}
