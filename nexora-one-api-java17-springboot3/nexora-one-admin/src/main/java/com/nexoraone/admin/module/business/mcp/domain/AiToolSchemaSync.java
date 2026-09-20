package com.nexoraone.admin.module.business.mcp.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 平台 API 版本与工具 Schema 的显式同步历史。 */
@Data
@TableName("nexora_one_ai_tool_schema_sync")
public class AiToolSchemaSync {
    /** 同步记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long syncId;
    /** AI 工具主键。 */
    private Long toolId;
    /** 平台 API 主键。 */
    private Long openApiId;
    /** 本次同步使用的 API 版本主键。 */
    private Long apiVersionId;
    /** 本次同步使用的 API 版本号。 */
    private String apiVersionNo;
    /** 同步类型：创建关联或手动同步。 */
    private String syncType;
    /** 操作人主键。 */
    private Long operatorId;
    /** 同步时间。 */
    private LocalDateTime createTime;
}
