package pt.ulisboa.tecnico.hdsledger.service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.mockito.Mock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.times;
import org.mockito.MockitoAnnotations;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.*;
import org.mockito.Spy;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat.Type;

import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.KeyFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import com.google.gson.Gson;
import java.util.HashMap;

import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.PrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.builder.ConsensusMessageBuilder;
import pt.ulisboa.tecnico.hdsledger.communication.ClientData;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;
import pt.ulisboa.tecnico.hdsledger.service.models.InstanceInfo;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.PrePrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ClientMessage;
import pt.ulisboa.tecnico.hdsledger.communication.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.RoundChangeMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.Authenticate;
import pt.ulisboa.tecnico.hdsledger.service.blockchain.Block;
import pt.ulisboa.tecnico.hdsledger.service.blockchain.Blockchain;
import pt.ulisboa.tecnico.hdsledger.client.services.ClientService;
import pt.ulisboa.tecnico.hdsledger.client.models.ClientMessageBuilder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
public class DependabilityTest {
    private TestableNodeService nodeService;

    private static String nodesConfigPath = "src/test/resources/test_config.json";
    private static String clientsConfigPath = "src/test/resources/client_test_config.json";

    private ProcessConfig[] nodeConfigs;
    private ProcessConfig[] clientConfigs;
    private ProcessConfig leaderConfig;

    private static String leaderId = "1";
    private static String testNodeId = "2";
    private static String clientId = "client1";

    private static Link linkSpy;

    private static PrivateKey clientKey;

    private Map<String, TestableNodeService> allNodes = new HashMap<>();
    private ClientService clientService = null;

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
        nodeService = testNodeSetup(testNodeId);
        allNodes.put(testNodeId, nodeService);
        MockitoAnnotations.initMocks(this);
    }

    public TestableNodeService testNodeSetup(String id) {
        nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
        clientConfigs = new ProcessConfigBuilder().fromFile(clientsConfigPath);
        leaderConfig = Arrays.stream(nodeConfigs)
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

    public void setupAllNodes() {
        for (ProcessConfig nodeConfig : nodeConfigs) {
            if (nodeConfig.getId().equals(testNodeId)) {
                continue;
            }
            TestableNodeService node = nodeSetup(nodeConfig.getId());
            allNodes.put(nodeConfig.getId(), node);
        }
    }

    // Function to set up all nodes in the network, for complete testing
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

        Link link = new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs, ConsensusMessage.class);

        // Add clients configs to the link, so node can send messages to clients
        link.addClient(clientConfigs);

        return new TestableNodeService(link, nodeConfig, leaderConfig,
                nodeConfigs);

    }

    public void setupClient() {
        // Set up client service
        ProcessConfig clientConfig = Arrays.stream(clientConfigs)
                .filter(c -> c.getId().equals(clientId))
                .findAny()
                .get();
        Link link = new Link(clientConfig, clientConfig.getPort(), nodeConfigs, ConsensusMessage.class);

        clientService = new ClientService(link, clientConfig, leaderConfig, nodeConfigs);
        ClientMessageBuilder clientMessageBuilder = new ClientMessageBuilder(clientConfig);
        // Start a thread to listen for messages from nodes
        // Listen for incoming messages from nodes
        clientService.listen();

    }

    @AfterEach
    void tearDown() {
        if (allNodes != null) {
            for (TestableNodeService node : allNodes.values()) {
                System.out.println("Shutting down node " + node.getConfig().getId());
                node.shutdown();
            }
            allNodes.clear(); // Clear the map after all nodes have been shut down
        }
        if (clientService != null) {
            clientService.shutdown();
            clientService = null;
        }
        Mockito.reset(linkSpy);
    }

    public ClientData setupClientData(String value) {
        // Set up client data
        ClientData clientData = new ClientData();
        clientData.setRequestID(1); // arbitrary number
        clientData.setValue(value);
        clientData.setClientID("client1");

        try {
            byte[] signature = Authenticate.signMessage(clientKey, clientData.getValue());
            clientData.setSignature(signature);
        } catch (Exception e) {
            System.out.println("Error signing value");
        }
        return clientData;
    }

    // Test that a replayed message is not accepted for quorum
    @Test
    @Order(1)
    private void testRejectReplayedMessage() {
        System.out.println("Reject replayed message");

        ClientData clientData = setupClientData("20 client2 1");
        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(clientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.sendPrepareMessages(blockHash, nodeService.getQuorum());

        // Verify that the commit messages were sent
        verify(linkSpy, times(nodeService.getQuorum())).send(anyString(),
                argThat(argument -> argument instanceof ConsensusMessage
                        && ((ConsensusMessage) argument).getType() == Message.Type.COMMIT));

        // Reset the link spy to clear the interactions
        Mockito.reset(linkSpy);

        // Send the same prepare messages again
        nodeService.sendPrepareMessages(blockHash, nodeService.getQuorum());

        // Verify that no commit messages were sent
        verify(linkSpy, times(0)).send(anyString(), argThat(argument -> argument instanceof ConsensusMessage
                && ((ConsensusMessage) argument).getType() == Message.Type.COMMIT));
    }

    // Test a byzantine leader sending conflicting pre-prepare messages
    // Should result in a round change
    // Will generate some socket exceptions, ignore them
    @Test
    @Order(2)
    public void testConflictingLeaderPrePrepareMessages() {
        System.out.println("Testing Byzantine leader sending conflicting pre-prepare messages...");

        setupAllNodes();
        setupClient();

        // Get leader node
        TestableNodeService leader = allNodes.get(leaderId);

        // Simulate Byzantine leader sending conflicting pre-prepare messages
        int consensusInstance = nodeService.getConsensusInstance().get();
        int round = 2;

        // Create a PrepareMessage object. Assume leader is Byzantine and does not have
        // the correct block hash
        PrePrepareMessage prePrepareMessage = new PrePrepareMessage("incorrectBlockHash");
        String preprepareMessageJson = new Gson().toJson(prePrepareMessage);

        for (Map.Entry<String, TestableNodeService> entry : allNodes.entrySet()) {
            TestableNodeService node = entry.getValue();
            node.listen();
            if (node.getConfig().getId().equals(leaderId)) {
                continue;
            }
            node.startConsensus();
        }
        // Send the conflicting pre-prepare message to all nodes
        for (Map.Entry<String, TestableNodeService> entry : allNodes.entrySet()) {
            TestableNodeService node = entry.getValue();

            ConsensusMessage consensusMessage = new ConsensusMessageBuilder(leaderId, Message.Type.PRE_PREPARE)
                    .setConsensusInstance(consensusInstance)
                    .setRound(round)
                    .setMessage(preprepareMessageJson) // Pass the serialized PrepareMessage JSON here
                    .build();
            node.uponPrePrepare(consensusMessage);
        }

        // Wait 7 seconds for round change
        try {
            System.out.println("Waiting for round change...");
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify that round change is performed
        verify(linkSpy, times(nodeConfigs.length)).send(anyString(),
                argThat(argument -> argument instanceof RoundChangeMessage
                        && ((RoundChangeMessage) argument).getType() == Message.Type.ROUND_CHANGE));
        assertEquals(false, nodeService.isLeader(leaderId));
        assertEquals(true, nodeService.isLeader("2"));

    }

    // Test a "complete" run of the system, from handleTransfer to block added
    @Test
    @Order(3)
    public void testCompleteRun() {
        System.out.println("Testing complete run of the system...");

        setupAllNodes();
        setupClient();
        nodeService.initialiseClientBalances(clientConfigs);

        // Set up clientservice

        ClientData clientData = setupClientData("20 client2 1");
        ClientMessage transferMessage = new ClientMessage(clientData.getClientID(), Message.Type.TRANSFER);
        transferMessage.setClientData(clientData);

        // Simulate client broadcasting transfer message
        for (Map.Entry<String, TestableNodeService> entry : allNodes.entrySet()) {
            TestableNodeService node = entry.getValue();
            node.initialiseClientBalances(clientConfigs);
            node.handleTransfer(transferMessage);
            node.startConsensus();
            node.listen();
        }

        // Wait 7 seconds for block to be added to blockchain
        try {
            System.out.println("Waiting for consensus to complete...");
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }

        // Verify that the block was added to the blockchain
        Block latestBlock = nodeService.getBlockchain().getLatestBlock();
        assertTrue(latestBlock.getTransactions().contains(clientData),
                "Transaction was not committed to the blockchain");
        assertEquals(80f, nodeService.clientBalances.get("client1"), "Client1 balance not updated correctly");
        assertEquals(120f, nodeService.clientBalances.get("client2"), "Client2 balance not updated correctly");
    }

}
