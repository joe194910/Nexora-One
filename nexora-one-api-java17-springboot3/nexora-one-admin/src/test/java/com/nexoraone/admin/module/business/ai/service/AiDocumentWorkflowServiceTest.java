package com.nexoraone.admin.module.business.ai.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.nexoraone.admin.module.business.ai.dao.AiDocumentParseConfigDao;
import com.nexoraone.admin.module.business.ai.dao.AiKnowledgeBaseDao;
import com.nexoraone.admin.module.business.ai.dao.AiModelDao;
import com.nexoraone.admin.module.business.ai.dao.AiModelServiceDao;
import com.nexoraone.admin.module.business.ai.dao.AiParseServiceDao;
import com.nexoraone.admin.module.business.ai.dao.AiParseStepDao;
import com.nexoraone.admin.module.business.ai.dao.AiParseTaskDao;
import com.nexoraone.admin.module.business.ai.dao.AiVectorDatabaseDao;
import com.nexoraone.admin.module.business.ai.domain.entity.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 文档入队、解析调用和向量清理的业务回归测试。 */
@ExtendWith(MockitoExtension.class)
class AiDocumentWorkflowServiceTest {
    @Mock private AiDocumentParseConfigDao planDao;
    @Mock private AiParseServiceDao serviceDao;
    @Mock private AiKnowledgeBaseDao baseDao;
    @Mock private AiParseTaskDao taskDao;
    @Mock private AiParseStepDao stepDao;
    @Mock private AiVectorDatabaseDao vectorDao;
    @Mock private AiModelDao modelDao;
    @Mock private AiModelServiceDao modelServiceDao;
    @Mock private AiDocumentObjectStorage storage;
    @Mock private ObjectProvider<AiParseTaskFileLocator> taskFileLocators;
    @Mock private AiParseTaskFileLocator taskFileLocator;
    @InjectMocks private AiDocumentWorkflowService workflow;

    /** 文件上传必须写入对象存储，同时创建可追踪的任务和阶段结果。 */
    @Test
    void uploadPersistsFileAndCreatesTask() throws Exception {
        AiKnowledgeBaseEntity base = new AiKnowledgeBaseEntity();
        base.setKnowledgeBaseId(10L);
        base.setParsePlanId(20L);
        AiDocumentParseConfigEntity plan = new AiDocumentParseConfigEntity();
        plan.setConfigId(20L);
        plan.setEnabledFlag(true);
        plan.setSupportedFormats("txt");
        plan.setDuplicateStrategy("SKIP");
        when(baseDao.selectById(10L)).thenReturn(base);
        when(planDao.selectById(20L)).thenReturn(plan);
        when(taskDao.selectCount(any())).thenReturn(0L);
        doAnswer(call -> {
            AiParseTaskEntity task = call.getArgument(0);
            task.setTaskId(30L);
            return 1;
        }).when(taskDao).insert(any(AiParseTaskEntity.class));
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain",
                "可检索的文档正文".getBytes(StandardCharsets.UTF_8));

        var result = workflow.upload(10L, file);

        assertNotNull(result.getData());
        assertEquals(30L, result.getData().getTaskId());
        assertEquals("QUEUED", result.getData().getStatus());
        assertTrue(result.getData().getFilePath().startsWith("documents/10/"));
        ArgumentCaptor<byte[]> storedBytes = ArgumentCaptor.forClass(byte[].class);
        verify(storage).put(eq(result.getData().getFilePath()), storedBytes.capture(), eq("text/plain"));
        assertArrayEquals(file.getBytes(), storedBytes.getValue());
        ArgumentCaptor<AiParseStepEntity> steps = ArgumentCaptor.forClass(AiParseStepEntity.class);
        verify(stepDao).insert(steps.capture());
        assertEquals("UPLOAD", steps.getValue().getStage());
        assertEquals("SUCCESS", steps.getValue().getStatus());
    }

    /** 历史任务的本地路径读不到时，回退到关联文档的对象键并回填任务表。 */
    @Test
    void legacyLocalPathFallsBackToObjectKey() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), AiParseTaskEntity.class);
        AiParseTaskEntity task = new AiParseTaskEntity();
        task.setTaskId(42L);
        task.setFilePath("D:\\old\\work\\sample.txt");
        when(taskFileLocators.getIfAvailable()).thenReturn(taskFileLocator);
        when(taskFileLocator.locateObjectKey(42L)).thenReturn("users/7/sample.txt");
        when(storage.get("users/7/sample.txt")).thenReturn("历史正文".getBytes(StandardCharsets.UTF_8));

        byte[] bytes = ReflectionTestUtils.invokeMethod(workflow, "readTaskBytes", task);

        assertArrayEquals("历史正文".getBytes(StandardCharsets.UTF_8), bytes);
        verify(storage, never()).get("D:\\old\\work\\sample.txt");
        verify(taskDao).update(isNull(), any());
    }

    /** 历史任务既无对象键、也无法按文档定位时，给出可重传的明确错误。 */
    @Test
    void legacyTaskWithoutLocatableObjectFails() {
        AiParseTaskEntity task = new AiParseTaskEntity();
        task.setTaskId(43L);
        task.setFilePath("/var/lib/legacy/sample.txt");
        when(taskFileLocators.getIfAvailable()).thenReturn(taskFileLocator);
        when(taskFileLocator.locateObjectKey(43L)).thenReturn(null);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(workflow, "readTaskBytes", task));

        assertTrue(error.getMessage().contains("重新上传"));
    }

    /** HTTP 服务采用 multipart /parse 合约并真实读取返回的正文。 */
    @Test
    void httpParseSendsActualMultipartFile() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicBoolean received = new AtomicBoolean();
        server.createContext("/parse", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
            received.set(body.contains("filename=\"sample.txt\"") && body.contains("name=\"file\""));
            byte[] response = "{\"text\":\"真实解析正文\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            AiParseServiceEntity service = new AiParseServiceEntity();
            service.setDeploymentType("HTTP");
            service.setEndpoint("http://127.0.0.1:" + server.getAddress().getPort());
            service.setSupportedFormats("txt");
            service.setTimeoutSeconds(5);
            assertEquals("真实解析正文", ReflectionTestUtils.invokeMethod(workflow, "extract", service,
                    "原始文件".getBytes(StandardCharsets.UTF_8), "sample.txt"));
            assertTrue(received.get());
        } finally {
            server.stop(0);
        }
    }

    /** 失败清理必须仅删除当前任务 ID 的点，不能按文件哈希误删成功版本。 */
    @Test
    void cleanupTargetsOnlyCurrentTask() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> filter = new AtomicReference<>();
        server.createContext("/collections/test/points/delete", exchange -> {
            filter.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"result\":{\"status\":\"completed\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            AiKnowledgeBaseEntity base = new AiKnowledgeBaseEntity();
            base.setVectorDatabaseId(1L);
            base.setCollectionName("test");
            AiParseTaskEntity task = new AiParseTaskEntity();
            task.setTaskId(42L);
            task.setFileHash("old-hash");
            AiVectorDatabaseEntity database = new AiVectorDatabaseEntity();
            database.setServiceUrl("http://127.0.0.1:" + server.getAddress().getPort());
            database.setRequestTimeoutSeconds(5);
            when(vectorDao.selectById(1L)).thenReturn(database);
            ReflectionTestUtils.invokeMethod(workflow, "clearPoints", base, task);
            var criteria = JSONUtil.parseObj(filter.get()).getJSONObject("filter").getJSONArray("must");
            assertEquals("taskId", criteria.getJSONObject(0).getStr("key"));
            assertEquals(42L, criteria.getJSONObject(0).getJSONObject("match").getLong("value"));
        } finally {
            server.stop(0);
        }
    }

    /** 用模拟的真实 HTTP 接口验证向量模型调用、集合创建和点写入闭环。 */
    @Test
    void indexCreatesCollectionAndWritesEmbeddingPoint() throws Exception {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), AiParseTaskEntity.class);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicBoolean embedded = new AtomicBoolean();
        AtomicBoolean inserted = new AtomicBoolean();
        server.createContext("/v1/embeddings", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            embedded.set(body.contains("可检索切片") && body.contains("embedding-model"));
            respond(exchange, 200, "{\"data\":[{\"embedding\":[0.2,0.8]}]}");
        });
        server.createContext("/collections/test", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) respond(exchange, 404, "{}");
            else respond(exchange, 200, "{\"result\":true}");
        });
        server.createContext("/collections/test/points/delete", exchange -> respond(exchange, 200, "{\"result\":true}"));
        server.createContext("/collections/test/points", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            inserted.set(body.contains("可检索切片") && body.contains("\"taskId\":42")
                    && body.contains("\"vector\":[0.2,0.8]"));
            respond(exchange, 200, "{\"result\":{\"status\":\"completed\"}}");
        });
        server.start();
        try {
            String address = "http://127.0.0.1:" + server.getAddress().getPort();
            AiKnowledgeBaseEntity base = new AiKnowledgeBaseEntity();
            base.setKnowledgeBaseId(10L);
            base.setEmbeddingModelId(1L);
            base.setVectorDatabaseId(2L);
            base.setCollectionName("test");
            AiParseTaskEntity task = new AiParseTaskEntity();
            task.setTaskId(42L);
            task.setFileHash("hash");
            task.setFileName("sample.txt");
            task.setStatus("RUNNING");
            AiModelEntity model = new AiModelEntity();
            model.setServiceId(3L);
            model.setModelCode("embedding-model");
            model.setEmbeddingDimension(2);
            model.setEnabledFlag(true);
            AiModelServiceEntity provider = new AiModelServiceEntity();
            provider.setEnabledFlag(true);
            provider.setBaseUrl(address);
            provider.setRequestTimeoutSeconds(5);
            AiVectorDatabaseEntity database = new AiVectorDatabaseEntity();
            database.setEnabledFlag(true);
            database.setServiceUrl(address);
            database.setRequestTimeoutSeconds(5);
            when(modelDao.selectById(1L)).thenReturn(model);
            when(modelServiceDao.selectById(3L)).thenReturn(provider);
            when(vectorDao.selectById(2L)).thenReturn(database);
            when(taskDao.selectById(42L)).thenReturn(task);

            Integer count = ReflectionTestUtils.invokeMethod(workflow, "index", base, task,
                    List.of("可检索切片"), System.currentTimeMillis() + 60_000L);

            assertEquals(1, count);
            assertTrue(embedded.get());
            assertTrue(inserted.get());
        } finally {
            server.stop(0);
        }
    }

    /** 返回测试用 HTTP 响应，并及时关闭请求连接。 */
    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    /** 固定切片应保留尾片，不因边界舍弃文本。 */
    @Test
    void fixedChunksKeepRemainder() {
        AiDocumentParseConfigEntity plan = new AiDocumentParseConfigEntity();
        plan.setChunkMethod("FIXED");
        plan.setChunkSize(5);
        plan.setChunkOverlap(0);
        plan.setMinChunkSize(1);
        List<String> chunks = ReflectionTestUtils.invokeMethod(workflow, "chunk", "abcdefghijkl", plan);
        assertEquals(List.of("abcde", "fghij", "kl"), chunks);
    }
}
