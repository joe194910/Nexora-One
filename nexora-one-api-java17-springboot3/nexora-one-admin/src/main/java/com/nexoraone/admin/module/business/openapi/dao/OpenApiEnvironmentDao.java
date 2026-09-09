package com.nexoraone.admin.module.business.openapi.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiEnvironmentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * API environment data access interface.
 */
@Mapper
public interface OpenApiEnvironmentDao extends BaseMapper<OpenApiEnvironmentEntity> {
}
