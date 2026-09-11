package com.nexoraone.admin.module.business.openapi.controller;

import com.nexoraone.admin.module.business.openapi.domain.vo.OpenApiGatewayResponse;
import com.nexoraone.admin.module.business.openapi.service.OpenApiGatewayService;
import com.nexoraone.base.common.annoation.NoNeedLogin;
import com.nexoraone.base.common.domain.ResponseDTO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 第三方应用调用开放 API 的统一网关入口。
 */
@RestController
public class OpenApiGatewayController {

    @Resource
    private OpenApiGatewayService gatewayService;

    /**
     * 接收任意开放 API 请求，完成鉴权后转发到真实业务服务。
     */
    @NoNeedLogin
    @RequestMapping("/open-api/v{version}/**")
    public ResponseEntity<byte[]> invoke(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] requestBody) {
        ResponseDTO<OpenApiGatewayResponse> result = gatewayService.invoke(request, requestBody);
        if (!result.getOk()) {
            String body = "{\"code\":" + result.getCode() + ",\"message\":\""
                    + escapeJson(result.getMsg()) + "\"}";
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.getBytes(StandardCharsets.UTF_8));
        }
        OpenApiGatewayResponse response = result.getData();
        if (response == null || response.getHttpStatus() == null) {
            String body = "{\"code\":500,\"message\":\"开放 API 网关未返回有效结果\"}";
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.getBytes(StandardCharsets.UTF_8));
        }
        HttpHeaders headers = new HttpHeaders();
        Objects.requireNonNullElseGet(response.getHeaders(), HttpHeaders::new).forEach((name, values) -> {
            if (!HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(name)
                    && !HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)
                    && !HttpHeaders.CONNECTION.equalsIgnoreCase(name)) {
                headers.put(name, values);
            }
        });
        if (response.getTraceId() != null) {
            headers.set("X-NexoraOne-Trace-Id", response.getTraceId());
        }
        return ResponseEntity.status(response.getHttpStatus())
                .headers(headers)
                .body(Objects.requireNonNullElse(response.getBody(), new byte[0]));
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
