package com.nexoraone.admin.module.business.application.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexoraone.admin.module.business.application.domain.entity.OpenApiEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/** 开放API目录数据访问接口。 */
@Mapper
public interface OpenApiDao extends BaseMapper<OpenApiEntity> {

    /** 原子增加 API 今日调用量和累计调用量。 */
    @Update("""
            UPDATE nexora_one_open_api
            SET today_call_count = CASE
                    WHEN call_count_date = CURRENT_DATE THEN COALESCE(today_call_count, 0) + 1
                    ELSE 1
                END,
                total_call_count = COALESCE(total_call_count, 0) + 1,
                call_count_date = CURRENT_DATE
            WHERE open_api_id = #{openApiId}
            """)
    int increaseCallCount(Long openApiId);
}
