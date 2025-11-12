package be.thebeehive.htf.client;

import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage.Values;

import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;

public class ClientUtils {

    /**
     * Sums the values of two Values objects, ensuring that the resulting
     * hull strength and crew health values do not exceed their respective maximums
     * and do not fall below zero.
     *
     * @param original the original Values object.
     * @param newValues the new Values object to be added to the original.
     * @return a new Values object containing the summed hull strength,
     *         max hull strength, crew health, and max crew health values.
     */
    public static Values sumValues(Values original, Values newValues) {
        BigDecimal maxHullStrength = original.getMaxHullStrength().add(newValues.getMaxHullStrength());
        BigDecimal maxCrewHealth = original.getMaxCrewHealth().add(newValues.getMaxCrewHealth());

        if (maxHullStrength.compareTo(ZERO) < 0) {
            maxHullStrength = ZERO;
        }

        if (maxCrewHealth.compareTo(ZERO) < 0) {
            maxCrewHealth = ZERO;
        }

        BigDecimal hullStrength = original.getHullStrength().add(newValues.getHullStrength());
        BigDecimal crewHealth = original.getCrewHealth().add(newValues.getCrewHealth());

        if (hullStrength.compareTo(maxHullStrength) > 0) {
            hullStrength = maxHullStrength;
        }

        if (hullStrength.compareTo(ZERO) < 0) {
            hullStrength = ZERO;
        }

        if (crewHealth.compareTo(maxCrewHealth) > 0) {
            crewHealth = maxCrewHealth;
        }

        if (crewHealth.compareTo(ZERO) < 0) {
            crewHealth = ZERO;
        }

        Values sum = new Values();
        sum.setHullStrength(hullStrength);
        sum.setMaxHullStrength(maxHullStrength);
        sum.setCrewHealth(crewHealth);
        sum.setMaxCrewHealth(maxCrewHealth);

        return sum;
    }

    /**
     * Determines if a submarine is considered dead based on its hull strength and crew health values.
     *
     * @param values the Values object containing hull strength and crew health metrics.
     * @return true if the hull strength or crew health is zero, indicating the submarine is dead;
     *         false otherwise.
     */
    public static boolean isDead(Values values) {
        return values.getHullStrength().compareTo(ZERO) <= 0 ||
                values.getCrewHealth().compareTo(ZERO) <= 0;
    }

    /**
     * Determines if a submarine is considered alive based on its hull strength and crew health values.
     *
     * @param values the Values object containing hull strength and crew health metrics.
     * @return true if both the hull strength and crew health are greater than zero, indicating the submarine is alive;
     *         false if either is zero, which means the submarine is dead.
     */
    public static boolean isAlive(Values values) {
        return !isDead(values);
    }
}
