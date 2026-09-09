package com.nexoraone.admin.module.business.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexoraone.admin.module.business.application.dao.ApplicationApiPermissionDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationDao;
import com.nexoraone.admin.module.business.application.dao.OpenApiDao;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationApiPermissionEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationCredentialEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationEntity;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationConnectTestForm;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationTokenForm;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationAccessContextVO;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationAccessTokenVO;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationConnectVO;
import com.nexoraone.admin.module.business.application.manager.ApplicationAccessTokenManager;
import com.nexoraone.admin.module.business.application.manager.ApplicationCredentialManager;
import com.nexoraone.base.common.domain.ResponseDTO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 第三方应用通过App ID和App Secret接入NexoraOne的认证服务。
 */
@Service
public class ApplicationOpenAuthService {

    private static final String CLIENT_CREDENTIALS = "client_credentials";
    private static final long DEFAULT_TOKEN_TTL_SECONDS = 7200L;
    private static final long MIN_TOKEN_TTL_SECONDS = 60L;
    private static final long MAX_TOKEN_TTL_SECONDS = 86400L;

    @Resource
    private ApplicationDao applicationDao;
    @Resource
    private OpenApiDao openApiDao;
    @Resource
    private ApplicationApiPermissionDao permissionDao;
    @Resource
    private ApplicationCredentialManager credentialManager;
    @Resource
    private ApplicationAccessTokenManager accessTokenManager;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 使用客户端凭证换取Access Token。
     *
     * @param form App ID、App Secret和授权类型
     * @return Access Token及当前授权范围
     */
    public ResponseDTO<ApplicationAccessTokenVO> issueToken(ApplicationTokenForm form) {
        if (!CLIENT_CREDENTIALS.equals(form.getGrantType())) {
            return ResponseDTO.userErrorParam("当前仅支持client_credentials授权类型");
        }
        ApplicationCredentialEntity credential =
                credentialManager.verifyCredential(form.getAppId(), form.getAppSecret());
        if (credential == null) {
            return ResponseDTO.userErrorParam("App ID或App Secret无效");
        }
        ApplicationEntity application = applicationDao.selectById(credential.getApplicationId());
        if (application == null) {
            return ResponseDTO.userErrorParam("应用不存在或已停用");
        }
        if (Objects.equals(application.getListingStatus(), 4)) {
            return ResponseDTO.userErrorParam("应用已下架，无法签发Access Token");
        }

        long ttlSeconds = resolveTokenTtl(application.getLoginConfig());
        List<String> scopes = queryGrantedScopes(application.getApplicationId());
        long now = System.currentTimeMillis();

        ApplicationAccessContextVO context = new ApplicationAccessContextVO();
        context.setApplicationId(application.getApplicationId());
        context.setApplicationName(application.getApplicationName());
        context.setAppId(credential.getAppId());
        context.setCredentialVersion(credential.getVersionNo());
        context.setScopes(scopes);
        context.setIssuedAt(now);
        context.setExpiresAt(now + ttlSeconds * 1000);

        String accessToken = accessTokenManager.issue(context, ttlSeconds);
        ApplicationAccessTokenVO result = new ApplicationAccessTokenVO();
        result.setAccessToken(accessToken);
        result.setTokenType("Bearer");
        result.setExpiresIn(ttlSeconds);
        result.setScope(String.join(" ", scopes));
        result.setScopes(scopes);
        result.setApplicationId(application.getApplicationId());
        result.setApplicationName(application.getApplicationName());
        return ResponseDTO.ok(result);
    }

    /**
     * 校验Bearer Token和可选的API权限编码。
     *
     * <p>后续开放API控制器可调用本方法，并传入其API编码完成统一授权。</p>
     *
     * @param authorizationHeader Authorization请求头
     * @param requiredScope 当前接口要求的API编码；连通性检查可传null
     * @param markConnected 是否在验证成功后把应用标记为已接入
     * @return 当前应用访问上下文
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<ApplicationConnectVO> authorize(
            String authorizationHeader, String requiredScope, boolean markConnected) {
        String accessToken = parseBearerToken(authorizationHeader);
        if (accessToken == null) {
            return ResponseDTO.userErrorParam("请通过Authorization: Bearer {access_token}传递访问令牌");
        }
        ApplicationAccessContextVO context = accessTokenManager.getContext(accessToken);
        if (context == null || context.getExpiresAt() == null || context.getExpiresAt() <= System.currentTimeMillis()) {
            return ResponseDTO.userErrorParam("Access Token无效或已过期");
        }

        ApplicationCredentialEntity currentCredential =
                credentialManager.getCurrentCredential(context.getApplicationId());
        if (currentCredential == null
                || !Objects.equals(currentCredential.getAppId(), context.getAppId())
                || !Objects.equals(currentCredential.getVersionNo(), context.getCredentialVersion())) {
            accessTokenManager.revoke(accessToken);
            return ResponseDTO.userErrorParam("Access Token已因应用密钥变更而失效");
        }
        if (StringUtils.isNotBlank(requiredScope)
                && (context.getScopes() == null || !context.getScopes().contains(requiredScope))) {
            return ResponseDTO.userErrorParam("当前应用未获得API权限：" + requiredScope);
        }

        ApplicationEntity application = applicationDao.selectById(context.getApplicationId());
        if (application == null || Objects.equals(application.getListingStatus(), 4)) {
            accessTokenManager.revoke(accessToken);
            return ResponseDTO.userErrorParam("应用不存在或已停用");
        }
        if (markConnected && !Objects.equals(application.getAccessStatus(), 2)) {
            application.setAccessStatus(2);
            applicationDao.updateById(application);
        }

        ApplicationConnectVO result = buildConnectResult(context, currentCredential,
                Math.max(accessTokenManager.getExpire(accessToken), 0));
        result.setConnected(true);
        result.setAccessStatus(markConnected ? 2 : application.getAccessStatus());
        return ResponseDTO.ok(result);
    }

    /**
     * 在管理控制台中使用当前明文密钥完成一次签发和校验，确认应用真实可接入平台。
     *
     * @param form 控制台连通性测试参数
     * @return 连通性测试结果
     */
    public ResponseDTO<ApplicationConnectVO> testConnection(ApplicationConnectTestForm form) {
        ApplicationTokenForm tokenForm = new ApplicationTokenForm();
        tokenForm.setAppId(form.getAppId());
        tokenForm.setAppSecret(form.getAppSecret());
        tokenForm.setGrantType(CLIENT_CREDENTIALS);
        ResponseDTO<ApplicationAccessTokenVO> tokenResult = issueToken(tokenForm);
        if (!tokenResult.getOk()) {
            return ResponseDTO.error(tokenResult);
        }
        if (!Objects.equals(form.getApplicationId(), tokenResult.getData().getApplicationId())) {
            accessTokenManager.revoke(tokenResult.getData().getAccessToken());
            return ResponseDTO.userErrorParam("App ID不属于当前应用");
        }

        String accessToken = tokenResult.getData().getAccessToken();
        ResponseDTO<ApplicationConnectVO> connectResult = authorize("Bearer " + accessToken, null, true);
        accessTokenManager.revoke(accessToken);
        return connectResult;
    }

    /**
     * 查询应用当前可访问的API编码：公开API与审核通过的申请API。
     */
    private List<String> queryGrantedScopes(Long applicationId) {
        Set<String> scopes = new LinkedHashSet<>();
        openApiDao.selectList(new LambdaQueryWrapper<OpenApiEntity>()
                        .eq(OpenApiEntity::getEnabledFlag, true)
                        .eq(OpenApiEntity::getPermissionLevel, 1)
                        .orderByAsc(OpenApiEntity::getSort))
                .forEach(api -> scopes.add(api.getApiCode()));

        List<ApplicationApiPermissionEntity> permissions = permissionDao.selectList(
                new LambdaQueryWrapper<ApplicationApiPermissionEntity>()
                        .eq(ApplicationApiPermissionEntity::getApplicationId, applicationId)
                        .eq(ApplicationApiPermissionEntity::getApplyStatus, 2));
        if (!permissions.isEmpty()) {
            openApiDao.selectBatchIds(permissions.stream()
                            .map(ApplicationApiPermissionEntity::getOpenApiId)
                            .toList())
                    .stream()
                    .filter(api -> Boolean.TRUE.equals(api.getEnabledFlag()))
                    .forEach(api -> scopes.add(api.getApiCode()));
        }
        return new ArrayList<>(scopes);
    }

    /**
     * 从应用登录配置中读取Token有效期，并限制在安全范围内。
     */
    private long resolveTokenTtl(String loginConfig) {
        Map<String, Object> config = readJson(loginConfig);
        Object value = config.get("tokenTtl");
        if (value == null) {
            return DEFAULT_TOKEN_TTL_SECONDS;
        }
        try {
            long ttl = Long.parseLong(value.toString());
            return Math.max(MIN_TOKEN_TTL_SECONDS, Math.min(ttl, MAX_TOKEN_TTL_SECONDS));
        } catch (NumberFormatException exception) {
            return DEFAULT_TOKEN_TTL_SECONDS;
        }
    }

    /**
     * 解析Bearer认证头。
     */
    private String parseBearerToken(String authorizationHeader) {
        if (StringUtils.isBlank(authorizationHeader)
                || !StringUtils.startsWithIgnoreCase(authorizationHeader, "Bearer ")) {
            return null;
        }
        String token = authorizationHeader.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    /**
     * 构建对外连通性结果。
     */
    private ApplicationConnectVO buildConnectResult(
            ApplicationAccessContextVO context, ApplicationCredentialEntity credential, long expiresIn) {
        ApplicationConnectVO result = new ApplicationConnectVO();
        result.setApplicationId(context.getApplicationId());
        result.setApplicationName(context.getApplicationName());
        result.setAppId(context.getAppId());
        result.setCredentialVersion(credential.getVersionNo());
        result.setExpiresIn(expiresIn);
        result.setScopes(context.getScopes() == null ? List.of() : context.getScopes());
        return result;
    }

    /**
     * 将JSON配置转换为Map。
     */
    private Map<String, Object> readJson(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return new LinkedHashMap<>();
        }
    }
}
