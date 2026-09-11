package com.nexoraone.admin.module.business.application.manager;

import com.nexoraone.admin.module.business.application.domain.entity.ApplicationCredentialEntity;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 应用请求签名算法单元测试。
 */
class ApplicationCredentialManagerTest {

    /**
     * 验证调用方按公开协议生成的SHA256签名可以通过平台校验。
     */
    @Test
    void shouldVerifySha256SignatureGeneratedFromAppSecret() throws Exception {
        verifyProtocol("HMAC-SHA256", "HmacSHA256");
    }

    /**
     * 验证调用方按公开协议生成的SHA512签名可以通过平台校验。
     */
    @Test
    void shouldVerifySha512SignatureGeneratedFromAppSecret() throws Exception {
        verifyProtocol("HMAC-SHA512", "HmacSHA512");
    }

    /**
     * 按调用方协议生成签名并验证篡改后的查询参数无法通过校验。
     */
    private void verifyProtocol(String configuredAlgorithm, String javaAlgorithm) throws Exception {
        String appSecret = "test-secret-visible-only-once";
        String signingKey = sha256(appSecret);
        String canonicalRequest = "GET\n/open-api/v1/users?page=1\n1726027200000\nnonce-1\n"
                + sha256("");
        String signature = hmac(signingKey, canonicalRequest, javaAlgorithm);

        ApplicationCredentialEntity credential = new ApplicationCredentialEntity();
        credential.setSecretHash(signingKey);
        ApplicationCredentialManager manager = new ApplicationCredentialManager();

        assertTrue(manager.verifySignature(
                credential, canonicalRequest, signature, configuredAlgorithm));
        assertFalse(manager.verifySignature(
                credential, canonicalRequest.replace("page=1", "page=2"),
                signature, configuredAlgorithm));
    }

    /**
     * 计算字符串的小写十六进制SHA-256摘要。
     */
    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 计算小写十六进制HMAC签名。
     */
    private String hmac(String key, String value, String algorithm) throws Exception {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
