package be.thebeehive.htf.library.protocol.server;

/**
 * Represents an error message received from the server.
 */
public class ErrorServerMessage extends ServerMessage {

    private String msg;

    public ErrorServerMessage() {

    }

    /**
     * Returns the error message.
     *
     * @return the error message as a String
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Sets the error message.
     *
     * @param msg the error message to set
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }
}
