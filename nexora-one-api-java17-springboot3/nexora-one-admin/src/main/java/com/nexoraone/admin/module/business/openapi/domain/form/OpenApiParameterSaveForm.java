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
 * API request or response parameter save form.
 */
@Data
public class OpenApiParameterSaveForm {

    /** Open API primary key. */
    @NotNull(message = "API主键不能为空")
    private Long openApiId;
    /** API version primary key. */
    @NotNull(message = "API版本主键不能为空")
    private Long versionId;
    /** Direction: 1 request, 2 response. */
    @NotNull(message = "参数方向不能为空")
    @Min(value = 1, message = "参数方向不正确")
    @Max(value = 2, message = "参数方向不正确")
    private Integer direction;
    /** Whether unified response wrapping is enabled. */
    private Boolean unifiedResponseFlag;
    /** Whether response masking is enabled. */
    private Boolean dataMaskingFlag;
    /** Parameter rows. */
    @Valid
    @Size(max = 500, message = "单次最多保存500个参数")
    private List<ParameterItem> parameters;

    /**
     * API parameter item.
     */
    @Data
    public static class ParameterItem {

        /** Client-side row key used to resolve nested parent relationships. */
        @Length(max = 64, message = "参数行标识最多64个字符")
        private String rowKey;
        /** Parent client-side row key. */
        @Length(max = 64, message = "父参数行标识最多64个字符")
        private String parentRowKey;
        /** Parameter location. */
        @NotBlank(message = "参数位置不能为空")
        @Length(max = 20, message = "参数位置最多20个字符")
        private String location;
        /** Parameter name. */
        @NotBlank(message = "参数名称不能为空")
        @Length(max = 100, message = "参数名称最多100个字符")
        private String parameterName;
        /** Chinese display name. */
        @Length(max = 100, message = "中文名称最多100个字符")
        private String chineseName;
        /** Data type. */
        @NotBlank(message = "数据类型不能为空")
        @Length(max = 30, message = "数据类型最多30个字符")
        private String dataType;
        /** Whether the parameter is required. */
        private Boolean requiredFlag;
        /** Whether the response field may be null. */
        private Boolean nullableFlag;
        /** Default value. */
        @Length(max = 500, message = "默认值最多500个字符")
        private String defaultValue;
        /** Example value. */
        @Length(max = 1000, message = "示例值最多1000个字符")
        private String exampleValue;
        /** Validation rule. */
        @Length(max = 500, message = "校验规则最多500个字符")
        private String validationRule;
        /** Parameter description. */
        @Length(max = 500, message = "参数说明最多500个字符")
        private String description;
        /** Whether the field requires masking. */
        private Boolean maskingFlag;
        /** Display order. */
        private Integer sort;
    }
}
