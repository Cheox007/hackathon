package be.thebeehive.htf.library.protocol.server;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "_type"
)
@JsonSubTypes({
        @Type(value = ErrorServerMessage.class, name = "ErrorServerMessage"),
        @Type(value = GameEndedServerMessage.class, name = "GameEndedServerMessage"),
        @Type(value = GameRoundServerMessage.class, name = "GameRoundServerMessage"),
        @Type(value = WarningServerMessage.class, name = "WarningServerMessage")
})
public class ServerMessage {

}
