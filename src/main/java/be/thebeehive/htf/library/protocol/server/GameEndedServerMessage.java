package be.thebeehive.htf.library.protocol.server;

import java.math.BigDecimal;
import java.util.List;

public class GameEndedServerMessage extends ServerMessage {

    private long round;
    private List<LeaderboardTeam> leaderboard;

    public GameEndedServerMessage() {

    }

    /**
     * Retrieves the final round of the game.
     *
     * @return the current round number
     */
    public long getRound() {
        return round;
    }

    /**
     * Sets the final round of the game.
     *
     * @param round the round number to set
     */
    public void setRound(long round) {
        this.round = round;
    }

    /**
     * Retrieves the leaderboard of the game.
     *
     * @return a list of leaderboard teams
     */
    public List<LeaderboardTeam> getLeaderboard() {
        return leaderboard;
    }

    /**
     * Sets the leaderboard of the game.
     *
     * @param leaderboard a list of {@link LeaderboardTeam} representing the teams on the leaderboard
     */
    public void setLeaderboard(List<LeaderboardTeam> leaderboard) {
        this.leaderboard = leaderboard;
    }

    /**
     * Represents a team on the leaderboard of a game.
     */
    public static class LeaderboardTeam {

        private String name;
        private long lastRound;
        private BigDecimal points;

        public LeaderboardTeam() {

        }

        /**
         * Retrieves the name of the team.
         *
         * @return the name of the team.
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name of the team.
         *
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Retrieves the last completed round.
         *
         * @return the number of the last completed round
         */
        public long getLastRound() {
            return lastRound;
        }

        /**
         * Sets the number of the last completed round.
         *
         * @param lastRound the number of the last completed round
         */
        public void setLastRound(long lastRound) {
            this.lastRound = lastRound;
        }

        /**
         * Retrieves the points of the team.
         *
         * @return the points of the team
         */
        public BigDecimal getPoints() {
            return points;
        }

        /**
         * Sets the points of the team.
         *
         * @param points the points to set
         */
        public void setPoints(BigDecimal points) {
            this.points = points;
        }
    }
}
