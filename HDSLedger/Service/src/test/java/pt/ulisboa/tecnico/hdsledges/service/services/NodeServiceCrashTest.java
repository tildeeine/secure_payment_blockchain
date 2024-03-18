package test.java.pt.ulisboa.tecnico.hdsledges.service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import test.pt.ulisboa.tecnico.hdsledgers.service.services.TestableNodeService;

public class NodeServiceCrashTest {
    // Class to test byzantine behaviour
    private TestableNodeService nodeService;

    private static String nodesConfigPath = "src/test/resources/test_config.json";
    private static String clientsConfigPath = "src/test/resources/client_test_config.json";

    private static final String testNodeId = "testNodeId";
    private static String leaderId = "leader";
    private static String byzantineNodeId = "byzantineNode";
    private static String clientId = "client1";

    @BeforeEach
    void setUp() {
        // Instantiate TestableNodeService
        nodeSetup(testNodeId);
    }

    public TestableNodeService nodeSetup(String id) {
        ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
        ProcessConfig[] clientConfigs = new ProcessConfigBuilder().fromFile(clientsConfigPath);
        ProcessConfig leaderConfig = Arrays.stream(nodeConfigs)
                .filter(ProcessConfig::isLeader)
                .findAny()
                .get();
        ProcessConfig nodeConfig = Arrays.stream(nodeConfigs)
                .filter(c -> c.getId().equals(id))
                .findAny()
                .get();

        Link link = new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs,
                ConsensusMessage.class);
        // Add clients configs to the link, so node can send messages to clients
        link.addClient(clientConfigs);

        // New TestableNodeService
        return new TestableNodeService(link, nodeConfig, leaderConfig,
                nodeConfigs);
    }

    @AfterEach
    void tearDown() {
        // Perform cleanup tasks
        // For TestableNode, this might involve resetting static variables or
        // any external resources it might have altered.
    }

}
