package com.nexoraone.admin.module.business.openapi.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * API版本记录展示对象。
 */
@Data
public class OpenApiVersionVO {

    /** 版本主键。 */
    private Long versionId;
    /** 语义化版本号。 */
    private String versionNo;
    /** HTTP请求方式。 */
    private String requestMethod;
    /** 公开网关路径。 */
    private String gatewayPath;
    /** 版本状态：1草稿，2待审核，3已发布，4已停用，5已下线。 */
    private Integer status;
    /** 是否为当前编辑版本。 */
    private Boolean currentFlag;
    /** 是否为当前线上版本。 */
    private Boolean publishedFlag;
    /** 版本更新说明。 */
    private String changeLog;
    /** 创建人姓名。 */
    private String createUserName;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
