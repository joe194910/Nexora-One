package com.nexoraone.admin.module.business.ai.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.ai.domain.entity.AiParseStepEntity;
import org.apache.ibatis.annotations.Mapper;

/** 解析任务阶段记录数据访问接口。 */
@Mapper
public interface AiParseStepDao extends BaseMapper<AiParseStepEntity> {
}
