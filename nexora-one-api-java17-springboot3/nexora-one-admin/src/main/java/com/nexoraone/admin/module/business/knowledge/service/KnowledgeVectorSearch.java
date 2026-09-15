package com.nexoraone.admin.module.business.knowledge.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nexoraone.admin.module.business.ai.dao.AiKnowledgeBaseDao;
import com.nexoraone.admin.module.business.ai.dao.AiModelDao;
import com.nexoraone.admin.module.business.ai.dao.AiModelServiceDao;
import com.nexoraone.admin.module.business.ai.dao.AiVectorDatabaseDao;
import com.nexoraone.admin.module.business.ai.domain.entity.*;
import com.nexoraone.base.module.support.apiencrypt.service.ApiEncryptService;
import com.nexoraone.admin.module.business.knowledge.domain.KnowledgeDocument;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.*;

/** 复用平台已有的模型、集合与任务点，按用户文档任务限制 Qdrant 检索。 */
@Service
public class KnowledgeVectorSearch {
    @Resource private AiModelDao models;
    @Resource private AiModelServiceDao providers;
    @Resource private AiVectorDatabaseDao vectors;
    @Resource private AiKnowledgeBaseDao ingestBases;
    @Resource private ApiEncryptService encryption;

    /** 对不同入库配置分别生成查询向量，再合并、排序、截取真实命中。 */
    public List<Map<String, Object>> search(Collection<KnowledgeDocument> documents, String question,
                                            int topK, double threshold) {
        Map<Long, List<KnowledgeDocument>> groups = new LinkedHashMap<>();
        documents.forEach(document -> groups.computeIfAbsent(document.getIngestBaseId(), ignored -> new ArrayList<>())
                .add(document));
        List<Map<String, Object>> hits = new ArrayList<>();
        for (Map.Entry<Long, List<KnowledgeDocument>> entry : groups.entrySet()) {
            AiKnowledgeBaseEntity base = ingestBases.selectById(entry.getKey());
            if (base == null) throw new IllegalStateException("文档入库集合已不存在");
            AiVectorDatabaseEntity vector = vectors.selectById(base.getVectorDatabaseId());
            AiModelEntity model = models.selectById(base.getEmbeddingModelId());
            AiModelServiceEntity provider = model == null ? null : providers.selectById(model.getServiceId());
            if (vector == null || model == null || provider == null
                    || !Boolean.TRUE.equals(vector.getEnabledFlag()) || !Boolean.TRUE.equals(model.getEnabledFlag())
                    || !Boolean.TRUE.equals(provider.getEnabledFlag()))
                throw new IllegalStateException("文档对应的向量检索配置不可用");
            JSONArray embedding = embedding(model, provider, question);
            if (model.getEmbeddingDimension() != null && embedding.size() != model.getEmbeddingDimension())
                throw new IllegalStateException("查询向量维度与入库模型不一致");
            List<Long> taskIds = entry.getValue().stream().map(KnowledgeDocument::getTaskId).toList();
            Map<Long, KnowledgeDocument> byTask = new HashMap<>();
            entry.getValue().forEach(document -> byTask.put(document.getTaskId(), document));
            JSONObject filter = JSONUtil.createObj().set("must", JSONUtil.parseArray(List.of(
                    JSONUtil.createObj().set("key", "taskId")
                            .set("match", JSONUtil.createObj().set("any", taskIds)))));
            JSONObject body = JSONUtil.createObj().set("vector", embedding).set("limit", topK)
                    .set("with_payload", true).set("score_threshold", threshold).set("filter", filter);
            String url = StrUtil.removeSuffix(vector.getServiceUrl(), "/") + "/collections/"
                    + base.getCollectionName() + "/points/search";
            try (HttpResponse response = vectorRequest(HttpRequest.post(url).body(body.toString()), vector).execute()) {
                ensureOk(response);
                JSONArray result = JSONUtil.parseObj(response.body()).getJSONArray("result");
                if (result == null) throw new IllegalStateException("Qdrant 未返回检索结果");
                for (int i = 0; i < result.size(); i++) {
                    JSONObject point = result.getJSONObject(i);
                    JSONObject payload = point.getJSONObject("payload");
                    if (payload == null) continue;
                    KnowledgeDocument document = byTask.get(payload.getLong("taskId"));
                    if (document == null || StrUtil.isBlank(payload.getStr("text"))) continue;
                    Map<String, Object> hit = new LinkedHashMap<>();
                    hit.put("pointId", point.getStr("id"));
                    hit.put("documentId", document.getDocumentId());
                    hit.put("fileName", document.getFileName());
                    hit.put("chunkIndex", payload.getInt("chunkIndex"));
                    hit.put("text", payload.getStr("text"));
                    hit.put("score", point.getDouble("score"));
                    hits.add(hit);
                }
            }
        }
        hits.sort(Comparator.comparingDouble(hit -> -((Number) hit.get("score")).doubleValue()));
        Set<String> seen = new HashSet<>();
        return hits.stream().filter(hit -> seen.add(hit.get("documentId") + ":" + hit.get("chunkIndex")))
                .limit(topK).toList();
    }

    /** 删除独立文档的实际点，不影响同一源文档在其他知识库中的关联。 */
    public void delete(KnowledgeDocument document) {
        AiKnowledgeBaseEntity base = ingestBases.selectById(document.getIngestBaseId());
        if (base == null) return;
        AiVectorDatabaseEntity vector = vectors.selectById(base.getVectorDatabaseId());
        if (vector == null) throw new IllegalStateException("原向量实例已不存在，无法清理文档");
        JSONObject filter = JSONUtil.createObj().set("must", JSONUtil.parseArray(List.of(
                JSONUtil.createObj().set("key", "taskId")
                        .set("match", JSONUtil.createObj().set("value", document.getTaskId())))));
        String url = StrUtil.removeSuffix(vector.getServiceUrl(), "/") + "/collections/"
                + base.getCollectionName() + "/points/delete?wait=true";
        try (HttpResponse response = vectorRequest(HttpRequest.post(url)
                .body(JSONUtil.createObj().set("filter", filter).toString()), vector).execute()) {
            if (response.getStatus() != 404) ensureOk(response);
        }
    }

    /** 按平台 OpenAI 兼容协议获取完整查询 embedding。 */
    private JSONArray embedding(AiModelEntity model, AiModelServiceEntity provider, String question) {
        String url = StrUtil.removeSuffix(provider.getBaseUrl(), "/");
        url = (url.endsWith("/v1") ? url : url + "/v1") + "/embeddings";
        HttpRequest request = HttpRequest.post(url).header("Content-Type", "application/json")
                .body(JSONUtil.createObj().set("model", model.getModelCode()).set("input", question).toString())
                .timeout(Math.max(1, provider.getRequestTimeoutSeconds()) * 1000);
        if (StrUtil.isNotBlank(provider.getApiKeyCipher()))
            request.bearerAuth(encryption.decrypt(provider.getApiKeyCipher()));
        if (StrUtil.isNotBlank(provider.getOrganizationId()))
            request.header("OpenAI-Organization", provider.getOrganizationId());
        try (HttpResponse response = request.execute()) {
            ensureOk(response);
            JSONArray data = JSONUtil.parseObj(response.body()).getJSONArray("data");
            if (data == null || data.isEmpty()) throw new IllegalStateException("向量模型未返回 embedding");
            return data.getJSONObject(0).getJSONArray("embedding");
        }
    }

    /** 统一处理 Qdrant 授权、超时及 HTTP 错误。 */
    private HttpRequest vectorRequest(HttpRequest request, AiVectorDatabaseEntity vector) {
        request.header("Content-Type", "application/json")
                .timeout(Math.max(1, vector.getRequestTimeoutSeconds()) * 1000);
        if (StrUtil.isNotBlank(vector.getApiKeyCipher()))
            request.header("api-key", encryption.decrypt(vector.getApiKeyCipher()));
        return request;
    }

    /** 外部调用失败时终止当前业务操作，不伪装成成功。 */
    private void ensureOk(HttpResponse response) {
        if (!response.isOk())
            throw new IllegalStateException("外部服务 HTTP " + response.getStatus() + "：" + StrUtil.maxLength(response.body(), 300));
    }
}
