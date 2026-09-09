package com.nexoraone.admin.module.business.application.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用审核记录。
 */
@Data
@TableName("nexora_one_application_review")
public class ApplicationReviewEntity {
    /** 审核记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long reviewId;
    /** 应用主键。 */
    private Long applicationId;
    /** 审核阶段。 */
    private String reviewStage;
    /** 审核状态：1待审核，2通过，3驳回。 */
    private Integer reviewStatus;
    /** 审核说明。 */
    private String reviewRemark;
    /** 操作人主键。 */
    private Long operatorId;
    /** 操作人姓名。 */
    private String operatorName;
    /** 创建时间。 */
    private LocalDateTime createTime;
}
