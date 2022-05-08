import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JPanel;

import grid.GridPoint;

/**
 * Content panel for displaying dots in the game.
 * <br>
 * Should not be created outside of a {@link GameManager}.
 * @author Matthew Rease
 * @see Dot
 */
@SuppressWarnings("serial")
public class GameField extends JPanel {
	private final ConcurrentHashMap<GridPoint, Dot> dots; // Dot Data

	/**
	 * This method is called when a line (or its area) is clicked.
	 * This method should be overridden by one more useful.
	 * @param line The line that was clicked
	 * @param point The coordinates of the dot that the line belongs to
	 */
	public void click(Line line, GridPoint point) {
		System.out.println("Click!");
	}

	/**
	 * Draws the dots to the panel, in an evenly spaced grid.
	 * @param g Graphics object to draw to
	 * @see JPanel#paint(Graphics)
	 * @see Dot#paint(Graphics)
	 */
	public void paint(Graphics g) {
		super.paint(g);
		dots.forEach((point, dot) -> dot.paint(g));
	}

	/**
	 * Initializes a new dot grid game panel, with mouse events ready.
	 * @see GameField#setDots(ConcurrentHashMap)
	 */
	public GameField(ConcurrentHashMap<GridPoint, Dot> dots) {
		this.dots = dots;
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				dots.forEach((point, dot) -> {
					dot.forEach(line -> {
						if (line.click(e.getPoint()))
							click(line, point);
					});
				});
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				dots.forEach((point, dot) -> {
					dot.hover(e.getPoint());
					repaint();
				});
			}
		});
		setLayout(null);
	}
}
