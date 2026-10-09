package com.bozidar.tms.task_service.config;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.util.function.Supplier;

@Component
public class ResilienceExecutor {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public ResilienceExecutor() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                                                            .slidingWindowSize(6)
                                                            .minimumNumberOfCalls(6)
                                                            .failureRateThreshold(100)
                                                            .waitDurationInOpenState(Duration.ofSeconds(10))
                                                            .permittedNumberOfCallsInHalfOpenState(1)
                                                            .ignoreExceptions(HttpClientErrorException.class)
                                                            .build();
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
                                             .maxAttempts(4)
                                             .intervalFunction(
                                                     IntervalFunction.ofExponentialBackoff(
                                                             Duration.ofMillis(200), 2.0, Duration.ofSeconds(5)))
                                             .ignoreExceptions(HttpClientErrorException.class,
                                                               CallNotPermittedException.class)
                                             .build();
        this.retryRegistry = RetryRegistry.of(retryConfig);
    }

    public <T> T execute(String name, Supplier<T> supplier) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
        Retry retry = retryRegistry.retry(name);

        Supplier<T> decorated = Retry.decorateSupplier(
                retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, supplier)
        );

        return decorated.get();
    }
}
