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
public class BasicFunctionalitiesTest {
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

    // Test that block is appended to blockchain if valid commit quorum
    @Test
    public void testCommitAddsTransactionToBlock() {
        System.out.println("Add transaction to block on commit...");
        int ledgerLengthBefore = nodeService.getBlockchain().getLength();

        ClientData clientData = setupClientData("20 client2 1");
        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(clientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.sendCommitMessages(blockHash, nodeService.getQuorum());

        // Check that the block was added to the blockchain
        Block latestBlock = nodeService.getBlockchain().getLatestBlock();
        assertEquals(ledgerLengthBefore + 1, nodeService.getBlockchain().getLength());
        assertEquals(1, latestBlock.getTransactions().size());
        assertTrue(latestBlock.getTransactions().contains(clientData));
    }

    // Test that block is not appended to blockchain if invalid commit quorum
    @Test
    public void testNoCommitNoTransactionToBlock() {
        System.out.println("No transaction to block on no commit...");
        int ledgerLengthBefore = nodeService.getBlockchain().getLength();

        ClientData clientData = setupClientData("20 client2 1");
        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(clientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.sendCommitMessages(blockHash, nodeService.getQuorum() - 1);

        // Check that the block was not added to the blockchain
        assertEquals(ledgerLengthBefore, nodeService.getBlockchain().getLength());
    }

    // Test that updateLeader is done correctly
    @Test
    public void testUpdateLeader() {
        System.out.println("Update leader test...");
        String newLeaderId = "2";

        // Increment the current round to simulate a round change
        int initialConsensusInstance = nodeService.getConsensusInstance().get();

        nodeService.setupInstanceInfoForBlock("testString", 1);

        // Retrieve the updated instance info
        InstanceInfo instance = nodeService.getInstanceInfo().get(initialConsensusInstance);

        int targetRound = instance.getCurrentRound() + 1;
        instance.setCurrentRound(targetRound);

        // Act
        nodeService.updateLeader();

        assertEquals(true, nodeService.isLeader(newLeaderId));
    }

    // Test that commit is sent if there is a quorum of prepare messages
    @Test
    public void testCommitOnPrepareQuorum() {
        System.out.println("Commit on valid quorum...");

        ClientData clientData = setupClientData("20 client2 1");
        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(clientData);
        // nodeService.startConsensus();
        nodeService.sendPrepareMessages(blockHash, nodeService.getQuorum());

        int quorum = nodeService.getQuorum();

        // Check that the commit message was sent
        verify(linkSpy, times(quorum)).send(anyString(), argThat(argument -> argument instanceof ConsensusMessage &&
                ((ConsensusMessage) argument).getType() == Message.Type.COMMIT));

    }

}
