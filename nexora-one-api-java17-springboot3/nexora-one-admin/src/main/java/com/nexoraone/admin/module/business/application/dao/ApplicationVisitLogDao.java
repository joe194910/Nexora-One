package com.nexoraone.admin.module.business.application.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationVisitLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用访问日志数据访问接口。
 */
@Mapper
public interface ApplicationVisitLogDao extends BaseMapper<ApplicationVisitLogEntity> {
}
