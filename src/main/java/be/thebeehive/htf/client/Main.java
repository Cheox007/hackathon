package be.thebeehive.htf.client;

import be.thebeehive.htf.library.EnvironmentType;
import be.thebeehive.htf.library.HtfClient;

import java.net.URISyntaxException;

public class Main {

    /**
     * The entry point of the application.
     * Start an HtfClient which connects to the on-board computer of the submarine.
     */
    public static void main(String[] args) throws URISyntaxException {
        HtfClient client = new HtfClient(
                "wss://htf.b9s.dev/ws",
                "",
                EnvironmentType.SIMULATION,
                new MyClient()
        );
        Runtime.getRuntime().addShutdownHook(new Thread(client::close));
        client.connect();
    }
}
