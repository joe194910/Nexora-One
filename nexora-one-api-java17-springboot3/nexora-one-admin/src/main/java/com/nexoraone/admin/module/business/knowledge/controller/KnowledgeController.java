package com.nexoraone.admin.module.business.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexoraone.admin.module.business.knowledge.domain.*;
import com.nexoraone.admin.module.business.knowledge.service.*;
import com.nexoraone.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 用户知识库业务接口；不复用管理员解析配置的对外权限。 */
@RestController
@RequestMapping("/knowledge")
@Tag(name = "知识库-我的文档、知识库管理、智能助手")
public class KnowledgeController {
    @Resource private KnowledgeDocumentService documents;
    @Resource private KnowledgeCatalogService catalog;
    @Resource private KnowledgeChatService chat;

    /** 获取启用的解析方案及对话模型供用户选择。 */
    @GetMapping("/options") @Operation(summary = "用户可选配置")
    @SaCheckPermission("knowledge:document:query")
    public ResponseDTO<Map<String, Object>> options() { return ResponseDTO.ok(documents.options()); }

    /** 查询当前用户文档及真实任务状态。 */
    @GetMapping("/documents") @Operation(summary = "我的文档")
    @SaCheckPermission("knowledge:document:query")
    public ResponseDTO<List<KnowledgeDocument>> documents(@RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) String status) {
        return ResponseDTO.ok(documents.list(keyword, status));
    }

    /** MinIO 保存原件，并按选定方案创建已有解析任务。 */
    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传并解析文档")
    @SaCheckPermission("knowledge:document:write")
    public ResponseDTO<KnowledgeDocument> upload(@RequestPart("file") MultipartFile file,
                                                  @RequestParam(required = false) Long planId) throws IOException {
        return ResponseDTO.ok(documents.upload(file, planId));
    }

    /** 查看原解析任务四步结果与文档信息。 */
    @GetMapping("/documents/{id}") @Operation(summary = "文档处理详情")
    @SaCheckPermission("knowledge:document:query")
    public ResponseDTO<Map<String, Object>> detail(@PathVariable Long id) {
        return ResponseDTO.ok(documents.detail(id));
    }

    /** 按当前用户权限从 MinIO 下载源文件。 */
    @GetMapping("/documents/{id}/download") @Operation(summary = "下载原文档")
    @SaCheckPermission("knowledge:document:query")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        KnowledgeDocument document = documents.owned(id);
        String encoded = java.net.URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(documents.download(id));
    }

    /** 从 MinIO 恢复解析工作文件，并重试已有任务。 */
    @PostMapping("/documents/{id}/retry") @Operation(summary = "重新处理文档")
    @SaCheckPermission("knowledge:document:write")
    public ResponseDTO<String> retry(@PathVariable Long id) {
        documents.retry(id);
        return ResponseDTO.ok();
    }

    /** 删除未被知识库引用的原件与实际向量点。 */
    @PostMapping("/documents/{id}/delete") @Operation(summary = "删除我的文档")
    @SaCheckPermission("knowledge:document:write")
    public ResponseDTO<String> deleteDocument(@PathVariable Long id) {
        documents.delete(id);
        return ResponseDTO.ok();
    }

    /** 查询我的知识库及其文档关联。 */
    @GetMapping("/bases") @Operation(summary = "我的知识库")
    @SaCheckPermission("knowledge:base:query")
    public ResponseDTO<List<Map<String, Object>>> bases(@RequestParam(required = false) String keyword) {
        return ResponseDTO.ok(catalog.bases(keyword));
    }

    /** 创建或编辑知识库，不复制已就绪文档或向量。 */
    @PostMapping("/bases/save") @Operation(summary = "保存知识库")
    @SaCheckPermission("knowledge:base:write")
    public ResponseDTO<KnowledgeBase> saveBase(@RequestBody @Valid KnowledgeForms.BaseSave form) {
        return ResponseDTO.ok(catalog.saveBase(form));
    }

    /** 删除未被助手引用的知识库。 */
    @PostMapping("/bases/{id}/delete") @Operation(summary = "删除知识库")
    @SaCheckPermission("knowledge:base:write")
    public ResponseDTO<String> deleteBase(@PathVariable Long id) {
        catalog.deleteBase(id);
        return ResponseDTO.ok();
    }

    /** 查询我的助手及其多知识库关联。 */
    @GetMapping("/assistants") @Operation(summary = "我的智能助手")
    @SaCheckPermission("knowledge:assistant:query")
    public ResponseDTO<List<Map<String, Object>>> assistants() {
        return ResponseDTO.ok(catalog.assistants());
    }

    /** 配置对话模型、提示词、知识库及检索参数。 */
    @PostMapping("/assistants/save") @Operation(summary = "保存智能助手")
    @SaCheckPermission("knowledge:assistant:write")
    public ResponseDTO<KnowledgeAssistant> saveAssistant(@RequestBody @Valid KnowledgeForms.AssistantSave form) {
        return ResponseDTO.ok(catalog.saveAssistant(form));
    }

    /** 删除助手及其会话，不删除知识库。 */
    @PostMapping("/assistants/{id}/delete") @Operation(summary = "删除智能助手")
    @SaCheckPermission("knowledge:assistant:write")
    public ResponseDTO<String> deleteAssistant(@PathVariable Long id) {
        catalog.deleteAssistant(id);
        return ResponseDTO.ok();
    }

    /** 多知识库检索、真实模型对话及审计用量。 */
    @PostMapping("/assistants/{id}/chat") @Operation(summary = "助手问答")
    @SaCheckPermission("knowledge:assistant:chat")
    public ResponseDTO<Map<String, Object>> chat(@PathVariable Long id,
                                                   @RequestBody @Valid KnowledgeForms.Chat form) {
        return ResponseDTO.ok(chat.chat(id, form));
    }

    /** 查询本人助手的会话清单。 */
    @GetMapping("/assistants/{id}/conversations") @Operation(summary = "助手会话")
    @SaCheckPermission("knowledge:assistant:chat")
    public ResponseDTO<List<KnowledgeConversation>> conversations(@PathVariable Long id) {
        return ResponseDTO.ok(chat.conversations(id));
    }

    /** 查询本人会话及引用片段。 */
    @GetMapping("/assistants/{id}/conversations/{conversationId}/messages")
    @Operation(summary = "会话消息")
    @SaCheckPermission("knowledge:assistant:chat")
    public ResponseDTO<List<KnowledgeMessage>> messages(@PathVariable Long id,
                                                          @PathVariable Long conversationId) {
        return ResponseDTO.ok(chat.messages(id, conversationId));
    }

    /** 删除本人助手会话。 */
    @PostMapping("/assistants/{id}/conversations/{conversationId}/delete")
    @Operation(summary = "删除会话")
    @SaCheckPermission("knowledge:assistant:chat")
    public ResponseDTO<String> deleteConversation(@PathVariable Long id, @PathVariable Long conversationId) {
        chat.deleteConversation(id, conversationId);
        return ResponseDTO.ok();
    }
}
