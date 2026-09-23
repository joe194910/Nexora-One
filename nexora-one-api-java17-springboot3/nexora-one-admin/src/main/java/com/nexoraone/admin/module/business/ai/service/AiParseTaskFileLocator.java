package com.nexoraone.admin.module.business.ai.service;

/**
 * 历史任务兼容：早期解析任务的 file_path 保存的是本地绝对路径，
 * 由业务文档层把这类任务映射回对象存储键，使旧任务无需重新上传也能继续解析。
 */
public interface AiParseTaskFileLocator {
    /** 返回该任务对应的对象存储键；无法定位时返回 null。 */
    String locateObjectKey(Long taskId);
}
