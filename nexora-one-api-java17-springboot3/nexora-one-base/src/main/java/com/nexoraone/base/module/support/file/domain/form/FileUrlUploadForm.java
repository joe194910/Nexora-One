package com.nexoraone.base.module.support.file.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.nexoraone.base.common.swagger.SchemaEnum;
import com.nexoraone.base.common.validator.enumeration.CheckEnum;
import com.nexoraone.base.module.support.file.constant.FileFolderTypeEnum;

/**
 * url上传文件
 *
 * @Author NexoraOne: 罗伊
 * @Date 2019年10月11日 15:34:47
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@Data
public class FileUrlUploadForm {

    @SchemaEnum(value = FileFolderTypeEnum.class, desc = "业务类型")
    @CheckEnum(value = FileFolderTypeEnum.class, required = true, message = "业务类型错误")
    private Integer folder;

    @Schema(description = "文件url")
    @NotBlank(message = "文件url不能为空")
    private String fileUrl;

}
