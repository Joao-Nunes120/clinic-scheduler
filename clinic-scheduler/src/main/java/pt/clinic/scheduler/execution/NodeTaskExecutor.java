package pt.clinic.scheduler.execution;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class NodeTaskExecutor {

    private final ThreadPoolExecutor executor;

    public NodeTaskExecutor(
            @Qualifier("nodeExecutor") ThreadPoolExecutor executor) {

        this.executor = executor;
    }

    public <T> CompletableFuture<T> submit(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }
}