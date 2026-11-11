package pt.clinic.distributor.service;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import pt.clinic.distributor.node.NodeHttpClient;
import pt.clinic.distributor.node.NodeState;
import pt.clinic.distributor.policy.NodeSelectionPolicy;

@Service
public class DistributionService {

    private final List<NodeState> nodes;
    private final NodeSelectionPolicy policy;
    private final NodeHttpClient nodeHttpClient;
    private final ThreadPoolExecutor executor;

    private final LongAdder assignments = new LongAdder();
    private final LongAdder rejections = new LongAdder();
    private final LongAdder queueWaitNanos = new LongAdder();
    private final LongAdder decisionNanos = new LongAdder();

    public DistributionService(
            @Qualifier("nodes") List<NodeState> nodes,
            NodeSelectionPolicy policy,
            NodeHttpClient nodeHttpClient,
            @Qualifier("distributorExecutor") ThreadPoolExecutor executor
    ) {
        this.nodes = List.copyOf(nodes);
        this.policy = policy;
        this.nodeHttpClient = nodeHttpClient;
        this.executor = executor;
    }

    public CompletableFuture<HttpResponse<String>> get(
            String pathAndQuery
    ) {
        return dispatch(
                node -> nodeHttpClient.get(node, pathAndQuery)
        );
    }

    public CompletableFuture<HttpResponse<String>> post(
            String path,
            String jsonBody
    ) {
        return dispatch(
                node -> nodeHttpClient.post(node, path, jsonBody)
        );
    }

    private CompletableFuture<HttpResponse<String>> dispatch(
            Function<NodeState, CompletableFuture<HttpResponse<String>>> operation
    ) {
        long submittedAt = System.nanoTime();

        try {
            return CompletableFuture
                    .supplyAsync(
                            () -> selectAndRegisterNode(submittedAt),
                            executor
                    )
                    .thenCompose(node -> sendToNode(node, operation));

        } catch (RejectedExecutionException exception) {
            rejections.increment();
            return CompletableFuture.failedFuture(exception);
        }
    }

    private NodeState selectAndRegisterNode(long submittedAt) {
        long decisionStartedAt = System.nanoTime();

        // Tempo desde a submissão até ao início da atribuição.
        queueWaitNanos.add(decisionStartedAt - submittedAt);

        NodeState selectedNode = policy.selectNode(nodes);
        selectedNode.incrementPendingRequests();

        // Tempo necessário para escolher e registar o nó.
        decisionNanos.add(System.nanoTime() - decisionStartedAt);
        assignments.increment();

        return selectedNode;
    }

    private CompletableFuture<HttpResponse<String>> sendToNode(
            NodeState node,
            Function<NodeState, CompletableFuture<HttpResponse<String>>> operation
    ) {
        try {
            return operation.apply(node)
                    .whenComplete((response, error) -> {
                        // Executado tanto com resposta como com erro.
                        node.decrementPendingRequests();
                    });

        } catch (RuntimeException exception) {
            // Trata erros que acontecem antes de existir um future HTTP.
            node.decrementPendingRequests();
            return CompletableFuture.failedFuture(exception);
        }
    }

    public Metrics getMetrics() {
        long count = assignments.sum();

        double averageQueueWaitMs = count == 0
                ? 0.0
                : queueWaitNanos.sum() / (double) count / 1_000_000.0;

        double averageDecisionMs = count == 0
                ? 0.0
                : decisionNanos.sum() / (double) count / 1_000_000.0;

        return new Metrics(
                count,
                rejections.sum(),
                executor.getQueue().size(),
                averageQueueWaitMs,
                averageDecisionMs
        );
    }

    public record Metrics(
            long assignedRequests,
            long rejectedRequests,
            int queuedRequests,
            double averageQueueWaitMs,
            double averageDecisionMs
    ) {
    }
}