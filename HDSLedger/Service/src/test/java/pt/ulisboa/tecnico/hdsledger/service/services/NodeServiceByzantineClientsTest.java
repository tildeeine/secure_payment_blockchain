package pt.ulisboa.tecnico.hdsledger.service.services;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.ClientData;
import pt.ulisboa.tecnico.hdsledger.communication.ClientMessage;

@TestMethodOrder(OrderAnnotation.class)

public class NodeServiceByzantineClientsTest extends NodeServiceBaseTest {

    // Test client trying to send more money than they have
    // Should not update balances
    @Test
    @Order(1)
    public void testClientOverspend() {
        System.out.println("\nClient balances overspend test");

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
        String leader = nodeService.leaderConfig.getId();
        assertEquals(0.0f, nodeService.nodeBalances.get(leader), "Leader balance not updated correctly");
    }

    // Test that a client can't send money to non-existent clients
    @Test
    @Order(2)
    public void testClientBalancesNonExistentClient() {
        System.out.println("\nClient balances non-existent client test");

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
        String leader = nodeService.leaderConfig.getId();
        assertEquals(0.0f, nodeService.nodeBalances.get(leader), "Leader balance not updated correctly");
    }

    // Test that a client can't send negative money
    @Test
    @Order(3)
    public void testClientBalancesNegativeMoney() {
        System.out.println("\nClient balances negative money test");

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
        String leader = nodeService.leaderConfig.getId();
        assertEquals(0.0f, nodeService.nodeBalances.get(leader), "Leader balance should not be updated");
    }

    // Test client sending money to themselves
    @Test
    @Order(4)
    public void testClientBalancesSelfTransfer() {
        System.out.println("\nClient balances self transfer test");

        // Set up client balances
        nodeService.initialiseClientBalances(super.clientConfigs);

        // Set up client data
        ClientData clientData = super.setupClientData("20 client1 1");
        ClientMessage clientMessage = new ClientMessage(clientData.getClientID(), Message.Type.TRANSFER);
        clientMessage.setClientData(clientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.handleTransfer(clientMessage);

        // Check that the balances were not updated
        assertEquals(100f, nodeService.clientBalances.getOrDefault("client1", 0.0f));
        String leader = nodeService.leaderConfig.getId();
        assertEquals(0.0f, nodeService.nodeBalances.get(leader), "Leader balance should not be updated");
    }

    // Test a client impersonating another client. Should get invalid signature.
    @Test
    @Order(5)
    public void testClientImpersonation() {
        System.out.println("\nClient impersonation test");

        // Set up client balances
        nodeService.initialiseClientBalances(super.clientConfigs);

        // Set up client data
        ClientData clientData = super.setupClientData("20 client1 1");
        clientData.setClientID("client2");
        ClientMessage clientMessage = new ClientMessage(clientData.getClientID(), Message.Type.TRANSFER);
        clientMessage.setClientData(clientData);

        //
        nodeService.handleTransfer(clientMessage);

        // Check that the transaction was not added to the queue
        assertEquals(false, nodeService.transactionQueue.contains(clientData));
    }

    // Test that double spending doesn't work
    @Test
    @Order(6)
    public void testDoubleSpendingRejected() {
        System.out.println("\nDouble spending rejected test");

        // Set up client balances
        nodeService.initialiseClientBalances(super.clientConfigs);

        // Set up client data
        ClientData clientData1 = super.setupClientData("20 client2 1"); // amount, dest, reqID
        ClientData clientData2 = super.setupClientData("90 client2 2");

        ClientMessage clientMessage1 = new ClientMessage(clientData1.getClientID(), Message.Type.TRANSFER);
        clientMessage1.setClientData(clientData1);
        ClientMessage clientMessage2 = new ClientMessage(clientData2.getClientID(), Message.Type.TRANSFER);
        clientMessage2.setClientData(clientData2);

        String blockHash1 = nodeService.addToTransactionQueueAndCreateBlock(clientData1);
        String blockHash2 = nodeService.addToTransactionQueueAndCreateBlock(clientData2);

        nodeService.sendCommitMessages(blockHash1, nodeService.getQuorum());
        nodeService.sendCommitMessages(blockHash2, nodeService.getQuorum());

        // Check that only the first transaction was successful
        assertEquals(80f, nodeService.clientBalances.getOrDefault("client1", 0.0f));
        Double result = 120 - (20 * 0.1);
        Float receiverBalance = result.floatValue();
        assertEquals(receiverBalance, nodeService.clientBalances.getOrDefault("client2", 0.0f));
        assertEquals(100f, nodeService.clientBalances.getOrDefault("client3", 0.0f));
        result = 20 * 0.1;
        String leader = nodeService.leaderConfig.getId();
        assertEquals(result.floatValue(), nodeService.nodeBalances.get(leader), "Leader balance not updated correctly");
    }

    // Test that client data that is not signed is not accepted for quorum
    // Illustrates byzantine nodes sending invalid data
    // ! Fix! No commit will be sent
    @Test
    @Order(7)
    public void testRejectUnsignedClientData() {
        System.out.println("\nReject unsigned client data");

        // Prepare unsigned client data
        ClientData unsignedClientData = new ClientData();
        unsignedClientData.setRequestID(1); // Example request ID
        unsignedClientData.setValue("20 client2 1"); // Transaction value
        unsignedClientData.setClientID("client1"); // Client ID

        ClientMessage falseClientMessage = new ClientMessage(unsignedClientData.getClientID(), Message.Type.TRANSFER);
        falseClientMessage.setClientData(unsignedClientData);

        // Assuming setupInstanceInfoForBlock has already been called inside
        // addToTransactionQueueAndCreateBlock
        nodeService.handleTransfer(falseClientMessage);
        nodeService.startConsensus();

        // Should not have added the transaction to its block,a s it shouldn't pass the
        // signature check
        assertFalse(nodeService.blockNumberToBlockMapping.get(1).getTransactions().contains(unsignedClientData));

    }

}
