package com.nexoraone.admin.module.business.application.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import org.apache.ibatis.annotations.Mapper;

/** 开放API目录数据访问接口。 */
@Mapper
public interface OpenApiDao extends BaseMapper<OpenApiEntity> {
}
