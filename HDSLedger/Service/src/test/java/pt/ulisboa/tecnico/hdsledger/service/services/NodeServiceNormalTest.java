package pt.ulisboa.tecnico.hdsledger.service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.mockito.Mock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.times;
import org.mockito.MockitoAnnotations;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.*;
import org.mockito.Spy;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.KeyFactory;
import java.io.FileInputStream;
import java.io.IOException;

import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.PrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.builder.ConsensusMessageBuilder;
import pt.ulisboa.tecnico.hdsledger.communication.ClientData;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;
import pt.ulisboa.tecnico.hdsledger.service.models.InstanceInfo;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.PrePrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ClientMessage;
import pt.ulisboa.tecnico.hdsledger.communication.RoundChangeMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.Authenticate;

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

    private static Link linkSpy;

    private static PrivateKey clientKey;

    @Mock
    private ClientData mockClientData;

    @BeforeAll
    public static void setUpAll() {
        try {
            getClientPrivateKey();
        } catch (IOException e) {
            System.out.println("Error reading client private key");
        }
    }

    public static void getClientPrivateKey() throws IOException {
        try {
            String keyPath = "../Utilities/keys/key" + clientId + ".priv";
            FileInputStream fis = new FileInputStream(keyPath);
            byte[] encoded = new byte[fis.available()];
            fis.read(encoded);
            fis.close();

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory keyFac = KeyFactory.getInstance("RSA");
            clientKey = keyFac.generatePrivate(spec);
        } catch (Exception e) {
            System.out.println("Error reading client private key");
        }
    }

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

        linkSpy = Mockito.spy(new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs, ConsensusMessage.class));

        // Add clients configs to the link, so node can send messages to clients
        linkSpy.addClient(clientConfigs);

        return new TestableNodeService(linkSpy, nodeConfig, leaderConfig,
                nodeConfigs);
    }

    @AfterEach
    void tearDown() {
        nodeService.shutdown();
        Mockito.reset(linkSpy);

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

        // Set current round to target round and find the next leader
        instance.setCurrentRound(targetRound);
        String nextLeaderId = String.valueOf(((instance.getCurrentRound() - 1) % nodeConfigs.length + 1));

        // Act
        nodeService.updateLeader();

        assertEquals(true, nodeService.isLeader(nextLeaderId));
    }

    // Test that commit is only sent if there is a valid quorum for the prepare
    // messages
    @Test
    void testCommitOnValidQuorum() {
        // Test setup
        // Make enough prepare messages to reach quorum (2f+1)
        // Get number of nodes and calculate quorum
        int N = nodeConfigs.length;
        int f = (N - 1) / 3;
        int quorum = 3 * f + 1;

        // Set up client data //! Consider moving this to the testableNodeService class
        // as getter, for cleaner test code
        ClientData clientData = new ClientData();
        clientData.setRequestID(1); // arbitrary number
        clientData.setValue("Value");
        clientData.setClientID("client1");

        try {
            byte[] signature = Authenticate.signMessage(clientKey, clientData.getValue());
            clientData.setSignature(signature);
        } catch (Exception e) {
            System.out.println("Error signing value");
        }

        // Set values for the prepare message
        int consensusInstance = nodeService.getConsensusInstance().get();
        int round = 1;
        PrepareMessage prepareMessage = new PrepareMessage(clientData);
        String senderMessageId = "1";

        // Make and send enough prepare messages to reach quorum (2f+1)
        for (int i = 0; i < quorum; i++) {
            // Create prepare message
            ConsensusMessage consensusMessage = new ConsensusMessageBuilder(String.valueOf(i + 1), Message.Type.PREPARE)
                    .setConsensusInstance(consensusInstance)
                    .setRound(round)
                    .setMessage(prepareMessage.toJson())
                    .build();

            nodeService.uponPrepare(consensusMessage);

        }

        // Verify that nodeService sends quorum commit messages
        verify(linkSpy, times(quorum)).send(anyString(), argThat(argument -> argument instanceof ConsensusMessage &&
                ((ConsensusMessage) argument).getType() == Message.Type.COMMIT));

        // verify(linkSpy, times(1)).send(anyString(), any()); // ! Doesn't work, no
        // interaction with linkSpy
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
