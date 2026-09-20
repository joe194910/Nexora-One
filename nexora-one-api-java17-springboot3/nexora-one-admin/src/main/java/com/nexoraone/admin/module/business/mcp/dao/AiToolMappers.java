package com.nexoraone.admin.module.business.mcp.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.mcp.domain.AiTool;
import com.nexoraone.admin.module.business.mcp.domain.AiToolAssistant;
import com.nexoraone.admin.module.business.mcp.domain.AiToolCallLog;
import com.nexoraone.admin.module.business.mcp.domain.AiToolSchemaSync;
import org.apache.ibatis.annotations.Mapper;

/** MCP/AI 工具模块数据访问。 */
public final class AiToolMappers {
    private AiToolMappers() {}

    /** AI 工具数据访问接口。 */
    @Mapper
    public interface ToolDao extends BaseMapper<AiTool> {}

    /** Schema 同步历史数据访问接口。 */
    @Mapper
    public interface SchemaSyncDao extends BaseMapper<AiToolSchemaSync> {}

    /** 助手与工具关联数据访问接口。 */
    @Mapper
    public interface AssistantToolDao extends BaseMapper<AiToolAssistant> {}

    /** 工具调用日志数据访问接口。 */
    @Mapper
    public interface CallLogDao extends BaseMapper<AiToolCallLog> {}
}
