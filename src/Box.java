import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.concurrent.ConcurrentHashMap;

import grid.Grid;
import grid.GridPoint;

/**
 * A box for the game grid. Should be created by a {@link Dot}, special exception may be made for a {@link ScoreBoard}.
 * @author Matthew Rease
 * @see GameField
 */
public class Box {
	private Rectangle bounds; // Absolute size and position in the parent JPanel
	private Integer owner;    // Owner of this box

	private final Grid grid;                                  // Game grid information
	private final GridPoint gridPos;                          // Position on game grid
	private final ConcurrentHashMap<Integer, Player> players; // Player Data

	// Drawing Constants
	private static final int offset = 5;

	/**
	 * Get the box's owner.
	 * @return The number of the player that owns the box
	 */
	public int getOwner() {
		return owner;
	}

	/**
	 * Draw the box.
	 * @param g Graphics object to draw to.
	 * @see Dot#paint(Graphics)
	 */
	public void paint(Graphics g) {
		if (owner != -1) {
			if (!players.get(owner).disconnected())
				if (players.get(owner).getColor() != null)
					g.setColor(players.get(owner).getColor());
				else
					g.setColor(Color.getHSBColor((float)owner / (float)players.size(), Line.saturation, Line.brightness));
			else g.setColor(Color.BLACK);
			g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

			g.setColor(Color.BLACK);
			g.drawString(players.get(owner).toString().substring(0, 1), bounds.x + (bounds.width / 2) - 4, bounds.y + (bounds.height / 2) + 4);
		}
	}

	/**
	 * Reset box ownership.
	 */
	public void reset() {
		owner = -1;
	}

	/**
	 * Adjust elements if window (panel) has been resized.
	 * @param panelWidth New width of the game grid panel
	 * @param panelHeight New height of the game grid panel
	 * @see GameField
	 * @see Dot#resize(int, int)
	 */
	public void resize(int panelWidth, int panelHeight) {
		// Determine JPanel Coordinates
		bounds.setLocation(gridPos.x * panelWidth / (grid.width - 1) + offset * 4, gridPos.y * panelHeight / (grid.height - 1) + offset * 4);

		// Set Size
		bounds.setSize(panelWidth / (grid.width - 1) - offset * 4, panelHeight / (grid.height - 1) - offset * 4);
	}

	/**
	 * Claim ownership of the box.
	 * @param player Number of the player claiming the box
	 * @return <code>true</code> if successfully claimed, <code>false</code> if box already owned
	 */
	public Boolean setOwner(int playerID) {
		if (players.containsKey(playerID)) {
			if (owner == -1) {
				owner = playerID;
				return true;
			}
		}
		return false;
	}

	/**
	 * A new box, with no owner.
	 * @param gridPos Grid position of the box's {@link Dot}
	 * @param grid Grid information
	 * @param players Players in the game
	 * @see GridPoint
	 */
	public Box(GridPoint gridPos, Grid grid, ConcurrentHashMap<Integer, Player> players) {
		// Set Relative Location
		this.gridPos = gridPos;

		// Game Grid Information
		this.grid = grid;

		// Initialize Absolute Location
		bounds = new Rectangle();

		// Set Player Count
		this.players = players;

		reset();
	}
}
