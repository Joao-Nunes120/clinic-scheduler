package pt.clinic.distributor.config;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pt.clinic.distributor.distribution.NodeState;

@Configuration
public class NodeConfig {

    @Bean
    public List<NodeState> nodes(
            @Value("${distributor.nodes.node1.url}") String node1Url,
            @Value("${distributor.nodes.node1.capacity}") double node1Capacity,
            @Value("${distributor.nodes.node2.url}") String node2Url,
            @Value("${distributor.nodes.node2.capacity}") double node2Capacity,
            @Value("${distributor.nodes.node3.url}") String node3Url,
            @Value("${distributor.nodes.node3.capacity}") double node3Capacity
    ) {
        return List.of(
                new NodeState("node1", URI.create(node1Url), node1Capacity),
                new NodeState("node2", URI.create(node2Url), node2Capacity),
                new NodeState("node3", URI.create(node3Url), node3Capacity)
        );
    }
}