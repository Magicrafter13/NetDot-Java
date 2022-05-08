package grid;

import java.awt.Dimension;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Simple Data Class for Managing Point-based Grids.
 * @author Matthew Rease
 * @see GridPoint
 */
public class Grid {
	public int height;    // Grid Height
	public int maxSpaces; // Total Number of Spaces Between the Points in the Grid
	public int width;     // Grid Width

	/**
	 * Check if a point is inside the grid.
	 * @param point The point to check
	 * @return <code>true</code> if this point is within the bounds of the grid, <code>false</code> otherwise
	 */
	public boolean contains(GridPoint point) {
		return point.x >= 0 && point.y >= 0 && point.x < width && point.y < height;
	}

	public void forEach(Consumer<GridPoint> action) {
		Objects.requireNonNull(action);
		for (GridPoint point : newArray()) {
			action.accept(point);
		}
	}

	/**
	 * Create an array of all possible points in the grid.
	 * @return Array of {@link GridPoint}s, one for every point on the grid
	 */
	public GridPoint[] newArray() {
		GridPoint[] array = new GridPoint[width * height];
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				array[height * x + y] = new GridPoint(x, y);
		return array;
	}

	public void resize(Dimension size) {
		width = size.width;
		height = size.height;
		maxSpaces = (width - 1) * (height - 1);
	}

	public void resize(Grid grid) {
		resize(new Dimension(grid.width, grid.height));
	}

	public String toString() {
		return width + "x" + height;
	}

	/**
	 * A new grid, with size specified by a string.
	 * <br>
	 * Uses {@link Integer#parseInt(String)}, and {@link String#indexOf(int)}, and may throw exceptions if the string is not properly formatted.
	 * @param grid Properly formatted grid dimensions
	 * @return Grid of specified size
	 * @see Grid#toString()
	 */
	public static Grid parseGrid(String grid) {
		return new Grid(new Dimension(Integer.parseInt(grid.substring(0, grid.indexOf('x'))), Integer.parseInt(grid.substring(grid.indexOf('x') + 1))));
	}

	public Grid() {
		this(new Dimension(0, 0));
	}

	/**
	 * A new grid of set size.
	 * @param size The point size of the grid
	 */
	public Grid(Dimension size) {
		resize(size);
	}
}