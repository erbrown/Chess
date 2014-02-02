/* ChessClient.java 06/05/2013
 * Eric Brown & Max Koren
 * 
 * This class contains a client that interfaces with the corresponding ChessServer.
 * Both client and server also use the Game class, which represents a game of chess.
 * The ChessClient class is a subclass of JFrame.
 * When run, main creates an instance of ChessClient, then waits for a reply from the server.
 * The user can interact with the game and with the server through methods provided in the ChessClient.
 * 
 * The user must provide an IP address to connect to, a username, and a password.
 * The "Connect" button attempts to connect to the specified address.
 * Once connected, the "Log in" button attempts to log in using the specified username and password.
 * Instead of logging in, the "Register" button may be used to register as a new user on the server.
 * If in a game, the "Resign" button may be used to surrender a game.
 * The "Refresh user list" button asks the server to resend the list of users currently online.
 * The "Request Game" button sends a request to the selected user for a game.
 * 
 * The blank TextArea is the message log.  Messages from the server or game are displayed here.
 * Above the message log is a label that tells you if it's your turn or if you are in check.
 * Next to the message log is a list of users currently online.  This can be used to request a game.
 * 
 * The chess board is an 8x8 array of subclassed JToggleButtons.  It only functions while in a game.
 * 
 */
package chessclient;

import java.io.*;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.JPasswordField;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JTextField;
import java.net.*;
import java.awt.*;
import java.awt.event.*;

public class ChessClient extends JFrame {

    private static final String[] PIECE_CODES = {"\u2659", "\u2658", "\u2657",
        "\u2656", "\u2655", "\u2654", null, null, "\u265F", "\u265E", "\u265D",
        "\u265C", "\u265B", "\u265A"}; // unicode characters for the chess board
    private static final Color LIGHT = new Color(255, 206, 158); // coloration for the white squares
    private static final Color DARK = new Color(209, 139, 71); // coloration for the black squares
    private static final int PORT_NUM = 1729; // the smallest sum of two cubes in two different ways
    private static final String CRLF = "\r\n";
    private static final Font ccFont = new Font(Font.DIALOG, Font.PLAIN, 12); // font for the interface
    public DataOutputStream toServer; // used to talk to the server
    public BufferedReader fromServer; // used to listen to the server
    public Socket socket; // our connection to the server
    // These are our components for the interface
    private JButton btLogin = new JButton("Log in");
    private JButton btConnect = new JButton("Connect");
    private JButton btRegister = new JButton("      Register      ");
    private JButton btRequest = new JButton("Request Game");
    private JButton btRefresh = new JButton("Refresh user list");
    private JButton btResign = new JButton("Resign  ");
    private JLabel serverLabel = new JLabel("Chess server: ");
    private JTextField serverField = new JTextField("", 40);
    private JLabel userLabel = new JLabel("Username:      ");
    private JTextField userField = new JTextField("", 40);
    private JLabel passLabel = new JLabel("Password:       ");
    private JPasswordField passField = new JPasswordField();
    private JLabel listLabel = new JLabel("Users online:");
    private List userList = new List(15);
    private JLabel turnLabel = new JLabel(" ");
    private TextArea chatRoom = new TextArea(15, 40);
    private ChessButton[][] board = new ChessButton[8][8];
    ChessButton selected; // stores the last ChessButton clicked on
    String username = ""; // stores what username we are logged in as
    boolean requestPlaced = false; // stores whether we have requested a game
    boolean color = false; // our color: false is white, true is black
    boolean inGame = false; // stores whether we are in a game
    Game Chess; // our local copy of the chess game

    public ChessClient() {
        super("Chess Client"); // name the window
        socket = new Socket(); // initialize the socket
        Chess = null; // initialize our game

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // make sure it closes properly

        // set the font for the interface components

        btLogin.setFont(ccFont);
        btConnect.setFont(ccFont);
        btRegister.setFont(ccFont);
        btRequest.setFont(ccFont);
        btRefresh.setFont(ccFont);
        btResign.setFont(ccFont);
        serverLabel.setFont(ccFont);
        serverField.setFont(ccFont);
        userLabel.setFont(ccFont);
        userField.setFont(ccFont);
        passLabel.setFont(ccFont);
        passField.setFont(ccFont);
        listLabel.setFont(ccFont);
        userList.setFont(ccFont);
        turnLabel.setFont(ccFont);
        chatRoom.setFont(ccFont);

        // now we lay out the interface

        Panel serverPanel = new Panel(new BorderLayout());
        Panel userPanel = new Panel(new BorderLayout());
        Panel passPanel = new Panel(new BorderLayout());
        Panel connectPanel = new Panel(new BorderLayout());
        Panel buttonPanel = new Panel(new BorderLayout());
        Panel turnPanel = new Panel(new BorderLayout());
        Panel boardPanel = new Panel(new GridLayout(8, 8));
        Panel playerPanel = new Panel(new BorderLayout());

        serverPanel.add(serverLabel, BorderLayout.WEST);
        serverPanel.add(serverField, BorderLayout.CENTER);
        userPanel.add(userLabel, BorderLayout.WEST);
        userPanel.add(userField, BorderLayout.CENTER);
        passPanel.add(passLabel, BorderLayout.WEST);
        passPanel.add(passField, BorderLayout.CENTER);
        btConnect.addActionListener(new ConnectListener());
        btLogin.addActionListener(new LoginListener());
        btRegister.addActionListener(new RegisterListener());
        btRequest.addActionListener(new RequestListener());
        btRefresh.addActionListener(new RefreshListener());
        btResign.addActionListener(new ResignListener());
        connectPanel.add(btLogin, BorderLayout.CENTER);

        connectPanel.add(btRegister, BorderLayout.EAST);
        connectPanel.add(btConnect, BorderLayout.WEST);
        buttonPanel.add(btRefresh, BorderLayout.CENTER);
        buttonPanel.add(btRequest, BorderLayout.EAST);
        buttonPanel.add(btResign, BorderLayout.WEST);
        turnPanel.add(turnLabel, BorderLayout.CENTER);
        turnLabel.setHorizontalAlignment(JLabel.CENTER);
        playerPanel.add(chatRoom, BorderLayout.CENTER);
        userList.setMultipleMode(false);
        Panel listPanel = new Panel(new BorderLayout());
        listPanel.add(listLabel, BorderLayout.NORTH);
        listPanel.add(userList, BorderLayout.SOUTH);
        playerPanel.add(listPanel, BorderLayout.EAST);
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 16); // the chess pieces get their own font
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                ChessButton button = new ChessButton();
                if ((i + j) % 2 == 0) { // sets the background color for each space
                    button.setBackground(LIGHT);
                } else {
                    button.setBackground(DARK);
                }
                button.addActionListener(new MoveListener());
                button.setFont(font);
                button.setText(" ");
                boardPanel.add(button); // add to the panel
                board[i][j] = button; // add to the array
            }
        }

        Panel fieldPanel = new Panel(new GridLayout(0, 1));
        fieldPanel.add(serverPanel);
        fieldPanel.add(userPanel);
        fieldPanel.add(passPanel);
        fieldPanel.add(connectPanel);
        fieldPanel.add(buttonPanel);
        fieldPanel.add(turnPanel);

        Panel finalPanel = new Panel(new BorderLayout());
        finalPanel.add(fieldPanel, BorderLayout.NORTH);
        finalPanel.add(playerPanel, BorderLayout.SOUTH);

        add(finalPanel, BorderLayout.WEST);
        add(boardPanel, BorderLayout.EAST);

        pack();
        setVisible(true); // display the client
    }
    
    /** Used to send strings to the server. */
    private boolean send(String message) {
        try {
            toServer.writeBytes(message);
            return true; // return true when we send successfully
        } catch (Exception e) {
            out("Could not connect to the server.");
            return false; // if it returns false, we know our message didn't get through
        }
    }
    
    /** Displays the current turn by modifying a dedicated JLabel. */
    public void updateTurn() {
        if (!inGame) { // if we're not in a game, don't display anything
            turnLabel.setText(" ");
        } else {
            if (Chess.getTurn() == color) { // if it's our turn, say so
                turnLabel.setText("It's your turn" + (Chess.inCheck(color) ? ", and you're in check." : "!")); // if we're in check, say that as well
            } else {
                turnLabel.setText("It's your opponent's turn.");
                // if it's not our turn, we don't need to worry about check
            }
        }
    }
    
    /** Prints to the message log. */
    private void out(String message) {
        chatRoom.append(message + CRLF);
    }
    
    /** Updates the list of users online. */
    public void displayChatRoom(String[] users) {
        userList.removeAll(); // clear the list first
        for (int i = 0; i < users.length; i++) {
            if (!users[i].equals(username)) {
                userList.add(users[i]);
            } // this check makes sure we don't display our own name, so we can't request ourselves
        }
    }
    
    /** Update the board display to reflect the current game state. */
    public void displayGame() {
        if (!inGame) { // if we're not in a game, leave the board as we found it
            return; // even if we were in a game, don't clear it so you can see how you won/lost
        }

        for (int i = 0; i < 8; i++) { // iterate through the 8x8 array
            for (int j = 0; j < 8; j++) {
                int space = color ? Chess.getPiece(7 - i, 7 - j) : Chess.getPiece(i, j); // get the piece
                if (space == -1) { // if it's an empty space, set it to be clear
                    board[i][j].setText(" ");
                } else { // otherwise
                    board[i][j].setText(PIECE_CODES[space]); // set it to the appropriate unicode value
                }
            }
        }
        updateTurn(); // everytime this is called, the turn has changed, so update the turn label
    }
    
    /** Sends a game request, called from the "Request Game" button */
    private void requestGame(String user) {
        if (!requestPlaced) { // if we haven't placed a request
            if (!inGame) { // if we aren't in a game
                if (send("request " + user + CRLF)) { // send the request to the server
                    requestPlaced = true; // if sent, mark that we've placed a request
                }
            } else { // if we are in a game
                out("You cannot request a game while in a game."); // error message
            }
        } else { // we don't want multiple users to accept a game from the same person - one at a time
            out("You have already requested a game, please wait for a response before making another request."); // error message
        }
    }
    
    /** Displays a game request recieved from the server to the user, and replies with the response. */
    private void gameRequested(String user) {
        Object[] options = {"Yes", "No"};
        int op = JOptionPane.showOptionDialog(super.getParent(),
                "A user would like to start a game with you: " + user,
                "Game Request", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                options, options[1]); // displays a popup window, stores a value for the selected answer

        String response;
        if (op == 0) { // if they said yes, reply accept to the server
            response = "accept " + user + CRLF;
        } else { // if they said no, reply decline
            response = "decline " + user + CRLF;
        }
        send(response); // send the reply
    }
    
    /** Ends the game, called when the server tells us the game is over. */
    private void gameOver(String winner) {
        switch (winner) { // we display a result through the turn label, switch to determine what we say
            case "win":
                turnLabel.setText("You won! =)");
                break;
            case "lose":
                turnLabel.setText("You lost... =(");
                break;
            default:
                turnLabel.setText("The game ended in a draw. =|");
                break;
        }
        inGame = false; // now the user can request a game, cannot resign, etc.
        // don't need to clear out the board, we want the user to see how they won/lost
        // don't need to clear variables, those will be cleared when the client exits or a new game starts
    }
    
    /** Alerts the user when the server says the last game request we made was declined. */
    private void requestDeclined() {
        JOptionPane.showMessageDialog(super.getParent(), "Your last game request was declined."); // popup
        requestPlaced = false; // now we can request another game
    }
    
    /** When the server sends an opponent's move to us, this function makes the move locally. */
    private void oppMove(String data) {
        if (!inGame) { // shouldn't be called from outside a game, but just in case
            out("Error: received a move while not in a game.");
            return;
        }
        
        // get the numeric value from the characters
        
        int startRank = data.charAt(0) - '0';
        int startFile = data.charAt(1) - '0';
        int endRank = data.charAt(2) - '0';
        int endFile = data.charAt(3) - '0';

        if (data.length() == 5) { // if we're passed 5 arguments - used for pawn promotion
            Chess.move(startRank, startFile, endRank, endFile, data.charAt(4));
        } else { // otherwise just make the move
            Chess.move(startRank, startFile, endRank, endFile);
        }
        displayGame(); // redisplay the board
    }
    
    /** Makes a move locally and sends it to the server. */
    private void makeMove(int x1, int y1, int x2, int y2) {
        if (!inGame) { // shouldn't be called from outside a game, but just in case
            out("You are not in a game!");
            return;
        }
        
        char promotion = '\0';
        
        if (Chess.legalMove(x1, y1, x2, y2)) { // if the move is legal
            String move = "move " + x1 + y1 + x2 + y2; // create a message to send
            if ((Chess.getPiece(x1, y1) % 8 == 0) && (x2 == 0 || x2 == 7)) { // pawn promotion case
                Object[] options = {"Queen", "Knight", "Rook", "Bishop"};
                int piece = JOptionPane.showOptionDialog(super.getParent(),
                        "Choose what to promote your pawn to:",
                        "Pawn Promotion",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null, options, options[0]); // popup for pawn promotion, default is queen
                if (piece == 1) { // if they select knight
                    promotion = 'n'; // add the knight character
                } else if (piece == 2) { // if they select rook
                    promotion = 'r'; // add the rook character
                } else if (piece == 3) { // if they select bishop
                    promotion = 'b'; // add the bishop character
                } else { // default case
                    promotion = 'q'; // add the queen character
                }
                move += promotion;
            }
            move += CRLF; // add CRLF to the end of the message
            if (send(move)) { // attempt to send the message
                if ( promotion != '\0' ) {
                    Chess.move(x1, y1, x2, y2, promotion);
                } else {
                    Chess.move(x1, y1, x2, y2); // on successful send, make the move locally
                }
                displayGame(); // redisplay the game
            }
        }
    }
    
    /** Starts a new game sent to us by the server. */
    private void startGame(String[] data) {
        color = data[0].equals("black"); // set our color
        requestPlaced = false; // reset this variable
        inGame = true; // we are now in a game
        if (data.length == 1) { // if the only parameter is color
            Chess = new Game(); // we are starting a fresh game
            out("The game has started and you are the " + (color ? "black" : "white") + " player.");
        } else { // otherwise we are resuming a previous game
            Chess = new Game(data[1]); // initialize to the specified game state
        }
        
        // the default board orientation for the Game class is with white at the bottom
        // however, we want black players to see the board with black pieces at the bottom
        // this loop sets the rank and file values for each button on the board
        // if we are black, we flip the values so that the board is displayed upside down
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j].rank = color ? 7 - i : i;
                board[i][j].file = color ? 7 - j : j;
            }
        }
        displayGame(); // display the game board
    }
    
    /** Used to initially connect to the server. */
    private void connectToServer() {
        
        // temporary variable initialization
        
        Socket tempSocket;
        DataOutputStream tempWrite;
        BufferedReader tempRead;

        try {
            tempSocket = new Socket(serverField.getText(), PORT_NUM);
            tempWrite = new DataOutputStream(tempSocket.getOutputStream());
            tempRead = new BufferedReader(new InputStreamReader(tempSocket.getInputStream()));
            
            // if we connected successfully, then we bind these temp variables to class variables
            // this way, if the connection fails, our socket variable isn't bound
            
            socket = tempSocket;
            toServer = tempWrite;
            fromServer = tempRead;
        } catch (Exception e) { // if we can't connect, tell the user
            out("Could not connect");
        }
    }
    
    /** Logs in to the server, or registers a new user if register is true. */
    private void loginToServer(boolean register) {

        String type;
        if (register) {
            type = "register";
        } else {
            type = "login";
        }
        if (!send(type + " " + userField.getText() + "\t" + passField.getText() + CRLF)) {
            out("Connect to the server before trying to login."); // if we can't connect, scold the user
        } else {
            username = userField.getText(); // if we connect, bind username to a variable
            // we don't just pull from userField again later in case the user changes the field after login
        }
    }
    
    /** Confirms the user's resignation, and sends it to the server. */
    public void askResign() {
        Object[] options = {"Yes", "No"};
        int option = JOptionPane.showOptionDialog(super.getParent(),
                "Are you sure you want to forfeit the game?",
                "Resign?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                options, options[1]); // show popup asking to confirm resign
        if (option == 0) { // if they say yes, send resign to the server
            send("resign" + CRLF);
        } // otherwise do nothing
    }

    public static void main(String[] args) {
        ChessClient cc = new ChessClient(); // display the client
        while (true) { // infinite loop
            try { // need this for isBound
                while (cc.socket.isBound()) { // once the socket is bound to the server, loop forever
                    try {
                        String response = cc.fromServer.readLine(); // block on server response
                        int space = response.indexOf(' ');
                        String type, data = ""; // type indicates method to call in response, data is the parameters
                        if (response.contains(" ")) { // if our response is multiple "words"
                            // split on space
                            type = response.substring(0, space);
                            data = response.substring(space + 1);
                        } else { // otherwise the only parameter is the call itself
                            type = response;
                        }

                        switch (type) { // call method in ChessClient based on type recieved

                            case "svrmsg": // Server has a message for the user
                                cc.out("Server: " + data); // add "Server: " heading and print
                                break;

                            case "msg": // server is only relaying a message
                                cc.out(data); // print the message
                                break;

                            case "players": // recieved list of users online
                                cc.displayChatRoom(data.split("\t")); // display the user list
                                break;

                            case "decline": // other user declined our game request
                                cc.requestDeclined(); // tell the user
                                break;

                            case "gamereq": // recieved a request from another user
                                cc.gameRequested(data); // tell the user
                                break;

                            case "init": // recieved a new game from the server
                                cc.startGame(data.split("\t")); // start the game
                                break;

                            case "move": // the opponent has made a move
                                cc.oppMove(data); // update our board
                                break;

                            case "gameover": // the game has ended
                                cc.gameOver(data);
                                break;
                        }
                    } catch (Exception e) {
                        break;
                    }
                }
            } catch (Exception e) {
                break;


            }
        }
    }

    class MoveListener implements ActionListener { // used on each ChessButton
        
        @Override
        public void actionPerformed(ActionEvent e) {
            
            // we have a max of two buttons selected at a time - one space to move from, one to move to
            // the first piece the user clicks on is saved in 'selected' - this is considered the piece they want to move
            // if they select a space while they have a piece selected, this is considered the square they want to move that piece to
            // if they have a piece selected and they click it again, deselect the selected piece and the user is no longer considered to have that piece selected
            // this is kind of word salad - it's more obvious how it works in practice
            
            ChessButton space = (ChessButton) e.getSource();
            if (inGame == false || (color != Chess.getTurn())) { // if we aren't in a game or it's not our turn
                space.setSelected(false); // deselect the space
                return; // exit
            }
            if (selected == null && !Chess.getValidSelection(space.rank, space.file, color)) { // if we haven't selected a space and this space isn't a valid selection
                space.setSelected(false); // deselect the space
            } else if (space == selected) { // if this space matches the one we've already selected
                selected.setSelected(false); // deselect this space
                selected = null; // we no longer have a space selected
            } else if (selected != null) { // if we have a space already selected
                System.out.println(selected.rank + " " + selected.file + " " + space.rank + " " + space.file);
                System.out.println(Chess.validMove(selected.rank, selected.file, space.rank, space.file));
                System.out.println(Chess.putInCheck(selected.rank, selected.file, space.rank, space.file));
                if (Chess.legalMove(selected.rank, selected.file, space.rank, space.file)) { // if we can move from the space we selected first to the space we selected second, move there
                    makeMove(selected.rank, selected.file, space.rank, space.file); // make the move
                    selected.setSelected(false); // deselect the first space we selected
                    space.setSelected(false); // deselect the second space selected
                    selected = null; // we no longer have any space selected
                } else { // if that would not be a valid move
                    space.setSelected(false); // deselect the most recent selected space
                    // however, our previously selected space remains selected
                }
            } else { // if we have not selected a space already and it's not an invalid selection
                selected = space; // select the space
            }
        }
    }

    class ConnectListener implements ActionListener { // used on the "Connect" button
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (serverField.getText().equals("")) { // if we haven't entered a server address
                out("Need to enter server"); // scold the user
            } else {
                out("Connecting to server: " + serverField.getText()); // tell the user we're connecting
                connectToServer(); // and connect
            }
        }
    }

    class LoginListener implements ActionListener { // used on the "Log in" button
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (userField.getText().equals("")) { // if a username has not been entered
                out("Need to enter username"); // scold the user
            } else if (passField.getText().equals("")) { // if a password has not been entered
                out("Need to enter password"); // scold the user
            } else {
                out("Talking to server... "); // tell the user we're logging in
                loginToServer(false); // log in
            }
        }
    }

    class RegisterListener implements ActionListener { // used on the "Register" button
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (userField.getText().equals("")) { // if a username has not been entered
                out("Need to enter username"); // scold the user
            } else if (passField.getText().equals("")) { // if a password has not been entered
                out("Need to enter password"); // scold the user
            } else {
                out("Talking to server... " + serverField.getText()); // tell the user we're registering
                loginToServer(true); // register
            }
        }
    }

    class RequestListener implements ActionListener { // used on the "Request Game" button
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (userList.getSelectedItem() != null) { // if we have a user selected
                requestGame(userList.getSelectedItem()); // request a game from them
            }
        }
    }

    class ResignListener implements ActionListener { // used on the "Resign" button
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (inGame) { // if we're in a game
                askResign(); // ask if the user really wants to resign, and proceed from there
            }
        }
    }

    class RefreshListener implements ActionListener { // used on the "Refresh user list" button
        
        @Override
        public void actionPerformed(ActionEvent e) {
            send("refresh" + CRLF); // ask the server for an updated user list
        }
    }

    class ChessButton extends JToggleButton { // extension of JToggleButton - only a few extra things

        // rank and file are used to track each button's position for the purpose of determining moves
        
        public int rank;
        public int file;
        
        // minimum and preferred dimensions are overridden in order to size the buttons properly
        // need to have it as an inherent attribute of the button in order to get the layout manager to cooperate properly
        
        @Override
        public Dimension getMinimumSize() {
            return new Dimension(50, 50);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(50, 50);
        }
    }
}
