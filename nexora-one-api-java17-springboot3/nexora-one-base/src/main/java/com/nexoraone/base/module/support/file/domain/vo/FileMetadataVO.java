package com.nexoraone.base.module.support.file.domain.vo;

import lombok.Data;

/**
 * 文件元数据
 *
 * @Author NexoraOne: 罗伊
 * @Date 2019年10月11日 15:34:47
 * @Wechat NexoraOne
 * @Email NexoraOne
 * @Copyright  <a href="#">NexoraOne</a>
 */
@Data
public class FileMetadataVO {

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件大小/字节
     */
    private Long fileSize;

    /**
     * 文件格式
     */
    private String fileFormat;
}
