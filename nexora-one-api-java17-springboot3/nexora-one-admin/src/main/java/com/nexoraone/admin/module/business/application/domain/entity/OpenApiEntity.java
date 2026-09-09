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
    /** 是否启用。 */
    private Boolean enabledFlag;
    /** 排序。 */
    private Integer sort;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
