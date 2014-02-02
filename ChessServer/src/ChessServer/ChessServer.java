package ChessServer;

import java.io.*;
import java.util.*;

public class ChessServer {

	static final String CRLF = "\r\n";
	static final int MAX_GAMES = 16;

	public static String listNames(ArrayList<Client> players) {
		String names = "";
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i).id != null) {
				names += players.get(i).id.name + '\t';
			}
		}
		return names;
	}

	public static String listPlayers(HashMap<String, Profile> players) {
		String data = "";
		for (Profile profile : players.values()) {
			data += profile.name + (profile.client == null ? "0" : "1") + "\t";
		}
		return data;
	}

	public static void saveData(HashMap<String, Profile> players) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter("profiles.txt"))) {

			for (Profile profile : players.values()) {
				if (!profile.color) {
					int status = 0;
					if (profile.opp != null) {
						status = 1;
						if (profile.game != null) {
							status = 2;
						}
					}
					bw.write(profile.name + "\t" + profile.password + "\t" + status + CRLF);
					if (status != 0) {
						bw.write(profile.opp.name + "\t" + profile.opp.password + CRLF);
					}
					if (status == 2) {
						String gameState = profile.game.gameState();
						for (int j = 0; j < 8; j++) {
							bw.write(gameState.substring(8 * j, 8 * (j + 1)) + CRLF);
						}
						bw.write(gameState.substring(64) + CRLF);
					}
				}
			}
			bw.close();
		} catch (Exception e) {
			System.out.println("Problem with saving file: " + e);
		}
	}

	public static void loadData(HashMap<String, Profile> players) {
		try (BufferedReader br = new BufferedReader(new FileReader("profiles.txt"))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] data = line.split("\t");
				Profile profile = new Profile(data[0], data[1]);
				players.put(data[0], profile);
				int status = Integer.parseInt(data[2]);

				if (status != 0) {
					String[] oppData = br.readLine().split("\t");
					Profile opp = new Profile(oppData[0], oppData[1]);
					players.put(oppData[0], opp);
					profile.setOpp(opp);
					if (status == 2) {
						String gameData = "";
						for (int i = 0; i < 8; i++) {
							gameData += br.readLine();
						}
						gameData += br.readLine();
						profile.setGame(new Game(gameData));
					}
				}
			}
			br.close();
		} catch (IOException | NumberFormatException e) {
			System.out.println("File reading problem: " + e);
		}
	}

	public static void main(String[] args) {

		ArrayList<Client> clients = new ArrayList();
		HashMap<String, Profile> profiles = new HashMap();

		loadData(profiles);
		saveData(profiles);

		Receiver receiver = new Receiver();
		Thread receiverThread = new Thread(receiver);
		Random rand = new Random();
		receiverThread.start();

		Console console = new Console(profiles);

		System.out.println("The server has started.");

		while (true) {

			if (console.checkShutdown()) {
				saveData(profiles);
				System.exit(1);
			}
			
			if (receiver.check()) {
				Client next = new Client(receiver.nextConnection());
				next.send("svrmsg Please log in or register." + CRLF);
				Thread thread = new Thread(next);
				thread.start();
				clients.add(next);
			}

			for (int i = 0; i < clients.size(); i++) {
				if (clients.get(i).closed()) {
					System.out.print("Deleted client: ");
					if (clients.get(i).id == null) {
						System.out.println("Unidentified");
					} else {
						System.out.println(clients.get(i).id.name);
						clients.get(i).id.logOff();
					}
					clients.remove(i);
					i--;
					continue;
				}

				if (clients.get(i).check()) {
					Client sender = clients.get(i);
					String message = sender.nextMessage();
					int space = message.indexOf(' ');
					String type;
					String data;
					if (space == -1) {
						type = message;
						data = "N/A";
					} else {
						type = message.substring(0, space);
						data = message.substring(space + 1);
					}
					System.out.println(type + " : " + data);

					String reply;
					switch (type) {

						case "request":
							Profile recipient = profiles.get(data);
							if (sender.id.opp == null) { //request already initiated
								if (recipient != null) { //recpient's profile exists
									if (recipient.opp == null) { //recipient has not been requested
										sender.id.setOpp(recipient);
										if (recipient.client != null) { //recipient is online
											sender.id.setTime();
											recipient.client.send("gamereq " + sender.id.name + CRLF);
										}
										reply = "svrmsg Request sent." + CRLF;
									} else {
										reply = "svrmsg This player is already paired." + CRLF;
									}
								} else {
									reply = "svrmsg Player not found" + CRLF;
								}
							} else {
								reply = "svrmsg You are already involved in a request." + CRLF;
							}
							System.out.println(reply);
							recipient.client.send(reply);
							break;

						case "chat":
							sender.id.oppSend("chat " + data + CRLF);
							break;

						case "login": //login NAME [tab] PASSWORD
							String[] loginParams = data.split("\t");
							if (profiles.containsKey(loginParams[0])) {
								Profile temp = profiles.get(loginParams[0]);
								if (temp.password.equals(loginParams[1])) {
									if (temp.connect(sender)) {
										reply = "svrmsg Successfully logged in. " + CRLF
												+ "players " + listNames(clients) + CRLF;
										if (sender.id.game != null) {
											sender.send("init " + sender.id.colorWord() + "\t" 
													+ sender.id.game.gameState() + CRLF);
											
										} else {
											if (sender.id.opp != null && sender.id.color) {
												sender.send("gamereq " + sender.id.opp.name + CRLF);
											}
										}
									} else {
										reply = "svrmsg This player is already online, you hacker!" + CRLF;
									}
								} else {
									reply = "svrmsg Incorrect password." + CRLF;
								}
							} else {
								reply = "svrmsg Name unknown." + CRLF;
							}
							System.out.println("Server: " + reply);
							sender.send(reply);

							break;

						case "register":  // register NAME [tab] PASSWORD
							String[] regParams = data.split("\t");
							if (!profiles.containsKey(regParams[0])) {
								Profile n00b = new Profile(regParams[0], regParams[1]);
								n00b.connect(sender);
								profiles.put(regParams[0], n00b);
								reply = "svrmsg Successfully registered." + CRLF
										+ "players " + listNames(clients) + CRLF;
							} else {
								reply = "svrmsg Username already exists." + CRLF;
							}
							sender.send(reply);
							break;

						case "refresh":
							sender.send("players " + listNames(clients) + CRLF);
							break;

						case "cancel":
							if (System.currentTimeMillis() - sender.id.time > 30000) {
								sender.id.resetRequest();
								reply = "Request canceled." + CRLF;
							} else {
								reply = "You must wait at least 30 seconds before cancelling a request." + CRLF;
							}
							sender.send(reply);
							break;

						case "accept":
							Game game = new Game();
							sender.id.setGame(game);
							sender.id.setColor(rand.nextBoolean());
							sender.send("init " + sender.id.colorWord() + CRLF);
							sender.id.oppSend("init " + sender.id.opp.colorWord() + CRLF);
							break;

						case "decline":
							sender.id.oppSend("decline" + CRLF);
							sender.id.opp.resetRequest();
							break;

						case "move":
							Game currentGame = sender.id.game;
							System.out.println("Incoming move from " + sender.id.colorWord() + " and the current turn is " + (currentGame.getTurn() ? "black" : "white"));

							if (currentGame.getTurn() == sender.id.color) {


								int startRank = data.charAt(0) - '0';
								int startFile = data.charAt(1) - '0';
								int endRank = data.charAt(2) - '0';
								int endFile = data.charAt(3) - '0';

								if (data.length() == 5) {
									currentGame.move(startRank, startFile, endRank, endFile, data.charAt(4));
								} else {
									currentGame.move(startRank, startFile, endRank, endFile);
								}

								int gameover = currentGame.checkmate(!sender.id.color);
								sender.id.oppSend("move " + data + CRLF);

								if (gameover != 0) {
									if (gameover == 1) {
										sender.send("gameover win" + CRLF);
										sender.id.oppSend("gameover lose" + CRLF);
									} else {

										sender.send("gameover draw" + CRLF);
										sender.id.oppSend("gameover draw" + CRLF);
									}
									sender.id.gameOver();
								}
								currentGame.print();
							}
							break;

						case "resign":
							sender.send("gameover lose" + CRLF);
							sender.id.oppSend("gameover win" + CRLF
									+ "svrmsg Your opponent has resigned." + CRLF);
							sender.id.gameOver();
							break;
					} // End of message parsing
				}// End of message checking
			} // End of player checking
		} // End of main loop
	}// End of main
} // ALL THE BRACKETS

