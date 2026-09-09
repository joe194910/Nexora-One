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
 * API basic information save form.
 */
@Data
public class OpenApiBasicSaveForm {

    /** Open API primary key; empty when creating. */
    private Long openApiId;
    /** Current version primary key; empty when creating. */
    private Long versionId;
    /** API display name. */
    @NotBlank(message = "API名称不能为空")
    @Length(max = 100, message = "API名称最多100个字符")
    private String apiName;
    /** Stable API code. */
    @NotBlank(message = "API编码不能为空")
    @Length(max = 100, message = "API编码最多100个字符")
    private String apiCode;
    /** API category. */
    @NotBlank(message = "所属分类不能为空")
    @Length(max = 100, message = "所属分类最多100个字符")
    private String categoryName;
    /** Backend service name. */
    @NotBlank(message = "所属服务不能为空")
    @Length(max = 100, message = "所属服务最多100个字符")
    private String serviceName;
    /** API version number. */
    @NotBlank(message = "接口版本不能为空")
    @Length(max = 20, message = "接口版本最多20个字符")
    private String versionNo;
    /** HTTP request method. */
    @NotBlank(message = "请求方式不能为空")
    @Length(max = 10, message = "请求方式最多10个字符")
    private String requestMethod;
    /** Public gateway path. */
    @NotBlank(message = "网关路径不能为空")
    @Length(max = 300, message = "网关路径最多300个字符")
    private String gatewayPath;
    /** Internal forwarding path. */
    @Length(max = 300, message = "内部转发地址最多300个字符")
    private String internalPath;
    /** Request content type. */
    @NotBlank(message = "Content-Type不能为空")
    @Length(max = 50, message = "Content-Type最多50个字符")
    private String contentType;
    /** Permission level: 1 public, 2 approval required, 3 sensitive. */
    @NotNull(message = "权限级别不能为空")
    @Min(value = 1, message = "权限级别不正确")
    @Max(value = 3, message = "权限级别不正确")
    private Integer permissionLevel;
    /** Request timeout in seconds. */
    @NotNull(message = "请求超时不能为空")
    @Min(value = 1, message = "请求超时不能小于1秒")
    @Max(value = 300, message = "请求超时不能超过300秒")
    private Integer timeoutSeconds;
    /** API owner name. */
    @Length(max = 50, message = "接口负责人最多50个字符")
    private String ownerName;
    /** Comma separated tags. */
    @Length(max = 500, message = "标签最多500个字符")
    private String tags;
    /** API description. */
    @NotBlank(message = "API简介不能为空")
    @Length(max = 1000, message = "API简介最多1000个字符")
    private String description;
    /** Release environments. */
    @Valid
    @Size(min = 1, max = 10, message = "至少配置一个发布环境")
    private List<EnvironmentItem> environments;

    /**
     * API environment item.
     */
    @Data
    public static class EnvironmentItem {

        /** Environment code. */
        @NotBlank(message = "环境编码不能为空")
        @Length(max = 30, message = "环境编码最多30个字符")
        private String environmentCode;
        /** Environment name. */
        @NotBlank(message = "环境名称不能为空")
        @Length(max = 50, message = "环境名称最多50个字符")
        private String environmentName;
        /** Environment base URL. */
        @NotBlank(message = "环境基础地址不能为空")
        @Length(max = 300, message = "环境基础地址最多300个字符")
        private String baseUrl;
        /** Whether the environment is enabled. */
        private Boolean enabledFlag;
        /** Whether online debugging is enabled. */
        private Boolean onlineDebugFlag;
        /** Environment description. */
        @Length(max = 300, message = "环境说明最多300个字符")
        private String description;
    }
}
