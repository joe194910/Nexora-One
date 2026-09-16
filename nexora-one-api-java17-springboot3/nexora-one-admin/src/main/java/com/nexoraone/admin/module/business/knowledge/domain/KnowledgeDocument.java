package com.nexoraone.admin.module.business.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/** 用户文档及其唯一一次向量化结果；源文件持久存放于 MinIO。 */
@Data
@TableName("nexora_one_kb_document")
public class KnowledgeDocument {
    /** 文档主键。 */
    @TableId(type = IdType.AUTO) private Long documentId;
    /** 所属用户。 */
    private Long ownerId;
    /** 原始文件名。 */
    private String fileName;
    /** 扩展名。 */
    private String fileType;
    /** 字节数。 */
    private Long fileSize;
    /** 文件内容 SHA-256。 */
    private String fileHash;
    /** MinIO 对象键。 */
    private String objectKey;
    /** 解析方案快照关联。 */
    private Long parsePlanId;
    /** 向量模型关联。 */
    private Long embeddingModelId;
    /** 向量实例关联。 */
    private Long vectorDatabaseId;
    /** 复用的底层解析任务。 */
    private Long taskId;
    /** 复用的底层入库上下文。 */
    private Long ingestBaseId;
    /** PROCESSING、READY 或 FAILED。 */
    private String status;
    /** 解析任务生成的切片数，不持久化到用户文档表。 */
    @TableField(exist = false) private Integer chunkCount;
    /** 已写入向量库的切片数，不持久化到用户文档表。 */
    @TableField(exist = false) private Integer indexedCount;
    /** 当前解析阶段，不持久化到用户文档表。 */
    @TableField(exist = false) private String currentStage;
    /** 任务失败原因，不持久化到用户文档表。 */
    @TableField(exist = false) private String errorMessage;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 最后更新时间。 */
    private LocalDateTime updateTime;
}
