package test.java.pt.ulisboa.tecnico.hdsledges.service.services;

public class TestableNode extends NodeService {
    public TestableNode(Link link, ProcessConfig nodeConfig, ProcessConfig leaderConfig, ProcessConfig[] nodeConfigs) {
        // Add clients configs to the link, so node can send messages to clients
        super(link, nodeConfig, leaderConfig, nodeConfigs);
    }

    // Override methods as necessary to accommodate testing without real network
    // communication
}