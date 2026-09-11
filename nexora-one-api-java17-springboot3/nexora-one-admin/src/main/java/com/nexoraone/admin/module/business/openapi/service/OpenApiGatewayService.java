package com.nexoraone.admin.module.business.openapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexoraone.admin.module.business.application.dao.ApplicationApiPermissionDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationCredentialDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationDao;
import com.nexoraone.admin.module.business.application.dao.OpenApiDao;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationApiPermissionEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationCredentialEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationEntity;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationConnectVO;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationSecurityConfigVO;
import com.nexoraone.admin.module.business.application.manager.ApplicationCredentialManager;
import com.nexoraone.admin.module.business.application.service.ApplicationDataScopeService;
import com.nexoraone.admin.module.business.application.service.ApplicationOpenAuthService;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiCallLogDao;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiEnvironmentDao;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiVersionDao;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiCallLogEntity;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiEnvironmentEntity;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiVersionEntity;
import com.nexoraone.admin.module.business.openapi.domain.form.OpenApiDebugForm;
import com.nexoraone.admin.module.business.openapi.domain.vo.OpenApiGatewayResponse;
import com.nexoraone.admin.module.business.openapi.domain.vo.OpenApiRouteContext;
import com.nexoraone.base.common.domain.ResponseDTO;
import com.nexoraone.base.module.support.redis.RedisService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.net.InetAddress;
import java.net.http.HttpClient;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开放 API 网关鉴权、限流、转发和调用日志服务。
 */
@Service
@Slf4j
public class OpenApiGatewayService {

    private static final Set<String> FORBIDDEN_FORWARD_HEADERS = Set.of(
            "authorization", "cookie", "host", "content-length", "connection",
            "x-app-id", "x-timestamp", "x-nonce", "x-signature");
    private final Map<Integer, RestClient> timeoutRestClients = new ConcurrentHashMap<>();

    @Resource
    private OpenApiDao openApiDao;
    @Resource
    private OpenApiVersionDao versionDao;
    @Resource
    private OpenApiEnvironmentDao environmentDao;
    @Resource
    private ApplicationDao applicationDao;
    @Resource
    private ApplicationCredentialDao credentialDao;
    @Resource
    private ApplicationApiPermissionDao permissionDao;
    @Resource
    private OpenApiCallLogDao callLogDao;
    @Resource
    private ApplicationOpenAuthService openAuthService;
    @Resource
    private ApplicationCredentialManager credentialManager;
    @Resource
    private ApplicationDataScopeService applicationDataScopeService;
    @Resource
    private RedisService redisService;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 校验外部调用方请求并将请求转发到 API 配置的生产服务。
     */
    public ResponseDTO<OpenApiGatewayResponse> invoke(HttpServletRequest request, byte[] requestBody) {
        String path = request.getRequestURI();
        OpenApiRouteContext route = resolveRoute(request.getMethod(), path);
        if (route == null) {
            return reject(404, "请求的开放 API 不存在或尚未上架");
        }

        ResponseDTO<ApplicationConnectVO> authorization = openAuthService.authorize(
                request.getHeader(HttpHeaders.AUTHORIZATION), null, false);
        if (!authorization.getOk()) {
            return reject(401, authorization.getMsg());
        }
        ApplicationConnectVO access = authorization.getData();
        String appId = request.getHeader("X-App-Id");
        if (!Objects.equals(appId, access.getAppId())) {
            return reject(403, "X-App-Id 与 Access Token 所属应用不一致");
        }

        ApplicationEntity application = applicationDao.selectById(access.getApplicationId());
        if (application == null) {
            return reject(401, "Access Token 所属应用不存在");
        }
        if (!hasPermission(access.getApplicationId(), route.getApi())) {
            return reject(403, "当前应用未获得该 API 的有效调用权限");
        }
        ApplicationSecurityConfigVO securityConfig;
        try {
            securityConfig = readSecurityConfig(application.getSecurityConfig());
        } catch (IllegalStateException exception) {
            log.error("应用接口安全配置解析失败，applicationId={}", application.getApplicationId(), exception);
            return reject(500, "应用接口安全配置无效，请联系平台管理员");
        }
        ResponseDTO<String> accessCheck = checkAccessSecurity(request, securityConfig);
        if (!accessCheck.getOk()) {
            return reject(403, accessCheck.getMsg());
        }

        ApplicationCredentialEntity credential = getCredential(access.getApplicationId());
        if (credential == null) {
            return reject(401, "应用凭证不存在或已经失效");
        }
        String requestTarget = buildRequestTarget(path, request.getQueryString());
        String canonical = requestTarget;
        if ("SIGNATURE".equals(securityConfig.getAuthMode())) {
            String timestamp = request.getHeader(securityConfig.getTimestampHeader());
            String nonce = request.getHeader(securityConfig.getNonceHeader());
            String signature = request.getHeader(securityConfig.getSignatureHeader());
            ResponseDTO<String> signatureCheck = verifySignature(
                    credential, request.getMethod(), requestTarget, timestamp, nonce,
                    requestBody, signature, securityConfig);
            if (!signatureCheck.getOk()) {
                return reject(401, signatureCheck.getMsg());
            }
            canonical = signatureCheck.getData();
        }
        ResponseDTO<String> limitCheck = checkLimits(
                access.getApplicationId(), route.getApi(), securityConfig);
        if (!limitCheck.getOk()) {
            return reject(429, limitCheck.getMsg());
        }

        OpenApiEnvironmentEntity environment = getEnvironment(route.getVersion().getVersionId(), "prod", false);
        if (environment == null) {
            return reject(503, "API 未配置可用的生产环境");
        }
        Map<String, String> headers = copyHeaders(request, securityConfig);
        headers.put("X-NexoraOne-Application-Id", String.valueOf(access.getApplicationId()));
        headers.put("X-NexoraOne-App-Id", access.getAppId());
        headers.put("X-NexoraOne-Trace-Id", newTraceId());
        ResponseDTO<OpenApiGatewayResponse> result = dispatch(
                route, environment, request.getQueryString(), headers, requestBody,
                access.getApplicationId(), access.getAppId(), canonical, securityConfig);
        if (result.getOk() && result.getData() != null
                && result.getData().getHttpStatus() >= 200
                && result.getData().getHttpStatus() < 300) {
            safeMarkConnected(application);
        }
        return result;
    }

    /**
     * 使用平台托管凭证向测试环境发起真实在线调试请求。
     */
    public ResponseDTO<Map<String, Object>> debug(OpenApiDebugForm form) {
        OpenApiEntity api = openApiDao.selectById(form.getOpenApiId());
        if (api == null || !Objects.equals(api.getStatus(), 4) || !Boolean.TRUE.equals(api.getEnabledFlag())) {
            return ResponseDTO.userErrorParam("API 不存在或尚未上架");
        }
        ApplicationEntity application = applicationDao.selectById(form.getApplicationId());
        if (application == null || !Objects.equals(application.getListingStatus(), 2)) {
            return ResponseDTO.userErrorParam("应用不存在或尚未上架");
        }
        if (!applicationDataScopeService.canManage(application)) {
            return ResponseDTO.userErrorParam("无权使用该应用进行在线调试");
        }
        if (!hasPermission(application.getApplicationId(), api)) {
            return ResponseDTO.userErrorParam("当前应用尚未获得该 API 的有效授权");
        }
        OpenApiVersionEntity version = versionDao.selectById(api.getCurrentVersionId());
        OpenApiEnvironmentEntity environment = getEnvironment(
                version == null ? null : version.getVersionId(), form.getEnvironmentCode(), true);
        if (version == null || environment == null) {
            return ResponseDTO.userErrorParam("该环境未启用在线调试");
        }
        ApplicationCredentialEntity credential = getCredential(application.getApplicationId());
        if (credential == null) {
            return ResponseDTO.userErrorParam("应用没有有效凭证");
        }
        ApplicationSecurityConfigVO securityConfig;
        try {
            securityConfig = readSecurityConfig(application.getSecurityConfig());
        } catch (IllegalStateException exception) {
            log.error("在线调试读取应用安全配置失败，applicationId={}", application.getApplicationId(), exception);
            return ResponseDTO.userErrorParam("应用接口安全配置无效，请联系平台管理员");
        }

        String requestPath = fillPath(version.getGatewayPath(), form.getParams());
        Map<String, Object> queryParams = remainingParams(version.getGatewayPath(), form.getParams());
        String query = UriComponentsBuilder.newInstance().queryParams(toQueryParams(queryParams))
                .build().getQuery();
        String requestTarget = buildRequestTarget(requestPath, query);
        byte[] body = Objects.requireNonNullElse(form.getBody(), "").getBytes(StandardCharsets.UTF_8);
        long timestamp = System.currentTimeMillis();
        String nonce = newTraceId();
        String canonical = canonicalRequest(version.getRequestMethod(), requestTarget,
                String.valueOf(timestamp), nonce, body);
        String signature = credentialManager.signForPlatform(
                credential, canonical, securityConfig.getSignatureAlgorithm());

        Map<String, String> headers = new LinkedHashMap<>();
        if (form.getHeaders() != null) {
            form.getHeaders().forEach((name, value) -> {
                if (!FORBIDDEN_FORWARD_HEADERS.contains(name.toLowerCase())) {
                    headers.put(name, value);
                }
            });
        }
        headers.put("X-NexoraOne-Application-Id", String.valueOf(application.getApplicationId()));
        headers.put("X-NexoraOne-App-Id", credential.getAppId());
        headers.put("X-NexoraOne-Trace-Id", newTraceId());

        OpenApiRouteContext route = new OpenApiRouteContext(
                api, version, pathVariables(version.getGatewayPath(), form.getParams()));
        ResponseDTO<OpenApiGatewayResponse> dispatched = dispatch(route, environment, query, headers, body,
                application.getApplicationId(), credential.getAppId(), canonical, securityConfig);
        if (!dispatched.getOk()) {
            return ResponseDTO.error(dispatched);
        }
        OpenApiGatewayResponse response = dispatched.getData();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("traceId", response.getTraceId());
        result.put("httpStatus", response.getHttpStatus());
        result.put("durationMs", response.getDurationMs());
        result.put("responseBytes", response.getBody() == null ? 0 : response.getBody().length);
        result.put("requestUrl", response.getTargetUrl());
        result.put("requestMethod", version.getRequestMethod());
        result.put("appId", credential.getAppId());
        result.put("timestamp", timestamp);
        result.put("nonce", nonce);
        result.put("canonicalRequest", canonical);
        result.put("signature", signature);
        result.put("signatureAlgorithm", securityConfig.getSignatureAlgorithm());
        result.put("signatureHeader", securityConfig.getSignatureHeader());
        result.put("timestampHeader", securityConfig.getTimestampHeader());
        result.put("nonceHeader", securityConfig.getNonceHeader());
        result.put("responseHeaders", response.getHeaders());
        result.put("responseBody", new String(Objects.requireNonNullElse(response.getBody(), new byte[0]),
                StandardCharsets.UTF_8));
        result.put("dispatchMode", "real-forward");
        return ResponseDTO.ok(result);
    }

    /**
     * 根据请求方法和路径匹配已发布的 API 路由。
     */
    private OpenApiRouteContext resolveRoute(String method, String path) {
        AntPathMatcher matcher = new AntPathMatcher();
        List<OpenApiEntity> apis = openApiDao.selectList(new LambdaQueryWrapper<OpenApiEntity>()
                .eq(OpenApiEntity::getStatus, 4)
                .eq(OpenApiEntity::getEnabledFlag, true)
                .eq(OpenApiEntity::getRequestMethod, method.toUpperCase()));
        for (OpenApiEntity api : apis) {
            OpenApiVersionEntity version = versionDao.selectById(api.getCurrentVersionId());
            if (version != null && Objects.equals(version.getStatus(), 3)
                    && matcher.match(version.getGatewayPath(), path)) {
                return new OpenApiRouteContext(api, version,
                        matcher.extractUriTemplateVariables(version.getGatewayPath(), path));
            }
        }
        return null;
    }

    /**
     * 校验时间戳、随机串、防重放状态和HMAC请求签名。
     */
    private ResponseDTO<String> verifySignature(ApplicationCredentialEntity credential, String method,
                                                String requestTarget, String timestamp, String nonce,
                                                byte[] body, String signature,
                                                ApplicationSecurityConfigVO securityConfig) {
        if (credential == null || StringUtils.isAnyBlank(timestamp, nonce, signature)) {
            return ResponseDTO.userErrorParam("缺少签名请求头");
        }
        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            return ResponseDTO.userErrorParam(securityConfig.getTimestampHeader() + " 格式不正确");
        }
        long replayTtlMillis = securityConfig.getReplayTtl() * 1000L;
        if (Math.abs(System.currentTimeMillis() - requestTime) > replayTtlMillis) {
            return ResponseDTO.userErrorParam("请求时间戳已过期");
        }
        String canonical = canonicalRequest(method, requestTarget, timestamp, nonce, body);
        if (!credentialManager.verifySignature(
                credential, canonical, signature, securityConfig.getSignatureAlgorithm())) {
            return ResponseDTO.userErrorParam("请求签名无效");
        }
        if (Boolean.TRUE.equals(securityConfig.getReplayProtection())) {
            String replayKey = "open-api:nonce:" + credential.getAppId() + ":" + nonce;
            if (!redisService.getLock(replayKey, replayTtlMillis)) {
                return ResponseDTO.userErrorParam("请求随机串已使用，请勿重复提交");
            }
        }
        return ResponseDTO.ok(canonical);
    }

    /**
     * 校验每秒限流和应用每日授权额度。
     */
    private ResponseDTO<String> checkLimits(Long applicationId, OpenApiEntity api,
                                            ApplicationSecurityConfigVO securityConfig) {
        long currentSecond = System.currentTimeMillis() / 1000L;
        long qps = redisService.increment(
                "open-api:qps:" + currentSecond + ":" + applicationId + ":" + api.getOpenApiId(), 2);
        if (qps > securityConfig.getQpsLimit()) {
            return ResponseDTO.userErrorParam("请求过于频繁，请稍后重试");
        }
        long dailyQuota = Math.min(
                securityConfig.getDailyLimit(), resolveDailyQuota(applicationId, api));
        long dailyCount = redisService.increment(
                "open-api:daily:" + LocalDate.now() + ":" + applicationId + ":" + api.getOpenApiId(),
                RedisService.currentDaySecond() + 60);
        if (dailyCount > dailyQuota) {
            return ResponseDTO.userErrorParam("今日 API 调用额度已用完");
        }
        return ResponseDTO.ok();
    }

    /**
     * 将请求发送到真实目标服务并记录实际响应。
     */
    private ResponseDTO<OpenApiGatewayResponse> dispatch(
            OpenApiRouteContext route, OpenApiEnvironmentEntity environment, String query,
            Map<String, String> headers, byte[] body, Long applicationId, String appId, String canonical,
            ApplicationSecurityConfigVO securityConfig) {
        String traceId = Objects.requireNonNullElse(headers.get("X-NexoraOne-Trace-Id"), newTraceId());
        String targetPath = fillPath(route.getVersion().getInternalPath(), route.getPathVariables());
        String targetUrl = joinUrl(environment.getBaseUrl(), targetPath, query);
        long start = System.currentTimeMillis();
        try {
            RestClient.RequestBodySpec request = getRestClient(securityConfig.getTimeoutSeconds())
                    .method(HttpMethod.valueOf(route.getVersion().getRequestMethod()))
                    .uri(targetUrl)
                    .headers(httpHeaders -> headers.forEach(httpHeaders::set));
            if (StringUtils.isNotBlank(route.getVersion().getContentType())) {
                request.contentType(MediaType.parseMediaType(route.getVersion().getContentType()));
            }
            if (body != null && body.length > 0) {
                request.body(body);
            }
            ResponseEntity<byte[]> targetResponse = request.exchange((outboundRequest, clientResponse) ->
                    ResponseEntity.status(clientResponse.getStatusCode())
                            .headers(clientResponse.getHeaders())
                            .body(clientResponse.getBody().readAllBytes()));
            OpenApiGatewayResponse response = buildResponse(traceId, targetUrl, canonical, targetResponse,
                    System.currentTimeMillis() - start);
            safeSaveCallLog(response, applicationId, appId, route.getApi(), environment.getEnvironmentCode(), null);
            return ResponseDTO.ok(response);
        } catch (Exception exception) {
            OpenApiGatewayResponse response = new OpenApiGatewayResponse();
            response.setTraceId(traceId);
            response.setHttpStatus(502);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            response.setHeaders(responseHeaders);
            response.setBody(("{\"code\":502,\"message\":\"目标服务调用失败\",\"traceId\":\""
                    + traceId + "\"}").getBytes(StandardCharsets.UTF_8));
            response.setTargetUrl(targetUrl);
            response.setCanonicalRequest(canonical);
            response.setDispatched(true);
            response.setDurationMs(Math.max(System.currentTimeMillis() - start, 1));
            safeSaveCallLog(response, applicationId, appId, route.getApi(),
                    environment.getEnvironmentCode(), exception.getMessage());
            return ResponseDTO.ok(response);
        }
    }

    private OpenApiGatewayResponse buildResponse(String traceId, String targetUrl, String canonical,
                                                 ResponseEntity<byte[]> targetResponse, long duration) {
        OpenApiGatewayResponse response = new OpenApiGatewayResponse();
        response.setTraceId(traceId);
        response.setHttpStatus(targetResponse.getStatusCode().value());
        response.setHeaders(targetResponse.getHeaders());
        response.setBody(targetResponse.getBody());
        response.setTargetUrl(targetUrl);
        response.setCanonicalRequest(canonical);
        response.setDispatched(true);
        response.setDurationMs(Math.max(duration, 1));
        return response;
    }

    private void safeSaveCallLog(OpenApiGatewayResponse response, Long applicationId, String appId,
                                 OpenApiEntity api, String environmentCode, String errorMessage) {
        try {
            OpenApiCallLogEntity entity = new OpenApiCallLogEntity();
            entity.setTraceId(response.getTraceId());
            entity.setApplicationId(applicationId);
            entity.setAppId(appId);
            entity.setOpenApiId(api.getOpenApiId());
            entity.setApiCode(api.getApiCode());
            entity.setEnvironmentCode(environmentCode);
            entity.setRequestMethod(api.getRequestMethod());
            entity.setRequestPath(api.getRequestPath());
            entity.setHttpStatus(response.getHttpStatus());
            entity.setSuccessFlag(response.getHttpStatus() >= 200 && response.getHttpStatus() < 300);
            entity.setDurationMs(response.getDurationMs());
            entity.setResponseBytes((long) Objects.requireNonNullElse(response.getBody(), new byte[0]).length);
            entity.setErrorMessage(StringUtils.left(errorMessage, 1000));
            callLogDao.insert(entity);
            openApiDao.increaseCallCount(api.getOpenApiId());
        } catch (Exception exception) {
            log.error("开放 API 调用日志保存失败，traceId={}，apiCode={}",
                    response.getTraceId(), api.getApiCode(), exception);
        }
    }

    /**
     * 在真实业务请求成功后更新应用接入状态。
     */
    private void safeMarkConnected(ApplicationEntity application) {
        if (Objects.equals(application.getAccessStatus(), 2)) {
            return;
        }
        try {
            application.setAccessStatus(2);
            applicationDao.updateById(application);
        } catch (Exception exception) {
            log.error("更新应用接入状态失败，applicationId={}", application.getApplicationId(), exception);
        }
    }

    /**
     * 构建未转发到目标服务的标准网关错误响应。
     */
    private ResponseDTO<OpenApiGatewayResponse> reject(int httpStatus, String message) {
        String traceId = newTraceId();
        OpenApiGatewayResponse response = new OpenApiGatewayResponse();
        response.setTraceId(traceId);
        response.setHttpStatus(httpStatus);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        response.setHeaders(headers);
        response.setBody(("{\"code\":" + httpStatus + ",\"message\":\""
                + escapeJson(message) + "\",\"traceId\":\"" + traceId + "\"}")
                .getBytes(StandardCharsets.UTF_8));
        response.setDispatched(false);
        response.setDurationMs(0L);
        return ResponseDTO.ok(response);
    }

    /**
     * 转义网关生成的JSON文本。
     */
    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private boolean hasPermission(Long applicationId, OpenApiEntity api) {
        if (Objects.equals(api.getPermissionLevel(), 1)) {
            return true;
        }
        return permissionDao.selectCount(new LambdaQueryWrapper<ApplicationApiPermissionEntity>()
                .eq(ApplicationApiPermissionEntity::getApplicationId, applicationId)
                .eq(ApplicationApiPermissionEntity::getOpenApiId, api.getOpenApiId())
                .eq(ApplicationApiPermissionEntity::getApplyStatus, 2)
                .and(item -> item.isNull(ApplicationApiPermissionEntity::getEffectiveTime)
                        .or().le(ApplicationApiPermissionEntity::getEffectiveTime, java.time.LocalDateTime.now()))
                .and(item -> item.isNull(ApplicationApiPermissionEntity::getExpireTime)
                        .or().gt(ApplicationApiPermissionEntity::getExpireTime, java.time.LocalDateTime.now()))) > 0;
    }

    private long resolveDailyQuota(Long applicationId, OpenApiEntity api) {
        if (Objects.equals(api.getPermissionLevel(), 1)) {
            return Long.MAX_VALUE;
        }
        LocalDateTime now = LocalDateTime.now();
        ApplicationApiPermissionEntity permission = permissionDao.selectOne(
                new LambdaQueryWrapper<ApplicationApiPermissionEntity>()
                        .eq(ApplicationApiPermissionEntity::getApplicationId, applicationId)
                        .eq(ApplicationApiPermissionEntity::getOpenApiId, api.getOpenApiId())
                        .eq(ApplicationApiPermissionEntity::getApplyStatus, 2)
                        .and(item -> item.isNull(ApplicationApiPermissionEntity::getEffectiveTime)
                                .or().le(ApplicationApiPermissionEntity::getEffectiveTime, now))
                        .and(item -> item.isNull(ApplicationApiPermissionEntity::getExpireTime)
                                .or().gt(ApplicationApiPermissionEntity::getExpireTime, now))
                        .orderByDesc(ApplicationApiPermissionEntity::getCreateTime)
                        .last("limit 1"));
        return permission == null ? 0L : Objects.requireNonNullElse(permission.getDailyQuota(), 100_000L);
    }

    private ApplicationCredentialEntity getCredential(Long applicationId) {
        return credentialDao.selectOne(new LambdaQueryWrapper<ApplicationCredentialEntity>()
                .eq(ApplicationCredentialEntity::getApplicationId, applicationId)
                .eq(ApplicationCredentialEntity::getStatus, 1)
                .orderByDesc(ApplicationCredentialEntity::getVersionNo)
                .last("limit 1"));
    }

    private OpenApiEnvironmentEntity getEnvironment(Long versionId, String code, boolean debug) {
        if (versionId == null) {
            return null;
        }
        return environmentDao.selectOne(new LambdaQueryWrapper<OpenApiEnvironmentEntity>()
                .eq(OpenApiEnvironmentEntity::getVersionId, versionId)
                .eq(OpenApiEnvironmentEntity::getEnvironmentCode, code)
                .eq(OpenApiEnvironmentEntity::getEnabledFlag, true)
                .eq(debug, OpenApiEnvironmentEntity::getOnlineDebugFlag, true)
                .last("limit 1"));
    }

    private String canonicalRequest(String method, String requestTarget,
                                    String timestamp, String nonce, byte[] body) {
        return method.toUpperCase() + "\n" + requestTarget + "\n" + timestamp + "\n" + nonce + "\n"
                + sha256(Objects.requireNonNullElse(body, new byte[0]));
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception exception) {
            throw new IllegalStateException("计算请求体摘要失败", exception);
        }
    }

    private String fillPath(String path, Map<String, ?> variables) {
        String result = path;
        if (variables != null) {
            for (Map.Entry<String, ?> entry : variables.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private Map<String, Object> remainingParams(String path, Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (params != null) {
            params.forEach((key, value) -> {
                if (!path.contains("{" + key + "}")) {
                    result.put(key, value);
                }
            });
        }
        return result;
    }

    /**
     * 从调试参数中提取网关路径变量，用于填充内部转发路径。
     */
    private Map<String, String> pathVariables(String path, Map<String, Object> params) {
        Map<String, String> result = new LinkedHashMap<>();
        if (params != null) {
            params.forEach((key, value) -> {
                if (path.contains("{" + key + "}")) {
                    result.put(key, String.valueOf(value));
                }
            });
        }
        return result;
    }

    private org.springframework.util.MultiValueMap<String, String> toQueryParams(Map<String, Object> params) {
        org.springframework.util.LinkedMultiValueMap<String, String> result =
                new org.springframework.util.LinkedMultiValueMap<>();
        params.forEach((key, value) -> result.add(key, String.valueOf(value)));
        return result;
    }

    /**
     * 校验HTTPS要求和调用来源IP。
     */
    private ResponseDTO<String> checkAccessSecurity(HttpServletRequest request,
                                                    ApplicationSecurityConfigVO securityConfig) {
        if (Boolean.TRUE.equals(securityConfig.getForceHttps()) && !isHttpsRequest(request)) {
            return ResponseDTO.userErrorParam("当前应用仅允许通过HTTPS调用开放API");
        }
        List<String> whitelist = securityConfig.getIpWhitelist();
        if ("IP".equals(securityConfig.getAuthMode()) && whitelist.isEmpty()) {
            return ResponseDTO.userErrorParam("当前应用选择了IP白名单鉴权，但尚未配置允许访问的IP");
        }
        if (!whitelist.isEmpty() && whitelist.stream()
                .noneMatch(item -> matchesIp(request.getRemoteAddr(), item))) {
            return ResponseDTO.userErrorParam("当前来源IP不在应用白名单内");
        }
        return ResponseDTO.ok();
    }

    /**
     * 判断请求是否通过HTTPS到达平台。
     */
    private boolean isHttpsRequest(HttpServletRequest request) {
        return request.isSecure();
    }

    /**
     * 判断IP是否匹配指定地址或CIDR网段。
     */
    private boolean matchesIp(String clientIp, String rule) {
        if (StringUtils.isAnyBlank(clientIp, rule)
                || !rule.matches("^[0-9a-fA-F:.]+(?:/\\d{1,3})?$")) {
            return false;
        }
        try {
            String[] parts = rule.split("/", 2);
            byte[] client = InetAddress.getByName(clientIp).getAddress();
            byte[] network = InetAddress.getByName(parts[0]).getAddress();
            if (client.length != network.length) {
                return false;
            }
            int maximumPrefix = client.length * 8;
            int prefix = parts.length == 1 ? maximumPrefix : Integer.parseInt(parts[1]);
            if (prefix < 0 || prefix > maximumPrefix) {
                return false;
            }
            int completeBytes = prefix / 8;
            int remainingBits = prefix % 8;
            if (!Arrays.equals(
                    Arrays.copyOf(client, completeBytes), Arrays.copyOf(network, completeBytes))) {
                return false;
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits);
            return (client[completeBytes] & mask) == (network[completeBytes] & mask);
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * 读取应用安全配置，旧应用未配置时使用安全默认值。
     */
    private ApplicationSecurityConfigVO readSecurityConfig(String json) {
        if (StringUtils.isBlank(json)) {
            return new ApplicationSecurityConfigVO().normalize();
        }
        try {
            return objectMapper.readValue(json, ApplicationSecurityConfigVO.class).normalize();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("应用接口安全配置格式不正确", exception);
        }
    }

    /**
     * 获取具有指定连接和读取超时时间的HTTP客户端。
     */
    private RestClient getRestClient(Integer timeoutSeconds) {
        int timeout = Objects.requireNonNullElse(timeoutSeconds, 10);
        return timeoutRestClients.computeIfAbsent(timeout, value -> {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(value))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(Duration.ofSeconds(value));
            return RestClient.builder().requestFactory(requestFactory).build();
        });
    }

    private Map<String, String> copyHeaders(HttpServletRequest request,
                                            ApplicationSecurityConfigVO securityConfig) {
        Map<String, String> result = new LinkedHashMap<>();
        Set<String> configuredSecurityHeaders = Set.of(
                securityConfig.getSignatureHeader().toLowerCase(),
                securityConfig.getTimestampHeader().toLowerCase(),
                securityConfig.getNonceHeader().toLowerCase());
        request.getHeaderNames().asIterator().forEachRemaining(name -> {
            String lowerName = name.toLowerCase();
            if (!FORBIDDEN_FORWARD_HEADERS.contains(lowerName)
                    && !configuredSecurityHeaders.contains(lowerName)) {
                result.put(name, request.getHeader(name));
            }
        });
        return result;
    }

    /**
     * 构建参与签名的原始请求目标。
     */
    private String buildRequestTarget(String path, String query) {
        return StringUtils.isBlank(query) ? path : path + "?" + query;
    }

    private String joinUrl(String baseUrl, String path, String query) {
        String url = StringUtils.removeEnd(baseUrl, "/") + "/" + StringUtils.removeStart(path, "/");
        return StringUtils.isBlank(query) ? url : url + "?" + query;
    }

    private String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
