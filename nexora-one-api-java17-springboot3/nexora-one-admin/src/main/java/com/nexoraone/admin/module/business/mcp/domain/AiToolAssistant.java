package com.nexoraone.admin.module.business.mcp.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 智能助手与工具的多对多关系。 */
@Data
@TableName("nexora_one_kb_assistant_tool")
public class AiToolAssistant {
    /** 智能助手主键。 */
    private Long assistantId;
    /** AI 工具主键。 */
    private Long toolId;
    /** 关联创建时间。 */
    private LocalDateTime createTime;
}
