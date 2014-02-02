// Covers the identification of a player.
//Maybe this should have the send function and the server loops through profiles connected to clients
package ChessServer;

public class Profile {

	public String name;
	public String password;
	public Game game;
	public Client client;
	public boolean color; //counterpart designation (requester or requested / white or black)
	public Profile opp; //counterpart
	
	public /*it's been such a*/ long time;
	private boolean linked;
	
	public Profile(String n, String p) {
		name = n;
		password = p;
		game = null;
		color = false;
		linked = false;
		opp = null;
		time = 0;
	}

	public boolean oppSend(String message)
	{
		if (opp != null && opp.client != null) {
			opp.client.send(message);
			return true;
		}
		return false;
	}
	
	public void print() {
		System.out.println(name + " " + ((game == null) ? "is not in a game"
				: ("is the " + colorWord() + " player in a game with " + opp.name)) + ".");
	}

	public void logOff() {
		linked = false;
	}

	public void setTime() {
		time = System.currentTimeMillis();
	}

	public void setGame(Game g) {
		game = g;
		opp.game = g;
	}

	public void setColor(boolean c) {
		color = c;
		opp.color = !c;
	}

	public void setOpp(Profile request) {
		opp = request;
		opp.opp = this;
		color = false;
		opp.color = true;
	}

	public String colorWord() {
		return color ? "black" : "white";
	}

	public boolean connect(Client c) {
		if (linked) {
			return false;
		} else {
			linked = true;
			client = c;
			client.id = this;
			return true;
		}
	}

	public void gameOver() {
		game = null;
		opp.game = null;
		color = false;
		opp.color = false;
		opp.opp = null;
		opp = null;
	}

	public void resetRequest() {
		color = false;
		opp.color = false;
		opp.opp = null;
		opp = null;
		time = 0;
	}
}
