package com.isti.traceview.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.hexidec.ekit.EkitCore;
import java.awt.Dimension;

/**
 * Dialog to add comments during HTML report generation. 
 * Supports HTML text formatting.
 * 
 * @author Max Kokoulin
 */
public class CommentDialog extends JDialog implements PropertyChangeListener {

	private JOptionPane optionPane;
	private EkitCore editorPane = null;
	private String commentText = null;

	public CommentDialog(Frame owner) {
		super(owner, "Enter comments:", true);
		Object[] options = { "Close", "OK" };
		setPreferredSize(new Dimension(800, 500));
		// Create the JOptionPane.
		optionPane = new JOptionPane(createMessagePanel(), JOptionPane.PLAIN_MESSAGE, JOptionPane.CLOSED_OPTION, null, options, options[0]);
		// Make this dialog display it.
		setContentPane(optionPane);
		optionPane.addPropertyChangeListener(this);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window, we're going to change the JOptionPane's
				 * value property.
				 */
				optionPane.setValue("Close");
			}
		});
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();
		if (isVisible() && (e.getSource() == optionPane) && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
			Object value = optionPane.getValue();
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			// If you were going to check something
			// before closing the window, you'd do
			// it here.
			if (value.equals("Close")) {
				setVisible(false);
				dispose();
				commentText = null;
			} else if (value.equals("OK")) {
				commentText = editorPane.getDocumentBody();
				setVisible(false);
				dispose();
			}
		}
	}

	/**
	 * 
	 * @return HTML text of entered comment
	 */
	public String getCommentText() {
		return commentText;
	}

	private JPanel createMessagePanel() {
		editorPane = getEditorPane();
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(editorPane, BorderLayout.CENTER);
		panel.add(editorPane.getToolBar(true), BorderLayout.NORTH);
		Vector vcMenus = new Vector();
		vcMenus.add(EkitCore.KEY_MENU_EDIT);
		vcMenus.add(EkitCore.KEY_MENU_VIEW);
		vcMenus.add(EkitCore.KEY_MENU_FONT);
		vcMenus.add(EkitCore.KEY_MENU_FORMAT);
		vcMenus.add(EkitCore.KEY_MENU_INSERT);
		vcMenus.add(EkitCore.KEY_MENU_TABLE);
		vcMenus.add(EkitCore.KEY_MENU_FORMS);
		vcMenus.add(EkitCore.KEY_MENU_SEARCH);
		vcMenus.add(EkitCore.KEY_MENU_TOOLS);
		this.setJMenuBar(editorPane.getCustomMenuBar(vcMenus));
		return panel;
	}

	/**
	 * This method initializes editorPane
	 * 
	 * @return javax.swing.JEditorPane
	 */
	private EkitCore getEditorPane() {
		if (editorPane == null) {
			editorPane = new EkitCore(null, // [String] A text or HTML document to load in the
											// editor upon startup.
					null, // [String] A CSS stylesheet to load in the editor upon startup.
					null, // [String] A document encoded as a String to load in the editor upon
							// startup.
					null, // [StyledDocument] Optional document specification, using
							// javax.swing.text.StyledDocument.
					null, // [URL] A URL reference to the CSS style sheet.
					true, // [boolean] Specifies whether the app should include the toolbar(s).
					false, // [boolean] Specifies whether or not to show the View Source window on
							// startup.
					true, // [boolean] Specifies whether or not to show icon pictures in menus.
					true, // [boolean] Specifies whether or not to use exclusive edit mode
							// (recommended on).
					"en", // [String] The language portion of the Internationalization Locale to
							// run Ekit in.
					"US", // [String] The country portion of the Internationalization Locale to
							// run Ekit in.
					false, // [boolean] Specifies whether the raw document is Base64 encoded or
							// not.
					false, // [boolean] Specifies whether to show the Debug menu or not.
					false, // [boolean] Specifies whether or not this uses the SpellChecker module
					false, // [boolean] Specifies whether to use multiple toolbars or one big
							// toolbar.
					"CT|CP|PS|SP|UN|RE|SP|SP|BL|IT|UD|SP|SK|SU|SB|SP|AL|AC|AR|AJ|SP|UL|OL|SP|FN|SP|LK|SP|SR|SP|FO"); // [String]
																														// Code
																														// string
			// specifying the toolbar
			// buttons to show.

			// Tool Keys
			// public static final String KEY_TOOL_SEP = "SP";
			// public static final String KEY_TOOL_NEW = "NW";
			// public static final String KEY_TOOL_OPEN = "OP";
			// public static final String KEY_TOOL_SAVE = "SV";
			// public static final String KEY_TOOL_PRINT = "PR";
			// public static final String KEY_TOOL_CUT = "CT";
			// public static final String KEY_TOOL_COPY = "CP";
			// public static final String KEY_TOOL_PASTE = "PS";
			// public static final String KEY_TOOL_UNDO = "UN";
			// public static final String KEY_TOOL_REDO = "RE";
			// public static final String KEY_TOOL_BOLD = "BL";
			// public static final String KEY_TOOL_ITALIC = "IT";
			// public static final String KEY_TOOL_UNDERLINE = "UD";
			// public static final String KEY_TOOL_STRIKE = "SK";
			// public static final String KEY_TOOL_SUPER = "SU";
			// public static final String KEY_TOOL_SUB = "SB";
			// public static final String KEY_TOOL_ULIST = "UL";
			// public static final String KEY_TOOL_OLIST = "OL";
			// public static final String KEY_TOOL_ALIGNL = "AL";
			// public static final String KEY_TOOL_ALIGNC = "AC";
			// public static final String KEY_TOOL_ALIGNR = "AR";
			// public static final String KEY_TOOL_ALIGNJ = "AJ";
			// public static final String KEY_TOOL_UNICODE = "UC";
			// public static final String KEY_TOOL_UNIMATH = "UM";
			// public static final String KEY_TOOL_FIND = "FN";
			// public static final String KEY_TOOL_ANCHOR = "LK";
			// public static final String KEY_TOOL_SOURCE = "SR";
			// public static final String KEY_TOOL_STYLES = "ST";
			// public static final String KEY_TOOL_FONTS = "FO";
			// public static final String KEY_TOOL_INSTABLE = "TI";
			// public static final String KEY_TOOL_EDITTABLE = "TE";
			// public static final String KEY_TOOL_EDITCELL = "CE";
			// public static final String KEY_TOOL_INSERTROW = "RI";
			// public static final String KEY_TOOL_INSERTCOL = "CI";
			// public static final String KEY_TOOL_DELETEROW = "RD";
			// public static final String KEY_TOOL_DELETECOL = "CD";
		}
		return editorPane;
	}
}
