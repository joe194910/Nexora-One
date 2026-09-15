package com.nexoraone.admin.module.business.ai.domain.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexoraone.base.config.JsonConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AI 查询时间参数的 JSON 反序列化测试。
 */
class AiQueryDateFormatTest {

    /**
     * 调用日志和用量统计应能解析页面提交的时间格式。
     */
    @Test
    void queryDatesUseSystemJsonFormat() throws Exception {
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        new JsonConfig().customizer().customize(builder);
        ObjectMapper mapper = builder.build();
        String json = "{\"beginTime\":\"2026-09-15 00:00:00\",\"endTime\":\"2026-09-15 23:59:59\"}";

        AiPlatformForm.CallLogQuery logQuery = mapper.readValue(json, AiPlatformForm.CallLogQuery.class);
        AiPlatformForm.StatisticsQuery statisticsQuery = mapper.readValue(json, AiPlatformForm.StatisticsQuery.class);

        assertEquals(LocalDateTime.of(2026, 9, 15, 0, 0), logQuery.getBeginTime());
        assertEquals(LocalDateTime.of(2026, 9, 15, 23, 59, 59), logQuery.getEndTime());
        assertEquals(logQuery.getBeginTime(), statisticsQuery.getBeginTime());
        assertEquals(logQuery.getEndTime(), statisticsQuery.getEndTime());
    }
}
