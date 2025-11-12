package be.thebeehive.htf.library;

import be.thebeehive.htf.library.protocol.server.ErrorServerMessage;
import be.thebeehive.htf.library.protocol.server.GameEndedServerMessage;
import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage;
import be.thebeehive.htf.library.protocol.server.WarningServerMessage;

public interface HtfClientListener {

    void onErrorServerMessage(HtfClient client, ErrorServerMessage msg) throws Exception;
    void onGameEndedServerMessage(HtfClient client, GameEndedServerMessage msg) throws Exception;
    void onGameRoundServerMessage(HtfClient client, GameRoundServerMessage msg) throws Exception;
    void onWarningServerMessage(HtfClient client, WarningServerMessage msg) throws Exception;

}
