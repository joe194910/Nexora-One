package com.nexoraone.admin.module.business.openapi.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiVersionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * API version data access interface.
 */
@Mapper
public interface OpenApiVersionDao extends BaseMapper<OpenApiVersionEntity> {
}
