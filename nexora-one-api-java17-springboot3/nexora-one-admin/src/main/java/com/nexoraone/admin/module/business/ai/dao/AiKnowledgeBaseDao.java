package com.nexoraone.admin.module.business.ai.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.ai.domain.entity.AiKnowledgeBaseEntity;
import org.apache.ibatis.annotations.Mapper;

/** 知识库数据访问接口。 */
@Mapper
public interface AiKnowledgeBaseDao extends BaseMapper<AiKnowledgeBaseEntity> {
}
