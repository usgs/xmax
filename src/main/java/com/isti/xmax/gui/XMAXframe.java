package com.isti.xmax.gui;

import com.asl.traceview.transformations.coherence.TransCoherence;
import com.isti.traceview.CommandHandler;
import com.isti.traceview.ExecuteCommand;
import com.isti.traceview.ICommand;
import com.isti.traceview.IUndoableCommand;
import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.commands.OffsetCommand;
import com.isti.traceview.commands.OverlayCommand;
import com.isti.traceview.commands.RemoveGainCommand;
import com.isti.traceview.commands.RotateCommand;
import com.isti.traceview.commands.SaveAllDataCommand;
import com.isti.traceview.commands.SelectCommand;
import com.isti.traceview.commands.SelectTimeCommand;
import com.isti.traceview.commands.SelectValueCommand;
import com.isti.traceview.commands.SetScaleModeCommand;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.filters.FilterBP;
import com.isti.traceview.filters.FilterDYO;
import com.isti.traceview.filters.FilterHP;
import com.isti.traceview.filters.FilterLP;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.gui.ChannelView;
import com.isti.traceview.gui.ColorModeBW;
import com.isti.traceview.gui.ColorModeByGap;
import com.isti.traceview.gui.ColorModeBySegment;
import com.isti.traceview.gui.ColorModeGray;
import com.isti.traceview.gui.FileChooser;
import com.isti.traceview.gui.GraphUtil;
import com.isti.traceview.gui.MeanModeDisabled;
import com.isti.traceview.gui.MeanModeEnabled;
import com.isti.traceview.gui.OffsetModeDisabled;
import com.isti.traceview.gui.ScaleModeAuto;
import com.isti.traceview.gui.ScaleModeCom;
import com.isti.traceview.gui.ScaleModeXhair;
import com.isti.traceview.processing.RemoveGain;
import com.isti.traceview.processing.Rotation;
import com.isti.traceview.transformations.ITransformation;
import com.isti.traceview.transformations.correlation.TransCorrelation;
import com.isti.traceview.transformations.modal.TransModal;
import com.isti.traceview.transformations.ppm.TransPPM;
import com.isti.traceview.transformations.psd.TransPSD;
import com.isti.traceview.transformations.response.TransResp;
import com.isti.traceview.transformations.spectra.TransSpectra;
import com.isti.xmax.XMAX;
import com.isti.xmax.XMAXconfiguration;
import com.isti.xmax.common.Pick;
import com.isti.xmax.data.XMAXChannelFactory;
import com.isti.xmax.data.XMAXDataModule;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.TimeZone;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.MouseInputListener;
import javax.swing.text.MaskFormatter;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;

/**
 * <p>
 * Main frame for XMAX: holds Swing GUI widgets and information about current
 * application state.
 * </p>
 * <p>
 * Realize singleton pattern, i.e we can have only one frame in the program.
 * </p>
 *
 * @author Max Kokoulin
 */
public class XMAXframe extends JFrame implements MouseInputListener, ActionListener, ItemListener, Observer {
	private static final Logger logger = Logger.getLogger(XMAXframe.class); // @jve:decl-index=0:

	private static final long serialVersionUID = 1L;
	private static XMAXframe instance = null;

	private JPanel jContentPane = null;

	private ScalingButtonPanel scalingButtonPanel = null;
	private NavigationButtonPanel navigationButtonPanel = null;
	private FilterButtonPanel filterButtonPanel = null;
	private SelectionButtonPanel selectionButtonPanel = null;
	private AnalysisButtonPanel analysisButtonPanel = null;

	private QCPanel qCPanel = null;
	private PhasePanel phasePanel = null;

	private XMAXGraphPanel graphPanel = null;

	private StatusBar statusBar = null;

	private JMenuBar mainMenuBar = null;

	private JMenu fileMenu = null;

	private JMenuItem saveInternalMenuItem = null;

	private JMenuItem exitMenuItem = null;

	private JMenu viewMenu = null;

	private JMenuItem nextMenuItem = null;

	private JMenuItem previousMenuItem = null;
	private JMenu channelsMenu = null;
	private JPanel buttonPanel = null;
	private GridBagConstraints constraints;
	private JCheckBoxMenuItem showBigCursorMenuCheckBox = null;
	private JCheckBoxMenuItem showStatusBarMenuCheckBox = null;
	private JCheckBoxMenuItem phaseMenuCheckBox = null;
	private JCheckBoxMenuItem meanMenuCheckBox = null;
	private JMenuItem offsetMenuItem = null;
	private JMenuItem rotateMenuItem = null;
	private JMenuItem overlayMenuItem = null;
	private JMenuItem selectMenuItem= null;
	private JCheckBoxMenuItem showBlockHeadersMenuCheckBox = null;

	private ButtonGroup scaleModeBG = null; // @jve:decl-index=0:
	private JRadioButtonMenuItem scaleModeAutoMenuRadioBt = null;
	private JRadioButtonMenuItem scaleModeComMenuRadioBt = null;
	private JRadioButtonMenuItem scaleModeXHairMenuRadioBt = null;

	private JMenu colorMenu;
	private JRadioButtonMenuItem bySegmentMenuRadioBt = null;
	private JRadioButtonMenuItem byGapMenuRadioBt = null;
	private JRadioButtonMenuItem BWMenuRadioBt = null;
	private JRadioButtonMenuItem GrayMenuRadioBt = null;

	private JCheckBoxMenuItem showButtonsMenuCheckBox = null;
	private ButtonGroup showCommandButtonsBG = null; // @jve:decl-index=0:
	private JRadioButtonMenuItem commandButtonTopMenuRadioBt = null;
	private JRadioButtonMenuItem commandButtonBottomMenuRadioBt = null;

	private JMenu filterMenu = null;
	private ButtonGroup filterBG = null;

	private JMenu transformationMenu = null;

	private JMenuItem undoMenuItem = null;

	private JMenu dumpMenu = null;

	private JMenu exportMenu = null;

	private JMenuItem saveXMLmenuItem = null;

	private JMenuItem saveMseedMenuItem = null;

	private JMenuItem saveASCIImenuItem = null;

	private JMenuItem saveSACMenuItem = null;

	private JMenuItem reloadMenuItem = null;
	private JMenuItem loadMenuItem;
	private JMenuItem deleteMenuItem;

	private JMenuItem printMenuItem = null;

	private JMenuItem exportGRAPHmenuItem = null;

	private JMenuItem exportHTMLmenuItem = null;

	private JCheckBoxMenuItem qcMenuCheckBox = null;

	private JMenuItem aboutMenuItem = null;

	private ActionMap actionMap;

	static {
		ToolTipManager.sharedInstance().setInitialDelay(500);
		ToolTipManager.sharedInstance().setDismissDelay(10000);
	}

	/**
	 * This is the default constructor
	 */
	private XMAXframe() {
		super("XMAX version " + XMAX.getVersionMessage());
		logger.debug("Creating XMAXframe");
		setLocation(XMAX.getConfiguration().getFramePos());
		setSize(XMAX.getConfiguration().getFrameSize());
		setMinimumSize(new Dimension(400, 300));
		setExtendedState(XMAX.getConfiguration().getFrameExtendedState());
		// lg.debug("Java version message: " + XMAX.getJavaVersionMessage());
		if ((getExtendedState() == Frame.MAXIMIZED_BOTH || getExtendedState() == Frame.MAXIMIZED_HORIZ
				|| getExtendedState() == Frame.MAXIMIZED_VERT)
				&& (XMAX.getJavaVersionMessage().toLowerCase().contains("linux")
				|| XMAX.getJavaVersionMessage().toLowerCase().contains("sunos"))
				|| XMAX.getJavaVersionMessage().toLowerCase().contains("solaris")) {
			// Manual size setting for Linux and Solaris, setExtendedState
			// doesn't work
			GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] devices = graphicsEnvironment.getScreenDevices();
			GraphicsDevice gd = devices[0];
			if (gd.getDisplayMode() != null) {
				setSize(gd.getDisplayMode().getWidth(), gd.getDisplayMode().getHeight());
			}
		}
		Action action;
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		actionMap = new ActionMap();
		action = new NextAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new PreviousAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new SaveAllAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new OverlayAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new SelectChannelsAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ClearSelectionAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ScaleModeAutoAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ScaleModeComAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ScaleModeXHairAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new RemoveGainAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ShowBigCursorAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new SetColorModeBySegmentAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new SetColorModeByGapAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new SetColorModeBWAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new SetColorModeGrayAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new SwitchColorModeAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new PhasesAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new QCAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new DemeanAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ShowStatusBarAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ShowCommandButtonsAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ShowCommandButtonsTopAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ShowCommandButtonsBottomAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new UndoAction();
		actionMap.put(action.getValue(Action.NAME), action);

		action = new ParticleMotionAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new PowerSpectraDensityAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new CoherenceAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new SpectraAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ModalAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new CorrelationAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new RotationAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ResponseAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new OffsetAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new MarkPickAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new DelPickAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new DumpXMLAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new DumpMSeedAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new DumpSACAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new DumpASCIIAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new LimXAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new LimYAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new PrintAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ExpGRAPHAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ExpHTMLAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ViewHeadersAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ReLoadAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new AboutAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ExitAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new AddDataAction();
		actionMap.put(action.getValue(Action.NAME), action);

		// adding actions for filter plugins
		for (Class<? extends IFilter> curClass : XMAX.getFilters()) {
			try {
				action = new FilterAction((String) curClass.getField("NAME").get(null),
						(String) curClass.getField("NAME").get(null));
				actionMap.put(action.getValue(Action.NAME), action);
			} catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e1) {
				logger.error("Filter Initializing failed");
			}
		}
		initialize();
		CommandHandler.getInstance().addObserver(this);
		addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				graphPanel.xframeMouseEntered(e);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				graphPanel.xframeMouseExited(e);
			}
		});

		addComponentListener(new ComponentListener() {
			@Override
			public void componentHidden(ComponentEvent e) {
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				XMAX.getConfiguration().setFrameState(getExtendedState(), getX(), getY(), getWidth(), getHeight());
			}

			@Override
			public void componentResized(ComponentEvent e) {
				XMAX.getConfiguration().setFrameState(getExtendedState(), getX(), getY(), getWidth(), getHeight());
				graphPanel.forceRepaint(); // re-pixelize and paint data when
				// resizing
			}

			@Override
			public void componentShown(ComponentEvent e) {
			}
		});

		addWindowListener(new WindowListener() {
			@Override
			public void windowActivated(WindowEvent e) {
			}

			@Override
			public void windowClosed(WindowEvent e) {
			}

			@Override
			public void windowClosing(WindowEvent e) {
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
			}

			@Override
			public void windowIconified(WindowEvent e) {
			}

			@Override
			public void windowOpened(WindowEvent e) {
			}
		});
	}

	/**
	 * This method initializes this frame
	 */
	private void initialize() {
		logger.debug("== ENTER");
		this.setContentPane(getJContentPane());
		graphPanel.addObserver(statusBar);
		this.setJMenuBar(getMainMenuBar());
		scaleModeBG = new ButtonGroup();
		scaleModeBG.add(scaleModeAutoMenuRadioBt);
		scaleModeBG.add(scaleModeComMenuRadioBt);
		scaleModeBG.add(scaleModeXHairMenuRadioBt);

		graphPanel.setScaleMode(XMAX.getConfiguration().getScaleMode());
		if (XMAX.getConfiguration().getScaleMode() instanceof ScaleModeAuto) {
			scaleModeAutoMenuRadioBt.setSelected(true);
			// statusBar.setScaleMode("AUTO");
		} else if (XMAX.getConfiguration().getScaleMode() instanceof ScaleModeCom) {
			scaleModeComMenuRadioBt.setSelected(true);
			// statusBar.setScaleMode("COM");
		} else if (XMAX.getConfiguration().getScaleMode() instanceof ScaleModeXhair) {
			scaleModeXHairMenuRadioBt.setSelected(true);
			// statusBar.setScaleMode("XHAIR");
		}

		showButtonsMenuCheckBox.setState(XMAX.getConfiguration().getShowCommandButtons());

		showCommandButtonsBG = new ButtonGroup();
		showCommandButtonsBG.add(commandButtonTopMenuRadioBt);
		showCommandButtonsBG.add(commandButtonBottomMenuRadioBt);

		//initialize command buttons
		commandButtonBottomMenuRadioBt.setSelected(true);
		JPanel navigationPanel = getNavigationButtonPanel();
		navigationPanel.setBorder(BorderFactory.createEmptyBorder());
		JPanel selectPanel = getSelectionButtonPanel();
		selectPanel.setBorder(BorderFactory.createTitledBorder("Misc"));
		JPanel scalingPanel = getScalingButtonPanel();
		scalingPanel.setBorder(BorderFactory.createTitledBorder("Scaling"));
		JPanel analysisPanel = getAnalysisButtonPanel();
		analysisPanel.setBorder(BorderFactory.createTitledBorder("Analysis"));
		JPanel filterPanel = getFilterButtonPanel();
		filterPanel.setBorder(BorderFactory.createTitledBorder("Filter"));
		constraints.gridwidth = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 0;
		buttonPanel.add(navigationPanel, constraints);
		constraints.gridwidth = 1;
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.weightx = 0.2;
		buttonPanel.add(selectPanel, constraints);
		constraints.gridwidth = 1;
		constraints.gridx = 2;
		constraints.gridy = 0;
		constraints.weightx = 0.2;
		buttonPanel.add(analysisPanel, constraints);
		constraints.gridwidth = 1;
		constraints.gridx = 3;
		constraints.gridy = 0;
		constraints.weightx = 0.2;
		buttonPanel.add(filterPanel, constraints);
		constraints.gridwidth = 1;
		constraints.gridx = 4;
		constraints.gridy = 0;
		constraints.weightx = 0.2;
		buttonPanel.add(scalingPanel, constraints);


		if (XMAX.getConfiguration().getShowCommandButtonsTop()) {
			commandButtonTopMenuRadioBt.setSelected(true);
			jContentPane.add(buttonPanel, BorderLayout.NORTH);
		} else {
			commandButtonBottomMenuRadioBt.setSelected(true);
			jContentPane.add(buttonPanel, BorderLayout.SOUTH);

		}

		graphPanel.setShowBigCursor(XMAX.getConfiguration().getShowBigCursor());
		showBigCursorMenuCheckBox.setState(graphPanel.getShowBigCursor());
		graphPanel.setColorMode(XMAX.getConfiguration().getColorModeState());
		if (XMAX.getConfiguration().getColorModeState() instanceof ColorModeBySegment) {
			bySegmentMenuRadioBt.setSelected(true);
		} else if (XMAX.getConfiguration().getColorModeState() instanceof ColorModeByGap) {
			byGapMenuRadioBt.setSelected(true);
		} else if (XMAX.getConfiguration().getColorModeState() instanceof ColorModeBW) {
			BWMenuRadioBt.setSelected(true);
		} else if (XMAX.getConfiguration().getColorModeState() instanceof ColorModeGray){
			GrayMenuRadioBt.setSelected(true);
		}

		if (XMAX.getConfiguration().getShowStatusBar()) {
			statusBar.setVisible(true);
			showStatusBarMenuCheckBox.setState(true);
		} else {
			statusBar.setVisible(false);
			showStatusBarMenuCheckBox.setState(false);
		}

		phaseMenuCheckBox.setState(graphPanel.getPhaseState());
		meanMenuCheckBox.setState(graphPanel.getMeanState() instanceof MeanModeEnabled);
		XMAXDataModule dm = XMAX.getDataModule();
		try {
			graphPanel.setChannelShowSet(dm.getNextChannelSet());
		} catch (TraceViewException e) {
			if (dm.getAllSources().size() > 0) {
				JOptionPane.showMessageDialog(this, "This is the last set", "Information",
						JOptionPane.INFORMATION_MESSAGE);
			} else {
				String message = "No data specified in configuration or command-line on startup.\n"
						+ "Please select the load data option from the menu in order to load data.";
				JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
			}
		}
		statusBar.setChannelCountMessage(dm.getChannelSetStartIndex() + 1, dm.getChannelSetEndIndex(),
				dm.getAllChannels().size());
		logger.debug("== Exit");
	}

	public static synchronized XMAXframe getInstance() {
		if (instance == null) {
			instance = new XMAXframe();
		}
		return instance;
	}

	/*
	 * Methods from MouseInputListener interface
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent event) {
	}

	@Override
	public void mouseMoved(MouseEvent event) {
	}

	@Override
	public void mouseEntered(MouseEvent evt) {
		if (evt.getSource() instanceof AbstractButton) {
			AbstractButton button = (AbstractButton) evt.getSource();
			Action action = button.getAction();
			if (action != null) {
				Object message = action.getValue(Action.LONG_DESCRIPTION);
				statusBar.setMessage(message.toString());
			}
		}
	}

	@Override
	public void mouseExited(MouseEvent evt) {
		if (evt.getSource() instanceof AbstractButton) {
			statusBar.setMessage("");
		}
	}

	/*
	 * Methods from ItemListener interface to handle filters from menu
	 */

	@Override
	public void itemStateChanged(ItemEvent e) {
		String message = null;
		JCheckBoxMenuItem cb = (JCheckBoxMenuItem) e.getItem();
		if (e.getStateChange() == ItemEvent.SELECTED)
			message = "Applying " + cb.getText() + " filter to all visible channels";
		if (e.getStateChange() == ItemEvent.DESELECTED)
			message = "Removing " + cb.getText() + " filter from all visible channels";
		JOptionPane.showMessageDialog(XMAXframe.getInstance(), message, "Action description",
				JOptionPane.INFORMATION_MESSAGE);
		getGraphPanel().forceRepaint();
		statusBar.setMessage("");
	}

	/*
	 * Methods from ActionListener interface to handle transformations from menu
	 */

	@Override
	public void actionPerformed(ActionEvent e) {
		JMenuItem source = (JMenuItem) (e.getSource());
		Action action = actionMap.get(source.getText());
		logger.debug("ACTION NAME: " + action.getValue(Action.NAME));
		action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));

		statusBar.setMessage("");
	}

	// Method from Observer interface
	// Update cursor after notification that all tasks were executed
	@Override
	public void update(Observable o, Object arg) {
		logger.debug("updating frame due to request from " + o.getClass().getName());
		setWaitCursor(false);
	}

	/**
	 * Sets flag if we see waiting cursor
	 */
	private void setWaitCursor(boolean state) {
		if (state) {
			this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		} else {
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
		graphPanel.setWaitCursor(state);
	}

	/**
	 * Sets time range
	 */
	public void setTimeRange(TimeInterval ti) {
		// Create SelectTimeCommand runnable obj
		SelectTimeCommand timeTask = new SelectTimeCommand(graphPanel, ti);

		// Create ExecuteCommand obj for executing runnable
		ExecuteCommand executor = new ExecuteCommand(timeTask);
		executor.initialize();
		executor.start();
		executor.shutdown();
	}

	/**
	 * sets flag if internal graph panel should change time range to show all
	 * loaded data itself
	 */
	public void setShouldManageTimeRange(boolean value) {
		graphPanel.setShouldManageTimeRange(value);
	}

	/**
	 * This method initializes jContentPane
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getbuttonPanel(), BorderLayout.SOUTH);
			jContentPane.add(getGraphPanel(), BorderLayout.CENTER);
		}
		return jContentPane;
	}

	/**
	 * This method initializes GraphPanel
	 *
	 * @return javax.swing.JPanel
	 */
	public XMAXGraphPanel getGraphPanel() {
		if (graphPanel == null) {
			graphPanel = new XMAXGraphPanel();
		}
		return graphPanel;
	}


	/**
	 * This method initializes the ScalingButtonPanel
	 *
	 * @return ScalingButtonPanel
	 */
	private ScalingButtonPanel getScalingButtonPanel() {
		if (scalingButtonPanel == null) {
			scalingButtonPanel = new ScalingButtonPanel();
		}
		return scalingButtonPanel;
	}

	/**
	 * This method initializes the NavigationButtonPanel
	 *
	 * @return NavigationButtonPanel
	 */
	private NavigationButtonPanel getNavigationButtonPanel() {
		if (navigationButtonPanel == null) {
			navigationButtonPanel = new NavigationButtonPanel();
		}
		return navigationButtonPanel;
	}

	/**
	 * This method initializes the FilterButtonPanel
	 *
	 * @return FilterButtonPanel
	 */
	private FilterButtonPanel getFilterButtonPanel() {
		if (filterButtonPanel == null) {
			filterButtonPanel = new FilterButtonPanel();
		}
		return filterButtonPanel;
	}

	/**
	 * This method initializes the FilterButtonPanel
	 *
	 * @return FilterButtonPanel
	 */
	private SelectionButtonPanel getSelectionButtonPanel() {
		if (selectionButtonPanel == null) {
			selectionButtonPanel = new SelectionButtonPanel();
		}
		return selectionButtonPanel;
	}

	/**
	 * This method initializes the AnalysisButtonPanel
	 *
	 * @return AnalysisButtonPanel
	 */
	private AnalysisButtonPanel getAnalysisButtonPanel() {
		if (analysisButtonPanel == null) {
			analysisButtonPanel = new AnalysisButtonPanel();
		}
		return analysisButtonPanel;
	}

	/**
	 * This method initializes QCPanel
	 *
	 * @return com.isti.xmax.gui.QCPanel
	 */
	@SuppressWarnings("unused")
	private JPanel getQCPanel() {
		if (qCPanel == null) {
			qCPanel = new QCPanel();
		}
		return qCPanel;
	}

	/**
	 * This method initializes StatusBar
	 */
	public StatusBar getStatusBar() {
		if (statusBar == null) {
			statusBar = new StatusBar();
		}
		return statusBar;
	}

	/**
	 * This method initializes mainMenuBar
	 *
	 * @return javax.swing.JMenuBar
	 */
	private JMenuBar getMainMenuBar() {
		if (mainMenuBar == null) {
			mainMenuBar = new JMenuBar();
			mainMenuBar.add(getFileMenu());
			mainMenuBar.add(getChannelsMenu());
			mainMenuBar.add(getViewMenu());
		}
		return mainMenuBar;
	}

	/**
	 * This method initializes fileMenu
	 *
	 * @return javax.swing.JMenu
	 */
	private JMenu getFileMenu() {
		if (fileMenu == null) {
			fileMenu = new JMenu();
			fileMenu.setText("File");
			fileMenu.setMnemonic(KeyEvent.VK_F);
			fileMenu.add(getLoadMenuItem());
			fileMenu.add(getReloadMenuItem());
			fileMenu.add(getDeleteMenuItem());
			fileMenu.add(getUndoMenuItem());
			fileMenu.add(getAboutMenuItem());
			fileMenu.add(getPrintMenuItem());
			fileMenu.add(getDumpMenu());
			fileMenu.add(getExportMenu());
			fileMenu.add(getExitMenuItem());
		}
		return fileMenu;
	}

	private JMenuItem getDeleteMenuItem() {
		if (deleteMenuItem == null) {
			deleteMenuItem = new JMenuItem();
			deleteMenuItem.setAction(actionMap.get("Clear selected"));
			deleteMenuItem.addMouseListener(this);
		}
		return deleteMenuItem;
	}

	private JMenuItem getLoadMenuItem() {
		if (loadMenuItem == null) {
			loadMenuItem = new JMenuItem();
			loadMenuItem.setAction(actionMap.get("Add data"));
			loadMenuItem.addMouseListener(this);
		}
		return loadMenuItem;
	}

	/**
	 * This method initializes aboutMenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getAboutMenuItem() {
		if (aboutMenuItem == null) {
			aboutMenuItem = new JMenuItem();
			aboutMenuItem.setAction(actionMap.get("About"));
			aboutMenuItem.addMouseListener(this);
		}
		return aboutMenuItem;
	}

	/**
	 * This method initializes undoMenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getUndoMenuItem() {
		if (undoMenuItem == null) {
			undoMenuItem = new JMenuItem();
			undoMenuItem.setAction(actionMap.get("Undo"));
			undoMenuItem.getAction().setEnabled(false);
			undoMenuItem.addMouseListener(this);
		}
		return undoMenuItem;
	}

	/**
	 * This method initializes exitMenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getExitMenuItem() {
		if (exitMenuItem == null) {
			exitMenuItem = new JMenuItem();
			exitMenuItem.setAction(actionMap.get("Exit"));
			exitMenuItem.addMouseListener(this);
		}
		return exitMenuItem;
	}

	/**
	 * This method initializes dumpMenu
	 *
	 * @return javax.swing.JMenu
	 */
	private JMenu getDumpMenu() {
		if (dumpMenu == null) {
			dumpMenu = new JMenu();
			dumpMenu.setMnemonic(KeyEvent.VK_D);
			dumpMenu.setText("Dump");
			dumpMenu.add(getSaveXMLmenuItem());
			dumpMenu.add(getSaveMseedMenuItem());
			dumpMenu.add(getSaveASCIImenuItem());
			dumpMenu.add(getSaveSACMenuItem());
		}
		return dumpMenu;
	}

	/**
	 * This method initializes exportMenu
	 *
	 * @return javax.swing.JMenu
	 */
	private JMenu getExportMenu() {
		if (exportMenu == null) {
			exportMenu = new JMenu();
			exportMenu.setMnemonic(KeyEvent.VK_E);
			exportMenu.setText("Export");
			exportMenu.add(getExportGRAPHmenuItem());
			exportMenu.add(getExportHTMLmenuItem());
		}
		return exportMenu;
	}

	/**
	 * This method initializes channelsMenu
	 *
	 * @return javax.swing.JMenu
	 */
	private JMenu getChannelsMenu() {
		if (channelsMenu == null) {
			channelsMenu = new JMenu();
			channelsMenu.setText("Channels");
			channelsMenu.setMnemonic(KeyEvent.VK_C);
			channelsMenu.add(getScaleModeAutoMenuRadioBt());
			channelsMenu.add(getScaleModeComMenuRadioBt());
			channelsMenu.add(getScaleModeXHairMenuRadioBt());
			channelsMenu.addSeparator();
			channelsMenu.add(getOverlayMenuItem());
			channelsMenu.add(getSelectMenuItem());
			channelsMenu.add(getMeanMenuCheckBox());
			channelsMenu.add(getOffsetMenuItem());
			channelsMenu.add(getRotateMenuItem());
			channelsMenu.add(getFilterMenu());
			channelsMenu.add(getTransformationMenu());
			channelsMenu.addSeparator();
			channelsMenu.add(getPreviousMenuItem());
			channelsMenu.add(getNextMenuItem());
		}
		return channelsMenu;
	}

	/**
	 * This method initializes filterMenu
	 *
	 * @return javax.swing.JMenu
	 */
	private JMenu getFilterMenu() {
		if (filterMenu == null) {
			filterMenu = new JMenu();
			filterMenu.setText("Filters");
			filterMenu.setMnemonic(KeyEvent.VK_F);
			filterBG = new ButtonGroup();
			for (Class<? extends IFilter> curClass : XMAX.getFilters()) {
				try {
					JRadioButtonMenuItem filterItem = new JRadioButtonMenuItem();
					filterItem.setText((String) curClass.getField("NAME").get(null));
					filterItem.setSelected(false);
					filterItem.setAction(actionMap.get(curClass.getField("NAME").get(null)));
					filterItem.addMouseListener(this);
					filterBG.add(filterItem);
					filterMenu.add(filterItem);
				} catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException
						| SecurityException e) {
					logger.error("Filter failed to initialize");
				}

			}
		}
		return filterMenu;
	}

	/**
	 * This method initializes transformationMenu
	 *
	 * @return javax.swing.JMenu
	 */
	private JMenu getTransformationMenu() {
		if (transformationMenu == null) {
			transformationMenu = new JMenu();
			transformationMenu.setMnemonic(KeyEvent.VK_T);
			transformationMenu.setText("Transformations");
			for (Class<? extends ITransformation> curClass : XMAX.getTransformations()) {
				try {
					JMenuItem transformationItem = new JMenuItem();
					transformationItem.setText((String) curClass.getField("NAME").get(null));
					transformationItem.addActionListener(this);
					transformationMenu.add(transformationItem);
				} catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException
						| SecurityException e) {
					logger.error("Transformation failed to initialize");
				}
			}
		}
		return transformationMenu;
	}

	/**
	 * This method initializes nextMenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getNextMenuItem() {
		if (nextMenuItem == null) {
			nextMenuItem = new JMenuItem();
			nextMenuItem.setAction(actionMap.get("Next"));
			nextMenuItem.addMouseListener(this);
		}
		return nextMenuItem;
	}

	/**
	 * This method initializes previousMenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getPreviousMenuItem() {
		if (previousMenuItem == null) {
			previousMenuItem = new JMenuItem();
			previousMenuItem.setAction(actionMap.get("Previous"));
			previousMenuItem.addMouseListener(this);
		}
		return previousMenuItem;
	}

	/**
	 * This method initializes viewMenu
	 *
	 * @return javax.swing.JMenu
	 */
	private JMenu getViewMenu() {
		if (viewMenu == null) {
			viewMenu = new JMenu();
			viewMenu.setText("View");
			viewMenu.setMnemonic(KeyEvent.VK_V);
			viewMenu.add(getQcMenuCheckBox());
			viewMenu.add(getPhaseMenuCheckBox());
			viewMenu.addSeparator();
			viewMenu.add(getColorMenu());
			viewMenu.add(getShowBlockHeadersMenuCheckBox());
			viewMenu.add(getShowBigCursorMenuCheckBox());
			viewMenu.add(getShowStatusBarMenuCheckBox());
			viewMenu.addSeparator();
			viewMenu.add(getShowButtonsMenuCheckBox());
			viewMenu.add(getCommandButtonTopMenuRadioBt());
			viewMenu.add(getCommandButtonBottomMenuRadioBt());
		}
		return viewMenu;
	}

	/**
	 * This method initializes colorMenu
	 *
	 * @return javax.swing.JMenu
	 */
	private JMenu getColorMenu() {
		if (colorMenu == null) {
			colorMenu = new JMenu();
			colorMenu.setText("Color");
			colorMenu.setMnemonic(KeyEvent.VK_C);
			ButtonGroup colorBG = new ButtonGroup();
			colorBG.add(getBySegmentMenuRadioBt());
			colorMenu.add(getBySegmentMenuRadioBt());
			colorBG.add(getGrayMenuRadioBt());
			colorMenu.add(getGrayMenuRadioBt());
			colorBG.add(getBWMenuRadioBt());
			colorMenu.add(getBWMenuRadioBt());
			colorBG.add(getByGapMenuRadioBt());
			colorMenu.add(getByGapMenuRadioBt());
		}
		return colorMenu;
	}

	/**
	 * This method initializes bySegmentMenuRadioBt
	 *
	 * @return javax.swing.JRadioButtonMenuItem
	 */
	private JRadioButtonMenuItem getBySegmentMenuRadioBt() {
		if (bySegmentMenuRadioBt == null) {
			bySegmentMenuRadioBt = new JRadioButtonMenuItem();
			bySegmentMenuRadioBt.setAction(actionMap.get("Color mode by segment"));
			bySegmentMenuRadioBt.addMouseListener(this);
		}
		return bySegmentMenuRadioBt;
	}

	/**
	 * This method initializes byGapMenuRadioBt
	 *
	 * @return javax.swing.JRadioButtonMenuItem
	 */
	private JRadioButtonMenuItem getByGapMenuRadioBt() {
		if (byGapMenuRadioBt == null) {
			byGapMenuRadioBt = new JRadioButtonMenuItem();
			byGapMenuRadioBt.setAction(actionMap.get("Color mode by gap"));
			byGapMenuRadioBt.addMouseListener(this);
		}
		return byGapMenuRadioBt;
	}

	/**
	 * This method initializes BWMenuRadioBt
	 *
	 * @return javax.swing.JRadioButtonMenuItem
	 */
	private JRadioButtonMenuItem getBWMenuRadioBt() {
		if (BWMenuRadioBt == null) {
			BWMenuRadioBt = new JRadioButtonMenuItem();
			BWMenuRadioBt.setAction(actionMap.get("Color mode BW"));
			BWMenuRadioBt.addMouseListener(this);
		}
		return BWMenuRadioBt;
	}


	/**
	 * This method initializes GrayMenuRadioBt
	 *
	 * @return javax.swing.JRadioButtonMenuItem
	 */
	private JRadioButtonMenuItem getGrayMenuRadioBt() {
		if (GrayMenuRadioBt == null) {
			GrayMenuRadioBt = new JRadioButtonMenuItem();
			GrayMenuRadioBt.setAction(actionMap.get("Color mode gray"));
			GrayMenuRadioBt.addMouseListener(this);
		}
		return GrayMenuRadioBt;
	}

	/**
	 * This method initializes qcMenuCheckBox
	 *
	 * @return javax.swing.JCheckBoxMenuItem
	 */
	private JCheckBoxMenuItem getQcMenuCheckBox() {
		if (qcMenuCheckBox == null) {
			qcMenuCheckBox = new JCheckBoxMenuItem();
			qcMenuCheckBox.setAction(actionMap.get("Quality Control"));
			qcMenuCheckBox.addMouseListener(this);
		}
		return qcMenuCheckBox;
	}

	/**
	 * This method initializes phaseMenuCheckBox
	 *
	 * @return javax.swing.JCheckBoxMenuItem
	 */
	private JCheckBoxMenuItem getPhaseMenuCheckBox() {
		if (phaseMenuCheckBox == null) {
			phaseMenuCheckBox = new JCheckBoxMenuItem();
			phaseMenuCheckBox.setAction(actionMap.get("Phases"));
			phaseMenuCheckBox.addMouseListener(this);
		}
		return phaseMenuCheckBox;
	}

	/**
	 * This method initializes showBigCursorMenuCheckBox
	 *
	 * @return javax.swing.JCheckBoxMenuItem
	 */
	private JCheckBoxMenuItem getShowBigCursorMenuCheckBox() {
		if (showBigCursorMenuCheckBox == null) {
			showBigCursorMenuCheckBox = new JCheckBoxMenuItem();
			showBigCursorMenuCheckBox.setAction(actionMap.get("Show crosshair"));
			showBigCursorMenuCheckBox.addMouseListener(this);
		}
		return showBigCursorMenuCheckBox;
	}

	/**
	 * This method initializes showStatusBarMenuCheckBox
	 *
	 * @return javax.swing.JCheckBoxMenuItem
	 */
	private JCheckBoxMenuItem getShowStatusBarMenuCheckBox() {
		if (showStatusBarMenuCheckBox == null) {
			showStatusBarMenuCheckBox = new JCheckBoxMenuItem();
			showStatusBarMenuCheckBox.setAction(actionMap.get("Show status bar"));
			showStatusBarMenuCheckBox.addMouseListener(this);
		}
		return showStatusBarMenuCheckBox;
	}

	/**
	 * This method initializes showButtonsMenuCheckBox
	 *
	 * @return javax.swing.JCheckBoxMenuItem
	 */
	private JCheckBoxMenuItem getShowButtonsMenuCheckBox() {
		if (showButtonsMenuCheckBox == null) {
			showButtonsMenuCheckBox = new JCheckBoxMenuItem();
			showButtonsMenuCheckBox.setAction(actionMap.get("Show buttons panel"));
			showButtonsMenuCheckBox.addMouseListener(this);
		}
		return showButtonsMenuCheckBox;
	}

	/**
	 * This method initializes commandButtonTopMenuRadioBt
	 *
	 * @return javax.swing.JRadioButtonMenuItem
	 */
	private JRadioButtonMenuItem getCommandButtonTopMenuRadioBt() {
		if (commandButtonTopMenuRadioBt == null) {
			commandButtonTopMenuRadioBt = new JRadioButtonMenuItem();
			commandButtonTopMenuRadioBt.setAction(actionMap.get("Show buttons top"));
			commandButtonTopMenuRadioBt.addMouseListener(this);
		}
		return commandButtonTopMenuRadioBt;
	}

	/**
	 * This method initializes commandButtonBottomMenuRadioBt
	 *
	 * @return javax.swing.JRadioButtonMenuItem
	 */
	private JRadioButtonMenuItem getCommandButtonBottomMenuRadioBt() {
		if (commandButtonBottomMenuRadioBt == null) {
			commandButtonBottomMenuRadioBt = new JRadioButtonMenuItem();
			commandButtonBottomMenuRadioBt.setAction(actionMap.get("Show buttons bottom"));
			commandButtonBottomMenuRadioBt.addMouseListener(this);
		}
		return commandButtonBottomMenuRadioBt;
	}

	/**
	 * This method initializes overlayMenuCheckBox
	 *
	 * @return javax.swing.JCheckBoxMenuItem
	 */
	private JMenuItem getOverlayMenuItem() {
		if (overlayMenuItem == null) {
			overlayMenuItem = new JMenuItem();
			overlayMenuItem.setAction(actionMap.get("Overlay"));
			overlayMenuItem.addMouseListener(this);
		}
		return overlayMenuItem;
	}

	/**
	 * This method initializes selectMenuCheckBox
	 *
	 * @return javax.swing.JCheckBoxMenuItem
	 */
	private JMenuItem getSelectMenuItem() {
		if (selectMenuItem == null) {
			selectMenuItem = new JMenuItem();
			selectMenuItem.setAction(actionMap.get("Select"));
			selectMenuItem.addMouseListener(this);
		}
		return selectMenuItem;
	}

	/**
	 * This method initializes meanMenuCheckBox
	 *
	 * @return javax.swing.JCheckBoxMenuItem
	 */
	private JCheckBoxMenuItem getMeanMenuCheckBox() {
		if (meanMenuCheckBox == null) {
			meanMenuCheckBox = new JCheckBoxMenuItem();
			meanMenuCheckBox.setAction(actionMap.get("Demean"));
			meanMenuCheckBox.addMouseListener(this);
		}
		return meanMenuCheckBox;
	}

	/**
	 * This method initializes offsetMenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getOffsetMenuItem() {
		if (offsetMenuItem == null) {
			offsetMenuItem = new JMenuItem();
			offsetMenuItem.setAction(actionMap.get("Offset Segments"));
			offsetMenuItem.addMouseListener(this);
		}
		return offsetMenuItem;
	}

	/**
	 * This method initializes rotateMenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getRotateMenuItem() {
		if (rotateMenuItem == null) {
			rotateMenuItem = new JMenuItem();
			rotateMenuItem.setAction(actionMap.get("Rotation"));
			rotateMenuItem.addMouseListener(this);
		}
		return rotateMenuItem;
	}

	/**
	 * This method initializes showBlockHeadersMenuCheckBox
	 *
	 * @return javax.swing.JCheckBoxMenuItem
	 */
	private JCheckBoxMenuItem getShowBlockHeadersMenuCheckBox() {
		if (showBlockHeadersMenuCheckBox == null) {
			showBlockHeadersMenuCheckBox = new JCheckBoxMenuItem();
			showBlockHeadersMenuCheckBox.setState(graphPanel.getShowBlockHeader());
			showBlockHeadersMenuCheckBox.setAction(actionMap.get("View headers"));
			showBlockHeadersMenuCheckBox.addMouseListener(this);
		}
		return showBlockHeadersMenuCheckBox;
	}

	/**
	 * This method initializes scaleModeAutoMenuRadioBt
	 *
	 * @return javax.swing.JRadioButtonMenuItem
	 */
	private JRadioButtonMenuItem getScaleModeAutoMenuRadioBt() {
		if (scaleModeAutoMenuRadioBt == null) {
			scaleModeAutoMenuRadioBt = new JRadioButtonMenuItem();
			scaleModeAutoMenuRadioBt.setAction(actionMap.get("Scale auto"));
			scaleModeAutoMenuRadioBt.addMouseListener(this);
		}
		return scaleModeAutoMenuRadioBt;
	}

	/**
	 * This method initializes scaleModeComMenuRadioBt
	 *
	 * @return javax.swing.JRadioButtonMenuItem
	 */
	private JRadioButtonMenuItem getScaleModeComMenuRadioBt() {
		if (scaleModeComMenuRadioBt == null) {
			scaleModeComMenuRadioBt = new JRadioButtonMenuItem();
			scaleModeComMenuRadioBt.setAction(actionMap.get("Scale com"));
			scaleModeComMenuRadioBt.addMouseListener(this);
		}
		return scaleModeComMenuRadioBt;
	}

	/**
	 * This method initializes scaleModeXHairMenuRadioBt
	 *
	 * @return javax.swing.JRadioButtonMenuItem
	 */
	private JRadioButtonMenuItem getScaleModeXHairMenuRadioBt() {
		if (scaleModeXHairMenuRadioBt == null) {
			scaleModeXHairMenuRadioBt = new JRadioButtonMenuItem();
			scaleModeXHairMenuRadioBt.setAction(actionMap.get("Scale Xhair"));
			scaleModeXHairMenuRadioBt.addMouseListener(this);
		}
		return scaleModeXHairMenuRadioBt;
	}

	/**
	 * This method initializes soughtPanel
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getbuttonPanel() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel();
			constraints = new GridBagConstraints();
			GridBagLayout gridbagLayout = new GridBagLayout();
			buttonPanel.setLayout(gridbagLayout);
			constraints.gridx = 0;
			constraints.gridy = 1;
			constraints.gridwidth = 5;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			buttonPanel.add(getStatusBar(), constraints);
		}
		return buttonPanel;
	}

	public void setUndoEnabled(boolean enabled) {
		undoMenuItem.getAction().setEnabled(enabled);
	}

	/**
	 * This method initializes saveXMLmenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getSaveXMLmenuItem() {
		if (saveXMLmenuItem == null) {
			saveXMLmenuItem = new JMenuItem();
			saveXMLmenuItem.setAction(actionMap.get("Dump to XML"));
			saveXMLmenuItem.addMouseListener(this);

		}
		return saveXMLmenuItem;
	}

	/**
	 * This method initializes saveMseedMenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getSaveMseedMenuItem() {
		if (saveMseedMenuItem == null) {
			saveMseedMenuItem = new JMenuItem();
			saveMseedMenuItem.setAction(actionMap.get("Dump to MSEED"));
			saveMseedMenuItem.addMouseListener(this);
		}
		return saveMseedMenuItem;
	}

	/**
	 * This method initializes saveASCIImenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getSaveASCIImenuItem() {
		if (saveASCIImenuItem == null) {
			saveASCIImenuItem = new JMenuItem();
			saveASCIImenuItem.setAction(actionMap.get("Dump to ASCII"));
			saveASCIImenuItem.addMouseListener(this);
		}
		return saveASCIImenuItem;
	}

	/**
	 * This method initializes saveSACMenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getSaveSACMenuItem() {
		if (saveSACMenuItem == null) {
			saveSACMenuItem = new JMenuItem();
			saveSACMenuItem.setAction(actionMap.get("Dump to SAC"));
			saveSACMenuItem.addMouseListener(this);
		}
		return saveSACMenuItem;
	}

	/**
	 * This method initializes saveInternalMenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	@SuppressWarnings("unused")
	private JMenuItem getSaveInternalMenuItem() {
		if (saveInternalMenuItem == null) {
			saveInternalMenuItem = new JMenuItem();
			saveInternalMenuItem.setAction(actionMap.get("Dump all to Internal"));
			saveInternalMenuItem.addMouseListener(this);
		}
		return saveInternalMenuItem;
	}

	/**
	 * This method initializes printMenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getPrintMenuItem() {
		if (printMenuItem == null) {
			printMenuItem = new JMenuItem();
			printMenuItem.setAction(actionMap.get("Print"));
			printMenuItem.addMouseListener(this);
		}
		return printMenuItem;
	}

	/**
	 * This method initializes reloadMenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getReloadMenuItem() {
		if (reloadMenuItem == null) {
			reloadMenuItem = new JMenuItem();
			reloadMenuItem.setAction(actionMap.get("Reload data"));
			reloadMenuItem.addMouseListener(this);
		}
		return reloadMenuItem;
	}

	/**
	 * This method initializes exportGRAPHmenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getExportGRAPHmenuItem() {
		if (exportGRAPHmenuItem == null) {
			exportGRAPHmenuItem = new JMenuItem();
			exportGRAPHmenuItem.setAction(actionMap.get("Export to GRAPH"));
			exportGRAPHmenuItem.addMouseListener(this);
		}
		return exportGRAPHmenuItem;
	}

	/**
	 * This method initializes exportHTMLmenuItem
	 *
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getExportHTMLmenuItem() {
		if (exportHTMLmenuItem == null) {
			exportHTMLmenuItem = new JMenuItem();
			exportHTMLmenuItem.setAction(actionMap.get("Export to HTML"));
			exportHTMLmenuItem.addMouseListener(this);
		}
		return exportHTMLmenuItem;
	}

	public PhasePanel getPhasePanel() {
		return phasePanel;
	}

	/**
	 * This action loads next portion of traces into graph panel
	 */
	class NextAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		NextAction() {
			super();
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
			putValue(Action.NAME, "Next");
			putValue(Action.SHORT_DESCRIPTION, "next");
			putValue(Action.LONG_DESCRIPTION, "Show next set of channels");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				XMAXDataModule dm = XMAX.getDataModule();
				graphPanel.setChannelShowSet(dm.getNextChannelSet()); // and
				// also
				// switch
				// off
				// OVR,
				// SEL,
				// ROT
				graphPanel.setScaleMode(new ScaleModeAuto());
				graphPanel.setMeanState(new MeanModeDisabled());
				graphPanel.setOffsetState(new OffsetModeDisabled());
				graphPanel.setPickState(false);
				graphPanel.setPhaseState(false);
				graphPanel.setFilter(null);
				graphPanel.setManualValueMax(Integer.MIN_VALUE);
				graphPanel.setManualValueMin(Integer.MAX_VALUE);
				statusBar.setChannelCountMessage(dm.getChannelSetStartIndex() + 1, dm.getChannelSetEndIndex(),
						dm.getAllChannels().size());
			} catch (TraceViewException e1) {
				JOptionPane.showMessageDialog(XMAX.getFrame(), "This is the last set", "Information",
						JOptionPane.INFORMATION_MESSAGE);
				getGraphPanel().forceRepaint();
				// logger.error("TraceViewException:", e1); // last set
				// exception not needed
			} catch (Exception e1) {
				logger.error("NextAction error: ", e1);
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	/**
	 * This action loads previous portion of traces into graph panel
	 */
	class PreviousAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		PreviousAction() {
			super();
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK));
			putValue(Action.NAME, "Previous");
			putValue(Action.SHORT_DESCRIPTION, "prev");
			putValue(Action.LONG_DESCRIPTION, "Show previous set of channels");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				XMAXDataModule dm = XMAX.getDataModule();
				if (dm.getChannelSetStartIndex() == 0) {
					JOptionPane.showMessageDialog(XMAX.getFrame(), "This is the first set", "Information",
							JOptionPane.INFORMATION_MESSAGE);
					getGraphPanel().forceRepaint();
					statusBar.setMessage("");
					setWaitCursor(false);
					return;
				}
				graphPanel.setChannelShowSet(dm.getPreviousChannelSet());
				graphPanel.setScaleMode(new ScaleModeAuto());
				graphPanel.setMeanState(new MeanModeDisabled());
				graphPanel.setOffsetState(new OffsetModeDisabled());
				graphPanel.setPickState(false);
				graphPanel.setPhaseState(false);
				graphPanel.setFilter(null);
				graphPanel.setManualValueMax(Integer.MIN_VALUE);
				graphPanel.setManualValueMin(Integer.MAX_VALUE);
				statusBar.setChannelCountMessage(dm.getChannelSetStartIndex() + 1, dm.getChannelSetEndIndex(),
						dm.getAllChannels().size());
			} catch (TraceViewException e1) {
				JOptionPane.showMessageDialog(XMAX.getFrame(), "This is the first set", "Information",
						JOptionPane.INFORMATION_MESSAGE);
				getGraphPanel().forceRepaint();
				// logger.error("TraceViewException:", e1); // first set
				// exception not needed
			} catch (Exception e1) {
				logger.error("PreviousAction error: ", e1);
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class SaveAllAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		SaveAllAction() {
			super();
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
			putValue(Action.NAME, "Dump all to Internal");
			putValue(Action.SHORT_DESCRIPTION, "save all");
			putValue(Action.LONG_DESCRIPTION, "Save all loaded channels into internal file format");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			// Create Runnable SaveAllDataCommand obj
			SaveAllDataCommand saveAllTask = new SaveAllDataCommand();

			// Create ExecuteCommand obj for executing Runnable
			ExecuteCommand executor = new ExecuteCommand(saveAllTask);
			executor.initialize();
			executor.start();
			executor.shutdown();

			statusBar.setMessage("");
			setWaitCursor(false);
		}
	}

	class PhasesAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		PhasesAction() {
			super();
			putValue(Action.NAME, "Phases");
			putValue(Action.SHORT_DESCRIPTION, "phases");
			putValue(Action.LONG_DESCRIPTION, "Show phases on graphs");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				if (phasePanel == null) {
					phasePanel = new PhasePanel(graphPanel);
					add(phasePanel, BorderLayout.EAST);
					phasePanel.setVisible(true);
					graphPanel.setPhaseState(true);
					XMAX.getFrame().validateTree();
				} else {
					phasePanel.setVisible(false);
					remove(phasePanel);
					phasePanel = null;
					graphPanel.setPhaseState(false);
				}
				phaseMenuCheckBox.setState(graphPanel.getPhaseState());
				graphPanel.forceRepaint();
			} catch (Exception e1) {
				logger.error("PhasesAction error: ", e1);
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class QCAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		QCAction() {
			super();
			putValue(Action.NAME, "Quality Control");
			putValue(Action.SHORT_DESCRIPTION, "qc");
			putValue(Action.LONG_DESCRIPTION, "Show quality control panel");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_Q);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				if (qCPanel == null) {
					qCPanel = new QCPanel();
					add(qCPanel, BorderLayout.WEST);
					qCPanel.setVisible(true);
					qcMenuCheckBox.setState(true);
					XMAX.getFrame().validateTree();
				} else {
					qCPanel.setVisible(false);
					remove(qCPanel);
					qCPanel = null;
					qcMenuCheckBox.setState(false);
				}
				graphPanel.forceRepaint();
			} catch (RuntimeException e1) {
				logger.error("QCAction error: ", e1);
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class OverlayAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		OverlayAction() {
			super();
			putValue(Action.NAME, "Overlay");
			putValue(Action.SHORT_DESCRIPTION, "ovr");
			putValue(Action.LONG_DESCRIPTION, "Overlay selected channels");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				setWaitCursor(true);
				// Create Runnable OverlayCommand obj
				OverlayCommand overlayTask = new OverlayCommand(graphPanel);

				// Create ExecuteCommand obj for executing Runnable
				ExecuteCommand executor = new ExecuteCommand(overlayTask);
				executor.initialize();
				executor.start();
				executor.shutdown();
			} finally {
				setWaitCursor(false);
				statusBar.setMessage("");
			}
		}
	}

	class SelectChannelsAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		SelectChannelsAction() {
			super();
			putValue(Action.NAME, "Select");
			putValue(Action.SHORT_DESCRIPTION, "Sel");
			putValue(Action.LONG_DESCRIPTION, "Select channels");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				setWaitCursor(true);
				// Create SelectCommand obj
				SelectCommand selectTask = new SelectCommand(graphPanel);

				// Create executor obj for Runnable
				ExecuteCommand executor = new ExecuteCommand(selectTask);
				executor.initialize();
				executor.start();
				executor.shutdown();
			} finally {
				setWaitCursor(false);
				statusBar.setMessage("");
			}
		}
	}

	/*
	 * Removes all selected channels from the data module
	 */
	class ClearSelectionAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ClearSelectionAction() {
			super();
			putValue(Action.NAME, "Clear selected");
			putValue(Action.SHORT_DESCRIPTION, "clear");
			putValue(Action.LONG_DESCRIPTION, "Unloads all currently selected channels.");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
		}

		@Override
		public void actionPerformed(ActionEvent e) {

			List<PlotDataProvider> channels = graphPanel.getCurrentSelectedChannels();
			if (channels.size() == 0)
				return; // don't do anything if the list is empty
			XMAXDataModule dm = XMAX.getDataModule();
			dm.deleteChannels(channels);
			int panelCount = XMAXconfiguration.getInstance().getUnitsInFrame();

			graphPanel.setChannelShowSet(XMAX.getDataModule().getCurrentChannelSet(panelCount));
			// we will go ahead and reset all the plots as well to prevent weird GUI snags
			graphPanel.setScaleMode(new ScaleModeAuto());
			graphPanel.setMeanState(new MeanModeDisabled());
			graphPanel.setOffsetState(new OffsetModeDisabled());
			graphPanel.setPickState(false);
			graphPanel.setPhaseState(false);
			graphPanel.setFilter(null);
			graphPanel.setManualValueMax(Integer.MIN_VALUE);
			graphPanel.setManualValueMin(Integer.MAX_VALUE);
			graphPanel.repaint();
			// if the dataset is empty, first value will be 1 and second will be zero
			// and in that case we expect the channel count message to be "0-0 of 0"
			int startIndex = Math.min(dm.getChannelSetStartIndex() + 1, dm.getChannelSetEndIndex());
			statusBar.setChannelCountMessage(startIndex, dm.getChannelSetEndIndex(),
					dm.getAllChannels().size());
		}
	}

	class ScaleModeAutoAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ScaleModeAutoAction() {
			super();
			putValue(Action.NAME, "Scale auto");
			putValue(Action.SHORT_DESCRIPTION, "auto");
			putValue(Action.LONG_DESCRIPTION, "switch Scaling Mode to auto");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("Scale auto")) {
				setWaitCursor(true);
				// Create Runnable SetScaleModeCommand obj
				SetScaleModeCommand scaleTask = new SetScaleModeCommand(graphPanel, new ScaleModeAuto());

				// Create ExecuteCommand obj for executing Runnable
				ExecuteCommand executor = new ExecuteCommand(scaleTask);
				executor.initialize();
				executor.start();
				executor.shutdown();

				scaleModeAutoMenuRadioBt.setSelected(true);
				graphPanel.setOffsetState(new OffsetModeDisabled());
			}
			setWaitCursor(false);
			statusBar.setMessage("");
		}
	}

	class ScaleModeComAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ScaleModeComAction() {
			super();
			putValue(Action.NAME, "Scale com");
			putValue(Action.SHORT_DESCRIPTION, "com");
			putValue(Action.LONG_DESCRIPTION, "switch Scaling Mode to com");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("Scale com")) {
				setWaitCursor(true);
				// Create SetScaleModeCommand runnable obj
				SetScaleModeCommand scaleTask = new SetScaleModeCommand(graphPanel, new ScaleModeCom());

				// Create executor obj for Runnable
				ExecuteCommand executor = new ExecuteCommand(scaleTask);
				executor.initialize();
				executor.start();
				executor.shutdown();

				scaleModeComMenuRadioBt.setSelected(true);
				graphPanel.setOffsetState(new OffsetModeDisabled());
			}
			setWaitCursor(false);
			statusBar.setMessage("");
		}
	}

	class ScaleModeXHairAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ScaleModeXHairAction() {
			super();
			putValue(Action.NAME, "Scale Xhair");
			putValue(Action.SHORT_DESCRIPTION, "Crosshair");
			putValue(Action.LONG_DESCRIPTION, "Switch scaling mode to crosshair");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_X);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("Scale Xhair")) {
				setWaitCursor(true);
				// Create SetScaleModeCommand obj
				SetScaleModeCommand scaleTask = new SetScaleModeCommand(graphPanel, new ScaleModeXhair());

				// Create ExecuteCommand obj for executing Runnable
				ExecuteCommand executor = new ExecuteCommand(scaleTask);
				executor.initialize();
				executor.start();
				executor.shutdown();

				scaleModeXHairMenuRadioBt.setSelected(true);
				graphPanel.setOffsetState(new OffsetModeDisabled());
			}
			setWaitCursor(false);
			statusBar.setMessage("");
		}
	}

	class RemoveGainAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		RemoveGainAction() {
			super();
			putValue(Action.NAME, "Remove gain");
			putValue(Action.SHORT_DESCRIPTION, "RG");
			putValue(Action.LONG_DESCRIPTION, "Removes gain from a trace by division.");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				if (e.getActionCommand().equals("Remove gain")) {
					// Create Runnable RemoveGainCommand obj
					RemoveGainCommand removeGainTask = new RemoveGainCommand(graphPanel,
							new RemoveGain(!graphPanel.getRemoveGain().removestate));

					// Create ExecuteCommand obj for executing Runnable
					ExecuteCommand executor = new ExecuteCommand(removeGainTask);
					executor.initialize();
					executor.start();
					executor.shutdown();
				}
			} finally {
				if (graphPanel.getRemoveGain().removestate)
					statusBar.setMessage("Removed Gain");
				else
					statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class ShowBigCursorAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ShowBigCursorAction() {
			super();
			putValue(Action.NAME, "Show crosshair");
			putValue(Action.SHORT_DESCRIPTION, "Show crosshair");
			putValue(Action.LONG_DESCRIPTION, "Show big cross hair cursor");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			graphPanel.setShowBigCursor(showBigCursorMenuCheckBox.getState());
			statusBar.setMessage("");
		}
	}

	class SetColorModeBySegmentAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		SetColorModeBySegmentAction() {
			super();
			putValue(Action.NAME, "Color mode by segment");
			putValue(Action.SHORT_DESCRIPTION, "by segment");
			putValue(Action.LONG_DESCRIPTION, "Segments in the trace separated by color");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			graphPanel.setColorMode(new ColorModeBySegment());
			bySegmentMenuRadioBt.setSelected(true);
			statusBar.setMessage("");
		}
	}

	class SetColorModeBWAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		SetColorModeBWAction() {
			super();
			putValue(Action.NAME, "Color mode BW");
			putValue(Action.SHORT_DESCRIPTION, "BW");
			putValue(Action.LONG_DESCRIPTION, "All traces drawn in black and white");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			graphPanel.setColorMode(new ColorModeBW());
			BWMenuRadioBt.setSelected(true);
			statusBar.setMessage("");
		}
	}

	class SetColorModeGrayAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		SetColorModeGrayAction() {
			super();
			putValue(Action.NAME, "Color mode gray");
			putValue(Action.SHORT_DESCRIPTION, "Gray");
			putValue(Action.LONG_DESCRIPTION, "All traces drawn in gray");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			graphPanel.setColorMode(new ColorModeGray());
			GrayMenuRadioBt.setSelected(true);
			statusBar.setMessage("");
		}
	}

	class SetColorModeByGapAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		SetColorModeByGapAction() {
			super();
			putValue(Action.NAME, "Color mode by gap");
			putValue(Action.SHORT_DESCRIPTION, "by gap");
			putValue(Action.LONG_DESCRIPTION, "Gaps in the trace separated by color");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_G);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			graphPanel.setColorMode(new ColorModeByGap());
			byGapMenuRadioBt.setSelected(true);
			statusBar.setMessage("");
		}
	}

	// Used in screen toggle button
	class SwitchColorModeAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		SwitchColorModeAction() {
			super();
			putValue(Action.NAME, "Color mode");
			putValue(Action.SHORT_DESCRIPTION, "Switch color");
			putValue(Action.LONG_DESCRIPTION, "Switch color mode in rotating manner: Gray - BW - By segment - By gap");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (graphPanel.getColorMode() instanceof ColorModeBySegment) {
				graphPanel.setColorMode(new ColorModeGray());
				GrayMenuRadioBt.setSelected(true);
				statusBar.setMessage("Color mode gray");
			} else if (graphPanel.getColorMode() instanceof ColorModeGray) {
				graphPanel.setColorMode(new ColorModeBW());
				BWMenuRadioBt.setSelected(true);
				statusBar.setMessage("Color mode by black & white");
			} else if (graphPanel.getColorMode() instanceof ColorModeBW) {
				graphPanel.setColorMode(new ColorModeByGap());
				byGapMenuRadioBt.setSelected(true);
				statusBar.setMessage("Color mode by gap");
			} else if (graphPanel.getColorMode() instanceof ColorModeByGap) {
				graphPanel.setColorMode(new ColorModeBySegment());
				bySegmentMenuRadioBt.setSelected(true);
				statusBar.setMessage("Color mode by segment");
			}

		}
	}

	class DemeanAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		DemeanAction() {
			super();
			putValue(Action.NAME, "Demean");
			putValue(Action.SHORT_DESCRIPTION, "mean");
			putValue(Action.LONG_DESCRIPTION, "Shift mean trace value to 0");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_M);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (graphPanel.getMeanState() instanceof MeanModeDisabled) {
				graphPanel.setMeanState(new MeanModeEnabled());

			} else if (graphPanel.getMeanState() instanceof MeanModeEnabled) {
				graphPanel.setMeanState(new MeanModeDisabled());
			}
			meanMenuCheckBox.setState(graphPanel.getMeanState() instanceof MeanModeEnabled);
			statusBar.setMessage("");
		}
	}

	class ShowStatusBarAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ShowStatusBarAction() {
			super();
			putValue(Action.NAME, "Show status bar");
			putValue(Action.SHORT_DESCRIPTION, "Show status bar");
			putValue(Action.LONG_DESCRIPTION, "Toggles status bar on/off");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			statusBar.setVisible(showStatusBarMenuCheckBox.getState());
			graphPanel.repaint();
			statusBar.setMessage("");
		}
	}

	class ShowCommandButtonsAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ShowCommandButtonsAction() {
			super();
			putValue(Action.NAME, "Show buttons panel");
			putValue(Action.SHORT_DESCRIPTION, "Show command buttons panel");
			putValue(Action.LONG_DESCRIPTION, "Toggles command buttons panel on/off");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			buttonPanel.setVisible(showButtonsMenuCheckBox.getState());
			graphPanel.repaint();
			statusBar.setMessage("");
		}
	}

	class ShowCommandButtonsTopAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ShowCommandButtonsTopAction() {
			super();
			putValue(Action.NAME, "Show buttons top");
			putValue(Action.SHORT_DESCRIPTION, "Show command buttons top");
			putValue(Action.LONG_DESCRIPTION, "Show command buttons panel at the top of window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_T);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("Show buttons top")) {
				commandButtonTopMenuRadioBt.setSelected(true);
				jContentPane.add(buttonPanel, BorderLayout.NORTH);
				graphPanel.repaint();
			}
			statusBar.setMessage("");
		}
	}

	class ShowCommandButtonsBottomAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ShowCommandButtonsBottomAction() {
			super();
			putValue(Action.NAME, "Show buttons bottom");
			putValue(Action.SHORT_DESCRIPTION, "Show command buttons bottom");
			putValue(Action.LONG_DESCRIPTION, "Show command buttons panel at the bottom of window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("Show buttons bottom")) {
				commandButtonBottomMenuRadioBt.setSelected(true);
				jContentPane.add(buttonPanel, BorderLayout.SOUTH);
				graphPanel.repaint();
			}
			statusBar.setMessage("");
		}
	}

	class UndoAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public UndoAction() {
			super();
			putValue(Action.NAME, "Undo");
			putValue(Action.SHORT_DESCRIPTION, "undo");
			putValue(Action.LONG_DESCRIPTION, "Undo last operation");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			LinkedList<ICommand> history = CommandHandler.getInstance().getCommandHistory();
			if (!history.isEmpty()) {
				ICommand command = history.getLast();
				if (command instanceof IUndoableCommand) {
					IUndoableCommand undCommand = (IUndoableCommand) command;
					if (undCommand.canUndo()) {
						undCommand.undo();
						history.removeLast();
					}
				}
				statusBar.setMessage("");
			}
		}
	}

	class ParticleMotionAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ParticleMotionAction() {
			super();
			putValue(Action.NAME, TransPPM.NAME);
			putValue(Action.SHORT_DESCRIPTION, "PPM");
			putValue(Action.LONG_DESCRIPTION, "Show Particle Motion window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_M);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				ITransformation resp = new TransPPM();
				List<PlotDataProvider> selectedChannels = new ArrayList<>();
				List<ChannelView> selectedViews = graphPanel.getCurrentSelectedChannelShowSet();

				for (ChannelView channelView : selectedViews) {
					selectedChannels.addAll(channelView.getPlotDataProviders());
				}
				// if selected nothing
				if (selectedChannels.size() == 0) {
					// Try to fill default channels
					List<PlotDataProvider> channelSet = graphPanel.getChannelSet();
					// Station station = null;
					PlotDataProvider Nchannel = null;
					PlotDataProvider Echannel = null;
					for (PlotDataProvider channel : channelSet) {
						if (channel.getType() == 'N') {
							if (Nchannel == null) {
								Nchannel = channel;
							} else {
								break;
							}
						} else if (channel.getType() == 'E') {
							if (Echannel == null) {
								Echannel = channel;
							} else {
								break;
							}
						}
					}
					if (Nchannel != null && Echannel != null && Nchannel.getStation().equals(Echannel.getStation())) {
						selectedChannels.add(Nchannel);
						selectedChannels.add(Echannel);
					}
				}
				resp.transform(selectedChannels, graphPanel.getTimeRange(), graphPanel.getFilter(),
						null, getInstance());
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class PowerSpectraDensityAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		PowerSpectraDensityAction() {
			super();
			putValue(Action.NAME, TransPSD.NAME);
			putValue(Action.SHORT_DESCRIPTION, "PSD");
			putValue(Action.LONG_DESCRIPTION, "Show Power Spectra Density window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {

				ITransformation resp = new TransPSD();

				List<PlotDataProvider> selectedChannels = new ArrayList<>();

				List<ChannelView> selectedViews = graphPanel.getCurrentSelectedChannelShowSet();
				// because we can do this action on arbitrarily many traces, we will
				// use the current selection if no channels are actively selected
				if (selectedViews.size() == 0) {
					selectedViews = graphPanel.getCurrentChannelShowSet();
				}

				for (ChannelView channelView : selectedViews) {
					selectedChannels.addAll(channelView.getPlotDataProviders());
				}
				boolean hasRotatedData = false;
				for(PlotDataProvider pdp : selectedChannels) {
					if(pdp.isRotated()) {
						hasRotatedData = true;
						break;
					}
				}
				if(hasRotatedData) {
					JOptionPane.showMessageDialog(TraceView.getFrame(),
							"One or more of the traces you have selected contains rotated data.\n"
									+ "The rotated data will be plotted using their original responses.\n"
									+ "To use the original traces, un-rotate the data before calculating the PSD.",
							"PSD will be computed on rotated data", JOptionPane.INFORMATION_MESSAGE);
				}
				org.apache.commons.configuration.Configuration pluginConf = XMAXconfiguration.getInstance()
						.getConfigurationAt("Configuration.Plugins.PSD");
				resp.transform(selectedChannels, graphPanel.getTimeRange(), graphPanel.getFilter(), pluginConf,
						getInstance());
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class CoherenceAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		CoherenceAction() {
			super();
			putValue(Action.NAME, TransCoherence.NAME);
			putValue(Action.SHORT_DESCRIPTION, "Coherence");
			putValue(Action.LONG_DESCRIPTION, "Show Coherence window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {

				ITransformation resp = new TransCoherence();

				List<PlotDataProvider> selectedChannels = new ArrayList<>();
				List<ChannelView> selectedViews = graphPanel.getCurrentSelectedChannelShowSet();
				for (ChannelView channelView : selectedViews) {
					selectedChannels.addAll(channelView.getPlotDataProviders());
				}
				resp.transform(selectedChannels, graphPanel.getTimeRange(), graphPanel.getFilter(), null,
						getInstance());
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class AddDataAction extends AbstractAction implements Action {

		AddDataAction() {
			super();
			putValue(Action.NAME, "Add data");
			putValue(Action.SHORT_DESCRIPTION, "Add more data");
			putValue(Action.LONG_DESCRIPTION, "Load in additional data files");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		}

		void queryFDSN() {
			JTextField networkField = new JTextField();
			networkField.setMinimumSize(networkField.getPreferredSize());
			JTextField stationField = new JTextField();
			stationField.setMinimumSize(stationField.getPreferredSize());
			JTextField locationField = new JTextField();
			locationField.setMinimumSize(locationField.getPreferredSize());
			JTextField channelField = new JTextField();
			channelField.setMinimumSize(channelField.getPreferredSize());

			Date start = null;
			Date end = null;
			// set initial start and end times to be 2 days ago and 1 day ago at day start
			// such that there should be data for the full day's length that already exists
			Date defaultStartValue = Date.from(
					LocalDate.now().minusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant());
			Date defaultEndValue = Date.from(
					LocalDate.now().minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());

			JSpinner startPicker = timePickerFactory(start, end);
			JSpinner endPicker = timePickerFactory(start, end);

			SimpleDateFormat format = ((JSpinner.DateEditor) startPicker.getEditor()).getFormat();
			format.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
			format = ((JSpinner.DateEditor) endPicker.getEditor()).getFormat();
			format.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));

			startPicker.setValue(defaultStartValue);
			endPicker.setValue(defaultEndValue);

			JPanel queryPanel = new JPanel();
			queryPanel.setLayout(new GridLayout(6, 2));
			queryPanel.add(new JLabel("Network: (ex: IU)"));
			queryPanel.add(networkField);
			queryPanel.add(new JLabel("Station: (ex: ANMO)"));
			queryPanel.add(stationField);
			queryPanel.add(new JLabel("Location: (ex: 00)"));
			queryPanel.add(locationField);
			queryPanel.add(new JLabel("Channel: (ex: LHZ)"));
			queryPanel.add(channelField);
			queryPanel.add(new JLabel("Start time (UTC):"));
			queryPanel.add(startPicker);
			queryPanel.add(new JLabel("End time (UTC):"));
			queryPanel.add(endPicker);

			int result = JOptionPane.showConfirmDialog(XMAX.getFrame(), queryPanel,
					"Set FDSN query parameters", JOptionPane.OK_CANCEL_OPTION);

			if (result == JOptionPane.OK_OPTION) {
				String net = networkField.getText().toUpperCase();
				String sta = stationField.getText().replaceAll("\\s", "").toUpperCase();
				String loc = locationField.getText().replaceAll("\\s", "").toUpperCase();
				String cha = channelField.getText().toUpperCase();

				Date startDate = (Date) startPicker.getValue();
				long startMillis = startDate.toInstant().toEpochMilli();
				Date endDate = (Date) endPicker.getValue();
				long endMillis = endDate.toInstant().toEpochMilli();

				Thread load = new Thread(() -> {
					XMAXDataModule dm = XMAXDataModule.getInstance();
					dm.loadNewDataFromSocket(net, sta, loc, cha, startMillis,
							endMillis);
				});
				load.start();
				try {
					load.join();
					resetPlottedData();
				} catch (InterruptedException e) {
					logger.error(e);
				}
			}
		}

		void resetPlottedData() {
			try {
				XMAXDataModule dm = XMAXDataModule.getInstance();
				XMAX.getFrame().getGraphPanel().removeAll();
				dm.reLoadData();
				try {
					graphPanel.setChannelShowSet(dm.getNextChannelSet());
				} catch (TraceViewException ex) {
					JOptionPane.showMessageDialog(XMAX.getFrame(), ex.getMessage(), "Information",
							JOptionPane.INFORMATION_MESSAGE);
					getGraphPanel().forceRepaint();
				}

				statusBar .setChannelCountMessage(
						dm.getChannelSetStartIndex() + 1, dm.getChannelSetEndIndex(),
						dm.getAllChannels().size());
			} catch (TraceViewException e1) {
				logger.error(e1);
			}
		}

		JSpinner timePickerFactory(Date start, Date end) {
			String formatterPattern = "yyyy.DDD | HH:mm:ss.SSS";
			JSpinner.DateEditor timeEditor;
			SpinnerDateModel startModel = new SpinnerDateModel();
			startModel.setStart(start);
			startModel.setEnd(end);
			JSpinner timePicker = new JSpinner(startModel);
			timeEditor = new JSpinner.DateEditor(timePicker, formatterPattern);
			timePicker.setEditor(timeEditor);
			return timePicker;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			{
				JRadioButton local = new JRadioButton("Load from local file");
				JRadioButton fdsn = new JRadioButton("Load from FDSN");
				ButtonGroup group = new ButtonGroup();
				group.add(local);
				group.add(fdsn);
				local.setSelected(true);

				JPanel panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
				panel.add(local);
				panel.add(fdsn);

				int result = JOptionPane.showConfirmDialog(XMAXframe.getInstance(), panel,
						"Select load method", JOptionPane.OK_CANCEL_OPTION);

				if (result == JOptionPane.CANCEL_OPTION) {
					return;
				}

				if (fdsn.isSelected()) {
					queryFDSN();
					return;
				}
			}


			JFileChooser jfc = new JFileChooser();
			jfc.setMultiSelectionEnabled(true);
			if (XMAXconfiguration.getInstance().getDataPath() != null) {
				jfc.setCurrentDirectory(new File(XMAXconfiguration.getInstance().getDataPath()));
			}
			int selectionState = jfc.showOpenDialog(XMAXframe.getInstance());
			if (selectionState == JFileChooser.APPROVE_OPTION) {
				File[] files = jfc.getSelectedFiles();
				File[] errors = XMAXDataModule.getInstance().loadNewDataFromSources(files);

				if (errors.length > 0) {
					StringBuilder erroringFnames = new StringBuilder("Could not load the following files:\n");
					for (File error : errors) {
						erroringFnames.append(error.getName() + "\n");
					}
					logger.error(erroringFnames.toString());
					JOptionPane.showMessageDialog(XMAXframe.getInstance(), erroringFnames.toString(),
							"Could not load in all data!", JOptionPane.ERROR_MESSAGE);
				}

				resetPlottedData();
			}
		}
	}

	class SpectraAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		SpectraAction() {
			super();
			putValue(Action.NAME, TransSpectra.NAME);
			putValue(Action.SHORT_DESCRIPTION, "SPECTRA");
			putValue(Action.LONG_DESCRIPTION, "Show Spectra window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				ITransformation resp = new TransSpectra();
				List<PlotDataProvider> selectedChannels = new ArrayList<>();
				List<ChannelView> selectedViews = graphPanel.getCurrentSelectedChannelShowSet();
				// because we can do this action on arbitrarily many traces, we will
				// use the current selection if no channels are actively selected
				if (selectedViews.size() == 0) {
					selectedViews = graphPanel.getCurrentChannelShowSet();
				}
				for (ChannelView channelView : selectedViews) {
					selectedChannels.addAll(channelView.getPlotDataProviders());
				}
				resp.transform(selectedChannels, graphPanel.getTimeRange(), graphPanel.getFilter(), null,
						getInstance());
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class CorrelationAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		CorrelationAction() {
			super();
			putValue(Action.NAME, TransCorrelation.NAME);
			putValue(Action.SHORT_DESCRIPTION, "CORR");
			putValue(Action.LONG_DESCRIPTION, "Show 2 traces correlation");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				ITransformation resp = new TransCorrelation();
				List<PlotDataProvider> selectedChannels = new ArrayList<>();
				List<ChannelView> selectedViews = graphPanel.getCurrentSelectedChannelShowSet();
				for (ChannelView channelView : selectedViews) {
					selectedChannels.addAll(channelView.getPlotDataProviders());
				}
				resp.transform(selectedChannels, graphPanel.getTimeRange(), graphPanel.getFilter(), null,
						getInstance());
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class ModalAction extends AbstractAction implements Action {

		ModalAction() {
			super();
			putValue(Action.NAME, TransModal.NAME);
			putValue(Action.SHORT_DESCRIPTION, "N.MODES");
			putValue(Action.LONG_DESCRIPTION, "Show normal mode overlays");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				ITransformation psd = new TransModal();
				List<PlotDataProvider> selectedChannels = new ArrayList<>();
				List<ChannelView> selectedViews = graphPanel.getCurrentSelectedChannelShowSet();
				// because we can do this action on arbitrarily many traces, we will
				// use the current selection if no channels are actively selected
				if (selectedViews.size() == 0) {
					selectedViews = graphPanel.getCurrentChannelShowSet();
				}
				for (ChannelView channelView : selectedViews) {
					selectedChannels.addAll(channelView.getPlotDataProviders());
				}
				psd.transform(selectedChannels, graphPanel.getTimeRange(), graphPanel.getFilter(), null,
						getInstance());
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class RotationAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;
		private final List<PlotDataProvider> rotatedChannelsList = new ArrayList<>();

		RotationAction() {
			super();
			putValue(Action.NAME, "Rotation");
			putValue(Action.SHORT_DESCRIPTION, "ROT");
			putValue(Action.LONG_DESCRIPTION, "Rotates channels");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				List<PlotDataProvider> pdpsToRotate = new ArrayList<>();
				List<ChannelView> selectedViews = graphPanel.getCurrentSelectedChannelShowSet();
				for(ChannelView cv : selectedViews){
					for(PlotDataProvider pdp : cv.getPlotDataProviders())
					{
						if (rotatedChannelsList.contains(pdp)) {
							if(pdp.getRotation() == null || pdp.getRotation().getRotationType() == null) { //for case when the close button was clicked
								pdpsToRotate.add(pdp);
							} else {
								//Undo a already rotated channel
								rotatedChannelsList.remove(pdp); //remove from rotated list if trying to rotate an already rotated channel
								pdpsToRotate.add(pdp);
								RotateCommand rotateTask = new RotateCommand(pdpsToRotate, graphPanel, null);
								// Create ExecuteCommand obj for executing Runnable
								ExecuteCommand executor = new ExecuteCommand(rotateTask);
								executor.initialize();
								executor.start();
								executor.shutdown();
								pdpsToRotate.clear();
							}
						} else {
							pdpsToRotate.add(pdp);
						}
					}
				}
				if (pdpsToRotate.size() > 0) {
					if (pdpsToRotate.size() == 2) {
						if (Rotation.isComplementaryChannel(pdpsToRotate.get(0), pdpsToRotate.get(1))) {
							rotatedChannelsList.addAll(pdpsToRotate);
							PlotDataProvider pdp = pdpsToRotate.get(0);
							Date timeToGetAzimuth = pdp.getTimeRange().getStartTime();
							double initialRotation = 0.;
							try {
								Double azi =
										pdpsToRotate.get(0).getResponse().getEnclosingEpochAzimuth(timeToGetAzimuth);
								if (azi != null) {
									initialRotation = azi;
								}
							} catch (TraceViewException e1) {
								logger.warn("Error getting azimuth for selected data: ", e1);
							}
							RotateCommand rotateTask = new RotateCommand(pdpsToRotate, graphPanel,
									new Rotation(XMAX.getFrame(), 2, initialRotation));
							// Create ExecuteCommand obj for executing Runnable
							ExecuteCommand executor = new ExecuteCommand(rotateTask);
							executor.initialize();
							executor.start();
							executor.shutdown();
						} else {
							SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(TraceView.getFrame(),
									"The selected channels are not complementary.",
									"Invalid channels selected to rotate", JOptionPane.WARNING_MESSAGE));
						}
					} else if (pdpsToRotate.size() == 3) {
						if (Rotation.isComplementaryChannel(pdpsToRotate.get(0), pdpsToRotate.get(1),
								pdpsToRotate.get(2))) {
							rotatedChannelsList.addAll(pdpsToRotate);
							RotateCommand rotateTask = new RotateCommand(pdpsToRotate, graphPanel,
									new Rotation(XMAX.getFrame(), 3));
							// Create ExecuteCommand obj for executing Runnable
							ExecuteCommand executor = new ExecuteCommand(rotateTask);
							executor.initialize();
							executor.start();
							executor.shutdown();
						} else {
							SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(TraceView.getFrame(),
									"The selected channels are not complementary.",
									"Invalid channels selected to rotate", JOptionPane.WARNING_MESSAGE));
						}
					} else {
						SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(TraceView.getFrame(),
								"You have selected an invalid number of channels.\n"
										+ "Please select only  two channels for horizontal rotation,"
										+ "or three channels for vertical rotation.\n"
										+ "Each rotation action must be done separately.",
								"Invalid channels selected to rotate", JOptionPane.WARNING_MESSAGE));
					}
				} else {
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(TraceView.getFrame(),
							"Please click check-boxes for the complementary "
									+ "channels that you wish to rotate",
							"Invalid Selection", JOptionPane.WARNING_MESSAGE));
				}
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}


		/**
		 * Returns the rotated channel list
		 */
		public List<PlotDataProvider> getRotatedChannelList() {
			return rotatedChannelsList;
		}

		/**
		 * Adds a rotated channel to the rotatedChannelsList
		 */
		public void addRotatedChannel(PlotDataProvider pdp) {
			rotatedChannelsList.add(pdp);
		}

		/**
		 * Removes a channel from the rotatedChannelsList
		 */
		public void removeRotatedChannel(PlotDataProvider pdp) {
			rotatedChannelsList.remove(pdp);
		}
	}

	class ResponseAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ResponseAction() {
			super();
			putValue(Action.NAME, TransResp.NAME);
			putValue(Action.SHORT_DESCRIPTION, "RESP");
			putValue(Action.LONG_DESCRIPTION, "Show Response window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				ITransformation resp = new TransResp();
				List<PlotDataProvider> selectedChannels = new ArrayList<>();
				List<ChannelView> selectedViews = graphPanel.getCurrentSelectedChannelShowSet();
				// because we can do this action on arbitrarily many traces, we will
				// use the current selection if no channels are actively selected
				if (selectedViews.size() == 0) {
					selectedViews = graphPanel.getCurrentChannelShowSet();
				}
				for (ChannelView channelView : selectedViews) {
					selectedChannels.addAll(channelView.getPlotDataProviders());
				}
				resp.transform(selectedChannels, graphPanel.getTimeRange(), null, null, getInstance());
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class OffsetAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		OffsetAction() {
			super();
			putValue(Action.NAME, "Offset Segments");
			putValue(Action.SHORT_DESCRIPTION, "offset");
			putValue(Action.LONG_DESCRIPTION, "Show segments with offset so gaps could be seen clearer");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			OffsetCommand offsetCmd = new OffsetCommand(graphPanel);
			ExecuteCommand executor = new ExecuteCommand(offsetCmd);
			executor.initialize();
			executor.start();
			executor.shutdown();
			graphPanel.repaint(); // forceRepaint()? Check offsets
			statusBar.setMessage("");
		}
	}

	class MarkPickAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		MarkPickAction() {
			super();
			putValue(Action.NAME, "Mark pick");
			putValue(Action.SHORT_DESCRIPTION, "ttpick");
			putValue(Action.LONG_DESCRIPTION, "Mark travel time pick");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if (graphPanel.getPickState()) {
					graphPanel.setPickState(false);
				} else {
					graphPanel.setPickState(true);
				}
			} finally {
				statusBar.setMessage("");
			}
		}
	}

	class DelPickAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		DelPickAction() {
			super();
			putValue(Action.NAME, "Delete pick");
			putValue(Action.SHORT_DESCRIPTION, "delpick");
			putValue(Action.LONG_DESCRIPTION, "Delete travel time pick");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Pick.geleteLastPick();
			graphPanel.repaint(); // may need to override graph when deleting
			// (force?)
			statusBar.setMessage("");
		}
	}

	class DumpMSeedAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		DumpMSeedAction() {
			super();
			putValue(Action.NAME, "Dump to MSEED");
			putValue(Action.SHORT_DESCRIPTION, "MSEED");
			putValue(Action.LONG_DESCRIPTION, "Dump selected channels to MSEED format");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_M);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			final FileChooser fc = new FileChooser(FileChooser.Type.MSEED);
			String exportDir = XMAX.getConfiguration().getUserDir("MSEED");
			if (exportDir != null) {
				fc.setCurrentDirectory(new File(exportDir));
			}
			if (fc.showSaveDialog(XMAXframe.getInstance()) == JFileChooser.APPROVE_OPTION) {
				PlotDataProvider channel = null;
				DataOutputStream ds = null;
				try {
					setWaitCursor(true);
					File selectedFile = fc.getSelectedFile();
					XMAX.getConfiguration().setUserDir("MSEED", selectedFile.getParent());
					ds = new DataOutputStream(new FileOutputStream(selectedFile));
					List<PlotDataProvider> selectedChannels = new ArrayList<>();
					List<ChannelView> selectedViews = graphPanel.getCurrentSelectedChannelShowSet();
					for (ChannelView channelView : selectedViews) {
						selectedChannels.addAll(channelView.getPlotDataProviders());
					}
					if (selectedChannels.size() > 0) {
						for (PlotDataProvider selectedChannel : selectedChannels) {
							// (PlotDataProvider)
							channel = selectedChannel;
							channel.dumpMseed(ds, graphPanel.getTimeRange(), graphPanel.getFilter(),
									channel.getRotation());
						}
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Data sucessfully exported to Mseed",
								"Info", JOptionPane.INFORMATION_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(XMAXframe.getInstance(),
								"You should select at least one channel to export", "Warning",
								JOptionPane.WARNING_MESSAGE);
					}
					getGraphPanel().forceRepaint();
				} catch (IOException e1) {
					logger.error("Can't export to Mseed channel " + channel.getChannelName() + ": ", e1);
				} finally {
					try {
						ds.close();
					} catch (IOException e1) {
						logger.error("IOException:", e1);
					}
					setWaitCursor(false);
				}
			} else {
				getGraphPanel().forceRepaint();
			}
			statusBar.setMessage("");
		}
	}

	class DumpSACAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		DumpSACAction() {
			super();
			putValue(Action.NAME, "Dump to SAC");
			putValue(Action.SHORT_DESCRIPTION, "SAC");
			putValue(Action.LONG_DESCRIPTION, "Dump selected channels to SAC format");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			final FileChooser fc = new FileChooser(FileChooser.Type.SAC);
			String exportDir = XMAX.getConfiguration().getUserDir("SAC");
			if (exportDir != null) {
				fc.setCurrentDirectory(new File(exportDir));
			}
			if (fc.showSaveDialog(XMAXframe.getInstance()) == JFileChooser.APPROVE_OPTION) {
				PlotDataProvider channel = null;
				try {
					setWaitCursor(true);
					File selectedFile = fc.getSelectedFile();
					XMAX.getConfiguration().setUserDir("SAC", selectedFile.getParent());
					List<PlotDataProvider> selectedChannels = new ArrayList<>();
					List<ChannelView> selectedViews = graphPanel.getCurrentSelectedChannelShowSet();
					for (ChannelView channelView : selectedViews) {
						selectedChannels.addAll(channelView.getPlotDataProviders());
					}
					if (selectedChannels.size() > 0) {
						Iterator<PlotDataProvider> it = selectedChannels.iterator();
						int i = 1;
						while (it.hasNext()) {
							// (PlotDataProvider)
							channel = it.next();
							DataOutputStream ds = null;
							try {
								String exportFileName;
								if (selectedChannels.size() == 1) {
									exportFileName = selectedFile.getName();
								} else {
									if (selectedFile.getName().contains(".")) {
										int pointPosition = selectedFile.getName().lastIndexOf(".");
										exportFileName = selectedFile.getName().substring(0, pointPosition) + i
												+ selectedFile.getName().substring(pointPosition);
									} else {
										exportFileName = selectedFile.getName() + i;
									}
								}
								exportFileName = selectedFile.getPath().substring(0,
										selectedFile.getPath().lastIndexOf(File.separator)) + File.separator
										+ exportFileName;
								ds = new DataOutputStream(new FileOutputStream(new File(exportFileName)));
								channel.dumpSacAscii(ds, graphPanel.getTimeRange(), graphPanel.getFilter(),
										channel.getRotation());
							} catch (IOException e1) {
								logger.error("Can't export to SAC channel " + channel.getChannelName() + ": ", e1);
							} finally {
								try {
									ds.close();
								} catch (Exception e1) {
									logger.error("Exception:", e1);
								}
							}
							i++;
						}
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Data sucessfully exported to SAC",
								"Info", JOptionPane.INFORMATION_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(XMAXframe.getInstance(),
								"You should select at least one channel to export", "Warning",
								JOptionPane.WARNING_MESSAGE);
					}
					getGraphPanel().forceRepaint();
				} catch (TraceViewException e2) {
					logger.error("Can't export to SAC channel " + channel.getChannelName() + ": ", e2);
				} finally {
					setWaitCursor(false);
				}
			} else {
				getGraphPanel().forceRepaint();
			}
			statusBar.setMessage("");
		}
	}

	class DumpXMLAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		DumpXMLAction() {
			super();
			putValue(Action.NAME, "Dump to XML");
			putValue(Action.SHORT_DESCRIPTION, "XML");
			putValue(Action.LONG_DESCRIPTION, "Dump selected channels to XML format");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			final FileChooser fc = new FileChooser(FileChooser.Type.XML);
			String exportDir = XMAX.getConfiguration().getUserDir("XML");
			if (exportDir != null) {
				fc.setCurrentDirectory(new File(exportDir));
			}
			if (fc.showSaveDialog(XMAXframe.getInstance()) == JFileChooser.APPROVE_OPTION) {
				PlotDataProvider channel = null;
				FileWriter fw = null;
				try {
					setWaitCursor(true);
					File selectedFile = fc.getSelectedFile();
					XMAX.getConfiguration().setUserDir("XML", selectedFile.getParent());
					fw = new FileWriter(selectedFile);
					List<PlotDataProvider> selectedChannels = new ArrayList<>();
					List<ChannelView> selectedViews = graphPanel.getCurrentSelectedChannelShowSet();
					for (ChannelView channelView : selectedViews) {
						selectedChannels.addAll(channelView.getPlotDataProviders());
					}
					if (selectedChannels.size() > 0) {
						fw.write("<Export start = \""
								+ TimeInterval.formatDate(graphPanel.getTimeRange().getStartTime(),
								TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
								+ "\" end = \"" + TimeInterval.formatDate(graphPanel.getTimeRange().getEndTime(),
								TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
								+ "\">\n");
						for (PlotDataProvider selectedChannel : selectedChannels) {
							// (PlotDataProvider)
							channel = selectedChannel;
							channel.dumpXML(fw, graphPanel.getTimeRange(), graphPanel.getFilter(),
									channel.getRotation());
						}
						fw.write("</Export>");
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Data sucessfully exported to XML",
								"Info", JOptionPane.INFORMATION_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(XMAXframe.getInstance(),
								"You should select at least one channel to export", "Warning",
								JOptionPane.WARNING_MESSAGE);
					}
					getGraphPanel().forceRepaint();
				} catch (IOException e1) {
					logger.error("Can't export to XML channel " + channel.getChannelName() + ": ", e1);
				} finally {
					try {
						fw.close();
					} catch (IOException e1) {
						logger.error("IOException:", e1);
					}
					setWaitCursor(false);
				}
			} else {
				getGraphPanel().forceRepaint();
			}
			statusBar.setMessage("");
		}
	}

	class DumpASCIIAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		DumpASCIIAction() {
			super();
			putValue(Action.NAME, "Dump to ASCII");
			putValue(Action.SHORT_DESCRIPTION, "ASCII");
			putValue(Action.LONG_DESCRIPTION, "Dump selected channels to ASCII format");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			final FileChooser fc = new FileChooser(FileChooser.Type.ASCII);
			String exportDir = XMAX.getConfiguration().getUserDir("ASCII");
			if (exportDir != null) {
				fc.setCurrentDirectory(new File(exportDir));
			}
			if (fc.showSaveDialog(XMAXframe.getInstance()) == JFileChooser.APPROVE_OPTION) {
				PlotDataProvider channel = null;
				FileWriter fw = null;
				try {
					setWaitCursor(true);
					File selectedFile = fc.getSelectedFile();
					XMAX.getConfiguration().setUserDir("ASCII", selectedFile.getParent());
					fw = new FileWriter(selectedFile);
					List<PlotDataProvider> selectedChannels = new ArrayList<>();
					List<ChannelView> selectedViews = graphPanel.getCurrentSelectedChannelShowSet();
					for (ChannelView channelView : selectedViews) {
						selectedChannels.addAll(channelView.getPlotDataProviders());
					}
					if (selectedChannels.size() > 0) {
						for (PlotDataProvider selectedChannel : selectedChannels) {
							// (PlotDataProvider)
							channel = selectedChannel;
							channel.dumpASCII(fw, graphPanel.getTimeRange(), graphPanel.getFilter(),
									channel.getRotation());
						}
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Data sucessfully exported to ASCII",
								"Info", JOptionPane.INFORMATION_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(XMAXframe.getInstance(),
								"You should select at least one channel to export", "Warning",
								JOptionPane.WARNING_MESSAGE);
					}
					getGraphPanel().forceRepaint();
				} catch (IOException e1) {
					logger.error("Can't export to ASCII channel " + channel.getChannelName() + ": ", e1);
				} finally {
					try {
						fw.close();
					} catch (IOException e1) {
						logger.error("IOException:", e1);
					}
					setWaitCursor(false);
				}
			} else {
				getGraphPanel().forceRepaint();
			}
			statusBar.setMessage("");
		}
	}

	class FilterAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		FilterAction(String pluginId, String description) {
			super();
			putValue(Action.NAME, pluginId);
			putValue(Action.SHORT_DESCRIPTION, pluginId);
			putValue(Action.LONG_DESCRIPTION, description);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String pluginId = null;
			setWaitCursor(true);
			try {
				pluginId = getValue(Action.NAME).toString();
				IFilter currentFilter = graphPanel.getFilter();
				if ((currentFilter != null) && Objects.equals(currentFilter.getName(), pluginId)) {
					graphPanel.setFilter(null);
					setFilterMenuItem(null);
					setFilterButton(null);
					filterBG.clearSelection();
				} else {
					IFilter filter = XMAX.getFilter(pluginId);
					if (filter.needProcessing()) {
						graphPanel.setFilter(filter);
						if (graphPanel.getFilter() != null && graphPanel.getFilter().equals(filter)) {
							setFilterMenuItem(pluginId);
							setFilterButton(pluginId);
						}
						if (graphPanel.getFilter() == null) {
							filterBG.clearSelection();
						}
					} else {
						graphPanel.setFilter(null);
						setFilterMenuItem(null);
						setFilterButton(null);
						filterBG.clearSelection();
					}
				}
			} catch (ClassNotFoundException | IllegalAccessException | InstantiationException e1) {
				logger.error("Can't get " + pluginId + " plugin filter: ", e1);
			} finally {
				setWaitCursor(false);
			}
			statusBar.setMessage("");
		}

		private void setFilterMenuItem(String pluginId) {
			for (MenuElement m : filterMenu.getSubElements()) {
				if (m instanceof JPopupMenu) {
					for (MenuElement me : m.getSubElements()) {
						if (me instanceof JRadioButtonMenuItem) {
							JRadioButtonMenuItem mCB = (JRadioButtonMenuItem) me;
							if (pluginId == null) {
								mCB.setSelected(false);
							} else if (mCB.getText().equals(pluginId)) {
								mCB.setSelected(true);
							}
						}
					}
				}
			}
		}

		/**
		 * Selects the correct filter button group ToggleButton based on filters
		 * menu selection.
		 *
		 * @param pluginId
		 */
		private void setFilterButton(String pluginId) {
			for (int i = 0; i < getFilterButtonPanel().bg.getButtonCount(); i++) {
				if (pluginId == null) {
					getFilterButtonPanel().bg.clearSelection();
				} else if (pluginId.equals("LP")) {
					getFilterButtonPanel().lowPassButton.setSelected(true);
				} else if (pluginId.equals("HP")) {
					getFilterButtonPanel().highPassButton.setSelected(true);
				} else if (pluginId.equals("BP")) {
					getFilterButtonPanel().bandPassButton.setSelected(true);
				} else if (pluginId.equals("DYO")) {
					getFilterButtonPanel().dyoFilterButton.setSelected(true);
				}
			}

		}
	}

	class LimXAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		LimXAction() {
			super();
			putValue(Action.NAME, "X limits");
			putValue(Action.SHORT_DESCRIPTION, "xlim");
			putValue(Action.LONG_DESCRIPTION, "Show X Limits window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_X);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			TimeInterval ti = LimXDialog.showDialog(XMAXframe.getInstance(), graphPanel.getTimeRange());
			if (ti != null) {
				setWaitCursor(true);
				// Create Runnable SelectTimeCommand obj
				SelectTimeCommand timeTask = new SelectTimeCommand(graphPanel, ti);

				// Create ExecuteCommand obj for executing Runnable
				ExecuteCommand executor = new ExecuteCommand(timeTask);
				executor.initialize();
				executor.start();
				executor.shutdown();
				setWaitCursor(false);
			} else {
				getGraphPanel().forceRepaint();
			}
			statusBar.setMessage("");
		}
	}

	class LimYAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		LimYAction() {
			super();
			putValue(Action.NAME, "Y limits");
			putValue(Action.SHORT_DESCRIPTION, "ylim");
			putValue(Action.LONG_DESCRIPTION, "Show Y Limits window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_Y);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			LimYDialog dialog = new LimYDialog(XMAXframe.getInstance(),
					new Double(graphPanel.getManualValueMin()).intValue(),
					new Double(graphPanel.getManualValueMax()).intValue());
			if (dialog.min != Integer.MAX_VALUE && dialog.max != Integer.MIN_VALUE) {
				if (!(graphPanel.getScaleMode() instanceof ScaleModeXhair)) {
					// Create Runnable SetScaleModeCommand obj
					SetScaleModeCommand scaleTask = new SetScaleModeCommand(graphPanel, new ScaleModeXhair());

					// Create ExecuteCommand obj for executing Runnable
					ExecuteCommand executor1 = new ExecuteCommand(scaleTask);
					executor1.initialize();
					executor1.start();
					executor1.shutdown();

					scaleModeXHairMenuRadioBt.setSelected(true);
				}
				setWaitCursor(true);
				// Create SelectValueCommand obj
				SelectValueCommand valueTask = new SelectValueCommand(graphPanel, dialog.min, dialog.max);

				// Create ExecuteCommand obj for Runnable
				ExecuteCommand executor2 = new ExecuteCommand(valueTask);
				executor2.initialize();
				executor2.start();
				executor2.shutdown();
				setWaitCursor(false);
			} else {
				getGraphPanel().forceRepaint();
			}
			statusBar.setMessage("");
		}
	}

	class PrintAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		PrintAction() {
			super();
			putValue(Action.NAME, "Print");
			putValue(Action.SHORT_DESCRIPTION, "hardcopy");
			putValue(Action.LONG_DESCRIPTION, "Print selected channels");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			PrinterJob printJob = PrinterJob.getPrinterJob();
			printJob.setJobName("XMAX traces");

			PageFormat pf = printJob.defaultPage();
			PageFormat pf2 = printJob.pageDialog(pf);
			if (pf2 != pf) {
				printJob.setPrintable(graphPanel, pf2);
				if (printJob.printDialog()) {
					try {
						printJob.print();
					} catch (PrinterException e1) {
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), e1);
						getGraphPanel().forceRepaint();
					}
				}
			}

			// printJob.setCopies(1);
			// PageFormat format = printJob.defaultPage();
			// format.setOrientation(PageFormat.PORTRAIT);
			// if (printJob.printDialog()) {
			// printJob.setPrintable(graphPanel);
			// try {
			//
			// printJob.print();
			// } catch (PrinterException e1) {
			// lg.error("Can't print graph panel: " + e1);
			// }
			// } else {
			// return;
			// }
			statusBar.setMessage("");
		}
	}

	class ExpGRAPHAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ExpGRAPHAction() {
			super();
			putValue(Action.NAME, "Export to GRAPH");
			putValue(Action.SHORT_DESCRIPTION, "GRAPH");
			putValue(Action.LONG_DESCRIPTION, "Export selected channels to GRAPH format");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_G);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			File exportFile = GraphUtil.saveGraphics(graphPanel, XMAX.getConfiguration().getUserDir("GRAPH"));
			if (exportFile != null) {
				XMAX.getConfiguration().setUserDir("GRAPH", exportFile.getParent());
			} else {
				getGraphPanel().forceRepaint();
			}
			statusBar.setMessage("");
		}
	}

	class ExpHTMLAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ExpHTMLAction() {
			super();
			putValue(Action.NAME, "Export to HTML");
			putValue(Action.SHORT_DESCRIPTION, "HTML");
			putValue(Action.LONG_DESCRIPTION, "Export selected channels to HTML format");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_H);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			File exportFile = GraphUtil.addToHTML(graphPanel, XMAX.getConfiguration().getUserDir("HTML"));
			if (exportFile != null) {
				XMAX.getConfiguration().setUserDir("HTML", exportFile.getParent());
			} else {
				getGraphPanel().forceRepaint();
			}
			statusBar.setMessage("");
		}
	}

	class ViewHeadersAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ViewHeadersAction() {
			super();
			putValue(Action.NAME, "View headers");
			putValue(Action.SHORT_DESCRIPTION, "header");
			putValue(Action.LONG_DESCRIPTION, "Toggle on/off tooltips with block header information");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_H);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			graphPanel.setShowBlockHeader(!graphPanel.getShowBlockHeader());
			showBlockHeadersMenuCheckBox.setState(graphPanel.getShowBlockHeader());
		}
	}

	class ReLoadAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ReLoadAction() {
			super();
			putValue(Action.NAME, "Reload data");
			putValue(Action.SHORT_DESCRIPTION, "Reload");
			putValue(Action.LONG_DESCRIPTION, "Clears data structures and reloads all trace data from disk");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				XMAXDataModule dm = XMAX.getDataModule();
				XMAX.getFrame().getGraphPanel().setVisible(false);
				XMAX.getFrame().getGraphPanel().removeAll();
				dm.reLoadData();
				try {
					graphPanel.setChannelShowSet(dm.getNextChannelSet());
				} catch (TraceViewException ex) {
					JOptionPane.showMessageDialog(XMAX.getFrame(), ex.getMessage(), "Information",
							JOptionPane.INFORMATION_MESSAGE);
					getGraphPanel().forceRepaint();
				}

				statusBar.setChannelCountMessage(dm.getChannelSetStartIndex() + 1, dm.getChannelSetEndIndex(),
						dm.getAllChannels().size());
			} catch (TraceViewException e1) {
				logger.error("Can't reload trace data: ", e1);
			} finally {
				setWaitCursor(false);
				XMAX.getFrame().getGraphPanel().setVisible(true);
			}
		}
	}

	class AboutAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		AboutAction() {
			super();
			putValue(Action.NAME, "About");
			putValue(Action.SHORT_DESCRIPTION, "about");
			putValue(Action.LONG_DESCRIPTION, "Show About dialog");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			AboutDialog.showDialog(XMAXframe.getInstance());
			statusBar.setMessage("");
		}
	}

	class ExitAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		ExitAction() {
			super();
			putValue(Action.NAME, "Exit");
			putValue(Action.SHORT_DESCRIPTION, "exit");
			putValue(Action.LONG_DESCRIPTION, "Close the window and exit from application");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_E);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			/*
			 * try { Pick.updatePickFile(); } catch (Exception e1) {
			 * e1.printStackTrace(); }
			 */
			System.exit(0);
		}
	}

	/**
	 * Navigation buttons for south panel
	 */
	class NavigationButtonPanel extends JPanel implements ActionListener {
		private static final long serialVersionUID = 1L;
		private JButton backButton = null;
		private JButton nextButton = null;
		private JButton undoButton = null;

		NavigationButtonPanel() {
			super();
			GridLayout gridLayout = new GridLayout(3, 0);
			setLayout(gridLayout);
			setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			// Add buttons
			add(getNextButton(), null);
			add(getBackButton(), null);
			add(getUndoButton(), null);

		}

		private JButton getNextButton() {
			if (nextButton == null) {
				nextButton = new JButton("\u2192");
				nextButton.addActionListener(this);
			}
			return nextButton;
		}

		private JButton getBackButton() {
			if (backButton == null) {
				backButton = new JButton("\u2190");
				backButton.addActionListener(this);
			}
			return backButton;
		}

		private JButton getUndoButton() {
			if (undoButton == null) {
				undoButton = new JButton("Undo");
				undoButton.addActionListener(this);
			}
			return undoButton;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			Object src = evt.getSource();
			Action action;
			if (src == nextButton) {
				action = actionMap.get("Next");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == backButton) {
				action = actionMap.get("Previous");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == undoButton) {
				action = actionMap.get("Undo");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			}
		}
	}

	/**
	 * Selection buttons for south panel
	 */
	class SelectionButtonPanel extends JPanel implements ActionListener {
		private static final long serialVersionUID = 1L;
		private JButton selectButton = null;
		private JButton overlayButton = null;
		private JButton clearSelectedButton = null;
		private JButton demeanButton = null;
		private JButton offsetButton = null;
		private JButton colormodeButton = null;
		private JButton reloadButton;
		private JButton addDataButton;

		SelectionButtonPanel() {
			super();
			GridLayout gridLayout = new GridLayout(4, 2);
			setLayout(gridLayout);
			setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			setPreferredSize(new Dimension(100, 100));
			// Add buttons
			add(getSelectButton(), null);
			add(getOverlayButton(), null);
			add(getClearSelectedButton(), null);
			add(getDemeanButton(), null);
			add(getOffsetButton(), null);
			add(getColorModeButton(), null);
			add(getReloadButton(), null);
			add(getAddDataButton(), null);
		}

		private JButton getReloadButton() {
			if (reloadButton == null) {
				reloadButton = new JButton("Reload all data");
				reloadButton.addActionListener(this);
			}
			return reloadButton;
		}

		private JButton getAddDataButton() {
			if (addDataButton == null) {
				addDataButton = new JButton("Load (more) data");
				addDataButton.addActionListener(this);
			}
			return addDataButton;
		}

		private JButton getColorModeButton() {
			if (colormodeButton == null) {
				colormodeButton = new JButton("Switch color mode");
				colormodeButton.addActionListener(this);
			}
			return colormodeButton;
		}

		private JButton getSelectButton() {
			if (selectButton == null) {
				selectButton = new JButton("Select Channel(s)");
				selectButton.addActionListener(this);
			}
			return selectButton;
		}

		private JButton getOverlayButton() {
			if (overlayButton == null) {
				overlayButton = new JButton("Overlay Channels");
				overlayButton.addActionListener(this);
			}
			return overlayButton;
		}

		private JButton getClearSelectedButton() {
			if (clearSelectedButton == null) {
				clearSelectedButton = new JButton("Unload Selected");
				clearSelectedButton.addActionListener(this);
			}
			return clearSelectedButton;
		}

		private JButton getDemeanButton() {
			if (demeanButton == null) {
				demeanButton = new JButton("Demean");
				demeanButton.addActionListener(this);
			}
			return demeanButton;
		}

		private JButton getOffsetButton() {
			if (offsetButton == null) {
				offsetButton = new JButton("Offset Segments");
				offsetButton.addActionListener(this);
			}
			return offsetButton;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			Object src = evt.getSource();
			Action action;
			if (src == selectButton) {
				action = actionMap.get("Select");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == overlayButton) {
				action = actionMap.get("Overlay");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == demeanButton) {
				action = actionMap.get("Demean");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == offsetButton) {
				action = actionMap.get("Offset Segments");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == clearSelectedButton) {
				action = actionMap.get("Clear selected");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == colormodeButton) {
				action = actionMap.get("Color mode");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == reloadButton) {
				action = actionMap.get("Reload data");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == addDataButton) {
				action = actionMap.get("Add data");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			}
		}
	}

	/**
	 * Scaling buttons for south panel
	 */
	class ScalingButtonPanel extends JPanel implements ActionListener {
		private static final long serialVersionUID = 1L;
		private JButton commonScaleButton = null;
		private JButton autoScaleButton = null;
		private JButton xhairScaleButton = null;
		private JButton xlimScaleButton = null;
		private JButton ylimScaleButton = null;
		private JToggleButton removeGainButton = null;

		ScalingButtonPanel() {
			super();
			GridLayout gridLayout = new GridLayout(3, 2);
			setLayout(gridLayout);
			setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			setPreferredSize(new Dimension(100, 100));
			// Add buttons
			add(getAutoScaleButton(), null);
			add(getXLimScaleButton(), null);
			add(getCommonScaleButton(), null);
			add(getYLimScaleButton(), null);
			add(getXHairScaleButton(), null);
			add(getRemoveGainButton(), null);

		}

		private JButton getCommonScaleButton() {
			if (commonScaleButton == null) {
				commonScaleButton = new JButton("Common Scale");
				commonScaleButton.addActionListener(this);
			}
			return commonScaleButton;
		}

		private JButton getAutoScaleButton() {
			if (autoScaleButton == null) {
				autoScaleButton = new JButton("Auto Scale");
				autoScaleButton.addActionListener(this);
			}
			return autoScaleButton;
		}

		private JButton getXHairScaleButton() {
			if (xhairScaleButton == null) {
				xhairScaleButton = new JButton("Crosshair Scale");
				xhairScaleButton.addActionListener(this);
			}
			return xhairScaleButton;
		}

		private JButton getXLimScaleButton() {
			if (xlimScaleButton == null) {
				xlimScaleButton = new JButton("X limits");
				xlimScaleButton.addActionListener(this);
			}
			return xlimScaleButton;
		}

		private JButton getYLimScaleButton() {
			if (ylimScaleButton == null) {
				ylimScaleButton = new JButton("Y limits");
				ylimScaleButton.addActionListener(this);
			}
			return ylimScaleButton;
		}

		private JToggleButton getRemoveGainButton() {
			if (removeGainButton == null) {
				removeGainButton = new JToggleButton("Remove Gain");
				removeGainButton.addActionListener(this);
			}
			return removeGainButton;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			Object src = evt.getSource();
			Action action;
			if (src == commonScaleButton) {
				action = actionMap.get("Scale com");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == autoScaleButton) {
				action = actionMap.get("Scale auto");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == xhairScaleButton) {
				action = actionMap.get("Scale Xhair");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == xlimScaleButton) {
				action = actionMap.get("X limits");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == ylimScaleButton) {
				action = actionMap.get("Y limits");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == removeGainButton) {
				action = actionMap.get("Remove gain");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			}
		}
	}

	/**
	 * Filter buttons for south panel
	 */
	class FilterButtonPanel extends JPanel implements ActionListener {
		private static final long serialVersionUID = 1L;
		private final ButtonGroup bg;
		private JToggleButton lowPassButton = null;
		private JToggleButton bandPassButton = null;
		private JToggleButton highPassButton = null;
		private JToggleButton dyoFilterButton = null;
		private String filterSelected = "";

		FilterButtonPanel() {
			super();
			GridLayout gridLayout = new GridLayout(3, 2);
			setLayout(gridLayout);
			setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			setPreferredSize(new Dimension(100, 100));
			bg = new ButtonGroup();
			// Add buttons
			bg.add(getLowPassButton());
			bg.add(getBandPassButton());
			bg.add(getHighPassButton());
			bg.add(getDYOFilterButton());
			add(getLowPassButton(), null);
			add(getBandPassButton(), null);
			add(getHighPassButton(), null);
			add(getDYOFilterButton(), null);
		}

		private JToggleButton getLowPassButton() {
			if (lowPassButton == null) {
				lowPassButton = new JToggleButton("Low Pass Filter");
				lowPassButton.addActionListener(this);
			}
			return lowPassButton;
		}

		private JToggleButton getBandPassButton() {
			if (bandPassButton == null) {
				bandPassButton = new JToggleButton("Band Pass Filter");
				bandPassButton.addActionListener(this);
			}
			return bandPassButton;
		}

		private JToggleButton getHighPassButton() {
			if (highPassButton == null) {
				highPassButton = new JToggleButton("High Pass Filter");
				highPassButton.addActionListener(this);
			}
			return highPassButton;
		}

		private JToggleButton getDYOFilterButton() {
			if (dyoFilterButton == null) {
				dyoFilterButton = new JToggleButton("DYO Filter");
				dyoFilterButton.addActionListener(this);
			}
			return dyoFilterButton;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			Object src = evt.getSource();
			Action action;
			if (src == lowPassButton) {
				if (lowPassButton.isSelected() && filterSelected.equals(FilterLP.NAME)) {
					bg.clearSelection();
					filterSelected = "";
				} else {
					filterSelected = FilterLP.NAME;
				}
				action = actionMap.get(FilterLP.NAME);
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == bandPassButton) {
				if (bandPassButton.isSelected() && filterSelected.equals(FilterBP.NAME)) {
					bg.clearSelection();
					filterSelected = "";
				} else {
					filterSelected = FilterBP.NAME;
				}
				action = actionMap.get(FilterBP.NAME);
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == highPassButton) {
				if (highPassButton.isSelected() && filterSelected.equals(FilterHP.NAME)) {
					bg.clearSelection();
					filterSelected = "";
				} else {
					filterSelected = FilterHP.NAME;
				}
				action = actionMap.get(FilterHP.NAME);
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == dyoFilterButton) {
				if (dyoFilterButton.isSelected() && filterSelected.equals(FilterDYO.NAME)) {
					bg.clearSelection();
					filterSelected = "";
				} else {
					filterSelected = FilterDYO.NAME;
				}
				action = actionMap.get(FilterDYO.NAME);
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			}
		}
	}

	/**
	 * Analysis buttons for south panel
	 */
	class AnalysisButtonPanel extends JPanel implements ActionListener {
		private static final long serialVersionUID = 1L;
		private JButton PSDButton = null;
		private JButton CoherenceButton = null;
		private JButton spectraButton = null;
		private JButton respButton = null;
		private JButton particlemotionButton = null;
		private JButton rotationButton = null;
		private JButton correlationButton = null;
		private JButton modalButton = null; // for normal mode overlay plot

		AnalysisButtonPanel() {
			super();
			GridLayout gridLayout = new GridLayout(4, 2);
			setLayout(gridLayout);
			setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			setPreferredSize(new Dimension(100, 100));
			// Add buttons
			add(getPSDButton(), null);
			add(getCoherenceButton(), null);
			add(getSpectraButton(), null);
			add(getRespButton(), null);
			add(getParticleMotionButton(), null);
			add(getRotationButton(), null);
			add(getCorrelationButton(), null);
			add(getModalButton(), null);
		}

		private JButton getModalButton() {
			if (modalButton == null) {
				modalButton = new JButton("Normal mode overlay");
				modalButton.addActionListener(this);
			}
			return modalButton;
		}

		private JButton getPSDButton() {
			if (PSDButton == null) {
				PSDButton = new JButton("PSD");
				PSDButton.addActionListener(this);
			}
			return PSDButton;
		}

		private JButton getCoherenceButton() {
			if (CoherenceButton == null) {
				CoherenceButton = new JButton("Coherence");
				CoherenceButton.addActionListener(this);
			}
			return CoherenceButton;
		}

		private JButton getSpectraButton() {
			if (spectraButton == null) {
				spectraButton = new JButton("Spectra");
				spectraButton.addActionListener(this);
			}
			return spectraButton;
		}

		private JButton getRespButton() {
			if (respButton == null) {
				respButton = new JButton("Response");
				respButton.addActionListener(this);
			}
			return respButton;
		}

		private JButton getParticleMotionButton() {
			if (particlemotionButton == null) {
				particlemotionButton = new JButton("Particle Motion");
				particlemotionButton.addActionListener(this);
			}
			return particlemotionButton;
		}

		private JButton getRotationButton() {
			if (rotationButton == null) {
				rotationButton = new JButton("Rotate/Un-Rotate");
				rotationButton.addActionListener(this);
			}
			return rotationButton;
		}

		private JButton getCorrelationButton() {
			if (correlationButton == null) {
				correlationButton = new JButton("Correlation");
				correlationButton.addActionListener(this);
			}
			return correlationButton;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			Object src = evt.getSource();
			Action action;
			if (src == PSDButton) {
				action = actionMap.get(TransPSD.NAME);
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == CoherenceButton) {
				action = actionMap.get(TransCoherence.NAME);
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == spectraButton) {
				action = actionMap.get(TransSpectra.NAME);
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == respButton) {
				action = actionMap.get(TransResp.NAME);
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == particlemotionButton) {
				action = actionMap.get(TransPPM.NAME);
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == rotationButton) {
				action = actionMap.get("Rotation");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == correlationButton) {
				action = actionMap.get("Correlation");
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			} else if (src == modalButton) {
				action = actionMap.get(TransModal.NAME);
				action.actionPerformed(new ActionEvent(this, 0, (String) action.getValue(Action.NAME)));
			}
		}
	}
}
