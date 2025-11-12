package be.thebeehive.htf.client;

import be.thebeehive.htf.library.HtfClient;
import be.thebeehive.htf.library.HtfClientListener;
import be.thebeehive.htf.library.protocol.client.SelectActionsClientMessage;
import be.thebeehive.htf.library.protocol.server.ErrorServerMessage;
import be.thebeehive.htf.library.protocol.server.GameEndedServerMessage;
import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage;
import be.thebeehive.htf.library.protocol.server.WarningServerMessage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Submarine AI Client - Implements survival strategy for deep-sea descent.
 * Priority: Prevent death first, optimize stats second.
 */
public class MyClient implements HtfClientListener {
    
    private static final double HULL_PRIORITY_MULTIPLIER = 1.2;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    
    /**
     * You tried to perform an action that is not allowed.
     * An error occurred, and we are unable to recover from this.
     * You will also be disconnected.
     */
    @Override
    public void onErrorServerMessage(HtfClient client, ErrorServerMessage msg) throws Exception {
        System.err.println("FATAL ERROR: " + msg.getMsg());
    }

    /**
     * The game finished. Did you win?
     */
    @Override
    public void onGameEndedServerMessage(HtfClient client, GameEndedServerMessage msg) throws Exception {
        System.out.println("\n========== GAME ENDED ==========");
        System.out.println("Final Leaderboard:");
        msg.getLeaderboard().forEach(team -> {
            System.out.printf("  %s - Last Round: %d | Points: %s%n",
                team.getName(),
                team.getLastRound(),
                team.getPoints()
            );
        });
    }

    /**
     * A new round has started.
     * You must reply within 1 second!
     */
    @Override
    public void onGameRoundServerMessage(HtfClient client, GameRoundServerMessage msg) throws Exception {
        logRoundStatus(msg);
        
        List<Long> selectedActions = selectOptimalActions(msg);
        
        System.out.println("Selected " + selectedActions.size() + " action(s): " + selectedActions);
        client.send(new SelectActionsClientMessage(msg.getRoundId(), selectedActions));
    }

    /**
     * You tried to perform an action that is not allowed.
     * An error occurred but you can still play along.
     * You will NOT be disconnected.
     */
    @Override
    public void onWarningServerMessage(HtfClient client, WarningServerMessage msg) throws Exception {
        System.err.println("WARNING: " + msg.getMsg());
    }
    
    /**
     * Logs current round status for debugging and monitoring.
     */
    private void logRoundStatus(GameRoundServerMessage msg) {
        System.out.println("\n========== ROUND " + msg.getRound() + " ==========");
        
        GameRoundServerMessage.Values ourValues = msg.getOurSubmarine().getValues();
        System.out.printf("Hull: %s/%s (%.1f%%)%n",
            ourValues.getHullStrength(),
            ourValues.getMaxHullStrength(),
            getPercentage(ourValues.getHullStrength(), ourValues.getMaxHullStrength())
        );
        System.out.printf("Crew: %s/%s (%.1f%%)%n",
            ourValues.getCrewHealth(),
            ourValues.getMaxCrewHealth(),
            getPercentage(ourValues.getCrewHealth(), ourValues.getMaxCrewHealth())
        );
        System.out.println("Effects: " + msg.getEffects().size());
        System.out.println("Actions available: " + msg.getActions().size());
    }
    
    /**
     * Selects the optimal sequence of actions to maximize survival.
     * Strategy:
     * 1. Identify critical threats (effects that would kill us)
     * 2. Evaluate all actions by net benefit
     * 3. Execute actions in priority order while affordable
     */
    private List<Long> selectOptimalActions(GameRoundServerMessage msg) {
        GameRoundServerMessage.Values currentValues = msg.getOurSubmarine().getValues();
        List<GameRoundServerMessage.Effect> effects = msg.getEffects();
        List<GameRoundServerMessage.Action> actions = msg.getActions();
        
        // Evaluate each action
        List<ActionEvaluation> evaluations = actions.stream()
            .map(action -> evaluateAction(action, effects, currentValues))
            .sorted(Comparator.comparingInt(ActionEvaluation::getPriority)
                .thenComparing(Comparator.comparingDouble(ActionEvaluation::getNetBenefit).reversed()))
            .collect(Collectors.toList());
        
        // Simulate execution and select affordable actions
        return simulateAndSelectActions(evaluations, currentValues);
    }
    
    /**
     * Evaluates a single action's value and priority.
     */
    private ActionEvaluation evaluateAction(GameRoundServerMessage.Action action, 
                                           List<GameRoundServerMessage.Effect> effects, 
                                           GameRoundServerMessage.Values currentValues) {
        GameRoundServerMessage.Values actionValues = action.getValues();
        
        // Calculate immediate impact (null-safe)
        double hullChange = actionValues.getHullStrength() != null ? actionValues.getHullStrength().doubleValue() : 0.0;
        double crewChange = actionValues.getCrewHealth() != null ? actionValues.getCrewHealth().doubleValue() : 0.0;
        double maxHullChange = actionValues.getMaxHullStrength() != null ? actionValues.getMaxHullStrength().doubleValue() : 0.0;
        double maxCrewChange = actionValues.getMaxCrewHealth() != null ? actionValues.getMaxCrewHealth().doubleValue() : 0.0;
        
        // Check if action prevents an effect
        double effectBenefit = 0;
        boolean isCritical = false;
        
        if (action.getEffectId() != -1) {
            GameRoundServerMessage.Effect preventedEffect = findEffectById(effects, action.getEffectId());
            
            if (preventedEffect != null) {
                GameRoundServerMessage.Values effectValues = preventedEffect.getValues();
                
                // Benefit = damage prevented (null-safe)
                double effectHullDamage = effectValues.getHullStrength() != null ? effectValues.getHullStrength().doubleValue() : 0.0;
                double effectCrewDamage = effectValues.getCrewHealth() != null ? effectValues.getCrewHealth().doubleValue() : 0.0;
                effectBenefit = -effectHullDamage + -effectCrewDamage;
                
                // Check if effect would be fatal
                GameRoundServerMessage.Values afterEffect = safeSum(currentValues, effectValues);
                if (isDeadly(afterEffect)) {
                    isCritical = true;
                }
            }
        }
        
        // Calculate net benefit (prioritize hull slightly)
        double netBenefit = effectBenefit 
                          + maxHullChange 
                          + maxCrewChange 
                          + (hullChange * HULL_PRIORITY_MULTIPLIER) 
                          + crewChange;
        
        return new ActionEvaluation(action, netBenefit, isCritical);
    }
    
    /**
     * Simulates action execution step-by-step and selects those we can afford.
     */
    private List<Long> simulateAndSelectActions(List<ActionEvaluation> evaluations, 
                                               GameRoundServerMessage.Values startingValues) {
        List<Long> selectedActionIds = new ArrayList<>();
        GameRoundServerMessage.Values currentValues = copyValues(startingValues);
        Set<Long> stoppedEffects = new HashSet<>();
        
        for (ActionEvaluation eval : evaluations) {
            // Skip if effect already stopped
            if (eval.getAction().getEffectId() != -1 
                && stoppedEffects.contains(eval.getAction().getEffectId())) {
                continue;
            }
            
            // Simulate applying this action
            GameRoundServerMessage.Values afterAction = safeSum(currentValues, eval.getAction().getValues());
            
            // Check if we survive this action
            if (canSurvive(afterAction)) {
                // Take action if critical, or if net benefit is positive
                if (eval.isCritical() || eval.getNetBenefit() > 0) {
                    selectedActionIds.add(eval.getAction().getId());
                    currentValues = afterAction;
                    
                    if (eval.getAction().getEffectId() != -1) {
                        stoppedEffects.add(eval.getAction().getEffectId());
                    }
                }
            }
        }
        
        return selectedActionIds;
    }
    
    /**
     * Null-safe sum of two Values objects.
     */
    private GameRoundServerMessage.Values safeSum(GameRoundServerMessage.Values original, GameRoundServerMessage.Values change) {
        GameRoundServerMessage.Values result = new GameRoundServerMessage.Values();
        
        // Get original values with null safety
        BigDecimal origHull = original.getHullStrength() != null ? original.getHullStrength() : ZERO;
        BigDecimal origMaxHull = original.getMaxHullStrength() != null ? original.getMaxHullStrength() : ZERO;
        BigDecimal origCrew = original.getCrewHealth() != null ? original.getCrewHealth() : ZERO;
        BigDecimal origMaxCrew = original.getMaxCrewHealth() != null ? original.getMaxCrewHealth() : ZERO;
        
        // Get change values with null safety
        BigDecimal changeHull = change.getHullStrength() != null ? change.getHullStrength() : ZERO;
        BigDecimal changeMaxHull = change.getMaxHullStrength() != null ? change.getMaxHullStrength() : ZERO;
        BigDecimal changeCrew = change.getCrewHealth() != null ? change.getCrewHealth() : ZERO;
        BigDecimal changeMaxCrew = change.getMaxCrewHealth() != null ? change.getMaxCrewHealth() : ZERO;
        
        // Calculate new max values
        BigDecimal newMaxHull = origMaxHull.add(changeMaxHull).max(ZERO);
        BigDecimal newMaxCrew = origMaxCrew.add(changeMaxCrew).max(ZERO);
        
        // Calculate new current values (capped at max, floored at zero)
        BigDecimal newHull = origHull.add(changeHull).min(newMaxHull).max(ZERO);
        BigDecimal newCrew = origCrew.add(changeCrew).min(newMaxCrew).max(ZERO);
        
        result.setHullStrength(newHull);
        result.setMaxHullStrength(newMaxHull);
        result.setCrewHealth(newCrew);
        result.setMaxCrewHealth(newMaxCrew);
        
        return result;
    }
    
    /**
     * Creates a copy of Values object.
     */
    private GameRoundServerMessage.Values copyValues(GameRoundServerMessage.Values values) {
        GameRoundServerMessage.Values copy = new GameRoundServerMessage.Values();
        copy.setHullStrength(values.getHullStrength());
        copy.setMaxHullStrength(values.getMaxHullStrength());
        copy.setCrewHealth(values.getCrewHealth());
        copy.setMaxCrewHealth(values.getMaxCrewHealth());
        return copy;
    }
    
    /**
     * Finds an effect by its ID.
     */
    private GameRoundServerMessage.Effect findEffectById(List<GameRoundServerMessage.Effect> effects, long effectId) {
        return effects.stream()
            .filter(e -> e.getId() == effectId)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Checks if the given values would result in death.
     */
    private boolean isDeadly(GameRoundServerMessage.Values values) {
        BigDecimal hull = values.getHullStrength();
        BigDecimal crew = values.getCrewHealth();
        return (hull != null && hull.compareTo(ZERO) <= 0) 
            || (crew != null && crew.compareTo(ZERO) <= 0);
    }
    
    /**
     * Checks if the submarine can survive with the given values.
     */
    private boolean canSurvive(GameRoundServerMessage.Values values) {
        BigDecimal hull = values.getHullStrength();
        BigDecimal crew = values.getCrewHealth();
        return (hull == null || hull.compareTo(ZERO) > 0) 
            && (crew == null || crew.compareTo(ZERO) > 0);
    }
    
    /**
     * Calculates percentage of current vs max value.
     */
    private double getPercentage(BigDecimal current, BigDecimal max) {
        if (current == null || max == null || max.compareTo(ZERO) == 0) {
            return 0.0;
        }
        return current.divide(max, 4, RoundingMode.HALF_UP)
                     .multiply(new BigDecimal("100"))
                     .doubleValue();
    }
    
    /**
     * Inner class to hold action evaluation results.
     */
    private static class ActionEvaluation {
        private final GameRoundServerMessage.Action action;
        private final double netBenefit;
        private final boolean critical;
        
        public ActionEvaluation(GameRoundServerMessage.Action action, double netBenefit, boolean critical) {
            this.action = action;
            this.netBenefit = netBenefit;
            this.critical = critical;
        }
        
        public GameRoundServerMessage.Action getAction() {
            return action;
        }
        
        public double getNetBenefit() {
            return netBenefit;
        }
        
        public boolean isCritical() {
            return critical;
        }
        
        /**
         * Returns priority level (lower = higher priority).
         * 0 = critical (life-threatening)
         * 1 = normal
         */
        public int getPriority() {
            return critical ? 0 : 1;
        }
    }
}
