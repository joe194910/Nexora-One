package com.nexoraone.admin.module.business.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/** 由已就绪文档自由组合的用户知识库，不存放重复向量。 */
@Data
@TableName("nexora_one_kb_base")
public class KnowledgeBase {
    /** 业务知识库主键。 */
    @TableId(type = IdType.AUTO) private Long baseId;
    /** 所属用户。 */
    private Long ownerId;
    /** 名称。 */
    private String baseName;
    /** 描述。 */
    private String description;
    /** 是否启用。 */
    private Boolean enabledFlag;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
