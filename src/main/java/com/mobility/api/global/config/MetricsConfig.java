package com.mobility.api.global.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics 설정 클래스
 * Prometheus와 Grafana에서 사용할 커스텀 메트릭을 정의합니다.
 */
@Configuration
public class MetricsConfig {

    /**
     * @Timed 어노테이션 지원을 위한 Aspect
     * 메서드 실행 시간을 자동으로 측정합니다.
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * JVM 스레드 메트릭 수집
     * - 활성 스레드 수
     * - 데몬 스레드 수
     * - 피크 스레드 수 등
     */
    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    /**
     * CPU 프로세서 메트릭 수집
     * - CPU 사용률
     * - 사용 가능한 프로세서 수 등
     */
    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }
}
