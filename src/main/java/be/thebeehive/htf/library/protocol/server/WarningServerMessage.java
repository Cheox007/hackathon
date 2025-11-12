package be.thebeehive.htf.library.protocol.server;

/**
 * Represents a warning message received from the server.
 */
public class WarningServerMessage extends ServerMessage {

    private String msg;

    public WarningServerMessage() {

    }

    /**
     * Returns the warning message.
     *
     * @return the warning message as a String
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Sets the warning message.
     *
     * @param msg the warning message to set
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }
}
