/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.awt.GraphicsEnvironment;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.document.ObjectEvent;
import com.microproject.preference.GlobalPreferences;
import com.microproject.preference.ConfigurationFile;
import com.microproject.configuration.Settings;
import com.microproject.pm.graphic.gantt.MilestoneShapeChoice;
import com.microproject.testsupport.DialogLayoutAssertions;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.shell.AutoSaveControl;
import com.microproject.util.FlatLafSupport;

/** Visible coverage for the user preference controls added to the desktop dialog. */
class PreferencesDialogGuiAcceptanceTest {
	private GlobalPreferences testPreferences;
	private boolean originalDarkTheme;
	private String originalDatePattern;
	private String originalDateTimePattern;
	private String originalCurrencyCode;
	private String originalMilestoneShape;
	private java.util.prefs.Preferences localePreferences;
	private String originalLocalePreference;
	private String originalLanguages;

	@AfterEach
	void closeDialogs() throws Exception {
		GlobalPreferences preferences = testPreferences;
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows())
				if (window instanceof PreferencesDialogBox || window instanceof LocaleDialog)
					window.dispose();
			if (preferences != null) {
				preferences.setDarkTheme(originalDarkTheme);
				preferences.setDatePattern(originalDatePattern);
				preferences.setDateTimePattern(originalDateTimePattern);
				preferences.setCurrencyCode(originalCurrencyCode);
				preferences.setDefaultMilestoneShape(originalMilestoneShape);
				FlatLafSupport.initialize();
			}
			if (localePreferences != null) {
				if (originalLocalePreference == null) localePreferences.remove("locale");
				else localePreferences.put("locale", originalLocalePreference);
				try { localePreferences.flush(); } catch (java.util.prefs.BackingStoreException error) { throw new RuntimeException(error); }
			}
			if (originalLanguages != null) Settings.LANGUAGES = originalLanguages;
		});
		testPreferences = null;
	}

	@Test
	void languageLocaleTabHelpApplyAndCancelUseThePreferencesDialog() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		localePreferences = java.util.prefs.Preferences.userNodeForPackage(ConfigurationFile.class);
		originalLanguages = Settings.LANGUAGES;
		Settings.LANGUAGES = "default;de";
		originalLocalePreference = localePreferences.get("locale", null);
		localePreferences.put("locale", "default");
		localePreferences.flush();
		testPreferences = new GlobalPreferences();
		originalDarkTheme = testPreferences.isDarkTheme();
		originalDatePattern = testPreferences.getDatePattern();
		originalDateTimePattern = testPreferences.getDateTimePattern();
		originalCurrencyCode = testPreferences.getCurrencyCode();
		originalMilestoneShape = testPreferences.getDefaultMilestoneShape();
		AtomicInteger restartRequests = new AtomicInteger();
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		SwingUtilities.invokeLater(() -> PreferencesDialogBox.showDialog(null, testPreferences,
			restartRequests::incrementAndGet, null, true));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Preferences dialog did not open on Language and locale");
		PreferencesDialogBox dialog = findDialog();
		JTabbedPane tabs = findNamedComponent(dialog, JTabbedPane.class, null);
		assertTrue(tabs != null, "Preferences dialog must expose its settings tabs");
		assertEquals(UsabilityStrings.text("preferences.languageLocale"), tabs.getTitleAt(1));
		assertEquals(1, tabs.getSelectedIndex(), "Ribbon locale route must select Language and locale");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(tabs, "Preferences language and locale tab");
		DialogLayoutAssertions.assertWithinUsableScreen(dialog, "Preferences language and locale tab");
		JComboBox<?> language = findComboWithItem(dialog, "de");
		JComboBox<?> country = findComboWithItem(dialog, "Germany");
		assertTrue(language != null && country != null, "Language and locale tab must expose language and country selectors");
		AbstractButton help = findButton(dialog, com.microproject.menu.MenuManager.getMenuString("Help.text"));
		assertTrue(help != null && help.isShowing() && help.isEnabled(), "Locale Help route must be visible and enabled");
		selectCombo(robot, language, "de");
		selectCombo(robot, country, "Germany");
		click(robot, findButton(dialog, UsabilityStrings.text("preferences.cancel")));
		GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Cancel");
		assertEquals("default", localePreferences.get("locale", "missing"), "Cancel must leave locale unchanged");
		assertEquals(0, restartRequests.get(), "Cancel must not request an application restart");

		SwingUtilities.invokeLater(() -> PreferencesDialogBox.showDialog(null, testPreferences,
			restartRequests::incrementAndGet, null, true));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Preferences dialog did not reopen");
		PreferencesDialogBox appliedDialog = findDialog();
		language = findComboWithItem(appliedDialog, "de");
		country = findComboWithItem(appliedDialog, "Germany");
		selectCombo(robot, language, "de");
		selectCombo(robot, country, "Germany");
		click(robot, findButton(appliedDialog, UsabilityStrings.text("preferences.apply")));
		GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Apply");
		assertEquals("de_DE", localePreferences.get("locale", "missing"), "Apply must persist the selected language and country");
		assertEquals(1, restartRequests.get(), "Applying a locale change must request application restart");
	}

	@Test
	void preferencesDialogVisiblyOffersThemeAutomaticGridColorAndReset() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		AtomicInteger appliedInterval = new AtomicInteger(-1);
		testPreferences = new GlobalPreferences();
		originalDarkTheme = testPreferences.isDarkTheme();
		originalDatePattern = testPreferences.getDatePattern();
		originalDateTimePattern = testPreferences.getDateTimePattern();
		originalCurrencyCode = testPreferences.getCurrencyCode();
		originalMilestoneShape = testPreferences.getDefaultMilestoneShape();
		testPreferences.setDarkTheme(false);
		testPreferences.setDatePattern("");
		testPreferences.setDateTimePattern("");
		testPreferences.setCurrencyCode("");
		SwingUtilities.invokeLater(() -> PreferencesDialogBox.showDialog(null, testPreferences, null,
			new AutoSaveControl() {
				private int intervalMinutes = DEFAULT_INTERVAL_MINUTES;
				public boolean isEnabled() { return true; }
				public void setEnabled(boolean enabled) { }
				public int getIntervalMinutes() { return intervalMinutes; }
				public void setIntervalMinutes(int minutes) { intervalMinutes = minutes; appliedInterval.set(minutes); }
			}));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Preferences dialog did not open");
		PreferencesDialogBox dialog = findDialog();
		SwingUtilities.invokeAndWait(() -> {
			dialog.setAlwaysOnTop(true);
			dialog.toFront();
			dialog.requestFocus();
		});
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.delay(300);
		robot.waitForIdle();
		assertTrue(hasButton(dialog, UsabilityStrings.text("preferences.gridColorAutomatic")));
		assertTrue(hasButton(dialog, UsabilityStrings.text("preferences.ganttBarColorAutomatic")));
		assertTrue(hasButton(dialog, UsabilityStrings.text("preferences.reset")));
		assertTrue(hasComboItem(dialog, UsabilityStrings.text("preferences.ganttBarTextResourceNames")));
		assertTrue(hasComboItem(dialog, UsabilityStrings.text("preferences.ganttBarTextTaskName")));
		assertTrue(hasComboItem(dialog, UsabilityStrings.text("preferences.ganttBarTextPositionAutomatic")));
		assertTrue(hasComboItem(dialog, UsabilityStrings.text("preferences.ganttBarTextPositionRight")));
		assertTrue(hasComboItem(dialog, UsabilityStrings.text("preferences.ganttBarTextPositionLeft")));
		assertTrue(hasComboItem(dialog, MilestoneShapeChoice.DIAMOND.toString()));
		JComboBox<?> milestoneShape = findNamedComponent(dialog, JComboBox.class, "preferencesMilestoneShape");
		assertTrue(milestoneShape != null, "Preferences must expose a default milestone shape");
		assertTrue(hasComboItem(dialog, UsabilityStrings.text("preferences.themeLight")));
		assertTrue(hasComboItem(dialog, UsabilityStrings.text("preferences.themeDark")));
		JComboBox<?> theme = findNamedComponent(dialog, JComboBox.class, "preferencesTheme");
		assertTrue(theme != null, "Preferences must expose the theme choice");
		assertTrue(findNamedComponent(dialog, JLabel.class, "preferencesThemeRestartNotice") == null,
			"Theme changes apply live and must not announce a restart");
		JTextField datePattern = findNamedComponent(dialog, JTextField.class, "preferencesDatePattern");
		JTextField dateTimePattern = findNamedComponent(dialog, JTextField.class, "preferencesDateTimePattern");
		JTextField currencyCode = findNamedComponent(dialog, JTextField.class, "preferencesCurrencyCode");
		JLabel formatRestartNotice = findNamedComponent(dialog, JLabel.class, "preferencesDateFormatRestartRequired");
		assertTrue(datePattern != null && dateTimePattern != null && currencyCode != null && formatRestartNotice != null,
			"Preferences must expose date/currency formats and their restart notice");
		JSpinner interval = findNamedComponent(dialog, JSpinner.class, "preferencesAutoSaveInterval");
		assertTrue(interval != null, "Preferences must expose the auto-recovery interval");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog.getContentPane(), "Preferences dialog");
		DialogLayoutAssertions.assertWithinUsableScreen(dialog, "Preferences dialog");
		capture(robot, dialog, "preferences-theme-selection.png");
		enterText(robot, datePattern, "dd.MM.yyyy");
		enterText(robot, dateTimePattern, "dd.MM.yyyy HH:mm");
		enterText(robot, currencyCode, "EUR");
		GuiAcceptanceSupport.await(formatRestartNotice::isVisible,
			"Changing date formats must tell users to restart the application");
		JTextField intervalEditor = ((JSpinner.DefaultEditor) interval.getEditor()).getTextField();
		click(robot, intervalEditor);
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A); robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_1); robot.keyRelease(KeyEvent.VK_1);
		robot.keyPress(KeyEvent.VK_2); robot.keyRelease(KeyEvent.VK_2);
		robot.keyPress(KeyEvent.VK_ENTER); robot.keyRelease(KeyEvent.VK_ENTER);
		click(robot, theme);
		robot.keyPress(KeyEvent.VK_END); robot.keyRelease(KeyEvent.VK_END);
		robot.keyPress(KeyEvent.VK_ENTER); robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> UsabilityStrings.text("preferences.themeDark").equals(selectedItem(theme)),
			"Robot did not select the dark theme");
		click(robot, milestoneShape);
		robot.keyPress(KeyEvent.VK_END); robot.keyRelease(KeyEvent.VK_END);
		robot.keyPress(KeyEvent.VK_ENTER); robot.keyRelease(KeyEvent.VK_ENTER);
		click(robot, findButton(dialog, UsabilityStrings.text("preferences.apply")));
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "Preferences dialog did not close after Apply");
		assertEquals(12, appliedInterval.get(), "Apply must persist the entered recovery interval");
		assertTrue(testPreferences.isDarkTheme(), "Apply must persist the selected dark theme");
		GuiAcceptanceSupport.await(com.microproject.util.FlatUiSupport::isDarkTheme,
			"Apply must switch the running application to the dark theme without a restart");
		com.microproject.ui.theme.ThemeManager.applyTheme(false);
		assertEquals("dd.MM.yyyy", testPreferences.getDatePattern(), "Apply must persist the date-only format");
		assertEquals("dd.MM.yyyy HH:mm", testPreferences.getDateTimePattern(), "Apply must persist the date-time format");
		assertEquals("EUR", testPreferences.getCurrencyCode(), "Apply must persist the currency override");
		assertEquals(MilestoneShapeChoice.TRIANGLE_DOWN.getShapeName(), testPreferences.getDefaultMilestoneShape(),
			"Apply must persist the default milestone shape");
	}

	@Test
	void darkThemeStartupUsesDarkLookAndFeelAndKeepsPreferencesUsable() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		testPreferences = new GlobalPreferences();
		originalDarkTheme = testPreferences.isDarkTheme();
		originalDatePattern = testPreferences.getDatePattern();
		originalDateTimePattern = testPreferences.getDateTimePattern();
		originalCurrencyCode = testPreferences.getCurrencyCode();
		originalMilestoneShape = testPreferences.getDefaultMilestoneShape();
		testPreferences.setDarkTheme(true);
		FlatLafSupport.initialize();
		SwingUtilities.invokeLater(() -> PreferencesDialogBox.showDialog(null, testPreferences));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Dark-theme Preferences dialog did not open");
		PreferencesDialogBox dialog = findDialog();
		SwingUtilities.invokeAndWait(() -> {
			dialog.setAlwaysOnTop(true);
			dialog.toFront();
			dialog.requestFocus();
		});
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.delay(300);
		robot.waitForIdle();
		assertEquals("com.formdev.flatlaf.FlatDarkLaf", javax.swing.UIManager.getLookAndFeel().getClass().getName());
		assertEquals(new java.awt.Color(0x25272B), javax.swing.UIManager.getColor("Panel.background"));
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog.getContentPane(), "Dark Preferences dialog");
		DialogLayoutAssertions.assertWithinUsableScreen(dialog, "Dark Preferences dialog");
		capture(robot, dialog, "preferences-dark-theme.png");
	}

	@Test
	void applyRefreshesOpenWindowsOnlyWhenTheFontChangedAndThenOnlyOnce() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		testPreferences = new GlobalPreferences();
		originalDarkTheme = testPreferences.isDarkTheme();
		originalDatePattern = testPreferences.getDatePattern();
		originalDateTimePattern = testPreferences.getDateTimePattern();
		originalCurrencyCode = testPreferences.getCurrencyCode();
		originalMilestoneShape = testPreferences.getDefaultMilestoneShape();
		String originalFontFamily = testPreferences.getFontFamily();
		int originalFontSize = testPreferences.getFontSize();
		java.util.concurrent.atomic.AtomicInteger refreshes = new java.util.concurrent.atomic.AtomicInteger();
		AtomicInteger updates = new AtomicInteger();
		ObjectEvent.Listener listener = event -> {
			if (event.isUpdate())
				updates.incrementAndGet();
		};
		javax.swing.JFrame[] frame = new javax.swing.JFrame[1];
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		try {
			// The first font enumeration of a JVM is slow; keep it out of the dialog-open deadline.
			GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
			SwingUtilities.invokeAndWait(FlatLafSupport::initialize);
			SwingUtilities.invokeAndWait(() -> {
				frame[0] = new javax.swing.JFrame("Preferences refresh probe");
				frame[0].setContentPane(new javax.swing.JPanel() {
					private static final long serialVersionUID = 1L;
					@Override public void updateUI() {
						super.updateUI();
						refreshes.incrementAndGet();
					}
				});
				frame[0].setSize(240, 120);
				frame[0].setVisible(true);
			});
			robot.waitForIdle();
			int baseline = refreshes.get();
			testPreferences.addObjectListener(listener);

			SwingUtilities.invokeLater(() -> PreferencesDialogBox.showDialog(null, testPreferences));
			GuiAcceptanceSupport.await(() -> findDialog() != null, "Preferences dialog did not open");
			click(robot, findButton(findDialog(), UsabilityStrings.text("preferences.apply")));
			GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Apply");
			robot.waitForIdle();
			assertEquals(originalFontFamily, testPreferences.getFontFamily(),
				"Apply without a font change must keep the stored font family");
			assertEquals(0, updates.get(), "Apply without changes must not send preference update events");
			assertEquals(baseline, refreshes.get(), "Apply without a font change must not rebuild open windows");

			SwingUtilities.invokeLater(() -> PreferencesDialogBox.showDialog(null, testPreferences));
			GuiAcceptanceSupport.await(() -> findDialog() != null, "Preferences dialog did not reopen");
			PreferencesDialogBox dialog = findDialog();
			@SuppressWarnings("unchecked")
			JComboBox<String> font = findNamedComponent(dialog, JComboBox.class, "preferencesFontFamily");
			assertTrue(font != null, "Preferences must expose the font family selector");
			String[] chosen = new String[1];
			SwingUtilities.invokeAndWait(() -> {
				assertEquals(UsabilityStrings.text("preferences.fontAutomatic"), font.getItemAt(0),
					"The font list must start with the system default entry");
				for (int index = 1; index < font.getItemCount(); index++) {
					if (!font.getItemAt(index).equals(originalFontFamily)) {
						chosen[0] = font.getItemAt(index);
						font.setSelectedIndex(index);
						return;
					}
				}
			});
			Assumptions.assumeTrue(chosen[0] != null, "An installed font family is required");
			click(robot, findButton(dialog, UsabilityStrings.text("preferences.apply")));
			GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after the font change");
			robot.waitForIdle();
			assertEquals(chosen[0], testPreferences.getFontFamily(), "Apply must store the selected font family");
			assertEquals(baseline + 1, refreshes.get(), "A font change must rebuild open windows exactly once");
		} finally {
			testPreferences.removeObjectListener(listener);
			SwingUtilities.invokeAndWait(() -> {
				testPreferences.setFontFamily(originalFontFamily);
				testPreferences.setFontSize(originalFontSize);
				FlatLafSupport.applyUserFontPreference(originalFontFamily, originalFontSize);
				if (frame[0] != null)
					frame[0].dispose();
			});
		}
	}

	private static PreferencesDialogBox findDialog() {
		for (Window window : Window.getWindows())
			if (window instanceof PreferencesDialogBox dialog && dialog.isVisible())
				return dialog;
		return null;
	}

	private static boolean hasButton(java.awt.Container container, String text) {
		for (java.awt.Component child : container.getComponents()) {
			if (child instanceof AbstractButton button && text.equals(button.getText())) return true;
			if (child instanceof java.awt.Container nested && hasButton(nested, text)) return true;
		}
		return false;
	}

	private static AbstractButton findButton(java.awt.Container container, String text) {
		for (java.awt.Component child : container.getComponents()) {
			if (child instanceof AbstractButton button && text.equals(button.getText())) return button;
			if (child instanceof java.awt.Container nested) {
				AbstractButton found = findButton(nested, text);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static void click(Robot robot, Component component) throws Exception {
		Point[] center = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			Point location = component.getLocationOnScreen();
			center[0] = new Point(location.x + component.getWidth() / 2, location.y + component.getHeight() / 2);
		});
		robot.mouseMove(center[0].x, center[0].y);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static void enterText(Robot robot, JTextField field, String value) throws Exception {
		click(robot, field);
		java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
			new java.awt.datatransfer.StringSelection(value), null);
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A); robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_V);
		robot.keyRelease(KeyEvent.VK_V); robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
	}

	private static <T extends java.awt.Component> T findNamedComponent(java.awt.Container container, Class<T> type,
			String name) {
		for (java.awt.Component child : container.getComponents()) {
			if (type.isInstance(child) && java.util.Objects.equals(name, child.getName())) return type.cast(child);
			if (child instanceof java.awt.Container nested) {
				T found = findNamedComponent(nested, type, name);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static boolean hasComboItem(java.awt.Container container, String text) {
		for (java.awt.Component child : container.getComponents()) {
			if (child instanceof javax.swing.JComboBox<?> combo)
				for (int index = 0; index < combo.getItemCount(); index++)
					if (text.equals(String.valueOf(combo.getItemAt(index)))) return true;
			if (child instanceof java.awt.Container nested && hasComboItem(nested, text)) return true;
		}
		return false;
	}

	private static JComboBox<?> findComboWithItem(java.awt.Container container, String item) {
		for (java.awt.Component child : container.getComponents()) {
			if (child instanceof JComboBox<?> combo)
				for (int index = 0; index < combo.getItemCount(); index++)
					if (item.equals(String.valueOf(combo.getItemAt(index)))) return combo;
			if (child instanceof java.awt.Container nested) {
				JComboBox<?> found = findComboWithItem(nested, item);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static void selectCombo(Robot robot, JComboBox<?> combo, String item) throws Exception {
		click(robot, combo);
		for (char character : item.toCharArray()) {
			int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);
			robot.keyPress(keyCode); robot.keyRelease(keyCode);
		}
		robot.keyPress(KeyEvent.VK_ENTER); robot.keyRelease(KeyEvent.VK_ENTER);
		robot.waitForIdle();
		assertEquals(item, String.valueOf(selectedItem(combo)), "Robot did not select " + item);
	}

	private static Object selectedItem(JComboBox<?> combo) {
		Object[] value = new Object[1];
		try {
			SwingUtilities.invokeAndWait(() -> value[0] = combo.getSelectedItem());
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
		return value[0];
	}

	private static void capture(Robot robot, JDialog dialog, String fileName) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(dialog.getRootPane().getLocationOnScreen(), dialog.getRootPane().getSize()));
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		javax.imageio.ImageIO.write(robot.createScreenCapture(bounds[0]), "png",
			directory.resolve(fileName).toFile());
	}
}
