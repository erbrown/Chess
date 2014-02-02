/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ChessServer;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JFrame;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;

/**
 *
 * @author makoren
 */
public class Console extends JFrame {

	private static final String CRLF = "\r\n";
	private JTextField commandLine = new JTextField(80);
	private JTextArea output = new JTextArea(20, 80);
	private JButton submit = new JButton("Submit");
	private HashMap<String, Profile> players;
	private boolean shutdown;

	public Console(HashMap<String, Profile> map) {
		super("Chess Server Console");
		players = map;
		shutdown = false;

		Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);

		commandLine.setFont(font);
		output.setFont(font);
		submit.setFont(font);
		submit.addActionListener(new CmdListener());
		getRootPane().setDefaultButton(submit);

		Panel panel = new Panel(new BorderLayout());
		panel.add(commandLine, BorderLayout.WEST);
		panel.add(submit, BorderLayout.EAST);

		add(panel, BorderLayout.SOUTH);
		add(output, BorderLayout.NORTH);

		addWindowListener(new ServerCloser() {
		});

		pack();
		setVisible(true);
	}

	public boolean checkShutdown() {
		return shutdown;
	}

	private void displayGame(String username) {
		Profile profile = players.get(username);
		String data;
		if (profile != null) {
			if (profile.game != null) {
				String state = profile.game.gameState();
				data = profile.name + " (" + profile.colorWord() + ")" + " vs. "
						+ profile.opp.name + " (" + profile.opp.colorWord() + ")\n";
				for (int i = 0; i < 8; i++) {
					data += state.substring(i * 8, (i + 1) * 8) + "\n";
				}
				data += "\nTurn:       " + (state.charAt(64) == '0' ? "white" : "black") + "\n";
				data += "Castling    Queenside  Kingside\n";
				data += "White:      " + (state.charAt(65) == '1' ? "Yes" : "No ")
						+ "        " + (state.charAt(66) == '1' ? "Yes" : "No ") + "\n";
				data += "Black:      " + (state.charAt(67) == '1' ? "Yes" : "No ")
						+ "        " + (state.charAt(68) == '1' ? "Yes" : "No ") + "\n";
				data += "En passant? " + (state.charAt(69) == '/' ? "No"
						: ("At file " + state.charAt(69)) + "\n");
				output.setText(data);
			} else {
				data = "This player is not in a game.";
			}
		} else {
			data = "Player not found.";
		}
		output.setText(data);
	}

	private void displayPlayers() {
		String data = "";
		for (Profile profile : players.values()) {
			data += profile.name + " (" + (profile.client == null
					? "offline" : "online") + ", " + (profile.opp == null
					? "no opponent" : "opponent: " + profile.opp.name + ", "
					+ (profile.game == null
					? "not in a game" : "in a game")) + ")\n";
		}
		output.setText(data);
	}

	private void sendMessage(String recipient, String message) {
		Profile profile = players.get(recipient);
		if (recipient != null) {
			if (profile.client != null) {
				profile.client.send("svrmsg [ADMIN] " + message + CRLF);
			} else {
				output.setText("This player is offline.");
			}
		} else {
			output.setText("Player not found.");
		}
	}

	class CmdListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			String part[] = commandLine.getText().split(" ");
			try {
				switch (part[0]) {
					case "game":
						displayGame(part[1]);
						break;
					case "players":
						displayPlayers();
						break;
					case "exit":
						shutdown = true;
						break;
					case "msg":
						sendMessage(part[1], part[2]);
						break;
					default:
						output.setText("Command not recognized.\n\n"
								+ "Commands:\n"
								+ "players\n"
								+ "game [playername]\n"
								+ "msg [playername] [message]\n"
								+ "exit");
				}
			} catch (ArrayIndexOutOfBoundsException ex) {
				output.setText("Too few params.");
			}
			commandLine.setText(null);
		}
	}

	class ServerCloser extends WindowAdapter {

		@Override
		public void windowClosing(WindowEvent e) {
			shutdown = true;
		}
	}
}
