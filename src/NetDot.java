import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.net.InetAddress;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import coms.Server;

/**
 * A GUI-based game of "Dots n Boxes".
 * @author Matthew Rease
 * @class CSE 223 B
 * @date 5/19/21
 */
@SuppressWarnings("serial")
public class NetDot extends JFrame {
	private Boolean end = false;   // Indicates that the program should return from the game screen (GameManager) to the initial menu
	private GameManager game;      // A running game
	private Server masterServer;   // Connection to the master server list

	// UI Objects, and Thread
	private final JTextField address = new JTextField();                                                  // Remote address input field
	private final JCheckBox advertise = new JCheckBox("Advertise on Master Server");                      // Whether or not to send server details to "master server"
	private final JPanel clientPane = new JPanel();                                                       // Pane for connecting to a server
	private final FlowLayout flow = new FlowLayout();                                                     // Layout for ribbon
	private final JPanel gamePane = new JPanel();                                                         // Game Panel
	private final GridBagConstraints gbc_ribbon_client = new GridBagConstraints();                        // Constraints for shared ribbon in client tab
	private final GridBagConstraints gbc_ribbon_option = new GridBagConstraints();                        // Constraints for shared ribbon in option tab
	private final GridBagConstraints gbc_ribbon_server = new GridBagConstraints();                        // Constraints for shared ribbon in server tab
	private final JSpinner gridWidth = new JSpinner(new SpinnerNumberModel(8, 2, Integer.MAX_VALUE, 1));  // Grid Width input spinner
	private final JSpinner gridHeight = new JSpinner(new SpinnerNumberModel(8, 2, Integer.MAX_VALUE, 1)); // Grid Height input spinner
	private final JCheckBox limitPlayers = new JCheckBox("Limit Number of Players");                      // Limit players check box
	private final JSpinner maxPlayers = new JSpinner(new SpinnerNumberModel(5, 2, Integer.MAX_VALUE, 1)); // Maximum Player count input spinner
	private final JTabbedPane menuPane = new JTabbedPane();                                               // Main UI Panel
	private final DefaultListModel<String> model = new DefaultListModel<String>();                        // List model for server list
	private final Thread monitor;                                                                         // Monitors `end` field, and when true, returns the user to the menu
	private final JCheckBox networkTraffic = new JCheckBox("Show Network Traffic");                       // Whether or not to output network traffic
	private final JPanel optionPane = new JPanel();                                                       // Options Panel
	private final JPanel ribbon = new JPanel();                                                           // Shared UI Ribbon
	private final ArrayList<InetAddress> serverAddresses = new ArrayList<InetAddress>();                  // IP Address of servers on server list
	private final ArrayList<Integer> serverCurrents = new ArrayList<Integer>();                           // Current player count of servers on server list
	private final JList<String> serverList = new JList<String>(model);                                    // List of servers
	private final ArrayList<Integer> serverMaxes = new ArrayList<Integer>();                              // Max players of servers on server list
	private final JTextField serverName = new JTextField();                                               // Advertise name
	private final ArrayList<String> serverNames = new ArrayList<String>();                                // Names of servers on server list
	private final JPanel serverPane = new JPanel();                                                       // Pane for starting a server
	private final JButton start = new JButton("placeholder");                                             // Begins hosting / begins connecting to host

	// Constants
	private static final String masterServerAddress = "matthewrease.net"; // Master server :)
	//private static final int minWindowHeight = 391, minWindowWidth = 494; // Minimum window dimensions
	private static final int windowHeight = 480, windowWidth = 640;       // Default window dimensions

	// Constants
	public static final int maxPlayersPerColumn = 6; // How many players can be in a column before splitting it TODO this shouldn't be here...

	private void initializeClient() {
		/*
		 * Setup Tab Main Pane + Layout
		 */
		final JPanel main = new JPanel();

		// Server section
		final int rows0 = 1;
		final JLabel section0 = new JLabel(" - Server - ");
		final JLabel addressText = new JLabel("Remote Address:");

		section0.setHorizontalAlignment(SwingConstants.CENTER);

		// Layout
		GridBagLayout gbl_main = new GridBagLayout();
		gbl_main.columnWeights = new double[] { 0.3, 0.3, 0.4 };
		gbl_main.rowWeights = new double[] { Double.MIN_VALUE };
		main.setLayout(gbl_main);

		// Constraints for Server Section
		GridBagConstraints gbc_section0 = new GridBagConstraints();
		gbc_section0.fill = GridBagConstraints.BOTH;
		gbc_section0.gridx = 0;
		gbc_section0.gridy = 0;
		main.add(section0, gbc_section0);

		// Constraints for Address label
		GridBagConstraints gbc_addressText = new GridBagConstraints();
		gbc_addressText.fill = GridBagConstraints.BOTH;
		gbc_addressText.gridx = 1;
		gbc_addressText.gridy = 0;
		main.add(addressText, gbc_addressText);

		// Constraints for Address input field
		GridBagConstraints gbc_addressField = new GridBagConstraints();
		gbc_addressField.fill = GridBagConstraints.BOTH;
		gbc_addressField.gridx = 2;
		gbc_addressField.gridy = 0;
		main.add(address, gbc_addressField);

		// Internet section
		final int rows1 = 2;
		final JLabel section1 = new JLabel(" - Internet - ");
		final JButton refresh = new JButton("Refresh List");
	
		section1.setHorizontalAlignment(SwingConstants.CENTER);
		refresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				model.removeAllElements();
				serverNames.clear();
				serverCurrents.clear();
				serverMaxes.clear();
				serverAddresses.clear();
				masterServer.send("list");
			}
		});
		serverList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int index = serverList.locationToIndex(e.getPoint());
					if (index >= 0) {
						address.setText(serverAddresses.get(index).getHostAddress());
						start.doClick();
					}
				}
			}
		});

		// Constraints for Internet Section
		GridBagConstraints gbc_section1 = new GridBagConstraints();
		gbc_section1.fill = GridBagConstraints.BOTH;
		gbc_section1.gridx = 0;
		gbc_section1.gridy = rows0;
		main.add(section1, gbc_section1);

		// Constraints for Refresh button
		GridBagConstraints gbc_refresh = new GridBagConstraints();
		gbc_refresh.fill = GridBagConstraints.BOTH;
		gbc_refresh.gridx = 2;
		gbc_refresh.gridy = rows0;
		main.add(refresh, gbc_refresh);

		// Constraints for Server list
		GridBagConstraints gbc_serverList = new GridBagConstraints();
		gbc_serverList.fill = GridBagConstraints.BOTH;
		gbc_serverList.gridx = 1;
		gbc_serverList.gridy = rows0 + 1;
		gbc_serverList.gridwidth = 2;
		main.add(serverList, gbc_serverList);

		/*
		 * Setup Tab Pane + Layout
		 */
		GridBagLayout gbl_clientPane = new GridBagLayout();
		gbl_clientPane.columnWeights = new double[] { 1.0 };
		gbl_clientPane.rowWeights = new double[] { 0.0, 1.0 };
		clientPane.setLayout(gbl_clientPane);

		GridBagConstraints gbc_main = new GridBagConstraints();
		gbc_main.insets = new Insets(0, 0, 5, 0);
		gbc_main.fill = GridBagConstraints.BOTH;
		gbc_main.gridx = 0;
		gbc_main.gridy = 0;
		clientPane.add(main, gbc_main);

		GridBagConstraints gbc_spacing = new GridBagConstraints();
		gbc_spacing.fill = GridBagConstraints.BOTH;
		gbc_spacing.gridx = 0;
		gbc_spacing.gridy = 1;
		clientPane.add(new JPanel(), gbc_spacing);

		gbc_ribbon_client.fill = GridBagConstraints.BOTH;
		gbc_ribbon_client.gridx = 0;
		gbc_ribbon_client.gridy = 2;
	}

	private void initializeOption() {
		/*
		 * Setup Tab Main Pane + Layout
		 */
		final JPanel main = new JPanel();

		// Server section
		final int rows0 = 1;
		final JLabel section0 = new JLabel(" - Debug - ");

		section0.setHorizontalAlignment(SwingConstants.CENTER);
		networkTraffic.setSelected(true);

		// Layout
		GridBagLayout gbl_main = new GridBagLayout();
		gbl_main.columnWeights = new double[] { 0.3, 0.3, 0.4 };
		gbl_main.rowWeights = new double[] { Double.MIN_VALUE };
		main.setLayout(gbl_main);

		// Constraints for Debug Section
		GridBagConstraints gbc_section0 = new GridBagConstraints();
		gbc_section0.fill = GridBagConstraints.BOTH;
		gbc_section0.gridx = 0;
		gbc_section0.gridy = 0;
		main.add(section0, gbc_section0);

		// Constraints for Network Traffic checkbox
		GridBagConstraints gbc_networkTraffic = new GridBagConstraints();
		gbc_networkTraffic.fill = GridBagConstraints.BOTH;
		gbc_networkTraffic.gridx = 2;
		gbc_networkTraffic.gridy = 0;
		main.add(networkTraffic, gbc_networkTraffic);

		/*
		 * Setup Tab Pane + Layout
		 */
		GridBagLayout gbl_optionPane = new GridBagLayout();
		gbl_optionPane.columnWeights = new double[] { 1.0 };
		gbl_optionPane.rowWeights = new double[] { 0.0, 1.0 };
		optionPane.setLayout(gbl_optionPane);

		GridBagConstraints gbc_main = new GridBagConstraints();
		gbc_main.insets = new Insets(0, 0, 5, 0);
		gbc_main.fill = GridBagConstraints.BOTH;
		gbc_main.gridx = 0;
		gbc_main.gridy = 0;
		optionPane.add(main, gbc_main);

		GridBagConstraints gbc_spacing = new GridBagConstraints();
		gbc_spacing.fill = GridBagConstraints.BOTH;
		gbc_spacing.gridx = 0;
		gbc_spacing.gridy = 1;
		optionPane.add(new JPanel(), gbc_spacing);

		gbc_ribbon_option.fill = GridBagConstraints.BOTH;
		gbc_ribbon_option.gridx = 0;
		gbc_ribbon_option.gridy = 2;
	}

	private void initializeServer() {
		/*
		 * Setup Tab Main Pane + Layout
		 */
		final JPanel main = new JPanel();

		// Players Section
		final int rows0 = 2;
		final JLabel section0 = new JLabel(" - Players - ");
		final JLabel maxPlayersText = new JLabel("Maximum Players:");

		section0.setHorizontalAlignment(SwingConstants.CENTER);
		limitPlayers.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				maxPlayers.setEnabled(limitPlayers.isSelected());
			}
		});
		limitPlayers.setSelected(false);
		maxPlayersText.setHorizontalAlignment(SwingConstants.RIGHT);
		maxPlayers.setToolTipText("Limit how many players are able to connect to your server. Extra clients can still connect but may only spectate.");
		maxPlayers.addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (maxPlayers.isEnabled()) {
					int rotation = e.getWheelRotation();
					int next = (int)maxPlayers.getValue() - rotation;
					maxPlayers.setValue(
						rotation < 0
							? next < 2
								? Integer.MAX_VALUE
								: next
							: next < 2
								? 2
								: next);
				}
			}
		});
		maxPlayers.setEnabled(false);

		// Layout
		GridBagLayout gbl_main = new GridBagLayout();
		gbl_main.columnWeights = new double[] { 0.3, 0.3, 0.4 };
		gbl_main.rowWeights = new double[] { Double.MIN_VALUE, 0.0, 1.0 };
		main.setLayout(gbl_main);

		// Constraints for Players Section
		GridBagConstraints gbc_section0 = new GridBagConstraints();
		gbc_section0.fill = GridBagConstraints.BOTH;
		gbc_section0.gridx = 0;
		gbc_section0.gridy = 0;
		main.add(section0, gbc_section0);

		// Constraints for Limit Players checkbox
		GridBagConstraints gbc_limitPlayers = new GridBagConstraints();
		gbc_limitPlayers.fill = GridBagConstraints.BOTH;
		gbc_limitPlayers.gridx = 2;
		gbc_limitPlayers.gridy = 0;
		main.add(limitPlayers, gbc_limitPlayers);

		// Constraints for Max Players label
		GridBagConstraints gbc_maxPlayersText = new GridBagConstraints();
		gbc_maxPlayersText.fill = GridBagConstraints.BOTH;
		gbc_maxPlayersText.gridx = 1;
		gbc_maxPlayersText.gridy = 1;
		main.add(maxPlayersText, gbc_maxPlayersText);

		// Constraints for Max Players spinner
		GridBagConstraints gbc_maxPlayers = new GridBagConstraints();
		gbc_maxPlayers.fill = GridBagConstraints.BOTH;
		gbc_maxPlayers.gridx = 2;
		gbc_maxPlayers.gridy = 1;
		main.add(maxPlayers, gbc_maxPlayers);

		// Grid Section
		final int rows1 = 2;
		final JLabel section1 = new JLabel(" - Game Grid - ");
		final JLabel gridSizeText = new JLabel("Grid Dimensions:");
		final FlowLayout gridFlow = new FlowLayout();
		final JPanel gridSizePanel = new JPanel();

		section1.setHorizontalAlignment(SwingConstants.CENTER);
		gridSizeText.setHorizontalAlignment(SwingConstants.RIGHT);
		gridFlow.setHgap(0);
		gridFlow.setVgap(0);
		gridSizePanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		gridSizePanel.setLayout(gridFlow);
		gridSizePanel.add(gridWidth);
		gridSizePanel.add(new JLabel(" by "));
		gridSizePanel.add(gridHeight);
		gridWidth.setToolTipText("Width of the grid in dots.");
		gridHeight.setToolTipText("Height of the grid in dots.");

		// Constraints for Grid Section
		GridBagConstraints gbc_section1 = new GridBagConstraints();
		gbc_section1.fill = GridBagConstraints.BOTH;
		gbc_section1.gridx = 0;
		gbc_section1.gridy = rows0;
		main.add(section1, gbc_section1);

		// Constraints for Grid Dimensions label
		GridBagConstraints gbc_gridSizeText = new GridBagConstraints();
		gbc_gridSizeText.fill = GridBagConstraints.BOTH;
		gbc_gridSizeText.gridx = 1;
		gbc_gridSizeText.gridy = rows0;
		main.add(gridSizeText, gbc_gridSizeText);

		// Constraints for Grid Dimensions label
		GridBagConstraints gbc_gridSizePanel = new GridBagConstraints();
		gbc_gridSizePanel.fill = GridBagConstraints.BOTH;
		gbc_gridSizePanel.gridx = 2;
		gbc_gridSizePanel.gridy = rows0;
		main.add(gridSizePanel, gbc_gridSizePanel);

		// Internet Section
		final int rows2 = 2;
		final JLabel section2 = new JLabel(" - Internet - ");
		final JLabel serverNameText = new JLabel("Server Name:");
		final JLabel serverPassText = new JLabel("Server Password:");
		final JTextField serverPass = new JTextField("Not Implemented Yet");

		section2.setHorizontalAlignment(SwingConstants.CENTER);
		advertise.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				serverName.setEnabled(advertise.isSelected());
				//serverPass.setEnabled(advertise.isSelected());
			}
		});
		advertise.setSelected(false);
		serverNameText.setHorizontalAlignment(SwingConstants.RIGHT);
		serverPassText.setHorizontalAlignment(SwingConstants.RIGHT);
		serverName.setToolTipText("Unique string to display in the master server list.");
		serverPass.setEnabled(false); // TODO make a password system

		// Constraints for Internet Section
		GridBagConstraints gbc_section2 = new GridBagConstraints();
		gbc_section2.fill = GridBagConstraints.BOTH;
		gbc_section2.gridx = 0;
		gbc_section2.gridy = rows0 + rows1;
		main.add(section2, gbc_section2);

		// Constraints for Master Server button
		GridBagConstraints gbc_advertise = new GridBagConstraints();
		gbc_advertise.fill = GridBagConstraints.BOTH;
		gbc_advertise.gridx = 2;
		gbc_advertise.gridy = rows0 + rows1;
		main.add(advertise, gbc_advertise);

		// Constraints for Server Name label
		GridBagConstraints gbc_serverNameText = new GridBagConstraints();
		gbc_serverNameText.fill = GridBagConstraints.BOTH;
		gbc_serverNameText.gridx = 1;
		gbc_serverNameText.gridy = rows0 + rows1 + 1;
		main.add(serverNameText, gbc_serverNameText);

		// Constraints for Server Name input
		GridBagConstraints gbc_serverName = new GridBagConstraints();
		gbc_serverName.fill = GridBagConstraints.BOTH;
		gbc_serverName.gridx = 2;
		gbc_serverName.gridy = rows0 + rows1 + 1;
		main.add(serverName, gbc_serverName);

		// Constraints for Server Pass label
		GridBagConstraints gbc_serverPassText = new GridBagConstraints();
		gbc_serverPassText.fill = GridBagConstraints.BOTH;
		gbc_serverPassText.gridx = 1;
		gbc_serverPassText.gridy = rows0 + rows1 + 2;
		main.add(serverPassText, gbc_serverPassText);

		// Constraints for Server Pass input
		GridBagConstraints gbc_serverPass = new GridBagConstraints();
		gbc_serverPass.fill = GridBagConstraints.BOTH;
		gbc_serverPass.gridx = 2;
		gbc_serverPass.gridy = rows0 + rows1 + 2;
		main.add(serverPass, gbc_serverPass);

		/*
		 * Setup Tab Pane + Layout
		 */
		GridBagLayout gbl_serverPane = new GridBagLayout();
		gbl_serverPane.columnWeights = new double[] { 1.0 };
		gbl_serverPane.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		serverPane.setLayout(gbl_serverPane);

		GridBagConstraints gbc_main = new GridBagConstraints();
		gbc_main.insets = new Insets(0, 0, 5, 0);
		gbc_main.fill = GridBagConstraints.BOTH;
		gbc_main.gridx = 0;
		gbc_main.gridy = 0;
		serverPane.add(main, gbc_main);

		GridBagConstraints gbc_spacing = new GridBagConstraints();
		gbc_spacing.fill = GridBagConstraints.BOTH;
		gbc_spacing.gridx = 0;
		gbc_spacing.gridy = 1;
		serverPane.add(new JPanel(), gbc_spacing);

		gbc_ribbon_server.fill = GridBagConstraints.BOTH;
		gbc_ribbon_server.gridx = 0;
		gbc_ribbon_server.gridy = 2;
		serverPane.add(ribbon, gbc_ribbon_server);
	}

	private void returnMenu() {
		end = false;
		if (advertise.isSelected())
			masterServer.send("reset");
		game.clear();
		game = null;
		start.setEnabled(true);
		setContentPane(menuPane);
		//repaint();
	}

	public void advertise() {
		masterServer.send("name " + serverName.getText());
		masterServer.send("max " + (limitPlayers.isSelected() ? (int)maxPlayers.getValue() : 0));
		if (masterServer == null) {
		}
	}

	public boolean getEnd() {
		return end;
	}

	public void network(String message) {
		if (networkTraffic.isSelected())
			System.out.println(message);
	}

	public void setCurrent(Integer current) {
		if (advertise.isSelected())
			masterServer.send("current " + current);
	}

	public void setEnd() {
		System.out.println("Returning to menu.");
		end = true;
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					NetDot frame = new NetDot();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public NetDot() {
		NetDot window = this;
		monitor = new Thread() {
			@Override
			public void run() {
				while (true) {
					try {
						sleep(100);
					}
					catch (Exception e) {
						System.out.println(e);
					}
					if (getEnd())
						returnMenu();
				}
			}
		};
		monitor.start();

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setContentPane(menuPane);
		setBounds(100, 100, windowWidth, windowHeight);

		menuPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Component tab = menuPane.getSelectedComponent();
				start.setText(tab == serverPane ? "Start Server" : "Connect");
				if (tab == serverPane)
					serverPane.add(ribbon, gbc_ribbon_server);
				else if (tab == clientPane)
					clientPane.add(ribbon, gbc_ribbon_client);
				/*else if (tab == optionPane)
					optionPane.add(ribbon, gbc_ribbon_option);*/
			}
		});
		menuPane.addTab("Server", null, serverPane, "For Hosting a Game");
		menuPane.addTab("Client", null, clientPane, "For Connecting to a Server");
		menuPane.addTab("Options", null, optionPane, "Change Settings");

		// Shared Ribbon
		flow.setAlignment(FlowLayout.RIGHT);
		ribbon.setLayout(flow);

		start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				gamePane.setBounds(menuPane.getBounds());
				setContentPane(gamePane);
				//setMinimumSize(new Dimension(minWindowWidth, (int)((float)minWindowHeight * (float)dotHeight / (float)dotWidth)));

				// Server
				if (menuPane.getSelectedComponent() == serverPane) {
					game = new GameManager(window, gamePane, new Dimension((int)gridWidth.getValue(), (int)gridHeight.getValue()), limitPlayers.isSelected() ? (int)maxPlayers.getValue() : 0);
					if (advertise.isSelected())
						advertise();
				}
				// Client
				else {
					// TODO let client pick name and color early - pass as string to manager - store for later, once client joins the game send player-rename and player-color requests
					game = new GameManager(window, gamePane, address.getText());
				}

				game.update();
				repaint();
			}
		});
		ribbon.add(start);

		// Display Area for Game
		gamePane.setBorder(new EmptyBorder(0, 0, 0, 0));
		gamePane.setLayout(null);

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if (game != null)
					game.update();
			}
		});

		masterServer = new Server(masterServerAddress, 4321) {
			public void connected() {
				System.out.println("Connected to master server.");
			}

			public void disconnected() {
				// TODO idk lol
			}

			public void receive(String message) {
				String[] words = message.split(" ");
				if (words.length > 2) {
					InetAddress remoteAddr;
					try {
						remoteAddr = InetAddress.getByName(words[0]);
					}
					catch (Exception e) {
						System.out.println("Could not parse address from master server: " + words[0]);
						System.out.println("Ignoring...");
						return;
					}
					Integer current;
					try {
						current = Integer.parseInt(words[1]);
					}
					catch (Exception e) {
						System.out.println("Could not parse current players value from master server: " + words[1]);
						System.out.println("Ignoring...");
						return;
					}
					Integer max;
					try {
						max = Integer.parseInt(words[2]);
					}
					catch (Exception e) {
						System.out.println("Could not parse max players value from master server: " + words[2]);
						System.out.println("Ignoring...");
						return;
					}
					String name = "";
					if (words.length > 3)
						name = message.substring(words[0].length() + words[1].length() + words[2].length() + 3);
					serverAddresses.add(remoteAddr);
					serverCurrents.add(current);
					serverMaxes.add(max);
					serverNames.add(name);
					model.addElement(name + " | " + (current >= 0 ? current : "?") + "/" + (max > 0 ? max : "∞"));
				}
			}
		};

		masterServer.start();
		initializeServer();
		initializeClient();
		initializeOption();
	}
}
