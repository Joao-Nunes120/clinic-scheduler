package pt.clinic.distributor.distribution;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

public class NodeState {

    private final String id;
    private final URI baseUri;
    private final double capacity;
    private final AtomicInteger pendingRequests = new AtomicInteger(0);

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

    public void incrementPendingRequests() {
        pendingRequests.incrementAndGet();
    }

    public void decrementPendingRequests() {
        pendingRequests.decrementAndGet();
    }
}