import java.awt.Color;

import coms.Client;

/**
 * A player in the game.
 * @author Matthew Rease
 * @see GameManager
 */
public class Player {
	private Integer boxes;                // How many boxes this player owns
	private Client client;                // The client object for this player (shouldn't be used by clients...)
	private Color color;                  // This player's display color
	private Boolean disconnected = false; // Whether or not this player has disconnected (shouldn't be used by clients...)
	private String name;                  // The player's name

	/**
	 * Increment the player's score by one. (Meaning they claimed one box.)
	 */
	public void add() {
		boxes++;
	}

	/**
	 * Add <code>num</code> boxes to the player's score.
	 * @param num How many boxes they have just earned
	 */
	public void add(int num) {
		boxes += num;
	}

	/**
	 * Consider this player to be disconnected. (Changes their name too.)
	 */
	public void disconnect() {
		disconnected = true;
		//reset();
		name = "Disconnected";
	}

	/**
	 * Whether or not this player has been disconnected from the game.
	 * @see Player#disconnect()
	 * @return
	 */
	public boolean disconnected() {
		return disconnected;
	}

	/**
	 * The client that this player represents.
	 * @return <code>client</code> if one was assigned, otherwise <code>null</code>
	 */
	public Client getClient() {
		return client;
	}

	public Integer getClientID() {
		return client == null ? 0 : client.clientID;
	}

	/**
	 * This player's color.
	 * @return <code>color</code> if one was set, <code>null</code> otherwise
	 * @see Player#setColor(Color)
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * Reset this player's score.
	 */
	public void reset() {
		boxes = 0;
	}

	/**
	 * The player's current score.
	 * @return How many boxes this player has claimed
	 * @see GameManager
	 * @see ScoreBoard
	 */
	public int score() {
		return boxes;
	}

	/**
	 * Send a message to this player's client. (Unless they have disconnected.)
	 * @param message The message to send
	 */
	public String send(String message) {
		if (client != null)
			if (!disconnected)
				return client.send(message);
		return "";
	}

	public void setClient(Client client) {
		this.client = client;
	}

	/**
	 * Set this player's color.
	 * @param newColor The player's new color
	 */
	public void setColor(Color newColor) {
		color = newColor;
	}

	/**
	 * Changes this player's name.
	 * @param newName The player's new name
	 * @see GameManager
	 * @see ScoreBoard
	 */
	public void setName(String newName) {
		name = newName;
	}

	public String toString() {
		return name;
	}

	/**
	 * Creates a new player.
	 * @param name The player's display name
	 * @see #setName(String)
	 */
	public Player(String name) {
		this(name, null);
	}

	/**
	 * Create's a new player, associated with a {@link Client}.
	 * @param name The player's display name
	 * @param client The player's communication client
	 */
	public Player(String name, Client client) {
		setName(name);
		setClient(client);
		setColor(null);
		reset();
	}
}
