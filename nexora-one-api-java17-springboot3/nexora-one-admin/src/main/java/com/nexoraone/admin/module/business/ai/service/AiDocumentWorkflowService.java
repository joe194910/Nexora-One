package com.nexoraone.admin.module.business.ai.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexoraone.admin.module.business.ai.dao.*;
import com.nexoraone.admin.module.business.ai.domain.entity.*;
import com.nexoraone.admin.module.business.ai.domain.form.AiDocumentForm;
import com.nexoraone.admin.util.AdminRequestUtil;
import com.nexoraone.base.common.domain.PageResult;
import com.nexoraone.base.common.domain.ResponseDTO;
import com.nexoraone.base.common.util.SmartPageUtil;
import com.nexoraone.base.module.support.apiencrypt.service.ApiEncryptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** 文档解析方案、解析服务、知识库与持久化任务的统一业务服务。 */
@Slf4j
@Service
public class AiDocumentWorkflowService {
    private static final String MASK = "******";
    private static final List<String> FORMATS = List.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "png", "jpg", "jpeg");
    private static final List<String> STAGES = List.of("UPLOAD", "PARSE", "CHUNK", "INDEX");
    @Resource private AiDocumentParseConfigDao planDao;
    @Resource private AiParseServiceDao serviceDao;
    @Resource private AiKnowledgeBaseDao baseDao;
    @Resource private AiParseTaskDao taskDao;
    @Resource private AiParseStepDao stepDao;
    @Resource private AiModelDao modelDao;
    @Resource private AiModelServiceDao modelServiceDao;
    @Resource private AiVectorDatabaseDao vectorDao;
    @Resource private ApiEncryptService encryptService;
    @Value("${nexora.ai.document-dir:./data/ai-documents}")
    private String documentDir;

    /** 查询方案，返回真实关联知识库数量。 */
    public ResponseDTO<List<Map<String, Object>>> plans() {
        return ResponseDTO.ok(planDao.selectList(new LambdaQueryWrapper<AiDocumentParseConfigEntity>()
                        .orderByDesc(AiDocumentParseConfigEntity::getDefaultFlag)
                        .orderByDesc(AiDocumentParseConfigEntity::getUpdateTime)).stream().map(plan -> {
            Map<String, Object> item = BeanUtil.beanToMap(plan);
            item.put("baseCount", baseDao.selectCount(new LambdaQueryWrapper<AiKnowledgeBaseEntity>()
                    .eq(AiKnowledgeBaseEntity::getParsePlanId, plan.getConfigId())));
            return item;
        }).toList());
    }

    /** 保存方案；默认方案必须启用，服务能力必须已配置。 */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> savePlan(AiDocumentForm.PlanSave form) {
        if (form.getChunkOverlap() >= form.getChunkSize() || form.getMinChunkSize() > form.getChunkSize()) {
            return ResponseDTO.userErrorParam("切片重叠与最小长度必须小于或等于切片长度");
        }
        if (!"TIKA".equals(form.getParserType()) || !List.of("RECURSIVE", "FIXED").contains(form.getChunkMethod())
                || !List.of("SKIP", "REPLACE").contains(form.getDuplicateStrategy())
                || form.getSupportedFormats().stream().anyMatch(format -> !FORMATS.contains(format.toLowerCase(Locale.ROOT)))) {
            return ResponseDTO.userErrorParam("包含不支持的解析器、切片方式或文件格式");
        }
        boolean hasImage = form.getSupportedFormats().stream()
                .anyMatch(format -> List.of("png", "jpg", "jpeg").contains(format.toLowerCase(Locale.ROOT)));
        if (hasImage && !Boolean.TRUE.equals(form.getOcrEnabled()) && !Boolean.TRUE.equals(form.getExtractImage()))
            return ResponseDTO.userErrorParam("支持图片格式必须配置并启用 OCR 或图片理解服务");
        String error = validateService(form.getParserServiceId(), "TIKA", true);
        if (error == null && Boolean.TRUE.equals(form.getOcrEnabled())) error = validateService(form.getOcrServiceId(), "OCR", true);
        if (error == null && Boolean.TRUE.equals(form.getExtractTable()) && form.getTableServiceId() != null)
            error = validateService(form.getTableServiceId(), "TABLE", true);
        if (error == null && Boolean.TRUE.equals(form.getExtractImage()))
            error = validateService(form.getImageServiceId(), "IMAGE", true);
        if (error != null) return ResponseDTO.userErrorParam(error);
        if (Boolean.TRUE.equals(form.getDefaultFlag()) && Boolean.FALSE.equals(form.getEnabledFlag()))
            return ResponseDTO.userErrorParam("默认方案必须启用");
        AiDocumentParseConfigEntity plan = form.getConfigId() == null ? new AiDocumentParseConfigEntity() : planDao.selectById(form.getConfigId());
        if (plan == null) return ResponseDTO.userErrorParam("解析方案不存在");
        if (Boolean.FALSE.equals(form.getEnabledFlag()) && baseDao.selectCount(new LambdaQueryWrapper<AiKnowledgeBaseEntity>()
                .eq(AiKnowledgeBaseEntity::getParsePlanId, form.getConfigId())) > 0) {
            return ResponseDTO.userErrorParam("已有知识库使用该方案，请先更换知识库解析方案");
        }
        BeanUtil.copyProperties(form, plan, "supportedFormats", "ocrService");
        plan.setSupportedFormats(StrUtil.join(",", form.getSupportedFormats()).toLowerCase(Locale.ROOT));
        plan.setOcrService(null);
        plan.setDefaultFlag(Boolean.TRUE.equals(form.getDefaultFlag()));
        plan.setEnabledFlag(!Boolean.FALSE.equals(form.getEnabledFlag()));
        plan.setUpdateUserId(AdminRequestUtil.getRequestUserId());
        plan.setUpdateTime(LocalDateTime.now());
        if (plan.getDefaultFlag()) planDao.update(null, new LambdaUpdateWrapper<AiDocumentParseConfigEntity>()
                .set(AiDocumentParseConfigEntity::getDefaultFlag, false));
        if (form.getConfigId() == null) planDao.insert(plan); else planDao.updateById(plan);
        return ResponseDTO.okMsg("解析方案保存成功");
    }

    /** 复制方案，复制配置但不继承默认状态。 */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> copyPlan(Long id) {
        AiDocumentParseConfigEntity source = planDao.selectById(id);
        if (source == null) return ResponseDTO.userErrorParam("解析方案不存在");
        AiDocumentParseConfigEntity copy = new AiDocumentParseConfigEntity();
        BeanUtil.copyProperties(source, copy, "configId", "planName");
        copy.setPlanName(StrUtil.maxLength(source.getPlanName(), 80) + " - 副本 " + System.currentTimeMillis() % 100000);
        copy.setDefaultFlag(false);
        copy.setEnabledFlag(false);
        copy.setUpdateTime(LocalDateTime.now());
        planDao.insert(copy);
        return ResponseDTO.okMsg("解析方案已复制");
    }

    /** 删除未被知识库引用的非默认方案。 */
    public ResponseDTO<String> deletePlan(Long id) {
        AiDocumentParseConfigEntity plan = planDao.selectById(id);
        if (plan == null) return ResponseDTO.userErrorParam("解析方案不存在");
        if (Boolean.TRUE.equals(plan.getDefaultFlag()) || baseDao.selectCount(new LambdaQueryWrapper<AiKnowledgeBaseEntity>()
                .eq(AiKnowledgeBaseEntity::getParsePlanId, id)) > 0) return ResponseDTO.userErrorParam("默认或已关联知识库的方案不可删除");
        planDao.deleteById(id);
        return ResponseDTO.okMsg("解析方案已删除");
    }

    /** 查询服务配置；密钥仅返回掩码。 */
    public ResponseDTO<List<AiParseServiceEntity>> services() {
        List<AiParseServiceEntity> list = serviceDao.selectList(new LambdaQueryWrapper<AiParseServiceEntity>()
                .orderByAsc(AiParseServiceEntity::getServiceType).orderByDesc(AiParseServiceEntity::getDefaultFlag));
        list.forEach(item -> item.setApiKeyCipher(StrUtil.isBlank(item.getApiKeyCipher()) ? "" : MASK));
        return ResponseDTO.ok(list);
    }

    /** 保存真实受支持的解析服务，禁止无实现的内置能力。 */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> saveService(AiDocumentForm.ServiceSave form) {
        if (!List.of("TIKA", "OCR", "TABLE", "IMAGE").contains(form.getServiceType())
                || !List.of("BUILTIN", "HTTP").contains(form.getDeploymentType())
                || form.getSupportedFormats() == null || form.getSupportedFormats().stream()
                .anyMatch(format -> !FORMATS.contains(format.toLowerCase(Locale.ROOT))))
            return ResponseDTO.userErrorParam("服务类型、部署方式或支持格式无效");
        if ("BUILTIN".equals(form.getDeploymentType()) && !List.of("TIKA", "TABLE").contains(form.getServiceType()))
            return ResponseDTO.userErrorParam("OCR 和图片理解须配置真实 HTTP API；当前无对应内置实现");
        if ("HTTP".equals(form.getDeploymentType()) && !validUrl(form.getEndpoint()))
            return ResponseDTO.userErrorParam("HTTP 服务地址必须是有效的 HTTP(S) URL");
        if (Boolean.TRUE.equals(form.getDefaultFlag()) && Boolean.FALSE.equals(form.getEnabledFlag()))
            return ResponseDTO.userErrorParam("默认服务必须启用");
        AiParseServiceEntity entity = form.getParseServiceId() == null ? new AiParseServiceEntity() : serviceDao.selectById(form.getParseServiceId());
        if (entity == null) return ResponseDTO.userErrorParam("解析服务不存在");
        if (form.getParseServiceId() != null && !entity.getServiceType().equals(form.getServiceType()))
            return ResponseDTO.userErrorParam("服务类型不能修改，请新增对应类型的服务");
        BeanUtil.copyProperties(form, entity, "apiKey", "supportedFormats");
        entity.setSupportedFormats(StrUtil.join(",", form.getSupportedFormats()).toLowerCase(Locale.ROOT));
        entity.setEndpoint("HTTP".equals(form.getDeploymentType()) ? StrUtil.removeSuffix(form.getEndpoint().trim(), "/") : null);
        entity.setImplementation("BUILTIN".equals(form.getDeploymentType()) ? "APACHE_TIKA" : null);
        if (StrUtil.isNotBlank(form.getApiKey()) && !MASK.equals(form.getApiKey()))
            entity.setApiKeyCipher(encryptService.encrypt(form.getApiKey()));
        if ("BUILTIN".equals(form.getDeploymentType())) entity.setApiKeyCipher(null);
        entity.setEnabledFlag(!Boolean.FALSE.equals(form.getEnabledFlag()));
        entity.setDefaultFlag(Boolean.TRUE.equals(form.getDefaultFlag()));
        entity.setConnectionStatus("UNTESTED");
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getDefaultFlag()) serviceDao.update(null, new LambdaUpdateWrapper<AiParseServiceEntity>()
                .eq(AiParseServiceEntity::getServiceType, form.getServiceType()).set(AiParseServiceEntity::getDefaultFlag, false));
        if (form.getParseServiceId() == null) {
            entity.setCreateTime(LocalDateTime.now());
            serviceDao.insert(entity);
        } else serviceDao.updateById(entity);
        return ResponseDTO.okMsg("解析服务保存成功");
    }

    /** 对内置实现或 HTTP /health 接口进行真实健康检查。 */
    public ResponseDTO<Map<String, Object>> testService(Long id) {
        AiParseServiceEntity entity = serviceDao.selectById(id);
        if (entity == null) return ResponseDTO.userErrorParam("解析服务不存在");
        long begin = System.currentTimeMillis();
        try {
            if ("BUILTIN".equals(entity.getDeploymentType())) new Tika().detect("test.txt");
            else {
                try (HttpResponse response = authorized(HttpRequest.get(entity.getEndpoint() + "/health"), entity).execute()) {
                    ensureOk(response);
                }
            }
            entity.setConnectionStatus("CONNECTED");
            entity.setLastError(null);
        } catch (Exception exception) {
            entity.setConnectionStatus("FAILED");
            entity.setLastError(StrUtil.maxLength(exception.getMessage(), 1000));
        }
        entity.setLastTestTime(LocalDateTime.now());
        serviceDao.updateById(entity);
        return ResponseDTO.ok(Map.of("status", entity.getConnectionStatus(), "durationMs", System.currentTimeMillis() - begin,
                "message", StrUtil.blankToDefault(entity.getLastError(), "能力已就绪")));
    }

    /** 删除未被任何方案引用的服务。 */
    public ResponseDTO<String> deleteService(Long id) {
        AiParseServiceEntity entity = serviceDao.selectById(id);
        if (entity == null) return ResponseDTO.userErrorParam("解析服务不存在");
        if (Boolean.TRUE.equals(entity.getDefaultFlag())) return ResponseDTO.userErrorParam("请先更换默认服务");
        for (AiDocumentParseConfigEntity plan : planDao.selectList(null))
            if (Arrays.asList(plan.getParserServiceId(), plan.getOcrServiceId(), plan.getTableServiceId(), plan.getImageServiceId())
                    .stream().anyMatch(id::equals)) return ResponseDTO.userErrorParam("服务已被解析方案引用");
        serviceDao.deleteById(id);
        return ResponseDTO.okMsg("解析服务已删除");
    }

    /** 查询知识库。 */
    public ResponseDTO<List<AiKnowledgeBaseEntity>> knowledgeBases() {
        return ResponseDTO.ok(baseDao.selectList(new LambdaQueryWrapper<AiKnowledgeBaseEntity>()
                .orderByDesc(AiKnowledgeBaseEntity::getUpdateTime)));
    }

    /** 创建或编辑知识库时校验方案、模型、存储的真实可用性。 */
    public ResponseDTO<String> saveKnowledgeBase(AiDocumentForm.KnowledgeSave form) {
        AiDocumentParseConfigEntity plan = planDao.selectById(form.getParsePlanId());
        AiModelEntity model = modelDao.selectById(form.getEmbeddingModelId());
        AiVectorDatabaseEntity database = vectorDao.selectById(form.getVectorDatabaseId());
        if (plan == null || !Boolean.TRUE.equals(plan.getEnabledFlag()))
            return ResponseDTO.userErrorParam("请选择已启用的解析方案");
        if (model == null || !"EMBEDDING".equals(model.getModelType()) || !Boolean.TRUE.equals(model.getEnabledFlag()))
            return ResponseDTO.userErrorParam("请选择已启用的向量模型");
        if (database == null || !"QDRANT".equalsIgnoreCase(database.getDatabaseType()) || !Boolean.TRUE.equals(database.getEnabledFlag()))
            return ResponseDTO.userErrorParam("请选择已启用的 Qdrant 向量数据库");
        AiKnowledgeBaseEntity entity = form.getKnowledgeBaseId() == null ? new AiKnowledgeBaseEntity() : baseDao.selectById(form.getKnowledgeBaseId());
        if (entity == null) return ResponseDTO.userErrorParam("知识库不存在");
        if (form.getKnowledgeBaseId() != null && !entity.getVectorDatabaseId().equals(form.getVectorDatabaseId()))
            return ResponseDTO.userErrorParam("已有知识库不可变更向量实例，请新建知识库");
        BeanUtil.copyProperties(form, entity);
        entity.setUpdateTime(LocalDateTime.now());
        if (form.getKnowledgeBaseId() == null) {
            entity.setCreateTime(LocalDateTime.now());
            baseDao.insert(entity);
            entity.setCollectionName(database.getCollectionPrefix() + entity.getKnowledgeBaseId());
            baseDao.updateById(entity);
        } else baseDao.updateById(entity);
        return ResponseDTO.okMsg("知识库保存成功");
    }

    /** 文档上传后安全持久化文件，并自动创建四阶段解析任务。 */
    public ResponseDTO<AiParseTaskEntity> upload(Long baseId, MultipartFile file) {
        return upload(baseId, file, false);
    }

    /** 用户文档删除后重建时允许保留旧任务审计记录，不沿用已清理的历史向量。 */
    public ResponseDTO<AiParseTaskEntity> upload(Long baseId, MultipartFile file, boolean rebuildDeletedDocument) {
        AiKnowledgeBaseEntity base = baseDao.selectById(baseId);
        if (base == null) return ResponseDTO.userErrorParam("知识库不存在");
        AiDocumentParseConfigEntity plan = planDao.selectById(base.getParsePlanId());
        if (plan == null || !Boolean.TRUE.equals(plan.getEnabledFlag())) return ResponseDTO.userErrorParam("知识库解析方案未启用");
        if (file == null || file.isEmpty() || file.getSize() > 50L * 1024 * 1024) return ResponseDTO.userErrorParam("文件为空或超过 50 MB");
        String filename = StrUtil.blankToDefault(file.getOriginalFilename(), "document");
        String extension = FileUtil.extName(filename).toLowerCase(Locale.ROOT);
        if (!Arrays.asList(plan.getSupportedFormats().split(",")).contains(extension))
            return ResponseDTO.userErrorParam("解析方案不支持 ." + extension + " 文件");
        if (List.of("png", "jpg", "jpeg").contains(extension)
                && !(Boolean.TRUE.equals(plan.getOcrEnabled()) && plan.getOcrServiceId() != null)
                && !(Boolean.TRUE.equals(plan.getExtractImage()) && plan.getImageServiceId() != null))
            return ResponseDTO.userErrorParam("该历史方案未配置图片解析服务，请先设置 OCR 或图片理解服务");
        try {
            long uploadStart = System.currentTimeMillis();
            byte[] bytes = file.getBytes();
            String hash = SecureUtil.sha256(new java.io.ByteArrayInputStream(bytes));
            if (taskDao.selectCount(new LambdaQueryWrapper<AiParseTaskEntity>()
                    .eq(AiParseTaskEntity::getKnowledgeBaseId, baseId).eq(AiParseTaskEntity::getFileHash, hash)
                    .in(AiParseTaskEntity::getStatus, "QUEUED", "RUNNING")) > 0)
                return ResponseDTO.userErrorParam("相同文件正在处理，请等待任务完成");
            if (!rebuildDeletedDocument && "SKIP".equals(plan.getDuplicateStrategy()) && taskDao.selectCount(new LambdaQueryWrapper<AiParseTaskEntity>()
                    .eq(AiParseTaskEntity::getKnowledgeBaseId, baseId).eq(AiParseTaskEntity::getFileHash, hash)
                    .eq(AiParseTaskEntity::getStatus, "SUCCESS")) > 0)
                return ResponseDTO.userErrorParam("相同文件已成功入库");
            Path root = Path.of(documentDir).toAbsolutePath().normalize();
            Files.createDirectories(root);
            Path path = root.resolve(UUID.randomUUID() + "." + extension);
            Files.write(path, bytes);
            AiParseTaskEntity task = new AiParseTaskEntity();
            task.setKnowledgeBaseId(baseId);
            task.setParsePlanId(plan.getConfigId());
            task.setFileName(StrUtil.maxLength(Path.of(filename).getFileName().toString(), 255));
            task.setFilePath(path.toString());
            task.setFileHash(hash);
            task.setFileSize(file.getSize());
            task.setStatus("QUEUED");
            task.setCurrentStage("UPLOAD");
            task.setChunkCount(0);
            task.setIndexedCount(0);
            task.setCreateTime(LocalDateTime.now());
            task.setCreateUserId(AdminRequestUtil.getRequestUserId());
            try {
                taskDao.insert(task);
                recordStep(task.getTaskId(), "UPLOAD", "SUCCESS", "文件已持久化，SHA-256: " + hash, null,
                        System.currentTimeMillis() - uploadStart);
                return ResponseDTO.ok(task);
            } catch (Exception exception) {
                Files.deleteIfExists(path);
                throw exception;
            }
        } catch (Exception exception) {
            log.warn("文档上传失败", exception);
            return ResponseDTO.userErrorParam("上传失败：" + StrUtil.maxLength(exception.getMessage(), 300));
        }
    }

    /** 查询全部文档任务，支持业务筛选。 */
    public ResponseDTO<PageResult<AiParseTaskEntity>> tasks(AiDocumentForm.TaskQuery form) {
        LambdaQueryWrapper<AiParseTaskEntity> query = new LambdaQueryWrapper<AiParseTaskEntity>()
                .eq(form.getKnowledgeBaseId() != null, AiParseTaskEntity::getKnowledgeBaseId, form.getKnowledgeBaseId())
                .eq(form.getParsePlanId() != null, AiParseTaskEntity::getParsePlanId, form.getParsePlanId())
                .eq(StrUtil.isNotBlank(form.getStatus()), AiParseTaskEntity::getStatus, form.getStatus())
                .eq(StrUtil.isNotBlank(form.getCurrentStage()), AiParseTaskEntity::getCurrentStage, form.getCurrentStage())
                .orderByDesc(AiParseTaskEntity::getCreateTime);
        if (StrUtil.isNotBlank(form.getKeyword())) query.and(w -> w.like(AiParseTaskEntity::getFileName, form.getKeyword())
                .or().eq(StrUtil.isNumeric(form.getKeyword()), AiParseTaskEntity::getTaskId,
                        StrUtil.isNumeric(form.getKeyword()) ? Long.valueOf(form.getKeyword()) : -1L));
        Page<AiParseTaskEntity> page = taskDao.selectPage(new Page<>(form.getPageNum(), form.getPageSize()), query);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, page.getRecords()));
    }

    /** 查询任务及四阶段完整结果。 */
    public ResponseDTO<Map<String, Object>> taskDetail(Long id) {
        AiParseTaskEntity task = taskDao.selectById(id);
        if (task == null) return ResponseDTO.userErrorParam("任务不存在");
        return ResponseDTO.ok(Map.of("task", task, "steps", stepDao.selectList(new LambdaQueryWrapper<AiParseStepEntity>()
                .eq(AiParseStepEntity::getTaskId, id).orderByAsc(AiParseStepEntity::getStepId))));
    }

    /** 取消未结束任务；运行中的执行线程在下一阶段前检查状态。 */
    public ResponseDTO<String> cancel(Long id) {
        int changed = taskDao.update(null, new LambdaUpdateWrapper<AiParseTaskEntity>()
                .eq(AiParseTaskEntity::getTaskId, id).in(AiParseTaskEntity::getStatus, "QUEUED", "RUNNING")
                .set(AiParseTaskEntity::getStatus, "CANCELLED").set(AiParseTaskEntity::getFinishTime, LocalDateTime.now()));
        return changed == 1 ? ResponseDTO.okMsg("任务已取消") : ResponseDTO.userErrorParam("任务不存在或已结束");
    }

    /** 失败任务重入队；每次尝试保留历史阶段记录。 */
    public ResponseDTO<String> retry(Long id) {
        int changed = taskDao.update(null, new LambdaUpdateWrapper<AiParseTaskEntity>()
                .eq(AiParseTaskEntity::getTaskId, id).eq(AiParseTaskEntity::getStatus, "FAILED")
                .set(AiParseTaskEntity::getStatus, "QUEUED").set(AiParseTaskEntity::getErrorMessage, null)
                .set(AiParseTaskEntity::getCurrentStage, "UPLOAD").set(AiParseTaskEntity::getFinishTime, null)
                .set(AiParseTaskEntity::getChunkCount, 0).set(AiParseTaskEntity::getIndexedCount, 0));
        return changed == 1 ? ResponseDTO.okMsg("任务已重新排队") : ResponseDTO.userErrorParam("只有失败任务可以重试");
    }

    /** 使用选定方案执行文件解析和真实切片预览，不创建任务。 */
    public ResponseDTO<Map<String, Object>> testPlan(Long id, MultipartFile file) {
        AiDocumentParseConfigEntity plan = planDao.selectById(id);
        if (plan == null || file == null || file.isEmpty() || file.getSize() > 50L * 1024 * 1024)
            return ResponseDTO.userErrorParam("方案不存在或测试文件为空/超过 50 MB");
        String extension = FileUtil.extName(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!Arrays.asList(plan.getSupportedFormats().split(",")).contains(extension))
            return ResponseDTO.userErrorParam("方案不支持该文件格式");
        if (List.of("png", "jpg", "jpeg").contains(extension)
                && !(Boolean.TRUE.equals(plan.getOcrEnabled()) && plan.getOcrServiceId() != null)
                && !(Boolean.TRUE.equals(plan.getExtractImage()) && plan.getImageServiceId() != null))
            return ResponseDTO.userErrorParam("该方案未配置图片解析服务");
        try {
            String text = parse(plan, file.getBytes(), file.getOriginalFilename());
            List<String> chunks = chunk(text, plan);
            if (chunks.isEmpty()) return ResponseDTO.userErrorParam("未提取到可切片文本");
            return ResponseDTO.ok(Map.of("fileName", file.getOriginalFilename(), "characters", text.length(),
                    "chunkCount", chunks.size(), "chunks", chunks.stream().limit(10).toList()));
        } catch (Exception exception) {
            return ResponseDTO.userErrorParam("解析测试失败：" + StrUtil.maxLength(exception.getMessage(), 300));
        }
    }

    /** 每次领取一个排队任务；数据库 CAS 防止多实例重复执行。 */
    @Scheduled(fixedDelay = 2000)
    public void processQueue() {
        failExpiredTasks();
        List<AiParseTaskEntity> queued = taskDao.selectList(new LambdaQueryWrapper<AiParseTaskEntity>()
                .eq(AiParseTaskEntity::getStatus, "QUEUED").orderByAsc(AiParseTaskEntity::getCreateTime).last("LIMIT 1"));
        if (queued.isEmpty()) return;
        AiParseTaskEntity task = queued.get(0);
        if (taskDao.update(null, new LambdaUpdateWrapper<AiParseTaskEntity>()
                .eq(AiParseTaskEntity::getTaskId, task.getTaskId()).eq(AiParseTaskEntity::getStatus, "QUEUED")
                .set(AiParseTaskEntity::getStatus, "RUNNING").set(AiParseTaskEntity::getStartTime, LocalDateTime.now())) != 1) return;
        try {
            AiKnowledgeBaseEntity base = baseDao.selectById(task.getKnowledgeBaseId());
            AiDocumentParseConfigEntity plan = planDao.selectById(task.getParsePlanId());
            if (base == null || plan == null) throw new IllegalStateException("知识库或解析方案已删除");
            long deadline = System.currentTimeMillis() + plan.getTimeoutMinutes() * 60_000L;
            byte[] bytes = Files.readAllBytes(Path.of(task.getFilePath()));
            long begin = System.currentTimeMillis();
            stage(task.getTaskId(), "PARSE");
            String text;
            try {
                checkDeadline(deadline);
                text = parse(plan, bytes, task.getFileName());
                checkDeadline(deadline);
                recordStep(task.getTaskId(), "PARSE", "SUCCESS", "提取字符数：" + text.length(), null, System.currentTimeMillis() - begin);
            } catch (Exception exception) {
                recordStep(task.getTaskId(), "PARSE", "FAILED", null, exception.getMessage(), System.currentTimeMillis() - begin);
                throw exception;
            }
            begin = System.currentTimeMillis();
            stage(task.getTaskId(), "CHUNK");
            List<String> chunks;
            try {
                checkDeadline(deadline);
                chunks = chunk(text, plan);
                if (chunks.isEmpty()) throw new IllegalStateException("解析文本未产生有效切片");
                taskDao.update(null, new LambdaUpdateWrapper<AiParseTaskEntity>()
                        .eq(AiParseTaskEntity::getTaskId, task.getTaskId()).set(AiParseTaskEntity::getChunkCount, chunks.size()));
                recordStep(task.getTaskId(), "CHUNK", "SUCCESS", "切片数：" + chunks.size(), null, System.currentTimeMillis() - begin);
            } catch (Exception exception) {
                recordStep(task.getTaskId(), "CHUNK", "FAILED", null, exception.getMessage(), System.currentTimeMillis() - begin);
                throw exception;
            }
            begin = System.currentTimeMillis();
            stage(task.getTaskId(), "INDEX");
            try {
                int count = index(base, task, chunks, deadline);
                if (taskDao.update(null, new LambdaUpdateWrapper<AiParseTaskEntity>()
                        .eq(AiParseTaskEntity::getTaskId, task.getTaskId()).eq(AiParseTaskEntity::getStatus, "RUNNING")
                        .set(AiParseTaskEntity::getStatus, "SUCCESS").set(AiParseTaskEntity::getFinishTime, LocalDateTime.now())) != 1)
                    throw new CancelledException();
                recordStep(task.getTaskId(), "INDEX", "SUCCESS", "已写入 Qdrant 向量：" + count, null, System.currentTimeMillis() - begin);
                if ("REPLACE".equals(plan.getDuplicateStrategy())) clearReplacedPoints(base, task);
            } catch (CancelledException exception) {
                throw exception;
            } catch (Exception exception) {
                clearPoints(base, task);
                recordStep(task.getTaskId(), "INDEX", "FAILED", null, exception.getMessage(), System.currentTimeMillis() - begin);
                throw exception;
            }
        } catch (CancelledException exception) {
            AiKnowledgeBaseEntity base = baseDao.selectById(task.getKnowledgeBaseId());
            if (base != null) clearPoints(base, task);
            String currentStage = taskDao.selectById(task.getTaskId()).getCurrentStage();
            if (STAGES.contains(currentStage))
                recordStep(task.getTaskId(), currentStage, "CANCELLED", "用户取消", null, 0);
        } catch (Exception exception) {
            log.warn("解析任务失败，taskId={}", task.getTaskId(), exception);
            taskDao.update(null, new LambdaUpdateWrapper<AiParseTaskEntity>()
                    .eq(AiParseTaskEntity::getTaskId, task.getTaskId()).eq(AiParseTaskEntity::getStatus, "RUNNING")
                    .set(AiParseTaskEntity::getStatus, "FAILED").set(AiParseTaskEntity::getErrorMessage, StrUtil.maxLength(exception.getMessage(), 1000))
                    .set(AiParseTaskEntity::getFinishTime, LocalDateTime.now()));
        }
    }

    /** 恢复中断的任务：超过方案处理期限的运行任务失败落盘，避免服务重启后永久卡住。 */
    private void failExpiredTasks() {
        List<AiParseTaskEntity> running = taskDao.selectList(new LambdaQueryWrapper<AiParseTaskEntity>()
                .eq(AiParseTaskEntity::getStatus, "RUNNING").orderByAsc(AiParseTaskEntity::getStartTime).last("LIMIT 20"));
        for (AiParseTaskEntity task : running) {
            AiDocumentParseConfigEntity plan = planDao.selectById(task.getParsePlanId());
            if (task.getStartTime() == null || plan == null || plan.getTimeoutMinutes() == null
                    || task.getStartTime().plusMinutes(plan.getTimeoutMinutes()).isAfter(LocalDateTime.now())) continue;
            String reason = "任务超过方案规定的处理时限或处理进程已中断，请重试";
            if (taskDao.update(null, new LambdaUpdateWrapper<AiParseTaskEntity>()
                    .eq(AiParseTaskEntity::getTaskId, task.getTaskId()).eq(AiParseTaskEntity::getStatus, "RUNNING")
                    .set(AiParseTaskEntity::getStatus, "FAILED").set(AiParseTaskEntity::getErrorMessage, reason)
                    .set(AiParseTaskEntity::getFinishTime, LocalDateTime.now())) == 1) {
                AiKnowledgeBaseEntity base = baseDao.selectById(task.getKnowledgeBaseId());
                if (base != null) clearPoints(base, task);
                recordStep(task.getTaskId(), task.getCurrentStage(), "FAILED", null, reason,
                        java.time.Duration.between(task.getStartTime(), LocalDateTime.now()).toMillis());
            }
        }
    }

    /** 进入阶段前确认未取消，更新监控阶段。 */
    private void stage(Long taskId, String stage) {
        if (taskDao.update(null, new LambdaUpdateWrapper<AiParseTaskEntity>()
                .eq(AiParseTaskEntity::getTaskId, taskId).eq(AiParseTaskEntity::getStatus, "RUNNING")
                .set(AiParseTaskEntity::getCurrentStage, stage)) != 1) throw new CancelledException();
    }

    /** 在每个耗时阶段及向量请求前确认方案规定的任务期限。 */
    private void checkDeadline(long deadline) {
        if (System.currentTimeMillis() >= deadline) throw new IllegalStateException("解析任务超过方案规定的处理时限");
    }

    /** 保存一次阶段执行结果及耗时。 */
    private void recordStep(Long taskId, String stage, String status, String result, String error, long ms) {
        AiParseStepEntity step = new AiParseStepEntity();
        step.setTaskId(taskId);
        step.setStage(stage);
        step.setStatus(status);
        step.setResultSummary(result);
        step.setErrorMessage(StrUtil.maxLength(error, 1000));
        step.setDurationMs(ms);
        step.setStartTime(LocalDateTime.now().minusNanos(ms * 1_000_000));
        step.setFinishTime(LocalDateTime.now());
        stepDao.insert(step);
    }

    /** 利用 Tika 或 HTTP API 提取文本；可选 OCR/表格/图片能力按方案执行。 */
    private String parse(AiDocumentParseConfigEntity plan, byte[] bytes, String name) {
        AiParseServiceEntity parser = requiredService(plan.getParserServiceId(), "TIKA");
        String extension = FileUtil.extName(name).toLowerCase(Locale.ROOT);
        boolean image = List.of("png", "jpg", "jpeg").contains(extension);
        String text = image && Boolean.TRUE.equals(plan.getExtractImage())
                ? extract(requiredService(plan.getImageServiceId(), "IMAGE"), bytes, name)
                : image ? "" : extract(parser, bytes, name);
        if (StrUtil.isBlank(text) && Boolean.TRUE.equals(plan.getOcrEnabled()))
            text = extract(requiredService(plan.getOcrServiceId(), "OCR"), bytes, name);
        if (StrUtil.isBlank(text)) throw new IllegalStateException("没有提取到文本；扫描件请配置并启用 OCR 服务");
        if (Boolean.TRUE.equals(plan.getExtractTable()) && plan.getTableServiceId() != null
                && List.of("xls", "xlsx", "csv").contains(extension))
            text += "\n" + extract(requiredService(plan.getTableServiceId(), "TABLE"), bytes, name);
        return text;
    }

    /** 调用配置的服务；HTTP 合约为 POST /parse multipart file，JSON text 字段。 */
    private String extract(AiParseServiceEntity service, byte[] bytes, String name) {
        if (!Arrays.asList(service.getSupportedFormats().split(",")).contains(FileUtil.extName(name).toLowerCase(Locale.ROOT)))
            throw new IllegalStateException(service.getServiceName() + " 不支持当前文件格式");
        if ("BUILTIN".equals(service.getDeploymentType())) {
            try {
                return new Tika().parseToString(new java.io.ByteArrayInputStream(bytes));
            } catch (Exception exception) {
                throw new IllegalStateException("Apache Tika 解析失败：" + exception.getMessage(), exception);
            }
        }
        try (HttpResponse response = authorized(HttpRequest.post(service.getEndpoint() + "/parse")
                .form("file", bytes, name), service).execute()) {
            ensureOk(response);
            JSONObject json = JSONUtil.parseObj(response.body());
            String text = json.getStr("text");
            if (StrUtil.isBlank(text)) throw new IllegalStateException("解析 API 响应缺少 text 字段");
            return text;
        }
    }

    /** 递归字符切片，按已配置分隔符优先寻找边界。 */
    private List<String> chunk(String raw, AiDocumentParseConfigEntity plan) {
        String text = StrUtil.trim(raw);
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + plan.getChunkSize());
            if ("RECURSIVE".equals(plan.getChunkMethod()) && end < text.length()) {
                String separators = StrUtil.blankToDefault(plan.getSeparators(), "\\n\\n,\\n,。！？；");
                int candidate = -1;
                for (String group : separators.split(",")) {
                    String separator = group.replace("\\n", "\n").replace("\\r", "\r");
                    for (int i = 0; i < separator.length(); i++) {
                        int position = text.lastIndexOf(separator.substring(i, i + 1), end - 1);
                        if (position + 1 >= start + plan.getMinChunkSize()) candidate = Math.max(candidate, position + 1);
                    }
                }
                if (candidate > start) end = candidate;
            }
            String piece = StrUtil.trim(text.substring(start, end));
            if (!piece.isEmpty() && (piece.length() >= plan.getMinChunkSize() || end == text.length())) result.add(piece);
            if (end == text.length()) break;
            start = Math.max(start + 1, end - plan.getChunkOverlap());
        }
        return result;
    }

    /** 对每片真实请求 embedding 并以任务专属点 ID 写入 Qdrant，失败重试不会覆盖历史成功版本。 */
    private int index(AiKnowledgeBaseEntity base, AiParseTaskEntity task, List<String> chunks, long deadline) {
        AiModelEntity model = modelDao.selectById(base.getEmbeddingModelId());
        AiModelServiceEntity provider = model == null ? null : modelServiceDao.selectById(model.getServiceId());
        AiVectorDatabaseEntity database = vectorDao.selectById(base.getVectorDatabaseId());
        if (model == null || provider == null || database == null || !Boolean.TRUE.equals(model.getEnabledFlag())
                || !Boolean.TRUE.equals(provider.getEnabledFlag()) || !Boolean.TRUE.equals(database.getEnabledFlag()))
            throw new IllegalStateException("入库依赖的模型、模型服务或向量数据库不可用");
        int dimension = model.getEmbeddingDimension();
        String collectionUrl = database.getServiceUrl() + "/collections/" + base.getCollectionName();
        String key = decrypt(database.getApiKeyCipher());
        try (HttpResponse response = vectorRequest(HttpRequest.get(collectionUrl), database, key).execute()) {
            if (response.getStatus() == 404) {
                JSONObject configuration = JSONUtil.createObj().set("vectors", JSONUtil.createObj()
                        .set("size", dimension).set("distance", "Cosine"));
                try (HttpResponse created = vectorRequest(HttpRequest.put(collectionUrl).body(configuration.toString())
                        .header("Content-Type", "application/json"), database, key).execute()) { ensureOk(created); }
            } else ensureOk(response);
        }
        clearPoints(base, task);
        for (int i = 0; i < chunks.size(); i++) {
            checkDeadline(deadline);
            if (!"RUNNING".equals(taskDao.selectById(task.getTaskId()).getStatus())) throw new CancelledException();
            JSONArray embedding = embed(model, provider, chunks.get(i));
            checkDeadline(deadline);
            if (!"RUNNING".equals(taskDao.selectById(task.getTaskId()).getStatus())) throw new CancelledException();
            if (embedding.size() != dimension) throw new IllegalStateException("向量维度不一致：模型返回 " + embedding.size() + "，配置 " + dimension);
            String pointId = UUID.nameUUIDFromBytes((task.getTaskId() + ":" + i).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
            JSONObject payload = JSONUtil.createObj().set("knowledgeBaseId", base.getKnowledgeBaseId())
                    .set("fileHash", task.getFileHash())
                    .set("taskId", task.getTaskId()).set("fileName", task.getFileName()).set("chunkIndex", i).set("text", chunks.get(i));
            JSONArray points = new JSONArray();
            points.add(JSONUtil.createObj().set("id", pointId).set("vector", embedding).set("payload", payload));
            JSONObject body = JSONUtil.createObj().set("points", points);
            try (HttpResponse response = vectorRequest(HttpRequest.put(collectionUrl + "/points?wait=true")
                    .header("Content-Type", "application/json").body(body.toString()), database, key).execute()) {
                ensureOk(response);
                JSONObject result = JSONUtil.parseObj(response.body()).getJSONObject("result");
                if (result == null || !"completed".equalsIgnoreCase(result.getStr("status", "")))
                    throw new IllegalStateException("Qdrant 未确认写入完成");
            }
            taskDao.update(null, new LambdaUpdateWrapper<AiParseTaskEntity>()
                    .eq(AiParseTaskEntity::getTaskId, task.getTaskId()).set(AiParseTaskEntity::getIndexedCount, i + 1));
        }
        return chunks.size();
    }

    /** 仅清除当前任务的部分向量，避免重试或取消误删历史成功版本。 */
    private void clearPoints(AiKnowledgeBaseEntity base, AiParseTaskEntity task) {
        deletePoints(base, JSONUtil.createObj().set("must", new JSONArray().put(
                JSONUtil.createObj().set("key", "taskId").set("match", JSONUtil.createObj().set("value", task.getTaskId())))), task);
    }

    /** 新版本成功后再删除同文件的历史向量；失败时保留原版本。 */
    private void clearReplacedPoints(AiKnowledgeBaseEntity base, AiParseTaskEntity task) {
        deletePoints(base, JSONUtil.createObj()
                .set("must", new JSONArray().put(JSONUtil.createObj().set("key", "fileHash")
                        .set("match", JSONUtil.createObj().set("value", task.getFileHash()))))
                .set("must_not", new JSONArray().put(JSONUtil.createObj().set("key", "taskId")
                        .set("match", JSONUtil.createObj().set("value", task.getTaskId())))), task);
    }

    /** 按 Qdrant payload 过滤器删除指定范围的点。 */
    private void deletePoints(AiKnowledgeBaseEntity base, JSONObject filter, AiParseTaskEntity task) {
        AiVectorDatabaseEntity database = vectorDao.selectById(base.getVectorDatabaseId());
        if (database == null) return;
        JSONObject body = JSONUtil.createObj().set("filter", filter);
        try (HttpResponse response = vectorRequest(HttpRequest.post(database.getServiceUrl()
                + "/collections/" + base.getCollectionName() + "/points/delete?wait=true")
                .header("Content-Type", "application/json").body(body.toString()), database,
                decrypt(database.getApiKeyCipher())).execute()) {
            if (response.getStatus() != 404) ensureOk(response);
        } catch (Exception exception) {
            log.warn("清理任务向量失败，taskId={}", task.getTaskId(), exception);
        }
    }

    /** 使用 OpenAI 兼容 embedding API 返回完整向量。 */
    private JSONArray embed(AiModelEntity model, AiModelServiceEntity provider, String text) {
        String baseUrl = StrUtil.removeSuffix(provider.getBaseUrl(), "/");
        String url = (baseUrl.endsWith("/v1") ? baseUrl : baseUrl + "/v1") + "/embeddings";
        JSONObject body = JSONUtil.createObj().set("model", model.getModelCode()).set("input", text);
        HttpRequest request = HttpRequest.post(url).header("Content-Type", "application/json")
                .body(body.toString()).timeout(Math.max(1, provider.getRequestTimeoutSeconds()) * 1000);
        if (StrUtil.isNotBlank(provider.getApiKeyCipher())) request.bearerAuth(decrypt(provider.getApiKeyCipher()));
        if (StrUtil.isNotBlank(provider.getOrganizationId())) request.header("OpenAI-Organization", provider.getOrganizationId());
        try (HttpResponse response = request.execute()) {
            ensureOk(response);
            JSONArray data = JSONUtil.parseObj(response.body()).getJSONArray("data");
            if (data == null || data.isEmpty()) throw new IllegalStateException("模型没有返回向量");
            return data.getJSONObject(0).getJSONArray("embedding");
        }
    }

    /** 校验方案引用的服务类型与状态。 */
    private String validateService(Long id, String type, boolean enabled) {
        AiParseServiceEntity entity = id == null ? null : serviceDao.selectById(id);
        return entity == null || !type.equals(entity.getServiceType()) || (enabled && !Boolean.TRUE.equals(entity.getEnabledFlag()))
                ? "请选择已启用的 " + type + " 解析服务" : null;
    }

    /** 取出执行阶段所需服务。 */
    private AiParseServiceEntity requiredService(Long id, String type) {
        String error = validateService(id, type, true);
        if (error != null) throw new IllegalStateException(error);
        return serviceDao.selectById(id);
    }

    /** HTTP 授权与超时。 */
    private HttpRequest authorized(HttpRequest request, AiParseServiceEntity entity) {
        if (StrUtil.isNotBlank(entity.getApiKeyCipher())) request.bearerAuth(decrypt(entity.getApiKeyCipher()));
        return request.timeout(Math.max(1, entity.getTimeoutSeconds()) * 1000);
    }

    /** Qdrant 授权与超时。 */
    private HttpRequest vectorRequest(HttpRequest request, AiVectorDatabaseEntity database, String key) {
        if (StrUtil.isNotBlank(key)) request.header("api-key", key);
        return request.timeout(Math.max(1, database.getRequestTimeoutSeconds()) * 1000);
    }

    /** 校验外部服务返回状态。 */
    private void ensureOk(HttpResponse response) {
        if (!response.isOk()) throw new IllegalStateException("HTTP " + response.getStatus() + "：" + StrUtil.maxLength(response.body(), 300));
    }

    /** 解密持久化的服务密钥。 */
    private String decrypt(String cipher) {
        return StrUtil.isBlank(cipher) ? "" : encryptService.decrypt(cipher);
    }

    /** 验证管理员配置的外部地址。 */
    private boolean validUrl(String value) {
        if (StrUtil.isBlank(value)) return false;
        try {
            URI uri = URI.create(value);
            return List.of("http", "https").contains(uri.getScheme()) && StrUtil.isNotBlank(uri.getHost()) && uri.getUserInfo() == null;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** 获取由任务记录指向的受控文件，供登录用户下载。 */
    public Path taskFile(Long id) {
        AiParseTaskEntity task = taskDao.selectById(id);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        Path root = Path.of(documentDir).toAbsolutePath().normalize();
        Path path = Path.of(task.getFilePath()).toAbsolutePath().normalize();
        if (!path.startsWith(root)) throw new IllegalStateException("文件路径不在文档存储目录内");
        return path;
    }

    /** 运行中任务被管理员取消时终止后续阶段。 */
    private static class CancelledException extends RuntimeException {
    }
}
