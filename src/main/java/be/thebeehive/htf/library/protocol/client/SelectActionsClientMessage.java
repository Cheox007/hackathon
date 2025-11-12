package be.thebeehive.htf.library.protocol.client;

import java.util.List;
import java.util.UUID;

public class SelectActionsClientMessage extends ClientMessage {

    private UUID roundId;
    private List<Long> actionIds;

    public SelectActionsClientMessage() {

    }

    public SelectActionsClientMessage(UUID roundId, List<Long> actionIds) {
        this.roundId = roundId;
        this.actionIds = actionIds;
    }

    public UUID getRoundId() {
        return roundId;
    }

    public void setRoundId(UUID roundId) {
        this.roundId = roundId;
    }

    public List<Long> getActionIds() {
        return actionIds;
    }

    public void setActionIds(List<Long> actionIds) {
        this.actionIds = actionIds;
    }
}
