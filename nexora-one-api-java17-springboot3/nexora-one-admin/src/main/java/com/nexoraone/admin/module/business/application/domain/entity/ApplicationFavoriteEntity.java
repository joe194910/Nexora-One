package com.nexoraone.admin.module.business.application.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用收藏记录。
 */
@Data
@TableName("nexora_one_application_favorite")
public class ApplicationFavoriteEntity {

    /** 收藏记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long favoriteId;
    /** 应用主键。 */
    private Long applicationId;
    /** 员工主键。 */
    private Long employeeId;
    /** 创建时间。 */
    private LocalDateTime createTime;
}
