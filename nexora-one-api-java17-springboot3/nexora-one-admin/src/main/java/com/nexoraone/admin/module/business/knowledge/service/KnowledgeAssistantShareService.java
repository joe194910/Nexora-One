package com.nexoraone.admin.module.business.knowledge.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nexoraone.admin.module.business.knowledge.dao.KnowledgeMappers.AssistantShareDao;
import com.nexoraone.admin.module.business.knowledge.domain.KnowledgeAssistant;
import com.nexoraone.admin.module.business.knowledge.domain.KnowledgeAssistantShare;
import com.nexoraone.admin.module.business.knowledge.domain.KnowledgeBase;
import com.nexoraone.admin.module.business.knowledge.domain.KnowledgeForms;
import com.nexoraone.admin.module.system.employee.dao.EmployeeDao;
import com.nexoraone.admin.module.system.employee.domain.entity.EmployeeEntity;
import com.nexoraone.base.module.support.redis.RedisService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 智能助手商店分享、分享记录管理和免登录访问服务。 */
@Service
public class KnowledgeAssistantShareService {

    /** 分享有效期上限，避免生成长期无人维护的公开入口。 */
    private static final long MAX_VALID_DAYS = 365;
    /** 分享来源固定为智能助手商店。 */
    private static final String SOURCE_STORE = "STORE";
    /** 公开访问失败时统一返回的提示，避免泄露内部状态。 */
    private static final String INVALID_MESSAGE = "分享链接已失效或当前不可访问";
    /** 单个来源 IP 对同一分享每分钟最多发起的问答次数。 */
    private static final long CHAT_LIMIT_PER_IP_MINUTE = 20;
    /** 单个分享链接每分钟最多发起的问答次数，限制分布式滥用。 */
    private static final long CHAT_LIMIT_PER_SHARE_MINUTE = 300;

    @Resource
    private AssistantShareDao shares;
    @Resource
    private EmployeeDao employees;
    @Resource
    private KnowledgeCatalogService catalog;
    @Resource
    private KnowledgeChatService chat;
    @Resource
    private KnowledgeAssistantShareLifecycleService lifecycle;
    @Resource
    private RedisService redisService;

    /**
     * 为当前商店中可见的助手创建免登录分享链接。
     *
     * @param assistantId 助手主键
     * @param form 分享有效期和访问源
     * @return 新建分享记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Long assistantId, KnowledgeForms.AssistantShareCreate form) {
        LocalDateTime now = LocalDateTime.now();
        if (form.getExpireTime().isAfter(now.plusDays(MAX_VALID_DAYS))) {
            throw new IllegalArgumentException("分享有效期最长为 " + MAX_VALID_DAYS + " 天");
        }
        KnowledgeAssistant assistant = catalog.storeAssistant(assistantId);
        Long sharerId = KnowledgeDocumentService.userId();
        EmployeeEntity sharer = employees.selectById(sharerId);
        if (!isActiveEmployee(sharer)) {
            throw new IllegalArgumentException("当前分享人账号不可用");
        }

        String origin = normalizeOrigin(form.getAccessOrigin());
        String shareCode = UUID.randomUUID().toString().replace("-", "");
        KnowledgeAssistantShare share = new KnowledgeAssistantShare();
        share.setAssistantId(assistant.getAssistantId());
        share.setShareCode(shareCode);
        share.setShareUrl(origin + "/api/knowledge/public/assistant-shares/" + shareCode + "/open");
        share.setSharerId(sharerId);
        share.setSharerName(StrUtil.blankToDefault(sharer.getActualName(), sharer.getLoginName()));
        share.setShareSource(SOURCE_STORE);
        share.setExpireTime(form.getExpireTime());
        share.setEnabledFlag(true);
        share.setAccessCount(0L);
        share.setCreateTime(now);
        share.setUpdateTime(now);
        shares.insert(share);
        return managementView(share, assistant, sharerId);
    }

    /**
     * 查询当前用户有权管理的助手分享记录。
     *
     * @param assistantId 助手主键
     * @return 分享记录列表
     */
    public List<Map<String, Object>> records(Long assistantId) {
        Long currentUserId = KnowledgeDocumentService.userId();
        KnowledgeAssistant assistant = catalog.findAssistant(assistantId);
        LambdaQueryWrapper<KnowledgeAssistantShare> query = new LambdaQueryWrapper<KnowledgeAssistantShare>()
                .eq(KnowledgeAssistantShare::getAssistantId, assistantId)
                .orderByDesc(KnowledgeAssistantShare::getCreateTime);
        if (assistant == null || !currentUserId.equals(assistant.getOwnerId())) {
            query.eq(KnowledgeAssistantShare::getSharerId, currentUserId);
        }
        return shares.selectList(query).stream()
                .map(share -> managementView(share, assistant, currentUserId))
                .toList();
    }

    /**
     * 分享人可以停用自己的链接，助手创建者可以停用该助手的全部人员分享。
     *
     * @param assistantId 助手主键
     * @param shareId 分享记录主键
     */
    public void disable(Long assistantId, Long shareId) {
        Long currentUserId = KnowledgeDocumentService.userId();
        KnowledgeAssistantShare share = shares.selectById(shareId);
        if (share == null || !assistantId.equals(share.getAssistantId())) {
            throw new IllegalArgumentException("分享记录不存在");
        }
        KnowledgeAssistant assistant = catalog.findAssistant(assistantId);
        boolean assistantOwner = assistant != null && currentUserId.equals(assistant.getOwnerId());
        if (!assistantOwner && !currentUserId.equals(share.getSharerId())) {
            throw new IllegalArgumentException("无权停用该分享链接");
        }
        lifecycle.disableRecord(shareId, KnowledgeAssistantShareLifecycleService.USER_DISABLED);
    }

    /**
     * 读取公开分享详情，并记录一次成功访问。
     *
     * @param shareCode 分享编码
     * @return 公开页面所需的助手信息
     */
    public Map<String, Object> publicDetail(String shareCode) {
        ValidatedShare validated = validatePublicShare(shareCode);
        LocalDateTime now = LocalDateTime.now();
        shares.update(null, new LambdaUpdateWrapper<KnowledgeAssistantShare>()
                .eq(KnowledgeAssistantShare::getShareId, validated.share().getShareId())
                .eq(KnowledgeAssistantShare::getEnabledFlag, true)
                .setSql("access_count = access_count + 1")
                .set(KnowledgeAssistantShare::getLastAccessTime, now)
                .set(KnowledgeAssistantShare::getUpdateTime, now));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shareCode", validated.share().getShareCode());
        result.put("expireTime", validated.share().getExpireTime());
        result.put("assistantId", validated.assistant().getAssistantId());
        result.put("assistantName", validated.assistant().getAssistantName());
        result.put("showCitations", validated.assistant().getShowCitations());
        result.put("ownerName", catalog.employeeDisplayName(validated.assistant().getOwnerId()));
        result.put("baseNames", validated.bases().stream().map(KnowledgeBase::getBaseName).toList());
        return result;
    }

    /**
     * 校验分享状态后执行匿名问答；匿名链路不会执行 MCP 或其他业务工具。
     *
     * @param shareCode 分享编码
     * @param form 问题和浏览器本地历史
     * @param clientIp 访问者 IP
     * @return 模型回答及知识库引用
     */
    public Map<String, Object> publicChat(
            String shareCode,
            KnowledgeForms.PublicChat form,
            String clientIp) {
        ValidatedShare validated = validatePublicShare(shareCode);
        enforcePublicChatRateLimit(validated.share().getShareId(), clientIp);
        return chat.publicChat(validated.assistant(), validated.bases(), form);
    }

    /** 使用 Redis 对公开问答进行按分钟限流，防止匿名分享消耗异常模型额度。 */
    private void enforcePublicChatRateLimit(Long shareId, String clientIp) {
        long minute = System.currentTimeMillis() / 60_000L;
        String normalizedIp = StrUtil.blankToDefault(clientIp, "unknown");
        long ipCount = redisService.increment(
                "knowledge:share-chat:ip:" + minute + ":" + shareId + ":" + normalizedIp, 120);
        if (ipCount > CHAT_LIMIT_PER_IP_MINUTE) {
            throw new IllegalArgumentException("请求过于频繁，请稍后重试");
        }
        long shareCount = redisService.increment(
                "knowledge:share-chat:share:" + minute + ":" + shareId, 120);
        if (shareCount > CHAT_LIMIT_PER_SHARE_MINUTE) {
            throw new IllegalArgumentException("当前分享访问繁忙，请稍后重试");
        }
    }

    /**
     * 校验分享后生成适配 PC 或移动端的问答页面地址。
     *
     * @param shareCode 分享编码
     * @param mobile 是否为移动设备
     * @return 前端问答页面地址
     */
    public String publicPageUrl(String shareCode, boolean mobile) {
        KnowledgeAssistantShare share = validatePublicShare(shareCode).share();
        String origin = URI.create(share.getShareUrl()).resolve("/").toString();
        String path = mobile
                ? "h5/#/pages/assistant/chat?shareCode=" + shareCode
                : "admin/#/share/assistant/" + shareCode;
        return origin + path;
    }

    /** 校验公开分享的每一项动态依赖，并将检测出的失效原因持久化。 */
    private ValidatedShare validatePublicShare(String shareCode) {
        if (StrUtil.isBlank(shareCode)) {
            throw new IllegalArgumentException(INVALID_MESSAGE);
        }
        KnowledgeAssistantShare share = shares.selectOne(new LambdaQueryWrapper<KnowledgeAssistantShare>()
                .eq(KnowledgeAssistantShare::getShareCode, shareCode)
                .last("LIMIT 1"));
        if (share == null || !Boolean.TRUE.equals(share.getEnabledFlag())) {
            throw new IllegalArgumentException(INVALID_MESSAGE);
        }
        LocalDateTime now = LocalDateTime.now();
        if (share.getExpireTime() == null || !share.getExpireTime().isAfter(now)) {
            invalidate(share, KnowledgeAssistantShareLifecycleService.EXPIRED);
        }
        EmployeeEntity sharer = employees.selectById(share.getSharerId());
        if (!isActiveEmployee(sharer)) {
            invalidate(share, KnowledgeAssistantShareLifecycleService.SHARER_UNAVAILABLE);
        }
        KnowledgeAssistant assistant = catalog.findAssistant(share.getAssistantId());
        if (assistant == null) {
            invalidate(share, KnowledgeAssistantShareLifecycleService.ASSISTANT_DELETED);
        }
        try {
            assistant = catalog.storeAssistant(share.getAssistantId());
        } catch (IllegalArgumentException exception) {
            invalidate(share, KnowledgeAssistantShareLifecycleService.ASSISTANT_UNAVAILABLE);
        }
        List<KnowledgeBase> bases = catalog.enabledBases(assistant.getAssistantId());
        if (bases.isEmpty()) {
            invalidate(share, KnowledgeAssistantShareLifecycleService.ASSISTANT_UNAVAILABLE);
        }
        return new ValidatedShare(share, assistant, bases);
    }

    /** 持久化动态失效状态后终止当前公开请求。 */
    private void invalidate(KnowledgeAssistantShare share, String reason) {
        lifecycle.disableRecordPersistently(share.getShareId(), reason);
        throw new IllegalArgumentException(INVALID_MESSAGE);
    }

    /** 生成管理端展示结构，并区分有效、过期、主动停用和系统停用。 */
    private Map<String, Object> managementView(
            KnowledgeAssistantShare share,
            KnowledgeAssistant assistant,
            Long currentUserId) {
        String status = shareStatus(share);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("share", share);
        view.put("assistantName", assistant == null ? "已删除助手" : assistant.getAssistantName());
        view.put("status", status);
        view.put("statusText", statusText(status));
        view.put("disableReasonText", disableReasonText(share.getDisableReason()));
        view.put("canDisable", "ACTIVE".equals(status)
                && (currentUserId.equals(share.getSharerId())
                || assistant != null && currentUserId.equals(assistant.getOwnerId())));
        return view;
    }

    /** 计算当前分享状态。 */
    private String shareStatus(KnowledgeAssistantShare share) {
        if (Boolean.TRUE.equals(share.getEnabledFlag())
                && share.getExpireTime() != null
                && share.getExpireTime().isAfter(LocalDateTime.now())) {
            return "ACTIVE";
        }
        if (Boolean.TRUE.equals(share.getEnabledFlag())
                || KnowledgeAssistantShareLifecycleService.EXPIRED.equals(share.getDisableReason())) {
            return "EXPIRED";
        }
        return KnowledgeAssistantShareLifecycleService.USER_DISABLED.equals(share.getDisableReason())
                ? "USER_DISABLED"
                : "SYSTEM_DISABLED";
    }

    /** 将状态编码转换为页面文案。 */
    private String statusText(String status) {
        return switch (status) {
            case "ACTIVE" -> "有效";
            case "EXPIRED" -> "已过期";
            case "USER_DISABLED" -> "已停用";
            default -> "系统失效";
        };
    }

    /** 将停用原因编码转换为审计文案。 */
    private String disableReasonText(String reason) {
        if (reason == null) {
            return "";
        }
        return switch (reason) {
            case KnowledgeAssistantShareLifecycleService.USER_DISABLED -> "分享人或助手创建者主动停用";
            case KnowledgeAssistantShareLifecycleService.ASSISTANT_UNPUBLISHED -> "智能助手已下架";
            case KnowledgeAssistantShareLifecycleService.ASSISTANT_UNAVAILABLE -> "智能助手或关联知识库不可用";
            case KnowledgeAssistantShareLifecycleService.ASSISTANT_DELETED -> "智能助手已删除";
            case KnowledgeAssistantShareLifecycleService.SHARER_UNAVAILABLE -> "分享人账号不可用";
            case KnowledgeAssistantShareLifecycleService.EXPIRED -> "已超过分享有效期";
            default -> "系统已停用";
        };
    }

    /** 仅允许未停用且未删除的人员创建和维持公开分享。 */
    private boolean isActiveEmployee(EmployeeEntity employee) {
        return employee != null
                && !Boolean.TRUE.equals(employee.getDisabledFlag())
                && !Boolean.TRUE.equals(employee.getDeletedFlag());
    }

    /** 规范化浏览器访问源，禁止路径、参数和用户信息进入分享链接。 */
    private String normalizeOrigin(String accessOrigin) {
        try {
            URI uri = URI.create(accessOrigin.trim());
            if (!List.of("http", "https").contains(uri.getScheme())
                    || StrUtil.isBlank(uri.getAuthority())
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null
                    || !(StrUtil.isBlank(uri.getPath()) || "/".equals(uri.getPath()))) {
                throw new IllegalArgumentException("分享访问地址格式不正确");
            }
            URI origin = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null);
            return StrUtil.removeSuffix(origin.toString(), "/");
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("分享访问地址格式不正确");
        }
    }

    /** 已通过公开访问校验的分享上下文。 */
    private record ValidatedShare(
            KnowledgeAssistantShare share,
            KnowledgeAssistant assistant,
            List<KnowledgeBase> bases) {
    }
}
