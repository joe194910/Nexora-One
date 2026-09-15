package com.nexoraone.admin.module.business.ai.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.ai.domain.entity.AiVectorDatabaseEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 向量数据库数据访问接口。
 */
@Mapper
public interface AiVectorDatabaseDao extends BaseMapper<AiVectorDatabaseEntity> {
}
