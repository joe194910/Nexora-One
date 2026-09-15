package com.nexoraone.admin.module.business.ai.service;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nexoraone.admin.module.business.ai.dao.AiCallLogDao;
import com.nexoraone.admin.module.business.ai.dao.AiModelDao;
import com.nexoraone.admin.module.business.ai.dao.AiModelServiceDao;
import com.nexoraone.admin.module.business.ai.domain.entity.AiCallLogEntity;
import com.nexoraone.admin.module.business.ai.domain.entity.AiModelEntity;
import com.nexoraone.admin.module.business.ai.domain.entity.AiModelServiceEntity;
import com.nexoraone.admin.module.business.ai.domain.form.AiPlatformForm;
import com.nexoraone.admin.util.AdminRequestUtil;
import com.nexoraone.base.common.domain.RequestUser;
import com.nexoraone.base.common.domain.ResponseDTO;
import com.nexoraone.base.module.support.apiencrypt.service.ApiEncryptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 模型运行时服务。
 * <p>
 * 知识库、智能助手及 MCP 工具应统一通过该服务调用模型，确保日志、费用和异常口径一致。
 */
@Slf4j
@Service
public class AiRuntimeService {

    private static final int SUMMARY_LENGTH = 1000;

    @Resource
    private AiModelDao modelDao;
    @Resource
    private AiModelServiceDao modelServiceDao;
    @Resource
    private AiCallLogDao callLogDao;
    @Resource
    private ApiEncryptService apiEncryptService;

    /**
     * 测试模型服务是否可正常访问。
     *
     * @param service 模型服务配置
     * @return 测试结果
     */
    public Map<String, Object> testModelService(AiModelServiceEntity service) {
        long begin = System.currentTimeMillis();
        HttpResponse response = createGet(service, openAiUrl(service.getBaseUrl(), "/models")).execute();
        try {
            ensureSuccess(response);
            JSONObject body = JSONUtil.parseObj(response.body());
            JSONArray models = body.getJSONArray("data");
            if (models == null) {
                throw new IllegalStateException("模型服务响应缺少 data 模型列表");
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("durationMs", System.currentTimeMillis() - begin);
            result.put("modelCount", models.size());
            result.put("message", "连接成功");
            return result;
        } finally {
            response.close();
        }
    }

    /**
     * 根据模型类型执行真实调试调用并记录审计日志。
     *
     * @param form 调试参数
     * @return 模型响应信息
     */
    public ResponseDTO<Map<String, Object>> debug(AiPlatformForm.ModelDebug form) {
        AiModelEntity model = modelDao.selectById(form.getModelId());
        if (model == null || !Boolean.TRUE.equals(model.getEnabledFlag())) {
            return ResponseDTO.userErrorParam("模型不存在或未启用");
        }
        AiModelServiceEntity service = modelServiceDao.selectById(model.getServiceId());
        if (service == null || !Boolean.TRUE.equals(service.getEnabledFlag())) {
            return ResponseDTO.userErrorParam("所属模型服务不存在或未启用");
        }
        if ("RERANK".equalsIgnoreCase(model.getModelType())) {
            return ResponseDTO.userErrorParam("重排序模型接口尚未形成统一协议，请通过兼容网关转换为平台支持的接口");
        }

        String traceId = "ai_" + UUID.fastUUID().toString(true);
        long begin = System.currentTimeMillis();
        AiCallLogEntity callLog = createCallLog(traceId, model, service, form);
        try {
            Map<String, Object> result = "EMBEDDING".equalsIgnoreCase(model.getModelType())
                    ? invokeEmbedding(model, service, form.getPrompt())
                    : invokeChat(model, service, form.getPrompt(), form.getSystemPrompt());
            fillSuccessLog(callLog, model, result, begin);
            result.put("traceId", traceId);
            return ResponseDTO.ok(result);
        } catch (Exception exception) {
            fillFailureLog(callLog, exception, begin);
            log.warn("AI 模型调用失败，traceId={}", traceId, exception);
            return ResponseDTO.userErrorParam("模型调用失败：" + StrUtil.maxLength(exception.getMessage(), 300));
        } finally {
            callLogDao.insert(callLog);
        }
    }

    /**
     * 调用 OpenAI 兼容的对话接口。
     */
    /** 为用户助手执行多轮对话，沿用平台模型服务的鉴权、用量和审计口径。 */
    public Map<String, Object> assistantChat(Long modelId, JSONArray history, String systemPrompt,
                                              String question, Long assistantId) {
        AiModelEntity model = modelDao.selectById(modelId);
        if (model == null || !"CHAT".equalsIgnoreCase(model.getModelType()) || !Boolean.TRUE.equals(model.getEnabledFlag()))
            throw new IllegalArgumentException("请选择已启用的对话模型");
        AiModelServiceEntity service = modelServiceDao.selectById(model.getServiceId());
        if (service == null || !Boolean.TRUE.equals(service.getEnabledFlag()))
            throw new IllegalStateException("对话模型所属服务不可用");
        AiPlatformForm.ModelDebug source = new AiPlatformForm.ModelDebug();
        source.setPrompt(question);
        source.setSourceName("智能助手");
        AiCallLogEntity callLog = createCallLog("kb_" + UUID.fastUUID().toString(true), model, service, source);
        callLog.setSourceType("ASSISTANT");
        callLog.setAssistantId(assistantId);
        long start = System.currentTimeMillis();
        try {
            JSONArray messages = new JSONArray();
            if (StrUtil.isNotBlank(systemPrompt))
                messages.add(JSONUtil.createObj().set("role", "system").set("content", systemPrompt));
            messages.addAll(history);
            messages.add(JSONUtil.createObj().set("role", "user").set("content", question));
            Map<String, Object> result = sendChat(model, service, messages);
            fillSuccessLog(callLog, model, result, start);
            result.put("traceId", callLog.getTraceId());
            return result;
        } catch (Exception exception) {
            fillFailureLog(callLog, exception, start);
            throw exception;
        } finally {
            callLogDao.insert(callLog);
        }
    }

    private Map<String, Object> invokeChat(AiModelEntity model, AiModelServiceEntity service,
                                           String prompt, String systemPrompt) {
        JSONArray messages = new JSONArray();
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(JSONUtil.createObj().set("role", "system").set("content", systemPrompt));
        }
        messages.add(JSONUtil.createObj().set("role", "user").set("content", prompt));
        return sendChat(model, service, messages);
    }

    /** 发送已构造的消息数组，避免助手与调试走两套响应解析。 */
    private Map<String, Object> sendChat(AiModelEntity model, AiModelServiceEntity service, JSONArray messages) {
        JSONObject requestBody = JSONUtil.createObj()
                .set("model", model.getModelCode())
                .set("messages", messages)
                .set("stream", false);
        if (model.getTemperature() != null) {
            requestBody.set("temperature", model.getTemperature());
        }
        if (model.getMaxOutputTokens() != null) {
            requestBody.set("max_tokens", model.getMaxOutputTokens());
        }

        HttpResponse response = createPost(service, openAiUrl(service.getBaseUrl(), "/chat/completions"), requestBody).execute();
        try {
            ensureSuccess(response);
            JSONObject body = JSONUtil.parseObj(response.body());
            JSONArray choices = body.getJSONArray("choices");
            if (choices == null || choices.isEmpty() || choices.getJSONObject(0) == null
                    || choices.getJSONObject(0).getJSONObject("message") == null) {
                throw new IllegalStateException("模型响应缺少 choices.message");
            }
            String content = choices.getJSONObject(0).getJSONObject("message").getStr("content", "");
            Map<String, Object> result = usageResult(body.getJSONObject("usage"));
            result.put("content", content);
            result.put("model", body.getStr("model", model.getModelCode()));
            return result;
        } finally {
            response.close();
        }
    }

    /**
     * 调用 OpenAI 兼容的向量化接口。
     */
    private Map<String, Object> invokeEmbedding(AiModelEntity model, AiModelServiceEntity service, String input) {
        JSONObject requestBody = JSONUtil.createObj()
                .set("model", model.getModelCode())
                .set("input", input);
        HttpResponse response = createPost(service, openAiUrl(service.getBaseUrl(), "/embeddings"), requestBody).execute();
        try {
            ensureSuccess(response);
            JSONObject body = JSONUtil.parseObj(response.body());
            JSONArray data = body.getJSONArray("data");
            JSONArray embedding = data == null || data.isEmpty()
                    ? new JSONArray()
                    : data.getJSONObject(0).getJSONArray("embedding");
            if (embedding == null || embedding.isEmpty()) {
                throw new IllegalStateException("向量模型未返回 embedding 数组");
            }
            Map<String, Object> result = usageResult(body.getJSONObject("usage"));
            result.put("dimension", embedding.size());
            result.put("vectorPreview", embedding.subList(0, Math.min(8, embedding.size())));
            result.put("content", "向量化成功，共 " + result.get("dimension") + " 维");
            result.put("model", body.getStr("model", model.getModelCode()));
            return result;
        } finally {
            response.close();
        }
    }

    /**
     * 创建携带鉴权信息的 GET 请求。
     */
    private HttpRequest createGet(AiModelServiceEntity service, String url) {
        return applyHeaders(HttpRequest.get(url), service);
    }

    /**
     * 创建携带鉴权信息的 POST 请求。
     */
    private HttpRequest createPost(AiModelServiceEntity service, String url, JSONObject body) {
        return applyHeaders(HttpRequest.post(url).body(body.toString()), service)
                .header("Content-Type", "application/json");
    }

    /**
     * 设置服务商鉴权头与超时时间。
     */
    private HttpRequest applyHeaders(HttpRequest request, AiModelServiceEntity service) {
        String apiKey = decryptSecret(service.getApiKeyCipher());
        if (StrUtil.isNotBlank(apiKey)) {
            request.bearerAuth(apiKey);
        }
        if (StrUtil.isNotBlank(service.getOrganizationId())) {
            request.header("OpenAI-Organization", service.getOrganizationId());
        }
        int timeoutMs = Math.max(1, service.getRequestTimeoutSeconds() == null ? 60 : service.getRequestTimeoutSeconds()) * 1000;
        return request.timeout(timeoutMs);
    }

    /**
     * 校验 HTTP 响应状态。
     */
    private void ensureSuccess(HttpResponse response) {
        if (!response.isOk()) {
            throw new IllegalStateException("HTTP " + response.getStatus() + "，" + StrUtil.maxLength(response.body(), 500));
        }
    }

    /**
     * 生成 OpenAI 兼容接口地址。
     */
    private String openAiUrl(String baseUrl, String path) {
        String normalized = StrUtil.removeSuffix(StrUtil.trim(baseUrl), "/");
        return normalized.endsWith("/v1") ? normalized + path : normalized + "/v1" + path;
    }

    /**
     * 解密服务密钥。
     */
    private String decryptSecret(String cipher) {
        return StrUtil.isBlank(cipher) ? "" : apiEncryptService.decrypt(cipher);
    }

    /**
     * 提取响应中的 Token 用量。
     */
    private Map<String, Object> usageResult(JSONObject usage) {
        Map<String, Object> result = new LinkedHashMap<>();
        int input = usage == null ? 0 : usage.getInt("prompt_tokens", 0);
        int output = usage == null ? 0 : usage.getInt("completion_tokens", 0);
        result.put("inputTokens", input);
        result.put("outputTokens", output);
        result.put("totalTokens", usage == null ? input + output : usage.getInt("total_tokens", input + output));
        return result;
    }

    /**
     * 创建调用日志基础信息。
     */
    private AiCallLogEntity createCallLog(String traceId, AiModelEntity model, AiModelServiceEntity service,
                                           AiPlatformForm.ModelDebug form) {
        RequestUser user = AdminRequestUtil.getRequestUser();
        AiCallLogEntity logEntity = new AiCallLogEntity();
        logEntity.setTraceId(traceId);
        logEntity.setUserId(user == null ? null : user.getUserId());
        logEntity.setUserName(user == null ? "系统任务" : user.getUserName());
        logEntity.setUserIp(user == null ? null : user.getIp());
        logEntity.setServiceId(service.getServiceId());
        logEntity.setProviderName(service.getServiceName());
        logEntity.setModelId(model.getModelId());
        logEntity.setModelCode(model.getModelCode());
        logEntity.setCallType(model.getModelType());
        logEntity.setSourceType("DEBUG");
        logEntity.setSourceName(StrUtil.blankToDefault(form.getSourceName(), "模型调试"));
        logEntity.setRequestSummary(StrUtil.maxLength(form.getPrompt(), SUMMARY_LENGTH));
        logEntity.setCreateTime(LocalDateTime.now());
        return logEntity;
    }

    /**
     * 填充成功调用日志。
     */
    private void fillSuccessLog(AiCallLogEntity callLog, AiModelEntity model, Map<String, Object> result, long begin) {
        callLog.setInputTokens(NumberUtil.parseInt(String.valueOf(result.getOrDefault("inputTokens", 0))));
        callLog.setOutputTokens(NumberUtil.parseInt(String.valueOf(result.getOrDefault("outputTokens", 0))));
        callLog.setTotalTokens(NumberUtil.parseInt(String.valueOf(result.getOrDefault("totalTokens", 0))));
        callLog.setTotalDurationMs(System.currentTimeMillis() - begin);
        callLog.setModelDurationMs(callLog.getTotalDurationMs());
        callLog.setSuccessFlag(true);
        callLog.setResponseSummary(StrUtil.maxLength(String.valueOf(result.getOrDefault("content", "")), SUMMARY_LENGTH));
        callLog.setEstimatedCost(calculateCost(model, callLog.getInputTokens(), callLog.getOutputTokens()));
    }

    /**
     * 填充失败调用日志。
     */
    private void fillFailureLog(AiCallLogEntity callLog, Exception exception, long begin) {
        callLog.setInputTokens(0);
        callLog.setOutputTokens(0);
        callLog.setTotalTokens(0);
        callLog.setTotalDurationMs(System.currentTimeMillis() - begin);
        callLog.setModelDurationMs(callLog.getTotalDurationMs());
        callLog.setSuccessFlag(false);
        callLog.setErrorCode(exception.getClass().getSimpleName());
        callLog.setErrorMessage(StrUtil.maxLength(exception.getMessage(), 1000));
        callLog.setEstimatedCost(BigDecimal.ZERO);
    }

    /**
     * 根据模型单价计算预估费用。
     */
    private BigDecimal calculateCost(AiModelEntity model, int inputTokens, int outputTokens) {
        BigDecimal inputPrice = model.getInputPrice() == null ? BigDecimal.ZERO : model.getInputPrice();
        BigDecimal outputPrice = model.getOutputPrice() == null ? BigDecimal.ZERO : model.getOutputPrice();
        return inputPrice.multiply(BigDecimal.valueOf(inputTokens))
                .add(outputPrice.multiply(BigDecimal.valueOf(outputTokens)))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }
}
