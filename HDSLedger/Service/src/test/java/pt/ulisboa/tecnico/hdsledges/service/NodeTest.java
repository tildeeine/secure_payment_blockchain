import pt.ulisboa.tecnico.hdsledger.service.Node;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.InjectMocks;

//Tests to make:
// 1. Test reaction when node sends incorrect message
// 2. Test reaction when node sends correct message
// 3. Test Node trying to interrupt communications
// 4. Test node sending conflicting messages
// 5. Test node trying to change the value for consensus

public class NodeTest {

    // Setup Node for testing

    @Test
    public void testNode() {
        Node node = new Node("1", "src/main/resources/config.json", "src/main/resources/client_config.json");

    }
}