import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JPanel;

/*
 * TODO:
 * allow user to re-claim boxes after disconnect if they reconnect within a certain time period
 * allow new clients to stay connected in a "waiting room" while game in progress, and notify server so they can return to lobby when the game is over
 * pressing enter in any of the main menu text fields should active the start/connect button
 */
import coms.Client;
import coms.ClientCollector;
import coms.Server;
import grid.Grid;
import grid.GridPoint;

/**
 * Main Object that manages the Dots-N-Boxes game, as well as drawing to the
 * screen.
 * @author Matthew Rease
 * @see GameField
 * @see ScoreBoard
 */
public class GameManager {
	private Integer currentPlayer;                  // playerID of the player whose turn it is, -1 if N/A
	private Boolean gameStarted = false;            // Whether the game is running, or we're in the lobby
	private Boolean gameFinished = false;           // Whether or not the game has been completed (somebody won/tied)

	// UI Objects, Data Collections, and Server Objects
	private final Chat chat;                                                                             // Chat window
	private final JPanel contentPane;                                                                    // Main panel for all UI content
	private final ConcurrentHashMap<GridPoint, Dot> dots = new ConcurrentHashMap<GridPoint, Dot>();      // Dot, Line, and Box Data
	private final GameField field;                                                                       // Sub-panel for dot grid (lines + boxes too)
	private final Grid grid;                                                                             // Grid information for Dots
	private final ClientCollector listener;                                                              // Waits for clients to connect to server
	private final Integer maxPlayers;                                                                    // Maximum allowed clients
	private final ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<Integer, Player>(); // Player Data
	private final CopyOnWriteArrayList<Client> queue = new CopyOnWriteArrayList<Client>();               // Client connection queue
	private final ScoreBoard score;                                                                      // Sub-panel for player names, score, and color
	private final Server server;                                                                         // (For clients only) the Server we are communicating with
	private final CopyOnWriteArrayList<Client> spectators = new CopyOnWriteArrayList<Client>();          // Spectator clients, waiting for the next game
	private final TextStrip text;                                                                        // Sub-panel for UI text, and buttons
	private final NetDot window;                                                                         // Main window

	// Constants
	private static final int gridPadding = 20; // Field sub-panel's margin for the dots
	private static final int textHeight = 21;  // Height of elements in the text sub-panel

	public Integer clientID;   // playerID for this client (0 for server, -1 for queued client, and -2 for spectator)
	public Integer nextID = 1; // next available unique ID

	public final Boolean isServer; // Whether or not we are the server, or a client

	// Constants
	public static final double horizontalGamePercentage = 0.703125; // Percentage of horizontal space to dedicate to dot grid (remaining amount goes to scoreboard)
	public static final int port = 1234;                            // Port to communicate on
	public static final int[] version = { 2, 0 };                   // Version information

	private void assign(Client client, Integer playerID) {
		client.clientID = playerID;
		broadcast(client, "network-assign " + playerID);
		updateScore();
	}

	private void close() {
		if (isServer) {
			listener.close();
			for (Client client : queue)
				client.close();
		}
		if (chat.isVisible())
			chat.setVisible(false);
		window.setEnd();
	}
	private void gameRestart() {
		// Stop Game
		gameStop();

		// Start Game
		gameStarted = true;
		gameFinished = false;
		currentPlayer = 0;

		update();
	}

	private void gameStop() {
		gameStarted = false;
		gameFinished = false;
		currentPlayer = -1;

		players.forEach((playerID, player) -> {
			if (player.disconnected()) {
				players.remove(playerID);
				return;
			}
			player.reset();
		});

		if (isServer) {
			moveSpectators();
		}

		dots.forEach((point, dot) -> dot.reset());

		update();
	}

	/**
	 * Attempts a move by the current player.
	 * <p>
	 * If the move is successful, the player's score is incremented by the
	 * number of boxes claimed (if any).<br>
	 * If the player did score points, then they are allowed to take another
	 * turn, otherwise play goes to the next player in line.
	 * </p>
	 * @param gridPos The grid position of the dot that owns the clicked line
	 * @param verticalLine <code>true</code> if the player clicked a vertical line,
	 * <code>false</code> if they clicked a horizontal line
	 * @return <code>true</code> if the move was successfully executed, <code>false</code>
	 * if it was not (most likely the line was already owned by a player)
	 */
	private Boolean makeMove(Integer playerID, GridPoint gridPos, Boolean verticalLine) {
		if (gameFinished || !grid.contains(gridPos))
			return false;

		GridPoint first = verticalLine ? gridPos.left() : gridPos.up();
		GridPoint third = verticalLine ? first.down() : first.right();

		Dot uniqueDot = grid.contains(first) ? dots.get(first) : null;

		Line request = verticalLine ? dots.get(gridPos).down : dots.get(gridPos).right;
		if (!request.setOwner(playerID)) {
			System.out.println("Invalid move! Line already taken.");
			return false;
		}

		// Claim boxes if possible
		Boolean scored = false;
		if (uniqueDot != null &&
			uniqueDot.right.getOwner() >= 0 &&
			uniqueDot.down.getOwner() >= 0 &&
			(verticalLine
				? dots.get(third).right.getOwner()
				: dots.get(third).down.getOwner()) >= 0) {
			uniqueDot.box.setOwner(playerID);
			players.get(playerID).add();
			scored = true;
		}
		Line adjacentLine = verticalLine ? dots.get(gridPos).right : dots.get(gridPos).down;
		if (adjacentLine != null && adjacentLine.getOwner() >= 0 &&
			dots.get(gridPos.right()).down.getOwner() >= 0 &&
			dots.get(gridPos.down()).right.getOwner() >= 0) {
			dots.get(gridPos).box.setOwner(playerID);
			players.get(playerID).add();
			scored = true;
		}

		// If a box was claimed, check if the game is over, otherwise change players. (Players who made a box get another move.)
		if (scored) {
			updateScore();
			int totalScore = 0;
			for (Player player : players.values())
				totalScore += player.score();
			if (totalScore == grid.maxSpaces) {
				gameFinished = true;
				Player[] winners = mostBoxes();
				text.text.setText(winners.length == 1 ? (winners[0] + " wins!") : "Tie!");
			}
		}
		else playerNext();
		updateField();
		return true;
	}

	/**
	 * Get the current highest scoring player.
	 * @return The {@link Player} with the highest score, <code>null</code> if there is a tie
	 * between two or more players.
	 */
	private Player[] mostBoxes() { // TODO utilize the fact that this tells us more than one player (say *who* the tie is between!)
		int tempScore = 0;
		// Find biggest box count first
		for (Player player : players.values())
			if (!player.disconnected() && player.score() > tempScore)
				tempScore = player.score();

		final int highscore = tempScore;

		return players.values().stream().filter(player -> player.score() == highscore).toArray(Player[]::new);
	}

	private void moveSpectators() {
		//System.out.println("Moving spectators into free slots...");
		while ((maxPlayers == 0 || players.size() < maxPlayers) && !spectators.isEmpty()) {
			Client client = spectators.get(0);
			spectators.remove(client);
			playerAdd(client);
		}
	}

	private void updateText() {
		// The sub-panel itself
		text.setBounds(0, 0, contentPane.getWidth(), textHeight);

		// The text
		text.text.setText(
			gameStarted
				? "Your move, " + players.get(currentPlayer)
				: isServer
					? "Waiting for players... press start when ready."
					: "Waiting for host to start the game...");
		// Return to lobby button
		text.lobby.setVisible(gameStarted);
		// (Re)Start Game button
		text.reset.setText(gameStarted ? "Restart Game" : "Start Game");
		// Chat Window Toggle
		// Quit (return to menu) button
		//text.repaint();
	}

	private void updateField() {
		// The sub-panel itself
		field.setBounds(0, textHeight, (int)(contentPane.getWidth() * horizontalGamePercentage), contentPane.getHeight() - textHeight);

		// The dots/lines/boxes
		dots.forEach((point, dot) -> {
			if (dot != null)
				dot.resize(field.getWidth() - gridPadding, field.getHeight() - gridPadding);
		});

		field.repaint();
	}

	private void updateScore() {
		score.setBounds(field.getWidth(), textHeight, contentPane.getWidth() - field.getWidth(), contentPane.getHeight() - textHeight);
		score.update();
		//score.repaint();
	}

	public void broadcast(Client client, String messages) {
		String prefix = "--> " + playerName(client.clientID) + ": ";
		for (String message : messages.split("\n"))
			window.network(prefix + client.send(message));
	}

	public void broadcast(String messages) {
		if (isServer) {
			players.forEach((playerID, player) -> {
				if (playerID > 0)
					broadcast(player.getClient(), messages);
			});
			spectators.forEach(client -> broadcast(client, messages));
		}
		else {
			String prefix = "--> server: ";
			for (String message : messages.split("\n"))
				window.network(prefix + server.send(message));
		}
	}

	public void clear() { // TODO anything else we should clear?
		contentPane.removeAll();
		if (isServer)
			listener.close();
	}
	public void clientMessage(Client client, String messages) {
		String prefix = "<-- " + (client == null ? "self" : playerName(client.clientID)) + ": ";
		for (String message : messages.split("\n")) {
			window.network(prefix + message);
			/*
			 * ID meanings
			 *  0: server
			 * -1: queued client
			 * -2: spectator
			 * >0: player
			 */
			int id = client == null ? 0 : client.clientID;

			Player player = id >= 0 ? players.get(id) : null;

			// Convenient for Message Parsing
			String[] words = message.split(" ");
			String[] command = words[0].split("-");

			if (command.length < 2) {
				if (id != 0)
					broadcast(client, "info-malformed " + command[0] + " was not followed by a hyphen!");
				return;
			}

			switch (command[0]) {
			// player- commands
			case "player":
				if (client == null || client.isValidated()) {
					if (id >= 0) {
						switch (command[1]) {
						case "rename":
							String name = message.substring(words[0].length() + 1);
							broadcast("player-rename " + id + " " + name);
							playerRename(player, name);
							break;
						case "color":
							Integer RGB;
							try {
								RGB = Integer.parseInt(words[1]);
							}
							catch (Exception e) {
								broadcast(client, "info-malformed Could not parse RGB color!");
								break;
							}
							broadcast("player-color " + id + " " + words[1]);
							playerColor(player, new Color(RGB));
							break;
						default:
							broadcast(client, "unknown-player");
						}
					}
					else broadcast(client, "info-warn You aren't a player yet!");
				}
				else broadcast(client, "info-warn Server has not validated you yet!\nrequest-info");
				break;
			// network- commands
			case "network":
				switch (command[1]) {
				case "disconnect":
					broadcast(client, "network-disconnect");
					// Inform Other Clients and Update Server (us)
					if (id < 0) {
						if (queue.contains(client))
							queue.remove(client);
						if (spectators.contains(client))
							spectators.remove(client);
					}
					else {
						broadcast("player-remove " + id);
						playerRemove(id);
						moveSpectators();
					}
					//client.close();
					break;
				case "chat":
					message = message.substring(words[0].length());
					broadcast("network-chat " + id + message);
					chat.receive(playerName(id) + ":" + message);
					if (!chat.isVisible())
						text.chat.setText("New Msg");
					break;
				default:
					broadcast(client, "unknown-network");
				}
				break;
			// game- commands
			case "game":
				if (client == null || client.isValidated()) {
					switch (command[1]) {
					case "play":
						if (id == currentPlayer) {
							GridPoint point;
							try {
								point = GridPoint.parsePoint(words[1]);
							}
							catch (Exception e) {
								broadcast(client, "info-malformed Could not parse GridPoint!");
								break;
							}
							Boolean vertical = words[2].equals("ver");
							if (!vertical && !words[2].equals("hor")) {
								broadcast(client, "info-malformed Could not parse line direction!");
								break;
							}
							if (makeMove(id, point, vertical))
								broadcast("game-play " + id + " " + point + " " + (vertical ? "ver" : "hor"));
							else if (client != null)
								broadcast(client, "info-warn Invalid move!");
						}
						else broadcast(client, "info-warn " + (id >= 0 ? "Not your turn!" : "You aren't part of this game!"));
						break;
					default:
						broadcast(client, "unknown-game");
					}
				}
				else broadcast(client, "info-warn Server has not validated you yet!\nrequest-info");
				break;
			// request- commands
			case "request":
				if (client == null || client.isValidated()) {
					switch (command[1]) {
					case "start":
					case "restart":
						broadcast("network-chat -3 " + chat.receive(playerName(id) + " wants to " + command[1] + " the game."));
						if (!chat.isVisible())
							text.chat.setText("New Msg");
						break;
					case "stop":
						broadcast("network-chat -3 " + chat.receive(playerName(id) + " wants to return to the lobby."));
						if (!chat.isVisible())
							text.chat.setText("New Msg");
						break;
					case "join":
						// If the client isn't in the queue, they must already be a player
						if (queue.contains(client)) {
							// If we're in the lobby, they can join, otherwise they can spectate or leave
							if (!gameStarted) {
								if (maxPlayers == 0 || players.size() != maxPlayers) {
									// Send full game data
									players.forEach((playerID, plyr) -> {
										broadcast(client, "player-add " + playerID + " " + plyr);
										if (plyr.getColor() != null)
											broadcast(client, "player-color " + playerID + " " + plyr.getColor().getRGB());
									});
									broadcast(client,
										"grid-size " + grid + "\n" +
										"grid-reset");
									playerAdd(client);
									queue.remove(client);
								}
								else broadcast(client, "request-deny Server full! (" + players.size() + "/" + maxPlayers + " players) - feel free to spectate\nnetwork-full");
							}
							else broadcast(client, "request-deny Server is in the middle of a game, feel free to spectate.\nnetwork-busy");
						}
						else broadcast(client, "request-deny Already joined.");
						break;
					case "spectate":
						if (queue.contains(client)) {
							if (gameStarted || maxPlayers == players.size()) {
								assign(client, -2);
								// Send full game data
								players.forEach((playerID, plyr) -> {
									broadcast(client, "player-add " + playerID + " " + plyr);
									if (plyr.getColor() != null)
										broadcast(client, "player-color " + playerID + " " + plyr.getColor().getRGB());
								});
								broadcast(client, "grid-size " + grid + "\ngrid-reset");
								spectators.add(client);
								queue.remove(client);
								if (gameStarted)
									broadcast(client, "game-start");
								players.forEach((playerID, plyr) -> {
									if (plyr.disconnected())
										broadcast(client, "player-remove " + playerID);
								});
								broadcast(client, "game-current " + currentPlayer);
								dots.forEach((point, dot) -> {
									dot.forEach(line -> {
										if (line.getOwner() >= 0)
											broadcast(client, "player-line " + line.getOwner() + " " + line + " " + point);
									});
									if (dot.box != null && dot.box.getOwner() >= 0)
										broadcast(client, "player-box " + dot.box.getOwner() + " " + point);
								});
							}
							else broadcast(client, "request-deny There isn't a game running right now, feel free to join the lobby!");
						}
						else broadcast(client, "request-deny Already joined.");
						break;
					default:
						broadcast(client, "unknown-request");
					}
				}
				else broadcast(client, "request-deny Server has not validated you yet!\nrequest-info");
				break;
			// info- commands
			case "info":
				switch (command[1]) {
				case "version":
					if (!client.isValidated()) {
						final int[] version = new int[2];
						try {
							version[0] = Integer.parseInt(words[1]);
							version[1] = Integer.parseInt(words[2]);
						}
						catch (Exception e) {
							broadcast(client, "bad-syntax Could not parse version numbers!");
							return;
						}
						System.out.println("Client is running version " + version[0] + "." + version[1]);
						if (version[0] != GameManager.version[0]) {
							System.out.println("Client version is incompatible with server version (" + GameManager.version[0] + "." + GameManager.version[1] + ")!");
							queue.remove(client);
							client.close();
						}
						else client.validate();
					}
					else broadcast(client, "info-warn Server has already received your version info.");
					break;
				case "malformed":
					System.out.println("The client reported a malformed command...");
					break;
				default:
					broadcast(client, "unknown-info");
				}
				break;
			// unknown- commands
			case "unknown":
				switch (command[1]) {
				case "":
					System.out.println("Client did not recognize command group!");
					break;
				default:
					System.out.println("Client did not recognize " + command[1] + " directive!");
				}
				break;
			default:
				broadcast(client, "unknown-");
			}
		} //else System.out.println("Client called GameServer#clientMessage(Client, String)!");
	}

	public void playerAdd(Client client) {
		int playerID = 0;
		do {
			playerID = nextID;
			nextID++;
		}
		while (players.containsKey(playerID));
		assign(client, playerID);
		Player player = playerAdd(playerID, "Client " + playerID, client);
		broadcast("player-add " + playerID + " " + player);
		window.setCurrent(players.size());
	}

	public Player playerAdd(Integer playerID, String name, Client client) {
		Player player = new Player(name, client);
		players.put(playerID, player);
		updateScore();
		return player;
	}

	public void playerColor(Player player, Color color) {
		player.setColor(color);
		update();
	}

	public void playerConnected(Client client) { // TODO handshake?
		// Add them to the queue
		queue.addIfAbsent(client);

		// Send Server Info
		broadcast(client, "info-version " + version[0] + " " + version[1]);
	}

	public String playerName(Integer playerID) {
		return clientID == playerID
			? "You"
			: playerID == -2
				? "Spectator"
				: playerID == -1 || !players.containsKey(playerID)
					? "Queued Client"
					: players.get(playerID).toString();
	}

	public void playerNext() {
		do {
			currentPlayer++;
			if (currentPlayer >= nextID)
				currentPlayer = 0;
		} while (!players.containsKey(currentPlayer) || players.get(currentPlayer).disconnected());
		updateText();
	}

	public void playerRemove(int playerID) {
		Player player;
		try {
			player = players.get(playerID);
		}
		catch (Exception e) {
			System.out.println(e);
			return;
		}
		if (gameStarted) {
			player.disconnect();
			if (playerID == currentPlayer)
				playerNext();
			updateField();
		}
		else players.remove(playerID);
		updateScore();
	}

	/**
	 * Changes a player's name.
	 * @param player The player's number (<code>>= 0</code>)
	 * @param newName The player's New Name
	 * @see GameManager
	 * @see Player
	 */
	public void playerRename(Player player, String newName) {
		player.setName(newName);
		if (gameStarted) {
			if (players.get(currentPlayer) == player) {
				Player[] winners = mostBoxes();
				text.text.setText(gameFinished
					? (winners.length == 1 && winners[0] == player)
						? player + " wins!"
						: "Tie!"
					: "Your move, " + player);
			}
			updateField();
		}
		updateText();
		updateScore();
	}

	public void serverMessage(String message) {
		String prefix = "<-- server: ";
		window.network(prefix + message);

		String[] words = message.split(" ");
		String[] command = words[0].split("-");

		Integer playerID;
		Boolean vertical;
		GridPoint point;

		switch (command[0]) {
		case "player":
			try {
				playerID = Integer.parseInt(words[1]);
			}
			catch (Exception e) {
				broadcast("info-malformed Could not parse playerID");
				break;
			}
			switch (command[1]) {
			case "add":
				playerAdd(playerID, message.substring(words[0].length() + words[1].length() + 2), null);
				if (playerID >= nextID)
					nextID = playerID + 1;
				break;
			case "rename":
				// Don't rename ourselves (that already should have happened)
				if (playerID != clientID) {
					playerRename(players.get(playerID), message.substring(words[0].length() + words[1].length() + 2));
					updateText();
					updateScore();
				}
				break;
			case "color":
				Integer RGB;
				try {
					RGB = Integer.parseInt(words[2]);
				}
				catch (Exception e) {
					broadcast("info-malformed Could not parse RGB color!");
					break;
				}
				players.get(playerID).setColor(new Color(RGB));
				update();
				break;
			case "remove":
				// If the server has disconnected us (or itself!) return to the menu
				if (playerID == clientID || playerID == 0)
					server.disconnected();
				playerRemove(playerID);
				update();
				break;
			case "line":
				vertical = words[2].equals("ver");
				if (!vertical && !words[2].equals("hor")) {
					broadcast("info-malformed Could not parse line direction!");
					break;
				}
				try {
					point = GridPoint.parsePoint(words[3]);
				}
				catch (Exception e) {
					broadcast("info-malformed Could not parse GridPoint!");
					break;
				}
				Dot dot = dots.get(point);
				(vertical ? dot.down : dot.right).setOwner(playerID);
				updateField();
				break;
			case "box":
				try {
					point = GridPoint.parsePoint(words[2]);
				}
				catch (Exception e) {
					broadcast("info-malformed Could not parse GridPoint!");
					break;
				}
				players.get(playerID).add();
				dots.get(point).box.setOwner(playerID);
				updateScore();
				break;
			default:
				broadcast("unknown-player");
			}
			break;
		case "grid":
			switch (command[1]) {
			case "size":
				Grid newGrid;
				try {
					newGrid = Grid.parseGrid(words[1]);
				}
				catch (Exception e) {
					broadcast("info-malformed Could not parse grid dimensions!");
					break;
				}
				grid.resize(newGrid);
				// Remove old dots (if board shrunk)
				dots.forEach((pnt, dot) -> {
					if (!grid.contains(pnt))
						dots.remove(pnt);
				});
				// Add new dots (if board expanded)
				for (GridPoint pnt : grid.newArray())
					if (!dots.containsKey(pnt))
						dots.put(pnt, new Dot(pnt, grid, players));
				break;
			case "reset":
				grid.forEach(pnt -> dots.get(pnt).reset());
				updateField();
				break;
			default:
				broadcast("unknown-grid");
			}
			break;
		case "network":
			switch (command[1]) {
			case "assign":
				try {
					playerID = Integer.parseInt(words[1]);
				}
				catch (Exception e) {
					broadcast("info-malformed Could not parse clientID!");
					break;
				}
				clientID = playerID;
				switch (clientID) {
				case -2:
					System.out.println("Assigned to spectator mode.");
					break;
				case -1:
					System.out.println("Placed in queue, waiting for further instructions from the server...");
					break;
				default:
					System.out.println("Joining game with player ID " + clientID);
				}
				break;
			case "busy":
				System.out.println("The server is already in the middle of a game. Asking to spectate.");
				broadcast("request-spectate");
				break;
			case "chat":
				try {
					playerID = Integer.parseInt(words[1]);
				}
				catch (Exception e) {
					broadcast("info-malformed Could not parse playerID!");
					break;
				}
				chat.receive((playerID > -3 ? playerName(playerID) + ": " : "") + message.substring(words[0].length() + words[1].length() + 2));
				if (!chat.isVisible())
					text.chat.setText("New Msg");
				break;
			case "disconnect":
				server.close();
				break;
			case "full":
				System.out.println("The server is full! Asking to spectate.");
				broadcast("request-spectate");
				break;
			default:
				broadcast("unknown-network");
			}
			break;
		case "game":
			switch (command[1]) {
			case "start":
			case "restart":
				gameRestart();
				break;
			case "play":
				try {
					playerID = Integer.parseInt(words[1]);
				}
				catch (Exception e) {
					broadcast("info-malformed Could not parse playerID!");
					break;
				}
				try {
					point = GridPoint.parsePoint(words[2]);
				}
				catch (Exception e) {
					broadcast("info-malformed Could not parse GridPoint!");
					break;
				}
				vertical = words[3].equals("ver");
				if (!vertical && !words[3].equals("hor")) {
					broadcast("info-malformed Could not parse line direction!");
					break;
				}
				makeMove(playerID, point, vertical);
				break;
			case "stop":
				gameStop();
				break;
			case "current":
				try {
					playerID = Integer.parseInt(words[1]);
				}
				catch (Exception e) {
					broadcast("info-malformed Could not parse current player!");
					break;
				}
				currentPlayer = playerID;
				updateText();
				break;
			default:
				broadcast("unknown-game");
			}
			break;
		case "info":
			switch (command[1]) {
			case "warn":
				System.out.println("Received warning: " + message.substring(words[0].length()));
				break;
			case "malformed":
				System.out.println("Whatever you just did sent a pretty bad request to the server, please report this error!");
				break;
			case "version":
				int[] version = new int[2];
				try {
					version[0] = Integer.parseInt(words[1]);
					version[1] = Integer.parseInt(words[2]);
				}
				catch (Exception e) {
					broadcast("info-malformed Could not parse version numbers!");
				}
				System.out.println("Server is running version " + version[0] + "." + version[1]);
				if (version[0] != GameManager.version[0]) {
					System.out.println("Server version is incompatible with client version (" + GameManager.version[0] + "." + GameManager.version[1] + ")!");
					//serverOut.close();
				}
				break;
			default:
				broadcast("unknown-info");
			}
			break;
		case "request":
			switch (command[1]) {
			case "deny":
				System.out.println("Server denied request with reason: " + message.substring(words[0].length() + 1));
				break;
			case "info":
				broadcast("info-version " + version[0] + " " + version[1]);
				break;
			}
			break;
		case "unknown":
			switch (command[1]) {
			case "":
				System.out.println("Server did not recognize command group!");
				break;
			default:
				System.out.println("Server did not recognize " + command[1] + " directive!");
			}
			break;
		default:
			broadcast("unknown-");
		}
	}

	public void update() {
		updateText();
		updateField();
		updateScore();
	}

	/**
	 * Begins a new game.
	 * The game can be restarted with {@link GameManager#reset()}
	 * @param window Main program window
	 * @param panel Content panel where this game can be drawn ({@link GameManager} will create and draw all visual elements for you)
	 * @param isServer Whether or not this is a server, or a client
	 * @param maxPlayers Maximum number of clients that can connect
	 * @param grid Dot grid to use for the game
	 * @param remoteAddr Address of the server to connect to
	 * @see Grid
	 * @see ClientCollector
	 */
	@SuppressWarnings("serial")
	private GameManager(NetDot window, JPanel panel, Boolean isServer, Integer maxPlayers, Grid grid, String remoteAddr) {
		this.window = window;         // Save window
		contentPane = panel;          // Save Content Panel (Main Display)
		this.isServer = isServer;     // Server or Client
		this.maxPlayers = maxPlayers; // Set client limit

		// Setup Text Display Panel
		text = new TextStrip(textHeight);
		text.quit.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				broadcast("network-disconnect");
				close();
			}
		});
		text.chat.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				chat.setVisible(!chat.isVisible());
				text.chat.setText("Toggle Chat");
			}
		});
		contentPane.add(text);

		// Setup Game Field Panel
		field = new GameField(dots) {
			@Override
			public void click(Line line, GridPoint point) {
				if (gameStarted) {
					if (!gameFinished) {
						if (clientID == currentPlayer) {
							String message = "game-play " + point + " " + line;
							if (isServer)
								clientMessage(null, message);
							else
								broadcast(message);
						}
						else System.out.println("It's not your turn.");
					}
					else System.out.println("The game is over!");
				}
				else System.out.println("Game hasn't started yet.");
			}
		};
		contentPane.add(field);

		// Setup Score Board Panel
		score = new ScoreBoard(this, players) {
			@Override
			public void rename(Player player, String name) {
				playerRename(player, name);
				broadcast("player-rename " + (isServer ? "0 " : "") + name);
			}

			@Override
			public void setColor(Color color) {
				players.get(clientID).setColor(color);
				update();
				String message = "player-color " + color.getRGB();
				if (isServer)
					clientMessage(null, message);
				else
					broadcast(message);
			}
		};
		score.setLayout(null);
		contentPane.add(score);

		// Setup Grid, Initialize Player Data, and Initialize Dot Grid Data
		this.grid = grid;

		// Initialize program state before making/accepting connections!
		gameStop();

		// Chat Window
		chat = new Chat() {
			@Override
			public void send(String message) {
				String command = "network-chat " + (message.indexOf("\n") == -1 ? message : message.substring(0, message.indexOf("\n")));
				if (isServer)
					clientMessage(null, command);
				else
					broadcast(command);
			}
		};
		chat.setVisible(false);

		if (isServer) {
			listener = new ClientCollector(port) {
				@Override
				public void connected(Socket sock) {
					Client client = new Client(sock) {
						@Override
						public void receive(String message) {
							clientMessage(this, message);
						}
					};
					assign(client, -1);
					client.disconnect = "network-disconnect";
					client.start();
					playerConnected(client);
				}
			};
			listener.start();
			server = null;
		}
		else {
			server = new Server(remoteAddr, port) {
				@Override
				public void connected() {
					broadcast("info-version " + version[0] + " " + version[1] + "\nrequest-join");
				}

				@Override
				public void disconnected() {
					if (chat.isVisible())
						chat.setVisible(false);
					window.setEnd();
				}

				@Override
				public void receive(String message) {
					serverMessage(message);
				}
			};
			server.start();
			listener = null;
		}
	}
	public GameManager(NetDot window, JPanel panel, Dimension size, Integer maxPlayers) {
		this(window, panel, true, maxPlayers, new Grid(size), null);

		clientID = 0;

		// Setup Text Display Panel
		text.reset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				broadcast("game-" + (gameStarted ? "restart" : "start"));
				gameRestart();
			}
		});
		text.lobby.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				broadcast("game-stop");
				gameStop();
			}
		});

		serverMessage("grid-size " + grid);
		serverMessage("grid-reset");

		playerAdd(0, "Server", null);
		window.setCurrent(1);

		updateText();
		updateField();
	}
	public GameManager(NetDot window, JPanel panel, String remoteAddr) {
		this(window, panel, false, 0, new Grid(), remoteAddr);

		clientID = -1;

		// Setup Text Display Panel
		text.reset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				broadcast("request-" + (gameStarted ? "restart" : "start"));
			}
		});
		text.lobby.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				broadcast("request-stop");
			}
		});

		update();
	}
}
