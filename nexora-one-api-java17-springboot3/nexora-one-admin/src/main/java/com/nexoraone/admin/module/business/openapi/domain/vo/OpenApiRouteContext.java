package com.nexoraone.admin.module.business.openapi.domain.vo;

import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiVersionEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 根据网关请求解析得到的已发布 API 路由上下文。
 */
@Data
@AllArgsConstructor
public class OpenApiRouteContext {

    /** API 主记录。 */
    private OpenApiEntity api;
    /** 已发布的 API 版本。 */
    private OpenApiVersionEntity version;
    /** 从开放网关路径中提取的路径参数。 */
    private Map<String, String> pathVariables;
}
