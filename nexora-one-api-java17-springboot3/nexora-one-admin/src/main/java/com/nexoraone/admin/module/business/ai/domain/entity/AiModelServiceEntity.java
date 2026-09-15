package com.nexoraone.admin.module.business.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 模型服务配置实体。
 */
@Data
@TableName("nexora_one_ai_model_service")
public class AiModelServiceEntity {

    /** 模型服务主键。 */
    @TableId(type = IdType.AUTO)
    private Long serviceId;
    /** 服务名称。 */
    private String serviceName;
    /** 服务商类型。 */
    private String providerType;
    /** 接口协议。 */
    private String protocolType;
    /** 服务基础地址。 */
    private String baseUrl;
    /** 加密后的 API Key。 */
    private String apiKeyCipher;
    /** 服务商组织 ID。 */
    private String organizationId;
    /** 请求超时秒数。 */
    private Integer requestTimeoutSeconds;
    /** 支持能力，使用逗号分隔。 */
    private String capabilities;
    /** 是否为默认服务。 */
    private Boolean defaultFlag;
    /** 是否启用。 */
    private Boolean enabledFlag;
    /** 连接状态。 */
    private String connectionStatus;
    /** 最近测试时间。 */
    private LocalDateTime lastTestTime;
    /** 最近一次错误信息。 */
    private String lastError;
    /** 创建人。 */
    private Long createUserId;
    /** 更新人。 */
    private Long updateUserId;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
