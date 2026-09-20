package com.nexoraone.admin.module.business.knowledge.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexoraone.admin.module.business.ai.dao.AiModelDao;
import com.nexoraone.admin.module.business.ai.domain.entity.AiModelEntity;
import com.nexoraone.admin.module.business.knowledge.dao.KnowledgeMappers.*;
import com.nexoraone.admin.module.business.knowledge.domain.*;
import com.nexoraone.admin.module.business.mcp.domain.AiToolForms;
import com.nexoraone.admin.module.business.mcp.service.AiToolService;
import com.nexoraone.admin.module.system.employee.dao.EmployeeDao;
import com.nexoraone.admin.module.system.employee.domain.entity.EmployeeEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 知识库、已发布助手、助手收藏及助手工具配置服务。 */
@Service
public class KnowledgeCatalogService {
    @Resource private BaseDao bases;
    @Resource private BaseDocumentDao baseDocuments;
    @Resource private AssistantDao assistants;
    @Resource private AssistantFavoriteDao assistantFavorites;
    @Resource private AssistantBaseDao assistantBases;
    @Resource private AiModelDao models;
    @Resource private EmployeeDao employees;
    @Resource private KnowledgeDocumentService documents;
    @Resource private AiToolService aiTools;

    public List<Map<String, Object>> bases(String keyword) {
        return bases.selectList(new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getOwnerId, KnowledgeDocumentService.userId())
                        .like(StrUtil.isNotBlank(keyword), KnowledgeBase::getBaseName, keyword)
                        .orderByDesc(KnowledgeBase::getUpdateTime)).stream()
                .map(this::baseView).toList();
    }

    /** Homepage knowledge-base section only shows the current user's bases. */
    public List<Map<String, Object>> availableBases() {
        return bases.selectList(new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getOwnerId, KnowledgeDocumentService.userId())
                        .orderByDesc(KnowledgeBase::getUpdateTime)).stream()
                .map(this::baseView).toList();
    }

    /** Published assistants visible in the assistant store. */
    public List<Map<String, Object>> assistantStore(String keyword) {
        Long owner = KnowledgeDocumentService.userId();
        Set<Long> favoriteIds = new HashSet<>(favoriteAssistantIds(owner));
        return assistants.selectList(new LambdaQueryWrapper<KnowledgeAssistant>()
                        .eq(KnowledgeAssistant::getPublishedFlag, true)
                        .eq(KnowledgeAssistant::getEnabledFlag, true)
                        .like(StrUtil.isNotBlank(keyword), KnowledgeAssistant::getAssistantName, keyword)
                        .orderByDesc(KnowledgeAssistant::getPublishedTime)).stream()
                .filter(assistant -> !enabledBasesForAssistant(assistant.getAssistantId()).isEmpty())
                .map(assistant -> assistantStoreView(
                        assistant,
                        owner.equals(assistant.getOwnerId()),
                        favoriteIds.contains(assistant.getAssistantId())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBase saveBase(KnowledgeForms.BaseSave form) {
        Long owner = KnowledgeDocumentService.userId();
        KnowledgeBase base = form.getBaseId() == null ? new KnowledgeBase() : ownedBase(form.getBaseId());
        List<Long> ids = distinct(form.getDocumentIds());
        for (Long id : ids) {
            KnowledgeDocument document = documents.owned(id);
            documents.refresh(document);
            if (!"READY".equals(document.getStatus())) {
                throw new IllegalArgumentException("只能添加已就绪文档");
            }
        }
        base.setOwnerId(owner);
        base.setBaseName(form.getBaseName().trim());
        base.setDescription(form.getDescription());
        base.setEnabledFlag(!Boolean.FALSE.equals(form.getEnabledFlag()));
        base.setUpdateTime(LocalDateTime.now());
        if (base.getBaseId() == null) {
            base.setCreateTime(base.getUpdateTime());
            bases.insert(base);
        } else {
            bases.updateById(base);
        }
        baseDocuments.delete(new LambdaQueryWrapper<KnowledgeBaseDocument>()
                .eq(KnowledgeBaseDocument::getBaseId, base.getBaseId()));
        for (Long id : ids) {
            KnowledgeBaseDocument link = new KnowledgeBaseDocument();
            link.setBaseId(base.getBaseId());
            link.setDocumentId(id);
            baseDocuments.insert(link);
        }
        unpublishAssistantsWithoutBases(linkedAssistantIds(base.getBaseId()));
        return base;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBase(Long id) {
        ownedBase(id);
        if (assistantBases.selectCount(new LambdaQueryWrapper<KnowledgeAssistantBase>()
                .eq(KnowledgeAssistantBase::getBaseId, id)) > 0) {
            throw new IllegalArgumentException("知识库正被助手使用，请先解除关联");
        }
        bases.deleteById(id);
    }

    public List<Map<String, Object>> assistants() {
        return assistants.selectList(new LambdaQueryWrapper<KnowledgeAssistant>()
                        .eq(KnowledgeAssistant::getOwnerId, KnowledgeDocumentService.userId())
                        .orderByDesc(KnowledgeAssistant::getUpdateTime)).stream()
                .map(this::assistantView).toList();
    }

    /** Own assistants plus favorited assistants that are still published. */
    public List<Map<String, Object>> availableAssistants() {
        Long owner = KnowledgeDocumentService.userId();
        LinkedHashMap<Long, KnowledgeAssistant> result = assistants.selectList(
                        new LambdaQueryWrapper<KnowledgeAssistant>()
                                .eq(KnowledgeAssistant::getOwnerId, owner)
                                .orderByDesc(KnowledgeAssistant::getUpdateTime)).stream()
                .collect(Collectors.toMap(
                        KnowledgeAssistant::getAssistantId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        List<Long> favoriteIds = favoriteAssistantIds(owner);
        if (!favoriteIds.isEmpty()) {
            assistants.selectList(new LambdaQueryWrapper<KnowledgeAssistant>()
                            .in(KnowledgeAssistant::getAssistantId, favoriteIds)
                            .eq(KnowledgeAssistant::getPublishedFlag, true)
                            .eq(KnowledgeAssistant::getEnabledFlag, true)
                            .orderByDesc(KnowledgeAssistant::getPublishedTime))
                    .stream()
                    .filter(assistant -> !enabledBasesForAssistant(assistant.getAssistantId()).isEmpty())
                    .forEach(assistant -> result.putIfAbsent(assistant.getAssistantId(), assistant));
        }
        return result.values().stream().map(this::assistantView).toList();
    }

    public Map<String, Object> assistant(Long id) {
        return assistantView(accessibleAssistant(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeAssistant saveAssistant(KnowledgeForms.AssistantSave form) {
        AiModelEntity model = models.selectById(form.getModelId());
        if (model == null || !"CHAT".equals(model.getModelType()) || !Boolean.TRUE.equals(model.getEnabledFlag())) {
            throw new IllegalArgumentException("请选择已启用的对话模型");
        }
        List<Long> ids = distinct(form.getBaseIds());
        for (Long id : ids) {
            ownedBase(id);
        }
        KnowledgeAssistant assistant = form.getAssistantId() == null
                ? new KnowledgeAssistant()
                : ownedAssistant(form.getAssistantId());
        assistant.setOwnerId(KnowledgeDocumentService.userId());
        assistant.setAssistantName(form.getAssistantName().trim());
        assistant.setSystemPrompt(form.getSystemPrompt());
        assistant.setModelId(model.getModelId());
        assistant.setTopK(form.getTopK());
        assistant.setScoreThreshold(form.getScoreThreshold());
        assistant.setShowCitations(!Boolean.FALSE.equals(form.getShowCitations()));
        assistant.setMaxToolCalls(Objects.requireNonNullElse(form.getMaxToolCalls(), 3));
        assistant.setToolDebugFlag(Boolean.TRUE.equals(form.getToolDebugFlag()));
        assistant.setAllowActionToolFlag(Boolean.TRUE.equals(form.getAllowActionToolFlag()));
        assistant.setEnabledFlag(!Boolean.FALSE.equals(form.getEnabledFlag()));
        if (assistant.getPublishedFlag() == null) {
            assistant.setPublishedFlag(false);
        }
        if (!Boolean.TRUE.equals(assistant.getEnabledFlag())) {
            assistant.setPublishedFlag(false);
            assistant.setPublishedTime(null);
        }
        assistant.setUpdateTime(LocalDateTime.now());
        if (assistant.getAssistantId() == null) {
            assistant.setCreateTime(assistant.getUpdateTime());
            assistants.insert(assistant);
        } else {
            assistants.updateById(assistant);
        }

        assistantBases.delete(new LambdaQueryWrapper<KnowledgeAssistantBase>()
                .eq(KnowledgeAssistantBase::getAssistantId, assistant.getAssistantId()));
        for (Long id : ids) {
            KnowledgeAssistantBase link = new KnowledgeAssistantBase();
            link.setAssistantId(assistant.getAssistantId());
            link.setBaseId(id);
            assistantBases.insert(link);
        }
        if (Boolean.TRUE.equals(assistant.getPublishedFlag())
                && enabledBasesForAssistant(assistant.getAssistantId()).isEmpty()) {
            assistant.setPublishedFlag(false);
            assistant.setPublishedTime(null);
            assistants.updateById(assistant);
        }
        // 兼容未升级的旧客户端：只有显式提交 toolIds 时才重建工具关系，避免编辑助手时误清空。
        if (form.getToolIds() != null) {
            AiToolForms.AssistantBind toolBinding = new AiToolForms.AssistantBind();
            toolBinding.setAssistantId(assistant.getAssistantId());
            toolBinding.setToolIds(form.getToolIds());
            toolBinding.setMaxToolCalls(assistant.getMaxToolCalls());
            toolBinding.setToolDebugFlag(assistant.getToolDebugFlag());
            toolBinding.setAllowActionToolFlag(assistant.getAllowActionToolFlag());
            aiTools.bindAssistant(toolBinding);
        }
        return assistant;
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeAssistant publishAssistant(Long id, boolean published) {
        KnowledgeAssistant assistant = ownedAssistant(id);
        if (published) {
            if (!Boolean.TRUE.equals(assistant.getEnabledFlag())) {
                throw new IllegalArgumentException("请先启用智能助手再上架");
            }
            if (enabledBasesForAssistant(id).isEmpty()) {
                throw new IllegalArgumentException("请先关联至少一个已启用的知识库");
            }
            assistant.setPublishedFlag(true);
            assistant.setPublishedTime(LocalDateTime.now());
        } else {
            assistant.setPublishedFlag(false);
            assistant.setPublishedTime(null);
        }
        assistant.setUpdateTime(LocalDateTime.now());
        assistants.updateById(assistant);
        return assistant;
    }

    @Transactional(rollbackFor = Exception.class)
    public void favoriteAssistant(Long id) {
        KnowledgeAssistant assistant = publicAssistant(id);
        Long owner = KnowledgeDocumentService.userId();
        if (owner.equals(assistant.getOwnerId())) {
            throw new IllegalArgumentException("自己创建的助手无需收藏");
        }
        if (assistantFavorites.selectCount(new LambdaQueryWrapper<KnowledgeAssistantFavorite>()
                .eq(KnowledgeAssistantFavorite::getAssistantId, id)
                .eq(KnowledgeAssistantFavorite::getOwnerId, owner)) > 0) {
            return;
        }
        KnowledgeAssistantFavorite favorite = new KnowledgeAssistantFavorite();
        favorite.setAssistantId(id);
        favorite.setOwnerId(owner);
        favorite.setCreateTime(LocalDateTime.now());
        assistantFavorites.insert(favorite);
    }

    public void unfavoriteAssistant(Long id) {
        assistantFavorites.delete(new LambdaQueryWrapper<KnowledgeAssistantFavorite>()
                .eq(KnowledgeAssistantFavorite::getAssistantId, id)
                .eq(KnowledgeAssistantFavorite::getOwnerId, KnowledgeDocumentService.userId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAssistant(Long id) {
        ownedAssistant(id);
        assistants.deleteById(id);
    }

    public KnowledgeBase ownedBase(Long id) {
        KnowledgeBase base = bases.selectById(id);
        if (base == null || !KnowledgeDocumentService.userId().equals(base.getOwnerId())) {
            throw new IllegalArgumentException("知识库不存在或无权访问");
        }
        return base;
    }

    public KnowledgeAssistant ownedAssistant(Long id) {
        KnowledgeAssistant assistant = assistants.selectById(id);
        if (assistant == null || !KnowledgeDocumentService.userId().equals(assistant.getOwnerId())) {
            throw new IllegalArgumentException("助手不存在或无权访问");
        }
        return assistant;
    }

    /** Owners may always open their assistant; other users require a published assistant. */
    public KnowledgeAssistant accessibleAssistant(Long id) {
        KnowledgeAssistant assistant = assistants.selectById(id);
        if (assistant == null) {
            throw new IllegalArgumentException("助手不存在或无权访问");
        }
        if (KnowledgeDocumentService.userId().equals(assistant.getOwnerId())) {
            return assistant;
        }
        return publicAssistant(id);
    }

    /** Bases used internally by an accessible assistant. */
    public List<KnowledgeBase> accessibleBasesForAssistant(KnowledgeAssistant assistant) {
        if (!KnowledgeDocumentService.userId().equals(assistant.getOwnerId())) {
            publicAssistant(assistant.getAssistantId());
        }
        return enabledBasesForAssistant(assistant.getAssistantId());
    }

    public List<Long> documentIds(Long baseId) {
        return baseDocuments.selectList(new LambdaQueryWrapper<KnowledgeBaseDocument>()
                        .eq(KnowledgeBaseDocument::getBaseId, baseId)).stream()
                .map(KnowledgeBaseDocument::getDocumentId).toList();
    }

    public List<Long> assistantBaseIds(Long assistantId) {
        return assistantBases.selectList(new LambdaQueryWrapper<KnowledgeAssistantBase>()
                        .eq(KnowledgeAssistantBase::getAssistantId, assistantId)).stream()
                .map(KnowledgeAssistantBase::getBaseId).toList();
    }

    private Map<String, Object> baseView(KnowledgeBase base) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("base", base);
        view.put("documentIds", documentIds(base.getBaseId()));
        view.put("assistants", enabledAssistants(base.getBaseId()).stream()
                .map(this::assistantSummary).toList());
        view.put("owned", true);
        view.put("ownerName", ownerName(base.getOwnerId()));
        return view;
    }

    private Map<String, Object> assistantStoreView(
            KnowledgeAssistant assistant,
            boolean owned,
            boolean favorited) {
        Map<String, Object> view = assistantView(assistant);
        view.put("owned", owned);
        view.put("favorited", favorited);
        view.put("favoriteCount", assistantFavorites.selectCount(
                new LambdaQueryWrapper<KnowledgeAssistantFavorite>()
                        .eq(KnowledgeAssistantFavorite::getAssistantId, assistant.getAssistantId())));
        return view;
    }

    private Map<String, Object> assistantView(KnowledgeAssistant assistant) {
        Long owner = KnowledgeDocumentService.userId();
        List<KnowledgeBase> visibleBases = owner.equals(assistant.getOwnerId())
                ? linkedBases(assistant.getAssistantId())
                : enabledBasesForAssistant(assistant.getAssistantId());
        Set<Long> favoriteIds = new HashSet<>(favoriteAssistantIds(owner));
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("assistant", assistant);
        view.put("baseIds", visibleBases.stream().map(KnowledgeBase::getBaseId).toList());
        view.put("baseNames", visibleBases.stream().map(KnowledgeBase::getBaseName).toList());
        view.put("owned", owner.equals(assistant.getOwnerId()));
        view.put("favorited", favoriteIds.contains(assistant.getAssistantId()));
        view.put("ownerName", ownerName(assistant.getOwnerId()));
        AiModelEntity model = models.selectById(assistant.getModelId());
        view.put("modelName", model == null ? "未知模型" : model.getModelName());
        List<Map<String, Object>> linkedTools = aiTools.linkedToolViews(assistant.getAssistantId());
        view.put("tools", linkedTools);
        view.put("toolIds", owner.equals(assistant.getOwnerId())
                ? linkedTools.stream().map(item -> (Long) item.get("toolId")).toList()
                : List.of());
        return view;
    }

    private Map<String, Object> assistantSummary(KnowledgeAssistant assistant) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("assistantId", assistant.getAssistantId());
        summary.put("assistantName", assistant.getAssistantName());
        return summary;
    }

    private List<KnowledgeAssistant> enabledAssistants(Long baseId) {
        List<Long> ids = linkedAssistantIds(baseId);
        if (ids.isEmpty()) {
            return List.of();
        }
        return assistants.selectList(new LambdaQueryWrapper<KnowledgeAssistant>()
                .in(KnowledgeAssistant::getAssistantId, ids)
                .eq(KnowledgeAssistant::getEnabledFlag, true)
                .orderByDesc(KnowledgeAssistant::getUpdateTime));
    }

    private List<Long> linkedAssistantIds(Long baseId) {
        return assistantBases.selectList(new LambdaQueryWrapper<KnowledgeAssistantBase>()
                        .eq(KnowledgeAssistantBase::getBaseId, baseId)).stream()
                .map(KnowledgeAssistantBase::getAssistantId).distinct().toList();
    }

    private List<KnowledgeBase> linkedBases(Long assistantId) {
        List<Long> ids = assistantBaseIds(assistantId);
        if (ids.isEmpty()) {
            return List.of();
        }
        return bases.selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .in(KnowledgeBase::getBaseId, ids));
    }

    private List<KnowledgeBase> enabledBasesForAssistant(Long assistantId) {
        List<Long> ids = assistantBaseIds(assistantId);
        if (ids.isEmpty()) {
            return List.of();
        }
        return bases.selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .in(KnowledgeBase::getBaseId, ids)
                .eq(KnowledgeBase::getEnabledFlag, true));
    }

    private KnowledgeAssistant publicAssistant(Long id) {
        KnowledgeAssistant assistant = assistants.selectById(id);
        if (assistant == null
                || !Boolean.TRUE.equals(assistant.getEnabledFlag())
                || !Boolean.TRUE.equals(assistant.getPublishedFlag())
                || enabledBasesForAssistant(id).isEmpty()) {
            throw new IllegalArgumentException("智能助手已下架或不可用");
        }
        return assistant;
    }

    private List<Long> favoriteAssistantIds(Long ownerId) {
        return assistantFavorites.selectList(new LambdaQueryWrapper<KnowledgeAssistantFavorite>()
                        .eq(KnowledgeAssistantFavorite::getOwnerId, ownerId)
                        .orderByDesc(KnowledgeAssistantFavorite::getCreateTime)).stream()
                .map(KnowledgeAssistantFavorite::getAssistantId).toList();
    }

    private void unpublishAssistantsWithoutBases(Collection<Long> assistantIds) {
        for (Long assistantId : assistantIds) {
            KnowledgeAssistant assistant = assistants.selectById(assistantId);
            if (assistant != null
                    && Boolean.TRUE.equals(assistant.getPublishedFlag())
                    && enabledBasesForAssistant(assistantId).isEmpty()) {
                assistant.setPublishedFlag(false);
                assistant.setPublishedTime(null);
                assistant.setUpdateTime(LocalDateTime.now());
                assistants.updateById(assistant);
            }
        }
    }

    private String ownerName(Long ownerId) {
        EmployeeEntity employee = employees.selectById(ownerId);
        return employee == null
                ? "未知用户"
                : StrUtil.blankToDefault(employee.getActualName(), employee.getLoginName());
    }

    private List<Long> distinct(List<Long> ids) {
        if (ids == null || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("关联主键无效");
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }
}
