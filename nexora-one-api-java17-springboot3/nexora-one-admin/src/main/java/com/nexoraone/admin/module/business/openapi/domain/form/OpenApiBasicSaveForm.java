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
 * API基本信息保存表单。
 */
@Data
public class OpenApiBasicSaveForm {

    /** 开放API主键，创建时为空。 */
    private Long openApiId;
    /** 当前版本主键，创建时为空。 */
    private Long versionId;
    /** API显示名称。 */
    @NotBlank(message = "API名称不能为空")
    @Length(max = 100, message = "API名称最多100个字符")
    private String apiName;
    /** 稳定的API编码。 */
    @NotBlank(message = "API编码不能为空")
    @Length(max = 100, message = "API编码最多100个字符")
    private String apiCode;
    /** API分类。 */
    @NotBlank(message = "所属分类不能为空")
    @Length(max = 100, message = "所属分类最多100个字符")
    private String categoryName;
    /** 后端服务名称。 */
    @NotBlank(message = "所属服务不能为空")
    @Length(max = 100, message = "所属服务最多100个字符")
    private String serviceName;
    /** API版本号。 */
    @NotBlank(message = "接口版本不能为空")
    @Length(max = 20, message = "接口版本最多20个字符")
    private String versionNo;
    /** HTTP请求方式。 */
    @NotBlank(message = "请求方式不能为空")
    @Length(max = 10, message = "请求方式最多10个字符")
    private String requestMethod;
    /** 对外网关路径。 */
    @NotBlank(message = "网关路径不能为空")
    @Length(max = 300, message = "网关路径最多300个字符")
    private String gatewayPath;
    /** 内部转发路径。 */
    @Length(max = 300, message = "内部转发地址最多300个字符")
    private String internalPath;
    /** 请求内容类型。 */
    @NotBlank(message = "Content-Type不能为空")
    @Length(max = 50, message = "Content-Type最多50个字符")
    private String contentType;
    /** 权限级别：1公开，2申请授权，3敏感。 */
    @NotNull(message = "权限级别不能为空")
    @Min(value = 1, message = "权限级别不正确")
    @Max(value = 3, message = "权限级别不正确")
    private Integer permissionLevel;
    /** 请求超时时间，单位秒。 */
    @NotNull(message = "请求超时不能为空")
    @Min(value = 1, message = "请求超时不能小于1秒")
    @Max(value = 300, message = "请求超时不能超过300秒")
    private Integer timeoutSeconds;
    /** API负责人姓名。 */
    @Length(max = 50, message = "接口负责人最多50个字符")
    private String ownerName;
    /** 逗号分隔的标签。 */
    @Length(max = 500, message = "标签最多500个字符")
    private String tags;
    /** API说明。 */
    @NotBlank(message = "API简介不能为空")
    @Length(max = 1000, message = "API简介最多1000个字符")
    private String description;
    /** 发布环境列表。 */
    @Valid
    @Size(min = 1, max = 10, message = "至少配置一个发布环境")
    private List<EnvironmentItem> environments;

    /**
     * API环境配置项。
     */
    @Data
    public static class EnvironmentItem {

        /** 环境编码。 */
        @NotBlank(message = "环境编码不能为空")
        @Length(max = 30, message = "环境编码最多30个字符")
        private String environmentCode;
        /** 环境名称。 */
        @NotBlank(message = "环境名称不能为空")
        @Length(max = 50, message = "环境名称最多50个字符")
        private String environmentName;
        /** 环境基础地址。 */
        @NotBlank(message = "环境基础地址不能为空")
        @Length(max = 300, message = "环境基础地址最多300个字符")
        private String baseUrl;
        /** 是否启用环境。 */
        private Boolean enabledFlag;
        /** 是否启用在线调试。 */
        private Boolean onlineDebugFlag;
        /** 环境说明。 */
        @Length(max = 300, message = "环境说明最多300个字符")
        private String description;
    }
}
