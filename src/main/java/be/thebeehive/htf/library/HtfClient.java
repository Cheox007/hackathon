package be.thebeehive.htf.library;

import be.thebeehive.htf.library.protocol.client.SelectActionsClientMessage;
import be.thebeehive.htf.library.protocol.server.ErrorServerMessage;
import be.thebeehive.htf.library.protocol.server.GameEndedServerMessage;
import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage;
import be.thebeehive.htf.library.protocol.server.ServerMessage;
import be.thebeehive.htf.library.protocol.server.WarningServerMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

public class HtfClient extends WebSocketClient {

    private final HtfClientListener listener;
    private final ObjectMapper objectMapper;

    public HtfClient(
            String uri,
            String apiKey,
            EnvironmentType environmentType,
            HtfClientListener listener
    ) throws URISyntaxException {
        super(new URI(uri), new HashMap<String, String>() {{
            this.put("apiKey", apiKey);
            this.put("clientType", "PLAYER");
            this.put("environment", environmentType.name());
        }});
        this.listener = listener;
        this.objectMapper = new ObjectMapper();
    }

    public void send(SelectActionsClientMessage msg) {
        try {
            this.send(this.objectMapper.writeValueAsString(msg));
        } catch (JsonProcessingException ex) {
            this.onError(ex);
        }
    }

    @Override
    public void onMessage(String messageStr) {
        try {
            ServerMessage msg = this.objectMapper.readValue(messageStr, ServerMessage.class);

            if (msg instanceof ErrorServerMessage) {
                this.listener.onErrorServerMessage(this, (ErrorServerMessage) msg);
            } else if (msg instanceof GameEndedServerMessage) {
                this.listener.onGameEndedServerMessage(this, (GameEndedServerMessage) msg);
            } else if (msg instanceof GameRoundServerMessage) {
                this.listener.onGameRoundServerMessage(this, (GameRoundServerMessage) msg);
            } else if (msg instanceof WarningServerMessage) {
                this.listener.onWarningServerMessage(this, (WarningServerMessage) msg);
            }
        } catch (Exception ex) {
            this.onError(ex);
        }
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("You are connected to HtfServer: " + getURI());
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("You have been disconnected from: " + getURI() + "; Code: " + code + " " + reason);
    }

    @Override
    public void onError(Exception ex) {
        this.close();
        System.err.println("Exception occurred ...\n" + ex);
    }
}
