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
 * API examples and error codes save form.
 */
@Data
public class OpenApiExampleSaveForm {

    /** Open API primary key. */
    @NotNull(message = "API主键不能为空")
    private Long openApiId;
    /** API version primary key. */
    @NotNull(message = "API版本主键不能为空")
    private Long versionId;
    /** Request and response examples. */
    @Valid
    @Size(max = 20, message = "示例最多20条")
    private List<ExampleItem> examples;
    /** Business error codes. */
    @Valid
    @Size(max = 100, message = "错误码最多100条")
    private List<ErrorCodeItem> errorCodes;
    /** Version change log. */
    @Length(max = 500, message = "更新说明最多500个字符")
    private String changeLog;

    /**
     * API example item.
     */
    @Data
    public static class ExampleItem {

        /** Example type. */
        @NotBlank(message = "示例类型不能为空")
        @Length(max = 30, message = "示例类型最多30个字符")
        private String exampleType;
        /** Example name. */
        @NotBlank(message = "示例名称不能为空")
        @Length(max = 100, message = "示例名称最多100个字符")
        private String exampleName;
        /** JSON or code content. */
        @NotBlank(message = "示例内容不能为空")
        private String content;
        /** Display order. */
        private Integer sort;
    }

    /**
     * API error code item.
     */
    @Data
    public static class ErrorCodeItem {

        /** HTTP status code. */
        @NotNull(message = "HTTP状态码不能为空")
        @Min(value = 100, message = "HTTP状态码不正确")
        @Max(value = 599, message = "HTTP状态码不正确")
        private Integer httpStatus;
        /** Business error code. */
        @NotBlank(message = "业务错误码不能为空")
        @Length(max = 50, message = "业务错误码最多50个字符")
        private String businessCode;
        /** Error message. */
        @NotBlank(message = "错误信息不能为空")
        @Length(max = 200, message = "错误信息最多200个字符")
        private String errorMessage;
        /** Trigger condition. */
        @Length(max = 500, message = "触发条件最多500个字符")
        private String triggerCondition;
        /** Recommended handling. */
        @Length(max = 500, message = "处理建议最多500个字符")
        private String handlingAdvice;
        /** Display order. */
        private Integer sort;
    }
}
