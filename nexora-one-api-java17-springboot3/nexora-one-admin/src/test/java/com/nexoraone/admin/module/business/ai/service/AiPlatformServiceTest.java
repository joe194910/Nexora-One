package com.nexoraone.admin.module.business.ai.service;

import com.nexoraone.admin.module.business.ai.domain.entity.AiDocumentParseConfigEntity;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 平台切片规则单元测试。
 */
class AiPlatformServiceTest {

    private final AiPlatformService service = new AiPlatformService();

    /**
     * 递归切片应优先落在标点分隔符处，并保留跨切片重叠。
     */
    @Test
    void recursiveChunksPreferSentenceBoundary() throws Exception {
        AiDocumentParseConfigEntity config = config("RECURSIVE", 12, 2, 3);
        List<String> chunks = chunks("第一段文字。第二段文字。第三段文字。", config);
        assertTrue(chunks.size() >= 2);
        assertTrue(chunks.get(0).endsWith("。"));
        assertEquals(chunks.get(0).substring(chunks.get(0).length() - 2), chunks.get(1).substring(0, 2));
    }

    /**
     * 固定切片应使用精确长度，不受语义分隔符影响。
     */
    @Test
    void fixedChunksUseConfiguredLength() throws Exception {
        AiDocumentParseConfigEntity config = config("FIXED", 5, 0, 1);
        assertEquals(List.of("abcde", "fghij", "kl"), chunks("abcdefghijkl", config));
    }

    /**
     * OCR 接口应收到 multipart 文件并将返回的文本交给后续切片。
     */
    @Test
    void ocrReadsTextFromMultipartEndpoint() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicBoolean receivedFile = new AtomicBoolean();
        server.createContext("/ocr", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
            receivedFile.set(body.contains("filename=\"sample.png\"") && body.contains("name=\"file\""));
            byte[] response = "{\"text\":\"识别后的正文\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            AiDocumentParseConfigEntity config = config("FIXED", 100, 0, 1);
            config.setOcrService("http://127.0.0.1:" + server.getAddress().getPort() + "/ocr");
            config.setTimeoutMinutes(1);
            config.setRetryCount(0);
            MockMultipartFile file = new MockMultipartFile("file", "sample.png", "image/png", new byte[]{1, 2, 3});
            Method method = AiPlatformService.class.getDeclaredMethod(
                    "recognizeOcr", AiDocumentParseConfigEntity.class, org.springframework.web.multipart.MultipartFile.class);
            method.setAccessible(true);
            assertEquals("识别后的正文", method.invoke(service, config, file));
            assertTrue(receivedFile.get());
        } finally {
            server.stop(0);
        }
    }

    /**
     * 导出的服务商或模型字段不能成为电子表格公式。
     */
    @Test
    void csvEscapesFormulaCells() throws Exception {
        Method method = AiPlatformService.class.getDeclaredMethod("csv", Object.class);
        method.setAccessible(true);
        assertEquals("\"'=HYPERLINK(\"\"url\"\")\"", method.invoke(service, "=HYPERLINK(\"url\")"));
        assertEquals("\"ordinary\"", method.invoke(service, "ordinary"));
    }

    /**
     * 配置测试实体。
     */
    private AiDocumentParseConfigEntity config(String method, int size, int overlap, int minimum) {
        AiDocumentParseConfigEntity config = new AiDocumentParseConfigEntity();
        config.setChunkMethod(method);
        config.setChunkSize(size);
        config.setChunkOverlap(overlap);
        config.setMinChunkSize(minimum);
        config.setSeparators("。");
        return config;
    }

    /**
     * 调用切片核心方法，以覆盖不同策略的实际边界行为。
     */
    @SuppressWarnings("unchecked")
    private List<String> chunks(String text, AiDocumentParseConfigEntity config) throws Exception {
        Method method = AiPlatformService.class.getDeclaredMethod(
                "chunkText", String.class, AiDocumentParseConfigEntity.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(service, text, config);
    }
}
