package com.nexoraone.admin.module.business.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexoraone.admin.module.business.application.dao.ApplicationCredentialDao;
import com.nexoraone.admin.module.business.application.dao.ApplicationDao;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationCredentialEntity;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationCreateForm;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationCredentialVO;
import com.nexoraone.admin.module.business.application.manager.ApplicationAccessTokenManager;
import com.nexoraone.admin.module.business.application.service.ApplicationService;
import com.nexoraone.base.common.domain.ResponseDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
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
 * App ID/App Secret开放认证链路集成测试。
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

    private Long applicationId;
    private String activeAccessToken;

    /**
     * 验证客户端凭证换取Token、平台连通性和密钥轮换失效机制。
     */
    @Test
    void shouldCompleteClientCredentialsFlowAndInvalidateOldTokenAfterSecretReset() throws Exception {
        ResponseDTO<Map<String, Object>> createResult = applicationService.create(buildApplicationForm());
        assertTrue(createResult.getOk());
        applicationId = Long.valueOf(createResult.getData().get("applicationId").toString());
        ApplicationCredentialVO credential =
                (ApplicationCredentialVO) createResult.getData().get("credential");

        JsonNode invalidTokenResponse = requestToken(credential.getAppId(), "invalid-secret");
        assertFalse(invalidTokenResponse.path("ok").asBoolean());

        JsonNode tokenResponse = requestToken(credential.getAppId(), credential.getAppSecret());
        assertTrue(tokenResponse.path("ok").asBoolean());
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
     * 构造隔离的临时应用。
     */
    private ApplicationCreateForm buildApplicationForm() {
        ApplicationCreateForm form = new ApplicationCreateForm();
        form.setApplicationName("开放认证集成测试");
        form.setApplicationCode("auth-it-" + System.currentTimeMillis());
        form.setApplicationType(2);
        form.setEnterpriseName("NexoraOne测试企业");
        form.setOwnerName("集成测试");
        form.setContact("integration-test@nexoraone.local");
        form.setSummary("用于验证App ID和App Secret真实接入链路的临时应用。");
        form.setHomeUrl("https://example.com");
        return form;
    }

    /**
     * 通过公开HTTP接口换取Access Token。
     */
    private JsonNode requestToken(String appId, String appSecret) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of(
                "appId", appId,
                "appSecret", appSecret,
                "grantType", "client_credentials"), headers);
        ResponseEntity<String> response = new RestTemplate().postForEntity(
                baseUrl() + "/open/application/oauth/token", request, String.class);
        return objectMapper.readTree(response.getBody());
    }

    /**
     * 携带Bearer Token请求平台连通性接口。
     */
    private JsonNode requestPing(String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<String> response = new RestTemplate().exchange(
                baseUrl() + "/open/application/connect/ping",
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
     * 删除本次测试创建的数据。
     */
    @AfterEach
    void cleanUp() {
        if (activeAccessToken != null) {
            accessTokenManager.revoke(activeAccessToken);
        }
        if (applicationId == null) {
            return;
        }
        credentialDao.delete(new LambdaQueryWrapper<ApplicationCredentialEntity>()
                .eq(ApplicationCredentialEntity::getApplicationId, applicationId));
        applicationDao.deleteById(applicationId);
    }
}
