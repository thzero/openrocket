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

	// --- Confirmation dialogs ---

	/** YES/NO buttons, question icon. Returns true if user clicked Yes. */
	public static boolean confirmYesNo(Component parent, Object message, String title) {
		return JOptionPane.showConfirmDialog(parent, message, title,
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
	}

	/** YES/NO buttons, warning icon. Returns true if user clicked Yes. Use for destructive actions. */
	public static boolean confirmYesNoWarning(Component parent, Object message, String title) {
		return JOptionPane.showConfirmDialog(parent, message, title,
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
	}

	/** OK/Cancel buttons, question icon. Returns true if user clicked OK. */
	public static boolean confirmOkCancel(Component parent, Object message, String title) {
		return JOptionPane.showConfirmDialog(parent, message, title,
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
	}

	/** OK/Cancel buttons, warning icon. Returns true if user clicked OK. Use for destructive actions. */
	public static boolean confirmOkCancelWarning(Component parent, Object message, String title) {
		return JOptionPane.showConfirmDialog(parent, message, title,
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
	}
}
