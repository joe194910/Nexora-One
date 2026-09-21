package com.partner.mcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 老项目可实现的 MCP 工具契约。
 *
 * <p>一个实现类对应一个工具，标注 {@code @Component} 后会被自动发现。</p>
 */
public interface McpTool {

    /** 返回全局唯一的工具名称。 */
    String getName();

    /** 返回供 AI 理解工具用途、适用场景和返回内容的说明。 */
    String getDescription();

    /** 返回工具入参 JSON Schema。 */
    default Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", Collections.emptyMap());
        schema.put("additionalProperties", false);
        return schema;
    }

    /** 返回工具出参 JSON Schema；暂不声明时返回空 Map。 */
    default Map<String, Object> getOutputSchema() {
        return Collections.emptyMap();
    }

    /**
     * 执行业务能力。
     *
     * @param arguments AI 传入的动态参数，无参数时为空 Map
     * @return 可被 Jackson 序列化的业务结果
     */
    Object execute(Map<String, Object> arguments);

    /** 返回符合 tools/list 要求的工具定义。 */
    default Map<String, Object> getDefinition() {
        Map<String, Object> definition =
                new LinkedHashMap<String, Object>();
        definition.put("name", getName());
        definition.put("description", getDescription());
        definition.put("inputSchema", getInputSchema());
        if (getOutputSchema() != null && !getOutputSchema().isEmpty()) {
            definition.put("outputSchema", getOutputSchema());
        }
        return definition;
    }
}
