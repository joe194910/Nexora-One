package com.nexoraone.admin.module.business.application.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationReviewEntity;
import org.apache.ibatis.annotations.Mapper;

/** 应用审核记录数据访问接口。 */
@Mapper
public interface ApplicationReviewDao extends BaseMapper<ApplicationReviewEntity> {
}
