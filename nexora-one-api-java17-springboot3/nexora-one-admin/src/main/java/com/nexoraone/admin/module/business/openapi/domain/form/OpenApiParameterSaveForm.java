package com.nexoraone.admin.module.business.openapi.domain.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * API请求或响应参数保存表单。
 */
@Data
public class OpenApiParameterSaveForm {

    /** 开放API主键。 */
    @NotNull(message = "API主键不能为空")
    private Long openApiId;
    /** API版本主键。 */
    @NotNull(message = "API版本主键不能为空")
    private Long versionId;
    /** 参数方向：1请求，2响应。 */
    @NotNull(message = "参数方向不能为空")
    @Min(value = 1, message = "参数方向不正确")
    @Max(value = 2, message = "参数方向不正确")
    private Integer direction;
    /** 是否启用统一响应封装。 */
    private Boolean unifiedResponseFlag;
    /** 是否启用响应脱敏。 */
    private Boolean dataMaskingFlag;
    /** 参数行。 */
    @Valid
    @Size(max = 500, message = "单次最多保存500个参数")
    private List<ParameterItem> parameters;

    /**
     * API参数项。
     */
    @Data
    public static class ParameterItem {

        /** 前端行标识，用于解析嵌套父子关系。 */
        @Length(max = 64, message = "参数行标识最多64个字符")
        private String rowKey;
        /** 前端父行标识。 */
        @Length(max = 64, message = "父参数行标识最多64个字符")
        private String parentRowKey;
        /** 参数位置。 */
        @NotBlank(message = "参数位置不能为空")
        @Length(max = 20, message = "参数位置最多20个字符")
        private String location;
        /** 参数名称。 */
        @NotBlank(message = "参数名称不能为空")
        @Length(max = 100, message = "参数名称最多100个字符")
        private String parameterName;
        /** 中文显示名称。 */
        @Length(max = 100, message = "中文名称最多100个字符")
        private String chineseName;
        /** 数据类型。 */
        @NotBlank(message = "数据类型不能为空")
        @Length(max = 30, message = "数据类型最多30个字符")
        private String dataType;
        /** 参数是否必填。 */
        private Boolean requiredFlag;
        /** 响应字段是否允许为空。 */
        private Boolean nullableFlag;
        /** 默认值。 */
        @Length(max = 500, message = "默认值最多500个字符")
        private String defaultValue;
        /** 示例值。 */
        @Length(max = 1000, message = "示例值最多1000个字符")
        private String exampleValue;
        /** 校验规则。 */
        @Length(max = 500, message = "校验规则最多500个字符")
        private String validationRule;
        /** 参数说明。 */
        @Length(max = 500, message = "参数说明最多500个字符")
        private String description;
        /** 字段是否需要脱敏。 */
        private Boolean maskingFlag;
        /** 显示顺序。 */
        private Integer sort;
    }
}
