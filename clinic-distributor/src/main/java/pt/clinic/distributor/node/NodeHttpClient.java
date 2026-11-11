package pt.clinic.distributor.node;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;

@Component
public class NodeHttpClient {

    private final HttpClient httpClient;

    public NodeHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public CompletableFuture<HttpResponse<String>> get(
            NodeState node,
            String pathAndQuery
    ) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri(node, pathAndQuery))
                .timeout(Duration.ofSeconds(25))
                .header("Accept", "application/json")
                .GET()
                .build();

        return send(request);
    }

    public CompletableFuture<HttpResponse<String>> post(
            NodeState node,
            String path,
            String jsonBody
    ) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri(node, path))
                .timeout(Duration.ofSeconds(25))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        jsonBody,
                        StandardCharsets.UTF_8
                ))
                .build();

        return send(request);
    }

    private CompletableFuture<HttpResponse<String>> send(
            HttpRequest request
    ) {
        return httpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private URI buildUri(NodeState node, String pathAndQuery) {
        return URI.create(node.getBaseUri().toString() + pathAndQuery);
    }
}