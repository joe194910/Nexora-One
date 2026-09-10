package com.nexoraone.admin.module.business.application.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.application.domain.entity.ApplicationFavoriteEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用收藏数据访问接口。
 */
@Mapper
public interface ApplicationFavoriteDao extends BaseMapper<ApplicationFavoriteEntity> {
}
