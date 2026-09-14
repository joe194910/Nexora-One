package com.nexoraone.admin.module.business.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexoraone.admin.module.business.application.dao.ApplicationApiPermissionDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationCredentialDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationReviewDao;
import com.nexoraone.admin.module.business.application.dao.OpenApiDao;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationApiPermissionEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationCredentialEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationReviewEntity;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationApiPermissionForm;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationCreateForm;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationPrePublishForm;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationStepSaveForm;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationCredentialVO;
import com.nexoraone.admin.module.business.application.manager.ApplicationAccessTokenManager;
import com.nexoraone.admin.module.business.application.service.ApplicationService;
import com.nexoraone.admin.module.system.employee.dao.EmployeeDao;
import com.nexoraone.admin.module.system.employee.domain.entity.EmployeeEntity;
import com.nexoraone.admin.module.system.login.domain.RequestEmployee;
import com.nexoraone.admin.module.system.login.manager.LoginManager;
import com.nexoraone.base.common.domain.ResponseDTO;
import com.nexoraone.base.common.util.SmartRequestUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * App ID 与 App Secret 开放认证链路集成测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationOpenAuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Resource
    private ApplicationService applicationService;
    @Resource
    private ApplicationDao applicationDao;
    @Resource
    private ApplicationCredentialDao credentialDao;
    @Resource
    private ApplicationApiPermissionDao permissionDao;
    @Resource
    private ApplicationReviewDao reviewDao;
    @Resource
    private OpenApiDao openApiDao;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private ApplicationAccessTokenManager accessTokenManager;
    @Resource
    private EmployeeDao employeeDao;
    @Resource
    private LoginManager loginManager;

    private Long applicationId;
    private String activeAccessToken;

    /**
     * 使用数据库中的平台管理员建立与正式请求一致的员工上下文。
     */
    @BeforeEach
    void prepareRequestEmployee() {
        EmployeeEntity employee = employeeDao.selectOne(new LambdaQueryWrapper<EmployeeEntity>()
                .eq(EmployeeEntity::getAdministratorFlag, true)
                .eq(EmployeeEntity::getDisabledFlag, false)
                .eq(EmployeeEntity::getDeletedFlag, false)
                .last("LIMIT 1"));
        assertNotNull(employee, "集成测试需要一名可用的平台管理员");
        RequestEmployee requestEmployee = loginManager.getRequestEmployee(employee.getEmployeeId());
        assertNotNull(requestEmployee, "无法加载平台管理员登录信息");
        SmartRequestUtil.setRequestUser(requestEmployee);
    }

    /**
     * 验证应用必须先预发布才能换取令牌，接入前允许轮换密钥，接入后配置转为只读。
     */
    @Test
    void shouldCompleteClientCredentialsFlowAndLockConfigurationAfterConnected() throws Exception {
        ResponseDTO<Map<String, Object>> createResult = applicationService.create(buildApplicationForm());
        assertTrue(createResult.getOk());
        applicationId = Long.valueOf(createResult.getData().get("applicationId").toString());
        ApplicationCredentialVO credential =
                (ApplicationCredentialVO) createResult.getData().get("credential");
        assertEquals(0, applicationDao.selectById(applicationId).getListingStatus());

        JsonNode invalidTokenResponse = requestToken(credential.getAppId(), "invalid-secret");
        assertFalse(invalidTokenResponse.path("ok").asBoolean());

        JsonNode draftTokenResponse = requestToken(credential.getAppId(), credential.getAppSecret());
        assertFalse(draftTokenResponse.path("ok").asBoolean());

        preparePrePublishedApplication();
        assertEquals(5, applicationDao.selectById(applicationId).getListingStatus());

        ResponseDTO<ApplicationCredentialVO> resetResult = applicationService.resetSecret(applicationId);
        assertTrue(resetResult.getOk());
        assertNotNull(resetResult.getData().getAppSecret());
        assertEquals(1, applicationDao.selectById(applicationId).getAccessStatus());

        JsonNode expiredCredentialResponse = requestToken(credential.getAppId(), credential.getAppSecret());
        assertFalse(expiredCredentialResponse.path("ok").asBoolean());

        JsonNode tokenResponse =
                requestToken(resetResult.getData().getAppId(), resetResult.getData().getAppSecret());
        assertTrue(tokenResponse.path("ok").asBoolean());
        assertTrue(tokenResponse.path("data").path("scopes").toString()
                .contains("application:connect:ping"));
        String accessToken = tokenResponse.path("data").path("access_token").asText();
        activeAccessToken = accessToken;
        assertFalse(accessToken.isBlank());
        assertEquals(2, applicationDao.selectById(applicationId).getAccessStatus());

        JsonNode pingResponse = requestPing(accessToken);
        assertTrue(pingResponse.path("ok").asBoolean());
        assertTrue(pingResponse.path("data").path("connected").asBoolean());

        ResponseDTO<ApplicationCredentialVO> lockedResetResult = applicationService.resetSecret(applicationId);
        assertFalse(lockedResetResult.getOk());
        assertEquals(2, applicationDao.selectById(applicationId).getAccessStatus());
    }

    /**
     * 补齐应用配置并提交预发布，模拟真实的接入准备流程。
     */
    private void preparePrePublishedApplication() {
        Map<String, Object> loginConfig = new LinkedHashMap<>();
        loginConfig.put("protocol", "OIDC");
        loginConfig.put("homeUrl", "http://127.0.0.1/application");
        loginConfig.put("callbackUrls", List.of("http://127.0.0.1/oauth/callback"));
        loginConfig.put("tokenTtl", 7200);
        loginConfig.put("codeTtl", 60);
        saveStep(3, loginConfig);

        Map<String, Object> securityConfig = new LinkedHashMap<>();
        securityConfig.put("authMode", "SIGNATURE");
        securityConfig.put("signatureAlgorithm", "HMAC-SHA256");
        securityConfig.put("signatureHeader", "X-Signature");
        securityConfig.put("timestampHeader", "X-Timestamp");
        securityConfig.put("nonceHeader", "X-Nonce");
        securityConfig.put("replayTtl", 300);
        securityConfig.put("qpsLimit", 50);
        securityConfig.put("dailyLimit", 100000);
        securityConfig.put("timeoutSeconds", 10);
        securityConfig.put("ipWhitelist", List.of());
        securityConfig.put("forceHttps", true);
        securityConfig.put("replayProtection", true);
        saveStep(4, securityConfig);

        OpenApiEntity openApi = openApiDao.selectOne(new LambdaQueryWrapper<OpenApiEntity>()
                .eq(OpenApiEntity::getStatus, 4)
                .eq(OpenApiEntity::getEnabledFlag, true)
                .last("LIMIT 1"));
        assertNotNull(openApi, "集成测试需要至少一条已上架且已启用的开放API");

        ApplicationApiPermissionForm permissionForm = new ApplicationApiPermissionForm();
        permissionForm.setApplicationId(applicationId);
        permissionForm.setOpenApiIdList(List.of(openApi.getOpenApiId()));
        permissionForm.setApplyReason("应用接入集成测试");
        assertTrue(applicationService.saveApiPermissions(permissionForm).getOk());

        Map<String, Object> listingConfig = new LinkedHashMap<>();
        listingConfig.put("marketName", "开放认证集成测试");
        listingConfig.put("subtitle", "验证应用开放认证链路");
        listingConfig.put("category", "研发工具");
        listingConfig.put("versionNo", "v1.0.0");
        listingConfig.put("releaseNotes", "集成测试初始版本");
        listingConfig.put("description", "用于验证App ID、App Secret、Access Token和接入探测的真实调用流程。");
        listingConfig.put("providerName", "NexoraOne");
        listingConfig.put("contactEmail", "integration-test@nexoraone.local");
        listingConfig.put("privacyUrl", "http://127.0.0.1/privacy");
        listingConfig.put("termsUrl", "http://127.0.0.1/terms");
        listingConfig.put("bannerUrl", "/file/application/integration-test-banner.png");
        saveStep(6, listingConfig);

        Map<String, Object> publishConfig = new LinkedHashMap<>();
        publishConfig.put("scopeType", "ENTERPRISE");
        publishConfig.put("portalVisible", true);
        publishConfig.put("sort", 100);
        publishConfig.put("openMode", "NEW_TAB");
        saveStep(7, publishConfig);

        ApplicationPrePublishForm prePublishForm = new ApplicationPrePublishForm();
        prePublishForm.setApplicationId(applicationId);
        prePublishForm.setConfirmed(true);
        assertTrue(applicationService.prePublish(prePublishForm).getOk());
    }

    /**
     * 保存应用指定步骤的结构化配置。
     *
     * @param step 步骤编号
     * @param data 配置数据
     */
    private void saveStep(Integer step, Map<String, Object> data) {
        ApplicationStepSaveForm form = new ApplicationStepSaveForm();
        form.setApplicationId(applicationId);
        form.setStep(step);
        form.setData(data);
        assertTrue(applicationService.saveStep(form).getOk());
    }

    /**
     * 构造相互隔离的临时测试应用。
     */
    private ApplicationCreateForm buildApplicationForm() {
        ApplicationCreateForm form = new ApplicationCreateForm();
        form.setApplicationName("开放认证集成测试");
        form.setApplicationCode("auth-it-" + System.currentTimeMillis());
        form.setApplicationType(2);
        form.setEnterpriseName("NexoraOne 测试企业");
        form.setOwnerName("集成测试");
        form.setContact("integration-test@nexoraone.local");
        form.setSummary("用于验证 App ID 与 App Secret 真实接入链路的临时应用。");
        form.setHomeUrl("http://127.0.0.1");
        return form;
    }

    /**
     * 通过公开 HTTP 接口换取访问令牌。
     */
    private JsonNode requestToken(String appId, String appSecret) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of(
                "appId", appId,
                "appSecret", appSecret,
                "grantType", "client_credentials"), headers);
        ResponseEntity<String> response = new RestTemplate().postForEntity(
                baseUrl() + "/open-api/oauth/token", request, String.class);
        return objectMapper.readTree(response.getBody());
    }

    /**
     * 携带访问令牌请求平台连通性接口。
     */
    private JsonNode requestPing(String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<String> response = new RestTemplate().exchange(
                baseUrl() + "/open-api/connect/ping",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        return objectMapper.readTree(response.getBody());
    }

    /**
     * 返回当前随机测试端口地址。
     */
    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    /**
     * 删除本次测试创建的数据和访问令牌。
     */
    @AfterEach
    void cleanUp() {
        try {
            if (activeAccessToken != null) {
                accessTokenManager.revoke(activeAccessToken);
            }
            if (applicationId == null) {
                return;
            }
            reviewDao.delete(new LambdaQueryWrapper<ApplicationReviewEntity>()
                    .eq(ApplicationReviewEntity::getApplicationId, applicationId));
            permissionDao.delete(new LambdaQueryWrapper<ApplicationApiPermissionEntity>()
                    .eq(ApplicationApiPermissionEntity::getApplicationId, applicationId));
            credentialDao.delete(new LambdaQueryWrapper<ApplicationCredentialEntity>()
                    .eq(ApplicationCredentialEntity::getApplicationId, applicationId));
            applicationDao.deleteById(applicationId);
        } finally {
            SmartRequestUtil.remove();
        }
    }
}
