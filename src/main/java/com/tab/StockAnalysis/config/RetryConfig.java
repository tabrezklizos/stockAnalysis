package com.tab.StockAnalysis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRetry
public class RetryConfig {

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Set backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000); // 1 second
        backOffPolicy.setMaxInterval(10000);    // 10 seconds
        backOffPolicy.setMultiplier(2);         // double the interval each time
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Set retry policy
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(HttpClientErrorException.TooManyRequests.class, true);
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions); // retry 3 times
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
} 