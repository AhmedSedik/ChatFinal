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
    public int uniqueId;
    // an ArrayList to keep the list of the Client
    private ArrayList<ClientThread> clients;
    //ArrayList for the logged In uUsers
    private ArrayList<String> onlineUsers;
    // if I am in a GUI
    private ServerGUI serverGUI;
    // to display time
    private SimpleDateFormat simpleDateFormat;
    // the port number to listen forx` connection
    private int port;
    // the boolean that will be turned off to stop the server
    public boolean keepGoing;
    //the csv File
    public static File users;

    public boolean connected;


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
        //could be also ArrayList
        onlineUsers = new ArrayList<>();
    }

    public void start() {
        keepGoing = true;
        /* create socket server and wait for connection requests */
        try {
            // the socket used by the server
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections
            while (keepGoing) {
                // format message saying we are waiting
                display("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept();    // accept connection
                // if I was asked to stop
                if (!keepGoing)
                    break;
                ClientThread thread = new ClientThread(socket);  // make a thread of it
                clients.add(thread);                            // save it in the ArrayList

                thread.start();
            }
            // I was asked to stop
            try {
                serverSocket.close();
                for (int i = 0; i < clients.size(); ++i) {
                    ClientThread clientThread = clients.get(i);
                    try {
                        clientThread.sInput.close();
                        clientThread.sOutput.close();
                        clientThread.socket.close();
                    } catch (IOException ioE) {
                        // not much I can do
                    }
                }
            } catch (Exception e) {
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
        } catch (Exception e) {
            // nothing I can really do
        }
    }

    /*
     * Display an event (not a message) to the console or the GUI
     */
    private void display(String msg) {
        String time = simpleDateFormat.format(new Date()) + " " + msg;
        if (serverGUI == null)
            System.out.println(time);
        else
            serverGUI.appendEvent(time + "\n");
    }

    /*
     *  to broadcast a message to all Clients
     */
    public synchronized void broadcast(String message) {

        String time = simpleDateFormat.format(new Date());
        String fullMessage = time + " " + message + "\n";
        // display message on console or GUI
        if (serverGUI == null)
            System.out.print(fullMessage);
        else
            serverGUI.appendRoom(fullMessage);     // append in the room window

        // we loop in reverse order in case we would have to remove a Client
        // because it has disconnected
        for (int i = clients.size(); --i >= 0; ) {
            ClientThread clientThread = clients.get(i);
            // try to write to the Client if it fails remove it from the list
            if (!clientThread.writeMsg(fullMessage)) {
                clients.remove(i);
                display("Disconnected Client " + clientThread.username + " removed from list.");
            }
        }
    }

    // for a client who logoff using the LOGOUT message
    synchronized void remove(int id) {
        // scan the array list until we found the Id
        for (int i = 0; i < clients.size(); ++i) {
            ClientThread clientThread = clients.get(i);
            // found it
            if (clientThread.id == id) {
                clients.remove(i);
                return;
            }
        }
    }

    synchronized void kickClient() {

        for (int i = clients.size(); --i >= 0; ) {
            ClientThread clientThread = clients.get(i);

            if (clientThread.id == serverGUI.userIndex) {
                //clients.remove(i);//ArrayList size not correct
                //TODO fix user ID incrementation Done
                clientThread.id = --uniqueId;
                //TODO fix user Index (index is at some point wrong)

                if (serverGUI.userIndex >= 1) {
                    serverGUI.userIndex = -serverGUI.userIndex;
                }
                clientThread.writeMsg("kicked");
                display("Disconnected Client " + clientThread.username + " removed from list.");
                onlineUsers.remove(clientThread.username);
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

        switch (args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
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

    /**
     * One instance of this thread will run for each client
     */
    class ClientThread extends Thread {
        // the socket where to listen/talk
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        // my unique id (used in disconnecting)
        int id;
        // the Username of the Client
        String username = "";
        //instance of the helper Class
        ChatMessage message;
        //login status
        boolean loggedIn;


        String date;


        // Constructor
        ClientThread(Socket socket) {


            users = new File("users.csv");
            // a unique id
            id = ++uniqueId;
            this.socket = socket;
            /* Creating both Data Stream */
            System.out.println("Thread trying to create Object Input/Output Streams");
            try {
                // create output first
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
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

                broadcast(username + " just connected.");
                display(username + " just connected.");
                connected = true;

            } catch (IOException | ClassNotFoundException e) {
                display("Exception creating new Input/output Streams: " + e);
                connected = false;
                return;
            }

            date = new Date().toString() + "\n";
        }

        // what will run forever
        public void run() {
            // to loop until LOGOUT
            boolean keepGoing = true;
            while (keepGoing) {
                // read a String (which is an object)
                try {
                    message = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    // in case client quit while server running reading stream
                    display(username + " Exception reading Streams: " + e);
                    //user disconnected log him off


                    onlineUsers.remove(username);
                    id--;//TODO Fix
                    break;
                } catch (ClassNotFoundException e2) {
                    break;
                }
                // the message part of the ChatMessage
                String message = this.message.getMessage();
                ChatMessage chatMessage = new ChatMessage();
                // Switch on the type of message receive
                switch (this.message.getType()) {

                    case ChatMessage.MESSAGE:
                        broadcast(username + ": " + message);
                        break;
                    case ChatMessage.LOGOUT:
                        broadcast(username + " disconnected");
                        display(username + " disconnected");
                        onlineUsers.remove(username);
                        keepGoing = false;
                        break;
                    case ChatMessage.OnlineUsers:
                        // scan al the users connected
                        if (clients != null && clients.size() >= 1) {
                            for (int i = 0; i < clients.size(); i++) {
                                ClientThread ct = clients.get(i);
                                writeMsg("online" + ct.username);
                            }
                        }
                        break;
                    case ChatMessage.PLAY_REQUEST:
                        String selectedUsername = this.message.getMessage();
                        String sender = this.message.getSender();
                        writeMsgToUser("playRequest" + "-" + sender, selectedUsername);
                        break;

                    case ChatMessage.REPSONE_PLAY_REQUEST:
                        String msg = this.message.getMessage();
                        String[] msgSplit = msg.split("-");
                        String response = msgSplit[0];
                        String userTO = msgSplit[1];
                        String userFROM = this.message.getSender();
                        writeMsgToUser(response + "-" +userFROM, userTO);
                        break;

                    case ChatMessage.PLAY:
                        String userTO3 = this.message.getMessage();
                        String userFROM3 = this.username;
                        writeMsgToUser("connect4",userTO3);
                        writeMsgToUser("connect4",userFROM3);
                        startGame(userTO3,userFROM3);
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

                    while (((readUsername = (String) sInput.readObject()) != null) &&
                            ((readPassword = (String) sInput.readObject()) != null)) {

                        String[] nextRecord;
                        userExists = false;
                        CSVReader reader = new CSVReader(new FileReader(users));
                        while ((((nextRecord = reader.readNext())) != null) && !userExists) {
                            if (nextRecord[0].equals(readUsername)) {
                                System.out.println("a client entered an already taken username");
                                sOutput.writeObject("falseRegister");
                                userExists = true;
                            }
                        }
                        if (!userExists) {

                            String[] data = {readUsername, readPassword};
                            System.out.println(socket + "Registered New User");
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

        //TODO if user already logged in DONE
        public void userLogin() throws IOException {

            try {

                String readUsername;
                String readPassword;

                boolean loginCheck = false;
                while (((readUsername = (String) sInput.readObject()) != null) &&
                        ((readPassword = (String) sInput.readObject()) != null)) {
                    String[] nextRecord;
                    CSVReader reader = new CSVReader(new FileReader(users));

                    while ((((nextRecord = reader.readNext())) != null) && !loginCheck) {
                        if (nextRecord[0].equals(readUsername)) {
                            if (nextRecord[1].equals(readPassword)) {
                                for (String onlineUser : onlineUsers) {
                                    username = onlineUser;
                                    // found it
                                    if (username.equals(readUsername)) {
                                        System.out.println("doubled");
                                        sOutput.writeObject("userlogged");
                                    }
                                }
                                loginCheck = true;

                            }
                        }
                    }
                    if (loginCheck) {


                        sOutput.writeObject("trueLogin");

                        //sOutput.writeObject(readUsername + " Login Accepted!");
                        username = readUsername;
                        //adding user to ArrayList
                        onlineUsers.add(username);
                        serverGUI.userIndex++;
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


        // try to close everything
        private void close() {
            // try to close the connection
            try {
                if (sOutput != null) sOutput.close();
            } catch (Exception e) {
                //nothing to catch
            }
            try {
                if (sInput != null) sInput.close();
            } catch (Exception e) {
            }

            try {
                if (socket != null) socket.close();
            } catch (Exception e) {
            }
        }

        /*
         * Write a String to the Client output stream
         */
        private boolean writeMsg(String msg) {
            // if Client is still connected send the message to it
            if (!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                sOutput.writeObject(msg);
            }
            // if an error occurs, do not abort just inform the user
            catch (IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }

        private boolean writeMsgToUser(String msg, String username) {
            // if Client is still connected send the message to it
            if (!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                ClientThread clientThread = findByUsername(username);
                ObjectOutputStream out = clientThread.sOutput;
                out.writeObject(msg);
                out.flush();
            }
            // if an error occurs, do not abort just inform the user
            catch (IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }

    }


    //TODO better have interface also for Remove and Kick Clients
    void loggedClients() {
        if (clients.isEmpty()) {
            serverGUI.appendEvent("No Current Online Users\n");
            return;
        }

        for (int i = 0; i < clients.size(); ++i) {
            ClientThread ct = clients.get(i);
            serverGUI.appendEvent((i + 1) + ") " + ct.username + " since " + ct.date);
        }


    }

    ClientThread findByUsername(String username){
        for(ClientThread clientThread :clients){
            if (clientThread.username.equals(username))
                return clientThread;
        }
        return null;
    }

    void onlineUsers() {
        if (clients != null && clients.size() >= 1) {
            for (int i = 0; i < clients.size(); i++) {
                ClientThread clientThread = clients.get(i);
                serverGUI.appendClients((i + 1) + ")" + clientThread.username);

            }
        }
    }

    void startGame(String user1, String user2){
        Socket player1 = findByUsername(user1).socket;
        Socket player2 = findByUsername(user2).socket;

        HandleASession task = new HandleASession(player1, player2);

        // Start the new thread
        new Thread(task).start();

    }

    class HandleASession implements Runnable, connectfourconstraints {
        private Socket player1;
        private Socket player2;

        // Create and initialize cells
        private char[][] cell =  new char[6][7];

        private DataInputStream fromPlayer1;
        private DataOutputStream toPlayer1;
        private DataInputStream fromPlayer2;
        private DataOutputStream toPlayer2;

        // Continue to play
        private boolean continueToPlay = true;

        /** Construct a thread */
        public HandleASession(Socket player1, Socket player2) {
            this.player1 = player1;
            this.player2 = player2;

            // Initialize cells
            for (int i = 0; i < 6; i++)
                for (int j = 0; j < 7; j++)
                    cell[i][j] = ' ';
        }

        /** Implement the run() method for the thread */
        public void run() {
            try {
                // Create data input and output streams
                DataInputStream fromPlayer1 = new DataInputStream(
                        player1.getInputStream());
                DataOutputStream toPlayer1 = new DataOutputStream(
                        player1.getOutputStream());
                DataInputStream fromPlayer2 = new DataInputStream(
                        player2.getInputStream());
                DataOutputStream toPlayer2 = new DataOutputStream(
                        player2.getOutputStream());

                // Write anything to notify player 1 to start
                // This is just to let player 1 know to start
                toPlayer1.writeInt(1);

                // Continuously serve the players and determine and report
                // the game status to the players
                while (true) {
                    // Receive a move from player 1
                    int row = fromPlayer1.readInt();
                    int column = fromPlayer1.readInt();
                    char token = 'r';

                    cell[row][column] = 'r';

                    // Check if Player 1 wins
                    if (isWon(row, column, token)) {
                        toPlayer1.writeInt(PLAYER1_WON);
                        toPlayer2.writeInt(PLAYER1_WON);
                        sendMove(toPlayer2, row, column);
                        break; // Break the loop
                    }
                    else if (isFull()) { // Check if all cells are filled
                        toPlayer1.writeInt(DRAW);
                        toPlayer2.writeInt(DRAW);
                        sendMove(toPlayer2, row, column);
                        break;
                    }
                    else {
                        // Notify player 2 to take the turn
                        toPlayer2.writeInt(CONTINUE);

                        // Send player 1's selected row and column to player 2
                        sendMove(toPlayer2, row, column);
                    }

                    // Receive a move from Player 2
                    row = fromPlayer2.readInt();
                    column = fromPlayer2.readInt();
                    cell[row][column] = 'b';

                    // Check if Player 2 wins
                    if (isWon(row, column, token)) {
                        toPlayer1.writeInt(PLAYER2_WON);
                        toPlayer2.writeInt(PLAYER2_WON);
                        sendMove(toPlayer1, row, column);
                        break;
                    }
                    else {
                        // Notify player 1 to take the turn
                        toPlayer1.writeInt(CONTINUE);

                        // Send player 2's selected row and column to player 1
                        sendMove(toPlayer1, row, column);
                    }
                }
            }
            catch(IOException ex) {
                System.err.println(ex);
            }
        }

        /** Send the move to other player */
        private void sendMove(DataOutputStream out, int row, int column)
                throws IOException {
            out.writeInt(row); // Send row index
            out.writeInt(column); // Send column index
        }

        /** Determine if the cells are all occupied */
        private boolean isFull() {
            for (int i = 0; i < 6; i++)
                for (int j = 0; j < 7; j++)
                    if (cell[i][j] == ' ')
                        return false; // At least one cell is not filled

            // All cells are filled
            return true;
        }

        /*Determine if the player with the specified token wins */
        private boolean isWon(int row, int column, char token) {

            // TEST BOARD VALUES
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 6; y++) {
                    System.out.print(cell[x][y]);
                }
                System.out.println();
            }

            //Horizontal

            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 3; y++) {
                    if (cell[x][y] == token && cell[x][y+1] == token && cell[x][y+2] == token && cell[x][y+3] == token) {
                        return true;
                    }
                }
            }
            //Vertical

            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 7; y++) {
                    if (cell[x][y] == token && cell[x+1][y] == token && cell[x+2][y] == token && cell[x+3][y] == token) {
                        return true;
                    }
                }
            }


            //Diagonal wins
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 6; y++) {
                    if (cell[x][y] == token && cell[x+1][y+1] == token && cell[x+2][y+2] == token && cell[x+3][y+3] == token) {
                        return true;
                    }
                }
            }

            //Other diagonal wins
            for (int x = 0; x < 3; x++) {
                for (int y = 3; y < 7; y++) {
                    if (cell[x][y] == token && cell[x+1][y-1] == token && cell[x+2][y-2] == token && cell[x+3][y-3] == token) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
