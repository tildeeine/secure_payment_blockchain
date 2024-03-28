package pt.ulisboa.tecnico.hdsledger.service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

@ExtendWith(MockitoExtension.class)
public class ByzantineClientTest {
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

    // Test that a byzantine node sends invalid data
    // Should not commit, but should not crash
    @Test
    public void testInvalidData() {
        System.out.println("Byzantine node invalid data test");

        // Prepare unsigned client data
        ClientData unsignedClientData = new ClientData();
        unsignedClientData.setRequestID(1); // Example request ID
        unsignedClientData.setValue("invalid data"); // Transaction value
        unsignedClientData.setClientID("client1"); // Client ID

        ClientMessage falseClientMessage = new ClientMessage(unsignedClientData.getClientID(), Message.Type.TRANSFER);
        falseClientMessage.setClientData(unsignedClientData);

        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(unsignedClientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.sendCommitMessages(blockHash, nodeService.getQuorum());

        // Verify no commit messages were sent due to the unsigned client data
        verify(linkSpy, times(0)).send(anyString(), argThat(argument -> argument instanceof ConsensusMessage
                && ((ConsensusMessage) argument).getType() == Message.Type.COMMIT));
    }

    // Test client trying to send more money than they have
    // Should not update balances
    @Test
    public void testClientOverspend() {
        System.out.println("Client balances overspend test");

        // Set up client balances
        nodeService.initialiseClientBalances(clientConfigs);

        // Set up client data
        ClientData clientData = setupClientData("200 client2 1");

        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(clientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.sendCommitMessages(blockHash, nodeService.getQuorum());

        // Check that the balances were not updated
        assertEquals(100f, nodeService.clientBalances.getOrDefault("client1", 0.0f));
        assertEquals(100f, nodeService.clientBalances.getOrDefault("client2", 0.0f));
    }

    // Test that a client can't send money to non-existent clients
    @Test
    public void testClientBalancesNonExistentClient() {
        System.out.println("Client balances non-existent client test");

        // Set up client balances
        nodeService.initialiseClientBalances(clientConfigs);

        // Set up client data
        ClientData clientData = setupClientData("20 nonexistent 1");
        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(clientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.sendCommitMessages(blockHash, nodeService.getQuorum());

        // Check that the balances were not updated
        assertEquals(100f, nodeService.clientBalances.getOrDefault("client1", 0.0f));
    }

    // Test that a client can't send negative money
    @Test
    public void testClientBalancesNegativeMoney() {
        System.out.println("Client balances negative money test");

        // Set up client balances
        nodeService.initialiseClientBalances(clientConfigs);

        // Set up client data
        ClientData clientData = setupClientData("-20 client2 1");
        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(clientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.sendCommitMessages(blockHash, nodeService.getQuorum());

        // Check that the balances were not updated
        assertEquals(100f, nodeService.clientBalances.getOrDefault("client1", 0.0f));
        assertEquals(100f, nodeService.clientBalances.getOrDefault("client2", 0.0f));
    }

    // Test that double spending doesn't work
    @Test
    public void testDoubleSpendingRejected() {
        System.out.println("Double spending rejected test");

        // Set up client balances
        nodeService.initialiseClientBalances(clientConfigs);

        // Set up client data
        ClientData clientData1 = setupClientData("20 client2 1"); // amount, dest, reqID
        ClientData clientData2 = setupClientData("20 client3 1");

        ClientMessage clientMessage1 = new ClientMessage(clientData1.getClientID(), Message.Type.TRANSFER);
        clientMessage1.setClientData(clientData1);
        ClientMessage clientMessage2 = new ClientMessage(clientData2.getClientID(), Message.Type.TRANSFER);
        clientMessage2.setClientData(clientData2);

        String blockHash1 = nodeService.addToTransactionQueueAndCreateBlock(clientData1);
        String blockHash2 = nodeService.addToTransactionQueueAndCreateBlock(clientData2);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.sendCommitMessages(blockHash1, nodeService.getQuorum());
        nodeService.sendCommitMessages(blockHash2, nodeService.getQuorum());

        // Check that only the first transaction was successful
        assertEquals(80f, nodeService.clientBalances.getOrDefault("client1", 0.0f));
        assertEquals(120f, nodeService.clientBalances.getOrDefault("client2", 0.0f));
        assertEquals(100f, nodeService.clientBalances.getOrDefault("client3", 0.0f));
    }

    // Test that client data that is not signed is not accepted for quorum
    // Illustrates byzantine nodes sending invalid data
    @Test
    void testRejectUnsignedClientData() {
        System.out.println("Reject unsigned client data");

        // Prepare unsigned client data
        ClientData unsignedClientData = new ClientData();
        unsignedClientData.setRequestID(1); // Example request ID
        unsignedClientData.setValue("20 client2 1"); // Transaction value
        unsignedClientData.setClientID("client1"); // Client ID

        ClientMessage falseClientMessage = new ClientMessage(unsignedClientData.getClientID(), Message.Type.TRANSFER);
        falseClientMessage.setClientData(unsignedClientData);

        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(unsignedClientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.sendCommitMessages(blockHash, nodeService.getQuorum());

        // Verify no commit messages were sent due to the unsigned client data
        verify(linkSpy, times(0)).send(anyString(), argThat(argument -> argument instanceof ConsensusMessage
                && ((ConsensusMessage) argument).getType() == Message.Type.COMMIT));
    }

    // Test that block is not appended if the hash is invalid
    @Test
    public void testNoBlockIfInvalidHash() {
        System.out.println("No block if invalid hash");

        ClientData clientData = setupClientData("20 client2 1");
        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(clientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.sendCommitMessages("invalidHash", nodeService.getQuorum());

        // Check that the block was not added to the blockchain
        assertEquals(1, nodeService.getBlockchain().getLength());
    }

}