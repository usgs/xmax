package com.isti.xmax.gui;

import javax.swing.JPanel;
import javax.swing.JDialog;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import javax.swing.JLabel;

import com.centerkey.utils.BareBonesBrowserLaunch;
import com.isti.xmax.XMAX;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JButton;
import java.awt.Cursor;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * "About" dialog for XMAX
 * 
 * @author Max Kokoulin
 */
public class AboutDialog extends JPanel implements ActionListener, MouseListener {

	private JLabel programNameL = null;
	private JLabel dateL = null;

	private JTextArea javaVersionMessageTA = null;

	private JLabel usedLibsL = null;

	private JTextArea usedLibsTA = null;

	private JButton getJavaB = null;

	private JPanel copyrightPanel = null;

	private JLabel writtenL = null;

	private JLabel istiL = null;

	private JLabel forL = null;

	private JLabel usgsL = null;
	private JScrollPane usedLibsSP = null;

	/**
	 * Default constructor
	 */
	public AboutDialog() {
		super();
		initialize();
	}

	/**
	 * This method initializes dialog
	 */
	private void initialize() {
		setSize(new Dimension(320, 285));
		setMinimumSize(new Dimension(320, 285));
		setMaximumSize(new Dimension(320, 285));
		setPreferredSize(new Dimension(320, 285));
		setBorder(BorderFactory.createEtchedBorder());
		setLayout(new GridBagLayout());

		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.insets = new Insets(5, 10, 5, 15);
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		programNameL = new JLabel();
		programNameL.setText("XMAX version " + XMAX.getVersionMessage());
		add(programNameL, gridBagConstraints);

		GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
		gridBagConstraints1.gridx = 1;
		gridBagConstraints1.gridy = 0;
		gridBagConstraints1.gridwidth = 1;
		gridBagConstraints1.anchor = GridBagConstraints.EAST;
		gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints1.insets = new Insets(5, 10, 5, 15);
		dateL = new JLabel();
		dateL.setText(XMAX.getReleaseDateMessage());
		dateL.setHorizontalAlignment(SwingConstants.TRAILING);
		GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
		gridBagConstraints2.gridx = 0;
		gridBagConstraints2.gridy = 1;
		gridBagConstraints2.gridwidth = 2;
		gridBagConstraints2.anchor = GridBagConstraints.WEST;
		gridBagConstraints2.insets = new Insets(5, 10, 5, 10);
		gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
		this.add(dateL, gridBagConstraints1);
		this.add(getCopyrightPanel(), gridBagConstraints2);

		GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
		gridBagConstraints3.gridx = 0;
		gridBagConstraints3.gridy = 2;
		gridBagConstraints3.gridwidth = 2;
		gridBagConstraints3.anchor = GridBagConstraints.WEST;
		gridBagConstraints3.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints3.insets = new Insets(5, 10, 0, 10);
		usedLibsL = new JLabel();
		usedLibsL.setText("Used libraries:");
		add(usedLibsL, gridBagConstraints3);

		GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
		gridBagConstraints4.gridx = 0;
		gridBagConstraints4.gridy = 3;
		gridBagConstraints4.gridwidth = 2;
		gridBagConstraints4.anchor = GridBagConstraints.WEST;
		gridBagConstraints4.insets = new Insets(2, 10, 5, 10);
		gridBagConstraints4.fill = GridBagConstraints.HORIZONTAL;
		usedLibsSP = new JScrollPane(getUsedLibsTA());
		usedLibsSP.setMaximumSize(new Dimension(310, 70));
		usedLibsSP.setMinimumSize(new Dimension(310, 70));
		add(usedLibsSP, gridBagConstraints4);

		GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
		gridBagConstraints5.gridx = 0;
		gridBagConstraints5.gridy = 4;
		gridBagConstraints5.gridwidth = 1;
		gridBagConstraints5.anchor = GridBagConstraints.WEST;
		gridBagConstraints5.insets = new Insets(5, 10, 5, 10);
		gridBagConstraints5.fill = GridBagConstraints.HORIZONTAL;
		javaVersionMessageTA = new JTextArea();
		javaVersionMessageTA.setForeground(Color.RED);
		javaVersionMessageTA.setLineWrap(true);
		javaVersionMessageTA.setWrapStyleWord(true);
		javaVersionMessageTA.setText(XMAX.getJavaVersionMessage());
		javaVersionMessageTA.setBackground(this.getBackground());
		javaVersionMessageTA.setEditable(false);
		javaVersionMessageTA.setMaximumSize(new Dimension(190, 70));
		javaVersionMessageTA.setMinimumSize(new Dimension(190, 70));
		add(javaVersionMessageTA, gridBagConstraints5);

		GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
		gridBagConstraints6.gridx = 1;
		gridBagConstraints6.gridy = 4;
		gridBagConstraints6.gridwidth = 1;
		gridBagConstraints6.anchor = GridBagConstraints.WEST;
		gridBagConstraints6.insets = new Insets(5, 10, 5, 10);
		gridBagConstraints6.fill = GridBagConstraints.HORIZONTAL;
		this.add(getGetJava(), gridBagConstraints6);
	}

	/**
	 * This method initializes getJava
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getGetJava() {
		if (getJavaB == null) {
			getJavaB = new JButton();
			getJavaB.setEnabled(!javaVersionMessageTA.getText().contains("version OK"));
			getJavaB.setText("Get JRE");
			getJavaB.addActionListener(this);
		}
		return getJavaB;
	}

	/**
	 * This method initializes copyrightPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getCopyrightPanel() {
		if (copyrightPanel == null) {
			usgsL = new JLabel();
			usgsL.setText("US Geological Survey");
			usgsL.setCursor(new Cursor(Cursor.HAND_CURSOR));
			usgsL.setForeground(Color.blue);
			usgsL.addMouseListener(this);
			forL = new JLabel();
			forL.setText("for ");
			istiL = new JLabel();
			istiL.setText("Instrumental Software Technologies, Inc ");
			istiL.setCursor(new Cursor(Cursor.HAND_CURSOR));
			istiL.setForeground(Color.blue);
			istiL.addMouseListener(this);
			writtenL = new JLabel();
			writtenL.setText("Written by ");
			copyrightPanel = new JPanel();
			copyrightPanel.setMinimumSize(new Dimension(300, 65));
			copyrightPanel.add(writtenL);
			copyrightPanel.add(istiL);
			copyrightPanel.add(forL);
			copyrightPanel.add(usgsL);
		}
		return copyrightPanel;
	}

	/**
	 * This method initializes usedLibsTA
	 * 
	 * @return javax.swing.JTextArea
	 */
	private JTextArea getUsedLibsTA() {
		if (usedLibsTA == null) {
			usedLibsTA = new JTextArea();
			usedLibsTA.setText("JFreeChart\nJava Plugin Framework\nLog4j\nTauP\nBareBones BrowserLaunch\nApache commons");
			usedLibsTA.setBackground(this.getBackground());
			usedLibsTA.setEditable(false);
			usedLibsTA.setCaretPosition(0);
		}
		return usedLibsTA;
	}

	public static void showDialog(JFrame frame) {
		JOptionPane optionPane = new JOptionPane(new AboutDialog(), JOptionPane.PLAIN_MESSAGE, JOptionPane.CLOSED_OPTION);
		JDialog dialog = optionPane.createDialog(frame, "About");
		dialog.setVisible(true);
		dialog.dispose();
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setVisible(true);
		showDialog(frame);

	}

	// Method from ActionListener interface

	public void actionPerformed(ActionEvent e) {
		BareBonesBrowserLaunch.openURL("http://java.sun.com/javase/downloads/index.jsp");
	}

	// Methods from MouseListener interface

	public void mouseClicked(MouseEvent e) {
		if (e.getSource().equals(istiL)) {
			BareBonesBrowserLaunch.openURL("http://www.isti.com");
		} else if (e.getSource().equals(usgsL)) {
			BareBonesBrowserLaunch.openURL("http://www.usgs.gov");
		}
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}
} // @jve:decl-index=0:visual-constraint="4,7"
