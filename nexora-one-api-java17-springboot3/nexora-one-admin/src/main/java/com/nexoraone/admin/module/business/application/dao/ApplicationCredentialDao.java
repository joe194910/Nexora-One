package com.nexoraone.admin.module.business.application.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationCredentialEntity;
import org.apache.ibatis.annotations.Mapper;

/** 应用凭证数据访问接口。 */
@Mapper
public interface ApplicationCredentialDao extends BaseMapper<ApplicationCredentialEntity> {
}
