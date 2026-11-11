package pt.clinic.distributor.policy;

import java.util.List;

import pt.clinic.distributor.node.NodeState;

public interface NodeSelectionPolicy {

    NodeState selectNode(List<NodeState> nodes);
}