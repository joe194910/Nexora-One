package com.nexoraone.admin.module.business.ai.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.ai.domain.entity.AiModelServiceEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型服务数据访问接口。
 */
@Mapper
public interface AiModelServiceDao extends BaseMapper<AiModelServiceEntity> {
}
