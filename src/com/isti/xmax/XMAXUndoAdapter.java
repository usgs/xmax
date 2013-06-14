package com.isti.xmax;

import com.isti.traceview.IUndoAdapter;
import com.isti.xmax.gui.XMAXframe;

/**
 * Actions performed after enabling/disabling
 * 
 * @author Max Kokoulin
 */
public class XMAXUndoAdapter implements IUndoAdapter {

	public void setUndoEnabled(boolean ue) {
		XMAXframe.getInstance().setUndoEnabled(ue);
	}

}
