package com.nexoraone.admin.module.business.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/** 解析任务阶段执行记录实体。 */
@Data
@TableName("nexora_one_ai_parse_step")
public class AiParseStepEntity {
    /** 阶段记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long stepId;
    /** 任务主键。 */
    private Long taskId;
    /** 阶段代码。 */
    private String stage;
    /** SUCCESS、FAILED、RUNNING 或 CANCELLED。 */
    private String status;
    /** 执行结果。 */
    private String resultSummary;
    /** 失败原因。 */
    private String errorMessage;
    /** 毫秒耗时。 */
    private Long durationMs;
    /** 开始时间。 */
    private LocalDateTime startTime;
    /** 结束时间。 */
    private LocalDateTime finishTime;
}
