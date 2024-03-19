package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.client.services.ClientMessageBuilder;
import pt.ulisboa.tecnico.hdsledger.client.services.ClientService;
import pt.ulisboa.tecnico.hdsledger.communication.ClientMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.Arrays;
import java.util.Scanner;
import java.io.InputStream;
import java.io.IOException;

public class Client {
    private static final CustomLogger LOGGER = new CustomLogger("Client");

    private static final String CLIENTCONFIGPATH = "src/main/resources/client_config.json";
    private static final String NODECONFIGPATH = "src/main/resources/regular_config.json";



    public static void main(String[] args) throws IOException, ClassNotFoundException {
        try {
            String id = args[0];
            Scanner scanner = new Scanner(System.in);

            // Set up the link to the nodes
            InputStream resourceStream = Client.class.getClassLoader().getResourceAsStream(NODECONFIGPATH);

            // Creating a link to the service
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(NODECONFIGPATH);
            ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
            ProcessConfig[] clientConfigs = new ProcessConfigBuilder().fromFile(CLIENTCONFIGPATH);
            ProcessConfig clientConfig = Arrays.stream(clientConfigs).filter(c -> c.getId().equals(id)).findAny().get();

            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Running at {1}:{2}; is leader: {3}",
                    clientConfig.getId(), clientConfig.getHostname(), clientConfig.getPort(),
                    clientConfig.isLeader()));

            Link link = new Link(clientConfig, clientConfig.getPort(), nodeConfigs,
                    ConsensusMessage.class);

            link.addClient(clientConfigs);

            // Create a clientService for sending messages to nodes
            ClientService clientService = new ClientService(link, clientConfig, leaderConfig, nodeConfigs);
            // Create a ClientMessageBuilder for creating the messages
            ClientMessageBuilder clientMessageBuilder = new ClientMessageBuilder(clientConfig);
            // Start a thread to listen for messages from nodes
            new Thread(() -> {
                // Listen for incoming messages from nodes
                clientService.listen();

            }).start();

            // Continuous loop to read user commands
            while (true) {
                System.out.print("Enter command: ");
                String userCommand = scanner.nextLine().trim().toLowerCase();
                switch (userCommand.split(" ")[0]) {
                    case "append":
                        // Extract the payload from the user input
                        String payload = userCommand.substring("append".length()).trim();
                        ClientMessage appendMessage = clientMessageBuilder.buildMessage(payload, clientConfig.getId());
                        // Send the message to nodes
                        clientService.sendClientMessage(appendMessage);
                        break;
                    case "quit":
                        // Handle other commands as needed
                        quitHandler();
                        break;
                    default:
                        System.out.println("Unknown command. Try again.");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void quitHandler() {
        // Perform any cleanup or shutdown tasks before exiting
        System.out.println("Exiting the client application.");
        System.exit(0);
    }
}
