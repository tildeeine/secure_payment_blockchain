package pt.ulisboa.tecnico.hdsledger.client.services;

import java.io.IOException;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import pt.ulisboa.tecnico.hdsledger.communication.ClientMessage;
import pt.ulisboa.tecnico.hdsledger.communication.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.communication.Message;

import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class ClientService implements UDPServiceClient {

    private static final CustomLogger LOGGER = new CustomLogger(ClientService.class.getName());
    // Nodes configurations
    private final ProcessConfig[] nodesConfig;
    // Current node is leader
    private final ProcessConfig config;
    // Leader configuration
    private ProcessConfig leaderConfig;

    // Link to communicate with nodes
    private final Link link;

    private Timer timer;

    private int timeout;

    // Client requests
    private String clientRequestID;

    private String inputValue;

    // if the rules have been done already
    private boolean rule1 = false;

    private boolean rule2 = false;

    public ClientService(Link link, ProcessConfig config,
            ProcessConfig leaderConfig, ProcessConfig[] nodesConfig) {

        this.link = link;
        this.config = config;
        this.leaderConfig = leaderConfig;
        this.nodesConfig = nodesConfig;

        this.timeout = 5000;
    }

    public void sendClientMessage(Message message) {
        link.broadcast(message);
    }

    public ProcessConfig getConfig() {
        return this.config;
    }

    public void cancelTimer() {
        if (timer != null)
            timer.cancel();
    }

    private void startTimer() {
        cancelTimer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Task timed out!");
            }
        };

        timer = new Timer(true); // Daemon thread
        timer.schedule(task, timeout); // 1000 milliseconds = 1 second
    }

    @Override
    public void listen() {
        try {
            // Thread to listen on every request
            new Thread(() -> {
                try {
                    System.out.println(
                            "Listening on " + InetAddress.getLocalHost().getHostAddress() + ":" + config.getPort());
                    // remove
                    while (true) {
                        Message message = link.receive();

                        // non verified messages
                        if (message == null)
                            return;

                        // Separate thread to handle each message
                        new Thread(() -> {

                            switch (message.getType()) {

                                case ACK ->
                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received ACK message from {1}",
                                            config.getId(), message.getSenderId()));

                                case IGNORE ->
                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received IGNORE message from {1}",
                                                    config.getId(), message.getSenderId()));

                                default ->
                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received unknown message from {1}",
                                                    config.getId(), message.getSenderId()));

                            }

                        }).start();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
