package com.nexoraone.admin.module.business.openapi.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiExampleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * API example data access interface.
 */
@Mapper
public interface OpenApiExampleDao extends BaseMapper<OpenApiExampleEntity> {
}
