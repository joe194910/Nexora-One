package com.nexoraone.admin.module.business.knowledge.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexoraone.admin.module.business.ai.dao.*;
import com.nexoraone.admin.module.business.ai.domain.entity.*;
import com.nexoraone.admin.module.business.ai.service.AiDocumentWorkflowService;
import com.nexoraone.admin.module.business.knowledge.dao.KnowledgeMappers.*;
import com.nexoraone.admin.module.business.knowledge.domain.*;
import com.nexoraone.admin.util.AdminRequestUtil;
import com.nexoraone.base.common.domain.ResponseDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

/** 用户文档接入层；MinIO 保存源文件，原解析服务负责真实向量化。 */
@Service
public class KnowledgeDocumentService {
    @Resource private DocumentDao documents;
    @Resource private ContextDao contexts;
    @Resource private AiKnowledgeBaseDao ingestBases;
    @Resource private AiDocumentParseConfigDao plans;
    @Resource private AiModelDao models;
    @Resource private AiVectorDatabaseDao vectors;
    @Resource private AiParseTaskDao tasks;
    @Resource private AiParseStepDao steps;
    @Resource private AiDocumentWorkflowService workflow;
    @Resource private KnowledgeMinioStorage storage;
    @Resource private KnowledgeVectorSearch vectorSearch;
    @Resource private BaseDocumentDao links;

    /** 返回可供用户选择的真实启用配置，不开放管理员配置接口。 */
    public Map<String, Object> options() {
        return Map.of(
                "plans", plans.selectList(new LambdaQueryWrapper<AiDocumentParseConfigEntity>()
                        .eq(AiDocumentParseConfigEntity::getEnabledFlag, true).orderByDesc(AiDocumentParseConfigEntity::getDefaultFlag)),
                "chatModels", models.selectList(new LambdaQueryWrapper<AiModelEntity>()
                        .eq(AiModelEntity::getModelType, "CHAT").eq(AiModelEntity::getEnabledFlag, true)
                        .orderByDesc(AiModelEntity::getDefaultFlag)));
    }

    /** 当前用户文档；只有原任务成功且全部切片入库时才标记已就绪。 */
    public List<KnowledgeDocument> list(String keyword, String status) {
        List<KnowledgeDocument> result = documents.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getOwnerId, userId())
                .like(StrUtil.isNotBlank(keyword), KnowledgeDocument::getFileName, keyword)
                .orderByDesc(KnowledgeDocument::getCreateTime));
        result.forEach(this::refresh);
        return result.stream().filter(doc -> StrUtil.isBlank(status) || status.equals(doc.getStatus())).toList();
    }

    /** 校验方案格式与默认模型、实例，按用户+内容+配置严格去重。 */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocument upload(MultipartFile file, Long planId) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > 50L * 1024 * 1024)
            throw new IllegalArgumentException("文件不能为空且大小不能超过 50 MB");
        String original = StrUtil.blankToDefault(file.getOriginalFilename(), "document").replace('\\', '/');
        String name = StrUtil.maxLength(original.substring(original.lastIndexOf('/') + 1), 255);
        String type = FileUtil.extName(name).toLowerCase(Locale.ROOT);
        AiDocumentParseConfigEntity plan = planId == null
                ? plans.selectOne(new LambdaQueryWrapper<AiDocumentParseConfigEntity>()
                    .eq(AiDocumentParseConfigEntity::getDefaultFlag, true).eq(AiDocumentParseConfigEntity::getEnabledFlag, true).last("LIMIT 1"))
                : plans.selectById(planId);
        if (plan == null || !Boolean.TRUE.equals(plan.getEnabledFlag())
                || !Arrays.asList(plan.getSupportedFormats().split(",")).contains(type))
            throw new IllegalArgumentException("请选择支持该文件格式的已启用解析方案");
        AiModelEntity model = models.selectOne(new LambdaQueryWrapper<AiModelEntity>()
                .eq(AiModelEntity::getModelType, "EMBEDDING").eq(AiModelEntity::getDefaultFlag, true)
                .eq(AiModelEntity::getEnabledFlag, true).last("LIMIT 1"));
        AiVectorDatabaseEntity vector = vectors.selectOne(new LambdaQueryWrapper<AiVectorDatabaseEntity>()
                .eq(AiVectorDatabaseEntity::getDatabaseType, "QDRANT").eq(AiVectorDatabaseEntity::getDefaultFlag, true)
                .eq(AiVectorDatabaseEntity::getEnabledFlag, true).last("LIMIT 1"));
        if (model == null || vector == null) throw new IllegalStateException("请先配置默认向量模型和默认 Qdrant 实例");
        Long owner = userId();
        byte[] bytes = file.getBytes();
        String hash = SecureUtil.sha256(new ByteArrayInputStream(bytes));
        KnowledgeDocument existing = documents.selectOne(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getOwnerId, owner).eq(KnowledgeDocument::getFileHash, hash)
                .eq(KnowledgeDocument::getParsePlanId, plan.getConfigId())
                .eq(KnowledgeDocument::getEmbeddingModelId, model.getModelId())
                .eq(KnowledgeDocument::getVectorDatabaseId, vector.getVectorDatabaseId()).last("LIMIT 1"));
        if (existing != null) { refresh(existing); return existing; }
        KnowledgeIngestContext context = context(owner, plan, model, vector);
        String key = "users/" + owner + "/" + UUID.randomUUID() + "." + type;
        KnowledgeDocument document = new KnowledgeDocument();
        document.setOwnerId(owner);
        document.setFileName(name);
        document.setFileType(type);
        document.setFileSize(file.getSize());
        document.setFileHash(hash);
        document.setObjectKey(key);
        document.setParsePlanId(plan.getConfigId());
        document.setEmbeddingModelId(model.getModelId());
        document.setVectorDatabaseId(vector.getVectorDatabaseId());
        document.setIngestBaseId(context.getIngestBaseId());
        document.setStatus("PROCESSING");
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(document.getCreateTime());
        // 唯一版本先入库，以免并发重复上传时生成两个解析任务。
        documents.insert(document);
        boolean stored = false;
        try {
            storage.put(key, bytes, file.getContentType());
            stored = true;
            ResponseDTO<AiParseTaskEntity> queued = workflow.upload(context.getIngestBaseId(), file, true);
            if (!Boolean.TRUE.equals(queued.getOk())) throw new IllegalStateException(queued.getMsg());
            document.setTaskId(queued.getData().getTaskId());
            documents.updateById(document);
            return document;
        } catch (RuntimeException exception) {
            if (stored) storage.delete(key);
            throw exception;
        }
    }

    /** 读取源文件时首先核查所有权。 */
    public byte[] download(Long id) {
        return storage.get(owned(id).getObjectKey());
    }

    /** 查看文档关联的实际任务进度和每一步结果。 */
    public Map<String, Object> detail(Long id) {
        KnowledgeDocument document = owned(id);
        refresh(document);
        ResponseDTO<Map<String, Object>> result = workflow.taskDetail(document.getTaskId());
        if (!Boolean.TRUE.equals(result.getOk())) throw new IllegalStateException(result.getMsg());
        return Map.of("document", document, "task", result.getData());
    }

    /** 仅查看本人已入库文档的真实向量切片，不暴露向量记录给知识库选择器。 */
    public Map<String, Object> chunks(Long id, String offset) {
        KnowledgeDocument document = owned(id);
        refresh(document);
        if (!"READY".equals(document.getStatus()))
            throw new IllegalArgumentException("文档尚未完成向量化");
        return vectorSearch.chunks(document, offset);
    }

    /** 请求取消本人尚未结束的解析任务；已写入的点由底层任务负责清理。 */
    public void cancel(Long id) {
        KnowledgeDocument document = owned(id);
        AiParseTaskEntity task = tasks.selectById(document.getTaskId());
        if (task == null || !List.of("QUEUED", "RUNNING").contains(task.getStatus()))
            throw new IllegalArgumentException("该任务已经结束，无法取消");
        ResponseDTO<String> result = workflow.cancel(task.getTaskId());
        if (!Boolean.TRUE.equals(result.getOk())) throw new IllegalStateException(result.getMsg());
        refresh(document);
    }

    /** 失败任务从 MinIO 恢复工作文件后再调用现有重试逻辑。 */
    public void retry(Long id) {
        KnowledgeDocument document = owned(id);
        refresh(document);
        if (!"FAILED".equals(document.getStatus())) throw new IllegalArgumentException("只有失败文档可以重试");
        AiParseTaskEntity task = tasks.selectById(document.getTaskId());
        if (task == null) throw new IllegalStateException("原解析任务已丢失");
        try {
            java.nio.file.Path workFile = workflow.taskFile(task.getTaskId());
            if (!Files.exists(workFile)) {
                Files.createDirectories(workFile.getParent());
                Files.write(workFile, storage.get(document.getObjectKey()));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("从 MinIO 恢复解析文件失败", exception);
        }
        ResponseDTO<String> result = workflow.retry(task.getTaskId());
        if (!Boolean.TRUE.equals(result.getOk())) throw new IllegalStateException(result.getMsg());
        document.setStatus("PROCESSING");
        document.setUpdateTime(LocalDateTime.now());
        documents.updateById(document);
    }

    /** 未被任何知识库引用时才允许删除；运行中的任务先取消。 */
    public void delete(Long id) {
        KnowledgeDocument document = owned(id);
        if (links.selectCount(new LambdaQueryWrapper<KnowledgeBaseDocument>()
                .eq(KnowledgeBaseDocument::getDocumentId, id)) > 0)
            throw new IllegalArgumentException("文档已关联知识库，请先解除关联");
        AiParseTaskEntity task = tasks.selectById(document.getTaskId());
        if (task != null && List.of("QUEUED", "RUNNING").contains(task.getStatus()))
            throw new IllegalArgumentException("任务正在处理，请先取消并等待后台处理停止后再删除");
        if (task != null && "CANCELLED".equals(task.getStatus()) && task.getStartTime() != null
                && steps.selectCount(new LambdaQueryWrapper<AiParseStepEntity>()
                    .eq(AiParseStepEntity::getTaskId, task.getTaskId())
                    .eq(AiParseStepEntity::getStatus, "CANCELLED")) == 0)
            throw new IllegalArgumentException("后台正在清理取消的任务，请稍后再删除");
        if (task != null) vectorSearch.delete(document);
        storage.delete(document.getObjectKey());
        documents.deleteById(id);
        // 保留任务审计记录，已完成任务的实际向量点已按任务主键删除。
    }

    /** 所有操作均用登录态核查文档归属。 */
    public KnowledgeDocument owned(Long id) {
        KnowledgeDocument document = documents.selectById(id);
        if (document == null || !userId().equals(document.getOwnerId()))
            throw new IllegalArgumentException("文档不存在或无权访问");
        return document;
    }

    /** 供已授权知识库检索内部调用，只接受知识库创建人的文档。 */
    public KnowledgeDocument accessible(Long id, Long expectedOwnerId) {
        KnowledgeDocument document = documents.selectById(id);
        if (document == null || !expectedOwnerId.equals(document.getOwnerId()))
            throw new IllegalArgumentException("知识库文档不存在或归属异常");
        return document;
    }

    /** 将底层任务的最终结果映射到用户业务文档。 */
    public void refresh(KnowledgeDocument document) {
        AiParseTaskEntity task = document.getTaskId() == null ? null : tasks.selectById(document.getTaskId());
        if (task == null) return;
        document.setChunkCount(task.getChunkCount());
        document.setIndexedCount(task.getIndexedCount());
        document.setCurrentStage(task.getCurrentStage());
        document.setErrorMessage(task.getErrorMessage());
        String status = "SUCCESS".equals(task.getStatus()) && task.getChunkCount() != null
                && task.getChunkCount() > 0 && task.getChunkCount().equals(task.getIndexedCount()) ? "READY"
                : List.of("FAILED", "CANCELLED").contains(task.getStatus()) ? "FAILED" : "PROCESSING";
        if (!status.equals(document.getStatus())) {
            document.setStatus(status);
            document.setUpdateTime(LocalDateTime.now());
            documents.updateById(document);
        }
    }

    /** 获取或创建入库配置，旧解析任务仍读取旧表，但它只是内部工作上下文。 */
    private KnowledgeIngestContext context(Long owner, AiDocumentParseConfigEntity plan,
                                            AiModelEntity model, AiVectorDatabaseEntity vector) {
        LambdaQueryWrapper<KnowledgeIngestContext> query = new LambdaQueryWrapper<KnowledgeIngestContext>()
                .eq(KnowledgeIngestContext::getOwnerId, owner)
                .eq(KnowledgeIngestContext::getParsePlanId, plan.getConfigId())
                .eq(KnowledgeIngestContext::getEmbeddingModelId, model.getModelId())
                .eq(KnowledgeIngestContext::getVectorDatabaseId, vector.getVectorDatabaseId());
        KnowledgeIngestContext existing = contexts.selectOne(query);
        if (existing != null) return existing;
        AiKnowledgeBaseEntity internal = new AiKnowledgeBaseEntity();
        internal.setBaseName("_kb_ingest_" + owner + "_" + UUID.randomUUID());
        internal.setDescription("用户文档入库上下文；不是用户知识库");
        internal.setParsePlanId(plan.getConfigId());
        internal.setEmbeddingModelId(model.getModelId());
        internal.setVectorDatabaseId(vector.getVectorDatabaseId());
        internal.setCreateTime(LocalDateTime.now());
        internal.setUpdateTime(internal.getCreateTime());
        ingestBases.insert(internal);
        internal.setCollectionName(vector.getCollectionPrefix() + "user_docs_" + internal.getKnowledgeBaseId());
        ingestBases.updateById(internal);
        KnowledgeIngestContext created = new KnowledgeIngestContext();
        created.setOwnerId(owner);
        created.setParsePlanId(plan.getConfigId());
        created.setEmbeddingModelId(model.getModelId());
        created.setVectorDatabaseId(vector.getVectorDatabaseId());
        created.setIngestBaseId(internal.getKnowledgeBaseId());
        contexts.insert(created);
        return created;
    }

    /** 返回当前登录用户主键；匿名请求不能被当作系统任务执行。 */
    public static Long userId() {
        Long id = AdminRequestUtil.getRequestUserId();
        if (id == null) throw new IllegalStateException("请先登录");
        return id;
    }
}
