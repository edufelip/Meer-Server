package com.edufelip.meer.config;

import java.util.concurrent.Executor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for async image moderation processing. Creates a dedicated thread pool with low
 * priority for background image verification tasks.
 */
@Configuration
@EnableAsync
@EnableScheduling
@ConditionalOnProperty(
    name = "moderation.nsfw.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ModerationAsyncConfig {

  private final ModerationProperties properties;

  public ModerationAsyncConfig(ModerationProperties properties) {
    this.properties = properties;
  }

  @Bean(name = "moderationTaskExecutor")
  public Executor moderationTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    // Use configured pool size (default: 2 threads)
    int poolSize = properties.getWorker().getThreadPoolSize();
    executor.setCorePoolSize(poolSize);
    executor.setMaxPoolSize(poolSize);

    // Large queue to buffer pending tasks
    executor.setQueueCapacity(properties.getWorker().getQueueCapacity());

    // Meaningful thread names for debugging
    executor.setThreadNamePrefix("moderation-worker-");

    // Allow core threads to timeout when idle
    executor.setAllowCoreThreadTimeOut(true);
    executor.setKeepAliveSeconds(60);

    // Reject policy: caller runs (back-pressure mechanism)
    executor.setRejectedExecutionHandler(
        new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

    executor.initialize();
    return executor;
  }
}
