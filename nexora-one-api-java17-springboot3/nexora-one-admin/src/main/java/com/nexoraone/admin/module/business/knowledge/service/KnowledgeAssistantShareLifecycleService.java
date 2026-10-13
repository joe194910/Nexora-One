package com.nexoraone.admin.module.business.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nexoraone.admin.module.business.knowledge.dao.KnowledgeMappers.AssistantShareDao;
import com.nexoraone.admin.module.business.knowledge.domain.KnowledgeAssistantShare;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 统一处理助手分享链接的主动停用和系统自动失效。 */
@Service
public class KnowledgeAssistantShareLifecycleService {

    /** 分享人主动停用。 */
    public static final String USER_DISABLED = "USER_DISABLED";
    /** 助手被明确下架。 */
    public static final String ASSISTANT_UNPUBLISHED = "ASSISTANT_UNPUBLISHED";
    /** 助手因禁用或知识库不可用而失效。 */
    public static final String ASSISTANT_UNAVAILABLE = "ASSISTANT_UNAVAILABLE";
    /** 助手已经删除。 */
    public static final String ASSISTANT_DELETED = "ASSISTANT_DELETED";
    /** 分享人已经停用或删除。 */
    public static final String SHARER_UNAVAILABLE = "SHARER_UNAVAILABLE";
    /** 分享链接已经超过有效期。 */
    public static final String EXPIRED = "EXPIRED";

    @Resource
    private AssistantShareDao shares;

    /**
     * 在助手业务事务中同步停用该助手的全部有效分享。
     *
     * @param assistantId 助手主键
     * @param reason 停用原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void disableByAssistant(Long assistantId, String reason) {
        if (assistantId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        shares.update(null, new LambdaUpdateWrapper<KnowledgeAssistantShare>()
                .eq(KnowledgeAssistantShare::getAssistantId, assistantId)
                .eq(KnowledgeAssistantShare::getEnabledFlag, true)
                .set(KnowledgeAssistantShare::getEnabledFlag, false)
                .set(KnowledgeAssistantShare::getDisableReason, reason)
                .set(KnowledgeAssistantShare::getDisabledTime, now)
                .set(KnowledgeAssistantShare::getUpdateTime, now));
    }

    /**
     * 由分享人或助手创建者主动停用单条分享。
     *
     * @param shareId 分享记录主键
     * @param reason 停用原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void disableRecord(Long shareId, String reason) {
        disableRecordInternal(shareId, reason);
    }

    /**
     * 公开访问发现动态失效时独立提交停用结果，避免后续校验异常回滚审计状态。
     *
     * @param shareId 分享记录主键
     * @param reason 停用原因
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void disableRecordPersistently(Long shareId, String reason) {
        disableRecordInternal(shareId, reason);
    }

    /** 更新单条仍处于启用状态的分享记录。 */
    private void disableRecordInternal(Long shareId, String reason) {
        if (shareId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        shares.update(null, new LambdaUpdateWrapper<KnowledgeAssistantShare>()
                .eq(KnowledgeAssistantShare::getShareId, shareId)
                .eq(KnowledgeAssistantShare::getEnabledFlag, true)
                .set(KnowledgeAssistantShare::getEnabledFlag, false)
                .set(KnowledgeAssistantShare::getDisableReason, reason)
                .set(KnowledgeAssistantShare::getDisabledTime, now)
                .set(KnowledgeAssistantShare::getUpdateTime, now));
    }
}
