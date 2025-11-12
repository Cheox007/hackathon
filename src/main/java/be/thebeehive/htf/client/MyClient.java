package be.thebeehive.htf.client;

import be.thebeehive.htf.library.HtfClient;
import be.thebeehive.htf.library.HtfClientListener;
import be.thebeehive.htf.library.protocol.client.SelectActionsClientMessage;
import be.thebeehive.htf.library.protocol.server.ErrorServerMessage;
import be.thebeehive.htf.library.protocol.server.GameEndedServerMessage;
import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage;
import be.thebeehive.htf.library.protocol.server.WarningServerMessage;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
        // Log current submarine status
        GameRoundServerMessage.Submarine sub = msg.getOurSubmarine();
        if (sub != null && sub.getValues() != null) {
            GameRoundServerMessage.Values v = sub.getValues();
            System.out.println(String.format("Round %d: Hull: %s/%s, Crew: %s/%s", 
                msg.getRound(),
                v.getHullStrength(), v.getMaxHullStrength(),
                v.getCrewHealth(), v.getMaxCrewHealth()));
        }
        
        int cap = Math.min(maxActionsCap, msg.getActions() != null ? msg.getActions().size() : 0);
        List<Long> selected = engine.decideActions(msg, cap);

        // Improved fallback: if no actions selected, pick safest actions
        if (selected.isEmpty() && msg.getActions() != null && !msg.getActions().isEmpty()) {
            System.out.println("WARNING: No actions selected by engine, using fallback!");
            // Try to find the least harmful or most beneficial action
            List<Long> fallbackList = msg.getActions().stream()
                    .sorted(Comparator.comparing(a -> {
                        // Score based on benefits and avoid self-harm
                        java.math.BigDecimal hull = a.getValues().getHullStrength() != null ? 
                            a.getValues().getHullStrength() : java.math.BigDecimal.ZERO;
                        java.math.BigDecimal crew = a.getValues().getCrewHealth() != null ? 
                            a.getValues().getCrewHealth() : java.math.BigDecimal.ZERO;
                        java.math.BigDecimal maxHull = a.getValues().getMaxHullStrength() != null ? 
                            a.getValues().getMaxHullStrength() : java.math.BigDecimal.ZERO;
                        java.math.BigDecimal maxCrew = a.getValues().getMaxCrewHealth() != null ? 
                            a.getValues().getMaxCrewHealth() : java.math.BigDecimal.ZERO;
                        return hull.add(crew).add(maxHull).add(maxCrew);
                    }, Comparator.reverseOrder()))
                    .limit(Math.min(3, cap))
                    .map(a -> a.getId())
                    .collect(Collectors.toList());
            
            if (!fallbackList.isEmpty()) {
                selected = fallbackList;
            }
        }
        
        System.out.println(String.format("Selected %d actions: %s", selected.size(), selected));
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
