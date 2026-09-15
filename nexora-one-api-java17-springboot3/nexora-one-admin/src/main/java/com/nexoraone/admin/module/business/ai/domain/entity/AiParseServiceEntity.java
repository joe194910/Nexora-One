package com.nexoraone.admin.module.business.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/** 文档解析能力服务实体。 */
@Data
@TableName("nexora_one_ai_parse_service")
public class AiParseServiceEntity {
    /** 服务主键。 */
    @TableId(type = IdType.AUTO)
    private Long parseServiceId;
    /** 服务名称。 */
    private String serviceName;
    /** TIKA、OCR、TABLE 或 IMAGE。 */
    private String serviceType;
    /** BUILTIN 或 HTTP。 */
    private String deploymentType;
    /** 内置实现代码。 */
    private String implementation;
    /** HTTP API 根地址。 */
    private String endpoint;
    /** 加密密钥。 */
    private String apiKeyCipher;
    /** 支持格式，逗号分隔。 */
    private String supportedFormats;
    /** 请求超时秒数。 */
    private Integer timeoutSeconds;
    /** 是否启用。 */
    private Boolean enabledFlag;
    /** 是否默认。 */
    private Boolean defaultFlag;
    /** CONNECTED、FAILED 或 UNTESTED。 */
    private String connectionStatus;
    /** 最近检测错误。 */
    private String lastError;
    /** 最近检测时间。 */
    private LocalDateTime lastTestTime;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
