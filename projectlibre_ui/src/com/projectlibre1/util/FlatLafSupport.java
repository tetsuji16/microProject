package com.projectlibre1.util;

import java.awt.Font;
import java.util.Locale;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * Centralized FlatLaf bootstrap for the desktop application.
 */
public final class FlatLafSupport {
	private static boolean initialized;

	private FlatLafSupport() {
	}

	public static synchronized void initialize() {
		try {
			Environment.setNewLook(true);

			JFrame.setDefaultLookAndFeelDecorated(false);
			JDialog.setDefaultLookAndFeelDecorated(false);

			UiServices.setFileChooserProvider(new SwingFileChooserProvider());
			FlatLightLaf.setup();
			Environment.setNewLaf(isFlatLafLookAndFeel());

			Font defaultFont = createDefaultFont();
			if (defaultFont != null) {
				applyUIFontDefaults(defaultFont);
				applyEnvironmentFonts(defaultFont);
			}
			FlatUiTheme.installIntoUIManager();
			UIManager.put("Component.hideMnemonics", Boolean.FALSE);
		} catch (Exception ex) {
			System.err.println("Failed to initialize LaF");
			ex.printStackTrace(System.err);
		} finally {
			initialized = true;
		}
	}

	public static synchronized void ensureInitialized() {
		if (!initialized) {
			initialize();
		}
	}

	private static Font createDefaultFont() {
		String fontName;
		if (isWindows()) {
			fontName = firstAvailable("Yu Gothic UI", "Yu Gothic", "Meiryo", "Segoe UI", "SansSerif");
		} else if (isMac()) {
			fontName = firstAvailable("Hiragino Sans", "Hiragino Kaku Gothic ProN", "Hiragino Kaku Gothic Pro", "SansSerif");
		} else if (isLinux()) {
			fontName = firstAvailable("Noto Sans JP", "Noto Sans CJK JP", "Droid Sans Fallback", "SansSerif");
		} else {
			fontName = firstAvailable("SansSerif");
		}
		return fontName == null ? null : new Font(fontName, Font.PLAIN, 12);
	}

	private static void applyUIFontDefaults(Font baseFont) {
		FontUIResource plain = new FontUIResource(baseFont);
		FontUIResource bold = new FontUIResource(baseFont.deriveFont(Font.BOLD));
		UIManager.put("defaultFont", plain);
		UIManager.put("Label.font", plain);
		UIManager.put("Button.font", plain);
		UIManager.put("CheckBox.font", plain);
		UIManager.put("RadioButton.font", plain);
		UIManager.put("ToggleButton.font", plain);
		UIManager.put("Menu.font", plain);
		UIManager.put("MenuItem.font", plain);
		UIManager.put("PopupMenu.font", plain);
		UIManager.put("ToolTip.font", plain);
		UIManager.put("ComboBox.font", plain);
		UIManager.put("List.font", plain);
		UIManager.put("Tree.font", plain);
		UIManager.put("Table.font", plain);
		UIManager.put("TableHeader.font", bold);
		UIManager.put("TextField.font", plain);
		UIManager.put("PasswordField.font", plain);
		UIManager.put("TextArea.font", plain);
		UIManager.put("TextPane.font", plain);
		UIManager.put("EditorPane.font", plain);
		UIManager.put("FormattedTextField.font", plain);
		UIManager.put("Spinner.font", plain);
		UIManager.put("Slider.font", plain);
		UIManager.put("TabbedPane.font", plain);
		UIManager.put("ToolBar.font", plain);
		UIManager.put("OptionPane.font", plain);
		UIManager.put("TitledBorder.font", bold);
	}

	private static void applyEnvironmentFonts(Font baseFont) {
		String family = baseFont.getFamily();
		Environment.setFont(family, Environment.DEFAULT_FONT);
		Environment.setFont(family + " PLAIN 11", Environment.NETWORK_FONT);
		Environment.setFont(family + " BOLD 11", Environment.GANTT_ANNOTATIONS_FONT);
	}

	private static String firstAvailable(String... candidates) {
		String[] available = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		for (String candidate : candidates) {
			for (String family : available) {
				if (family.equals(candidate)) {
					return candidate;
				}
			}
		}
		return candidates.length > 0 ? candidates[candidates.length - 1] : null;
	}

	private static boolean isWindows() {
		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		return osName.startsWith("windows");
	}

	private static boolean isMac() {
		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		return osName.startsWith("mac");
	}

	private static boolean isLinux() {
		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		return osName.startsWith("linux");
	}

	private static boolean isFlatLafLookAndFeel() {
		if (UIManager.getLookAndFeel() == null) {
			return false;
		}
		String className = UIManager.getLookAndFeel().getClass().getName();
		return className != null && className.startsWith("com.formdev.flatlaf.");
	}
}
