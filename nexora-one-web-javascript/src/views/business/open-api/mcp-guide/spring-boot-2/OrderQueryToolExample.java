package com.partner.mcp;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 订单查询工具示例。
 *
 * <p>接入方应将示例返回值替换为自己的 OrderService 调用。</p>
 */
@Component
public class OrderQueryToolExample implements McpTool {

    /** 返回 AI 调用时使用的稳定工具名。 */
    @Override
    public String getName() {
        return "query_order";
    }

    /** 返回清晰描述，帮助 AI 判断何时调用该工具。 */
    @Override
    public String getDescription() {
        return "根据订单编号查询订单基本信息、状态和金额";
    }

    /** 声明 AI 必须传入的订单编号。 */
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> orderId =
                new LinkedHashMap<String, Object>();
        orderId.put("type", "string");
        orderId.put("description",
                "订单编号，例如 SO20260921001");

        Map<String, Object> properties =
                new LinkedHashMap<String, Object>();
        properties.put("orderId", orderId);

        Map<String, Object> schema =
                new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", Arrays.asList("orderId"));
        schema.put("additionalProperties", false);
        return schema;
    }

    /** 调用现有业务 Service 并返回可序列化结果。 */
    @Override
    public Object execute(Map<String, Object> arguments) {
        String orderId =
                String.valueOf(arguments.get("orderId")).trim();
        if (orderId.isEmpty()) {
            throw new IllegalArgumentException("订单编号不能为空");
        }

        // TODO 替换为 orderService.getByOrderId(orderId)。
        Map<String, Object> result =
                new LinkedHashMap<String, Object>();
        result.put("orderId", orderId);
        result.put("status", "PAID");
        result.put("amount", 199.00);
        return result;
    }
}
