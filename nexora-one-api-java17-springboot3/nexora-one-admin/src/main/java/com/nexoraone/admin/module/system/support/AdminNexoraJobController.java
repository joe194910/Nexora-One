package com.nexoraone.admin.module.system.support;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.nexoraone.base.common.controller.SupportBaseController;
import com.nexoraone.base.common.domain.PageResult;
import com.nexoraone.base.common.domain.RequestUser;
import com.nexoraone.base.common.domain.ResponseDTO;
import com.nexoraone.base.common.util.SmartRequestUtil;
import com.nexoraone.base.constant.SwaggerTagConst;
import com.nexoraone.base.module.support.job.api.NexoraJobService;
import com.nexoraone.base.module.support.job.api.domain.*;
import com.nexoraone.base.module.support.job.config.NexoraJobAutoConfiguration;
import com.nexoraone.base.module.support.repeatsubmit.annoation.RepeatSubmit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

/**
 * 定时任务 管理接口
 *
 * @author huke
 * @date 2024/6/17 20:41
 */
@Tag(name = SwaggerTagConst.Support.JOB)
@RestController
@ConditionalOnBean(NexoraJobAutoConfiguration.class)
public class AdminNexoraJobController extends SupportBaseController {

    @Autowired
    private NexoraJobService jobService;

    @Operation(summary = "定时任务-立即执行 @huke")
    @PostMapping("/job/execute")
    @RepeatSubmit
    public ResponseDTO<String> execute(@RequestBody @Valid NexoraJobExecuteForm executeForm) {
        RequestUser requestUser = SmartRequestUtil.getRequestUser();
        executeForm.setUpdateName(requestUser.getUserName());
        return jobService.execute(executeForm);
    }

    @Operation(summary = "定时任务-查询详情 @huke")
    @GetMapping("/job/{jobId}")
    public ResponseDTO<NexoraJobVO> queryJobInfo(@PathVariable Integer jobId) {
        return jobService.queryJobInfo(jobId);
    }

    @Operation(summary = "定时任务-分页查询 @huke")
    @PostMapping("/job/query")
    public ResponseDTO<PageResult<NexoraJobVO>> queryJob(@RequestBody @Valid NexoraJobQueryForm queryForm) {
        return jobService.queryJob(queryForm);
    }

    @Operation(summary = "定时任务-添加任务 @huke")
    @PostMapping("/job/add")
    @RepeatSubmit
    public ResponseDTO<String> addJob(@RequestBody @Valid NexoraJobAddForm addForm) {
        RequestUser requestUser = SmartRequestUtil.getRequestUser();
        addForm.setUpdateName(requestUser.getUserName());
        return jobService.addJob(addForm);
    }

    @Operation(summary = "定时任务-更新-任务信息 @huke")
    @PostMapping("/job/update")
    @RepeatSubmit
    public ResponseDTO<String> updateJob(@RequestBody @Valid NexoraJobUpdateForm updateForm) {
        RequestUser requestUser = SmartRequestUtil.getRequestUser();
        updateForm.setUpdateName(requestUser.getUserName());
        return jobService.updateJob(updateForm);
    }

    @Operation(summary = "定时任务-更新-开启状态 @huke")
    @PostMapping("/job/update/enabled")
    @RepeatSubmit
    public ResponseDTO<String> updateJobEnabled(@RequestBody @Valid NexoraJobEnabledUpdateForm updateForm) {
        RequestUser requestUser = SmartRequestUtil.getRequestUser();
        updateForm.setUpdateName(requestUser.getUserName());
        return jobService.updateJobEnabled(updateForm);
    }

    @Operation(summary = "定时任务-删除 @zhuoda")
    @GetMapping("/job/delete")
    @RepeatSubmit
    public ResponseDTO<String> deleteJob(@RequestParam Integer jobId) {
        return jobService.deleteJob(jobId, SmartRequestUtil.getRequestUser());
    }

    @Operation(summary = "定时任务-执行记录-分页查询 @huke")
    @PostMapping("/job/log/query")
    public ResponseDTO<PageResult<NexoraJobLogVO>> queryJobLog(@RequestBody @Valid NexoraJobLogQueryForm queryForm) {
        return jobService.queryJobLog(queryForm);
    }
}