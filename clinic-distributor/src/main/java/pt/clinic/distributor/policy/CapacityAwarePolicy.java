package pt.clinic.distributor.policy;

import java.util.List;

import pt.clinic.distributor.node.NodeState;

public class CapacityAwarePolicy implements NodeSelectionPolicy {

    @Override
    public NodeState selectNode(List<NodeState> nodes) {
        if (nodes.isEmpty()) {
            throw new IllegalStateException(
                    "Não existem nós disponíveis."
            );
        }

        NodeState selectedNode = nodes.getFirst();
        double lowestScore = calculateScore(selectedNode);

        for (int i = 1; i < nodes.size(); i++) {
            NodeState candidate = nodes.get(i);
            double candidateScore = calculateScore(candidate);

            if (candidateScore < lowestScore) {
                selectedNode = candidate;
                lowestScore = candidateScore;
            }
        }

        return selectedNode;
    }

    private double calculateScore(NodeState node) {
        // Fórmula: (pedidos pendentes + 1) / capacidade medida.
        // O +1 representa o pedido que estamos prestes a atribuir.
        // Escolhemos o nó com a menor pontuação.
        return (node.getPendingRequests() + 1.0) / node.getCapacity();
    }
}