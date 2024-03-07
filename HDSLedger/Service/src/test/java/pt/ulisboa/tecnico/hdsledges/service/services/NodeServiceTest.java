import pt.ulisboa.tecnico.hdsledger.service.Node;

import org.mockito.Mockito;
import org.mockito.Mock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.times;
import org.mockito.MockitoAnnotations;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.*;

import java.beans.Transient;
import java.util.Arrays;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.atLeastOnce;

import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeService;
import pt.ulisboa.tecnico.hdsledger.service.models.InstanceInfo;
import pt.ulisboa.tecnico.hdsledger.communication.PrePrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ClientMessage;

@ExtendWith(MockitoExtension.class)
public class NodeServiceTest {

    private static NodeService leaderNodeService;
    private static NodeService nodeService;
    private static NodeService byzantineNode;

    private static String nodesConfigPath = "src/test/resources/test_config.json";
    private static String clientsConfigPath = "src/test/resources/client_test_config.json";
    private static String nodeId = "node";
    private static String leaderId = "leader";
    private static String byzantineNodeId = "byzantineNode";
    private static String clientId = "client1";

    // Creating mock objects
    @Mock
    private Link mockNodeLink;

    @Mock
    private Link mockLeaderLink;

    @Mock
    private Link mockbyzantineNodeLink;

    @Mock
    private ProcessConfig mockProcessConfig;

    @Mock
    private ConsensusMessage mockConsensusMessage;

    @Mock
    private ClientMessage mockClientMessage;

    @Mock
    private PrePrepareMessage mockPrePrepareMessage;

    @BeforeEach
    public void setUp() {
        leaderNodeService = nodeSetup(leaderId, mockLeaderLink);
        nodeService = nodeSetup(nodeId, mockNodeLink);
        byzantineNode = nodeSetup(byzantineNodeId, mockbyzantineNodeLink);
    }

    public NodeService nodeSetup(String id, Link link) {
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

        // Add clients configs to the link, so node can send messages to clients
        link.addClient(clientConfigs);

        // Services that implement listen from UDPService
        return new NodeService(link, nodeConfig, leaderConfig,
                nodeConfigs);
    }

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    // Test that the uponPrePrepare method sends a PREPARE message upon receiving a
    // PRE-PREPARE message from the leader
    // TODO check that sends ACK on valid PRE-PREPARE
    // TODO check that sends IGNORE on invalid PRE-PREPARE
    @Test
    public void test_uponPrePrepare_leader() {
        // Mock that sender is leader
        when(mockConsensusMessage.getSenderId()).thenReturn(leaderId);
        when(mockConsensusMessage.deserializePrePrepareMessage()).thenReturn(mockPrePrepareMessage);
        when(mockPrePrepareMessage.getValue()).thenReturn("val");
        // Call method
        leaderNodeService.uponPrePrepare(mockConsensusMessage);

        // Verify that uponPrePrepare broadcasts a message
        verify(mockLeaderLink, times(1)).broadcast(argThat(argument -> {
            // Check if the broadcasted message is a PREPARE message
            if (argument instanceof ConsensusMessage) {
                ConsensusMessage consensusMessage = (ConsensusMessage) argument;
                return consensusMessage.getType() == Message.Type.PREPARE;
            }
            return false;
        }));
    }

    // Test that the uponPrePrepare method does not send a PREPARE message upon
    // receiving PRE-PREPARE message from a non-leader
    @Test
    public void test_uponPrePrepare_nonLeader() {
        // Mock that sender is not leader
        when(mockConsensusMessage.getSenderId()).thenReturn(nodeId);
        when(mockConsensusMessage.deserializePrePrepareMessage()).thenReturn(mockPrePrepareMessage);
        when(mockPrePrepareMessage.getValue()).thenReturn("val");

        // Call method
        nodeService.uponPrePrepare(mockConsensusMessage);

        // Verify that uponPrePrepare does not broadcast any message
        verify(mockNodeLink, times(0)).broadcast(any());
    }

    // Test that startConsensus initiates a new
    // consensus instance, updates the relevant internal data structures, and
    // broadcasts the necessary messages when the node is the leader.
    @Test
    public void test_handleClientRequest_leader() {
        when(mockClientMessage.getSenderId()).thenReturn(clientId);
        when(mockClientMessage.getValue()).thenReturn("val");

        // Call method
        leaderNodeService.handleClientRequest(mockClientMessage);

        // Verify that startConsensus broadcasts a PRE-PREPARE message
        verify(mockLeaderLink, times(1)).broadcast(argThat(argument -> {
            // Check if the broadcasted message is a PRE-PREPARE message
            if (argument instanceof ConsensusMessage) {
                ConsensusMessage prePrepareMessage = (ConsensusMessage) argument;
                return prePrepareMessage.getType() == Message.Type.PRE_PREPARE;
            }
            return false;
        }));
    }

    // Test that handleClientRequest does not start a new consensus instance when
    // the node is not the leader.
    @Test
    public void test_handleClientRequest_notLeader() {
        // Call method
        nodeService.handleClientRequest(mockClientMessage);

        // Verify that handleClientRequest does not start a new consensus instance
        verify(mockNodeLink, times(0)).broadcast(any());
    }

    @Test
    public void test_incorrectMessage() {
        ConsensusMessage incorrectMessage = new ConsensusMessage(byzantineNodeId, Message.Type.PRE_PREPARE);

        nodeService.listen(); // Set normal node to listen

    }

    // TODO verify that uponCommit sends a CLIENT_CONFIRMATION message when
    // consensus is achieved

    // TODO Verify appropriate handling of invalid messages

    // TODO Ensure algorithm can still reach consensus even if some nodes behave
    // byzantinely

    // TODO Verify that ledger is consistent among all nodes after consensus is
    // reached

    // TODO test concurrent client requests

    // TODO test node crashes during consensus

    // TODO test leader crashes during consensus

    // TODO test timeout handling for consensus

}
