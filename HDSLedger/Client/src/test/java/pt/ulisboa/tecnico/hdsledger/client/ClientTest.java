import pt.ulisboa.tecnico.hdsledger.client.Client;

import org.mockito.Mockito;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import static org.junit.Assert.*;

public class ClientTest {

    @Test
    public void testClient() {
        Client client = new Client();
        assertNotNull(client);
    }

}
