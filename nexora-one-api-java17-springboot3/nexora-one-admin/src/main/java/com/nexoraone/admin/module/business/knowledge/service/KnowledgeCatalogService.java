package com.nexoraone.admin.module.business.knowledge.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexoraone.admin.module.business.ai.dao.AiModelDao;
import com.nexoraone.admin.module.business.ai.domain.entity.AiModelEntity;
import com.nexoraone.admin.module.business.knowledge.dao.KnowledgeMappers.*;
import com.nexoraone.admin.module.business.knowledge.domain.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** 业务知识库和助手配置；底层解析方案及向量实例由上传环节决定。 */
@Service
public class KnowledgeCatalogService {
    @Resource private BaseDao bases;
    @Resource private BaseDocumentDao baseDocuments;
    @Resource private AssistantDao assistants;
    @Resource private AssistantBaseDao assistantBases;
    @Resource private AiModelDao models;
    @Resource private KnowledgeDocumentService documents;

    /** 当前用户知识库清单和真实关联文档。 */
    public List<Map<String, Object>> bases(String keyword) {
        return bases.selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getOwnerId, KnowledgeDocumentService.userId())
                .like(StrUtil.isNotBlank(keyword), KnowledgeBase::getBaseName, keyword)
                .orderByDesc(KnowledgeBase::getUpdateTime)).stream().map(this::baseView).toList();
    }

    /** 创建或编辑知识库，并原子替换关联的已就绪文档。 */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBase saveBase(KnowledgeForms.BaseSave form) {
        Long owner = KnowledgeDocumentService.userId();
        KnowledgeBase base = form.getBaseId() == null ? new KnowledgeBase() : ownedBase(form.getBaseId());
        List<Long> ids = distinct(form.getDocumentIds());
        for (Long id : ids) {
            KnowledgeDocument document = documents.owned(id);
            documents.refresh(document);
            if (!"READY".equals(document.getStatus())) throw new IllegalArgumentException("只能添加已就绪文档");
        }
        base.setOwnerId(owner);
        base.setBaseName(form.getBaseName().trim());
        base.setDescription(form.getDescription());
        base.setEnabledFlag(!Boolean.FALSE.equals(form.getEnabledFlag()));
        base.setUpdateTime(LocalDateTime.now());
        if (base.getBaseId() == null) {
            base.setCreateTime(base.getUpdateTime());
            bases.insert(base);
        } else bases.updateById(base);
        baseDocuments.delete(new LambdaQueryWrapper<KnowledgeBaseDocument>()
                .eq(KnowledgeBaseDocument::getBaseId, base.getBaseId()));
        for (Long id : ids) {
            KnowledgeBaseDocument link = new KnowledgeBaseDocument();
            link.setBaseId(base.getBaseId());
            link.setDocumentId(id);
            baseDocuments.insert(link);
        }
        return base;
    }

    /** 删除无助手引用的业务知识库，不删除源文档或共享向量。 */
    public void deleteBase(Long id) {
        ownedBase(id);
        if (assistantBases.selectCount(new LambdaQueryWrapper<KnowledgeAssistantBase>()
                .eq(KnowledgeAssistantBase::getBaseId, id)) > 0)
            throw new IllegalArgumentException("知识库正被助手使用，请先解除关联");
        bases.deleteById(id);
    }

    /** 助手清单及其关联知识库，不返回管理员模型密钥。 */
    public List<Map<String, Object>> assistants() {
        return assistants.selectList(new LambdaQueryWrapper<KnowledgeAssistant>()
                .eq(KnowledgeAssistant::getOwnerId, KnowledgeDocumentService.userId())
                .orderByDesc(KnowledgeAssistant::getUpdateTime)).stream().map(this::assistantView).toList();
    }

    /** 验证对话模型及所有业务知识库归属，原子保存助手配置。 */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeAssistant saveAssistant(KnowledgeForms.AssistantSave form) {
        AiModelEntity model = models.selectById(form.getModelId());
        if (model == null || !"CHAT".equals(model.getModelType()) || !Boolean.TRUE.equals(model.getEnabledFlag()))
            throw new IllegalArgumentException("请选择已启用的对话模型");
        List<Long> ids = distinct(form.getBaseIds());
        for (Long id : ids) ownedBase(id);
        KnowledgeAssistant assistant = form.getAssistantId() == null
                ? new KnowledgeAssistant() : ownedAssistant(form.getAssistantId());
        assistant.setOwnerId(KnowledgeDocumentService.userId());
        assistant.setAssistantName(form.getAssistantName().trim());
        assistant.setSystemPrompt(form.getSystemPrompt());
        assistant.setModelId(model.getModelId());
        assistant.setTopK(form.getTopK());
        assistant.setScoreThreshold(form.getScoreThreshold());
        assistant.setShowCitations(!Boolean.FALSE.equals(form.getShowCitations()));
        assistant.setEnabledFlag(!Boolean.FALSE.equals(form.getEnabledFlag()));
        assistant.setUpdateTime(LocalDateTime.now());
        if (assistant.getAssistantId() == null) {
            assistant.setCreateTime(assistant.getUpdateTime());
            assistants.insert(assistant);
        } else assistants.updateById(assistant);
        assistantBases.delete(new LambdaQueryWrapper<KnowledgeAssistantBase>()
                .eq(KnowledgeAssistantBase::getAssistantId, assistant.getAssistantId()));
        for (Long id : ids) {
            KnowledgeAssistantBase link = new KnowledgeAssistantBase();
            link.setAssistantId(assistant.getAssistantId());
            link.setBaseId(id);
            assistantBases.insert(link);
        }
        return assistant;
    }

    /** 删除助手时关联知识库与会话按数据库外键级联，原文档不变。 */
    public void deleteAssistant(Long id) {
        ownedAssistant(id);
        assistants.deleteById(id);
    }

    /** 当前登录用户的知识库。 */
    public KnowledgeBase ownedBase(Long id) {
        KnowledgeBase base = bases.selectById(id);
        if (base == null || !KnowledgeDocumentService.userId().equals(base.getOwnerId()))
            throw new IllegalArgumentException("知识库不存在或无权访问");
        return base;
    }

    /** 当前登录用户的助手。 */
    public KnowledgeAssistant ownedAssistant(Long id) {
        KnowledgeAssistant assistant = assistants.selectById(id);
        if (assistant == null || !KnowledgeDocumentService.userId().equals(assistant.getOwnerId()))
            throw new IllegalArgumentException("助手不存在或无权访问");
        return assistant;
    }

    /** 某知识库关联的文档 ID；后台仍需逐一过滤状态。 */
    public List<Long> documentIds(Long baseId) {
        return baseDocuments.selectList(new LambdaQueryWrapper<KnowledgeBaseDocument>()
                .eq(KnowledgeBaseDocument::getBaseId, baseId)).stream()
                .map(KnowledgeBaseDocument::getDocumentId).toList();
    }

    /** 某助手可检索的业务知识库 ID。 */
    public List<Long> assistantBaseIds(Long assistantId) {
        return assistantBases.selectList(new LambdaQueryWrapper<KnowledgeAssistantBase>()
                .eq(KnowledgeAssistantBase::getAssistantId, assistantId)).stream()
                .map(KnowledgeAssistantBase::getBaseId).toList();
    }

    /** 将业务知识库和已就绪文档关系组装成列表项。 */
    private Map<String, Object> baseView(KnowledgeBase base) {
        return Map.of("base", base, "documentIds", documentIds(base.getBaseId()));
    }

    /** 将助手及关联知识库组装成列表项。 */
    private Map<String, Object> assistantView(KnowledgeAssistant assistant) {
        return Map.of("assistant", assistant, "baseIds", assistantBaseIds(assistant.getAssistantId()));
    }

    /** 去掉提交列表中的重复主键并拒绝非法空值。 */
    private List<Long> distinct(List<Long> ids) {
        if (ids == null || ids.stream().anyMatch(id -> id == null || id <= 0))
            throw new IllegalArgumentException("关联主键无效");
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }
}
