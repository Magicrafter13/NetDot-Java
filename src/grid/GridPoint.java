package grid;

import java.awt.Point;

/**
 * Represents a point (coordinate pair) on a grid.
 * @author Matthew Rease
 * @see Grid
 */
@SuppressWarnings("serial")
public class GridPoint extends Point {
	/**
	 * Get a point to the bottom.
	 * @return The {@link GridPoint} directly below (<code>y + 1</code>) of this one
	 */
	public GridPoint down() {
		return new GridPoint(x, y + 1);
	}

	/**
	 * Get a point to the left.
	 * @return The {@link GridPoint} directly to the left (<code>x - 1</code>) of this one
	 */
	public GridPoint left() {
		return new GridPoint(x - 1, y);
	}

	/**
	 * Get a point to the right.
	 * @return The {@link GridPoint} directly to the right (<code>x + 1</code>) of this one
	 */
	public GridPoint right() {
		return new GridPoint(x + 1, y);
	}

	/**
	 * Get a point to the top.
	 * @return The {@link GridPoint} directly above (<code>y - 1</code>) of this one
	 */
	public GridPoint up() {
		return new GridPoint(x, y - 1);
	}

	/**
	 * Indicates whether another point is "equal" to this one.
	 * @param point The operand of the test
	 * @return <code>true</code> if <code>point</code> has the same x, y coordinates as this point, <code>false</code> if not
	 * @see Point#equals(Object)
	 */
	public boolean equals(GridPoint point) {
		return point.x == x && point.y == y;
	}

	public String toString() {
		return x + "," + y;
	}

	public static GridPoint parsePoint(String point) {
		return new GridPoint(Integer.parseInt(point.substring(0, point.indexOf(","))), Integer.parseInt(point.substring(point.indexOf(",") + 1)));
	}

	/**
	 * A new point.
	 * @param column The point's grid x coordinate
	 * @param row The point's grid y coordinate
	 */
	public GridPoint(int column, int row) {
		x = column;
		y = row;
	}
}
