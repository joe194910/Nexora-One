package com.nexoraone.admin.module.business.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 智能助手免登录分享记录，用于控制有效期、停用状态和访问审计。 */
@Data
@TableName("nexora_one_kb_assistant_share")
public class KnowledgeAssistantShare {

    /** 分享记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long shareId;

    /** 被分享的智能助手主键。 */
    private Long assistantId;

    /** 对外公开且不可预测的分享编码。 */
    private String shareCode;

    /** 创建分享时生成的完整访问链接。 */
    private String shareUrl;

    /** 分享人主键。 */
    private Long sharerId;

    /** 分享人名称快照，避免人员删除后审计信息丢失。 */
    private String sharerName;

    /** 分享来源，当前固定为智能助手商店。 */
    private String shareSource;

    /** 分享链接失效时间。 */
    private LocalDateTime expireTime;

    /** 分享链接是否仍处于启用状态。 */
    private Boolean enabledFlag;

    /** 停用原因编码。 */
    private String disableReason;

    /** 实际停用时间。 */
    private LocalDateTime disabledTime;

    /** 成功打开公开详情的累计次数。 */
    private Long accessCount;

    /** 最近一次成功打开公开详情的时间。 */
    private LocalDateTime lastAccessTime;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
