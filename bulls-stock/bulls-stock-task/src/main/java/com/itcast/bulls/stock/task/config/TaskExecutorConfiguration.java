package com.itcast.bulls.stock.task.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync(proxyTargetClass=true)
public class TaskExecutorConfiguration {

    @Value("${task.executor.corePoolSize:5 }")
    private int corePoolSize;

    @Value("${task.executor.maxPoolSize:10 }")
    private int maxPoolSize;

    @Value("${task.executor.queueCapacity:500 }")
    private int queueCapacity;

    @Value("${task.executor.keepAliveSeconds:60 }")
    private int keepAliveSeconds;

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("PendingAsyncTaskExec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

}
