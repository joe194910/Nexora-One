package com.nexoraone.base.module.support.codegenerator.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.nexoraone.base.common.domain.PageParam;


/**
 * 查询表数据
 *
 * @Author NexoraOne-主任: 卓大
 * @Date 2022-06-30 22:15:38
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@Data
public class TableQueryForm extends PageParam {

    @Schema(description = "表名关键字")
    private String tableNameKeywords;

}
