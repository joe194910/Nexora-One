package com.nexoraone.admin.module.business.application.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用API权限申请。
 */
@Data
@TableName("nexora_one_application_api_permission")
public class ApplicationApiPermissionEntity {
    /** 权限申请主键。 */
    @TableId(type = IdType.AUTO)
    private Long permissionId;
    /** 应用主键。 */
    private Long applicationId;
    /** 开放API主键。 */
    private Long openApiId;
    /** 申请原因。 */
    private String applyReason;
    /** 使用场景。 */
    private String useScene;
    /** 申请环境：test测试，prod生产。 */
    private String applyEnvironment;
    /** 申请人主键。 */
    private Long applicantId;
    /** 申请人姓名。 */
    private String applicantName;
    /** 申请状态：1待审核，2已授权，3已驳回。 */
    private Integer applyStatus;
    /** 审核人主键。 */
    private Long reviewerId;
    /** 审核人姓名。 */
    private String reviewerName;
    /** 审核意见。 */
    private String reviewRemark;
    /** 每日调用额度。 */
    private Long dailyQuota;
    /** 授权开始时间。 */
    private LocalDateTime effectiveTime;
    /** 授权结束时间。 */
    private LocalDateTime expireTime;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
