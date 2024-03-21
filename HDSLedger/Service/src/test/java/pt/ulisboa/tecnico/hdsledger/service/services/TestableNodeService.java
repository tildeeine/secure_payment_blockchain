package pt.ulisboa.tecnico.hdsledger.service.services;

import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class TestableNodeService extends NodeService {

    private Link link;

    public TestableNodeService(Link link, ProcessConfig nodeConfig, ProcessConfig leaderConfig,
            ProcessConfig[] nodesConfig) {
        // Add clients configs to the link, so node can send messages to clients
        super(link, nodeConfig, leaderConfig, nodesConfig);
        this.link = link;
    }

    // Override methods as necessary to accommodate testing without real network
    // communication

    // Add a shutdown method to allow for cleanup after tests
    public void shutdown() {
        this.link.shutdown();
    }
}