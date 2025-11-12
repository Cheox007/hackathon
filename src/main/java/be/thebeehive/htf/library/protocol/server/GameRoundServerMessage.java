package be.thebeehive.htf.library.protocol.server;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The GameRoundServerMessage class represents a server message that contains information about
 * the current game round. This includes details such as round number, unique round identifier,
 * next checkpoint, effects and actions relevant to the current game round, and information
 * about the submarines (our submarine and competing submarines).
 */
public class GameRoundServerMessage extends ServerMessage {

    private long round;
    private UUID roundId;
    private Checkpoint nextCheckpoint;
    private List<Effect> effects;
    private List<Action> actions;
    private Submarine ourSubmarine;
    private List<Submarine> competingSubmarines;

    public GameRoundServerMessage() {

    }

    /**
     * Retrieves the current round number of the game.
     *
     * @return the current round number as a long.
     */
    public long getRound() {
        return round;
    }

    /**
     * Sets the current round number of the game.
     *
     * @param round the round number to be set as a long.
     */
    public void setRound(long round) {
        this.round = round;
    }

    /**
     * Retrieves the unique identifier of the current game round.
     * This is must be provided in the SelectActionsClientMessage.
     *
     * @return the unique identifier (UUID) of the current game round.
     */
    public UUID getRoundId() {
        return roundId;
    }

    /**
     * Sets the unique identifier for the current game round.
     *
     * @param roundId the unique identifier (UUID) to be set for the current game round
     */
    public void setRoundId(UUID roundId) {
        this.roundId = roundId;
    }

    /**
     * Retrieves the next checkpoint that will be reached.
     *
     * @return the next {@link Checkpoint}
     */
    public Checkpoint getNextCheckpoint() {
        return nextCheckpoint;
    }

    /**
     * Sets the next checkpoint that the submarine will reach.
     *
     * @param nextCheckpoint the next {@link Checkpoint} to be reached.
     */
    public void setNextCheckpoint(Checkpoint nextCheckpoint) {
        this.nextCheckpoint = nextCheckpoint;
    }

    /**
     * Retrieves the list of effects that will occur this round.
     *
     * @return a list of {@link Effect} objects.
     */
    public List<Effect> getEffects() {
        return effects;
    }

    /**
     * Sets the list of effects that will occur during this game round.
     *
     * @param effects the list of {@link Effect} objects to be set.
     */
    public void setEffects(List<Effect> effects) {
        this.effects = effects;
    }

    /**
     * Retrieves the list of actions that are available this round.
     *
     * @return a list of {@link Action} objects.
     */
    public List<Action> getActions() {
        return actions;
    }

    /**
     * Sets the list of actions that are available for this game round.
     *
     * @param actions the list of {@link Action} objects to be set
     */
    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    /**
     * Retrieves our submarine.
     *
     * @return the current instance of our submarine.
     */
    public Submarine getOurSubmarine() {
        return ourSubmarine;
    }

    /**
     * Sets our submarine for the current game.
     *
     * @param ourSubmarine the instance of our {@link Submarine} to be set.
     */
    public void setOurSubmarine(Submarine ourSubmarine) {
        this.ourSubmarine = ourSubmarine;
    }

    /**
     * Retrieves the list of competing submarines for the current game.
     *
     * @return a list of {@link Submarine} objects representing the competing submarines.
     */
    public List<Submarine> getCompetingSubmarines() {
        return competingSubmarines;
    }

    /**
     * Sets the list of competing submarines for the current game.
     *
     * @param competingSubmarines the list of {@link Submarine} objects to be set as competing submarines.
     */
    public void setCompetingSubmarines(List<Submarine> competingSubmarines) {
        this.competingSubmarines = competingSubmarines;
    }

    /**
     * Represents a checkpoint within a game round.
     * A checkpoint is characterized by a specific round and the values that will be
     * applied to the submarine when the checkpoint is reached.
     */
    public static class Checkpoint {

        private long round;
        private Values values;

        public Checkpoint() {

        }

        /**
         * The checkpoint will be reached at the start of this round.
         *
         * @return the round as a long.
         */
        public long getRound() {
            return round;
        }

        /**
         * Sets the round at which the checkpoint will be reached.
         *
         * @param round the round number as a long.
         */
        public void setRound(long round) {
            this.round = round;
        }

        /**
         * The values that will be applied to the submarine when the checkpoint is reached
         *
         * @return the current Values instance.
         */
        public Values getValues() {
            return values;
        }

        /**
         * Sets the values that will be applied to the submarine when the checkpoint is reached.
         *
         * @param values the new Values instance to be set.
         */
        public void setValues(Values values) {
            this.values = values;
        }
    }

    /**
     * Represents an effect within a game round.
     * This effect includes an identifier, a step value indicating when the effect
     * will be triggered, and the values that will be applied when the effect is triggered.
     */
    public static class Effect {

        private long id;
        private int step;
        private Values values;

        public Effect() {

        }

        /**
         * Retrieves the ID associated with this effect.
         *
         * @return the ID as a long value.
         */
        public long getId() {
            return id;
        }

        /**
         * Sets the ID associated with this effect.
         *
         * @param id the new ID as a long value.
         */
        public void setId(long id) {
            this.id = id;
        }

        /**
         * Retrieves the step when the effect will be triggered.
         *
         * @return the step as an integer.
         */
        public int getStep() {
            return step;
        }

        /**
         * Sets the step when the effect will be triggered.
         *
         * @param step the step value to set.
         */
        public void setStep(int step) {
            this.step = step;
        }

        /**
         * The values that will be applied to the submarine when the effect is NOT removed
         *
         * @return the current values as a Values object.
         */
        public Values getValues() {
            return values;
        }

        /**
         * Sets the values that will be applied to the submarine when the effect is not removed.
         *
         * @param values the Values object containing hull strength and crew health information.
         */
        public void setValues(Values values) {
            this.values = values;
        }
    }

    /**
     * Represents an action within a game round.
     * An action is characterized by an identifier, an optional associated effect ID,
     * and the values that apply to the submarine upon action execution.
     */
    public static class Action {

        private long id;
        private long effectId;
        private Values values;

        public Action() {

        }

        /**
         * Retrieves the ID of the action.
         *
         * @return the ID of the action as a long.
         */
        public long getId() {
            return id;
        }

        /**
         * Sets the ID for this action.
         *
         * @param id the new ID for the action
         */
        public void setId(long id) {
            this.id = id;
        }

        /**
         * Retrieves the ID of the effect associated with the action.
         * The effectId can be equal to -1 if the action is not coupled to any effect.
         *
         * @return the effect ID as a long. Or -1.
         */
        public long getEffectId() {
            return effectId;
        }

        /**
         * Sets the ID for the effect associated with this action.
         *
         * @param effectId the new effect ID for the action
         */
        public void setEffectId(long effectId) {
            this.effectId = effectId;
        }

        /**
         * The values that will be applied to the submarine when the action is executed
         *
         * @return the values as a {@link Values} object.
         */
        public Values getValues() {
            return values;
        }

        /**
         * Sets the values that will be applied to the submarine when the action is executed
         *
         * @param values the new values as a {@link Values} object
         */
        public void setValues(Values values) {
            this.values = values;
        }
    }

    /**
     * Represents a submarine within the game context.
     * This class handles various properties of the submarine such as its team name,
     * the values associated with its hull strength and crew health, and its alive status.
     */
    public static class Submarine {

        private String name;
        private Values values;
        private boolean alive;

        public Submarine() {

        }

        /**
         * Retrieves the name of the team which manages the submarine.
         *
         * @return the name of the team.
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name of the team which manages the submarine.
         *
         * @param name the name the team.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Retrieves the current values associated with the submarine, including hull strength and crew health details.
         *
         * @return the current values as a {@link Values} object.
         */
        public Values getValues() {
            return values;
        }

        /**
         * Sets the current values associated with the submarine, including hull strength and crew health details.
         *
         * @param values the new values to be set as a {@link Values} object.
         */
        public void setValues(Values values) {
            this.values = values;
        }

        /**
         * Checks if the submarine is currently alive.
         *
         * @return true if the submarine is alive, false otherwise.
         */
        public boolean isAlive() {
            return alive;
        }

        /**
         * Sets the alive status of the submarine.
         *
         * @param alive true if the submarine is to be set as alive, false otherwise.
         */
        public void setAlive(boolean alive) {
            this.alive = alive;
        }
    }

    /**
     * Represents the state of various values associated with an entity, such as a submarine or game effect.
     * This class includes properties related to hull strength and crew health, each having their respective maximum values.
     */
    public static class Values {

        private BigDecimal hullStrength;
        private BigDecimal maxHullStrength;
        private BigDecimal crewHealth;
        private BigDecimal maxCrewHealth;

        public Values() {

        }

        /**
         * Retrieves the current hull strength value.
         *
         * @return the current hull strength as a BigDecimal.
         */
        public BigDecimal getHullStrength() {
            return hullStrength;
        }

        /**
         * Sets the current hull strength value.
         *
         * @param hullStrength the new hull strength value as a BigDecimal.
         */
        public void setHullStrength(BigDecimal hullStrength) {
            this.hullStrength = hullStrength;
        }

        /**
         * Retrieves the maximum hull strength value.
         *
         * @return the maximum hull strength as a BigDecimal.
         */
        public BigDecimal getMaxHullStrength() {
            return maxHullStrength;
        }

        /**
         * Sets the maximum hull strength value.
         *
         * @param maxHullStrength the new maximum hull strength value as a BigDecimal.
         */
        public void setMaxHullStrength(BigDecimal maxHullStrength) {
            this.maxHullStrength = maxHullStrength;
        }

        /**
         * Retrieves the current crew health value.
         *
         * @return the current crew health as a BigDecimal.
         */
        public BigDecimal getCrewHealth() {
            return crewHealth;
        }

        /**
         * Sets the current crew health value.
         *
         * @param crewHealth the new crew health value as a BigDecimal.
         */
        public void setCrewHealth(BigDecimal crewHealth) {
            this.crewHealth = crewHealth;
        }

        /**
         * Retrieves the maximum crew health value.
         *
         * @return the maximum crew health as a BigDecimal.
         */
        public BigDecimal getMaxCrewHealth() {
            return maxCrewHealth;
        }

        /**
         * Sets the maximum crew health value.
         *
         * @param maxCrewHealth the new maximum crew health value as a BigDecimal.
         */
        public void setMaxCrewHealth(BigDecimal maxCrewHealth) {
            this.maxCrewHealth = maxCrewHealth;
        }
    }
}
