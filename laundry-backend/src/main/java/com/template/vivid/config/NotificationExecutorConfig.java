package com.template.vivid.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class NotificationExecutorConfig {

    private static final int CORE_POOL_SIZE = 24;
    private static final int MAX_POOL_SIZE = 24;
    private static final int QUEUE_CAPACITY = 500; // Nombre maximum de notifications en attente dans la file d'attente
    private static final long KEEP_ALIVE_SECONDS = 60L;

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "notif-async-" + counter.getAndIncrement());
                // Threads démons : ne doivent jamais empêcher l'arrêt de l'application.
                thread.setDaemon(true);
                return thread;
            }
        };

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());
        // Permet aux 24 threads du cœur d'expirer après une période
        // d'inactivité (KEEP_ALIVE_SECONDS) plutôt que de rester en
        // permanence, tout en gardant la capacité disponible immédiatement
        // lors des pics.
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
