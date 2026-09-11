package com.nexoraone.admin.module.business.application.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 应用接口鉴权与安全配置。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationSecurityConfigVO {

    private static final Set<String> AUTH_MODES = Set.of("SIGNATURE", "TOKEN", "IP");
    private static final Set<String> SIGNATURE_ALGORITHMS = Set.of("HMAC-SHA256", "HMAC-SHA512");
    private static final String HEADER_NAME_PATTERN = "^[!#$%&'*+.^_`|~0-9A-Za-z-]+$";

    /** 鉴权模式：SIGNATURE为令牌加签名，TOKEN为仅令牌，IP为令牌加来源IP白名单。 */
    private String authMode = "SIGNATURE";
    /** 签名算法。 */
    private String signatureAlgorithm = "HMAC-SHA256";
    /** 签名请求头名称。 */
    private String signatureHeader = "X-Signature";
    /** 时间戳请求头名称。 */
    private String timestampHeader = "X-Timestamp";
    /** 随机串请求头名称。 */
    private String nonceHeader = "X-Nonce";
    /** 防重放有效期，单位为秒。 */
    private Integer replayTtl = 300;
    /** 单个应用调用单个API的每秒请求上限。 */
    private Integer qpsLimit = 50;
    /** 单个应用调用单个API的每日请求上限。 */
    private Long dailyLimit = 100_000L;
    /** 转发到内部服务的请求超时时间，单位为秒。 */
    private Integer timeoutSeconds = 10;
    /** 允许调用开放API的来源IP或CIDR列表。 */
    private List<String> ipWhitelist = new ArrayList<>();
    /** 是否强制使用HTTPS。 */
    private Boolean forceHttps = true;
    /** 是否启用随机串防重放校验。 */
    private Boolean replayProtection = true;

    /**
     * 校验配置是否可以由网关完整执行。
     *
     * @return 校验通过时返回空字符串，否则返回错误说明
     */
    public String validate() {
        String actualAuthMode = StringUtils.upperCase(StringUtils.defaultIfBlank(authMode, "SIGNATURE"));
        if (!AUTH_MODES.contains(actualAuthMode)) {
            return "鉴权模式不正确";
        }
        String actualAlgorithm = StringUtils.upperCase(
                StringUtils.defaultIfBlank(signatureAlgorithm, "HMAC-SHA256"));
        if (!SIGNATURE_ALGORITHMS.contains(actualAlgorithm)) {
            return "签名算法不正确";
        }
        if (!isHeaderName(signatureHeader) || !isHeaderName(timestampHeader) || !isHeaderName(nonceHeader)) {
            return "签名相关请求头名称不正确";
        }
        Set<String> headerNames = new HashSet<>();
        headerNames.add(signatureHeader.toLowerCase());
        headerNames.add(timestampHeader.toLowerCase());
        headerNames.add(nonceHeader.toLowerCase());
        if (headerNames.size() != 3) {
            return "签名、时间戳和随机串请求头不能重复";
        }
        if (replayTtl == null || replayTtl < 30 || replayTtl > 900) {
            return "防重放有效期必须在30到900秒之间";
        }
        if (qpsLimit == null || qpsLimit < 1 || qpsLimit > 10_000) {
            return "每秒请求上限必须在1到10000之间";
        }
        if (dailyLimit == null || dailyLimit < 1 || dailyLimit > 1_000_000_000L) {
            return "每日调用上限必须在1到1000000000之间";
        }
        if (timeoutSeconds == null || timeoutSeconds < 1 || timeoutSeconds > 120) {
            return "请求超时必须在1到120秒之间";
        }
        if ("IP".equals(actualAuthMode) && (ipWhitelist == null || ipWhitelist.isEmpty())) {
            return "IP白名单鉴权至少需要配置一个IP或CIDR";
        }
        if (ipWhitelist != null) {
            for (String rule : ipWhitelist) {
                if (!isIpRule(rule)) {
                    return "IP白名单包含无效地址：" + rule;
                }
            }
        }
        return "";
    }

    /**
     * 规范化配置并补齐安全默认值。
     *
     * @return 当前配置
     */
    public ApplicationSecurityConfigVO normalize() {
        authMode = StringUtils.upperCase(StringUtils.defaultIfBlank(authMode, "SIGNATURE"));
        if (!AUTH_MODES.contains(authMode)) {
            authMode = "SIGNATURE";
        }
        signatureAlgorithm = StringUtils.upperCase(
                StringUtils.defaultIfBlank(signatureAlgorithm, "HMAC-SHA256"));
        if (!SIGNATURE_ALGORITHMS.contains(signatureAlgorithm)) {
            signatureAlgorithm = "HMAC-SHA256";
        }
        signatureHeader = normalizeHeader(signatureHeader, "X-Signature");
        timestampHeader = normalizeHeader(timestampHeader, "X-Timestamp");
        nonceHeader = normalizeHeader(nonceHeader, "X-Nonce");
        replayTtl = clamp(replayTtl, 30, 900, 300);
        qpsLimit = clamp(qpsLimit, 1, 10_000, 50);
        dailyLimit = clamp(dailyLimit, 1L, 1_000_000_000L, 100_000L);
        timeoutSeconds = clamp(timeoutSeconds, 1, 120, 10);
        forceHttps = !Boolean.FALSE.equals(forceHttps);
        replayProtection = !Boolean.FALSE.equals(replayProtection);
        ipWhitelist = ipWhitelist == null ? new ArrayList<>() : ipWhitelist.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        return this;
    }

    /**
     * 规范化HTTP请求头名称。
     */
    private String normalizeHeader(String value, String defaultValue) {
        String header = StringUtils.defaultIfBlank(value, defaultValue).trim();
        return header.matches(HEADER_NAME_PATTERN) ? header : defaultValue;
    }

    /**
     * 判断请求头名称是否合法。
     */
    private boolean isHeaderName(String value) {
        return StringUtils.isNotBlank(value) && value.trim().matches(HEADER_NAME_PATTERN);
    }

    /**
     * 判断IP或CIDR规则是否合法。
     */
    private boolean isIpRule(String value) {
        if (StringUtils.isBlank(value)
                || !value.trim().matches("^[0-9a-fA-F:.]+(?:/\\d{1,3})?$")) {
            return false;
        }
        try {
            String[] parts = value.trim().split("/", 2);
            int maximumPrefix = InetAddress.getByName(parts[0]).getAddress().length * 8;
            int prefix = parts.length == 1 ? maximumPrefix : Integer.parseInt(parts[1]);
            return prefix >= 0 && prefix <= maximumPrefix;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * 将整数限制在允许范围内。
     */
    private Integer clamp(Integer value, int minimum, int maximum, int defaultValue) {
        int actual = value == null ? defaultValue : value;
        return Math.max(minimum, Math.min(actual, maximum));
    }

    /**
     * 将长整数限制在允许范围内。
     */
    private Long clamp(Long value, long minimum, long maximum, long defaultValue) {
        long actual = value == null ? defaultValue : value;
        return Math.max(minimum, Math.min(actual, maximum));
    }
}
