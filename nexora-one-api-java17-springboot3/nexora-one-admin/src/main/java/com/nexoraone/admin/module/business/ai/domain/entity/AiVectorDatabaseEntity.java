package com.nexoraone.admin.module.business.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 向量数据库配置实体。
 */
@Data
@TableName("nexora_one_ai_vector_database")
public class AiVectorDatabaseEntity {

    /** 向量数据库主键。 */
    @TableId(type = IdType.AUTO)
    private Long vectorDatabaseId;
    /** 实例名称。 */
    private String instanceName;
    /** 数据库类型。 */
    private String databaseType;
    /** 服务地址。 */
    private String serviceUrl;
    /** 加密后的 API Key。 */
    private String apiKeyCipher;
    /** 集合名称前缀。 */
    private String collectionPrefix;
    /** 请求超时秒数。 */
    private Integer requestTimeoutSeconds;
    /** 是否使用 TLS。 */
    private Boolean tlsEnabled;
    /** 是否为默认实例。 */
    private Boolean defaultFlag;
    /** 是否启用。 */
    private Boolean enabledFlag;
    /** 连接状态。 */
    private String connectionStatus;
    /** 集合数量。 */
    private Integer collectionCount;
    /** 向量总数。 */
    private Long vectorCount;
    /** 最近测试时间。 */
    private LocalDateTime lastTestTime;
    /** 最近错误信息。 */
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
