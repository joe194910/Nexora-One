package com.nexoraone.admin.module.business.application.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用发布版本。
 */
@Data
@TableName("nexora_one_application_version")
public class ApplicationVersionEntity {
    /** 版本主键。 */
    @TableId(type = IdType.AUTO)
    private Long versionId;
    /** 应用主键。 */
    private Long applicationId;
    /** 版本号。 */
    private String versionNo;
    /** 版本状态：1审核中，2已发布，3已驳回，4已下架。 */
    private Integer versionStatus;
    /** 提交时完整配置快照。 */
    private String configSnapshot;
    /** 提交时间。 */
    private LocalDateTime submitTime;
    /** 发布时间。 */
    private LocalDateTime publishTime;
}
