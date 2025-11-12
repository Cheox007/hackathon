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

        List<Effect> harmfulEffects = new ArrayList<>(effects);
        harmfulEffects.sort(
                Comparator.comparingInt(Effect::getStep)
                        .thenComparing((Effect e) -> harmScore(e.getValues()), Comparator.reverseOrder())
        );

        List<Action> chosen = new ArrayList<>();
        Set<Long> blockedEffectIds = new HashSet<>();

        for (Effect e : harmfulEffects) {
            List<Action> candidates = actionsByEffect.get(e.getId());
            if (candidates == null || candidates.isEmpty()) continue;
            int nextStep = chosen.size() + 1;
            if (nextStep > e.getStep()) continue;
            Action best = candidates.stream()
                    .min(Comparator.comparing((Action a) -> actionSelfHarmScore(a.getValues())))
                    .orElse(null);
            if (best == null) continue;
            chosen.add(best);
            blockedEffectIds.add(e.getId());
            if (chosen.size() >= maxActionsHint) break;
        }

        if (chosen.size() < maxActionsHint) {
            freeActions.sort(Comparator.comparing((Action a) -> actionBenefitScore(a.getValues())).reversed());
            for (Action a : freeActions) {
                if (chosen.contains(a)) continue;
                chosen.add(a);
                if (chosen.size() >= maxActionsHint) break;
            }
        }

        List<Long> order = buildFeasibleOrder(chosen, effects, blockedEffectIds, maxActionsHint);

        List<Long> safe = improveAndSanityCheck(order, start, effects);
        return safe;
    }

    private List<Long> buildFeasibleOrder(List<Action> chosen,
                                          List<Effect> effects,
                                          Set<Long> blockedEffectIds,
                                          int maxActionsHint) {
        List<Action> ordered = new ArrayList<>(chosen);
        ordered.sort(Comparator.comparingLong(a -> {
            Effect e = effectById.get(a.getEffectId());
            return e == null ? Integer.MAX_VALUE : e.getStep();
        }));

        List<Long> ids = new ArrayList<>();
        for (Action a : ordered) {
            if (ids.contains(a.getId())) continue;
            ids.add(a.getId());
            if (ids.size() >= maxActionsHint) break;
        }
        return ids;
    }

    private List<Long> improveAndSanityCheck(List<Long> actionIds,
                                             Values start,
                                             List<Effect> effects) {
        Values v = cloneValues(start);
        int maxStep = Math.max(actionIds.size(), effects.stream().mapToInt(Effect::getStep).max().orElse(0));
        for (int s = 1; s <= maxStep; s++) {
            int idx = s - 1;
            if (idx >= 0 && idx < actionIds.size()) {
                Values delta = findActionValues(actionIds.get(idx));
                if (delta != null) v = sumValues(v, delta);
                if (isDead(v)) return new ArrayList<>();
            }
            for (Effect e : effects) {
                if (e.getStep() == s) {
                    boolean blocked = hasActionAtOrBefore(actionIds, e);
                    if (!blocked) {
                        v = sumValues(v, e.getValues());
                        if (isDead(v)) return new ArrayList<>();
                    }
                }
            }
        }
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

    private long findEffectIdForAction(Long actionId) {
        return actionToEffectId.getOrDefault(actionId, -1L);
    }

    private Values findActionValues(Long actionId) {
        return actionToValues.get(actionId);
    }

    private BigDecimal harmScore(Values v) {
        BigDecimal h = v.getHullStrength();
        BigDecimal c = v.getCrewHealth();
        BigDecimal negH = h == null ? BigDecimal.ZERO : h.min(BigDecimal.ZERO).abs();
        BigDecimal negC = c == null ? BigDecimal.ZERO : c.min(BigDecimal.ZERO).abs();
        return negH.add(negC);
    }

    private BigDecimal actionSelfHarmScore(Values v) {
        BigDecimal hs = v.getHullStrength() == null ? BigDecimal.ZERO : v.getHullStrength().min(BigDecimal.ZERO).abs();
        BigDecimal cs = v.getCrewHealth() == null ? BigDecimal.ZERO : v.getCrewHealth().min(BigDecimal.ZERO).abs();
        return hs.add(cs);
    }

    private BigDecimal actionBenefitScore(Values v) {
        BigDecimal hs = v.getHullStrength() == null ? BigDecimal.ZERO : v.getHullStrength().max(BigDecimal.ZERO);
        BigDecimal cs = v.getCrewHealth() == null ? BigDecimal.ZERO : v.getCrewHealth().max(BigDecimal.ZERO);
        BigDecimal mhs = v.getMaxHullStrength() == null ? BigDecimal.ZERO : v.getMaxHullStrength().max(BigDecimal.ZERO);
        BigDecimal mcs = v.getMaxCrewHealth() == null ? BigDecimal.ZERO : v.getMaxCrewHealth().max(BigDecimal.ZERO);
        return hs.add(cs).add(mhs).add(mcs);
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
