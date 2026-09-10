package com.nexoraone.admin.module.business.openapi.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiCallLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 开放API调用日志数据访问接口。
 */
@Mapper
public interface OpenApiCallLogDao extends BaseMapper<OpenApiCallLogEntity> {
}
