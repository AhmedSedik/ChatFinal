import java.io.Serializable;

/**
 * @author zozzy on 31.12.19
 */
/*
 * This class defines the different type of messages that will be exchanged between the
 * Clients and the Server.
 * When talking from a Java Client to a Java Server a lot easier to pass Java objects, no
 * need to count bytes or to wait for a line feed at the end of the frame
 */
public class ChatMessage implements Serializable {

    public static final int PLAY_REQUEST = 3;
    protected static final long serialVersionUID = 1112122200L;

    // The different types of message sent by the Client
    // MESSAGE an ordinary message
    // LOGOUT to disconnect from the Server
    static final int OnlineUsers = 0, MESSAGE = 1, LOGOUT = 2;
    private int type;
    private String message;

    // constructor
    ChatMessage(int type, String message) {
        this.type = type;
        this.message = message;
    }

    ChatMessage() {

    }

    // getters
    int getType() {
        return type;
    }
    String getMessage() {
        return message;
    }

    void setMessage(String msg) {
            this.message = msg;
    }
}