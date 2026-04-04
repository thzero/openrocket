package info.openrocket.swing.gui.dialogs;

import java.awt.Component;

import javax.swing.JOptionPane;

/**
 * Utility class for showing simple single-message dialogs consistently.
 * <p>
 * Use {@link WarningDialog} or {@link ErrorWarningDialog} when displaying
 * structured {@link info.openrocket.core.logging.WarningSet} /
 * {@link info.openrocket.core.logging.ErrorSet} content.
 */
public abstract class MessageDialog {

	public static void showInfo(Component parent, String message, String title) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
	}

	public static void showWarning(Component parent, String message, String title) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
	}

	public static void showError(Component parent, String message, String title) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
	}
}
