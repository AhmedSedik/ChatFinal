import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * @author zozzy on 31.12.19
 */
/*
 * The Client that can be run both as a console or a GUI
 */
public class Client  {

    //TODO Register user

    // for I/O
    private ObjectInputStream sInput;		// to read from the socket
    private ObjectOutputStream sOutput;		// to write on the socket
    private Socket socket;

    // if I use a GUI or not
    private ClientGUI clientGUI;

    // the server, the port and the username
    private String server, username,password;
    private int port;

    /*
     *  Constructor called by console mode
     *  server: the server address
     *  port: the port number
     *  username: the username
     */
    Client(String server, int port, String username,String password) {
        // which calls the common constructor with the GUI set to null
        this(server, port, username,password, null);
    }

    /*
     * Constructor call when used from a GUI
     * in console mode the ClientGUI parameter is null
     */
    Client(String server, int port, String username,String password, ClientGUI clientGUI) {
        this.server = server;
        this.port = port;
        this.username = username;
        this.password = password;
        // save if we are in GUI mode or not
        this.clientGUI = clientGUI;
    }

    /*
     * To start the dialog
     */
    public boolean start() {
        // try to connect to the server
        try {

            socket = new Socket(server, port);
        }
        // if it failed not much I can so
        catch(Exception ec) {
            display("Error connecting to server:" + ec);
            return false;
        }

        String msg = "Attempting Conneting to:  " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);




        /* Creating both Data Stream */
        try
        {
            sInput  = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }
        sendChoice(clientGUI.userChoices);
        // creates the Thread to listen from the server
        new ListenFromServer().start();
        // Send our username to the server this is the only message that we
        // will send as a String. All other messages will be ChatMessage objects

        try
        {
            sOutput.writeObject(username);
            sOutput.writeObject(password);
        }
        catch (IOException eIO) {
            display("Exception doing login : " + eIO);
            disconnect();
            return false;
        }

        // success we inform the caller that it worked
        return true;
    }

    /*
     * To send a message to the console or the GUI
     */
    private void display(String msg) {
        if(clientGUI == null)
            System.out.println(msg);      // println in console mode
        else
            clientGUI.append(msg + "\n");		// append to the ClientGUI JTextArea (or whatever)
    }

    /*
     * To send a message to the server
     */
    void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        }
        catch(IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    void sendChoice(String msg) {
        try {
            sOutput.writeObject(msg);
        }
        catch(IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    /*
     * When something goes wrong
     * Close the Input/Output streams and disconnect not much to do in the catch clause
     */
    private void disconnect() {
        try {
            if(sInput != null) sInput.close();
        }
        catch(Exception e) {} // not much else I can do
        try {
            if(sOutput != null) sOutput.close();
        }
        catch(Exception e) {} // not much else I can do
        try{
            if(socket != null) socket.close();
        }
        catch(Exception e) {} // not much else I can do

        // inform the GUI
        if(clientGUI != null)
            clientGUI.connectionFailed();

    }
    /*
     * To start the Client in console mode use one of the following command
     * > java Client
     * > java Client username
     * > java Client username portNumber
     * > java Client username portNumber serverAddress
     * at the console prompt
     * If the portNumber is not specified 1500 is used
     * If the serverAddress is not specified "localHost" is used
     * If the username is not specified "Anonymous" is used
     * > java Client
     * is equivalent to
     * > java Client Anonymous 1500 localhost
     * are equivalent
     *
     * In console mode, if an error occurs the program simply stops
     * when a GUI id used, the GUI is informed of the disconnection
     */
    public static void main(String[] args) {
        // default values
        int portNumber = 1500;
        String serverAddress = "localhost";
        String userName = "Anonymous";
        String password = "";

        // depending of the number of arguments provided we fall through
        switch(args.length) {
            // > javac Client username portNumber serverAddr
            case 3:
                serverAddress = args[2];
                // > javac Client username portNumber
            case 2:
                try {
                    portNumber = Integer.parseInt(args[1]);
                }
                catch(Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                    return;
                }
                // > javac Client username
            case 1:
                userName = args[0];
                // > java Client
            case 0:
                break;
            // invalid number of arguments
            default:
                System.out.println("Usage is: > java Client [username] [portNumber] {serverAddress]");
                return;
        }
        // create the Client object
        Client client = new Client(serverAddress, portNumber, userName,password);
        // test if we can start the connection to the Server
        // if it failed nothing we can do
        if(!client.start())
            return;

        // wait for messages from user
        Scanner scan = new Scanner(System.in);
        // loop forever for message from the user
        while(true) {
            System.out.print("> ");
            // read message from user
            String msg = scan.nextLine();
            // logout if message is LOGOUT
            if(msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                // break to do the disconnect
                break;
            }
            // message WhoIsIn
            else if(msg.equalsIgnoreCase("WHOISIN")) {
                client.sendMessage(new ChatMessage(ChatMessage.OnlineUsers, ""));
            }
            else {				// default to ordinary message
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
            }
        }
        // done disconnect
        client.disconnect();
    }

    /*
     * a class that waits for the message from the server and append them to the JTextArea
     */
    class ListenFromServer extends Thread {

        public void run() {
            while(true) {
                try {
                    String msg = (String) sInput.readObject();

                    // if console mode print the message and add back the prompt
                    if(clientGUI == null) {
                        System.out.println(msg);
                        System.out.print("> ");
                    } else if (msg.equalsIgnoreCase("trueLogin")) {
                        clientGUI.loginAccepted();
                        display("Login Accepted!");
                    } else if (msg.equalsIgnoreCase("falseLogin")) {
                        clientGUI.loginFailed();
                        display("Login Failed!");
                        socket.close();

                    } else if (msg.equalsIgnoreCase("trueRegister")) {
                        clientGUI.registerSucceed();
                        display("Registration Successful!");
                    } else if (msg.equalsIgnoreCase("falseRegister")) {
                        clientGUI.registerFailed();
                        display("Registration Failed!");
                        socket.close();
                    } else if (msg.equalsIgnoreCase("kicked")) {
                        clientGUI.kicked();
                        socket.close();
                    } else {
                        clientGUI.append(msg);
                    }
                }
                catch(IOException e) {
                    //display("false Login or Register");
                    display("Connection Interrupted \n Cause: server could have closed the Connection\n or couldn't connect to the server \n" + e);
                    if(clientGUI != null)
                        clientGUI.connectionFailed();
                    break;
                }
                // can't happen with a String object but need the catch anyhow
                catch(ClassNotFoundException e2) {
                }
            }
        }
    }
}
