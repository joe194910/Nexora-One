package com.nexoraone.base.module.support.job.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import com.nexoraone.base.common.code.UserErrorCode;
import com.nexoraone.base.common.domain.PageResult;
import com.nexoraone.base.common.domain.RequestUser;
import com.nexoraone.base.common.domain.ResponseDTO;
import com.nexoraone.base.common.util.SmartBeanUtil;
import com.nexoraone.base.common.util.SmartPageUtil;
import com.nexoraone.base.module.support.job.api.domain.*;
import com.nexoraone.base.module.support.job.config.NexoraJobAutoConfiguration;
import com.nexoraone.base.module.support.job.constant.NexoraJobTriggerTypeEnum;
import com.nexoraone.base.module.support.job.constant.NexoraJobUtil;
import com.nexoraone.base.module.support.job.repository.NexoraJobDao;
import com.nexoraone.base.module.support.job.repository.NexoraJobLogDao;
import com.nexoraone.base.module.support.job.repository.domain.NexoraJobEntity;
import com.nexoraone.base.module.support.job.repository.domain.NexoraJobLogEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 定时任务 接口业务管理
 * 如果不需要通过接口管理定时任务 可以删除此类
 *
 * @author huke
 * @date 2024/6/17 20:41
 */
@ConditionalOnBean(NexoraJobAutoConfiguration.class)
@Service
public class NexoraJobService {

    @Resource
    private NexoraJobDao jobDao;

    @Resource
    private NexoraJobLogDao jobLogDao;

    @Resource
    private NexoraJobClientManager jobClientManager;

    /**
     * 查询 定时任务详情
     *
     * @param jobId
     * @return
     */
    public ResponseDTO<NexoraJobVO> queryJobInfo(Integer jobId) {
        NexoraJobEntity jobEntity = jobDao.selectById(jobId);
        if (null == jobEntity) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        NexoraJobVO jobVO = SmartBeanUtil.copy(jobEntity, NexoraJobVO.class);
        // 处理设置job详情
        this.handleJobInfo(Lists.newArrayList(jobVO));
        return ResponseDTO.ok(jobVO);
    }

    /**
     * 分页查询 定时任务
     *
     * @param queryForm
     * @return
     */
    public ResponseDTO<PageResult<NexoraJobVO>> queryJob(NexoraJobQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<NexoraJobVO> jobList = jobDao.query(page, queryForm);
        PageResult<NexoraJobVO> pageResult = SmartPageUtil.convert2PageResult(page, jobList);
        // 处理设置job详情
        this.handleJobInfo(jobList);
        return ResponseDTO.ok(pageResult);
    }

    /**
     * 处理设置 任务信息
     *
     * @param jobList
     */
    private void handleJobInfo(List<NexoraJobVO> jobList) {
        if (CollectionUtils.isEmpty(jobList)) {
            return;
        }
        // 查询最后一次执行记录
        List<Long> logIdList = jobList.stream().map(NexoraJobVO::getLastExecuteLogId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Long, NexoraJobLogVO> lastLogMap = Collections.emptyMap();
        if (CollectionUtils.isNotEmpty(logIdList)) {
            lastLogMap = jobLogDao.selectBatchIds(logIdList)
                    .stream()
                    .collect(Collectors.toMap(NexoraJobLogEntity::getLogId, e -> SmartBeanUtil.copy(e, NexoraJobLogVO.class)));
        }

        // 循环处理任务信息
        for (NexoraJobVO jobVO : jobList) {
            // 设置最后一次执行记录
            Long lastExecuteLogId = jobVO.getLastExecuteLogId();
            if (null != lastExecuteLogId) {
                jobVO.setLastJobLog(lastLogMap.get(lastExecuteLogId));
            }
            // 计算未来5次执行时间
            if (jobVO.getEnabledFlag()) {
                List<LocalDateTime> nextTimeList = NexoraJobUtil.queryNextTimeFromNow(jobVO.getTriggerType(), jobVO.getTriggerValue(), jobVO.getLastExecuteTime(), 5);
                jobVO.setNextJobExecuteTimeList(nextTimeList);
            }
        }
    }

    /**
     * 分页查询 定时任务-执行记录
     *
     * @param queryForm
     * @return
     */
    public ResponseDTO<PageResult<NexoraJobLogVO>> queryJobLog(NexoraJobLogQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<NexoraJobLogVO> jobList = jobLogDao.query(page, queryForm);
        PageResult<NexoraJobLogVO> pageResult = SmartPageUtil.convert2PageResult(page, jobList);
        return ResponseDTO.ok(pageResult);
    }

    /**
     * 添加定时任务
     *
     * @param addForm
     * @return
     */
    public synchronized ResponseDTO<String> addJob(NexoraJobAddForm addForm) {
        // 校验参数
        ResponseDTO<String> checkRes = this.checkParam(addForm);
        if (!checkRes.getOk()) {
            return checkRes;
        }

        // 校验重复的执行类
        NexoraJobEntity existJobClass = jobDao.selectByJobClass(addForm.getJobClass());
        if (null != existJobClass && !existJobClass.getDeletedFlag()) {
            return ResponseDTO.userErrorParam("已经存在相同的执行类");
        }

        // 添加数据
        NexoraJobEntity jobEntity = SmartBeanUtil.copy(addForm, NexoraJobEntity.class);
        jobDao.insert(jobEntity);

        // 更新执行端
        NexoraJobMsg jobMsg = new NexoraJobMsg();
        jobMsg.setJobId(jobEntity.getJobId());
        jobMsg.setMsgType(NexoraJobMsg.MsgTypeEnum.UPDATE_JOB);
        jobMsg.setUpdateName(addForm.getUpdateName());
        jobClientManager.publishToClient(jobMsg);
        return ResponseDTO.ok();
    }

    /**
     * 更新定时任务
     *
     * @param updateForm
     * @return
     */
    public synchronized ResponseDTO<String> updateJob(NexoraJobUpdateForm updateForm) {
        // 校验参数
        Integer jobId = updateForm.getJobId();
        NexoraJobEntity jobEntity = jobDao.selectById(jobId);
        if (null == jobEntity) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        ResponseDTO<String> checkRes = this.checkParam(updateForm);
        if (!checkRes.getOk()) {
            return checkRes;
        }

        // 校验重复的执行类
        NexoraJobEntity existJobClass = jobDao.selectByJobClass(updateForm.getJobClass());
        if (null != existJobClass && !existJobClass.getDeletedFlag() && !existJobClass.getJobId().equals(jobId)) {
            return ResponseDTO.userErrorParam("已经存在相同的执行类");
        }

        // 更新数据
        jobEntity = SmartBeanUtil.copy(updateForm, NexoraJobEntity.class);
        jobDao.updateById(jobEntity);

        // 更新执行端
        NexoraJobMsg jobMsg = new NexoraJobMsg();
        jobMsg.setJobId(jobId);
        jobMsg.setMsgType(NexoraJobMsg.MsgTypeEnum.UPDATE_JOB);
        jobMsg.setUpdateName(updateForm.getUpdateName());
        jobClientManager.publishToClient(jobMsg);
        return ResponseDTO.ok();
    }

    /**
     * 校验参数
     * 如需其他校验，请自行添加校验逻辑
     *
     * @param addForm
     * @return
     */
    private ResponseDTO<String> checkParam(NexoraJobAddForm addForm) {
        // 校验触发时间配置
        String triggerType = addForm.getTriggerType();
        String triggerValue = addForm.getTriggerValue();
        if (NexoraJobTriggerTypeEnum.CRON.equalsValue(triggerType) && !NexoraJobUtil.checkCron(triggerValue)) {
            return ResponseDTO.userErrorParam("cron表达式错误");
        }
        if (NexoraJobTriggerTypeEnum.FIXED_DELAY.equalsValue(triggerType) && !NexoraJobUtil.checkFixedDelay(triggerValue)) {
            return ResponseDTO.userErrorParam("固定间隔配置错误：必须是大于0的整数");
        }
        // 校验job class
        return NexoraJobUtil.checkJobClass(addForm.getJobClass());
    }

    /**
     * 更新定时任务-是否开启
     *
     * @param updateForm
     * @return
     */
    public ResponseDTO<String> updateJobEnabled(NexoraJobEnabledUpdateForm updateForm) {
        Integer jobId = updateForm.getJobId();
        NexoraJobEntity jobEntity = jobDao.selectById(jobId);
        if (null == jobEntity) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        Boolean enabledFlag = updateForm.getEnabledFlag();
        if (Objects.equals(enabledFlag, jobEntity.getEnabledFlag())) {
            return ResponseDTO.ok();
        }
        // 更新数据
        jobEntity = new NexoraJobEntity();
        jobEntity.setJobId(jobId);
        jobEntity.setEnabledFlag(enabledFlag);
        jobEntity.setUpdateName(updateForm.getUpdateName());
        jobDao.updateById(jobEntity);

        // 更新执行端
        NexoraJobMsg jobMsg = new NexoraJobMsg();
        jobMsg.setJobId(jobId);
        jobMsg.setMsgType(NexoraJobMsg.MsgTypeEnum.UPDATE_JOB);
        jobMsg.setUpdateName(updateForm.getUpdateName());
        jobClientManager.publishToClient(jobMsg);
        return ResponseDTO.ok();
    }

    /**
     * 执行定时任务
     * 忽略任务的开启状态,立即执行一次
     *
     * @param executeForm
     * @return
     */
    public ResponseDTO<String> execute(NexoraJobExecuteForm executeForm) {
        Integer jobId = executeForm.getJobId();
        NexoraJobEntity jobEntity = jobDao.selectById(jobId);
        if (null == jobEntity) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        // 更新执行端
        NexoraJobMsg jobMsg = new NexoraJobMsg();
        jobMsg.setJobId(jobId);
        jobMsg.setParam(executeForm.getParam());
        jobMsg.setMsgType(NexoraJobMsg.MsgTypeEnum.EXECUTE_JOB);
        jobMsg.setUpdateName(executeForm.getUpdateName());
        jobClientManager.publishToClient(jobMsg);
        return ResponseDTO.ok();
    }

    /**
     * 移除定时任务
     * 物理删除
     *
     * @return
     * @author huke
     */
    public synchronized ResponseDTO<String> deleteJob(Integer jobId, RequestUser requestUser) {
        // 删除任务
        jobDao.updateDeletedFlag(jobId, Boolean.TRUE);

        // 更新执行端
        NexoraJobMsg jobMsg = new NexoraJobMsg();
        jobMsg.setJobId(jobId);
        jobMsg.setMsgType(NexoraJobMsg.MsgTypeEnum.UPDATE_JOB);
        jobMsg.setUpdateName(requestUser.getUserName());
        jobClientManager.publishToClient(jobMsg);
        return ResponseDTO.ok();
    }

}
