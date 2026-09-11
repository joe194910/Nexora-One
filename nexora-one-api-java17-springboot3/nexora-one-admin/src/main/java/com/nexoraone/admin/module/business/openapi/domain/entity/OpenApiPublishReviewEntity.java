package com.nexoraone.admin.module.business.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API发布审核记录。
 */
@Data
@TableName("nexora_one_open_api_publish_review")
public class OpenApiPublishReviewEntity {

    /** 审核记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long reviewId;
    /** 开放API主键。 */
    private Long openApiId;
    /** 提交审核的版本主键。 */
    private Long versionId;
    /** 申请人主键。 */
    private Long applicantId;
    /** 申请人姓名。 */
    private String applicantName;
    /** 审核状态：1待审核，2审核通过，3审核驳回。 */
    private Integer reviewStatus;
    /** 审核意见。 */
    private String reviewRemark;
    /** 审核人主键。 */
    private Long reviewerId;
    /** 审核人姓名。 */
    private String reviewerName;
    /** 审核时间。 */
    private LocalDateTime reviewTime;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
