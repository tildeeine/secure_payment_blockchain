package pt.ulisboa.tecnico.hdsledger.service.services;

import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
}