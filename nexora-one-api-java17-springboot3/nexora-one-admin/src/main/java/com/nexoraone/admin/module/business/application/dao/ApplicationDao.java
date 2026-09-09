package com.nexoraone.admin.module.business.application.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationEntity;
import org.apache.ibatis.annotations.Mapper;

/** 应用数据访问接口。 */
@Mapper
public interface ApplicationDao extends BaseMapper<ApplicationEntity> {
}
