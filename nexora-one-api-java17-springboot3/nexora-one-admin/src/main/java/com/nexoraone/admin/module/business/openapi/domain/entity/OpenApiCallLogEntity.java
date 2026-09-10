package com.nexoraone.admin.module.business.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开放API调用日志。
 */
@Data
@TableName("nexora_one_open_api_call_log")
public class OpenApiCallLogEntity {

    /** 调用日志主键。 */
    @TableId(type = IdType.AUTO)
    private Long callLogId;
    /** 链路追踪标识。 */
    private String traceId;
    /** 应用主键。 */
    private Long applicationId;
    /** App ID快照。 */
    private String appId;
    /** 开放API主键。 */
    private Long openApiId;
    /** API编码快照。 */
    private String apiCode;
    /** 调试或调用环境。 */
    private String environmentCode;
    /** 请求方式。 */
    private String requestMethod;
    /** 请求路径。 */
    private String requestPath;
    /** HTTP状态码。 */
    private Integer httpStatus;
    /** 是否调用成功。 */
    private Boolean successFlag;
    /** 调用耗时毫秒数。 */
    private Long durationMs;
    /** 响应字节数。 */
    private Long responseBytes;
    /** 错误信息。 */
    private String errorMessage;
    /** 创建时间。 */
    private LocalDateTime createTime;
}
