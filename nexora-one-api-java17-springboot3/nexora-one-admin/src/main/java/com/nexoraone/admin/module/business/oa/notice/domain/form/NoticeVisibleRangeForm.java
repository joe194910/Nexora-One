package com.nexoraone.admin.module.business.oa.notice.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.nexoraone.admin.module.business.oa.notice.constant.NoticeVisibleRangeDataTypeEnum;
import com.nexoraone.base.common.swagger.SchemaEnum;
import com.nexoraone.base.common.validator.enumeration.CheckEnum;

/**
 * 通知公告 可见范围数据
 *
 * @Author NexoraOne-主任: 卓大
 * @Date 2022-08-12 21:40:39
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeVisibleRangeForm {

    @SchemaEnum(NoticeVisibleRangeDataTypeEnum.class)
    @CheckEnum(value = NoticeVisibleRangeDataTypeEnum.class, required = true, message = "数据类型错误")
    private Integer dataType;

    @Schema(description = "员工/部门id")
    @NotNull(message = "员工/部门id不能为空")
    private Long dataId;
}
