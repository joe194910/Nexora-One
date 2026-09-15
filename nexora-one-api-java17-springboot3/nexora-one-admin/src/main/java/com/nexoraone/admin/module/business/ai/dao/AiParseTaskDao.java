package com.nexoraone.admin.module.business.ai.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.ai.domain.entity.AiParseTaskEntity;
import org.apache.ibatis.annotations.Mapper;

/** 解析任务数据访问接口。 */
@Mapper
public interface AiParseTaskDao extends BaseMapper<AiParseTaskEntity> {
}
