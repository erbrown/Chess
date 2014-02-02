package ChessServer;

import java.io.*;
import java.net.*;
import java.util.*;

public class Receiver implements Runnable {

	private Queue<Socket> connections;
	private ServerSocket server;
	private final int port = 1729;

	public Receiver() {
		connections = new LinkedList<>();
		try {
			server = new ServerSocket(port);
		} catch (Exception e) {
			System.out.println("Unable to create receiver: " + e);
		}
	}

	public synchronized boolean check() {
		return !connections.isEmpty();
	}

	public synchronized Socket nextConnection() {
		return connections.poll();
	}

	private void acceptConnections() {

		Socket incomingConnection = null;
		while (true) {
			try {
				incomingConnection = server.accept();
				System.out.println("Incoming connection!");
				synchronized (this) {
					connections.offer(incomingConnection);
				}
			} catch (IOException e) {
				System.out.println("Unable to esablish incoming connection.  " + e);
			}
		}
	}

	public void run() {
		acceptConnections();
	}
}
