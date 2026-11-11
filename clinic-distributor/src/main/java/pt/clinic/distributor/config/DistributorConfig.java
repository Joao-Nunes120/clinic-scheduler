package pt.clinic.distributor.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pt.clinic.distributor.policy.CapacityAwarePolicy;
import pt.clinic.distributor.policy.NodeSelectionPolicy;
import pt.clinic.distributor.policy.RoundRobinPolicy;

@Configuration
public class DistributorConfig {

    @Bean
    public NodeSelectionPolicy nodeSelectionPolicy(
            @Value("${distributor.policy}") String policy
    ) {
        return switch (policy) {
            case "round-robin" -> new RoundRobinPolicy();
            case "capacity-aware" -> new CapacityAwarePolicy();
            default -> throw new IllegalArgumentException(
                    "Política desconhecida: " + policy
            );
        };
    }

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolExecutor distributorExecutor(
            @Value("${distributor.policy}") String policy,
            @Value("${distributor.workers}") int workers,
            @Value("${distributor.queue-capacity}") int queueCapacity
    ) {
        if (workers <= 0 || queueCapacity <= 0) {
            throw new IllegalArgumentException(
                    "O número de workers e a capacidade da fila devem ser positivos."
            );
        }

        if ("round-robin".equals(policy) && workers != 1) {
            throw new IllegalArgumentException(
                    "O round-robin deve usar uma única thread de atribuição."
            );
        }

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                workers,
                workers,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                Thread.ofPlatform()
                        .name("distributor-worker-", 1)
                        .factory(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        executor.prestartAllCoreThreads();
        return executor;
    }
}