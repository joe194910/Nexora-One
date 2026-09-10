package com.nexoraone.admin.module.business.application.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开放API目录。
 */
@Data
@TableName("nexora_one_open_api")
public class OpenApiEntity {
    /** 开放API主键。 */
    @TableId(type = IdType.AUTO)
    private Long openApiId;
    /** API分类。 */
    private String categoryName;
    /** API名称。 */
    private String apiName;
    /** API编码。 */
    private String apiCode;
    /** HTTP请求方式。 */
    private String requestMethod;
    /** 请求路径。 */
    private String requestPath;
    /** API版本。 */
    private String apiVersion;
    /** 权限级别：1公开，2申请授权，3敏感审核。 */
    private Integer permissionLevel;
    /** 接口说明。 */
    private String description;
    /** 所属后端服务。 */
    private String serviceName;
    /** 接口负责人。 */
    private String ownerName;
    /** 标签，多个标签使用英文逗号分隔。 */
    private String tags;
    /** API市场标题。 */
    private String marketTitle;
    /** API市场简介。 */
    private String marketSummary;
    /** 服务等级说明。 */
    private String slaDescription;
    /** 发布可见范围：1全平台，2指定企业。 */
    private String publishScope;
    /** 最近发布时间。 */
    private LocalDateTime publishTime;
    /** 当前版本主键。 */
    private Long currentVersionId;
    /** 发布状态：1草稿，2配置中，3待发布，4已上架，5已停用，6已下线。 */
    private Integer status;
    /** 当前编辑步骤。 */
    private Integer workflowStep;
    /** 今日调用量。 */
    private Long todayCallCount;
    /** 累计调用量。 */
    private Long totalCallCount;
    /** 是否启用。 */
    private Boolean enabledFlag;
    /** 排序。 */
    private Integer sort;
    /** 创建人主键。 */
    private Long createUserId;
    /** 创建人姓名。 */
    private String createUserName;
    /** 更新人主键。 */
    private Long updateUserId;
    /** 更新人姓名。 */
    private String updateUserName;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
