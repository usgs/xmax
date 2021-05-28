package com.isti.traceview.gui;

import com.isti.traceview.source.SourceFile;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * Customized file chooser to select files handled by TraceView
 * 
 * @author Max Kokoulin
 */
public class FileChooser extends JFileChooser {

	private static final long serialVersionUID = 1L;

	/**
	 * Enumeration for file types handled by this file chooser
	 */
	public enum Type {
		ALL, GRAPH, HTML, XML, ASCII, MSEED, SAC
	}

	public FileChooser(Type type) {
		setFileSelectionMode(JFileChooser.FILES_ONLY);
		setAcceptAllFileFilterUsed(false);
		switch (type) {
		case GRAPH:
			addChoosableFileFilter(new BMPFilter());
			addChoosableFileFilter(new PNGFilter());
			addChoosableFileFilter(new JPEGFilter());
			setDialogTitle("Graphics export");
			break;
		case HTML:
			setFileFilter(new HTMLFilter());
			setDialogTitle("Export to HTML");
			break;
		case XML:
			setFileFilter(new XMLFilter());
			setDialogTitle("Export to XML");
			break;
		case ASCII:
			setFileFilter(new TXTFilter());
			setDialogTitle("Export to ASCII");
			break;
		case MSEED:
			setFileFilter(new MSEEDFilter());
			setDialogTitle("Export to MSEED");
			break;
		case SAC:
			setFileFilter(new SACFilter());
			setDialogTitle("Export to SAC");
			break;
		case ALL:
			setDialogTitle("Select file:");
			break;
		}
	}

	private static class PNGFilter extends FileFilter {
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			String extension = SourceFile.getExtension(f);
			if (extension != null) {
				return extension.equals("png") || extension.equals("PNG");
			}
			return false;
		}

		// The description of this filter
		public String getDescription() {
			return "PNG Images";
		}
	}

	private static class JPEGFilter extends FileFilter {
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			String extension = SourceFile.getExtension(f);
			if (extension != null) {
				return extension.equals("jpg") || extension.equals("JPG") || extension.equals("jpeg")
						|| extension.equals("JPEG");
			}
			return false;
		}

		// The description of this filter
		public String getDescription() {
			return "JPEG Images";
		}
	}

	private static class BMPFilter extends FileFilter {
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			String extension = SourceFile.getExtension(f);
			if (extension != null) {
				return extension.equals("bmp") || extension.equals("BMP");
			}
			return false;
		}

		// The description of this filter
		public String getDescription() {
			return "BMP Images";
		}
	}

	private static class HTMLFilter extends FileFilter {
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			String extension = SourceFile.getExtension(f);
			if (extension != null) {
				return extension.equals("html") || extension.equals("HTML") || extension.equals("htm")
						|| extension.equals("HTM");
			}
			return false;
		}

		// The description of this filter
		public String getDescription() {
			return "HTML files";
		}
	}

	private static class XMLFilter extends FileFilter {
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			String extension = SourceFile.getExtension(f);
			if (extension != null) {
				if (extension.equals("xml") || extension.equals("XML")) {
					return true;
				} else {
					return false;
				}
			}
			return false;
		}

		// The description of this filter
		public String getDescription() {
			return "XML files";
		}
	}

	private static class TXTFilter extends FileFilter {
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			String extension = SourceFile.getExtension(f);
			if (extension != null) {
				if (extension.equals("TXT") || extension.equals("txt")) {
					return true;
				} else {
					return false;
				}
			}
			return false;
		}

		// The description of this filter
		public String getDescription() {
			return "ASCII files";
		}
	}

	private static class MSEEDFilter extends FileFilter {
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			String extension = SourceFile.getExtension(f);
			if (extension != null) {
				if (extension.toLowerCase().equals("msd") || extension.toLowerCase().equals("mseed")) {
					return true;
				} else {
					return false;
				}
			}
			return false;
		}

		// The description of this filter
		public String getDescription() {
			return "MSEED files";
		}
	}
	
	private static class SACFilter extends FileFilter {
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			String extension = SourceFile.getExtension(f);
			if (extension != null) {
				if (extension.toLowerCase().equals("sac")) {
					return true;
				} else {
					return false;
				}
			}
			return false;
		}

		// The description of this filter
		public String getDescription() {
			return "SAC files";
		}
	}
}
