package pt.clinic.scheduler.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NodeExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolExecutor nodeExecutor(
            @Value("${node.workers:4}") int workers,
            @Value("${node.queue-capacity:100}") int queueCapacity) {

        if (workers <= 0 || queueCapacity <= 0) {
            throw new IllegalArgumentException(
                    "O número de workers e a capacidade da fila "
                            + "devem ser positivos.");
        }

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                workers,
                workers,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                Thread.ofPlatform()
                        .name("node-worker-", 1)
                        .factory(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        executor.prestartAllCoreThreads();

        return executor;
    }
}