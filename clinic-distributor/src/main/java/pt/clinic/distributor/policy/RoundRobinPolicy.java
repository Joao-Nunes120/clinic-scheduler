package pt.clinic.distributor.policy;

import java.util.List;

import pt.clinic.distributor.node.NodeState;

public class RoundRobinPolicy implements NodeSelectionPolicy {

    private int nextIndex = 0;

    @Override
    public NodeState selectNode(List<NodeState> nodes) {
        if (nodes.isEmpty()) {
            throw new IllegalStateException(
                    "Não existem nós disponíveis."
            );
        }

        NodeState selectedNode = nodes.get(nextIndex);
        nextIndex = (nextIndex + 1) % nodes.size();

        return selectedNode;
    }
}