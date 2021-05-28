package com.isti.traceview.gui;
import javax.swing.JPanel;

/**
 * Abstract representation of mouse behavior. Concrete realizations of this interface
 * can be assigned to ChannelView or GraphPanel to customize its.
 *
 **/
public interface IMouseAdapter {
	void mouseClickedButton1(int x, int y, JPanel clickedAt);

	void mouseClickedButton2(int x, int y, JPanel clickedAt);

	void mouseClickedButton3(int x, int y, JPanel clickedAt);

	void mouseMoved(int x, int y, JPanel clickedAt);
	
	void mouseDragged(int x, int y, JPanel clickedAt);

	void mouseReleasedButton1(int x, int y, JPanel clickedAt);

	void mouseReleasedButton3(int x, int y, JPanel clickedAt);
}
