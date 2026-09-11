package com.nexoraone.admin.module.business.application.manager;

import com.nexoraone.admin.constant.AdminRedisKeyConst;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationAccessContextVO;
import com.nexoraone.base.module.support.redis.RedisService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 应用Access Token签发、读取和撤销管理。
 */
@Component
public class ApplicationAccessTokenManager {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Resource
    private RedisService redisService;

    /**
     * 签发不可预测的Access Token并保存安全上下文。
     *
     * @param context Token上下文
     * @param ttlSeconds 有效期秒数
     * @return Access Token明文
     */
    public String issue(ApplicationAccessContextVO context, long ttlSeconds) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = "nxo_at_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redisService.set(buildTokenKey(token), context, ttlSeconds);
        return token;
    }

    /**
     * 根据Access Token读取安全上下文。
     *
     * @param token 访问令牌
     * @return Token上下文，不存在或过期时返回null
     */
    public ApplicationAccessContextVO getContext(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return redisService.getObject(buildTokenKey(token), ApplicationAccessContextVO.class);
    }

    /**
     * 撤销指定Access Token。
     *
     * @param token 访问令牌
     */
    public void revoke(String token) {
        if (token != null && !token.isBlank()) {
            redisService.delete(buildTokenKey(token));
        }
    }

    /**
     * 获取Token在Redis中的剩余有效期。
     *
     * @param token 访问令牌
     * @return 剩余秒数
     */
    public long getExpire(String token) {
        return redisService.getExpire(buildTokenKey(token));
    }

    /**
     * 使用Token摘要构建Redis键，避免明文Token暴露在键名中。
     */
    private String buildTokenKey(String token) {
        return redisService.generateRedisKey(AdminRedisKeyConst.APPLICATION_ACCESS_TOKEN, sha256(token));
    }

    /**
     * 计算SHA-256摘要。
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持SHA-256", exception);
        }
    }
}
