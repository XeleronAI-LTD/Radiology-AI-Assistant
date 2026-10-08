package com.xeleronai.medicalimagingbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executor dédié à l'analyse IA, borné à 1-2 threads : la machine de démo
 * ne doit jamais lancer plusieurs inférences lourdes en parallèle (voir
 * DetectionAnalyseServiceImpl#analyserEnArrierePlan).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "detectionTaskExecutor")
    public TaskExecutor detectionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("detection-analyse-");
        executor.initialize();
        return executor;
    }
}
