package pt.ulisboa.tecnico.hdsledger.service.services;

import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.hdsledger.communication.ClientData;
import pt.ulisboa.tecnico.hdsledger.service.blockchain.Block;
import pt.ulisboa.tecnico.hdsledger.service.blockchain.Blockchain;
import pt.ulisboa.tecnico.hdsledger.communication.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.builder.ConsensusMessageBuilder;
import pt.ulisboa.tecnico.hdsledger.service.models.InstanceInfo;
import pt.ulisboa.tecnico.hdsledger.communication.Message;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class TestableNodeService extends NodeService {

    private Link link;

    public TestableNodeService(Link link, ProcessConfig nodeConfig, ProcessConfig leaderConfig,
            ProcessConfig[] nodesConfig) {
        // Add clients configs to the link, so node can send messages to clients
        super(link, nodeConfig, leaderConfig, nodesConfig);
        this.link = link;

    }

    @Override
    public void startConsensusWithTimer() {
        System.out.println("Timer started");
    }

    // Shut down currently running services
    public void shutdown() {
        System.out.println("Shutting down NodeService...");

        shutdownExecutorService(scheduler);

        // Shutdown Link or network components
        if (link != null) {
            link.shutdown(); // Ensure this method properly closes sockets and cleans up resources.
        }
        System.out.println("NodeService shutdown completed.");
    }

    // Utility method for shutting down an ExecutorService
    private void shutdownExecutorService(ScheduledExecutorService executorService) {
        System.out.println("Shutting down ExecutorService...");
        executorService.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Executor service did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    public String addToTransactionQueueAndCreateBlock(ClientData clientData) {
        // Add to transaction queue
        super.addToTransactionQueue(clientData);

        // Create block from transactions in the queue
        Block block = super.blockCreator();

        // Store the block with a corresponding hash as key
        try {
            String blockHash = Blockchain.calculateHash(block);
            super.blockNumberToBlockMapping.put(block.getBLOCK_ID(), block);
            setupInstanceInfoForBlock(blockHash);
            return blockHash;
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setupInstanceInfoForBlock(String blockHash) {
        int currentInstance = super.getConsensusInstance().incrementAndGet();
        InstanceInfo instanceInfo = new InstanceInfo(blockHash);
        instanceInfo.setCurrentRound(1); // Assuming the initial round starts at 1
        super.getInstanceInfo().put(currentInstance, instanceInfo);
    }

    public void sendQuorumOfCommitMessages(String blockHash) {
        int consensusInstance = super.getConsensusInstance().get();
        int round = 1; // or dynamic if needed

        CommitMessage commitMessage = new CommitMessage(blockHash);
        int quorum = calculateQuorum();

        for (int i = 0; i < quorum; i++) {
            ConsensusMessage consensusMessage = new ConsensusMessageBuilder(String.valueOf(i + 1), Message.Type.COMMIT)
                    .setConsensusInstance(consensusInstance)
                    .setRound(round)
                    .setMessage(commitMessage.toJson())
                    .build();
            super.uponCommit(consensusMessage);
        }
    }

    private int calculateQuorum() {
        int f = Math.floorDiv(nodesConfig.length - 1, 3);
        return Math.floorDiv(nodesConfig.length + f, 2) + 1;
    }

}