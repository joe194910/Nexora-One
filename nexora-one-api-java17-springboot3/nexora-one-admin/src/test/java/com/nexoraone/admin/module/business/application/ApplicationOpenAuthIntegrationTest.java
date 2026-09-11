package com.nexoraone.admin.module.business.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexoraone.admin.module.business.application.dao.ApplicationCredentialDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationDao;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationCredentialEntity;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationEntity;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationCreateForm;
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
     * 验证草稿应用可完成客户端凭证签发和平台连通性验证，并验证密钥轮换后的失效机制。
     */
    @Test
    void shouldCompleteClientCredentialsFlowAndInvalidateOldTokenAfterSecretReset() throws Exception {
        ResponseDTO<Map<String, Object>> createResult = applicationService.create(buildApplicationForm());
        assertTrue(createResult.getOk());
        applicationId = Long.valueOf(createResult.getData().get("applicationId").toString());
        ApplicationCredentialVO credential =
                (ApplicationCredentialVO) createResult.getData().get("credential");
        assertEquals(0, applicationDao.selectById(applicationId).getListingStatus());

        JsonNode invalidTokenResponse = requestToken(credential.getAppId(), "invalid-secret");
        assertFalse(invalidTokenResponse.path("ok").asBoolean());

        JsonNode tokenResponse = requestToken(credential.getAppId(), credential.getAppSecret());
        assertTrue(tokenResponse.path("ok").asBoolean());
        assertTrue(tokenResponse.path("data").path("scopes").toString()
                .contains("application:connect:ping"));
        String accessToken = tokenResponse.path("data").path("access_token").asText();
        activeAccessToken = accessToken;
        assertFalse(accessToken.isBlank());

        JsonNode pingResponse = requestPing(accessToken);
        assertTrue(pingResponse.path("ok").asBoolean());
        assertTrue(pingResponse.path("data").path("connected").asBoolean());
        assertEquals(2, applicationDao.selectById(applicationId).getAccessStatus());

        ResponseDTO<ApplicationCredentialVO> resetResult = applicationService.resetSecret(applicationId);
        assertTrue(resetResult.getOk());
        assertNotNull(resetResult.getData().getAppSecret());

        JsonNode expiredTokenResponse = requestPing(accessToken);
        assertFalse(expiredTokenResponse.path("ok").asBoolean());

        JsonNode renewedTokenResponse =
                requestToken(resetResult.getData().getAppId(), resetResult.getData().getAppSecret());
        assertTrue(renewedTokenResponse.path("ok").asBoolean());
        activeAccessToken = renewedTokenResponse.path("data").path("access_token").asText();
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
            credentialDao.delete(new LambdaQueryWrapper<ApplicationCredentialEntity>()
                    .eq(ApplicationCredentialEntity::getApplicationId, applicationId));
            applicationDao.deleteById(applicationId);
        } finally {
            SmartRequestUtil.remove();
        }
    }
}
