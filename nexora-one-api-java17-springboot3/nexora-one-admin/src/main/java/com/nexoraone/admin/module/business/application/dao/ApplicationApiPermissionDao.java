package com.nexoraone.admin.module.business.application.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationApiPermissionEntity;
import org.apache.ibatis.annotations.Mapper;

/** 应用API权限数据访问接口。 */
@Mapper
public interface ApplicationApiPermissionDao extends BaseMapper<ApplicationApiPermissionEntity> {
}
