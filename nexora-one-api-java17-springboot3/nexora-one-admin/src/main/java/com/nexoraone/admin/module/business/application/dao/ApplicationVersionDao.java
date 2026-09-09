package com.nexoraone.admin.module.business.application.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationVersionEntity;
import org.apache.ibatis.annotations.Mapper;

/** 应用版本数据访问接口。 */
@Mapper
public interface ApplicationVersionDao extends BaseMapper<ApplicationVersionEntity> {
}
