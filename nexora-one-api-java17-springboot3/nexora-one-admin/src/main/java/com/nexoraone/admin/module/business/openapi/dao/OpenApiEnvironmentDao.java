package com.nexoraone.admin.module.business.openapi.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiEnvironmentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * API环境数据访问接口。
 */
@Mapper
public interface OpenApiEnvironmentDao extends BaseMapper<OpenApiEnvironmentEntity> {
}
