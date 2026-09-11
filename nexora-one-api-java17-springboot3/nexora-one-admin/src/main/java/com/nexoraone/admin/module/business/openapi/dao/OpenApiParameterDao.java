package com.nexoraone.admin.module.business.openapi.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiParameterEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * API参数数据访问接口。
 */
@Mapper
public interface OpenApiParameterDao extends BaseMapper<OpenApiParameterEntity> {
}
