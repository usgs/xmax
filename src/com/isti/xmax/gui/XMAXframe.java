package com.isti.xmax.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import javax.swing.JPanel;
import javax.swing.JFrame;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import javax.swing.event.MouseInputListener;

import com.isti.traceview.ICommand;
import com.isti.traceview.IUndoableCommand;
import com.isti.traceview.CommandExecutor;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.UndoException;
import com.isti.traceview.commands.OverlayCommand;
import com.isti.traceview.commands.RotateCommand;
import com.isti.traceview.commands.SaveAllDataCommand;
import com.isti.traceview.commands.SelectCommand;
import com.isti.traceview.commands.SelectTimeCommand;
import com.isti.traceview.commands.SelectValueCommand;
import com.isti.traceview.commands.SetScaleModeCommand;
import com.isti.xmax.XMAX;
import com.isti.xmax.XMAXconfiguration;
import com.isti.xmax.common.Pick;
import com.isti.xmax.data.XMAXDataModule;
import com.isti.xmax.gui.XMAXGraphPanel;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.gui.ChannelView;
import com.isti.traceview.gui.ColorModeBW;
import com.isti.traceview.gui.ColorModeByGap;
import com.isti.traceview.gui.ColorModeBySegment;
import com.isti.traceview.gui.FileChooser;
import com.isti.traceview.gui.GraphUtil;
import com.isti.traceview.gui.MeanModeDisabled;
import com.isti.traceview.gui.MeanModeEnabled;
import com.isti.traceview.gui.OffsetModeDisabled;
import com.isti.traceview.gui.OffsetModeEnabled;
import com.isti.traceview.gui.ScaleModeAuto;
import com.isti.traceview.gui.ScaleModeCom;
import com.isti.traceview.gui.ScaleModeXhair;
import com.isti.traceview.processing.IFilter;
import com.isti.traceview.processing.ITransformation;
import com.isti.traceview.processing.Rotation;

import java.awt.GridLayout;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.ToolTipManager;

import org.apache.log4j.Logger;
import org.java.plugin.registry.Extension;

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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;

/**
 * <p>
 * Main frame for XMAX: holds Swing GUI widgets and information about current application state.
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

	private ButtonPanel buttonPanel = null;

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
	private JPanel southPanel = null;
	private JCheckBoxMenuItem showBigCursorMenuCheckBox = null;
	private JCheckBoxMenuItem showStatusBarMenuCheckBox = null;
	private JCheckBoxMenuItem phaseMenuCheckBox = null;
	private JCheckBoxMenuItem meanMenuCheckBox = null;
	private JCheckBoxMenuItem offsetMenuCheckBox = null;
	private JCheckBoxMenuItem rotateMenuCheckBox = null;
	private JCheckBoxMenuItem overlayMenuCheckBox = null;
	private JCheckBoxMenuItem selectMenuCheckBox = null;
	private JCheckBoxMenuItem showBlockHeadersMenuCheckBox = null;
	
	private ButtonGroup scaleModeBG = null; // @jve:decl-index=0:
	private JRadioButtonMenuItem scaleModeAutoMenuRadioBt = null;
	private JRadioButtonMenuItem scaleModeComMenuRadioBt = null;
	private JRadioButtonMenuItem scaleModeXHairMenuRadioBt = null;
	
	private JMenu colorMenu = null;
	private ButtonGroup colorBG = null;
	private JRadioButtonMenuItem bySegmentMenuRadioBt = null;
	private JRadioButtonMenuItem byGapMenuRadioBt = null;
	private JRadioButtonMenuItem BWMenuRadioBt = null;

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

	private JMenuItem printMenuItem = null;

	private JMenuItem exportGRAPHmenuItem = null;

	private JMenuItem exportHTMLmenuItem = null;

	private JCheckBoxMenuItem qcMenuCheckBox = null;

	private JMenuItem aboutMenuItem = null;

	private boolean executorBusy = false;
	private ActionMap actionMap = null;
	
	static {
		ToolTipManager.sharedInstance().setInitialDelay(500);
		ToolTipManager.sharedInstance().setDismissDelay(10000);
	}

	/**
	 * This is the default constructor
	 */
	public XMAXframe() {
		super("XMAX version " + XMAX.getVersionMessage());
		logger.debug("Creating XMAXframe");
		setLocation(XMAX.getConfiguration().getFramePos());
		setSize(XMAX.getConfiguration().getFrameSize());
		setMinimumSize(new Dimension(400, 300));
		setExtendedState(XMAX.getConfiguration().getFrameExtendedState());
		// lg.debug("Java version message: " + XMAX.getJavaVersionMessage());
		if ((getExtendedState() == Frame.MAXIMIZED_BOTH || getExtendedState() == Frame.MAXIMIZED_HORIZ || getExtendedState() == Frame.MAXIMIZED_VERT) && (XMAX.getJavaVersionMessage().toLowerCase().contains("linux") || XMAX.getJavaVersionMessage().toLowerCase().contains("sunos"))
			     || XMAX.getJavaVersionMessage().toLowerCase().contains("solaris")) {
			// Manual size setting for Linux and Solaris, setExtendedState doesn't work
			GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] devices = graphicsEnvironment.getScreenDevices();
			GraphicsDevice gd = devices[0];
			if (gd.getDisplayMode() != null) {
				setSize(gd.getDisplayMode().getWidth(), gd.getDisplayMode().getHeight());
			}
		}
		Action action = null;
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
		action = new ScaleModeAutoAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ScaleModeComAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ScaleModeXHairAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new ShowBigCursorAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new SetColorModeBySegmentAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new SetColorModeByGapAction();
		actionMap.put(action.getValue(Action.NAME), action);
		action = new SetColorModeBWAction();
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
		action = new SpectraAction();
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

		// adding actions for filter plugins
		for (Extension ext: XMAX.getFilters()) {
			action = new FilterAction(ext.getId(), ext.getParameter("description").valueAsString());
			actionMap.put(action.getValue(Action.NAME), action);
		}
		initialize();
		CommandExecutor.getInstance().addObserver(this);
		addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}

			public void mouseEntered(MouseEvent e) {
				graphPanel.xframeMouseEntered(e);
			}

			public void mouseExited(MouseEvent e) {
				graphPanel.xframeMouseExited(e);
			}
		});

		addComponentListener(new ComponentListener(){
			public void componentHidden(ComponentEvent e) {
			}

			public void componentMoved(ComponentEvent e) {
				XMAX.getConfiguration().setFrameState(getExtendedState(), getX() ,getY() ,getWidth() ,getHeight());
			}

			public void componentResized(ComponentEvent e) {
				XMAX.getConfiguration().setFrameState(getExtendedState(), getX() ,getY() ,getWidth() ,getHeight());
				graphPanel.forceRepaint();	// re-pixelize and paint data when resizing
			}

			public void componentShown(ComponentEvent e) {
			}
		});

		addWindowListener(new WindowListener(){
			public void windowActivated(WindowEvent e) {
			}

			public void windowClosed(WindowEvent e) {
			}

			public void windowClosing(WindowEvent e) {
			}

			public void windowDeactivated(WindowEvent e) {
			}

			public void windowDeiconified(WindowEvent e) {
			}

			public void windowIconified(WindowEvent e) {
			}

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
		if (XMAX.getConfiguration().getShowCommandButtonsTop()) {
			commandButtonTopMenuRadioBt.setSelected(true);
			jContentPane.add(getButtonPanel(), BorderLayout.NORTH);
		} else {
			commandButtonBottomMenuRadioBt.setSelected(true);
			southPanel.add(getButtonPanel(), 0);
		}

		graphPanel.setShowBigCursor(XMAX.getConfiguration().getShowBigCursor());
		showBigCursorMenuCheckBox.setState(graphPanel.getShowBigCursor());
		graphPanel.setColorMode(XMAX.getConfiguration().getColorModeState());
		if(XMAX.getConfiguration().getColorModeState() instanceof ColorModeBySegment){
			bySegmentMenuRadioBt.setSelected(true);
		} else if(XMAX.getConfiguration().getColorModeState() instanceof ColorModeByGap){
			byGapMenuRadioBt.setSelected(true);
		} if(XMAX.getConfiguration().getColorModeState() instanceof ColorModeBW){
			BWMenuRadioBt.setSelected(true);
		}
		if (XMAX.getConfiguration().getShowStatusBar()) {
			statusBar.setVisible(true);
			showStatusBarMenuCheckBox.setState(true);
		} else {
			statusBar.setVisible(false);
			showStatusBarMenuCheckBox.setState(false);
		}

		phaseMenuCheckBox.setState(graphPanel.getPhaseState());
		overlayMenuCheckBox.setState(graphPanel.getOverlayState());
		selectMenuCheckBox.setState(graphPanel.getSelectState());
		meanMenuCheckBox.setState(graphPanel.getMeanState() instanceof MeanModeEnabled);
		offsetMenuCheckBox.setState(graphPanel.getOffsetState() instanceof OffsetModeEnabled);
		rotateMenuCheckBox.setState(false);
		XMAXDataModule dm = XMAX.getDataModule();
		try {
			graphPanel.setChannelShowSet(dm.getNextChannelSet());
		} catch (TraceViewException e) {
			JOptionPane.showMessageDialog(this, "This is the last set", "Information", JOptionPane.INFORMATION_MESSAGE);
		}
		statusBar.setChannelCountMessage(dm.getChannelSetStartIndex() + 1, dm.getChannelSetEndIndex(), dm.getAllChannels().size());
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
	public void mouseClicked(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent event) {
	}

	public void mouseMoved(MouseEvent event) {
	}

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

	public void mouseExited(MouseEvent evt) {
		if (evt.getSource() instanceof AbstractButton) {
			statusBar.setMessage("");
		}
	}

	/*
	 * Methods from ItemListener interface to handle filters from menu
	 */

	public void itemStateChanged(ItemEvent e) {
		String message = null;
		JCheckBoxMenuItem cb = (JCheckBoxMenuItem) e.getItem();
		if (e.getStateChange() == ItemEvent.SELECTED)
			message = "Applying " + cb.getText() + " filter to all visible channels";
		if (e.getStateChange() == ItemEvent.DESELECTED)
			message = "Removing " + cb.getText() + " filter from all visible channels";
		JOptionPane.showMessageDialog(XMAXframe.getInstance(), message, "Action description", JOptionPane.INFORMATION_MESSAGE);
		getGraphPanel().forceRepaint();
		statusBar.setMessage("");
	}

	/*
	 * Methods from ActionListener interface to handle transformations from menu
	 */

	public void actionPerformed(ActionEvent e) {
		JMenuItem source = (JMenuItem) (e.getSource());
		try {
			ITransformation resp = XMAX.getTransformation(source.getText());
			List<PlotDataProvider> selectedChannels = new ArrayList<PlotDataProvider>();
			List<ChannelView> selectedViews = graphPanel.getSelectedChannelShowSet();
			for (ChannelView channelView: selectedViews) {
				selectedChannels.addAll(channelView.getPlotDataProviders());
			}
			org.apache.commons.configuration.Configuration pluginConf = XMAXconfiguration.getInstance().getConfigurationAt("SessionData.Plugins." + source.getText());
			resp.transform(selectedChannels, graphPanel.getTimeRange(), graphPanel.getFilter(), pluginConf, getInstance());
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			logger.error("ClassNotFoundException:", e1);	
		} catch (InstantiationException e1) {
			// TODO Auto-generated catch block
			logger.error("InstantiationException:", e1);	
		} catch (IllegalAccessException e1) {
			// TODO Auto-generated catch block
			logger.error("IllegalAccessException:", e1);	
		}
		statusBar.setMessage("");
	}

	// Method from Observer interface
	// Update cursor after notification that all tasks were executed
	public void update(Observable o, Object arg) {
		logger.debug("updating frame due to request from " + o.getClass().getName());
		setWaitCursor(false);
		executorBusy = false;
	}

	/**
	 * Sets flag if we see waiting cursor
	 */
	public void setWaitCursor(boolean state) {
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
		executorBusy = true;
		System.out.println("XMAXframe.setTimeRange(): Time range: " + ti.toString());
		CommandExecutor.getInstance().execute(new SelectTimeCommand(graphPanel, ti));
		// We should to wait while command will be executed for
		while (executorBusy) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				logger.error("InterruptedException:", e1);
			}
		}
	}

	/**
	 * sets flag if internal graph panel should change time range to show all loaded data itself
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
			jContentPane.add(getSoughtPanel(), BorderLayout.SOUTH);
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
	 * This method initializes ButtonPanel
	 * 
	 * @return ButtonPanel
	 */
	private ButtonPanel getButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = new ButtonPanel();
		}
		return buttonPanel;
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
			fileMenu.add(getAboutMenuItem());
			fileMenu.add(getUndoMenuItem());
			fileMenu.add(getPrintMenuItem());
			fileMenu.add(getDumpMenu());
			fileMenu.add(getExportMenu());
			fileMenu.add(getReloadMenuItem());
			fileMenu.add(getExitMenuItem());
		}
		return fileMenu;
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
			channelsMenu.add(getOverlayMenuCheckBox());
			channelsMenu.add(getSelectMenuCheckBox());
			channelsMenu.add(getMeanMenuCheckBox());
			channelsMenu.add(getOffsetMenuCheckBox());
			channelsMenu.add(getRotateMenuCheckBox());
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
			for (Extension ext: XMAX.getFilters()) {
				JRadioButtonMenuItem filterItem = new JRadioButtonMenuItem();
				filterItem.setText(ext.getId());
				filterItem.setSelected(false);
				filterItem.setAction(actionMap.get(ext.getId()));
				filterItem.addMouseListener(this);
				filterBG.add(filterItem);
				filterMenu.add(filterItem);
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
			for (Extension ext: XMAX.getTransformations()) {
				JMenuItem transformationItem = new JMenuItem();
				transformationItem.setText(ext.getId());
				transformationItem.addActionListener(this);
				transformationMenu.add(transformationItem);
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
			colorBG = new ButtonGroup();
			colorBG.add(getBySegmentMenuRadioBt());
			colorMenu.add(getBySegmentMenuRadioBt());
			colorBG.add(getByGapMenuRadioBt());
			colorMenu.add(getByGapMenuRadioBt());
			colorBG.add(getBWMenuRadioBt());
			colorMenu.add(getBWMenuRadioBt());
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
	private JCheckBoxMenuItem getOverlayMenuCheckBox() {
		if (overlayMenuCheckBox == null) {
			overlayMenuCheckBox = new JCheckBoxMenuItem();
			overlayMenuCheckBox.setAction(actionMap.get("Overlay"));
			overlayMenuCheckBox.addMouseListener(this);
		}
		return overlayMenuCheckBox;
	}

	/**
	 * This method initializes selectMenuCheckBox
	 * 
	 * @return javax.swing.JCheckBoxMenuItem
	 */
	private JCheckBoxMenuItem getSelectMenuCheckBox() {
		if (selectMenuCheckBox == null) {
			selectMenuCheckBox = new JCheckBoxMenuItem();
			selectMenuCheckBox.setAction(actionMap.get("Select"));
			selectMenuCheckBox.addMouseListener(this);
		}
		return selectMenuCheckBox;
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
	 * This method initializes offsetMenuCheckBox
	 * 
	 * @return javax.swing.JCheckBoxMenuItem
	 */
	private JCheckBoxMenuItem getOffsetMenuCheckBox() {
		if (offsetMenuCheckBox == null) {
			offsetMenuCheckBox = new JCheckBoxMenuItem();
			offsetMenuCheckBox.setAction(actionMap.get("Offset"));
			offsetMenuCheckBox.addMouseListener(this);
		}
		return offsetMenuCheckBox;
	}

	/**
	 * This method initializes rotateMenuCheckBox
	 * 
	 * @return javax.swing.JCheckBoxMenuItem
	 */
	private JCheckBoxMenuItem getRotateMenuCheckBox() {
		if (rotateMenuCheckBox == null) {
			rotateMenuCheckBox = new JCheckBoxMenuItem();
			rotateMenuCheckBox.setAction(actionMap.get("Rotation"));
			rotateMenuCheckBox.addMouseListener(this);
		}
		return rotateMenuCheckBox;
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
	private JPanel getSoughtPanel() {
		if (southPanel == null) {
			southPanel = new JPanel();
			southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
			southPanel.add(getStatusBar(), null);
		}
		return southPanel;
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
	 * Main method for XMAXFrame (sets visibility)
	 * @param args
	 */
	public static void main(String[] args) {
		//GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		//GraphicsDevice[] devices = graphicsEnvironment.getScreenDevices();
		XMAXframe frame = XMAXframe.getInstance();
		frame.setVisible(true);
	}

	/**
	 * This action loads next portion of traces into graph panel
	 */
	class NextAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public NextAction() {
			super();
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
			putValue(Action.NAME, "Next");
			putValue(Action.SHORT_DESCRIPTION, "next");
			putValue(Action.LONG_DESCRIPTION, "Show next set of channels");
		}

		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				XMAXDataModule dm = XMAX.getDataModule();
				graphPanel.setChannelShowSet(dm.getNextChannelSet()); // and also switch off OVR,
																		// SEL, ROT
				graphPanel.setScaleMode(new ScaleModeAuto());
				graphPanel.setMeanState(new MeanModeDisabled());
				graphPanel.setOffsetState(new OffsetModeDisabled());
				graphPanel.setPickState(false);
				graphPanel.setPhaseState(false);
				graphPanel.setFilter(null);
				graphPanel.setManualValueMax(Integer.MIN_VALUE);
				graphPanel.setManualValueMin(Integer.MAX_VALUE);
				statusBar.setChannelCountMessage(dm.getChannelSetStartIndex() + 1, dm.getChannelSetEndIndex(), dm.getAllChannels().size());
			} catch (TraceViewException e1) {
				JOptionPane.showMessageDialog(XMAX.getFrame(), "This is the last set", "Information", JOptionPane.INFORMATION_MESSAGE);
				getGraphPanel().forceRepaint();
				//logger.error("TraceViewException:", e1);	// last set exception not needed
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

		public PreviousAction() {
			super();
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK));
			putValue(Action.NAME, "Previous");
			putValue(Action.SHORT_DESCRIPTION, "prev");
			putValue(Action.LONG_DESCRIPTION, "Show previous set of channels");
		}

		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				XMAXDataModule dm = XMAX.getDataModule();
				graphPanel.setChannelShowSet(dm.getPreviousChannelSet());
				graphPanel.setScaleMode(new ScaleModeAuto());
				graphPanel.setMeanState(new MeanModeDisabled());
				graphPanel.setOffsetState(new OffsetModeDisabled());
				graphPanel.setPickState(false);
				graphPanel.setPhaseState(false);
				graphPanel.setFilter(null);
				graphPanel.setManualValueMax(Integer.MIN_VALUE);
				graphPanel.setManualValueMin(Integer.MAX_VALUE);
				statusBar.setChannelCountMessage(dm.getChannelSetStartIndex() + 1, dm.getChannelSetEndIndex(), dm.getAllChannels().size());
			} catch (TraceViewException e1) {
				JOptionPane.showMessageDialog(XMAX.getFrame(), "This is the first set", "Information", JOptionPane.INFORMATION_MESSAGE);
				getGraphPanel().forceRepaint();
				//logger.error("TraceViewException:", e1);	// first set exception not needed
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

		public SaveAllAction() {
			super();
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
			putValue(Action.NAME, "Dump all to Internal");
			putValue(Action.SHORT_DESCRIPTION, "save all");
			putValue(Action.LONG_DESCRIPTION, "Save all loaded channels into internal file format");
		}

		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			SaveAllDataCommand saveCommand = new SaveAllDataCommand();
			CommandExecutor.getInstance().execute(saveCommand);
			statusBar.setMessage("");
		}
	}

	class PhasesAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public PhasesAction() {
			super();
			putValue(Action.NAME, "Phases");
			putValue(Action.SHORT_DESCRIPTION, "phases");
			putValue(Action.LONG_DESCRIPTION, "Show phases on graphs");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
		}

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

		public QCAction() {
			super();
			putValue(Action.NAME, "Quality Control");
			putValue(Action.SHORT_DESCRIPTION, "qc");
			putValue(Action.LONG_DESCRIPTION, "Show quality control panel");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_Q);
		}

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

		public OverlayAction() {
			super();
			putValue(Action.NAME, "Overlay");
			putValue(Action.SHORT_DESCRIPTION, "ovr");
			putValue(Action.LONG_DESCRIPTION, "Overlay selected channels");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK));
		}

		public void actionPerformed(ActionEvent e) {
			try {
				setWaitCursor(true);
				CommandExecutor.getInstance().execute(new OverlayCommand(graphPanel));
				overlayMenuCheckBox.setState(graphPanel.getOverlayState());
			} finally {
				setWaitCursor(false);
				statusBar.setMessage("");
			}
		}
	}

	class SelectChannelsAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public SelectChannelsAction() {
			super();
			putValue(Action.NAME, "Select");
			putValue(Action.SHORT_DESCRIPTION, "Sel");
			putValue(Action.LONG_DESCRIPTION, "Select channels");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
		}

		public void actionPerformed(ActionEvent e) {
			try {
				setWaitCursor(true);
				CommandExecutor.getInstance().execute(new SelectCommand(graphPanel));
				selectMenuCheckBox.setState(graphPanel.getSelectState());
			} finally {
				setWaitCursor(false);
				statusBar.setMessage("");
			}
		}
	}

	class ScaleModeAutoAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public ScaleModeAutoAction() {
			super();
			putValue(Action.NAME, "Scale auto");
			putValue(Action.SHORT_DESCRIPTION, "auto");
			putValue(Action.LONG_DESCRIPTION, "switch Scaling Mode to auto");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("Scale auto")) {
				setWaitCursor(true);
				CommandExecutor.getInstance().execute(new SetScaleModeCommand(graphPanel, new ScaleModeAuto()));
				scaleModeAutoMenuRadioBt.setSelected(true);
				graphPanel.setOffsetState(new OffsetModeDisabled());
				offsetMenuCheckBox.setState(false);
			}
			statusBar.setMessage("");
		}
	}

	class ScaleModeComAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public ScaleModeComAction() {
			super();
			putValue(Action.NAME, "Scale com");
			putValue(Action.SHORT_DESCRIPTION, "com");
			putValue(Action.LONG_DESCRIPTION, "switch Scaling Mode to com");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("Scale com")) {
				setWaitCursor(true);
				CommandExecutor.getInstance().execute(new SetScaleModeCommand(graphPanel, new ScaleModeCom()));
				scaleModeComMenuRadioBt.setSelected(true);
				graphPanel.setOffsetState(new OffsetModeDisabled());
				offsetMenuCheckBox.setState(false);
			}
			statusBar.setMessage("");
		}
	}

	class ScaleModeXHairAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public ScaleModeXHairAction() {
			super();
			putValue(Action.NAME, "Scale Xhair");
			putValue(Action.SHORT_DESCRIPTION, "Xhair");
			putValue(Action.LONG_DESCRIPTION, "switch Scaling Mode to X hair");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_X);
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("Scale Xhair")) {
				setWaitCursor(true);
				CommandExecutor.getInstance().execute(new SetScaleModeCommand(graphPanel, new ScaleModeXhair()));
				scaleModeXHairMenuRadioBt.setSelected(true);
				graphPanel.setOffsetState(new OffsetModeDisabled());
				offsetMenuCheckBox.setState(false);
			}
			statusBar.setMessage("");
		}
	}

	class ShowBigCursorAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public ShowBigCursorAction() {
			super();
			putValue(Action.NAME, "Show crosshair");
			putValue(Action.SHORT_DESCRIPTION, "Show crosshair");
			putValue(Action.LONG_DESCRIPTION, "Show big cross hair cursor");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);
		}

		public void actionPerformed(ActionEvent e) {
			graphPanel.setShowBigCursor(showBigCursorMenuCheckBox.getState());
			statusBar.setMessage("");
		}
	}

	class SetColorModeBySegmentAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public SetColorModeBySegmentAction() {
			super();
			putValue(Action.NAME, "Color mode by segment");
			putValue(Action.SHORT_DESCRIPTION, "by segment");
			putValue(Action.LONG_DESCRIPTION, "Segments in the trace separated by color");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		}

		public void actionPerformed(ActionEvent e) {		
			graphPanel.setColorMode(new ColorModeBySegment());
			bySegmentMenuRadioBt.setSelected(true);
			statusBar.setMessage("");
		}
	}
	
	class SetColorModeBWAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public SetColorModeBWAction() {
			super();
			putValue(Action.NAME, "Color mode BW");
			putValue(Action.SHORT_DESCRIPTION, "BW");
			putValue(Action.LONG_DESCRIPTION, "All traces drawn in black and white");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);
		}

		public void actionPerformed(ActionEvent e) {		
			graphPanel.setColorMode(new ColorModeBW());
			BWMenuRadioBt.setSelected(true);
			statusBar.setMessage("");
		}
	}
	
	class SetColorModeByGapAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public SetColorModeByGapAction() {
			super();
			putValue(Action.NAME, "Color mode by gap");
			putValue(Action.SHORT_DESCRIPTION, "by gap");
			putValue(Action.LONG_DESCRIPTION, "Gaps in the trace separated by color");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_G);
		}

		public void actionPerformed(ActionEvent e) {		
			graphPanel.setColorMode(new ColorModeByGap());
			byGapMenuRadioBt.setSelected(true);
			statusBar.setMessage("");
		}
	}
	
	//Used in screen toggle button
	class SwitchColorModeAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public SwitchColorModeAction() {
			super();
			putValue(Action.NAME, "Color mode");
			putValue(Action.SHORT_DESCRIPTION, "Switch color");
			putValue(Action.LONG_DESCRIPTION, "Switch color mode in rotating manner: BW - By segment - By gap");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		}

		public void actionPerformed(ActionEvent e) {
			if(graphPanel.getColorMode() instanceof ColorModeBW){
				graphPanel.setColorMode(new ColorModeBySegment());
				bySegmentMenuRadioBt.setSelected(true);
			}
			else if(graphPanel.getColorMode() instanceof ColorModeBySegment){
				graphPanel.setColorMode(new ColorModeByGap());
				byGapMenuRadioBt.setSelected(true);
			}
			else if(graphPanel.getColorMode() instanceof ColorModeByGap){
				graphPanel.setColorMode(new ColorModeBW());
				BWMenuRadioBt.setSelected(true);
			}
			statusBar.setMessage("");
		}
	}
	
	class DemeanAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public DemeanAction() {
			super();
			putValue(Action.NAME, "Demean");
			putValue(Action.SHORT_DESCRIPTION, "mean");
			putValue(Action.LONG_DESCRIPTION, "Shift mean trace value to 0");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_M);
		}

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

		public ShowStatusBarAction() {
			super();
			putValue(Action.NAME, "Show status bar");
			putValue(Action.SHORT_DESCRIPTION, "Show status bar");
			putValue(Action.LONG_DESCRIPTION, "Toggles status bar on/off");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);
		}

		public void actionPerformed(ActionEvent e) {
			statusBar.setVisible(showStatusBarMenuCheckBox.getState());
			graphPanel.repaint();
			statusBar.setMessage("");
		}
	}

	class ShowCommandButtonsAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public ShowCommandButtonsAction() {
			super();
			putValue(Action.NAME, "Show buttons panel");
			putValue(Action.SHORT_DESCRIPTION, "Show command buttons panel");
			putValue(Action.LONG_DESCRIPTION, "Toggles command buttons panel on/off");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
		}

		public void actionPerformed(ActionEvent e) {
			buttonPanel.setVisible(showButtonsMenuCheckBox.getState());
			graphPanel.repaint();
			statusBar.setMessage("");
		}
	}

	class ShowCommandButtonsTopAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public ShowCommandButtonsTopAction() {
			super();
			putValue(Action.NAME, "Show buttons top");
			putValue(Action.SHORT_DESCRIPTION, "Show command buttons top");
			putValue(Action.LONG_DESCRIPTION, "Show command buttons panel at the top of window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_T);
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("Show buttons top")) {
				southPanel.remove(getButtonPanel());
				jContentPane.add(getButtonPanel(), BorderLayout.NORTH);
				graphPanel.repaint();
			}
			statusBar.setMessage("");
		}
	}

	class ShowCommandButtonsBottomAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public ShowCommandButtonsBottomAction() {
			super();
			putValue(Action.NAME, "Show buttons bottom");
			putValue(Action.SHORT_DESCRIPTION, "Show command buttons bottom");
			putValue(Action.LONG_DESCRIPTION, "Show command buttons panel at the bottom of window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("Show buttons bottom")) {
				jContentPane.remove(getButtonPanel());
				southPanel.add(getButtonPanel(), 0);
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

		public void actionPerformed(ActionEvent e) {
			LinkedList<ICommand> history = CommandExecutor.getInstance().getCommandHistory();
			ICommand command = history.getLast();
			if (command instanceof IUndoableCommand) {
				IUndoableCommand undCommand = (IUndoableCommand) command;
				try {	
					if (undCommand.canUndo()) {
						undCommand.undo();
					}
				} catch (UndoException e1) {
					logger.error("UndoException:", e1);
				}
			}
			statusBar.setMessage("");
		}
	}

	class ParticleMotionAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public ParticleMotionAction() {
			super();
			putValue(Action.NAME, "Particle motion");
			putValue(Action.SHORT_DESCRIPTION, "PPM");
			putValue(Action.LONG_DESCRIPTION, "Show Particle Motion window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_M);
		}

		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				ITransformation resp = XMAX.getTransformation("Particle Motion");
				List<PlotDataProvider> selectedChannels = new ArrayList<PlotDataProvider>();
				List<ChannelView> selectedViews = graphPanel.getSelectedChannelShowSet();
				for (ChannelView channelView: selectedViews) {
					selectedChannels.addAll(channelView.getPlotDataProviders());
				}
				// if selected nothing
				if (selectedChannels.size() == 0) {
					// Try to fill default channels
					List<PlotDataProvider> channelSet = graphPanel.getChannelSet();
					//Station station = null;
					PlotDataProvider Nchannel = null;
					PlotDataProvider Echannel = null;
					for (PlotDataProvider channel: channelSet) {
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
				resp.transform(selectedChannels, graphPanel.getTimeRange(), graphPanel.getFilter(), null, getInstance());
			} catch (ClassNotFoundException e1) {
				logger.error("Can't compute PPM: ", e1);
			} catch (InstantiationException e1) {
				logger.error("Can't compute PPM: ", e1);
			} catch (IllegalAccessException e1) {
				logger.error("Can't compute PPM: ", e1);
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class PowerSpectraDensityAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public PowerSpectraDensityAction() {
			super();
			putValue(Action.NAME, "Power spectra density");
			putValue(Action.SHORT_DESCRIPTION, "PSD");
			putValue(Action.LONG_DESCRIPTION, "Show Power Spectra Density window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		}

		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				ITransformation resp = XMAX.getTransformation("Power Spectra Density");
				List<PlotDataProvider> selectedChannels = new ArrayList<PlotDataProvider>();
				List<ChannelView> selectedViews = graphPanel.getSelectedChannelShowSet();
				if (selectedViews.size() == 0) {
					selectedViews = graphPanel.getChannelShowSet();
				}
				for (ChannelView channelView: selectedViews) {
					selectedChannels.addAll(channelView.getPlotDataProviders());
				}
				org.apache.commons.configuration.Configuration pluginConf = XMAXconfiguration.getInstance().getConfigurationAt("Configuration.Plugins.PSD");
				resp.transform(selectedChannels, graphPanel.getTimeRange(), graphPanel.getFilter(), pluginConf, getInstance());
			} catch (ClassNotFoundException e1) {
				logger.error("Can't compute PSD: ", e1);
			} catch (InstantiationException e1) {
				logger.error("Can't compute PSD: ", e1);
			} catch (IllegalAccessException e1) {
				logger.error("Can't compute PSD: ", e1);
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class SpectraAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public SpectraAction() {
			super();
			putValue(Action.NAME, "Spectra");
			putValue(Action.SHORT_DESCRIPTION, "SPECTRA");
			putValue(Action.LONG_DESCRIPTION, "Show Spectra window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		}

		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				ITransformation resp = XMAX.getTransformation("Spectra");
				List<PlotDataProvider> selectedChannels = new ArrayList<PlotDataProvider>();
				List<ChannelView> selectedViews = graphPanel.getSelectedChannelShowSet();
				if (selectedViews.size() == 0) {
					selectedViews = graphPanel.getChannelShowSet();
				}
				for (ChannelView channelView: selectedViews) {
					selectedChannels.addAll(channelView.getPlotDataProviders());
				}
				resp.transform(selectedChannels, graphPanel.getTimeRange(), graphPanel.getFilter(), null, getInstance());
			} catch (ClassNotFoundException e1) {
				logger.error("Can't compute Spectra: ", e1);
			} catch (InstantiationException e1) {
				logger.error("Can't compute Spectra: ", e1);
			} catch (IllegalAccessException e1) {
				logger.error("Can't compute Spectra: ", e1);
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class CorrelationAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public CorrelationAction() {
			super();
			putValue(Action.NAME, "Correlation");
			putValue(Action.SHORT_DESCRIPTION, "CORR");
			putValue(Action.LONG_DESCRIPTION, "Show 2 traces correlation");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
		}

		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				ITransformation resp = XMAX.getTransformation("Correlation");
				List<PlotDataProvider> selectedChannels = new ArrayList<PlotDataProvider>();
				List<ChannelView> selectedViews = graphPanel.getSelectedChannelShowSet();
				for (ChannelView channelView: selectedViews) {
					selectedChannels.addAll(channelView.getPlotDataProviders());
				}
				resp.transform(selectedChannels, graphPanel.getTimeRange(), graphPanel.getFilter(), null, getInstance());
			} catch (ClassNotFoundException e1) {
				logger.error("Can't compute Correlation: ", e1);
			} catch (InstantiationException e1) {
				logger.error("Can't compute Correlation: ", e1);
			} catch (IllegalAccessException e1) {
				logger.error("Can't compute Correlation: ", e1);
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class RotationAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public RotationAction() {
			super();
			putValue(Action.NAME, "Rotation");
			putValue(Action.SHORT_DESCRIPTION, "ROT");
			putValue(Action.LONG_DESCRIPTION, "Rotates channels");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		}

		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				if (graphPanel.getRotation() != null) {
					graphPanel.setRotation(null);
					rotateMenuCheckBox.setState(false);
				} else {
					CommandExecutor.getInstance().execute(new RotateCommand(graphPanel, new Rotation(XMAX.getFrame())));
					rotateMenuCheckBox.setState(graphPanel.getRotation() != null);
				}
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class ResponseAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public ResponseAction() {
			super();
			putValue(Action.NAME, "Response");
			putValue(Action.SHORT_DESCRIPTION, "RESP");
			putValue(Action.LONG_DESCRIPTION, "Show Response window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		}

		public void actionPerformed(ActionEvent e) {
			setWaitCursor(true);
			try {
				ITransformation resp = XMAX.getTransformation("Response");
				List<PlotDataProvider> selectedChannels = new ArrayList<PlotDataProvider>();
				List<ChannelView> selectedViews = graphPanel.getSelectedChannelShowSet();
				for (ChannelView channelView: selectedViews) {
					selectedChannels.addAll(channelView.getPlotDataProviders());
				}
				resp.transform(selectedChannels, graphPanel.getTimeRange(), null, null, getInstance());
			} catch (ClassNotFoundException e1) {
				logger.error("Can't compute responses: ", e1);
			} catch (InstantiationException e1) {
				logger.error("Can't compute responses: ", e1);
			} catch (IllegalAccessException e1) {
				logger.error("Can't compute responses: ", e1);
			} finally {
				statusBar.setMessage("");
				setWaitCursor(false);
			}
		}
	}

	class OffsetAction extends AbstractAction implements Action {
		
		private static final long serialVersionUID = 1L;

		public OffsetAction() {
			super();
			putValue(Action.NAME, "Offset");
			putValue(Action.SHORT_DESCRIPTION, "offset");
			putValue(Action.LONG_DESCRIPTION, "Show segments with offset so gaps could be seen clearer");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
		}

		public void actionPerformed(ActionEvent e) {
			if (graphPanel.getOffsetState() instanceof OffsetModeDisabled) {
				graphPanel.setOffsetState(new OffsetModeEnabled());

			} else if (graphPanel.getOffsetState() instanceof OffsetModeEnabled) {
				graphPanel.getOffsetState().increaseStep();
				graphPanel.repaint();	// forceRepaint()? Check offsets
			}
			offsetMenuCheckBox.setState(graphPanel.getOffsetState() instanceof OffsetModeEnabled);
			statusBar.setMessage("");
		}
	}

	class MarkPickAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public MarkPickAction() {
			super();
			putValue(Action.NAME, "Mark pick");
			putValue(Action.SHORT_DESCRIPTION, "ttpick");
			putValue(Action.LONG_DESCRIPTION, "Mark travel time pick");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
		}

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

		public DelPickAction() {
			super();
			putValue(Action.NAME, "Delete pick");
			putValue(Action.SHORT_DESCRIPTION, "delpick");
			putValue(Action.LONG_DESCRIPTION, "Delete travel time pick");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
		}

		public void actionPerformed(ActionEvent e) {
			Pick.geleteLastPick();
			graphPanel.repaint();	// may need to override graph when deleting (force?)
			statusBar.setMessage("");
		}
	}

	class DumpMSeedAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public DumpMSeedAction() {
			super();
			putValue(Action.NAME, "Dump to MSEED");
			putValue(Action.SHORT_DESCRIPTION, "MSEED");
			putValue(Action.LONG_DESCRIPTION, "Dump selected channels to MSEED format");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_M);
		}

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
					List<PlotDataProvider> selectedChannels = new ArrayList<PlotDataProvider>();
					List<ChannelView> selectedViews = graphPanel.getSelectedChannelShowSet();
					for (ChannelView channelView: selectedViews) {
						selectedChannels.addAll(channelView.getPlotDataProviders());
					}
					if (selectedChannels.size() > 0) {
						Iterator<PlotDataProvider> it = selectedChannels.iterator();
						while (it.hasNext()) {
							//(PlotDataProvider)
							channel = it.next();
							channel.dumpMseed(ds, graphPanel.getTimeRange(), graphPanel.getFilter());
						}
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Data sucessfully exported to Mseed", "Info",
								JOptionPane.INFORMATION_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), "You should select at least one channel to export", "Warning",
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

		public DumpSACAction() {
			super();
			putValue(Action.NAME, "Dump to SAC");
			putValue(Action.SHORT_DESCRIPTION, "SAC");
			putValue(Action.LONG_DESCRIPTION, "Dump selected channels to SAC format");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		}

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
					List<PlotDataProvider> selectedChannels = new ArrayList<PlotDataProvider>();
					List<ChannelView> selectedViews = graphPanel.getSelectedChannelShowSet();
					for (ChannelView channelView: selectedViews) {
						selectedChannels.addAll(channelView.getPlotDataProviders());
					}
					if (selectedChannels.size() > 0) {
						Iterator<PlotDataProvider> it = selectedChannels.iterator();
						int i = 1;
						while (it.hasNext()) {
							//(PlotDataProvider)
							channel = it.next();
							DataOutputStream ds = null;
							try {
								String exportFileName = "";
								if(selectedChannels.size()==1){
									exportFileName = selectedFile.getName();
								} else {
									if(selectedFile.getName().contains(".")){
										int pointPosition = selectedFile.getName().lastIndexOf(".");
										exportFileName = selectedFile.getName().substring(0, pointPosition) + i + selectedFile.getName().substring(pointPosition);
									} else {
										exportFileName = selectedFile.getName()+i;
									}
								}
								exportFileName = selectedFile.getPath().substring(0, selectedFile.getPath().lastIndexOf(File.separator)) + File.separator + exportFileName;
								ds = new DataOutputStream(new FileOutputStream(new File(exportFileName)));
								channel.dumpSacAscii(ds, graphPanel.getTimeRange(), graphPanel.getFilter());
							} catch (IOException e1) {
								logger.error("Can't export to SAC channel " + channel.getChannelName() + ": ", e1);
							}
							finally {
								try {
									ds.close();
								} catch (Exception e1) {
									logger.error("Exception:", e1);	
								}
							}
							i++;
						}
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Data sucessfully exported to SAC", "Info",
								JOptionPane.INFORMATION_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), "You should select at least one channel to export", "Warning",
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

		public DumpXMLAction() {
			super();
			putValue(Action.NAME, "Dump to XML");
			putValue(Action.SHORT_DESCRIPTION, "XML");
			putValue(Action.LONG_DESCRIPTION, "Dump selected channels to XML format");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		}

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
					List<PlotDataProvider> selectedChannels = new ArrayList<PlotDataProvider>();
					List<ChannelView> selectedViews = graphPanel.getSelectedChannelShowSet();
					for (ChannelView channelView: selectedViews) {
						selectedChannels.addAll(channelView.getPlotDataProviders());
					}
					if (selectedChannels.size() > 0) {
						fw.write("<Export start = \""
								+ TimeInterval.formatDate(graphPanel.getTimeRange().getStartTime(), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
								+ "\" end = \""
								+ TimeInterval.formatDate(graphPanel.getTimeRange().getEndTime(), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
								+ "\">\n");
						Iterator<PlotDataProvider> it = selectedChannels.iterator();
						while (it.hasNext()) {
							//(PlotDataProvider)
							channel = it.next();
							channel.dumpXML(fw, graphPanel.getTimeRange(), graphPanel.getFilter());
						}
						fw.write("</Export>");
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Data sucessfully exported to XML", "Info",
								JOptionPane.INFORMATION_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), "You should select at least one channel to export", "Warning",
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

		public DumpASCIIAction() {
			super();
			putValue(Action.NAME, "Dump to ASCII");
			putValue(Action.SHORT_DESCRIPTION, "ASCII");
			putValue(Action.LONG_DESCRIPTION, "Dump selected channels to ASCII format");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
		}

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
					List<PlotDataProvider> selectedChannels = new ArrayList<PlotDataProvider>();
					List<ChannelView> selectedViews = graphPanel.getSelectedChannelShowSet();
					for (ChannelView channelView: selectedViews) {
						selectedChannels.addAll(channelView.getPlotDataProviders());
					}
					if (selectedChannels.size() > 0) {
						Iterator<PlotDataProvider> it = selectedChannels.iterator();
						while (it.hasNext()) {
							//(PlotDataProvider)
							channel = it.next();
							channel.dumpASCII(fw, graphPanel.getTimeRange(), graphPanel.getFilter());
						}
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Data sucessfully exported to ASCII", "Info",
								JOptionPane.INFORMATION_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), "You should select at least one channel to export", "Warning",
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

		public FilterAction(String pluginId, String description) {
			super();
			putValue(Action.NAME, pluginId);
			putValue(Action.SHORT_DESCRIPTION, pluginId);
			putValue(Action.LONG_DESCRIPTION, description);
		}

		public void actionPerformed(ActionEvent e) {
			String pluginId = null;
			setWaitCursor(true);
			try {
				pluginId = getValue(Action.NAME).toString();
				IFilter currentFilter = graphPanel.getFilter();
				if ((currentFilter != null) && currentFilter.getName().equals(pluginId)) {
					graphPanel.setFilter(null);
					setFilterMenuItem(null);
					filterBG.clearSelection();
				} else {
					IFilter filter = XMAX.getFilter(pluginId);
					if (filter.needProcessing()) {
						graphPanel.setFilter(filter);
						if(graphPanel.getFilter()!=null && graphPanel.getFilter().equals(filter)){
							setFilterMenuItem(pluginId);
						}
						if(graphPanel.getFilter()==null){
							filterBG.clearSelection();
						}
					} else {
						graphPanel.setFilter(null);
						setFilterMenuItem(null);
						filterBG.clearSelection();
						filter = null;
					}
				}
			} catch (ClassNotFoundException e1) {
				logger.error("Can't get " + pluginId + " plugin filter: ", e1);
			} catch (InstantiationException e1) {
				logger.error("Can't get " + pluginId + " plugin filter: ", e1);
			} catch (IllegalAccessException e1) {
				logger.error("Can't get " + pluginId + " plugin filter: ", e1);
			} finally {
				setWaitCursor(false);
			}
			statusBar.setMessage("");
		}

		private void setFilterMenuItem(String pluginId) {
			for (MenuElement m: filterMenu.getSubElements()) {
				if (m instanceof JPopupMenu) {
					for (MenuElement me: m.getSubElements()) {
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
	}

	class LimXAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public LimXAction() {
			super();
			putValue(Action.NAME, "X limits");
			putValue(Action.SHORT_DESCRIPTION, "xlim");
			putValue(Action.LONG_DESCRIPTION, "Show X Limits window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_X);
		}

		public void actionPerformed(ActionEvent e) {
			TimeInterval ti = LimXDialog.showDialog(XMAXframe.getInstance(), graphPanel.getTimeRange());
			if (ti != null) {
				setWaitCursor(true);
				System.out.println("XMAXframe.LimXAction(): Time Range: " + ti.toString() + 
						" --> SelectTimeCommand()");
				CommandExecutor.getInstance().execute(new SelectTimeCommand(graphPanel, ti));
			} else {
				getGraphPanel().forceRepaint();
			}
			statusBar.setMessage("");
		}
	}

	class LimYAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public LimYAction() {
			super();
			putValue(Action.NAME, "Y limits");
			putValue(Action.SHORT_DESCRIPTION, "ylim");
			putValue(Action.LONG_DESCRIPTION, "Show Y Limits window");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_Y);
		}

		public void actionPerformed(ActionEvent e) {
			LimYDialog dialog = new LimYDialog(XMAXframe.getInstance(), new Double(graphPanel.getManualValueMin()).intValue(), new Double(graphPanel.getManualValueMax()).intValue());
			if (dialog.min != Integer.MAX_VALUE && dialog.max != Integer.MIN_VALUE) {
				if (!(graphPanel.getScaleMode() instanceof ScaleModeXhair)) {
					CommandExecutor.getInstance().execute(new SetScaleModeCommand(graphPanel, new ScaleModeXhair()));
					scaleModeXHairMenuRadioBt.setSelected(true);
				}
				setWaitCursor(true);
				System.out.println("XMAXframe.LimYAction( " + dialog.min + 
						", " + dialog.max + " ) --> SelectValueCommand()");
				CommandExecutor.getInstance().execute(new SelectValueCommand(graphPanel, dialog.min, dialog.max));
			} else {
				getGraphPanel().forceRepaint();
			}
			statusBar.setMessage("");
		}
	}

	class PrintAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public PrintAction() {
			super();
			putValue(Action.NAME, "Print");
			putValue(Action.SHORT_DESCRIPTION, "hardcopy");
			putValue(Action.LONG_DESCRIPTION, "Print selected channels");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
		}

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

		public ExpGRAPHAction() {
			super();
			putValue(Action.NAME, "Export to GRAPH");
			putValue(Action.SHORT_DESCRIPTION, "GRAPH");
			putValue(Action.LONG_DESCRIPTION, "Export selected channels to GRAPH format");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_G);
		}

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

		public ExpHTMLAction() {
			super();
			putValue(Action.NAME, "Export to HTML");
			putValue(Action.SHORT_DESCRIPTION, "HTML");
			putValue(Action.LONG_DESCRIPTION, "Export selected channels to HTML format");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_H);
		}

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

		public ViewHeadersAction() {
			super();
			putValue(Action.NAME, "View headers");
			putValue(Action.SHORT_DESCRIPTION, "header");
			putValue(Action.LONG_DESCRIPTION, "Toggle on/off tooltips with block header information");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_H);
		}

		public void actionPerformed(ActionEvent e) {
			graphPanel.setShowBlockHeader(!graphPanel.getShowBlockHeader());
			showBlockHeadersMenuCheckBox.setState(graphPanel.getShowBlockHeader());
		}
	}
	
	class ReLoadAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public ReLoadAction() {
			super();
			putValue(Action.NAME, "Reload data");
			putValue(Action.SHORT_DESCRIPTION, "Reload");
			putValue(Action.LONG_DESCRIPTION, "Clears data structures and reloads all trace data from disk");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		}

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
					JOptionPane.showMessageDialog(XMAX.getFrame(), "This is the last set", "Information", JOptionPane.INFORMATION_MESSAGE);
					getGraphPanel().forceRepaint();
				}
				statusBar.setChannelCountMessage(dm.getChannelSetStartIndex() + 1, dm.getChannelSetEndIndex(), dm.getAllChannels().size());
			} catch (TraceViewException e1) {
				logger.error("Can't reload trace data: ", e1);
			} 
			finally {
				setWaitCursor(false);
				XMAX.getFrame().getGraphPanel().setVisible(true);
			}
		}
	}

	class AboutAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public AboutAction() {
			super();
			putValue(Action.NAME, "About");
			putValue(Action.SHORT_DESCRIPTION, "about");
			putValue(Action.LONG_DESCRIPTION, "Show About dialog");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
		}

		public void actionPerformed(ActionEvent e) {
			AboutDialog.showDialog(XMAXframe.getInstance());
			statusBar.setMessage("");
		}
	}

	class ExitAction extends AbstractAction implements Action {

		private static final long serialVersionUID = 1L;

		public ExitAction() {
			super();
			putValue(Action.NAME, "Exit");
			putValue(Action.SHORT_DESCRIPTION, "exit");
			putValue(Action.LONG_DESCRIPTION, "Close the window and exit from application");
			putValue(Action.MNEMONIC_KEY, KeyEvent.VK_E);
		}

		public void actionPerformed(ActionEvent e) {
			/*
			 * try { Pick.updatePickFile(); } catch (Exception e1) { e1.printStackTrace(); }
			 */
			System.exit(0);
		}
	}

	/**
	 * Command buttons top/bottom panel
	 */
	class ButtonPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		private CommandButton filterButton = null;
		private CommandButton scaleButton = null;
		private CommandButton selectButton = null;
		private CommandButton winButton = null;
		private CommandButton plotButton = null;
		private CommandButton toggleButton = null;
		private CommandButton dumpButton = null;
		private CommandButton qcButton = null;
		private CommandButton pick_offsetButton = null;
		private CommandButton exportButton = null;
		private CommandButton limButton = null;
		private CommandButton processingButton = null;

		public ButtonPanel() {
			super();
			GridLayout gridLayout2 = new GridLayout(3, 4, 2, 2);
			setLayout(gridLayout2);
			setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			setPreferredSize(new Dimension(100, 100));

			add(getPlotButton(), null);
			add(getDumpButton(), null);
			add(getExportButton(), null);
			add(getScaleButton(), null);

			add(getSelectButton(), null);
			add(getProcessingButton(), null);
			add(getPick_offsetButton(), null);
			add(getWinButton(), null);

			add(getToggleButton(), null);
			add(getPhaseButton(), null);
			add(getFilterButton(), null);
			add(getLimButton(), null);
		}

		/**
		 * This method initializes filterButton
		 * 
		 * @return CommandButton
		 */
		private CommandButton getFilterButton() {
			if (filterButton == null) {
				filterButton = new CommandButton("FLTR");
				filterButton.setAction1(actionMap.get("LP"));
				filterButton.setAction2(actionMap.get("BP"));
				filterButton.setAction3(actionMap.get("DYO"));
			}
			return filterButton;
		}

		/**
		 * This method initializes scaleButton
		 * 
		 * @return CommandButton
		 */
		private CommandButton getScaleButton() {
			if (scaleButton == null) {
				scaleButton = new CommandButton("SCL");
				scaleButton.setAction1(actionMap.get("Scale auto"));
				scaleButton.setAction2(actionMap.get("Scale com"));
				scaleButton.setAction3(actionMap.get("Scale Xhair"));
			}
			return scaleButton;
		}

		/**
		 * This method initializes selectButton
		 * 
		 * @return CommandButton
		 */
		private CommandButton getSelectButton() {
			if (selectButton == null) {
				selectButton = new CommandButton("SEL");
				selectButton.setAction1(actionMap.get("Select"));
				selectButton.setAction2(actionMap.get("Reload data"));
				selectButton.setAction3(actionMap.get("Overlay"));
			}
			return selectButton;
		}

		/**
		 * This method initializes winButton
		 * 
		 * @return CommandButton
		 */
		private CommandButton getWinButton() {
			if (winButton == null) {
				winButton = new CommandButton("WIN");
				winButton.setAction1(actionMap.get("Particle motion"));
				winButton.setAction2(actionMap.get("Power spectra density"));
				winButton.setAction3(actionMap.get("Response"));
			}
			return winButton;
		}

		/**
		 * This method initializes plotButton
		 * 
		 * @return CommandButton
		 */
		private CommandButton getPlotButton() {
			if (plotButton == null) {
				plotButton = new CommandButton("PLT");
				plotButton.setAction1(actionMap.get("Next"));
				plotButton.setAction2(actionMap.get("Previous"));
				plotButton.setAction3(actionMap.get("Undo"));
			}
			return plotButton;
		}

		/**
		 * This method initializes toggleButton
		 * 
		 * @return CommandButton
		 */
		private CommandButton getToggleButton() {
			if (toggleButton == null) {
				toggleButton = new CommandButton("TOG");
				toggleButton.setAction1(actionMap.get("Phases"));
				toggleButton.setAction2(actionMap.get("Color mode"));
				toggleButton.setAction3(actionMap.get("Demean"));
			}
			return toggleButton;
		}

		/**
		 * This method initializes dumpButton
		 * 
		 * @return CommandButton
		 */
		private CommandButton getDumpButton() {
			if (dumpButton == null) {
				dumpButton = new CommandButton("DMP");
				dumpButton.setAction1(actionMap.get("Dump to XML"));
				dumpButton.setAction2(actionMap.get("Dump to MSEED"));
				dumpButton.setAction3(actionMap.get("Dump to ASCII"));
			}
			return dumpButton;
		}

		/**
		 * This method initializes phaseButton
		 * 
		 * @return CommandButton
		 */
		private CommandButton getPhaseButton() {
			if (qcButton == null) {
				qcButton = new CommandButton("QC");
				qcButton.setAction1(actionMap.get("Quality Control"));
				qcButton.setAction2(actionMap.get("View headers"));
				qcButton.setAction3(actionMap.get("Dump to SAC"));
			}
			return qcButton;
		}

		/**
		 * This method initializes exitButton
		 * 
		 * @return CommandButton
		 */
		private CommandButton getProcessingButton() {
			if (processingButton == null) {
				processingButton = new CommandButton(null);
				processingButton.setAction1(actionMap.get("Rotation"));
				processingButton.setAction2(actionMap.get("Spectra"));
				processingButton.setAction3(actionMap.get("Correlation"));
			}
			return processingButton;
		}

		/**
		 * This method initializes pick_offsetButton
		 * 
		 * @return CommandButton
		 */
		private CommandButton getPick_offsetButton() {
			if (pick_offsetButton == null) {
				pick_offsetButton = new CommandButton(null);
				pick_offsetButton.setAction1(actionMap.get("Offset"));
				pick_offsetButton.setAction2(actionMap.get("Mark pick"));
				pick_offsetButton.setAction3(actionMap.get("Delete pick"));
			}
			return pick_offsetButton;
		}

		/**
		 * This method initializes exportButton
		 * 
		 * @return CommandButton
		 */
		private CommandButton getExportButton() {
			if (exportButton == null) {
				exportButton = new CommandButton("EXP");
				exportButton.setAction1(actionMap.get("Print"));
				exportButton.setAction2(actionMap.get("Export to GRAPH"));
				exportButton.setAction3(actionMap.get("Export to HTML"));
			}
			return exportButton;
		}

		/**
		 * This method initializes limButton
		 * 
		 * @return CommandButton
		 */
		private CommandButton getLimButton() {
			if (limButton == null) {
				limButton = new CommandButton("LIM");
				limButton.setAction1(actionMap.get("X limits"));
				limButton.setAction2(actionMap.get("Y limits"));
				limButton.setAction3(actionMap.get("Exit"));
			}
			return limButton;
		}

	}
} // @jve:decl-index=0:visual-constraint="10,10"
