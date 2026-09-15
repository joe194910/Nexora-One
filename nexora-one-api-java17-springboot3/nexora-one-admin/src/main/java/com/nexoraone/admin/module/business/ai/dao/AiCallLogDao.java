package com.nexoraone.admin.module.business.ai.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.ai.domain.entity.AiCallLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 调用日志数据访问接口。
 */
@Mapper
public interface AiCallLogDao extends BaseMapper<AiCallLogEntity> {
}
