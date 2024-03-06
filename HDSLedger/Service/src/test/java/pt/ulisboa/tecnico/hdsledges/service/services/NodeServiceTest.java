import pt.ulisboa.tecnico.hdsledger.service.Node;

import org.mockito.Mockito;
import org.junit.jupiter.api.Test;
import static org.junit.Assert.*;
import org.mockito.InjectMocks;

import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeService;

public class NodeServiceTest {

    private static final String leaderNodeId = "1";

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
    // PRE-PREPARE message
    @Test
    public void test_uponPrePrepare_sendsPrepareUponReceiving() {
        // Arrange: Set up the mock entities for test scenario
        when(mockMessage.getConsensusInstance()).thenReturn(1);
        when(mockMessage.getRound()).thenReturn(1);
        when(mockMessage.getSenderId()).thenReturn(leaderNodeId);
        // when(mockMessage.deserializePrePrepareMessage()).thenReturn(new
        // PrePrepareMessage("testValue")); // ! set
        // // correct
        // // value
        when(mockConfig.getId()).thenReturn(leaderNodeId);

        // Call method
        nodeService.uponPrePrepare(mockMessage);

        // Verify that uponPrePrepare broadcasts a PREPARE message
        verify(mockLink, times(1)).broadcast(argThat(argument -> {
            // Check if the broadcasted message is a PREPARE message
            if (argument instanceof ConsensusMessage) {
                ConsensusMessage prepareMessage = (ConsensusMessage) argument;
                return prepareMessage.getType() == Message.Type.PREPARE;
            }
            return false;
        }));
    }

    // Test that startConsensus initiates a new
    // consensus instance, updates the relevant internal data structures, and
    // broadcasts the necessary messages when the node is the leader.
    @Test
    public void test_startConsensus_leader() {
        // Arrange: Set up the mock entities for test scenario
        when(mockConfig.getId()).thenReturn(leaderNodeId);

        // Call method
        nodeService.startConsensus();

        // Verify that startConsensus initiates a new consensus instance and broadcasts
        // the necessary messages
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
        // Arrange: Set up the mock entities for test scenario
        when(mockConfig.getId()).thenReturn("2");

        // Call method
        nodeService.startConsensus();

        // Verify that startConsensus does not initiate a new consensus instance
        verify(mockLink, times(0)).broadcast(any());
    }

    // Test that handleClientRequest starts a new consensus instance when the
    // sending node i sthe leader, nad updates clientRequestID appropriately
    @Test
    public void test_handleClientRequest() {
        // Arrange: Set up the mock entities for test scenario
        when(mockConfig.getId()).thenReturn(leaderNodeId);

        // Call method
        nodeService.handleClientRequest("testValue"); // ! Add correct value

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
        // Arrange: Set up the mock entities for test scenario
        when(mockConfig.getId()).thenReturn("2");

        // Call method
        nodeService.handleClientRequest("testValue"); // ! Add correct value

        // Verify that handleClientRequest does not start a new consensus instance
        verify(mockLink, times(0)).broadcast(any());
    }

    // Test the listen() method - complications due to threaded nature of the method
    @Test
    public void test_listen() {
        // Arrange: Set up the mock entities for test scenario
        when(mockConfig.getId()).thenReturn(leaderNodeId);

        // Call method
        nodeService.listen();

        // Verify that listen() method is called
        verify(mockLink, times(1)).listen();

        // ! Add some more "actual" testing here, to see that the correct methods are
        // called in response to the different message types
    }
}
