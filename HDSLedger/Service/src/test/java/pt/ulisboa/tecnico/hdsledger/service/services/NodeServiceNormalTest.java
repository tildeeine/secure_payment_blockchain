package pt.ulisboa.tecnico.hdsledger.service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.mockito.Mock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.times;
import org.mockito.MockitoAnnotations;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ClientData;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;
import pt.ulisboa.tecnico.hdsledger.service.models.InstanceInfo;

@ExtendWith(MockitoExtension.class)
class NodeServiceNormalTest {
    // Class to test normal functioning of NodeService

    // Current strategy: Use mockito to mock the network communication for the most
    // basic tests, like testing that the correct message is sent at the correct
    // time.
    // Use Junit5 to test the more overall system logic, not small components, like
    // checking that the correct consensus is reached

    private TestableNodeService nodeService;

    private static String nodesConfigPath = "src/test/resources/test_config.json";
    private static String clientsConfigPath = "src/test/resources/client_test_config.json";

    private ProcessConfig[] nodeConfigs;
    private ProcessConfig[] clientConfigs;

    private static String leaderId = "1";
    private static String testNodeId = "2";
    private static String byzantineNodeId = "3";
    private static String clientId = "client1";

    @Mock
    private Link mockNodeLink;

    @Mock
    private ClientData mockClientData;

    @BeforeEach
    void setUp() {
        // Instantiate TestableNodeService
        nodeService = nodeSetup(testNodeId);
        MockitoAnnotations.initMocks(this);
    }

    public TestableNodeService nodeSetup(String id) {
        nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
        clientConfigs = new ProcessConfigBuilder().fromFile(clientsConfigPath);
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
        nodeService.shutdown();
    }

    // Test that the node sends a PREPARE message when receiving a PRE-PREPARE
    @Test
    void testPrepareOnPrePrepare() {
        // Test logic
    }

    // Test that the node does not send a PREPARE message when receiving a
    // PRE-PREPARE from someone who is not the leader
    @Test
    void testNoPrepareOnPrePrepareFromNonLeader() {
        // Test logic
    }

    // Test that the node starts a new consensus if it is the leader
    @Test
    void testStartConsensusOnClientRequestFromLeader() {
        // Test logic
    }

    // Test that node does not start a new consensus if it is not the leader
    @Test
    void testNoStartConsensusOnClientRequestFromNonLeader() {
        // Test logic
    }

    // Test that the correct updateLeader is done
    // Desired result: Next leader is r mod N -> r = current leader + 1 (node)
    @Test
    void testUpdateLeader() {
        // Test setup: Ensure an InstanceInfo exists for the current consensus instance
        int initialConsensusInstance = nodeService.getConsensusInstance().get();

        InstanceInfo initialInstanceInfo = new InstanceInfo(mockClientData);
        initialInstanceInfo.setCurrentRound(1); // Assuming the initial round starts at 1
        nodeService.getInstanceInfo().put(initialConsensusInstance, initialInstanceInfo);

        InstanceInfo instance = nodeService.getInstanceInfo().get(initialConsensusInstance);

        // Test logic
        int targetRound = instance.getCurrentRound() + 1;

        // Set current round to target round
        instance.setCurrentRound(targetRound);

        // Assert that the next node in config is now leader
        String nextLeaderId = String.valueOf(((instance.getCurrentRound() - 1) % nodeConfigs.length + 1));

        // Act
        nodeService.updateLeader();

        assertEquals(true, nodeService.isLeader(nextLeaderId));
    }

    // Test that commit is only sent if there is a valid quorum for the prepare
    // messages
    @Test
    void testCommitOnValidQuorum() {
        // Test logic
    }

    // Test that commit is not sent if there is not a valid quorum for the prepare
    // messages
    @Test
    void testNoCommitOnInvalidQuorum() {
        // Test logic
    }

    // Test that client data that is not signed is not accepted
    @Test
    void testRejectUnsignedClientData() {
        // Test logic
    }

    // Test that client data that is signed is accepted
    @Test
    void testAcceptSignedClientData() {
        // Test logic
    }

}
