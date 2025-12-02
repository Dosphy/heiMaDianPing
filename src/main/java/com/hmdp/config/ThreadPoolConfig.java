package com.hmdp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Dosphy
 * @Date 2025/12/1 23:56
 */
@Configuration
public class ThreadPoolConfig {

    private static final int CORE_POOL_SIZE = 17;

    private static final int MAX_POOL_SIZE = 50;

    private static final int QUEUE_SIZE = 1000;

    private static final int KEEP_ALIVE_TIME = 500;

    @Bean("taskExecutor")
    public ExecutorService executorService() {
        AtomicInteger c = new AtomicInteger(1);
        LinkedBlockingDeque<Runnable> queue = new LinkedBlockingDeque<>(QUEUE_SIZE);

        return new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.MILLISECONDS,
                queue,
                r -> new Thread(r, "task-" + c.getAndIncrement()),
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }
}
