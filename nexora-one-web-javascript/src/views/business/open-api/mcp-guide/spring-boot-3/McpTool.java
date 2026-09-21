package com.partner.mcp;

import java.util.Collections;
import java.util.Map;

/**
 * 可暴露给 NexoraOne 的 MCP 工具。
 *
 * <p>一个业务能力实现一个 Spring Bean。工具名、说明和 Schema 会通过
 * tools/list 自动同步到 NexoraOne。</p>
 */
public interface McpTool {

    /** 返回全局唯一的工具名称。 */
    String getName();

    /** 返回供 AI 理解工具用途、适用场景和返回内容的说明。 */
    String getDescription();

    /** 返回工具入参 JSON Schema。 */
    Map<String, Object> getInputSchema();

    /** 返回工具出参 JSON Schema；暂不声明时返回空 Map。 */
    default Map<String, Object> getOutputSchema() {
        return Collections.emptyMap();
    }

    /**
     * 执行业务能力。
     *
     * @param arguments 已通过官方 SDK Schema 校验的动态参数
     * @return 可被 Jackson 序列化的业务结果
     */
    Object execute(Map<String, Object> arguments);
}
