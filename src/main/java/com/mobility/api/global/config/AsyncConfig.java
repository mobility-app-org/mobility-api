package com.mobility.api.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 설정
 * - 자동 배차 알림 시스템에서 순차 알림 전송을 비동기로 처리
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 자동 배차용 ThreadPool Executor
     * - 배차 등록 시 비동기로 순차 알림 전송
     * - 여러 배차가 동시에 등록되어도 독립적으로 처리
     */
    @Bean(name = "autoDispatchExecutor")
    public Executor autoDispatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);  // 기본 스레드 수
        executor.setMaxPoolSize(10);  // 최대 스레드 수
        executor.setQueueCapacity(100);  // 대기 큐 크기
        executor.setThreadNamePrefix("auto-dispatch-");  // 스레드 이름 prefix
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());  // 큐가 가득 찼을 때 호출자 스레드에서 실행
        executor.initialize();
        return executor;
    }

    /**
     * 타임아웃 처리용 ScheduledExecutorService
     * - 각 기사별 5초 타임아웃 스케줄링
     */
    @Bean(name = "dispatchTimeoutScheduler")
    public ScheduledExecutorService dispatchTimeoutScheduler() {
        return Executors.newScheduledThreadPool(10);
    }
}
