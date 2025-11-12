package be.thebeehive.htf.client;

import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage;
import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage.Action;
import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage.Effect;
import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage.Values;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static be.thebeehive.htf.client.ClientUtils.isDead;
import static be.thebeehive.htf.client.ClientUtils.sumValues;

public class DecisionEngine {

    private Map<Long, Action> actionById = new HashMap<>();
    private Map<Long, Long> actionToEffectId = new HashMap<>();
    private Map<Long, Values> actionToValues = new HashMap<>();
    private Map<Long, Effect> effectById = new HashMap<>();

    public List<Long> decideActions(GameRoundServerMessage msg, int maxActionsHint) {
        List<Action> actions = msg.getActions();
        List<Effect> effects = msg.getEffects();
        Values start = cloneValues(msg.getOurSubmarine().getValues());

        this.actionById.clear();
        this.actionToEffectId.clear();
        this.actionToValues.clear();
        this.effectById.clear();

        for (Effect e : effects) effectById.put(e.getId(), e);
        Map<Long, List<Action>> actionsByEffect = new HashMap<>();
        List<Action> freeActions = new ArrayList<>();
        for (Action a : actions) {
            actionById.put(a.getId(), a);
            actionToEffectId.put(a.getId(), a.getEffectId());
            actionToValues.put(a.getId(), a.getValues());
            if (a.getEffectId() >= 0 && effectById.containsKey(a.getEffectId())) {
                actionsByEffect.computeIfAbsent(a.getEffectId(), k -> new ArrayList<>()).add(a);
            } else {
                freeActions.add(a);
            }
        }

        // Calculate health percentage to determine if we're in critical state
        // Check BOTH hull strength and crew health independently
        BigDecimal hullPercent = start.getMaxHullStrength().compareTo(BigDecimal.ZERO) > 0 ?
                start.getHullStrength().divide(start.getMaxHullStrength(), 2, java.math.RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
        BigDecimal crewPercent = start.getMaxCrewHealth().compareTo(BigDecimal.ZERO) > 0 ?
                start.getCrewHealth().divide(start.getMaxCrewHealth(), 2, java.math.RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
        
        // Critical if EITHER hull or crew is below 30% (balanced threshold)
        boolean criticalHealth = hullPercent.compareTo(new BigDecimal("0.30")) < 0 || 
                                 crewPercent.compareTo(new BigDecimal("0.30")) < 0;
        
        // Very critical if EITHER is below 15% (emergency!)
        boolean veryCritical = hullPercent.compareTo(new BigDecimal("0.15")) < 0 || 
                              crewPercent.compareTo(new BigDecimal("0.15")) < 0;

        // Separate effects into critical and non-critical
        List<Effect> criticalEffects = new ArrayList<>();
        List<Effect> normalEffects = new ArrayList<>();
        
        for (Effect e : effects) {
            Values testState = cloneValues(start);
            testState = sumValues(testState, e.getValues());
            if (isDead(testState) || wouldBeCritical(testState)) {
                criticalEffects.add(e);
            } else {
                normalEffects.add(e);
            }
        }

        // Sort critical effects by step first, then by damage
        criticalEffects.sort(
                Comparator.comparingInt(Effect::getStep)
                        .thenComparing((Effect e) -> harmScore(e.getValues()), Comparator.reverseOrder())
        );
        
        normalEffects.sort(
                Comparator.comparingInt(Effect::getStep)
                        .thenComparing((Effect e) -> harmScore(e.getValues()), Comparator.reverseOrder())
        );

        List<Action> chosen = new ArrayList<>();
        Set<Long> blockedEffectIds = new HashSet<>();

        // PRIORITY 1: Block all critical threats
        for (Effect e : criticalEffects) {
            List<Action> candidates = actionsByEffect.get(e.getId());
            if (candidates == null || candidates.isEmpty()) continue;
            
            // Find the best action to block this effect (least self-harm, most benefit)
            Action best = candidates.stream()
                    .min(Comparator.comparing((Action a) -> actionSelfHarmScore(a.getValues()))
                            .thenComparing((Action a) -> actionBenefitScore(a.getValues()), Comparator.reverseOrder()))
                    .orElse(null);
            if (best == null) continue;
            
            if (!chosen.contains(best)) {
                chosen.add(best);
                blockedEffectIds.add(e.getId());
                if (chosen.size() >= maxActionsHint) break;
            }
        }

        // PRIORITY 2: If health is critical, add healing actions AGGRESSIVELY
        if (criticalHealth && chosen.size() < maxActionsHint) {
            List<Action> healingActions = freeActions.stream()
                    .filter(a -> isHealingAction(a.getValues()))
                    .sorted(Comparator.comparing((Action a) -> healingScore(a.getValues())).reversed())
                    .collect(Collectors.toList());
            
            // If very critical, add MORE healing actions (up to 40% of slots)
            int healingSlotsNeeded = veryCritical ? 
                    Math.max(1, (int)(maxActionsHint * 0.4)) : 
                    Math.max(1, (int)(maxActionsHint * 0.25));
            
            int healingAdded = 0;
            for (Action a : healingActions) {
                if (!chosen.contains(a) && healingAdded < healingSlotsNeeded) {
                    chosen.add(a);
                    healingAdded++;
                    if (chosen.size() >= maxActionsHint) break;
                }
            }
        }

        // PRIORITY 3: Block remaining harmful effects if we have space
        for (Effect e : normalEffects) {
            if (chosen.size() >= maxActionsHint) break;
            List<Action> candidates = actionsByEffect.get(e.getId());
            if (candidates == null || candidates.isEmpty()) continue;
            
            Action best = candidates.stream()
                    .filter(a -> !chosen.contains(a))
                    .min(Comparator.comparing((Action a) -> actionSelfHarmScore(a.getValues()))
                            .thenComparing((Action a) -> actionBenefitScore(a.getValues()), Comparator.reverseOrder()))
                    .orElse(null);
            
            if (best != null) {
                chosen.add(best);
                blockedEffectIds.add(e.getId());
            }
        }

        // PRIORITY 4: Fill remaining slots with best beneficial actions
        if (chosen.size() < maxActionsHint) {
            List<Action> remainingFree = freeActions.stream()
                    .filter(a -> !chosen.contains(a))
                    .sorted(Comparator.comparing((Action a) -> overallActionScore(a.getValues(), criticalHealth)).reversed())
                    .collect(Collectors.toList());
            
            for (Action a : remainingFree) {
                if (chosen.size() >= maxActionsHint) break;
                chosen.add(a);
            }
        }

        List<Long> order = buildOptimalOrder(chosen, effects, start, maxActionsHint);
        List<Long> safe = improveAndSanityCheck(order, start, effects);
        return safe;
    }

    private List<Long> buildOptimalOrder(List<Action> chosen,
                                         List<Effect> effects,
                                         Values start,
                                         int maxActionsHint) {
        // Separate actions into categories
        List<Action> blockingActions = new ArrayList<>();
        List<Action> healingActions = new ArrayList<>();
        List<Action> otherActions = new ArrayList<>();
        
        for (Action a : chosen) {
            if (a.getEffectId() >= 0 && effectById.containsKey(a.getEffectId())) {
                blockingActions.add(a);
            } else if (isHealingAction(a.getValues())) {
                healingActions.add(a);
            } else {
                otherActions.add(a);
            }
        }
        
        // Sort blocking actions by effect step (must be done before the effect triggers)
        blockingActions.sort(Comparator.comparingInt(a -> {
            Effect e = effectById.get(a.getEffectId());
            return e == null ? Integer.MAX_VALUE : e.getStep();
        }));
        
        // Sort healing by benefit
        healingActions.sort(Comparator.comparing((Action a) -> healingScore(a.getValues()), Comparator.reverseOrder()));
        
        // Sort other actions by overall benefit
        otherActions.sort(Comparator.comparing((Action a) -> actionBenefitScore(a.getValues()), Comparator.reverseOrder()));
        
        // Build order: interleave healing before blocking if we're low health, otherwise block first
        List<Action> ordered = new ArrayList<>();
        int blockIdx = 0, healIdx = 0, otherIdx = 0;
        
        Values current = cloneValues(start);
        boolean needsHealing = wouldBeCritical(current);
        
        while (ordered.size() < maxActionsHint && ordered.size() < chosen.size()) {
            // If critical health and healing available, prioritize healing
            if (needsHealing && healIdx < healingActions.size()) {
                Action a = healingActions.get(healIdx++);
                ordered.add(a);
                current = sumValues(current, a.getValues());
                needsHealing = wouldBeCritical(current);
            }
            // Check if we need to block an effect at this step
            else if (blockIdx < blockingActions.size()) {
                Action a = blockingActions.get(blockIdx);
                Effect e = effectById.get(a.getEffectId());
                int nextStep = ordered.size() + 1;
                
                // Must block before the effect triggers
                if (e != null && nextStep <= e.getStep()) {
                    ordered.add(a);
                    blockIdx++;
                    current = sumValues(current, a.getValues());
                    needsHealing = wouldBeCritical(current);
                } else if (healIdx < healingActions.size()) {
                    // Effect not urgent, do healing if available
                    Action heal = healingActions.get(healIdx++);
                    ordered.add(heal);
                    current = sumValues(current, heal.getValues());
                    needsHealing = wouldBeCritical(current);
                } else if (otherIdx < otherActions.size()) {
                    // Do other actions
                    Action other = otherActions.get(otherIdx++);
                    ordered.add(other);
                    current = sumValues(current, other.getValues());
                    needsHealing = wouldBeCritical(current);
                } else {
                    // Add blocking action even if late
                    ordered.add(a);
                    blockIdx++;
                    current = sumValues(current, a.getValues());
                    needsHealing = wouldBeCritical(current);
                }
            }
            // Fill with other actions
            else if (otherIdx < otherActions.size()) {
                Action a = otherActions.get(otherIdx++);
                ordered.add(a);
                current = sumValues(current, a.getValues());
                needsHealing = wouldBeCritical(current);
            }
            // Fill with remaining healing
            else if (healIdx < healingActions.size()) {
                Action a = healingActions.get(healIdx++);
                ordered.add(a);
                current = sumValues(current, a.getValues());
                needsHealing = wouldBeCritical(current);
            } else {
                break;
            }
        }
        
        List<Long> ids = new ArrayList<>();
        for (Action a : ordered) {
            ids.add(a.getId());
        }
        return ids;
    }

    private List<Long> improveAndSanityCheck(List<Long> actionIds,
                                             Values start,
                                             List<Effect> effects) {
        // Don't return empty list even if we predict death - always try to survive!
        // The simulation might be wrong, and doing nothing guarantees death
        if (actionIds.isEmpty()) {
            return actionIds;
        }
        
        Values v = cloneValues(start);
        int maxStep = Math.max(actionIds.size(), effects.stream().mapToInt(Effect::getStep).max().orElse(0));
        
        // Simulate to verify actions are beneficial, but don't reject them if we predict death
        for (int s = 1; s <= maxStep; s++) {
            int idx = s - 1;
            if (idx >= 0 && idx < actionIds.size()) {
                Values delta = findActionValues(actionIds.get(idx));
                if (delta != null) v = sumValues(v, delta);
                // Removed: if (isDead(v)) return new ArrayList<>();
                // Better to try than to give up!
            }
            for (Effect e : effects) {
                if (e.getStep() == s) {
                    boolean blocked = hasActionAtOrBefore(actionIds, e);
                    if (!blocked) {
                        v = sumValues(v, e.getValues());
                        // Removed: if (isDead(v)) return new ArrayList<>();
                        // Better to try than to give up!
                    }
                }
            }
        }
        
        // Always return the actions - trying is better than giving up
        return actionIds;
    }

    private boolean hasActionAtOrBefore(List<Long> actionIds, Effect e) {
        int idx = indexOfActionBlockingEffect(actionIds, e.getId());
        return idx >= 0 && (idx + 1) <= e.getStep();
    }

    private int indexOfActionBlockingEffect(List<Long> actionIds, long effectId) {
        for (int i = 0; i < actionIds.size(); i++) {
            long aId = actionIds.get(i);
            Long eid = actionToEffectId.get(aId);
            if (eid != null && eid == effectId) return i;
        }
        return -1;
    }

    private Values findActionValues(Long actionId) {
        return actionToValues.get(actionId);
    }

    private BigDecimal harmScore(Values v) {
        BigDecimal h = v.getHullStrength();
        BigDecimal c = v.getCrewHealth();
        BigDecimal negH = h == null ? BigDecimal.ZERO : h.min(BigDecimal.ZERO).abs();
        BigDecimal negC = c == null ? BigDecimal.ZERO : c.min(BigDecimal.ZERO).abs();
        // Weight both equally - both can kill you!
        return negH.add(negC);
    }

    private BigDecimal actionSelfHarmScore(Values v) {
        BigDecimal hs = v.getHullStrength() == null ? BigDecimal.ZERO : v.getHullStrength().min(BigDecimal.ZERO).abs();
        BigDecimal cs = v.getCrewHealth() == null ? BigDecimal.ZERO : v.getCrewHealth().min(BigDecimal.ZERO).abs();
        // Weight both equally - avoid damaging either
        return hs.add(cs);
    }

    private BigDecimal actionBenefitScore(Values v) {
        BigDecimal hs = v.getHullStrength() == null ? BigDecimal.ZERO : v.getHullStrength().max(BigDecimal.ZERO);
        BigDecimal cs = v.getCrewHealth() == null ? BigDecimal.ZERO : v.getCrewHealth().max(BigDecimal.ZERO);
        BigDecimal mhs = v.getMaxHullStrength() == null ? BigDecimal.ZERO : v.getMaxHullStrength().max(BigDecimal.ZERO);
        BigDecimal mcs = v.getMaxCrewHealth() == null ? BigDecimal.ZERO : v.getMaxCrewHealth().max(BigDecimal.ZERO);
        // All positive changes are beneficial - prioritize current health slightly more
        return hs.add(cs).multiply(new BigDecimal("1.2")).add(mhs).add(mcs);
    }
    
    private boolean wouldBeCritical(Values v) {
        BigDecimal hullPercent = v.getMaxHullStrength().compareTo(BigDecimal.ZERO) > 0 ? 
            v.getHullStrength().divide(v.getMaxHullStrength(), 2, java.math.RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
        BigDecimal crewPercent = v.getMaxCrewHealth().compareTo(BigDecimal.ZERO) > 0 ? 
            v.getCrewHealth().divide(v.getMaxCrewHealth(), 2, java.math.RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
        // Critical if EITHER metric is below 45% (conservative)
        return hullPercent.compareTo(new BigDecimal("0.45")) < 0 || 
               crewPercent.compareTo(new BigDecimal("0.45")) < 0;
    }
    
    private boolean isHealingAction(Values v) {
        BigDecimal hs = v.getHullStrength() == null ? BigDecimal.ZERO : v.getHullStrength();
        BigDecimal cs = v.getCrewHealth() == null ? BigDecimal.ZERO : v.getCrewHealth();
        // Any positive hull OR crew change is healing
        return hs.compareTo(BigDecimal.ZERO) > 0 || cs.compareTo(BigDecimal.ZERO) > 0;
    }
    
    private BigDecimal healingScore(Values v) {
        BigDecimal hs = v.getHullStrength() == null ? BigDecimal.ZERO : v.getHullStrength().max(BigDecimal.ZERO);
        BigDecimal cs = v.getCrewHealth() == null ? BigDecimal.ZERO : v.getCrewHealth().max(BigDecimal.ZERO);
        // Sum both hull and crew healing equally
        return hs.add(cs);
    }
    
    private BigDecimal overallActionScore(Values v, boolean criticalHealth) {
        BigDecimal benefit = actionBenefitScore(v);
        BigDecimal harm = actionSelfHarmScore(v);
        
        // If we're in critical health, HEAVILY prioritize healing and avoid ANY harm
        if (criticalHealth) {
            BigDecimal healing = healingScore(v);
            // 4x healing bonus, 10x harm penalty when critical
            return benefit.add(healing.multiply(new BigDecimal("4"))).subtract(harm.multiply(new BigDecimal("10")));
        }
        
        // Otherwise balance benefit and avoid harm (but still avoid harm strongly)
        return benefit.subtract(harm.multiply(new BigDecimal("3")));
    }

    private Values cloneValues(Values v) {
        Values c = new Values();
        c.setHullStrength(v.getHullStrength());
        c.setMaxHullStrength(v.getMaxHullStrength());
        c.setCrewHealth(v.getCrewHealth());
        c.setMaxCrewHealth(v.getMaxCrewHealth());
        return c;
    }
}
