package com.nexoraone.admin.module.business.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API版本定义。
 */
@Data
@TableName("nexora_one_open_api_version")
public class OpenApiVersionEntity {

    /** 版本主键。 */
    @TableId(type = IdType.AUTO)
    private Long versionId;
    /** 开放API主键。 */
    private Long openApiId;
    /** 语义化版本号。 */
    private String versionNo;
    /** HTTP请求方式。 */
    private String requestMethod;
    /** 公开网关路径。 */
    private String gatewayPath;
    /** 内部转发路径。 */
    private String internalPath;
    /** 请求内容类型。 */
    private String contentType;
    /** 权限级别：1公开，2申请授权，3敏感审核。 */
    private Integer permissionLevel;
    /** 请求超时时间，单位秒。 */
    private Integer timeoutSeconds;
    /** 版本说明。 */
    private String description;
    /** 是否使用统一响应结构包装。 */
    private Boolean unifiedResponseFlag;
    /** 响应示例和日志是否需要脱敏。 */
    private Boolean dataMaskingFlag;
    /** 预留JSON安全配置。 */
    private String securityConfig;
    /** 版本更新日志。 */
    private String changeLog;
    /** 版本状态：1草稿，2待审核，3已发布，4已停用，5已下线。 */
    private Integer status;
    /** 当前版本是否禁止修改。 */
    private Boolean lockedFlag;
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
