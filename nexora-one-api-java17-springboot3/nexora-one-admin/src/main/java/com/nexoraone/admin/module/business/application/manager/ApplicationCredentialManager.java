package com.nexoraone.admin.module.business.application.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexoraone.admin.module.business.application.dao.ApplicationCredentialDao;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationCredentialEntity;
import com.nexoraone.admin.module.business.application.domain.vo.ApplicationCredentialVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 应用凭证生成、脱敏和失效管理。
 */
@Component
public class ApplicationCredentialManager {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Resource
    private ApplicationCredentialDao credentialDao;

    /**
     * 为新应用创建唯一凭证，完整密钥只通过本次返回。
     */
    public ApplicationCredentialVO createCredential(Long applicationId) {
        String secret = generateSecret();
        ApplicationCredentialEntity entity = new ApplicationCredentialEntity();
        entity.setApplicationId(applicationId);
        entity.setAppId(generateAppId());
        entity.setSecretHash(sha256(secret));
        entity.setSecretHint(secret.substring(secret.length() - 4));
        entity.setStatus(1);
        entity.setVersionNo(1);
        credentialDao.insert(entity);
        return toVO(entity, secret);
    }

    /**
     * 查询应用当前有效凭证，仅返回脱敏密钥。
     */
    public ApplicationCredentialVO getMaskedCredential(Long applicationId) {
        ApplicationCredentialEntity entity = getActiveCredential(applicationId);
        return entity == null ? null : toVO(entity, null);
    }

    /**
     * 重置应用密钥并立即使旧密钥失效。
     */
    public ApplicationCredentialVO resetSecret(Long applicationId) {
        ApplicationCredentialEntity current = getActiveCredential(applicationId);
        if (current == null) {
            return createCredential(applicationId);
        }
        String secret = generateSecret();
        current.setSecretHash(sha256(secret));
        current.setSecretHint(secret.substring(secret.length() - 4));
        current.setVersionNo(current.getVersionNo() + 1);
        current.setLastResetTime(LocalDateTime.now());
        credentialDao.updateById(current);
        return toVO(current, secret);
    }

    /**
     * 根据App ID校验App Secret，校验过程使用恒定时间比较，避免时序攻击。
     *
     * @param appId App ID
     * @param appSecret App Secret明文
     * @return 校验成功时返回当前有效凭证，否则返回null
     */
    public ApplicationCredentialEntity verifyCredential(String appId, String appSecret) {
        if (appId == null || appSecret == null) {
            return null;
        }
        ApplicationCredentialEntity entity = credentialDao.selectOne(
                new LambdaQueryWrapper<ApplicationCredentialEntity>()
                        .eq(ApplicationCredentialEntity::getAppId, appId)
                        .eq(ApplicationCredentialEntity::getStatus, 1)
                        .last("limit 1"));
        if (entity == null) {
            return null;
        }
        byte[] expected = entity.getSecretHash().getBytes(StandardCharsets.UTF_8);
        byte[] actual = sha256(appSecret).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual) ? entity : null;
    }

    /**
     * 查询应用当前有效凭证，供Access Token版本校验使用。
     *
     * @param applicationId 应用主键
     * @return 当前有效凭证
     */
    public ApplicationCredentialEntity getCurrentCredential(Long applicationId) {
        return getActiveCredential(applicationId);
    }

    /**
     * 查询应用当前有效凭证实体。
     */
    private ApplicationCredentialEntity getActiveCredential(Long applicationId) {
        return credentialDao.selectOne(new LambdaQueryWrapper<ApplicationCredentialEntity>()
                .eq(ApplicationCredentialEntity::getApplicationId, applicationId)
                .eq(ApplicationCredentialEntity::getStatus, 1)
                .orderByDesc(ApplicationCredentialEntity::getVersionNo)
                .last("limit 1"));
    }

    /**
     * 生成不易预测的应用App ID。
     */
    private String generateAppId() {
        byte[] suffix = new byte[6];
        SECURE_RANDOM.nextBytes(suffix);
        return "app_nxo_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "_"
                + HexFormat.of().formatHex(suffix);
    }

    /**
     * 生成URL安全的高熵App Secret。
     */
    private String generateSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 对密钥计算不可逆摘要，数据库不保存明文。
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持SHA-256", exception);
        }
    }

    /**
     * 将凭证实体转换为安全的前端展示对象。
     */
    private ApplicationCredentialVO toVO(ApplicationCredentialEntity entity, String secret) {
        ApplicationCredentialVO vo = new ApplicationCredentialVO();
        vo.setAppId(entity.getAppId());
        vo.setAppSecret(secret);
        vo.setMaskedSecret("************************" + entity.getSecretHint());
        vo.setVersionNo(entity.getVersionNo());
        vo.setCreateTime(entity.getCreateTime());
        vo.setLastResetTime(entity.getLastResetTime());
        return vo;
    }
}
