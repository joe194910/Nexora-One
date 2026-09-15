package com.nexoraone.admin.module.business.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/** 文档解析任务实体，状态变更由数据库条件更新保护。 */
@Data
@TableName("nexora_one_ai_parse_task")
public class AiParseTaskEntity {
    /** 任务主键。 */
    @TableId(type = IdType.AUTO)
    private Long taskId;
    /** 知识库主键。 */
    private Long knowledgeBaseId;
    /** 方案主键快照。 */
    private Long parsePlanId;
    /** 文档名。 */
    private String fileName;
    /** 受控文件存储路径。 */
    private String filePath;
    /** 文件哈希。 */
    private String fileHash;
    /** 字节数。 */
    private Long fileSize;
    /** QUEUED、RUNNING、SUCCESS、FAILED 或 CANCELLED。 */
    private String status;
    /** 当前阶段：UPLOAD、PARSE、CHUNK、INDEX。 */
    private String currentStage;
    /** 切片数。 */
    private Integer chunkCount;
    /** 入库数。 */
    private Integer indexedCount;
    /** 错误原因。 */
    private String errorMessage;
    /** 创建人。 */
    private Long createUserId;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 开始时间。 */
    private LocalDateTime startTime;
    /** 结束时间。 */
    private LocalDateTime finishTime;
}
