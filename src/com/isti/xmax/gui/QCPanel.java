package com.isti.xmax.gui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Side-panel for quality control issues
 * 
 * @author Max Kokoulin
 */
public class QCPanel extends JPanel {

	private JTree qcTree = null;
	private JScrollPane scrollPane = null;

	/**
	 * This method initializes QCpanel
	 */
	public QCPanel() {
		super();
		initialize();
	}

	/**
	 * This method initializes this QCpanel
	 */
	private void initialize() {
		FlowLayout flowLayout = new FlowLayout();
		flowLayout.setHgap(0);
		flowLayout.setVgap(0);
		this.setLayout(flowLayout);
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		this.add(getScrollPane(), null);
	}

	/**
	 * This method initializes qcTree
	 * 
	 * @return javax.swing.JTree
	 */
	private JTree getQcTree() {
		if (qcTree == null) {
			DefaultMutableTreeNode top = new DefaultMutableTreeNode("Quality Control");
			qcTree = new JTree(top);
			qcTree.setBackground(getBackground());
			createNodes(top);
		}
		return qcTree;
	}

	/**
	 * This method initializes scrollPane
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getScrollPane() {
		if (scrollPane == null) {
			scrollPane = new JScrollPane();
			scrollPane.setPreferredSize(new Dimension(150, 32767));
			scrollPane.setViewportView(getQcTree());
		}
		return scrollPane;
	}

	private void createNodes(DefaultMutableTreeNode top) {
		DefaultMutableTreeNode issue = null;

		issue = new DefaultMutableTreeNode("Flatlined trace");
		top.add(issue);
		issue = new DefaultMutableTreeNode("Spikey trace");
		top.add(issue);
		issue = new DefaultMutableTreeNode("Timing problem likely");
		top.add(issue);
	}
}
