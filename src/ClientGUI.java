/**
 * @author zozzy on 31.12.19
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/* * The Client with its GUI
 */
public class ClientGUI extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private boolean loginfailed = false;
    // will first hold "Username:", later on "Enter message"
    private JLabel label;


    private JTextField usernameField;
    private JPasswordField passwordField;

    // to hold the Username and later on the messages
    private JTextField chatTextField;
    // to hold the server address an the port number
    private JTextField tfServer, tfPort;
    // to Logout and get the list of the users
    private JButton login, logout,register, whoIsIn,send;
    // for the chat room
    private JTextArea ta;
    // if it is for connection
    private boolean connected;
    // the Client object
    private Client client;
    // the default port number
    private int defaultPort;
    private String defaultHost;

    // Constructor connection receiving a socket number
    ClientGUI(String host, int port) {

        super("Chat Client");
        defaultPort = port;
        defaultHost = host;




        // The NorthPanel with:
        JPanel northPanel = new JPanel(new GridLayout(4, 1));
        // the server name anmd the port number
        JPanel serverAndPort = new JPanel(new GridLayout(1, 5, 1, 3));
        // the two JTextField with default value for server address and port number
        tfServer = new JTextField(host);
        tfPort = new JTextField("" + port);
        tfPort.setHorizontalAlignment(SwingConstants.RIGHT);



        serverAndPort.add(new JLabel("Server Address:  "));
        serverAndPort.add(tfServer);
        serverAndPort.add(new JLabel("Port Number:  "));
        serverAndPort.add(tfPort);
        serverAndPort.add(new JLabel(""));


        // adds the Server and port field to the GUI
        northPanel.add(serverAndPort);
        JPanel userandpass = new JPanel(new GridLayout(1, 4, 1, 3));

        usernameField = new JTextField("");
        passwordField = new JPasswordField("");
        //passwordField.setHorizontalAlignment(SwingConstants.RIGHT);

        userandpass.add(new JLabel("Username: "));
        userandpass.add(usernameField);
        userandpass.add(new JLabel("Password: "));
        userandpass.add(passwordField);

        northPanel.add(userandpass);

        add(northPanel, BorderLayout.NORTH);

        // The CenterPanel which is the chat room
        ta = new JTextArea("Welcome to the Chat room\n", 80, 80);
        this.ta.setSize(400,400);
        JPanel centerPanel = new JPanel(new GridLayout(3, 1,1,3));
        centerPanel.add(new JScrollPane(ta));
        ta.setEditable(false);


        // the Label and the TextField
        label = new JLabel("Enter message below", SwingConstants.CENTER);
        centerPanel.add(label);

        chatTextField = new JTextField("");
        chatTextField.setBackground(Color.WHITE);
        centerPanel.add(chatTextField);
        add(centerPanel, BorderLayout.CENTER);

        // the 3 buttons
        login = new JButton("Login");
        login.addActionListener(this);
        register = new JButton("Register");
        logout = new JButton("Logout");
        logout.addActionListener(this);
        logout.setEnabled(false);        // you have to login before being able to logout
        whoIsIn = new JButton("Who is in");
        whoIsIn.addActionListener(this);
        whoIsIn.setEnabled(false);        // you have to login before being able to Who is in

        JPanel southPanel = new JPanel();
        southPanel.add(login);
        southPanel.add(register);
        southPanel.add(logout);
        southPanel.add(whoIsIn);
        add(southPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);
        chatTextField.requestFocus();

    }

    // called by the Client to append text in the TextArea
    void append(String str) {
        ta.append(str);
        ta.setCaretPosition(ta.getText().length() - 1);
    }

    void loginAccepted() {
        JOptionPane.showMessageDialog(this, "Login Accepted");
    }
    void loginFailed() {

        JOptionPane.showMessageDialog(this, "Login Failed\n Please try again!");
        loginfailed = true;
        login.setEnabled(true);
    }

    // called by the GUI is the connection failed
    // we reset our buttons, label, textfield
    void connectionFailed() {
        login.setEnabled(true);
        register.setEnabled(true);
        logout.setEnabled(false);
        whoIsIn.setEnabled(false);
        //label.setText("Enter your username below");
        //screenName.setText("");
        // reset port number and host name as a construction time
        tfPort.setText("" + defaultPort);
        tfServer.setText(defaultHost);
        // let the user change them
        tfServer.setEditable(false);
        tfPort.setEditable(false);
        // don't react to a <CR> after the username
        chatTextField.removeActionListener(this);
        connected = false;
    }

    /*
     * Button or JTextField clicked
     */
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        // if it is the Logout button
        if (o == logout) {
            client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
            return;
        }
        // if it the who is in button
        if (o == whoIsIn) {
            client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
            return;
        }

        // ok it is coming from the JTextField
        if (connected) {
            // just have to send the message
            client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, chatTextField.getText()));
            chatTextField.setText("");
            return;
        }


        if (o == login) {
            // ok it is a connection request
            String username = usernameField.getText().trim();
            String password = String.valueOf(passwordField.getPassword());
            // empty username ignore it
            if (username.length() == 0)
                return;
            if (password.length() ==0)
                return;
            // empty serverAddress ignore it
            String server = tfServer.getText().trim();
            if (server.length() == 0)
                return;
            // empty or invalid port numer, ignore it
            String portNumber = tfPort.getText().trim();
            if (portNumber.length() == 0)
                return;
            int port = 0;
            try {
                port = Integer.parseInt(portNumber);
            } catch (Exception en) {
                return;   // nothing I can do if port number is not valid
            }

            // try creating a new Client with GUI
            client = new Client(server, port, username,password, this);
            // test if we can start the Client
            if (!client.start())
                return;
            chatTextField.setText("");
            usernameField.setText("");
            passwordField.setText("");
            //label.setText("Enter your message below");
            connected = true;

            // disable login button
            login.setEnabled(false);
            // enable the 2 buttons
            logout.setEnabled(true);
            whoIsIn.setEnabled(true);
            // disable the Server and Port JTextField
            tfServer.setEditable(false);
            tfPort.setEditable(false);
            // Action listener for when the user enter a message
            chatTextField.addActionListener(this);
        }

    }

    // to start the whole thing the server
    public static void main(String[] args) {
        new ClientGUI("localhost", 1500);
    }

}