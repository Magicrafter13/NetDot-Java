import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class TextStrip extends JPanel {
	public final JButton chat = new JButton("Toggle Chat");
	public final JButton lobby = new JButton("Return to Lobby");
	public final Integer panelHeight;
	public final JButton quit = new JButton("Quit");
	public final JButton reset = new JButton();
	public final JLabel text = new JLabel();

	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		text.setBounds(0, 0, getWidth(), panelHeight);
		lobby.setBounds(getWidth() - 100 - 120 - 120 - 160, 0, 160, panelHeight);
		reset.setBounds(getWidth() - 100 - 120 - 120, 0, 120, panelHeight);
		chat.setBounds(getWidth() - 100 - 120, 0, 120, panelHeight);
		quit.setBounds(getWidth() - 100, 0, 100, panelHeight);
	}

	public TextStrip(int panelHeight) {
		this.panelHeight = panelHeight;

		add(text);
		add(lobby);
		add(reset);
		add(chat);
		add(quit);
		setLayout(null);
	}
}