package coms;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Server extends Thread {
	private Boolean connected;     // Whether or not we successfully made a connection
	private Scanner serverIn;      // Connection to receive commands from server
	private PrintWriter serverOut; // Connection to send commands to server
	private Socket sock;           // Initial socket for connection to server

	public void close() {
		try {
			sock.close();
		}
		catch (Exception e) {
			System.out.println("Unable to close server socket...");
			System.out.println(e);
		}
		serverOut.close();
	}

	/**
	 * Fires when a connection is established with a server.
	 * <br>
	 * Should be overridden by something more useful.
	 */
	public void connected() {
		System.out.println("Server connected.");
	};

	/**
	 * Fires when a connection is lost or terminated with a server.
	 * <br>
	 * Should be overridden by something more useful.
	 */
	public void disconnected() {
		System.out.println("Server disconnected.");
	};

	/**
	 * Receive a message from the client.
	 * <br>
	 * Should be overridden by something more useful.
	 * @param message The message to receive
	 */
	public void receive(String message) {
		System.out.println("Server said: " + message);
	}

	/**
	 * Sends a command to the server.
	 * @param message The command
	 */
	public String send(String message) {
		serverOut.println(message);
		serverOut.flush();
		return message;
	}

	/**
	 * Begin the thread.
	 */
	public void run() {
		if (connected) {
			connected();
			while (serverIn.hasNextLine()) {
				String message = serverIn.nextLine();
				receive(message);
			}
			System.out.println("Server has disconnected!");
			disconnected();
			serverIn.close();
			close();
		}
		disconnected();
	}

	/**
	 * Connect to a game server.
	 * @param remoteAddr The server's address
	 */
	public Server(String remoteAddr, Integer port) {
		// TODO add timer in GameManager, if unable to connect within certain time, say connection timed out in console, and kill the server thread
		connected = false;
		try {
			sock = new Socket(remoteAddr, port);
		}
		catch (Exception e) {
			System.out.println("Unable to connect to " + remoteAddr + " on port " + port);
			System.out.println(e);
			return;
		}
		connected = true;

		try {
			serverIn = new Scanner(sock.getInputStream());
			serverOut = new PrintWriter(sock.getOutputStream());
		}
		catch (Exception e) {
			System.out.println("Could not create Scanner, or PrintWriter for server!");
			System.out.println(e);
			close();
		}
	}
}
