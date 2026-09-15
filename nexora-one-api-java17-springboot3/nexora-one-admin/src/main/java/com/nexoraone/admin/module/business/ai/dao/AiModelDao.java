package com.nexoraone.admin.module.business.ai.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.ai.domain.entity.AiModelEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型数据访问接口。
 */
@Mapper
public interface AiModelDao extends BaseMapper<AiModelEntity> {
}
