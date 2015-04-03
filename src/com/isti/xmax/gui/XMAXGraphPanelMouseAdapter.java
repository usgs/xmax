package com.isti.xmax.gui;

import java.awt.event.ActionEvent;

import javax.swing.JPanel;

import com.isti.traceview.gui.IMouseAdapter;

public class XMAXGraphPanelMouseAdapter implements IMouseAdapter {

	// Actions for GraphPanel mouse movements, clicks, etc.
	public void mouseClickedButton1(int x, int y, JPanel clickedAt) {
	}

	public void mouseClickedButton2(int x, int y, JPanel clickedAt) {
		XMAXframe.UndoAction action = XMAXframe.getInstance().new UndoAction();
		action.actionPerformed(new ActionEvent(clickedAt, 0, "undo"));
	}

	public void mouseClickedButton3(int x, int y, JPanel clickedAt) {
	}
	
	public void mouseMoved(int x, int y, JPanel clickedAt) {
	}

	public void mouseDragged(int x, int y, JPanel clickedAt) {
	}
	
	public void mouseReleasedButton1(int x, int y, JPanel clickedAt){}
	public void mouseReleasedButton3(int x, int y, JPanel clickedAt){}
}
