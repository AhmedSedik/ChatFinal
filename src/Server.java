import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author zozzy on 31.12.19
 */
/*
 * The server that can be run both as a console application or a GUI
 */
public class Server {
    // a unique ID for each connection
    private static int uniqueId;
    // an ArrayList to keep the list of the Client
    private ArrayList<ClientThread> clients;
    // if I am in a GUI
    private ServerGUI serverGUI;
    // to display time
    private SimpleDateFormat simpleDateFormat;
    // the port number to listen for connection
    private int port;
    // the boolean that will be turned off to stop the server
    private boolean keepGoing;
    //the csv File
    public  static File users;


    /*
     *  server constructor that receive the port to listen to for connection as parameter
     *  in console
     */
    public Server(int port) {
        this(port, null);
    }

    public Server(int port, ServerGUI serverGUI) {
        // GUI or not
        this.serverGUI = serverGUI;
        // the port
        this.port = port;
        // to display hh:mm:ss
        simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        // ArrayList for the Client list
        clients = new ArrayList<ClientThread>();
    }

    public void start() {
        keepGoing = true;
        /* create socket server and wait for connection requests */
        try
        {
            // the socket used by the server
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections
            while(keepGoing)
            {
                // format message saying we are waiting
                display("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept();  	// accept connection
                // if I was asked to stop
                if(!keepGoing)
                    break;
                ClientThread thread = new ClientThread(socket);  // make a thread of it
                clients.add(thread);									// save it in the ArrayList
                thread.start();
            }
            // I was asked to stop
            try {
                serverSocket.close();
                for(int i = 0; i < clients.size(); ++i) {
                    ClientThread clientThread = clients.get(i);
                    try {
                        clientThread.sInput.close();
                        clientThread.sOutput.close();
                        clientThread.socket.close();
                    }
                    catch(IOException ioE) {
                        // not much I can do
                    }
                }
            }
            catch(Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        }
        // something went bad
        catch (IOException e) {
            String msg = simpleDateFormat.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }
    /*
     * For the GUI to stop the server
     */
    protected void stop() {
        keepGoing = false;
        // connect to myself as Client to exit statement
        // Socket socket = serverSocket.accept();
        try {
            new Socket("localhost", port);
        }
        catch(Exception e) {
            // nothing I can really do
        }
    }
    /*
     * Display an event (not a message) to the console or the GUI
     */
    private void display(String msg) {
        String time = simpleDateFormat.format(new Date()) + " " + msg;
        if(serverGUI == null)
            System.out.println(time);
        else
            serverGUI.appendEvent(time + "\n");
    }
    /*
     *  to broadcast a message to all Clients
     */
    private synchronized void broadcast(String message) {

        String time = simpleDateFormat.format(new Date());
        String fullMessage = time + " " + message + "\n";
        // display message on console or GUI
        if(serverGUI == null)
            System.out.print(fullMessage);
        else
            serverGUI.appendRoom(fullMessage);     // append in the room window

        // we loop in reverse order in case we would have to remove a Client
        // because it has disconnected
        for(int i = clients.size(); --i >= 0;) {
            ClientThread ct = clients.get(i);
            // try to write to the Client if it fails remove it from the list
            if(!ct.writeMsg(fullMessage)) {
                clients.remove(i);
                display("Disconnected Client " + ct.username + " removed from list.");
            }
        }
    }

    // for a client who logoff using the LOGOUT message
    synchronized void remove(int id) {
        // scan the array list until we found the Id
        for(int i = 0; i < clients.size(); ++i) {
            ClientThread clientThread = clients.get(i);
            // found it
            if(clientThread.id == id) {
                clients.remove(i);
                return;
            }
        }
    }

    /*
     *  To run as a console application just open a console window and:
     * > java Server
     * > java Server portNumber
     * If the port number is not specified 1500 is used
     */
    public static void main(String[] args) {

        // start server on port 1500 unless a PortNumber is specified
        int portNumber = 1500;

        switch(args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                }
                catch(Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;

        }
        // create a server object and start it
        Server server = new Server(portNumber);
        server.start();
    }

    /** One instance of this thread will run for each client */
    class ClientThread extends Thread {
        // the socket where to listen/talk
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        // my unique id (used in disconnecting)
        int id;
        // the Username of the Client
        String username;

        ChatMessage message;

        String date;

        // Constructor
        ClientThread(Socket socket) {


            users = new File("users.csv");
            // a unique id
            id = ++uniqueId;
            this.socket = socket;
            /* Creating both Data Stream */
            System.out.println("Thread trying to create Object Input/Output Streams");
            try
            {
                // create output first
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput  = new ObjectInputStream(socket.getInputStream());
                // read the username
                //TODO check user Choice  Done


                String userChoice = (String) sInput.readObject();

                if (userChoice.equalsIgnoreCase("/register")) {
                    registerUser();
                } else if (userChoice.equalsIgnoreCase("/Login")) {
                    userLogin();
                } else {
                    display("Failed to Login or Register");
                }


                display(username + " just connected.");
            }
            catch (IOException | ClassNotFoundException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }

            date = new Date().toString() + "\n";
        }

        // what will run forever
        public void run() {
            // to loop until LOGOUT
            boolean keepGoing = true;
            while(keepGoing) {
                // read a String (which is an object)
                try {
                    message = (ChatMessage) sInput.readObject();
                }
                catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                }
                catch(ClassNotFoundException e2) {
                    break;
                }
                // the messaage part of the ChatMessage
                String message = this.message.getMessage();

                // Switch on the type of message receive
                switch(this.message.getType()) {

                    case ChatMessage.MESSAGE:
                        broadcast(username + ": " + message);
                        break;
                    case ChatMessage.LOGOUT:
                        broadcast(username + " disconnected");
                        display(username + " disconnected");
                        keepGoing = false;
                        break;
                    case ChatMessage.WHOISIN:
                        writeMsg("List of the users connected at " + simpleDateFormat.format(new Date()) + "\n");
                        // scan al the users connected
                        for(int i = 0; i < clients.size(); ++i) {
                            ClientThread ct = clients.get(i);
                            writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
                        }
                        break;
                }
            }
            // remove myself from the arrayList containing the list of the
            // connected Clients
            remove(id);
            close();
        }
        private void registerUser() {
            try (
                    CSVWriter writer = new CSVWriter(new FileWriter(users.getAbsoluteFile(), true));

            ) {

                String readUsername;
                String readPassword;

                boolean userExists = false;
                while (!userExists) {

                    while(((readUsername = (String) sInput.readObject()) != null) &&
                            ((readPassword = (String) sInput.readObject()) != null)) {

                        String[] nextRecord;
                        userExists = false;
                        CSVReader reader = new CSVReader(new FileReader(users));
                        while ((((nextRecord = reader.readNext())) != null) && userExists == false) {
                            if (nextRecord[0].equals(readUsername)) {
                                System.out.println("a client entered an already taken username");
                                sOutput.writeObject("falseRegister");
                                userExists = true;
                            }
                        }
                        if (!userExists) {

                            String[] data = {readUsername, readPassword};
                            System.out.println(socket +"Registered New User");
                            sOutput.writeObject("trueRegister");
                            username = readUsername;
                            writer.writeNext(data);
                            userExists = true;
                            break;
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException | CsvValidationException e) {
                e.printStackTrace();
            }
        }

        //TODO if user already logged in
        public void userLogin() throws IOException {

            try
//                    CSVWriter writer = new CSVWriter(new FileWriter(users.getAbsoluteFile(), true));

             {

             //  String userChoice = (String) sInput.readObject();
                String readUsername;
                String readPassword;

                    boolean loginCheck = false;
                    while (((readUsername = (String) sInput.readObject()) != null) &&
                            ((readPassword = (String) sInput.readObject()) != null)) {
                        String[] nextRecord;
                        CSVReader reader = new CSVReader(new FileReader(users));

                        while ((((nextRecord = reader.readNext())) != null) && !loginCheck) {
                            if (nextRecord[0].equals(readUsername)) {
                                if (nextRecord[1].equals(readPassword))
                                    loginCheck = true;
                            }
                        }
                        if (loginCheck) {
                            sOutput.writeObject("trueLogin");
                            //sOutput.writeObject(readUsername + " Login Accepted!");
                            username = readUsername;
                            System.out.println("Client: " + socket + " logged in with username " + readUsername);
                            break;
                        } else
                            sOutput.writeObject("falseLogin");
                    }


            } catch (
                    CsvValidationException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        public void sendMessage(ChatMessage msg) {
            try {
                sOutput.writeObject(msg);
            }
            catch(IOException e) {
                display("Exception writing to server: " + e);
            }
        }

        // try to close everything
        private void close() {
            // try to close the connection
            try {
                if(sOutput != null) sOutput.close();
            }
            catch(Exception e) {}
            try {
                if(sInput != null) sInput.close();
            }
            catch(Exception e) {};
            try {
                if(socket != null) socket.close();
            }
            catch (Exception e) {}
        }

        /*
         * Write a String to the Client output stream
         */
        private boolean writeMsg(String msg) {
            // if Client is still connected send the message to it
            if(!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                sOutput.writeObject(msg);
            }
            // if an error occurs, do not abort just inform the user
            catch(IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }


    }

    void loggedClients() {
        for(int i = 0; i < clients.size(); ++i) {
            ClientThread ct = clients.get(i);
            serverGUI.appendEvent((i+1) + ") " + ct.username + " since " + ct.date);
        }
    }
}
