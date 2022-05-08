import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import grid.Grid;
import grid.GridPoint;

/**
 * Represents a dot to be used in a grid of dots.
 * <p>
 * Each dot tracks 2 lines, a horizontal and a vertical line. These represent
 * the line directly to the right, and directly below, the dot.<br>
 * Each dot also tracks a single box.<br>
 * Dots on the edge of the game grid never have a box, will not track one or
 * more lines.
 * </p>
 * @author Matthew Rease
 * @see GameField
 * @see Line
 * @see Box
 */
public class Dot {
	private final Grid grid;         // Grid Data
	private final GridPoint gridPos; // Coordinate position of this dot on the grid
	private final Point panelPos;    // Absolute coordinate position of this dot in its JPanel

	// Drawing Constants
	private static final int offset = 5;

	public final Box box;    // The box
	public final Line down;  // The vertical line
	public final Line right; // The horizontal line

	public static final int diameter = 10; // Diameter of the dot (circle)

	/**
	 * Checks if the user clicked in (or near) a {@link Line}.
	 * @param vertical <code>true</code> if the vertical line is being checked, <code>false</code>
	 * to check the horizontal line
	 * @param point The point clicked
	 * @return <code>true</code> if the line was clicked, <code>false</code> if not
	 */
	public boolean click(boolean vertical, Point point) {
		return vertical
			? down != null ? down.click(point) : false
			: right != null ? right.click(point) : false;
	}

	public void forEach(Consumer<Line> action) {
		Objects.requireNonNull(action);
		if (right != null)
			action.accept(right);
		if (down != null)
			action.accept(down);
	}

	/**
	 * Informs this dot's lines, that the mouse has moved.
	 * @param point The mouse pointer's x,y location
	 */
	public void hover(Point point) {
		if (down != null)
			down.hover(point);
		if (right != null)
			right.hover(point);
	}

	/**
	 * Draws the dot, as well as its lines and box if they exist.
	 * @param g Graphics object to draw to
	 * @see GameField#paint(Graphics)
	 * @see Line#paint(Graphics)
	 * @see Box#paint(Graphics)
	 */
	public void paint(Graphics g) {
		// Draw Dot
		g.setColor(new Color(51, 51, 51));
		g.fillOval(panelPos.x, panelPos.y, diameter, diameter);

		// Draw Lines
		if (right != null)
			right.paint(g);
		if (down != null)
			down.paint(g);
		if (box != null)
			box.paint(g);
	}

	/**
	 * Resets this dot, as well as its lines and box. (Ownership info.)
	 */
	public void reset() {
		if (right != null)
			right.reset();
		if (down != null)
			down.reset();
		if (box != null)
			box.reset();
	}

	/**
	 * Updates elements if window (panel) size has changed.
	 * @param panelWidth The new width of the JPanel
	 * @param panelHeight The new height of the JPanel
	 * @see GameManager#resize()
	 * @see Line#resize(int, int)
	 * @see Box#resize(int, int)
	 */
	public void resize(int panelWidth, int panelHeight) {
		// Determine JPanel Coordinates
		panelPos.setLocation(offset + gridPos.x * panelWidth / (grid.width - 1), offset + gridPos.y * panelHeight / (grid.height - 1));

		// Resize Lines
		if (right != null)
			right.resize(panelWidth, panelHeight);
		if (down != null)
			down.resize(panelWidth, panelHeight);

		// Resize Box
		if (box != null)
			box.resize(panelWidth, panelHeight);
	}

	/**
	 * Create a new dot.<br>
	 * The dot will also create lines and a box if necessary.
	 * @param gridPos Grid position of the dot
	 * @param grid Grid information
	 * @param players The players in the game
	 * @see Line
	 * @see Box
	 * @see GameField
	 * @see Player
	 */
	public Dot(GridPoint gridPos, Grid grid, ConcurrentHashMap<Integer, Player> players) {
		// Set Relative Location
		this.gridPos = gridPos;

		// Game Grid Information
		this.grid = grid;

		// Initialize Absolute Location
		panelPos = new Point();

		// Create Lines
		right = gridPos.x < grid.width - 1 ? new Line(gridPos, grid, players, false) : null;
		down = gridPos.y < grid.height - 1 ? new Line(gridPos, grid, players, true) : null;

		box = right != null && down != null ? new Box(gridPos, grid, players) : null;
	}
}
