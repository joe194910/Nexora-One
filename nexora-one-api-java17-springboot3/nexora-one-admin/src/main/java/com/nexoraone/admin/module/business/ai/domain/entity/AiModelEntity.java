package com.nexoraone.admin.module.business.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 模型配置实体。
 */
@Data
@TableName("nexora_one_ai_model")
public class AiModelEntity {

    /** 模型主键。 */
    @TableId(type = IdType.AUTO)
    private Long modelId;
    /** 模型名称。 */
    private String modelName;
    /** 服务商模型编码。 */
    private String modelCode;
    /** 模型类型。 */
    private String modelType;
    /** 所属模型服务主键。 */
    private Long serviceId;
    /** 支持能力，使用逗号分隔。 */
    private String capabilities;
    /** 上下文长度。 */
    private Integer contextLength;
    /** 向量维度。 */
    private Integer embeddingDimension;
    /** 默认温度。 */
    private BigDecimal temperature;
    /** 最大输出 Token。 */
    private Integer maxOutputTokens;
    /** 每百万输入 Token 价格。 */
    private BigDecimal inputPrice;
    /** 每百万输出 Token 价格。 */
    private BigDecimal outputPrice;
    /** 是否为默认模型。 */
    private Boolean defaultFlag;
    /** 是否启用。 */
    private Boolean enabledFlag;
    /** 创建人。 */
    private Long createUserId;
    /** 更新人。 */
    private Long updateUserId;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
