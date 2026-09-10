package com.nexoraone.admin.module.business.application.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationSsoAuthCodeEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用单点登录授权码数据访问接口。
 */
@Mapper
public interface ApplicationSsoAuthCodeDao extends BaseMapper<ApplicationSsoAuthCodeEntity> {
}
