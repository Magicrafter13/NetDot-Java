import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Content panel for displaying player scores and other information.
 * @author Matthew Rease
 * @see Player
 */
@SuppressWarnings("serial")
public class ScoreBoard extends JPanel {
	private Boolean assigned;                           // Whether or not events have been assigned to your player box

	private final ConcurrentHashMap<Player, JPanel> colors = new ConcurrentHashMap<Player, JPanel>();              // Player color box
	private final GameManager game;                                                                                // The game
	private final ConcurrentHashMap<Player, JPanel> infoBoxes = new ConcurrentHashMap<Player, JPanel>();           // Sub-panels for each player
	private final ConcurrentHashMap<Integer, Player> players;                                                      // Player Data
	private final ConcurrentHashMap<Player, JTextField> playerTexts = new ConcurrentHashMap<Player, JTextField>(); // Player name input fields
	private final ConcurrentHashMap<Player, JLabel> scores = new ConcurrentHashMap<Player, JLabel>();              // Player score text

	// Drawing Constants
	private static final int padding = 5;
	private static final int boxPadding = 5;

	/**
	 * Change a player's name.
	 * <br>
	 * Should be overridden by something more useful.
	 * @param player The player being updated
	 * @param name Their new name
	 */
	public void rename(Player player, String name) {
		System.out.println("New name: " + name);
	};

	/**
	 * Change your player's color.
	 * <br>
	 * Should be overridden by something more useful.
	 * @param color The new color
	 */
	public void setColor(Color color) {
		System.out.println("New color: " + color);
	};

	/**
	 * Updates the panel's elements, their spacing, and more.
	 */
	public void update() {
		// Get rid of any objects for players that no longer exist
		infoBoxes.forEach((player, obj) -> {
			if (!players.contains(player)) {
				infoBoxes.remove(player);
				remove(obj);
			}
		});

		// If no players, then there's nothing to do!
		if (players.size() == 0)
			return;

		// Get Bounds per Box
		int columns = (players.size() - 1) / NetDot.maxPlayersPerColumn + 1;
		int ppc = ((Double)Math.ceil((double)players.size() / columns)).intValue();
		// Determine width and height of each player info box.
		int boxWidth = (getWidth() - padding * 2) / columns - padding;
		int boxHeight = (getHeight() - padding * 2) / ppc - padding;
		boolean compress = boxHeight < 21 * 3;

		int index = 0;
		// Update remaining objects
		for (Player player : players.values()) {
			// Add Info Box if it doesn't exist.
			if (!infoBoxes.containsKey(player)) {
				JPanel panel = new JPanel();
				panel.setBorder(new LineBorder(new Color(0, 0, 0)));
				panel.setLayout(null);
				infoBoxes.put(player, panel);
				add(panel);
			}
			JPanel box = infoBoxes.get(player);

			// Add Player Name Field if it doesn't exist.
			if (!playerTexts.containsKey(player)) {
				JTextField field = new JTextField();
				field.addKeyListener(new KeyAdapter() {
					Timer timer = new Timer(1000, new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							rename(player, field.getText());
							timer.stop();
						}
					});
					@Override
					public void keyReleased(KeyEvent e) {
						JTextField text = (JTextField)e.getSource();
						if (!text.getText().equals("")) {
							timer.restart();
						}
					}
				});
				field.setEditable(false);
				//field.setColumns(15);
				playerTexts.put(player, field);
				box.add(field);
			}
			JTextField name = playerTexts.get(player);

			// Add Score text if it doesn't exist.
			if (!scores.containsKey(player)) {
				JLabel label = new JLabel();
				scores.put(player, label);
				box.add(label);
			}
			JLabel score = scores.get(player);

			// Add Color Box if it doesn't exist.
			if (!colors.containsKey(player)) {
				JPanel panel = new JPanel();
				panel.setBorder(new EmptyBorder(0, 0, 0, 0));
				panel.setLayout(null);
				colors.put(player, panel);
				box.add(panel);
			}
			JPanel color = colors.get(player);

			// Update Elements
			name.setText(player.disconnected() ? "Disconnected" : player.toString());
			score.setText(player.disconnected() ? "" : player.score() + " boxes claimed.");
			color.setBackground(
				player.disconnected()
					? Color.BLACK
					: player.getColor() == null
						? Color.getHSBColor((float)index / (float)players.size(), Line.saturation, Line.brightness)
						: player.getColor());

			// Update Element Bounds
			int column = index / ppc;
			int row = index % ppc;

			box.setBounds(
				padding + boxWidth * column + padding * column,
				padding + boxHeight * row + padding * row,
				boxWidth,
				boxHeight);
			name.setBounds(
				boxPadding,
				boxPadding,
				boxWidth - boxPadding * 2,
				21);
			score.setBounds(
				boxPadding,
				boxPadding + 21,
				((Double)((boxWidth - boxPadding * 2) * (compress ? 0.7 : 1.0))).intValue(),
				21);
			color.setBounds(
				boxPadding + (compress ? scores.get(player).getWidth() : 0),
				boxPadding + 21 * (compress ? 1 : 2),
				((Double)((boxWidth - boxPadding * 2) * (compress ? 0.3 : 1.0))).intValue(),
				boxHeight - 21 * (compress ? 1 : 2) - boxPadding * 2);
			index++;
		}
		if (!assigned && players.containsKey(game.clientID)) {
			Player player = players.get(game.clientID);
			playerTexts.get(player).setEditable(true);
			colors.get(player).addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					Color color = JColorChooser.showDialog(new JColorChooser(), "Choose Player Color", getBackground());
					if (color != null) {
						player.setColor(color);
						setColor(color);
					}
				}
			});
			assigned = true;
		}
	}

	/**
	 * Create a new information area for player stats.
	 * <p>
	 * Displays vertical blocks, one for each player.<br>
	 * In each block, the player's name, and current score, are shown, as well as the color of their boxes.
	 * </p>
	 */
	public ScoreBoard(GameManager game, ConcurrentHashMap<Integer,Player> players) {
		this.game = game;
		this.players = players;
		assigned = false;
	}
}