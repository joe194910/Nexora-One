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
 * API示例和错误码保存表单。
 */
@Data
public class OpenApiExampleSaveForm {

    /** 开放API主键。 */
    @NotNull(message = "API主键不能为空")
    private Long openApiId;
    /** API版本主键。 */
    @NotNull(message = "API版本主键不能为空")
    private Long versionId;
    /** 请求和响应示例。 */
    @Valid
    @Size(max = 20, message = "示例最多20条")
    private List<ExampleItem> examples;
    /** 业务错误码。 */
    @Valid
    @Size(max = 100, message = "错误码最多100条")
    private List<ErrorCodeItem> errorCodes;
    /** 版本更新说明。 */
    @Length(max = 500, message = "更新说明最多500个字符")
    private String changeLog;

    /**
     * API示例项。
     */
    @Data
    public static class ExampleItem {

        /** 示例类型。 */
        @NotBlank(message = "示例类型不能为空")
        @Length(max = 30, message = "示例类型最多30个字符")
        private String exampleType;
        /** 示例名称。 */
        @NotBlank(message = "示例名称不能为空")
        @Length(max = 100, message = "示例名称最多100个字符")
        private String exampleName;
        /** JSON或代码内容。 */
        @NotBlank(message = "示例内容不能为空")
        private String content;
        /** 显示顺序。 */
        private Integer sort;
    }

    /**
     * API错误码配置项。
     */
    @Data
    public static class ErrorCodeItem {

        /** HTTP状态码。 */
        @NotNull(message = "HTTP状态码不能为空")
        @Min(value = 100, message = "HTTP状态码不正确")
        @Max(value = 599, message = "HTTP状态码不正确")
        private Integer httpStatus;
        /** 业务错误码。 */
        @NotBlank(message = "业务错误码不能为空")
        @Length(max = 50, message = "业务错误码最多50个字符")
        private String businessCode;
        /** 错误信息。 */
        @NotBlank(message = "错误信息不能为空")
        @Length(max = 200, message = "错误信息最多200个字符")
        private String errorMessage;
        /** 触发条件。 */
        @Length(max = 500, message = "触发条件最多500个字符")
        private String triggerCondition;
        /** 处理建议。 */
        @Length(max = 500, message = "处理建议最多500个字符")
        private String handlingAdvice;
        /** 显示顺序。 */
        private Integer sort;
    }
}
