package com.nexoraone.admin.module.business.mcp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexoraone.admin.module.business.openapi.dao.OpenApiParameterDao;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiParameterEntity;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** API 参数与 AI 工具 JSON Schema 之间的转换及运行时校验。 */
@Service
public class AiToolSchemaService {

    @Resource
    private OpenApiParameterDao parameterDao;
    @Resource
    private ObjectMapper objectMapper;

    /** 从指定 API 版本生成输入和输出 Schema。 */
    public SchemaPair generate(Long versionId) {
        List<OpenApiParameterEntity> parameters = parameterDao.selectList(
                new LambdaQueryWrapper<OpenApiParameterEntity>()
                        .eq(OpenApiParameterEntity::getVersionId, versionId)
                        .orderByAsc(OpenApiParameterEntity::getSort)
                        .orderByAsc(OpenApiParameterEntity::getParameterId));
        return new SchemaPair(
                writeSchema(buildRoot(parameters, 1)),
                writeSchema(buildRoot(parameters, 2)));
    }

    /** 校验第三方提交的 Schema，并返回规范化 JSON。 */
    public String normalizeSchema(String schema, boolean required) {
        if (StringUtils.isBlank(schema)) {
            if (!required) {
                return null;
            }
            throw new IllegalArgumentException("JSON Schema 不能为空");
        }
        try {
            JsonNode root = objectMapper.readTree(schema);
            if (!root.isObject()) {
                throw new IllegalArgumentException("JSON Schema 根节点必须是对象");
            }
            String type = root.path("type").asText();
            if (StringUtils.isNotBlank(type) && !"object".equals(type)) {
                throw new IllegalArgumentException("JSON Schema 根节点 type 必须为 object");
            }
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON Schema 格式不正确");
        }
    }

    /** 按工具输入 Schema 校验模型生成的参数。 */
    public void validateArguments(String schema, Map<String, Object> arguments) {
        validateValue(readSchema(schema), arguments == null ? Map.of() : arguments, "$");
    }

    /** 按输出 Schema 校验工具返回结果；未配置输出 Schema 时不限制。 */
    public void validateResult(String schema, Object result) {
        if (StringUtils.isBlank(schema)) {
            return;
        }
        validateValue(readSchema(schema), result, "$");
    }

    /** 按入参或出参方向构建对象类型的根 Schema。 */
    private ObjectNode buildRoot(List<OpenApiParameterEntity> parameters, int direction) {
        List<OpenApiParameterEntity> selected = parameters.stream()
                .filter(item -> Objects.equals(item.getDirection(), direction))
                .toList();
        Map<Long, List<OpenApiParameterEntity>> children = new LinkedHashMap<>();
        for (OpenApiParameterEntity parameter : selected) {
            children.computeIfAbsent(parameter.getParentId(), ignored -> new ArrayList<>()).add(parameter);
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        ObjectNode properties = root.putObject("properties");
        ArrayNode required = root.putArray("required");
        for (OpenApiParameterEntity parameter : children.getOrDefault(null, List.of())) {
            properties.set(parameter.getParameterName(), buildNode(parameter, children));
            if (Boolean.TRUE.equals(parameter.getRequiredFlag())) {
                required.add(parameter.getParameterName());
            }
        }
        root.put("additionalProperties", false);
        return root;
    }

    /** 将单个 API 参数及其子参数递归转换为 Schema 节点。 */
    private ObjectNode buildNode(OpenApiParameterEntity parameter,
                                 Map<Long, List<OpenApiParameterEntity>> children) {
        ObjectNode node = objectMapper.createObjectNode();
        String dataType = StringUtils.defaultIfBlank(parameter.getDataType(), "String");
        if ("Array<Object>".equalsIgnoreCase(dataType) || "Array".equalsIgnoreCase(dataType)) {
            node.put("type", "array");
            ObjectNode items = node.putObject("items");
            items.put("type", "object");
            appendChildren(items, children.getOrDefault(parameter.getParameterId(), List.of()), children);
        } else if ("Object".equalsIgnoreCase(dataType)) {
            node.put("type", "object");
            appendChildren(node, children.getOrDefault(parameter.getParameterId(), List.of()), children);
        } else {
            node.put("type", jsonType(dataType));
            if ("Date".equalsIgnoreCase(dataType)) {
                node.put("format", "date");
            } else if ("DateTime".equalsIgnoreCase(dataType)) {
                node.put("format", "date-time");
            } else if ("Long".equalsIgnoreCase(dataType) || "Integer".equalsIgnoreCase(dataType)) {
                node.put("format", "int64");
            }
        }
        String description = StringUtils.firstNonBlank(parameter.getDescription(), parameter.getChineseName());
        if (StringUtils.isNotBlank(description)) {
            node.put("description", description);
        }
        if (StringUtils.isNotBlank(parameter.getExampleValue())) {
            node.put("example", parameter.getExampleValue());
        }
        if (StringUtils.isNotBlank(parameter.getDefaultValue())) {
            node.put("default", parameter.getDefaultValue());
        }
        applyValidationRule(node, parameter.getValidationRule());
        return node;
    }

    /** 将子参数追加到对象节点，并同步生成必填字段列表。 */
    private void appendChildren(ObjectNode node,
                                List<OpenApiParameterEntity> childList,
                                Map<Long, List<OpenApiParameterEntity>> children) {
        ObjectNode properties = node.putObject("properties");
        ArrayNode required = node.putArray("required");
        for (OpenApiParameterEntity child : childList) {
            properties.set(child.getParameterName(), buildNode(child, children));
            if (Boolean.TRUE.equals(child.getRequiredFlag())) {
                required.add(child.getParameterName());
            }
        }
        node.put("additionalProperties", false);
    }

    /** 将 API 参数上的简易校验规则转换为 JSON Schema 约束。 */
    private void applyValidationRule(ObjectNode node, String rule) {
        if (StringUtils.isBlank(rule)) {
            return;
        }
        for (String item : rule.split("[;,]")) {
            String[] pair = item.trim().split("[:=]", 2);
            if (pair.length != 2) {
                continue;
            }
            String key = pair[0].trim();
            String value = pair[1].trim();
            try {
                switch (key) {
                    case "minLength", "maxLength", "minimum", "maximum" ->
                            node.put(key, new BigDecimal(value));
                    case "pattern" -> node.put("pattern", value);
                    case "enum" -> {
                        ArrayNode values = node.putArray("enum");
                        for (String enumValue : value.split("\\|")) {
                            values.add(enumValue.trim());
                        }
                    }
                    default -> {
                    }
                }
            } catch (NumberFormatException ignored) {
                // 兼容历史 API 中仅供阅读的校验说明，无法解析时不阻断 Schema 生成。
            }
        }
    }

    /** 将平台参数类型映射为 JSON Schema 基础类型。 */
    private String jsonType(String dataType) {
        if ("Integer".equalsIgnoreCase(dataType) || "Long".equalsIgnoreCase(dataType)) {
            return "integer";
        }
        if ("Decimal".equalsIgnoreCase(dataType) || "Number".equalsIgnoreCase(dataType)) {
            return "number";
        }
        if ("Boolean".equalsIgnoreCase(dataType)) {
            return "boolean";
        }
        return "string";
    }

    /** 解析已保存的 JSON Schema，格式损坏时返回明确业务错误。 */
    private JsonNode readSchema(String schema) {
        try {
            return objectMapper.readTree(schema);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("工具 Schema 格式不正确");
        }
    }

    /** 递归校验对象、数组、基础类型及枚举值。 */
    private void validateValue(JsonNode schema, Object value, String path) {
        if (schema == null || schema.isMissingNode()) {
            return;
        }
        String type = schema.path("type").asText();
        if ("object".equals(type)) {
            if (!(value instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException(path + " 必须是对象");
            }
            JsonNode required = schema.path("required");
            if (required.isArray()) {
                for (JsonNode field : required) {
                    if (!map.containsKey(field.asText()) || map.get(field.asText()) == null) {
                        throw new IllegalArgumentException(path + "." + field.asText() + " 为必填参数");
                    }
                }
            }
            JsonNode properties = schema.path("properties");
            if (properties.isObject()) {
                properties.fields().forEachRemaining(entry -> {
                    if (map.containsKey(entry.getKey()) && map.get(entry.getKey()) != null) {
                        validateValue(entry.getValue(), map.get(entry.getKey()), path + "." + entry.getKey());
                    }
                });
            }
            return;
        }
        if ("array".equals(type)) {
            if (!(value instanceof List<?> list)) {
                throw new IllegalArgumentException(path + " 必须是数组");
            }
            for (int index = 0; index < list.size(); index++) {
                validateValue(schema.path("items"), list.get(index), path + "[" + index + "]");
            }
            return;
        }
        boolean valid = switch (type) {
            case "integer" -> value instanceof Byte || value instanceof Short
                    || value instanceof Integer || value instanceof Long;
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "string" -> value instanceof String;
            default -> true;
        };
        if (!valid) {
            throw new IllegalArgumentException(path + " 类型不符合 Schema 要求");
        }
        JsonNode enumValues = schema.path("enum");
        if (enumValues.isArray() && enumValues.size() > 0) {
            boolean match = false;
            for (JsonNode enumValue : enumValues) {
                if (Objects.equals(enumValue.asText(), Objects.toString(value, null))) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                throw new IllegalArgumentException(path + " 不在允许的枚举值中");
            }
        }
    }

    /** 将 Schema 节点序列化为数据库保存的 JSON 字符串。 */
    private String writeSchema(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("生成工具 Schema 失败", exception);
        }
    }

    /** 一次生成的输入和输出 Schema 结果。 */
    public record SchemaPair(String inputSchema, String outputSchema) {
    }
}
