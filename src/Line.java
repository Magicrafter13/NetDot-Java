import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.concurrent.ConcurrentHashMap;

import grid.Grid;
import grid.GridPoint;

/**
 * A line belonging to a {@link Dot}.<br>
 * Player's can take ownership of the line, but ownership cannot be transferred.
 * @author Matthew Rease
 * @see GameField
 */
public class Line {
	private Color color;                                // Current line color
	private Integer owner;                              // Owner of this line
	private Rectangle panelRect;                        // Absolute location and size, in the parent JPanel
	private ConcurrentHashMap<Integer, Player> players; // Player Data
	private Polygon validArea;                          // Bounds for Valid Mouse Clicks/Hovers

	private final Grid grid;         // Game grid information
	private final GridPoint gridPos; // Location on the grid
	private final Boolean vertical;  // Line orientation

	// Drawing Constants
	private static final int close = 3;
	private static final Color defaultColor = new Color(230, 230, 230); // Default drawing color
	private static final int far = 15;
	private static final Color hoverColor = new Color(180, 180, 180);   // Color when hovering over line with mouse
	private static final int offset = 5;
	private static final boolean showMouseBoundaries = false;           // Show debug boundary lines
	private static final int size = 4;                                  // Line thickness

	public static final float brightness = 0.90f, saturation = 0.85f; // Default color saturation and brightness (HSB)

	/**
	 * Check if the user clicked in (or near) the line.
	 * @param point Where the user clicked
	 * @return <code>true</code> if the user clicked this line, <code>false</code> if not
	 */
	public boolean click(Point point) {
		return validArea.contains(point);
	}

	/**
	 * Get the user that owns this line.
	 * @return The owner (0 if unclaimed)
	 */
	public int getOwner() {
		return owner;
	}

	/**
	 * Tell the line where the mouse is hovering, for color changing.
	 * @param point The location of the mouse cursor
	 */
	public void hover(Point point) {
		if (owner == -1)
			color = validArea == null || !validArea.contains(point) ? defaultColor : hoverColor;
	}

	/**
	 * Draw the line.
	 * @param g Graphics object to draw to
	 * @see GameField#paint(Graphics)
	 * @see Dot#paint(Graphics)
	 */
	public void paint(Graphics g) {
		if (owner >= 0) {
			if (!players.get(owner).disconnected())
				if (players.get(owner).getColor() != null)
					color = players.get(owner).getColor();
				else
					color = Color.getHSBColor((float)owner / (float)players.size(), saturation, brightness);
			else
				color = Color.BLACK;
		}
		g.setColor(color);
		g.fillRect(panelRect.x, panelRect.y, panelRect.width, panelRect.height);

		// Draw Mouse Bounds
		if (showMouseBoundaries)
			g.drawPolygon(validArea);
	}

	/**
	 * Reset line ownership and color.
	 */
	public void reset() {
		owner = -1;
		color = defaultColor;
	}

	/**
	 * Adjust elements if window (panel) has been resized.
	 * @param panelWidth New width of the game grid panel
	 * @param panelHeight New height of the game grid panel
	 * @see GameField
	 * @see Dot#resize(int, int)
	 */
	public void resize(int panelWidth, int panelHeight) {
		// Determine JPanel Coordinates and Size
		panelRect.setBounds(
			offset + gridPos.x * panelWidth / (grid.width - 1) + (vertical ? close : far),
			offset + gridPos.y * panelHeight / (grid.height - 1) + (vertical ? far : close),
			vertical ? size : panelWidth / (grid.width - 1) - offset * 4,
			vertical ? panelHeight / (grid.height - 1) - offset * 4 : size
		);

		// Calculate Mouse Diamond
		int horizontalRadius = (panelWidth / (grid.width - 1) - offset * 4) / 2 + Dot.diameter / 2 + offset;
		int verticalRadius = (panelHeight / (grid.height - 1) - offset * 4) / 2 + Dot.diameter / 2 + offset;
		Point center = new Point(panelRect.x + (panelRect.width / 2), panelRect.y + (panelRect.height / 2));
		validArea = new Polygon(
			new int[] {
				center.x - horizontalRadius,
				center.x,
				center.x + horizontalRadius,
				center.x
			},
			new int[] {
				center.y,
				center.y - verticalRadius,
				center.y,
				center.y + verticalRadius
			},
			4);
	}

	/**
	 * Claim ownership of this line.
	 * @param player The claimant
	 * @return <code>true</code> if the line was claimed, <code>false</code> if it was already claimed
	 */
	public Boolean setOwner(Integer playerID) {
		if (players.containsKey(playerID)) {
			if (owner == -1) {
				owner = playerID;
				return true;
			}
		}
		return false;
	}

	public String toString() {
		return vertical ? "ver" : "hor";
	}

	/**
	 * A new line - should only be created by a {@link Dot}.
	 * @param gridPos Grid position of this line's {@link Dot}
	 * @param grid Grid information
	 * @param players Player data
	 * @param vertical <code>true</code> if this is a vertical line, <code>false</code> if horizontal
	 */
	public Line(GridPoint gridPos, Grid grid, ConcurrentHashMap<Integer, Player> players, boolean vertical) {
		// Set Relative Location
		this.gridPos = gridPos;

		// Game Grid Information
		this.grid = grid;

		// Initialize Absolute Location and Size
		panelRect = new Rectangle();

		// Set Player Count
		this.players = players;

		// Set Line Direction
		this.vertical = vertical;

		reset();
	}
}
