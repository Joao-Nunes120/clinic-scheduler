package pt.clinic.distributor.node;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class NodeState {

    private final String id;
    private final URI baseUri;
    private final double capacity;

    private final AtomicInteger pendingRequests = new AtomicInteger(0);
    private final LongAdder assignedRequests = new LongAdder();

    public NodeState(String id, URI baseUri, double capacity) {
        if (!Double.isFinite(capacity) || capacity <= 0) {
            throw new IllegalArgumentException(
                    "A capacidade deve ser um número positivo."
            );
        }

        this.id = id;
        this.baseUri = baseUri;
        this.capacity = capacity;
    }

    public String getId() {
        return id;
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public double getCapacity() {
        return capacity;
    }

    public int getPendingRequests() {
        return pendingRequests.get();
    }

    public long getAssignedRequests() {
        return assignedRequests.sum();
    }

    public void incrementPendingRequests() {
        pendingRequests.incrementAndGet();
        assignedRequests.increment();
    }

    public void decrementPendingRequests() {
        pendingRequests.decrementAndGet();
    }
}