package be.thebeehive.htf.client;

import be.thebeehive.htf.library.HtfClient;
import be.thebeehive.htf.library.HtfClientListener;
import be.thebeehive.htf.library.protocol.client.SelectActionsClientMessage;
import be.thebeehive.htf.library.protocol.server.ErrorServerMessage;
import be.thebeehive.htf.library.protocol.server.GameEndedServerMessage;
import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage;
import be.thebeehive.htf.library.protocol.server.WarningServerMessage;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyClient implements HtfClientListener {
    private final DecisionEngine engine = new DecisionEngine();
    private int maxActionsCap = 5;
    
    /**
     * You tried to perform an action that is not allowed.
     * An error occurred, and we are unable to recover from this.
     * You will also be disconnected.
     */
    @Override
    public void onErrorServerMessage(HtfClient client, ErrorServerMessage msg) throws Exception {
        System.err.println("ERROR: " + msg.getMsg());
    }

    /**
     * The game finished. Did you win?
     */
    @Override
    public void onGameEndedServerMessage(HtfClient client, GameEndedServerMessage msg) throws Exception {
        System.out.println("Game ended at round " + msg.getRound());
    }

    /**
     * A new round has started.
     * You must reply within 1 second!
     */
    @Override
    public void onGameRoundServerMessage(HtfClient client, GameRoundServerMessage msg) throws Exception {
        int cap = Math.min(maxActionsCap, msg.getActions() != null ? msg.getActions().size() : 0);
        List<Long> selected = engine.decideActions(msg, cap);

        if (selected.isEmpty() && msg.getActions() != null && !msg.getActions().isEmpty()) {
            Long fallback = msg.getActions().stream()
                    .sorted(Comparator.comparing(a -> a.getValues().getHullStrength() == null ? java.math.BigDecimal.ZERO : a.getValues().getHullStrength(), Comparator.reverseOrder()))
                    .map(a -> a.getId())
                    .findFirst().orElse(null);
            if (fallback != null) selected = Collections.singletonList(fallback);
        }

        client.send(new SelectActionsClientMessage(msg.getRoundId(), selected));
    }

    /**
     * You tried to perform an action that is not allowed.
     * An error occurred but you can still play along.
     * You will NOT be disconnected.
     */
    @Override
    public void onWarningServerMessage(HtfClient client, WarningServerMessage msg) throws Exception {
        String m = msg.getMsg() == null ? "" : msg.getMsg().toLowerCase();
        System.out.println("WARNING: " + msg.getMsg());
        if (m.contains("too many") || m.contains("maximum") || m.contains("exceeded")) {
            maxActionsCap = Math.max(1, maxActionsCap - 1);
            System.out.println("Adjusting maxActionsCap to " + maxActionsCap);
        }
    }
}
