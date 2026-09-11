package com.nexoraone.admin.module.business.openapi.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.openapi.domain.entity.OpenApiPublishReviewEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * API发布审核记录数据访问接口。
 */
@Mapper
public interface OpenApiPublishReviewDao extends BaseMapper<OpenApiPublishReviewEntity> {
}
