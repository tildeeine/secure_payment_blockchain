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
import static org.mockito.Mockito.doNothing;

import java.util.Arrays;

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

public class NodeServiceTransactionTest extends NodeServiceBaseTest {

    // Test that block is appended to blockchain if valid commit quorum
    @Test
    public void testCommitAddsTransactionToBlock() {
        System.out.println("Add transaction to block on commit...");
        int ledgerLengthBefore = nodeService.getBlockchain().getLength();

        ClientData clientData = super.setupClientData("20 client2 1");
        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(clientData);
        // Prevent the real send method from being called on the spy
        doNothing().when(super.linkSpy).send(anyString(), any(ConsensusMessage.class));

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

        ClientData clientData = super.setupClientData("20 client2 1");
        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(clientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.sendCommitMessages(blockHash, nodeService.getQuorum() - 1);

        // Check that the block was not added to the blockchain
        assertEquals(ledgerLengthBefore, nodeService.getBlockchain().getLength());
    }

    // Test client balances after a successful transaction
    // Balance should be updated correctly for both nodes
    @Test
    public void testClientBalances() {
        System.out.println("Client balances test");
        // Prevent the real send method from being called on the spy
        doNothing().when(super.linkSpy).send(anyString(), any(ConsensusMessage.class));

        // Set up client balances
        nodeService.initialiseClientBalances(super.clientConfigs);

        // Set up client data
        ClientData clientData = super.setupClientData("20 client2 1");
        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(clientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.sendCommitMessages(blockHash, nodeService.getQuorum());

        // Check that the balances were updated correctly
        assertEquals(80f, nodeService.clientBalances.getOrDefault("client1", 0.0f));
        assertEquals(120f, nodeService.clientBalances.getOrDefault("client2", 0.0f));
    }

    // Test client trying to send more money than they have
    // Should not update balances
    @Test
    public void testClientOverspend() {
        System.out.println("Client balances overspend test");

        // Set up client balances
        nodeService.initialiseClientBalances(super.clientConfigs);

        // Set up client data
        ClientData clientData = super.setupClientData("200 client2 1");

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
        nodeService.initialiseClientBalances(super.clientConfigs);

        // Set up client data
        ClientData clientData = super.setupClientData("20 nonexistent 1");
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
        nodeService.initialiseClientBalances(super.clientConfigs);

        // Set up client data
        ClientData clientData = super.setupClientData("-20 client2 1");
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
        nodeService.initialiseClientBalances(super.clientConfigs);

        // Set up client data
        ClientData clientData1 = super.setupClientData("20 client2 1"); // amount, dest, reqID
        ClientData clientData2 = super.setupClientData("20 client3 1");

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
        verify(super.linkSpy, times(0)).send(anyString(), argThat(argument -> argument instanceof ConsensusMessage
                && ((ConsensusMessage) argument).getType() == Message.Type.COMMIT));
    }

    // Test that block is not appended if the hash is invalid
    @Test
    public void testNoBlockIfInvalidHash() {
        System.out.println("No block if invalid hash");

        ClientData clientData = super.setupClientData("20 client2 1");
        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(clientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.sendCommitMessages("invalidHash", nodeService.getQuorum());

        // Check that the block was not added to the blockchain
        assertEquals(1, nodeService.getBlockchain().getLength());
    }

}
