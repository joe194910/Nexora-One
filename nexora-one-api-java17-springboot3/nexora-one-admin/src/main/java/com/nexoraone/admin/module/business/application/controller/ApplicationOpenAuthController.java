package com.nexoraone.admin.module.business.application.controller;

import com.nexoraone.admin.module.business.application.domain.form.ApplicationTokenForm;
import com.nexoraone.admin.module.business.application.domain.form.ApplicationSsoTokenForm;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationAccessTokenVO;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationConnectVO;
import com.nexoraone.admin.module.business.application.service.ApplicationOpenAuthService;
import com.nexoraone.admin.module.business.application.service.ApplicationPortalService;
import com.nexoraone.base.common.annoation.NoNeedLogin;
import com.nexoraone.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 第三方应用使用App ID和App Secret访问平台的公开认证接口。
 */
@RestController
@RequestMapping({"/open-api", "/open/application"})
@Tag(name = "开放平台-应用认证")
public class ApplicationOpenAuthController {

    @Resource
    private ApplicationOpenAuthService openAuthService;
    @Resource
    private ApplicationPortalService portalService;

    /**
     * 使用JSON格式的客户端凭证换取Access Token。
     */
    @NoNeedLogin
    @Operation(summary = "使用App ID和App Secret换取Access Token")
    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<ApplicationAccessTokenVO> issueToken(
            @RequestBody @Valid ApplicationTokenForm form) {
        return openAuthService.issueToken(form);
    }

    /**
     * 使用OAuth 2.0表单格式的客户端凭证换取Access Token。
     */
    @NoNeedLogin
    @Operation(summary = "使用表单格式换取Access Token")
    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseDTO<ApplicationAccessTokenVO> issueTokenByForm(
            @RequestParam("app_id") String appId,
            @RequestParam("app_secret") String appSecret,
            @RequestParam(value = "grant_type", defaultValue = "client_credentials") String grantType) {
        ApplicationTokenForm form = new ApplicationTokenForm();
        form.setAppId(appId);
        form.setAppSecret(appSecret);
        form.setGrantType(grantType);
        return openAuthService.issueToken(form);
    }

    /**
     * 携带Access Token请求平台，验证完整接入链路并标记应用已接入。
     */
    @NoNeedLogin
    @Operation(summary = "验证Access Token和平台连通性")
    @GetMapping("/connect/ping")
    public ResponseDTO<ApplicationConnectVO> ping(
            @Parameter(description = "Bearer Access Token", required = true)
            @RequestHeader("Authorization") String authorization) {
        return openAuthService.authorize(
                authorization, ApplicationOpenAuthService.CONNECTION_VERIFICATION_SCOPE, true);
    }

    /**
     * 第三方应用使用一次性授权码换取当前登录用户信息。
     */
    @NoNeedLogin
    @Operation(summary = "使用SSO授权码换取用户信息")
    @PostMapping(value = "/sso/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Map<String, Object>> exchangeSsoCode(
            @RequestBody @Valid ApplicationSsoTokenForm form) {
        return portalService.exchangeSsoCode(form);
    }
}
