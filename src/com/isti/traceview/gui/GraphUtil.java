package com.isti.traceview.gui;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.isti.traceview.TraceView;
import com.isti.traceview.gui.FileChooser;

/**
 * Sets of general procedures used for gui construction
 */
public class GraphUtil {
	private static Logger lg = Logger.getLogger(GraphUtil.class);
	private static final double default_compression = 0.9; // the compression level (0.0-1.0).

	/**
	 * Creates a JButton using <i>imageName</i>, or if <i>imageName</i> is unavailable, use
	 * <i>alternateText</i>
	 * 
	 * @param imageName
	 *            the mage to place in the button
	 * @param alternateText
	 *            the alternate text to use if image unavailable
	 * @return a graphical JButton
	 * @see JButton
	 */
	static public JButton createNoURLGraphicalButton(String imageName, String alternateText) {

		return new JButton(alternateText, new ImageIcon(imageName));

	}

	/**
	 * Creates a JButton using <i>imageName</i>, or if <i>imageName</i> is unavailable, use
	 * <i>alternateText</i>
	 * 
	 * @param imageName
	 *            the mage to place in the button
	 * @param alternateText
	 *            the alternate text to use if image unavailable
	 * @return a graphical JButton
	 * @see JButton
	 */
	static public JButton createGraphicalButton(String imageName, String alternateText) {

		try {
			JButton tb = new JButton(new ImageIcon(ClassLoader.getSystemResource(imageName)));
			return tb;
		} catch (Exception e) {
			return new JButton(alternateText);
		}
	}

	/**
	 * Creates a JLabel using <i>imageName</i>, or if <i>imageName</i> is unavailable, use
	 * <i>alternateText</i>
	 * 
	 * @param imageName
	 *            the mage to place in the button
	 * @param alternateText
	 *            the alternate text to use if image unavailable
	 * @return a graphical JLabel
	 * @see JButton
	 */
	static public JLabel createGraphicalLabel(String imageName, String alternateText) {

		try {

			BufferedImage bi = javax.imageio.ImageIO.read(ClassLoader.getSystemResource("images/" + imageName));
			int w = (int) (bi.getWidth() * 0.75);
			int h = (int) (bi.getHeight() * 0.75);
			Image tbi = bi.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING);
			JLabel tmplab = new JLabel(new ImageIcon(tbi));
			// return new JLabel(new ImageIcon(ClassLoader.getSystemResource("images/" +
			// imageName)));
			return tmplab;
		} catch (Exception e) {
			return new JLabel(alternateText);
		}
	}

	/**
	 * Select HTML report file, then adds to HTML report image with text comments
	 * 
	 * @param panel
	 *            panel to get image
	 * @param defaultDir directory to open dialog in
	 */
	public static File addToHTML(JPanel panel, String defaultDir) {
		File file = null;
		final FileChooser fc = new FileChooser(FileChooser.Type.HTML);
		if (defaultDir != null) {
			fc.setCurrentDirectory(new File(defaultDir));
		}
		if (fc.showSaveDialog(TraceView.getFrame()) == JFileChooser.APPROVE_OPTION) {
			String filename = fc.getSelectedFile().getPath();
			if (!(filename.toLowerCase().endsWith(".html") || filename.toLowerCase().endsWith(".htm"))) {
				filename = filename + ".html";
			}
			try {
				file = new File(filename);
				if (!file.exists()) {
					file.createNewFile();
					RandomAccessFile raf = new RandomAccessFile(file, "rw");
					raf.writeBytes(TraceView.getConfiguration().getDefaultHTMLPattern());
					raf.close();
				}
				CommentDialog cd = new CommentDialog(TraceView.getFrame());
				String comment = cd.getCommentText();
				SimpleDateFormat df = new SimpleDateFormat("yyyyDDDHHmmss");
				String pict_filename = df.format(new Date()) + ".png";
				GraphUtil.exportGraphics(panel, file.getParent() + File.separator + pict_filename, "PNG", default_compression);
				FileInputStream fis = new FileInputStream(file);
				FileChannel fch = fis.getChannel();
				MappedByteBuffer mbf = fch.map(FileChannel.MapMode.READ_ONLY, 0, fch.size());
				byte[] barray = new byte[(int) (fch.size())];
				mbf.get(barray);
				String lines = new String(barray); // one big string
				int insertPlace = lines.toLowerCase().indexOf("</body>");
				String insertText = "\n<img border=0 src=" + pict_filename + "><h3>" + pict_filename + "</h3>" + (comment == null ? "" : comment);
				RandomAccessFile raf = new RandomAccessFile(file, "rw");
				raf.seek(insertPlace);
				raf.writeBytes(insertText + "</body></html>");
				raf.close();
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return file;
	}

	/**
	 * Selects file and image format, then stores panel's graphics as image in file
	 * 
	 * @param panel
	 *            panel to save
	 * @param defaultDir directory to open dialog in
	 */
	public static File saveGraphics(JPanel panel, String defaultDir) {
		String encFormat = null;
		File selectedFile = null;
		final FileChooser fc = new FileChooser(FileChooser.Type.GRAPH);
		if (defaultDir != null) {
			fc.setCurrentDirectory(new File(defaultDir));
		}
		if (fc.showSaveDialog(TraceView.getFrame()) == JFileChooser.APPROVE_OPTION) {
			selectedFile = fc.getSelectedFile();
			String filename = selectedFile.getPath();
			if (fc.getFileFilter().getDescription().equals("PNG Images")) {
				encFormat = "PNG";
				if (!filename.toLowerCase().endsWith(".png")) {
					filename = filename + ".png";
				}
			} else if (fc.getFileFilter().getDescription().equals("JPEG Images")) {
				encFormat = "JPG";
				if (!(filename.toLowerCase().endsWith(".jpeg") || filename.toLowerCase().endsWith(".jpg"))) {
					filename = filename + ".jpg";
				}
			} else if (fc.getFileFilter().getDescription().equals("BMP Images")) {
				encFormat = "BMP";
				if (!filename.toLowerCase().endsWith(".bmp")) {
					filename = filename + ".bmp";
				}
			}
			GraphUtil.exportGraphics(panel, filename, encFormat, default_compression);
		}
		return selectedFile;
	}

	/**
	 * Stores panel's graphics as image in file
	 * 
	 * @param panel
	 *            panel to save
	 * @param filename
	 *            name of image file
	 * @param encFormat
	 *            encoding format
	 * @param compression
	 *            encoding compression
	 */
	public static void exportGraphics(JPanel panel, String filename, String encFormat, double compression) {
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(filename));
			BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = image.createGraphics();
			panel.paintAll(g2);
			g2.dispose();
			ImageIO.write(image, encFormat, out);
		} catch (FileNotFoundException e1) {
			lg.error("Can't export graphics to file " + filename + ": " + e1);
		} catch (IOException e1) {
			lg.error("Can't export graphics to file " + filename + ": " + e1);
		} finally {
			try {
				out.close();
			} catch (IOException e1) {
				// do nothing
			}
		}
	}
}
