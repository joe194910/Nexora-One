package com.nexoraone.admin.module.business.ai.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexoraone.admin.module.business.ai.dao.AiCallLogDao;
import com.nexoraone.admin.module.business.ai.dao.AiDocumentParseConfigDao;
import com.nexoraone.admin.module.business.ai.dao.AiModelDao;
import com.nexoraone.admin.module.business.ai.dao.AiModelServiceDao;
import com.nexoraone.admin.module.business.ai.dao.AiVectorDatabaseDao;
import com.nexoraone.admin.module.business.ai.domain.entity.AiCallLogEntity;
import com.nexoraone.admin.module.business.ai.domain.entity.AiDocumentParseConfigEntity;
import com.nexoraone.admin.module.business.ai.domain.entity.AiModelEntity;
import com.nexoraone.admin.module.business.ai.domain.entity.AiModelServiceEntity;
import com.nexoraone.admin.module.business.ai.domain.entity.AiVectorDatabaseEntity;
import com.nexoraone.admin.module.business.ai.domain.form.AiPlatformForm;
import com.nexoraone.admin.util.AdminRequestUtil;
import com.nexoraone.base.common.domain.PageResult;
import com.nexoraone.base.common.domain.ResponseDTO;
import com.nexoraone.base.common.util.SmartPageUtil;
import com.nexoraone.base.module.support.apiencrypt.service.ApiEncryptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * AI 平台配置、日志和统计服务。
 */
@Slf4j
@Service
public class AiPlatformService {

    private static final String SECRET_MASK = "******";
    private static final long MAX_QUERY_DAYS = 366;
    private static final int MAX_EXPORT_ROWS = 100_000;

    @Resource
    private AiModelServiceDao modelServiceDao;
    @Resource
    private AiModelDao modelDao;
    @Resource
    private AiVectorDatabaseDao vectorDatabaseDao;
    @Resource
    private AiDocumentParseConfigDao documentConfigDao;
    @Resource
    private AiCallLogDao callLogDao;
    @Resource
    private ApiEncryptService apiEncryptService;
    @Resource
    private AiRuntimeService runtimeService;

    /**
     * 分页查询模型服务。
     */
    public ResponseDTO<PageResult<AiModelServiceEntity>> queryModelServices(AiPlatformForm.PageQuery form) {
        LambdaQueryWrapper<AiModelServiceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(form.getKeyword()), AiModelServiceEntity::getServiceName, form.getKeyword())
                .eq(StrUtil.isNotBlank(form.getType()), AiModelServiceEntity::getProtocolType, form.getType())
                .eq(form.getEnabledFlag() != null, AiModelServiceEntity::getEnabledFlag, form.getEnabledFlag())
                .orderByDesc(AiModelServiceEntity::getDefaultFlag)
                .orderByDesc(AiModelServiceEntity::getUpdateTime);
        Page<AiModelServiceEntity> page = modelServiceDao.selectPage(page(form), wrapper);
        page.getRecords().forEach(this::maskSecret);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, page.getRecords()));
    }

    /**
     * 查询模型服务概览。
     */
    public ResponseDTO<Map<String, Object>> modelServiceSummary() {
        List<AiModelServiceEntity> list = modelServiceDao.selectList(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", list.size());
        result.put("enabled", list.stream().filter(item -> Boolean.TRUE.equals(item.getEnabledFlag())).count());
        result.put("connected", list.stream().filter(item -> "CONNECTED".equals(item.getConnectionStatus())).count());
        result.put("defaultName", list.stream().filter(item -> Boolean.TRUE.equals(item.getDefaultFlag()))
                .map(AiModelServiceEntity::getServiceName).findFirst().orElse("未设置"));
        return ResponseDTO.ok(result);
    }

    /**
     * 查询全部可选模型服务。
     */
    public ResponseDTO<List<AiModelServiceEntity>> modelServiceOptions() {
        List<AiModelServiceEntity> list = modelServiceDao.selectList(
                new LambdaQueryWrapper<AiModelServiceEntity>().orderByDesc(AiModelServiceEntity::getEnabledFlag)
                        .orderByAsc(AiModelServiceEntity::getServiceName));
        list.forEach(this::maskSecret);
        return ResponseDTO.ok(list);
    }

    /**
     * 新增或更新模型服务。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> saveModelService(AiPlatformForm.ModelServiceSave form) {
        AiModelServiceEntity entity = form.getServiceId() == null ? new AiModelServiceEntity()
                : modelServiceDao.selectById(form.getServiceId());
        if (entity == null) {
            return ResponseDTO.userErrorParam("模型服务不存在");
        }
        if (modelServiceDao.selectCount(new LambdaQueryWrapper<AiModelServiceEntity>()
                .eq(AiModelServiceEntity::getServiceName, form.getServiceName())
                .ne(form.getServiceId() != null, AiModelServiceEntity::getServiceId, form.getServiceId())) > 0) {
            return ResponseDTO.userErrorParam("服务名称已存在");
        }
        BeanUtil.copyProperties(form, entity, "apiKey", "capabilities");
        entity.setBaseUrl(StrUtil.removeSuffix(StrUtil.trim(form.getBaseUrl()), "/"));
        entity.setCapabilities(CollUtil.join(form.getCapabilities(), ","));
        if (StrUtil.isNotBlank(form.getApiKey()) && !SECRET_MASK.equals(form.getApiKey())) {
            entity.setApiKeyCipher(apiEncryptService.encrypt(form.getApiKey()));
        }
        if (form.getServiceId() == null && StrUtil.isBlank(entity.getApiKeyCipher())) {
            return ResponseDTO.userErrorParam("API Key 不能为空");
        }
        entity.setDefaultFlag(Boolean.TRUE.equals(form.getDefaultFlag()));
        entity.setEnabledFlag(!Boolean.FALSE.equals(form.getEnabledFlag()));
        if (Boolean.TRUE.equals(entity.getDefaultFlag()) && !Boolean.TRUE.equals(entity.getEnabledFlag())) {
            return ResponseDTO.userErrorParam("默认模型服务必须保持启用");
        }
        if (!Boolean.TRUE.equals(entity.getEnabledFlag()) && form.getServiceId() != null
                && modelDao.selectCount(new LambdaQueryWrapper<AiModelEntity>()
                .eq(AiModelEntity::getServiceId, form.getServiceId())
                .eq(AiModelEntity::getEnabledFlag, true)) > 0) {
            return ResponseDTO.userErrorParam("该服务下仍有启用模型，请先停用关联模型");
        }
        entity.setUpdateUserId(AdminRequestUtil.getRequestUserId());
        entity.setUpdateTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(entity.getDefaultFlag())) {
            clearModelServiceDefault(form.getServiceId());
        }
        if (form.getServiceId() == null) {
            entity.setConnectionStatus("UNTESTED");
            entity.setCreateUserId(AdminRequestUtil.getRequestUserId());
            entity.setCreateTime(LocalDateTime.now());
            modelServiceDao.insert(entity);
        } else {
            modelServiceDao.updateById(entity);
        }
        return ResponseDTO.okMsg("模型服务保存成功");
    }

    /**
     * 测试模型服务连接并持久化测试结果。
     */
    public ResponseDTO<Map<String, Object>> testModelService(Long serviceId) {
        AiModelServiceEntity entity = modelServiceDao.selectById(serviceId);
        if (entity == null) {
            return ResponseDTO.userErrorParam("模型服务不存在");
        }
        try {
            Map<String, Object> result = runtimeService.testModelService(entity);
            updateServiceConnection(entity, "CONNECTED", null);
            return ResponseDTO.ok(result);
        } catch (Exception exception) {
            updateServiceConnection(entity, "FAILED", StrUtil.maxLength(exception.getMessage(), 1000));
            return ResponseDTO.userErrorParam("连接失败：" + StrUtil.maxLength(exception.getMessage(), 300));
        }
    }

    /**
     * 删除未被模型引用的模型服务。
     */
    public ResponseDTO<String> deleteModelService(Long serviceId) {
        AiModelServiceEntity entity = modelServiceDao.selectById(serviceId);
        if (entity == null) {
            return ResponseDTO.userErrorParam("模型服务不存在");
        }
        if (Boolean.TRUE.equals(entity.getDefaultFlag())) {
            return ResponseDTO.userErrorParam("默认模型服务不能删除，请先设置其他默认服务");
        }
        if (modelDao.selectCount(new LambdaQueryWrapper<AiModelEntity>()
                .eq(AiModelEntity::getServiceId, serviceId)) > 0) {
            return ResponseDTO.userErrorParam("该服务已被模型引用，请先迁移或删除关联模型");
        }
        modelServiceDao.deleteById(serviceId);
        return ResponseDTO.okMsg("模型服务删除成功");
    }

    /**
     * 分页查询模型。
     */
    public ResponseDTO<PageResult<AiModelEntity>> queryModels(AiPlatformForm.PageQuery form) {
        LambdaQueryWrapper<AiModelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StrUtil.isNotBlank(form.getKeyword()), query -> query
                        .like(AiModelEntity::getModelName, form.getKeyword())
                        .or().like(AiModelEntity::getModelCode, form.getKeyword()))
                .eq(StrUtil.isNotBlank(form.getType()), AiModelEntity::getModelType, form.getType())
                .eq(form.getServiceId() != null, AiModelEntity::getServiceId, form.getServiceId())
                .eq(form.getEnabledFlag() != null, AiModelEntity::getEnabledFlag, form.getEnabledFlag())
                .orderByDesc(AiModelEntity::getDefaultFlag)
                .orderByDesc(AiModelEntity::getUpdateTime);
        Page<AiModelEntity> page = modelDao.selectPage(page(form), wrapper);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, page.getRecords()));
    }

    /**
     * 查询模型概览。
     */
    public ResponseDTO<Map<String, Object>> modelSummary() {
        List<AiModelEntity> list = modelDao.selectList(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", list.size());
        result.put("chat", list.stream().filter(item -> "CHAT".equals(item.getModelType())).count());
        result.put("embedding", list.stream().filter(item -> "EMBEDDING".equals(item.getModelType())).count());
        result.put("enabled", list.stream().filter(item -> Boolean.TRUE.equals(item.getEnabledFlag())).count());
        return ResponseDTO.ok(result);
    }

    /**
     * 新增或更新模型。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> saveModel(AiPlatformForm.ModelSave form) {
        AiModelServiceEntity service = modelServiceDao.selectById(form.getServiceId());
        if (service == null) {
            return ResponseDTO.userErrorParam("所属模型服务不存在");
        }
        if (!Boolean.TRUE.equals(service.getEnabledFlag()) && !Boolean.FALSE.equals(form.getEnabledFlag())) {
            return ResponseDTO.userErrorParam("所属模型服务未启用，不能启用该模型");
        }
        if ("EMBEDDING".equalsIgnoreCase(form.getModelType()) && form.getEmbeddingDimension() == null) {
            return ResponseDTO.userErrorParam("向量模型必须配置向量维度");
        }
        if (!"EMBEDDING".equalsIgnoreCase(form.getModelType()) && form.getContextLength() == null) {
            return ResponseDTO.userErrorParam("非向量模型必须配置上下文长度");
        }
        if (modelDao.selectCount(new LambdaQueryWrapper<AiModelEntity>()
                .eq(AiModelEntity::getModelCode, form.getModelCode())
                .eq(AiModelEntity::getServiceId, form.getServiceId())
                .ne(form.getModelId() != null, AiModelEntity::getModelId, form.getModelId())) > 0) {
            return ResponseDTO.userErrorParam("当前服务下模型编码已存在");
        }
        AiModelEntity entity = form.getModelId() == null ? new AiModelEntity() : modelDao.selectById(form.getModelId());
        if (entity == null) {
            return ResponseDTO.userErrorParam("模型不存在");
        }
        BeanUtil.copyProperties(form, entity, "capabilities");
        entity.setCapabilities(CollUtil.join(form.getCapabilities(), ","));
        entity.setTemperature(form.getTemperature() == null ? new BigDecimal("0.7") : form.getTemperature());
        entity.setDefaultFlag(Boolean.TRUE.equals(form.getDefaultFlag()));
        entity.setEnabledFlag(!Boolean.FALSE.equals(form.getEnabledFlag()));
        if (Boolean.TRUE.equals(entity.getDefaultFlag()) && !Boolean.TRUE.equals(entity.getEnabledFlag())) {
            return ResponseDTO.userErrorParam("默认模型必须保持启用");
        }
        entity.setUpdateUserId(AdminRequestUtil.getRequestUserId());
        entity.setUpdateTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(entity.getDefaultFlag())) {
            modelDao.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AiModelEntity>()
                    .eq(AiModelEntity::getModelType, entity.getModelType())
                    .ne(form.getModelId() != null, AiModelEntity::getModelId, form.getModelId())
                    .set(AiModelEntity::getDefaultFlag, false));
        }
        if (form.getModelId() == null) {
            entity.setCreateUserId(AdminRequestUtil.getRequestUserId());
            entity.setCreateTime(LocalDateTime.now());
            modelDao.insert(entity);
        } else {
            modelDao.updateById(entity);
        }
        return ResponseDTO.okMsg("模型保存成功");
    }

    /**
     * 删除没有调用记录引用的模型。
     */
    public ResponseDTO<String> deleteModel(Long modelId) {
        AiModelEntity entity = modelDao.selectById(modelId);
        if (entity == null) {
            return ResponseDTO.userErrorParam("模型不存在");
        }
        if (Boolean.TRUE.equals(entity.getDefaultFlag())) {
            return ResponseDTO.userErrorParam("默认模型不能删除，请先设置同类型的其他默认模型");
        }
        if (callLogDao.selectCount(new LambdaQueryWrapper<AiCallLogEntity>()
                .eq(AiCallLogEntity::getModelId, modelId)) > 0) {
            return ResponseDTO.userErrorParam("模型已有调用日志，为保证审计完整性不能删除，可将其停用");
        }
        modelDao.deleteById(modelId);
        return ResponseDTO.okMsg("模型删除成功");
    }

    /**
     * 分页查询向量数据库。
     */
    public ResponseDTO<PageResult<AiVectorDatabaseEntity>> queryVectorDatabases(AiPlatformForm.PageQuery form) {
        LambdaQueryWrapper<AiVectorDatabaseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(form.getKeyword()), AiVectorDatabaseEntity::getInstanceName, form.getKeyword())
                .eq(StrUtil.isNotBlank(form.getType()), AiVectorDatabaseEntity::getDatabaseType, form.getType())
                .eq(form.getEnabledFlag() != null, AiVectorDatabaseEntity::getEnabledFlag, form.getEnabledFlag())
                .orderByDesc(AiVectorDatabaseEntity::getDefaultFlag)
                .orderByDesc(AiVectorDatabaseEntity::getUpdateTime);
        Page<AiVectorDatabaseEntity> page = vectorDatabaseDao.selectPage(page(form), wrapper);
        page.getRecords().forEach(this::maskSecret);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, page.getRecords()));
    }

    /**
     * 查询全部向量数据库的连接与集合概览。
     */
    public ResponseDTO<Map<String, Object>> vectorDatabaseSummary() {
        List<AiVectorDatabaseEntity> list = vectorDatabaseDao.selectList(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", list.size());
        result.put("connected", list.stream().filter(item -> "CONNECTED".equals(item.getConnectionStatus())).count());
        result.put("collections", list.stream().map(AiVectorDatabaseEntity::getCollectionCount)
                .filter(Objects::nonNull).mapToLong(Integer::longValue).sum());
        result.put("vectors", list.stream().map(AiVectorDatabaseEntity::getVectorCount)
                .filter(Objects::nonNull).mapToLong(Long::longValue).sum());
        return ResponseDTO.ok(result);
    }

    /**
     * 新增或更新向量数据库。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> saveVectorDatabase(AiPlatformForm.VectorDatabaseSave form) {
        if (vectorDatabaseDao.selectCount(new LambdaQueryWrapper<AiVectorDatabaseEntity>()
                .eq(AiVectorDatabaseEntity::getInstanceName, form.getInstanceName())
                .ne(form.getVectorDatabaseId() != null, AiVectorDatabaseEntity::getVectorDatabaseId,
                        form.getVectorDatabaseId())) > 0) {
            return ResponseDTO.userErrorParam("向量数据库实例名称已存在");
        }
        AiVectorDatabaseEntity entity = form.getVectorDatabaseId() == null
                ? new AiVectorDatabaseEntity() : vectorDatabaseDao.selectById(form.getVectorDatabaseId());
        if (entity == null) {
            return ResponseDTO.userErrorParam("向量数据库不存在");
        }
        BeanUtil.copyProperties(form, entity, "apiKey");
        entity.setServiceUrl(StrUtil.removeSuffix(StrUtil.trim(form.getServiceUrl()), "/"));
        if (Boolean.TRUE.equals(form.getTlsEnabled()) && !StrUtil.startWithIgnoreCase(entity.getServiceUrl(), "https://")) {
            return ResponseDTO.userErrorParam("启用 TLS 时服务地址必须使用 https://");
        }
        if (StrUtil.isNotBlank(form.getApiKey()) && !SECRET_MASK.equals(form.getApiKey())) {
            entity.setApiKeyCipher(apiEncryptService.encrypt(form.getApiKey()));
        }
        entity.setDefaultFlag(Boolean.TRUE.equals(form.getDefaultFlag()));
        entity.setEnabledFlag(!Boolean.FALSE.equals(form.getEnabledFlag()));
        if (Boolean.TRUE.equals(entity.getDefaultFlag()) && !Boolean.TRUE.equals(entity.getEnabledFlag())) {
            return ResponseDTO.userErrorParam("默认向量数据库必须保持启用");
        }
        entity.setUpdateUserId(AdminRequestUtil.getRequestUserId());
        entity.setUpdateTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(entity.getDefaultFlag())) {
            vectorDatabaseDao.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AiVectorDatabaseEntity>()
                            .ne(form.getVectorDatabaseId() != null, AiVectorDatabaseEntity::getVectorDatabaseId,
                                    form.getVectorDatabaseId())
                            .set(AiVectorDatabaseEntity::getDefaultFlag, false));
        }
        if (form.getVectorDatabaseId() == null) {
            entity.setConnectionStatus("UNTESTED");
            entity.setCollectionCount(0);
            entity.setVectorCount(0L);
            entity.setCreateUserId(AdminRequestUtil.getRequestUserId());
            entity.setCreateTime(LocalDateTime.now());
            vectorDatabaseDao.insert(entity);
        } else {
            vectorDatabaseDao.updateById(entity);
        }
        return ResponseDTO.okMsg("向量数据库保存成功");
    }

    /**
     * 连接向量数据库并同步集合概览。
     */
    public ResponseDTO<Map<String, Object>> syncVectorDatabase(Long vectorDatabaseId) {
        AiVectorDatabaseEntity entity = vectorDatabaseDao.selectById(vectorDatabaseId);
        if (entity == null) {
            return ResponseDTO.userErrorParam("向量数据库不存在");
        }
        if (!"QDRANT".equalsIgnoreCase(entity.getDatabaseType())) {
            return ResponseDTO.userErrorParam("当前版本已完整支持 Qdrant，其他类型请使用兼容网关后接入");
        }
        HttpRequest request = HttpRequest.get(entity.getServiceUrl() + "/collections")
                .timeout(Math.max(1, entity.getRequestTimeoutSeconds()) * 1000);
        String apiKey = decryptSecret(entity.getApiKeyCipher());
        if (StrUtil.isNotBlank(apiKey)) {
            request.header("api-key", apiKey);
        }
        try (HttpResponse response = request.execute()) {
            if (!response.isOk()) {
                throw new IllegalStateException("HTTP " + response.getStatus() + "，" + StrUtil.maxLength(response.body(), 500));
            }
            JSONArray collections = JSONUtil.parseObj(response.body()).getJSONObject("result").getJSONArray("collections");
            List<Map<String, Object>> list = new ArrayList<>();
            if (collections != null) {
                for (Object value : collections) {
                    JSONObject collection = JSONUtil.parseObj(value);
                    Map<String, Object> item = new LinkedHashMap<>();
                    String collectionName = collection.getStr("name");
                    item.put("name", collectionName);
                    item.putAll(queryQdrantCollection(entity, collectionName, apiKey));
                    list.add(item);
                }
            }
            long vectorCount = list.stream().map(item -> item.get("vectorCount"))
                    .filter(Objects::nonNull).mapToLong(value -> ((Number) value).longValue()).sum();
            entity.setConnectionStatus("CONNECTED");
            entity.setCollectionCount(list.size());
            entity.setVectorCount(vectorCount);
            entity.setLastError(null);
            entity.setLastTestTime(LocalDateTime.now());
            vectorDatabaseDao.updateById(entity);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("collections", list);
            result.put("collectionCount", list.size());
            result.put("vectorCount", entity.getVectorCount());
            return ResponseDTO.ok(result);
        } catch (Exception exception) {
            entity.setConnectionStatus("FAILED");
            entity.setLastError(StrUtil.maxLength(exception.getMessage(), 1000));
            entity.setLastTestTime(LocalDateTime.now());
            vectorDatabaseDao.updateById(entity);
            return ResponseDTO.userErrorParam("连接失败：" + StrUtil.maxLength(exception.getMessage(), 300));
        }
    }

    /**
     * 删除向量数据库配置。
     */
    public ResponseDTO<String> deleteVectorDatabase(Long vectorDatabaseId) {
        AiVectorDatabaseEntity entity = vectorDatabaseDao.selectById(vectorDatabaseId);
        if (entity == null) {
            return ResponseDTO.userErrorParam("向量数据库不存在");
        }
        if (Boolean.TRUE.equals(entity.getDefaultFlag())) {
            return ResponseDTO.userErrorParam("默认向量数据库不能删除，请先设置其他默认实例");
        }
        vectorDatabaseDao.deleteById(vectorDatabaseId);
        return ResponseDTO.okMsg("向量数据库删除成功");
    }

    /**
     * 获取文档解析配置。
     */
    public ResponseDTO<AiDocumentParseConfigEntity> documentConfig() {
        AiDocumentParseConfigEntity entity = documentConfigDao.selectOne(
                new LambdaQueryWrapper<AiDocumentParseConfigEntity>().last("LIMIT 1"));
        return ResponseDTO.ok(entity);
    }

    /**
     * 保存文档解析配置。
     */
    public ResponseDTO<String> saveDocumentConfig(AiPlatformForm.DocumentConfigSave form) {
        if (form.getChunkOverlap() >= form.getChunkSize()) {
            return ResponseDTO.userErrorParam("重叠长度必须小于切片长度");
        }
        if (form.getMinChunkSize() > form.getChunkSize()) {
            return ResponseDTO.userErrorParam("最小切片长度不能大于切片长度");
        }
        if (Boolean.TRUE.equals(form.getOcrEnabled()) && !validHttpUrl(form.getOcrService())) {
            return ResponseDTO.userErrorParam("启用 OCR 后必须配置有效的 HTTP(S) OCR 接口地址");
        }
        if (form.getEmbeddingModelId() != null) {
            AiModelEntity model = modelDao.selectById(form.getEmbeddingModelId());
            if (model == null || !"EMBEDDING".equals(model.getModelType()) || !Boolean.TRUE.equals(model.getEnabledFlag())) {
                return ResponseDTO.userErrorParam("请选择已启用的向量模型");
            }
        }
        AiDocumentParseConfigEntity entity = documentConfigDao.selectOne(
                new LambdaQueryWrapper<AiDocumentParseConfigEntity>().last("LIMIT 1"));
        boolean create = entity == null;
        entity = create ? new AiDocumentParseConfigEntity() : entity;
        BeanUtil.copyProperties(form, entity, "supportedFormats");
        entity.setSupportedFormats(CollUtil.join(form.getSupportedFormats(), ","));
        entity.setUpdateUserId(AdminRequestUtil.getRequestUserId());
        entity.setUpdateTime(LocalDateTime.now());
        if (create) {
            documentConfigDao.insert(entity);
        } else {
            documentConfigDao.updateById(entity);
        }
        return ResponseDTO.okMsg("文档解析配置保存成功");
    }

    /**
     * 使用当前配置真实解析上传文档并返回切片预览。
     */
    public ResponseDTO<Map<String, Object>> testDocumentParse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseDTO.userErrorParam("请选择测试文件");
        }
        if (file.getSize() > 50L * 1024 * 1024) {
            return ResponseDTO.userErrorParam("单个文件不能超过 50 MB");
        }
        AiDocumentParseConfigEntity config = documentConfigDao.selectOne(
                new LambdaQueryWrapper<AiDocumentParseConfigEntity>().last("LIMIT 1"));
        if (config == null) {
            return ResponseDTO.userErrorParam("请先保存文档解析配置");
        }
        String extension = FileUtil.extName(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!StrUtil.split(config.getSupportedFormats(), ',').contains(extension)) {
            return ResponseDTO.userErrorParam("当前配置不支持 ." + extension + " 文件");
        }
        try {
            boolean ocrUsed = false;
            String text = "";
            if (!List.of("png", "jpg", "jpeg").contains(extension)) {
                text = new Tika().parseToString(file.getInputStream());
            }
            if (StrUtil.isBlank(text) && Boolean.TRUE.equals(config.getOcrEnabled())) {
                text = recognizeOcr(config, file);
                ocrUsed = true;
            }
            if (StrUtil.isBlank(text)) {
                return ResponseDTO.userErrorParam("未提取到可切片文本，请启用 OCR 并配置可用的识别服务");
            }
            List<String> chunks = chunkText(text, config);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fileName", file.getOriginalFilename());
            result.put("characters", text.length());
            result.put("chunkCount", chunks.size());
            result.put("ocrUsed", ocrUsed);
            result.put("chunks", chunks.stream().limit(10).toList());
            return ResponseDTO.ok(result);
        } catch (Exception exception) {
            log.warn("文档解析测试失败，file={}", file.getOriginalFilename(), exception);
            return ResponseDTO.userErrorParam("解析失败：" + StrUtil.maxLength(exception.getMessage(), 300));
        }
    }

    /**
     * 调用外部 OCR HTTP 接口；以 multipart 的 file 字段上传，响应须包含 text 字段。
     */
    private String recognizeOcr(AiDocumentParseConfigEntity config, MultipartFile file) throws Exception {
        if (!validHttpUrl(config.getOcrService())) {
            throw new IllegalArgumentException("OCR 接口地址不是有效的 HTTP(S) URL");
        }
        int timeout = Math.toIntExact(Math.min(30L, Math.max(1L, config.getTimeoutMinutes())) * 60_000L);
        byte[] bytes = file.getBytes();
        String fileName = StrUtil.blankToDefault(file.getOriginalFilename(), "document");
        int retries = Math.min(2, Math.max(0, config.getRetryCount()));
        for (int attempt = 0; attempt <= retries; attempt++) {
            try (HttpResponse response = HttpRequest.post(config.getOcrService())
                    .form("file", bytes, fileName).timeout(timeout).execute()) {
                if (!response.isOk()) {
                    if (response.getStatus() >= 500 && attempt < retries) {
                        continue;
                    }
                    throw new IllegalStateException("OCR 服务响应 HTTP " + response.getStatus());
                }
                JSONObject result = JSONUtil.parseObj(response.body());
                String text = result.getStr("text");
                if (StrUtil.isBlank(text) && result.getJSONObject("result") != null) {
                    text = result.getJSONObject("result").getStr("text");
                }
                if (StrUtil.isBlank(text)) {
                    throw new IllegalStateException("OCR 服务未返回可用文本，响应须包含 text 字段");
                }
                return text;
            } catch (cn.hutool.core.io.IORuntimeException exception) {
                if (attempt == retries) {
                    throw exception;
                }
            }
        }
        throw new IllegalStateException("OCR 服务调用失败");
    }

    /**
     * 验证管理员配置的 OCR 接口是带主机名的 HTTP(S) 地址。
     */
    private boolean validHttpUrl(String address) {
        if (StrUtil.isBlank(address)) {
            return false;
        }
        try {
            URI uri = URI.create(address.trim());
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && StrUtil.isNotBlank(uri.getHost()) && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * 分页查询调用日志。
     */
    public ResponseDTO<PageResult<AiCallLogEntity>> queryCallLogs(AiPlatformForm.CallLogQuery form) {
        String rangeError = validateTimeRange(form.getBeginTime(), form.getEndTime());
        if (rangeError != null) {
            return ResponseDTO.userErrorParam(rangeError);
        }
        LambdaQueryWrapper<AiCallLogEntity> wrapper = callLogWrapper(form);
        wrapper.orderByDesc(AiCallLogEntity::getCreateTime);
        Page<AiCallLogEntity> page = callLogDao.selectPage(
                new Page<>(form.getPageNum(), form.getPageSize(), !Boolean.FALSE.equals(form.getSearchCount())), wrapper);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, page.getRecords()));
    }

    /**
     * 查询调用日志详情。
     */
    public ResponseDTO<AiCallLogEntity> callLogDetail(Long callLogId) {
        AiCallLogEntity entity = callLogDao.selectById(callLogId);
        return entity == null ? ResponseDTO.userErrorParam("调用日志不存在") : ResponseDTO.ok(entity);
    }

    /**
     * 查询调用日志顶部摘要。
     */
    public ResponseDTO<Map<String, Object>> callLogSummary(AiPlatformForm.CallLogQuery form) {
        String rangeError = validateTimeRange(form.getBeginTime(), form.getEndTime());
        if (rangeError != null) {
            return ResponseDTO.userErrorParam(rangeError);
        }
        List<AiCallLogEntity> list = callLogDao.selectList(callLogWrapper(form));
        long success = list.stream().filter(item -> Boolean.TRUE.equals(item.getSuccessFlag())).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", list.size());
        result.put("success", success);
        result.put("failed", list.size() - success);
        result.put("averageDurationMs", list.stream().map(AiCallLogEntity::getTotalDurationMs)
                .filter(Objects::nonNull).mapToLong(Long::longValue).average().orElse(0));
        return ResponseDTO.ok(result);
    }

    /**
     * 查询用量统计数据。
     */
    public ResponseDTO<Map<String, Object>> statistics(AiPlatformForm.StatisticsQuery form) {
        if (form.getEndTime().isBefore(form.getBeginTime())) {
            return ResponseDTO.userErrorParam("统计结束时间不能早于开始时间");
        }
        if (ChronoUnit.DAYS.between(form.getBeginTime(), form.getEndTime()) > MAX_QUERY_DAYS) {
            return ResponseDTO.userErrorParam("单次统计时间范围不能超过 366 天");
        }
        QueryWrapper<AiCallLogEntity> base = statisticsWrapper(form);
        List<AiCallLogEntity> logs = callLogDao.selectList(base.lambda().orderByAsc(AiCallLogEntity::getCreateTime));
        long success = logs.stream().filter(item -> Boolean.TRUE.equals(item.getSuccessFlag())).count();
        long totalTokens = logs.stream().map(AiCallLogEntity::getTotalTokens).filter(Objects::nonNull)
                .mapToLong(Integer::longValue).sum();
        BigDecimal cost = logs.stream().map(AiCallLogEntity::getEstimatedCost).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("calls", logs.size());
        summary.put("tokens", totalTokens);
        summary.put("successRate", logs.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(success * 100D / logs.size()).setScale(2, RoundingMode.HALF_UP));
        summary.put("cost", cost);

        Map<LocalDate, List<AiCallLogEntity>> dayGroups = logs.stream()
                .collect(Collectors.groupingBy(item -> item.getCreateTime().toLocalDate(), LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate cursor = form.getBeginTime().toLocalDate();
        LocalDate end = form.getEndTime().toLocalDate();
        while (!cursor.isAfter(end)) {
            List<AiCallLogEntity> dayLogs = dayGroups.getOrDefault(cursor, List.of());
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", cursor.format(DateTimeFormatter.ISO_DATE));
            point.put("calls", dayLogs.size());
            point.put("tokens", dayLogs.stream().map(AiCallLogEntity::getTotalTokens).filter(Objects::nonNull)
                    .mapToLong(Integer::longValue).sum());
            trend.add(point);
            cursor = cursor.plusDays(1);
        }

        Map<String, Long> modelDistribution = logs.stream().collect(Collectors.groupingBy(
                item -> StrUtil.blankToDefault(item.getModelCode(), "未知模型"), LinkedHashMap::new, Collectors.counting()));
        List<Map<String, Object>> providers = ranking(logs, true);
        List<Map<String, Object>> users = ranking(logs, false);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("trend", trend);
        result.put("modelDistribution", modelDistribution);
        result.put("providers", providers);
        result.put("users", users);
        return ResponseDTO.ok(result);
    }

    /**
     * 导出调用日志 CSV 内容。
     */
    public byte[] exportCallLogs(AiPlatformForm.CallLogQuery form) {
        String rangeError = validateTimeRange(form.getBeginTime(), form.getEndTime());
        if (rangeError != null) {
            throw new IllegalArgumentException(rangeError);
        }
        List<AiCallLogEntity> list = callLogDao.selectList(callLogWrapper(form)
                .orderByDesc(AiCallLogEntity::getCreateTime)
                .last("LIMIT " + MAX_EXPORT_ROWS));
        StringBuilder csv = new StringBuilder("\uFEFF调用时间,用户,服务商,模型,调用类型,输入Token,输出Token,总Token,耗时毫秒,状态,Trace ID\n");
        for (AiCallLogEntity item : list) {
            csv.append(csv(item.getCreateTime())).append(',')
                    .append(csv(item.getUserName())).append(',')
                    .append(csv(item.getProviderName())).append(',')
                    .append(csv(item.getModelCode())).append(',')
                    .append(csv(item.getCallType())).append(',')
                    .append(item.getInputTokens()).append(',')
                    .append(item.getOutputTokens()).append(',')
                    .append(item.getTotalTokens()).append(',')
                    .append(item.getTotalDurationMs()).append(',')
                    .append(Boolean.TRUE.equals(item.getSuccessFlag()) ? "成功" : "失败").append(',')
                    .append(csv(item.getTraceId())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 创建分页对象。
     */
    private <T> Page<T> page(AiPlatformForm.PageQuery form) {
        return new Page<>(form.getPageNum(), form.getPageSize(), !Boolean.FALSE.equals(form.getSearchCount()));
    }

    /**
     * 清除其他模型服务的默认标识。
     */
    private void clearModelServiceDefault(Long serviceId) {
        modelServiceDao.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AiModelServiceEntity>()
                        .ne(serviceId != null, AiModelServiceEntity::getServiceId, serviceId)
                        .set(AiModelServiceEntity::getDefaultFlag, false));
    }

    /**
     * 更新模型服务连接状态。
     */
    private void updateServiceConnection(AiModelServiceEntity entity, String status, String error) {
        entity.setConnectionStatus(status);
        entity.setLastError(error);
        entity.setLastTestTime(LocalDateTime.now());
        modelServiceDao.updateById(entity);
    }

    /**
     * 对返回给前端的密钥进行掩码处理。
     */
    private void maskSecret(AiModelServiceEntity entity) {
        entity.setApiKeyCipher(StrUtil.isBlank(entity.getApiKeyCipher()) ? "" : SECRET_MASK);
    }

    /**
     * 对返回给前端的向量数据库密钥进行掩码处理。
     */
    private void maskSecret(AiVectorDatabaseEntity entity) {
        entity.setApiKeyCipher(StrUtil.isBlank(entity.getApiKeyCipher()) ? "" : SECRET_MASK);
    }

    /**
     * 解密密钥。
     */
    private String decryptSecret(String cipher) {
        return StrUtil.isBlank(cipher) ? "" : apiEncryptService.decrypt(cipher);
    }

    /**
     * 查询单个 Qdrant 集合的向量数量和运行状态。
     */
    private Map<String, Object> queryQdrantCollection(AiVectorDatabaseEntity database, String collectionName,
                                                       String apiKey) {
        HttpRequest request = HttpRequest.get(database.getServiceUrl() + "/collections/" + collectionName)
                .timeout(Math.max(1, database.getRequestTimeoutSeconds()) * 1000);
        if (StrUtil.isNotBlank(apiKey)) {
            request.header("api-key", apiKey);
        }
        try (HttpResponse response = request.execute()) {
            if (!response.isOk()) {
                throw new IllegalStateException("读取集合失败，HTTP " + response.getStatus());
            }
            JSONObject result = JSONUtil.parseObj(response.body()).getJSONObject("result");
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("vectorCount", result == null ? 0L : result.getLong("points_count", 0L));
            detail.put("status", result == null ? "unknown" : result.getStr("status", "unknown"));
            return detail;
        }
    }

    /**
     * 根据配置切分文档文本。
     */
    private List<String> chunkText(String rawText, AiDocumentParseConfigEntity config) {
        String text = StrUtil.trim(rawText);
        if (StrUtil.isBlank(text)) {
            return List.of();
        }
        int size = config.getChunkSize();
        int overlap = config.getChunkOverlap();
        List<String> separators = parseSeparators(config.getSeparators());
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int maxEnd = Math.min(text.length(), start + size);
            int end = "RECURSIVE".equalsIgnoreCase(config.getChunkMethod())
                    ? findChunkEnd(text, start, maxEnd, config.getMinChunkSize(), separators)
                    : maxEnd;
            String chunk = StrUtil.trim(text.substring(start, end));
            if (chunk.length() >= config.getMinChunkSize() || end == text.length()) {
                result.add(chunk);
            }
            if (end == text.length()) {
                break;
            }
            start = Math.max(start + 1, end - overlap);
        }
        return result;
    }

    /**
     * 将配置中的转义分隔符转换为实际字符。
     */
    private List<String> parseSeparators(String value) {
        if (StrUtil.isBlank(value)) {
            return List.of("\n\n", "\n", "。", "！", "？", "；");
        }
        List<String> result = new ArrayList<>();
        for (String group : StrUtil.split(value, ',')) {
            String separator = group.replace("\\r", "\r").replace("\\n", "\n").replace("\\t", "\t");
            if (separator.length() <= 2 || separator.contains("\n") || separator.contains("\r")) {
                result.add(separator);
            } else {
                separator.codePoints().mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                        .forEach(result::add);
            }
        }
        return result;
    }

    /**
     * 在目标长度内优先选择最靠后的语义分隔符。
     */
    private int findChunkEnd(String text, int start, int maxEnd, int minChunkSize, List<String> separators) {
        if (maxEnd >= text.length()) {
            return text.length();
        }
        int minimumEnd = Math.min(maxEnd, start + minChunkSize);
        int candidate = -1;
        for (String separator : separators) {
            if (StrUtil.isEmpty(separator)) {
                continue;
            }
            int index = text.lastIndexOf(separator, maxEnd - 1);
            int end = index < 0 ? -1 : index + separator.length();
            if (end >= minimumEnd && end > candidate) {
                candidate = end;
            }
        }
        return candidate > start ? candidate : maxEnd;
    }

    /**
     * 校验日志与统计查询的时间范围，防止无边界扫描。
     */
    private String validateTimeRange(LocalDateTime beginTime, LocalDateTime endTime) {
        if (beginTime == null || endTime == null) {
            return null;
        }
        if (endTime.isBefore(beginTime)) {
            return "结束时间不能早于开始时间";
        }
        return ChronoUnit.DAYS.between(beginTime, endTime) > MAX_QUERY_DAYS
                ? "单次查询时间范围不能超过 366 天" : null;
    }

    /**
     * 构造调用日志查询条件。
     */
    private LambdaQueryWrapper<AiCallLogEntity> callLogWrapper(AiPlatformForm.CallLogQuery form) {
        return new LambdaQueryWrapper<AiCallLogEntity>()
                .ge(form.getBeginTime() != null, AiCallLogEntity::getCreateTime, form.getBeginTime())
                .le(form.getEndTime() != null, AiCallLogEntity::getCreateTime, form.getEndTime())
                .like(StrUtil.isNotBlank(form.getUserName()), AiCallLogEntity::getUserName, form.getUserName())
                .eq(form.getModelId() != null, AiCallLogEntity::getModelId, form.getModelId())
                .eq(StrUtil.isNotBlank(form.getCallType()), AiCallLogEntity::getCallType, form.getCallType())
                .eq(StrUtil.isNotBlank(form.getSourceType()), AiCallLogEntity::getSourceType, form.getSourceType())
                .eq(form.getSuccessFlag() != null, AiCallLogEntity::getSuccessFlag, form.getSuccessFlag())
                .like(StrUtil.isNotBlank(form.getTraceId()), AiCallLogEntity::getTraceId, form.getTraceId());
    }

    /**
     * 构造用量统计查询条件。
     */
    private QueryWrapper<AiCallLogEntity> statisticsWrapper(AiPlatformForm.StatisticsQuery form) {
        return new QueryWrapper<AiCallLogEntity>()
                .ge("create_time", form.getBeginTime())
                .le("create_time", form.getEndTime())
                .eq(form.getServiceId() != null, "service_id", form.getServiceId())
                .eq(form.getModelId() != null, "model_id", form.getModelId())
                .eq(StrUtil.isNotBlank(form.getCallType()), "call_type", form.getCallType());
    }

    /**
     * 按服务商或用户生成用量排行。
     */
    private List<Map<String, Object>> ranking(List<AiCallLogEntity> logs, boolean provider) {
        return logs.stream().collect(Collectors.groupingBy(
                        item -> provider ? StrUtil.blankToDefault(item.getProviderName(), "未知服务商")
                                : StrUtil.blankToDefault(item.getUserName(), "系统任务")))
                .entrySet().stream()
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", entry.getKey());
                    row.put("calls", entry.getValue().size());
                    row.put("tokens", entry.getValue().stream().map(AiCallLogEntity::getTotalTokens)
                            .filter(Objects::nonNull).mapToLong(Integer::longValue).sum());
                    row.put("successRate", entry.getValue().isEmpty() ? 0
                            : entry.getValue().stream().filter(item -> Boolean.TRUE.equals(item.getSuccessFlag())).count()
                            * 100D / entry.getValue().size());
                    return row;
                })
                .sorted((left, right) -> Long.compare(((Number) right.get("calls")).longValue(),
                        ((Number) left.get("calls")).longValue()))
                .limit(10)
                .toList();
    }

    /**
     * 对 CSV 字段进行转义。
     */
    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        String leading = text.stripLeading();
        if (!leading.isEmpty() && "=+-@".indexOf(leading.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
