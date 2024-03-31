package pt.ulisboa.tecnico.hdsledger.service.services;

import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;


import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.*;

import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.ClientData;
import pt.ulisboa.tecnico.hdsledger.communication.ClientMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;


import pt.ulisboa.tecnico.hdsledger.service.blockchain.Block;


@TestMethodOrder(OrderAnnotation.class)
public class NodeServiceTransactionTest extends NodeServiceBaseTest {

    // Test that block is appended to blockchain if valid commit quorum
    @Test
    @Order(1)
    public void testCommitAddsTransactionToBlock() {
        System.out.println("\nAdd transaction to block on commit...");
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
    @Order(2)
    public void testNoCommitNoTransactionToBlock() {
        System.out.println("\nNo transaction to block on no commit...");
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
    @Order(3)
    public void testClientBalances() {
        System.out.println("\nClient balances test");
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
        Double result = 120 - (20 * 0.1);
        Float receiverBalance = result.floatValue();
        assertEquals(receiverBalance, nodeService.clientBalances.getOrDefault("client2", 0.0f));
        result = 20 * 0.1;
        String leader = nodeService.leaderConfig.getId();
        assertEquals(result.floatValue(), nodeService.nodeBalances.get(leader), "Leader balance not updated correctly");
    }

    // Test that block is not appended if the hash is invalid
    @Test
    @Order(4)
    public void testNoBlockIfInvalidHash() {
        System.out.println("\nNo block if invalid hash");

        ClientData clientData = super.setupClientData("20 client2 1");
        String blockHash = nodeService.addToTransactionQueueAndCreateBlock(clientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.sendCommitMessages("invalidHash", nodeService.getQuorum());

        // Check that the block was not added to the blockchain
        assertEquals(1, nodeService.getBlockchain().getLength());
    }

    // Test a "complete" run of the system, from handleTransfer to block added
    @Test
    @Order(5)
    public void testCompleteRun() {
        System.out.println("\nTesting complete run of the system...");

        super.setupAllNodes();
        super.setupClient();
        nodeService.initialiseClientBalances(super.clientConfigs);

        // Set up clientservice

        ClientData clientData = super.setupClientData("20 client2 1");
        ClientMessage transferMessage = new ClientMessage(clientData.getClientID(), Message.Type.TRANSFER);
        transferMessage.setClientData(clientData);

        // Simulate client broadcasting transfer message
        for (Map.Entry<String, TestableNodeService> entry : allNodes.entrySet()) {
            TestableNodeService node = entry.getValue();
            node.initialiseClientBalances(super.clientConfigs);
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
        Double result = 120 - (20 * 0.1);
        Float receiverBalance = result.floatValue();
        assertEquals(receiverBalance, nodeService.clientBalances.get("client2"),
                "Client2 balance not updated correctly");

        result = 20 * 0.1;
        String leader = nodeService.leaderConfig.getId();
        assertEquals(result.floatValue(), nodeService.nodeBalances.get(leader), "Leader balance not updated correctly");
    }

    // Test that a node with the wrong balances before consensus gets updated
    // balances after
    @Test
    @Order(6)
    public void testNodeWithWrongBalances() {
        System.out.println("\nTesting node with wrong balances before consensus...");
    }

}
