package com.isti.traceview.gui;

/**
 * Abstract representation of mouse behavior. Concrete realizations of this interface
 * can be assigned to ChannelView or GraphPanel to customize its.
 */
import javax.swing.JPanel;

public interface IMouseAdapter {
	public void mouseClickedButton1(int x, int y, JPanel clickedAt);

	public void mouseClickedButton2(int x, int y, JPanel clickedAt);

	public void mouseClickedButton3(int x, int y, JPanel clickedAt);

	public void mouseMoved(int x, int y, JPanel clickedAt);

	public void mouseDragged(int x, int y, JPanel clickedAt);

	public void mouseReleasedButton1(int x, int y, JPanel clickedAt);

	public void mouseReleasedButton3(int x, int y, JPanel clickedAt);
}
