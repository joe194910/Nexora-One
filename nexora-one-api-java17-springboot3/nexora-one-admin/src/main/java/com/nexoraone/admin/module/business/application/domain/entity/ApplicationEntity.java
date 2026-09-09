package com.nexoraone.admin.module.business.application.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * NexoraOne应用主数据。
 */
@Data
@TableName("nexora_one_application")
public class ApplicationEntity {

    /** 应用主键。 */
    @TableId(type = IdType.AUTO)
    private Long applicationId;
    /** 应用名称。 */
    private String applicationName;
    /** 应用编码。 */
    private String applicationCode;
    /** 应用类型：1企业内部应用，2第三方应用。 */
    private Integer applicationType;
    /** 所属企业主键。 */
    private Long enterpriseId;
    /** 所属企业名称快照。 */
    private String enterpriseName;
    /** 应用负责人。 */
    private String ownerName;
    /** 负责人联系方式。 */
    private String contact;
    /** 应用图标地址。 */
    private String iconUrl;
    /** 应用简介。 */
    private String summary;
    /** 应用首页地址。 */
    private String homeUrl;
    /** 备注。 */
    private String remark;
    /** 接入状态：1接入中，2已接入，3接入失败。 */
    private Integer accessStatus;
    /** 上架状态：0未上架，1审核中，2已上架，3已驳回，4已下架。 */
    private Integer listingStatus;
    /** 当前配置步骤。 */
    private Integer workflowStep;
    /** 提交审核后是否锁定配置。 */
    private Boolean configLocked;
    /** 登录与单点跳转配置JSON。 */
    private String loginConfig;
    /** 接口鉴权与安全配置JSON。 */
    private String securityConfig;
    /** 应用上架资料JSON。 */
    private String listingConfig;
    /** 发布范围与可见权限JSON。 */
    private String publishConfig;
    /** 创建人主键。 */
    private Long createUserId;
    /** 创建人姓名。 */
    private String createUserName;
    /** 更新人主键。 */
    private Long updateUserId;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
