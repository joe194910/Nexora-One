package com.nexoraone.admin.module.business.openapi.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiErrorCodeEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * API错误码数据访问接口。
 */
@Mapper
public interface OpenApiErrorCodeDao extends BaseMapper<OpenApiErrorCodeEntity> {
}
