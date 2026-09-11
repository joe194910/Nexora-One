package com.nexoraone.admin.module.business.openapi.domain.vo;

import lombok.Data;
import org.springframework.http.HttpHeaders;

/**
 * API 网关转发结果。
 */
@Data
public class OpenApiGatewayResponse {

    /** 写入调用日志的链路追踪标识。 */
    private String traceId;
    /** 目标服务返回的 HTTP 状态码。 */
    private Integer httpStatus;
    /** 目标服务返回的响应头。 */
    private HttpHeaders headers;
    /** 目标服务返回的原始响应体。 */
    private byte[] body;
    /** 解析后的目标服务地址。 */
    private String targetUrl;
    /** 请求签名使用的规范请求字符串。 */
    private String canonicalRequest;
    /** 平台托管在线调试生成的请求签名。 */
    private String signature;
    /** 是否已经向真实目标服务发出请求。 */
    private Boolean dispatched;
    /** 真实目标服务调用耗时，单位毫秒。 */
    private Long durationMs;
}
