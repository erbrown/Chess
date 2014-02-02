// Covers the sending/ receiving functions of a player.
package ChessServer;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client implements Runnable {

	private Socket socket;
	private Queue<String> messages = new LinkedList<>();
	private DataOutputStream os;
	private BufferedReader br;
	private boolean mark;
	
	public Profile id;

	public Client(Socket s) {
		socket = s;
		mark = false;
		id = null;
		try {
			InputStream is = socket.getInputStream();
			br = new BufferedReader(new InputStreamReader(is));
			os = new DataOutputStream(socket.getOutputStream());
		} catch (Exception e) {
			System.out.println("Problem with creating io: " + e);
		}
	}

	public synchronized boolean check() {
		return !messages.isEmpty();
	}

	public synchronized String nextMessage() {
		return messages.poll();
	}

	public synchronized boolean send(String message) {
		try {
			os.writeBytes(message);
			return true;
		} catch (IOException e) {
			System.out.println("Some problem with sending: " + e);
			return false;
		}
	}

	public boolean closed() {
		return mark;
	}

	public void run() {

		while (true) {
			try {
				String message = br.readLine();
				synchronized (this) {
					messages.offer(message);
				}
			} catch (IOException e) {
				System.out.println("Client logged off.");
				mark = true;
				break;
			}
		}
	}
}
