import pt.ulisboa.tecnico.hdsledgers.service.Node;

import org.junit.jupiter.api.Test;
import static org.junit.Assert.*;

//Tests to make:
// 1. Test reaction when node sends incorrect message
// 2. Test reaction when node sends correct message
// 3. Test Node trying to interrupt communications
// 4. Test node sending conflicting messages
// 5. Test node trying to change the value for consensus

public class NodeTest {
    @Test
    public void testNode() {
        Node node = new Node();
        assertNotNull(node);
    }

}