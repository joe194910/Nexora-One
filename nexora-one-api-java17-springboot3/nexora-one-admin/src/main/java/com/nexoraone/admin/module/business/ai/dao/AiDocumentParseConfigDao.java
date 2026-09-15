package com.nexoraone.admin.module.business.ai.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.ai.domain.entity.AiDocumentParseConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档解析配置数据访问接口。
 */
@Mapper
public interface AiDocumentParseConfigDao extends BaseMapper<AiDocumentParseConfigEntity> {
}
