import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public class Chat extends JFrame {
	private final JTextPane chatPane;
	private final JTextField inputField;
	private final JScrollPane scroll;

	/**
	 * Receives a message from the chat-room and inserts it into the window.
	 * @param message The message to insert
	 */
	public String receive(String message) {
		Document doc = chatPane.getDocument();
		try {
			doc.insertString(doc.getLength(), message + "\n", null);
		}
		catch (Exception e) {
			System.out.println(e);
		}
		return message;
	}

	/**
	 * Send a message to the chat-room.
	 * <br>
	 * Should be overridden by something more useful.
	 * @param message The message to send
	 */
	public void send(String message) {
		System.out.println("Send: " + message);
	};

	/**
	 * Shows/Hides the window.
	 * @see JFrame#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			inputField.grabFocus();
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Chat window = new Chat();
					window.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Initialize the contents of the frame.
	 */
	public Chat() {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				setVisible(false);
			}
		});

		setBounds(100, 100, 450, 300);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		inputField = new JTextField();
		inputField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String message = inputField.getText();
				inputField.setText("");
				send(message);
			}
		});
		getContentPane().add(inputField, BorderLayout.SOUTH);
		inputField.setColumns(10);
		
		chatPane = new JTextPane();
		chatPane.setEditable(false);
		getContentPane().add(chatPane, BorderLayout.CENTER);

		scroll = new JScrollPane(chatPane);
		getContentPane().add(scroll, BorderLayout.CENTER);
	}
}
