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

@ExtendWith(MockitoExtension.class)
public class NodeServiceTest {

    private static NodeService leaderNodeService;
    private static NodeService nodeService;

    private static String nodesConfigPath = "src/test/resources/test_config.json";
    private static String clientsConfigPath = "src/test/resources/client_test_config.json";
    private static String nodeId = "2";
    private static String leaderId = "1";

    // Creating mock objects
    @Mock
    private Link mockNodeLink;

    @Mock
    private Link mockLeaderLink;

    @Mock
    private ProcessConfig mockProcessConfig;

    @Mock
    private ConsensusMessage mockMessage;

    @Mock
    private PrePrepareMessage mockPrePrepareMessage;

    @BeforeEach
    public void setUp() {
        leaderNodeService = nodeSetup(leaderId, mockLeaderLink);
        nodeService = nodeSetup(nodeId, mockNodeLink);
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
    @Test
    public void test_uponPrePrepare_sendsPrepareUponReceivingLeader() {
        // Mock that sender is leader
        when(mockMessage.getSenderId()).thenReturn(leaderId);
        when(mockMessage.deserializePrePrepareMessage()).thenReturn(mockPrePrepareMessage);
        when(mockPrePrepareMessage.getValue()).thenReturn("val");
        // Call method
        System.out.println(leaderNodeService.getLink());// ! testing

        leaderNodeService.uponPrePrepare(mockMessage);

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

    // // Test that the uponPrePrepare method does not send a PREPARE message upon
    // // receiving PRE-PREPARE message from a non-leader
    // @Test
    // public void test_uponPrePrepare_doesNotSendPrepareUponReceivingNonLeader() {
    // // Mock that sender is not leader
    // when(mockMessage.getSenderId()).thenReturn(nodeId);

    // // Call method
    // nodeService.uponPrePrepare(mockMessage);

    // // Verify that uponPrePrepare does not broadcast a message
    // verify(mockLink, times(0)).broadcast(any());
    // }

    // // Test that startConsensus initiates a new
    // // consensus instance, updates the relevant internal data structures, and
    // // broadcasts the necessary messages when the node is the leader.
    // @Test
    // public void test_startConsensus_leader() {
    // // Mock that sender is leader
    // when(mockMessage.getSenderId()).thenReturn(leaderId);
    // // Mock that there has been no consensus instance for the round
    // // when(nodeService.getInstanceInfo().put(Mockito.anyInt(),
    // // any(InstanceInfo.class))).thenReturn(null); //! Can't mock nodeservice
    // // // Mock that previous consensus instance has been decided
    // // when(nodeService.getConsensusInstance().incrementAndGet()).thenReturn(2);
    // // when(nodeService.getLastDecidedConsensusInstance()).thenReturn(1);

    // // Call method
    // nodeService.startConsensus("val");

    // // Verify that startConsensus initiates a new consensus instance and
    // broadcasts
    // // PRE-PREPARE message
    // verify(mockLink, times(1)).broadcast(argThat(argument -> {
    // // Check if the broadcasted message is a PRE-PREPARE message
    // if (argument instanceof ConsensusMessage) {
    // ConsensusMessage prePrepareMessage = (ConsensusMessage) argument;
    // return prePrepareMessage.getType() == Message.Type.PRE_PREPARE;
    // }
    // return false;
    // }));
    // }

    // // Test that startConsensus does not initiate a new consensus instance when
    // the
    // // sending node is not the leader.
    // @Test
    // public void test_startConsensus_notLeader() {
    // // Mock that sender is not the leader
    // when(mockMessage.getSenderId()).thenReturn(nodeId);
    // // when(nodeService.getInstanceInfo().put(Mockito.anyInt(),
    // // any(InstanceInfo.class))).thenReturn(null);
    // // Mock that previous consensus instance has been decided //! Can not mock
    // // nodeservice
    // // when(nodeService.getConsensusInstance().incrementAndGet()).thenReturn(2);
    // // when(nodeService.getLastDecidedConsensusInstance()).thenReturn(1);

    // // Call method
    // nodeService.startConsensus("val");

    // // Verify that startConsensus does not initiate a new consensus instance
    // verify(mockLink, times(0)).broadcast(any());
    // }

    // // Test that startConsensus does not initiate a new consensus if there has
    // // already been a consensus instance for the round
    // @Test
    // public void test_startConsensus_alreadyConsensusInstance() {
    // // Mock that sender is leader
    // when(mockMessage.getSenderId()).thenReturn(leaderId);
    // // when(nodeService.isLeader("2")).thenReturn(true); // Mock that there has
    // been
    // // a consensus instance for the round //! Can't mock nodeservice
    // // when(nodeService.getInstanceInfo().put(Mockito.anyInt(),
    // // any(InstanceInfo.class)))
    // // .thenReturn(new InstanceInfo("val"));

    // // // Mock that previous consensus instance has been decided
    // // when(nodeService.getConsensusInstance().incrementAndGet()).thenReturn(2);
    // // when(nodeService.getLastDecidedConsensusInstance()).thenReturn(1);

    // // Call method
    // nodeService.startConsensus("val");

    // // Verify that startConsensus does not initiate a new consensus instance
    // verify(mockLink, times(0)).broadcast(any());
    // }

    // // Test that startConsensus does not initiate a new consensus if the previous
    // // round was not decided yet

    // // Test that handleClientRequest starts a new consensus instance when the
    // // sending node is the leader, nad updates clientRequestID appropriately
    // @Test
    // public void test_handleClientRequest() {
    // // Mock that we are the leader and client ID is 0
    // // when(mockProcessConfig.isLeader()).thenReturn(true);
    // when(mockMessage.getSenderId()).thenReturn("0");
    // // Mock that there has been no consensus instance for the round
    // // when(nodeService.getInstanceInfo().put(Mockito.anyInt(),
    // // any(InstanceInfo.class))).thenReturn(null); //! Can't mock nodeservice
    // // // Mock that previous consensus instance has been decided
    // // when(nodeService.getConsensusInstance().incrementAndGet()).thenReturn(2);
    // // when(nodeService.getLastDecidedConsensusInstance()).thenReturn(1);

    // // Call method
    // nodeService.handleClientRequest(mockMessage);

    // // Verify that handleClientRequest starts a new consensus instance and
    // updates
    // // the clientRequestID
    // verify(mockLink, times(1)).broadcast(argThat(argument -> {
    // // Check if the broadcasted message is a PRE-PREPARE message
    // if (argument instanceof ConsensusMessage) {
    // ConsensusMessage prePrepareMessage = (ConsensusMessage) argument;
    // return prePrepareMessage.getType() == Message.Type.PRE_PREPARE;
    // }
    // return false;
    // }));
    // }

    // // Test that handleClientRequest does not start a new consensus instance when
    // // the node is not the leader.
    // @Test
    // public void test_handleClientRequest_notLeader() {
    // // Mock not the leader
    // when(mockMessage.getSenderId()).thenReturn(nodeId);

    // // Call method
    // nodeService.handleClientRequest(mockMessage);

    // // Verify that handleClientRequest does not start a new consensus instance
    // verify(mockLink, times(0)).broadcast(any());
    // }

}
