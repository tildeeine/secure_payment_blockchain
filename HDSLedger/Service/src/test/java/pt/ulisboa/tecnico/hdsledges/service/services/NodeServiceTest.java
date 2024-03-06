import pt.ulisboa.tecnico.hdsledger.service.Node;

import org.mockito.Mockito;
import org.mockito.Mock;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.*;

import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeService;
import pt.ulisboa.tecnico.hdsledger.service.models.InstanceInfo;

public class NodeServiceTest {

    // Creating mock objects
    @Mock
    private Link mockLink;

    @Mock
    private ProcessConfig mockConfig;

    @Mock
    private ConsensusMessage mockMessage;

    @InjectMocks
    private NodeService nodeService;

    // Test that the uponPrePrepare method sends a PREPARE message upon receiving a
    // PRE-PREPARE message from the leader
    @Test
    public void test_uponPrePrepare_sendsPrepareUponReceivingLeader() {
        // Mock that sender is leader
        when(nodeService.isLeader(Mockito.anyString())).thenReturn(true);

        // Call method
        nodeService.uponPrePrepare(mockMessage);

        // Verify that uponPrePrepare broadcasts a message
        verify(mockLink, times(1)).broadcast(argThat(argument -> {
            // Check if the broadcasted message is a PREPARE message
            if (argument instanceof ConsensusMessage) {
                ConsensusMessage prepareMessage = (ConsensusMessage) argument;
                return prepareMessage.getType() == Message.Type.PREPARE;
            }
            return false;
        }));
    }

    // Test that the uponPrePrepare method does not send a PREPARE message upon
    // receiving PRE-PREPARE message from a non-leader
    @Test
    public void test_uponPrePrepare_doesNotSendPrepareUponReceivingNonLeader() {
        // Mock that sender is not leader
        when(nodeService.isLeader(Mockito.anyString())).thenReturn(false);

        // Call method
        nodeService.uponPrePrepare(mockMessage);

        // Verify that uponPrePrepare does not broadcast a message
        verify(mockLink, times(0)).broadcast(any());
    }

    // Test that startConsensus initiates a new
    // consensus instance, updates the relevant internal data structures, and
    // broadcasts the necessary messages when the node is the leader.
    @Test
    public void test_startConsensus_leader() {
        // Mock that sender is leader
        when(nodeService.isLeader(Mockito.anyString())).thenReturn(true);
        // Mock that there has been no consensus instance for the round
        when(nodeService.getInstanceInfo().put(Mockito.anyInt(), any(InstanceInfo.class))).thenReturn(null);
        // Mock that previous consensus instance has been decided
        when(nodeService.getConsensusInstance().incrementAndGet()).thenReturn(2);
        when(nodeService.getLastDecidedConsensusInstance()).thenReturn(1);

        // Call method
        nodeService.startConsensus("val");

        // Verify that startConsensus initiates a new consensus instance and broadcasts
        // PRE-PREPARE message
        verify(mockLink, times(1)).broadcast(argThat(argument -> {
            // Check if the broadcasted message is a PRE-PREPARE message
            if (argument instanceof ConsensusMessage) {
                ConsensusMessage prePrepareMessage = (ConsensusMessage) argument;
                return prePrepareMessage.getType() == Message.Type.PRE_PREPARE;
            }
            return false;
        }));
    }

    // Test that startConsensus does not initiate a new consensus instance when the
    // sending node is not the leader.
    @Test
    public void test_startConsensus_notLeader() {
        // Mock that sender is not the leader
        when(nodeService.isLeader(Mockito.anyString())).thenReturn(false);
        // Mock that there has been no consensus instance for the round
        when(nodeService.getInstanceInfo().put(Mockito.anyInt(), any(InstanceInfo.class))).thenReturn(null);
        // Mock that previous consensus instance has been decided
        when(nodeService.getConsensusInstance().incrementAndGet()).thenReturn(2);
        when(nodeService.getLastDecidedConsensusInstance()).thenReturn(1);

        // Call method
        nodeService.startConsensus("val");

        // Verify that startConsensus does not initiate a new consensus instance
        verify(mockLink, times(0)).broadcast(any());
    }

    // Test that startConsensus does not initiate a new consensus if there has
    // already been a consensus instance for the round
    @Test
    public void test_startConsensus_alreadyConsensusInstance() {
        // Mock that sender is leader
        when(nodeService.isLeader(Mockito.anyString())).thenReturn(true);
        // Mock that there has been a consensus instance for the round
        when(nodeService.getInstanceInfo().put(Mockito.anyInt(), any(InstanceInfo.class)))
                .thenReturn(new InstanceInfo("val"));

        // Mock that previous consensus instance has been decided
        when(nodeService.getConsensusInstance().incrementAndGet()).thenReturn(2);
        when(nodeService.getLastDecidedConsensusInstance()).thenReturn(1);

        // Call method
        nodeService.startConsensus("val");

        // Verify that startConsensus does not initiate a new consensus instance
        verify(mockLink, times(0)).broadcast(any());
    }

    // Test that startConsensus does not initiate a new consensus if the previous
    // round was not decided yet
    // @Test // ! Modify this, while true loop
    // public void test_startConsensus_previousRoundNotDecided() {
    // // Mock that sender is leader
    // when(nodeService.isLeader()).thenReturn(true);
    // // Mock that there has been no consensus instance for the round
    // when(nodeService.getInstanceInfo().put(eq(localConsensusInstance),
    // any(InstanceInfo.class))).thenReturn(null);
    // // Mock that previous consensus instance has not been decided
    // when(nodeService.getLastDecidedConsensusInstance()).thenReturn(localConsensusInstance
    // - 2);

    // // Call method
    // nodeService.startConsensus();

    // // Verify that startConsensus does not initiate a new consensus instance
    // verify(mockLink, times(0)).broadcast(any());
    // }

    // Test that handleClientRequest starts a new consensus instance when the
    // sending node is the leader, nad updates clientRequestID appropriately
    @Test
    public void test_handleClientRequest() {
        // Mock that we are the leader and client ID is 0
        when(nodeService.isLeader(Mockito.anyString())).thenReturn(true);
        when(mockMessage.getSenderId()).thenReturn("0");
        // Mock that there has been no consensus instance for the round
        when(nodeService.getInstanceInfo().put(Mockito.anyInt(), any(InstanceInfo.class))).thenReturn(null);
        // Mock that previous consensus instance has been decided
        when(nodeService.getConsensusInstance().incrementAndGet()).thenReturn(2);
        when(nodeService.getLastDecidedConsensusInstance()).thenReturn(1);

        // Call method
        nodeService.handleClientRequest(mockMessage);

        // Verify that handleClientRequest starts a new consensus instance and updates
        // the clientRequestID
        verify(mockLink, times(1)).broadcast(argThat(argument -> {
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
        // Mock not the leader
        when(nodeService.isLeader(Mockito.anyString())).thenReturn(false);

        // Call method
        nodeService.handleClientRequest(mockMessage);

        // Verify that handleClientRequest does not start a new consensus instance
        verify(mockLink, times(0)).broadcast(any());
    }

    // Test the listen() method - complications due to threaded nature of the method
    @Test
    public void test_listen() {
        // Arrange: Set up the mock entities for test scenario
        when(nodeService.isLeader(Mockito.anyString())).thenReturn(true);

        // Call method
        nodeService.listen();

        // ! Add some more "actual" testing here, to see that the correct methods are
        // called in response to the different message types
    }
}
