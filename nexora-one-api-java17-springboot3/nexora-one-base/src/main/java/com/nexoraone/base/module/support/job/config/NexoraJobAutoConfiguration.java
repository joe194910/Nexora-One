package com.nexoraone.base.module.support.job.config;

import com.nexoraone.base.module.support.job.core.NexoraJob;
import com.nexoraone.base.module.support.job.core.NexoraJobLauncher;
import com.nexoraone.base.module.support.job.repository.NexoraJobRepository;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 定时任务 配置
 *
 * @author huke
 * @date 2024/6/17 21:30
 */
@Configuration
@EnableConfigurationProperties(NexoraJobConfig.class)
@ConditionalOnProperty(
        prefix = NexoraJobConfig.CONFIG_PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class NexoraJobAutoConfiguration {

    private final NexoraJobConfig jobConfig;

    private final NexoraJobRepository jobRepository;

    private final List<NexoraJob> jobInterfaceList;

    public NexoraJobAutoConfiguration(NexoraJobConfig jobConfig,
                                     NexoraJobRepository jobRepository,
                                     List<NexoraJob> jobInterfaceList) {
        this.jobConfig = jobConfig;
        this.jobRepository = jobRepository;
        this.jobInterfaceList = jobInterfaceList;
    }

    /**
     * 定时任务启动器
     *
     * @return
     */
    @Bean
    public NexoraJobLauncher initJobLauncher(RedissonClient redissonClient) {
        return new NexoraJobLauncher(jobConfig, jobRepository, jobInterfaceList, redissonClient);
    }
}
