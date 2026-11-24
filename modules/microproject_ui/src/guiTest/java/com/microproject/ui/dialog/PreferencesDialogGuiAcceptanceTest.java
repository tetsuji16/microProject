/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
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
import com.microproject.ui.view.gantt.MilestoneShapeChoice;
import com.microproject.ui.testsupport.DialogLayoutAssertions;
import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.shell.AutoSaveControl;
import com.microproject.ui.util.FlatLafSupport;

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
	private Boolean originalShowTimeInDates;

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
				if (originalShowTimeInDates != null)
					com.microproject.options.CalendarOption.getInstance().setShowTimeInDates(originalShowTimeInDates);
				preferences.applyFormatPreferences();
			}
			if (localePreferences != null) {
				if (originalLocalePreference == null) localePreferences.remove("locale");
				else localePreferences.put("locale", originalLocalePreference);
				try { localePreferences.flush(); } catch (java.util.prefs.BackingStoreException error) { throw new RuntimeException(error); }
			}
			if (originalLanguages != null) Settings.LANGUAGES = originalLanguages;
		});
		testPreferences = null;
		originalShowTimeInDates = null;
	}

	@Test
	void initialSizeUsesTheOwnerMonitorInsteadOfThePrimaryOne() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for GraphicsConfiguration coverage.");
		GraphicsConfiguration secondary = new SecondaryScreen();
		Component owner = new Component() {
			@Override public GraphicsConfiguration getGraphicsConfiguration() { return secondary; }
		};
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(secondary);

		Rectangle usable = DialogSizing.usableBounds(owner, null);

		assertEquals(new Rectangle(1000 + insets.left, 80 + insets.top,
			1280 - insets.left - insets.right, 720 - insets.top - insets.bottom), usable);
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
		Robot robot = new com.microproject.ui.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		SwingUtilities.invokeLater(() -> PreferencesDialogBox.showDialog(null, testPreferences,
			restartRequests::incrementAndGet, null, true));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Preferences dialog did not open on Language and locale");
		PreferencesDialogBox dialog = findDialog();
		JList<?> categories = findNamedComponent(dialog, JList.class, "preferencesCategories");
		assertTrue(categories != null, "Preferences dialog must expose its category list");
		assertEquals(UsabilityStrings.text("preferences.language"), categories.getSelectedValue(),
			"Ribbon locale route must select the Language category");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog.getContentPane(),
			"Preferences language and locale tab");
		DialogLayoutAssertions.assertWithinUsableScreen(dialog, "Preferences language and locale tab");
		JComboBox<?> language = findComboWithItem(dialog, "de");
		JComboBox<?> country = findComboWithItem(dialog, "Germany");
		assertTrue(language != null && country != null, "Language and locale tab must expose language and country selectors");
		AbstractButton help = findButton(dialog, com.microproject.ui.menu.MenuManager.getMenuString("Help.text"));
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
		Robot robot = new com.microproject.ui.testsupport.GuiRobot();
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
		assertTrue(datePattern != null && dateTimePattern != null && currencyCode != null,
			"Preferences must expose date/currency formats");
		JSpinner interval = findNamedComponent(dialog, JSpinner.class, "preferencesAutoSaveInterval");
		assertTrue(interval != null, "Preferences must expose the auto-recovery interval");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog.getContentPane(), "Preferences dialog");
		DialogLayoutAssertions.assertWithinUsableScreen(dialog, "Preferences dialog");
		capture(robot, dialog, "preferences-theme-selection.png");
		selectCategory(dialog, "preferences.display");
		for (String radio : new String[] { "preferencesDatePatternCustom", "preferencesDateTimePatternCustom",
			"preferencesCurrencyCodeCustom" })
			click(robot, findNamedComponent(dialog, javax.swing.JRadioButton.class, radio));
		enterText(robot, datePattern, "dd.MM.yyyy");
		enterText(robot, dateTimePattern, "dd.MM.yyyy HH:mm");
		enterText(robot, currencyCode, "EUR");
		assertTrue(findNamedComponent(dialog, JLabel.class, "preferencesDateFormatRestartRequired") == null,
			"Format changes apply live and must not announce a restart");
		openPopup(robot, milestoneShape);
		robot.keyPress(KeyEvent.VK_END); robot.keyRelease(KeyEvent.VK_END);
		robot.keyPress(KeyEvent.VK_ENTER); robot.keyRelease(KeyEvent.VK_ENTER);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> !milestoneShape.isPopupVisible(), "Milestone shape popup did not close");
		selectCategory(dialog, "preferences.save");
		JTextField intervalEditor = ((JSpinner.DefaultEditor) interval.getEditor()).getTextField();
		focusByClick(robot, intervalEditor);
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A); robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_1); robot.keyRelease(KeyEvent.VK_1);
		robot.keyPress(KeyEvent.VK_2); robot.keyRelease(KeyEvent.VK_2);
		robot.keyPress(KeyEvent.VK_ENTER); robot.keyRelease(KeyEvent.VK_ENTER);
		selectCategory(dialog, "preferences.general");
		openPopup(robot, theme);
		robot.keyPress(KeyEvent.VK_END); robot.keyRelease(KeyEvent.VK_END);
		robot.keyPress(KeyEvent.VK_ENTER); robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> UsabilityStrings.text("preferences.themeDark").equals(selectedItem(theme)),
			"Robot did not select the dark theme");
		click(robot, findButton(dialog, UsabilityStrings.text("preferences.apply")));
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "Preferences dialog did not close after Apply");
		assertEquals(12, appliedInterval.get(), "Apply must persist the entered recovery interval");
		assertTrue(testPreferences.isDarkTheme(), "Apply must persist the selected dark theme");
		GuiAcceptanceSupport.await(com.microproject.ui.util.FlatUiSupport::isDarkTheme,
			"Apply must switch the running application to the dark theme without a restart");
		com.microproject.ui.theme.ThemeManager.applyTheme(false);
		assertEquals("dd.MM.yyyy", testPreferences.getDatePattern(), "Apply must persist the date-only format");
		assertEquals("dd.MM.yyyy HH:mm", testPreferences.getDateTimePattern(), "Apply must persist the date-time format");
		assertEquals("EUR", testPreferences.getCurrencyCode(), "Apply must persist the currency override");
		assertEquals(MilestoneShapeChoice.TRIANGLE_DOWN.getShapeName(), testPreferences.getDefaultMilestoneShape(),
			"Apply must persist the default milestone shape");
	}

	@Test
	void formatSettingsSwitchBetweenFollowOsAndCustomAndKeepStoredValuesCompatible() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		testPreferences = new GlobalPreferences();
		originalDarkTheme = testPreferences.isDarkTheme();
		originalDatePattern = testPreferences.getDatePattern();
		originalDateTimePattern = testPreferences.getDateTimePattern();
		originalCurrencyCode = testPreferences.getCurrencyCode();
		originalMilestoneShape = testPreferences.getDefaultMilestoneShape();
		testPreferences.setDatePattern("");
		testPreferences.setDateTimePattern("");
		testPreferences.setCurrencyCode("");
		Robot robot = new com.microproject.ui.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		SwingUtilities.invokeLater(() -> PreferencesDialogBox.showDialog(null, testPreferences));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Preferences dialog did not open");
		PreferencesDialogBox dialog = findDialog();
		selectCategory(dialog, "preferences.display");
		JTextField date = findNamedComponent(dialog, JTextField.class, "preferencesDatePattern");
		javax.swing.JRadioButton dateOs = findNamedComponent(dialog, javax.swing.JRadioButton.class, "preferencesDatePatternFollowOs");
		javax.swing.JRadioButton dateCustom = findNamedComponent(dialog, javax.swing.JRadioButton.class, "preferencesDatePatternCustom");
		assertTrue(dateOs.isSelected() && !date.isEnabled(), "An empty stored value must open as follow-the-OS with a disabled field");
		assertEquals(PreferencesDialogBox.osDatePattern(false), date.getText(), "The OS default pattern must be shown");
		assertEquals(PreferencesDialogBox.osCurrencyCode(),
			findNamedComponent(dialog, JTextField.class, "preferencesCurrencyCode").getText());
		assertTrue(dateOs.getAccessibleContext().getAccessibleName() != null
			&& !dateOs.getAccessibleContext().getAccessibleName().isBlank(), "Radio buttons need accessible names");
		click(robot, dateCustom);
		assertTrue(date.isEnabled(), "Custom must enable the input field");
		enterText(robot, date, "dd.MM.yyyy");
		click(robot, findButton(dialog, UsabilityStrings.text("preferences.apply")));
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "Preferences dialog did not close after Apply");
		assertEquals("dd.MM.yyyy", testPreferences.getDatePattern());
		assertEquals("", testPreferences.getDateTimePattern(), "Untouched formats must stay empty");

		SwingUtilities.invokeLater(() -> PreferencesDialogBox.showDialog(null, testPreferences));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Preferences dialog did not reopen");
		PreferencesDialogBox reopened = findDialog();
		selectCategory(reopened, "preferences.display");
		JTextField reopenedDate = findNamedComponent(reopened, JTextField.class, "preferencesDatePattern");
		assertTrue(findNamedComponent(reopened, javax.swing.JRadioButton.class, "preferencesDatePatternCustom").isSelected(),
			"A stored pattern must reopen as custom");
		assertEquals("dd.MM.yyyy", reopenedDate.getText());
		enterText(robot, reopenedDate, "yyyy/MM/dd");
		javax.swing.JRadioButton reopenedCustom = findNamedComponent(reopened, javax.swing.JRadioButton.class, "preferencesDatePatternCustom");
		click(robot, reopenedCustom);
		assertEquals("yyyy/MM/dd", reopenedDate.getText(), "Clicking the selected Custom radio must keep the edited text");
		javax.swing.JRadioButton reopenedOs = findNamedComponent(reopened, javax.swing.JRadioButton.class, "preferencesDatePatternFollowOs");
		click(robot, reopenedOs);
		assertEquals(PreferencesDialogBox.osDatePattern(false), reopenedDate.getText());
		click(robot, reopenedOs);
		click(robot, reopenedCustom);
		assertEquals("yyyy/MM/dd", reopenedDate.getText(), "Custom -> Follow OS -> Custom must restore the custom text");
		click(robot, reopenedOs);
		assertTrue(!reopenedDate.isEnabled(), "Follow-the-OS must disable the input field");
		click(robot, findButton(reopened, UsabilityStrings.text("preferences.apply")));
		GuiAcceptanceSupport.await(() -> !reopened.isVisible(), "Preferences dialog did not close after Apply");
		assertEquals("", testPreferences.getDatePattern(), "Follow-the-OS must store an empty value");
	}

	@Test
	void categoryListNamesAndOrderFollowMicrosoftProjectOptions() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		testPreferences = new GlobalPreferences();
		originalDarkTheme = testPreferences.isDarkTheme();
		originalDatePattern = testPreferences.getDatePattern();
		originalDateTimePattern = testPreferences.getDateTimePattern();
		originalCurrencyCode = testPreferences.getCurrencyCode();
		originalMilestoneShape = testPreferences.getDefaultMilestoneShape();
		SwingUtilities.invokeLater(() -> PreferencesDialogBox.showDialog(null, testPreferences, null,
			new AutoSaveControl() {
				public boolean isEnabled() { return true; }
				public void setEnabled(boolean enabled) { }
				public int getIntervalMinutes() { return DEFAULT_INTERVAL_MINUTES; }
				public void setIntervalMinutes(int minutes) { }
			}));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Preferences dialog did not open");
		PreferencesDialogBox dialog = findDialog();
		JList<?> categories = findNamedComponent(dialog, JList.class, "preferencesCategories");
		assertTrue(categories != null, "Preferences dialog must expose its category list");
		assertEquals(java.util.List.of(UsabilityStrings.text("preferences.general"),
			UsabilityStrings.text("preferences.display"), UsabilityStrings.text("preferences.save"),
			UsabilityStrings.text("preferences.customizeRibbon"), UsabilityStrings.text("preferences.quickAccess"),
			UsabilityStrings.text("preferences.language")),
			java.util.stream.IntStream.range(0, categories.getModel().getSize())
				.mapToObj(index -> String.valueOf(categories.getModel().getElementAt(index))).toList(),
			"Categories must be General, Display, Save, Customize Ribbon, Quick Access, Language in this order");
		assertEquals(0, categories.getSelectedIndex(), "General must be selected by default");
		assertTrue(categories.getAccessibleContext().getAccessibleName() != null
			&& !categories.getAccessibleContext().getAccessibleName().isBlank(), "Category list needs an accessible name");
		SwingUtilities.invokeAndWait(() -> dialog.dispose());
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
		Robot robot = new com.microproject.ui.testsupport.GuiRobot();
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
		Robot robot = new com.microproject.ui.testsupport.GuiRobot();
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

	@Test
	void customFormatsApplyToTheRunningApplicationOnceAndNotWhenUnchanged() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		prepareFormats();
		AtomicInteger refreshes = new AtomicInteger();
		javax.swing.JFrame[] frame = new javax.swing.JFrame[1];
		Robot robot = new com.microproject.ui.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		try {
			SwingUtilities.invokeAndWait(() -> {
				frame[0] = new javax.swing.JFrame("Format refresh probe");
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
			PreferencesDialogBox dialog = openDisplayPage();
			click(robot, findButton(dialog, UsabilityStrings.text("preferences.apply")));
			GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Apply");
			assertEquals(baseline, refreshes.get(), "Apply without a format change must not rebuild open windows");

			dialog = openDisplayPage();
			click(robot, findNamedComponent(dialog, javax.swing.JRadioButton.class, "preferencesDatePatternCustom"));
			enterText(robot, findNamedComponent(dialog, JTextField.class, "preferencesDatePattern"), "dd.MM.yyyy");
			click(robot, findNamedComponent(dialog, javax.swing.JRadioButton.class, "preferencesCurrencyCodeCustom"));
			enterText(robot, findNamedComponent(dialog, JTextField.class, "preferencesCurrencyCode"), "EUR");
			click(robot, findButton(dialog, UsabilityStrings.text("preferences.apply")));
			GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Apply");
			robot.waitForIdle();
			assertEquals("dd.MM.yyyy", testPreferences.getDatePattern());
			assertEquals("EUR", testPreferences.getCurrencyCode());
			assertEquals(baseline + 1, refreshes.get(), "A format change must rebuild open windows exactly once");
			java.util.Date sample = new java.util.Date(0);
			assertTrue(com.microproject.options.EditOption.getInstance().getDateFormat().format(sample).startsWith("01.01.1970"),
				"The running application must use the new date format without a restart");
			assertTrue(com.microproject.datatype.Money.getMoneyFormatInstance().format(1).contains("\u20ac"),
				"The running application must use the new currency without a restart");
		} finally {
			SwingUtilities.invokeAndWait(() -> {
				if (frame[0] != null)
					frame[0].dispose();
			});
		}
	}

	@Test
	void customDateFormatChangesTheTaskSpreadsheetStartCellWithoutARestart() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		prepareFormats();
		testPreferences.applyFormatPreferences();
		com.microproject.undo.DataFactoryUndoController undo = new com.microproject.undo.DataFactoryUndoController();
		com.microproject.model.task.Project project = com.microproject.model.task.Project.createProject(
			com.microproject.model.resource.ResourcePool.createRourcePool("format-live", undo), undo);
		project.initialize(false, false);
		com.microproject.model.task.NormalTask task = project.createScriptedTask();
		task.setName("Format live");
		task.setStart(com.microproject.util.DateTime.calendarInstance(2026, java.util.Calendar.SEPTEMBER, 7).getTimeInMillis());
		com.microproject.ui.view.spreadsheet.SpreadSheet[] sheet = new com.microproject.ui.view.spreadsheet.SpreadSheet[1];
		javax.swing.JFrame[] frame = new javax.swing.JFrame[1];
		int[] cell = new int[2];
		Robot robot = new com.microproject.ui.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		try {
			SwingUtilities.invokeAndWait(() -> {
				sheet[0] = new com.microproject.ui.view.spreadsheet.SpreadSheet();
				sheet[0].setSpreadSheetCategory(com.microproject.view.configuration.SpreadSheetCategories.taskSpreadsheetCategory);
				com.microproject.ui.view.model.cache.NodeModelCache cache =
					com.microproject.ui.view.model.cache.NodeModelCacheFactory.getInstance().createFilteredCache(
						com.microproject.ui.view.model.cache.NodeModelCacheFactory.createTaskNodeModelCache(project,
							project.getTaskModel()), "format-live-cache", null);
				com.microproject.ui.view.spreadsheet.SpreadSheetUtils.setFieldsAndContext(sheet[0], cache,
					com.microproject.view.configuration.SpreadSheetCategories.taskSpreadsheetCategory,
					"Spreadsheet.Task.formatLive", true);
				com.microproject.ui.view.spreadsheet.SpreadSheetModel model =
					(com.microproject.ui.view.spreadsheet.SpreadSheetModel) sheet[0].getModel();
				com.microproject.node.Node node = (com.microproject.node.Node) cache.getModel().search(task);
				cell[0] = model.findGraphicNodeRow(cache.getGraphicNode(node));
				for (int column = 0; column < model.getColumnCount(); column++) {
					com.microproject.field.Field field = model.getFieldInColumn(column);
					if (field != null && "Field.start".equals(field.getId()))
						cell[1] = sheet[0].convertColumnIndexToView(column);
				}
				frame[0] = new javax.swing.JFrame("Format live probe");
				frame[0].add(new javax.swing.JScrollPane(sheet[0]));
				frame[0].setSize(900, 300);
				frame[0].setVisible(true);
			});
			robot.waitForIdle();
			String pattern = "yyyy-MM-dd'T'";
			Object[] startValue = new Object[1];
			SwingUtilities.invokeAndWait(() -> startValue[0] = sheet[0].getValueAt(cell[0], cell[1]));
			long startMillis = startValue[0] instanceof java.util.Date date ? date.getTime()
				: ((Number) startValue[0]).longValue();
			// The marker keeps the pattern distinct from every locale's default short date.
			String expected = com.microproject.util.DateTime.dateFormatInstance(pattern).format(new java.util.Date(startMillis));
			String before = renderedText(sheet[0], cell[0], cell[1]);
			assertTrue(!expected.equals(before), "The OS default must differ from the custom pattern: " + before);

			PreferencesDialogBox dialog = openDisplayPage();
			click(robot, findNamedComponent(dialog, javax.swing.JRadioButton.class, "preferencesDatePatternCustom"));
			enterText(robot, findNamedComponent(dialog, JTextField.class, "preferencesDatePattern"), pattern);
			click(robot, findButton(dialog, UsabilityStrings.text("preferences.apply")));
			GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Apply");
			robot.waitForIdle();
			GuiAcceptanceSupport.await(() -> {
				try {
					return expected.equals(renderedText(sheet[0], cell[0], cell[1]));
				} catch (Exception error) {
					throw new IllegalStateException(error);
				}
			}, "The task spreadsheet Start cell must show exactly " + expected + " without a restart");
		} finally {
			SwingUtilities.invokeAndWait(() -> {
				if (frame[0] != null)
					frame[0].dispose();
			});
		}
	}

	private static String renderedText(com.microproject.ui.view.spreadsheet.SpreadSheet sheet, int row, int column)
			throws Exception {
		String[] text = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			Component rendered = sheet.prepareRenderer(sheet.getCellRenderer(row, column), row, column);
			text[0] = rendered instanceof JLabel label ? label.getText() : String.valueOf(rendered);
		});
		return text[0];
	}

	@Test
	void categoryListKeepsItsWidthWhenTheDialogIsNarrow() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		prepareFormats();
		SwingUtilities.invokeLater(() -> PreferencesDialogBox.showDialog(null, testPreferences, null, null,
			PreferencesDialogBox.CATEGORY_CUSTOMIZE_RIBBON));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Preferences dialog did not open");
		PreferencesDialogBox dialog = findDialog();
		@SuppressWarnings("unchecked")
		JList<Object> categories = findNamedComponent(dialog, JList.class, "preferencesCategories");
		assertTrue(categories != null, "Preferences dialog must expose its category list");
		SwingUtilities.invokeAndWait(() -> {
			dialog.setSize(976, dialog.getHeight());
			dialog.validate();
			dialog.getContentPane().validate();
		});
		int[] widths = new int[2];
		SwingUtilities.invokeAndWait(() -> {
			int longest = 0;
			for (int i = 0; i < categories.getModel().getSize(); i++) {
				Component cell = categories.getCellRenderer().getListCellRendererComponent(categories,
					categories.getModel().getElementAt(i), i, false, false);
				longest = Math.max(longest, cell.getPreferredSize().width);
			}
			widths[0] = categories.getVisibleRect().width;
			widths[1] = longest;
		});
		assertTrue(widths[0] >= widths[1], "Category list is " + widths[0] + " px wide but needs " + widths[1]);
		selectCategory(dialog, "preferences.display");
		assertEquals(UsabilityStrings.text("preferences.display"), categories.getSelectedValue());
		selectCategory(dialog, "preferences.customizeRibbon");
		for (String name : new String[] { "ribbonCustomizeImportExport", "ribbonCustomizeUp",
			"ribbonCustomizeDown", "ribbonCustomizeNewTab", "ribbonCustomizeReset" }) {
			Component control = findNamedComponent(dialog, Component.class, name);
			assertTrue(control != null, name + " is missing");
			String[] problem = new String[1];
			SwingUtilities.invokeAndWait(() -> {
				javax.swing.JViewport viewport = (javax.swing.JViewport) SwingUtilities.getAncestorOfClass(
					javax.swing.JViewport.class, control);
				Rectangle inView = SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), viewport.getView());
				if (!viewport.getViewRect().contains(inView))
					problem[0] = name + " " + inView + " is outside the page viewport " + viewport.getViewRect();
			});
			assertTrue(problem[0] == null, problem[0]);
		}
	}

	@Test
	void emptyCustomDateFormatIsRejectedAndTheDialogRecovers() throws Exception {
		assertRejectedThenAccepted("preferencesDatePatternCustom", "preferencesDatePattern", "", "yyyy-MM-dd");
		assertEquals("yyyy-MM-dd", testPreferences.getDatePattern());
	}

	@Test
	void invalidCustomDatePatternIsRejectedAndTheDialogRecovers() throws Exception {
		assertRejectedThenAccepted("preferencesDatePatternCustom", "preferencesDatePattern", "yyyy-MM-dd '", "dd/MM/yyyy");
		assertEquals("dd/MM/yyyy", testPreferences.getDatePattern());
	}

	@Test
	void invalidCustomCurrencyCodeIsRejectedAndTheDialogRecovers() throws Exception {
		assertRejectedThenAccepted("preferencesCurrencyCodeCustom", "preferencesCurrencyCode", "NOT-A-CURRENCY", "JPY");
		assertEquals("JPY", testPreferences.getCurrencyCode());
	}

	private void prepareFormats() {
		testPreferences = new GlobalPreferences();
		originalDarkTheme = testPreferences.isDarkTheme();
		originalDatePattern = testPreferences.getDatePattern();
		originalDateTimePattern = testPreferences.getDateTimePattern();
		originalCurrencyCode = testPreferences.getCurrencyCode();
		originalMilestoneShape = testPreferences.getDefaultMilestoneShape();
		testPreferences.setDatePattern("");
		testPreferences.setDateTimePattern("");
		testPreferences.setCurrencyCode("");
		originalShowTimeInDates = com.microproject.options.CalendarOption.getInstance().isShowTimeInDates();
		com.microproject.options.CalendarOption.getInstance().setShowTimeInDates(false);
		testPreferences.applyFormatPreferences();
	}

	private PreferencesDialogBox openDisplayPage() throws Exception {
		SwingUtilities.invokeLater(() -> PreferencesDialogBox.showDialog(null, testPreferences));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Preferences dialog did not open");
		PreferencesDialogBox dialog = findDialog();
		selectCategory(dialog, "preferences.display");
		return dialog;
	}

	/**
	 * Applies an unacceptable Custom value, expects the existing error dialog with the preferences
	 * dialog still open and nothing stored, then corrects the input and expects Apply to succeed.
	 */
	private void assertRejectedThenAccepted(String customRadio, String fieldName, String badValue, String goodValue)
			throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		prepareFormats();
		Robot robot = new com.microproject.ui.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		PreferencesDialogBox dialog = openDisplayPage();
		JTextField field = findNamedComponent(dialog, JTextField.class, fieldName);
		click(robot, findNamedComponent(dialog, javax.swing.JRadioButton.class, customRadio));
		if (badValue.isEmpty()) {
			click(robot, field);
			robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_A);
			robot.keyRelease(KeyEvent.VK_A); robot.keyRelease(KeyEvent.VK_CONTROL);
			robot.keyPress(KeyEvent.VK_DELETE); robot.keyRelease(KeyEvent.VK_DELETE);
			robot.waitForIdle();
		} else {
			enterText(robot, field, badValue);
		}
		click(robot, findButton(dialog, UsabilityStrings.text("preferences.apply")));
		GuiAcceptanceSupport.await(() -> com.microproject.ui.testsupport.ResourceIntervalTableFixture.visibleError() != null,
			"Apply with an invalid Custom value must show the error dialog");
		assertTrue(dialog.isVisible(), "The preferences dialog must stay open after a rejected Apply");
		assertEquals("", testPreferences.getDatePattern(), "A rejected Apply must not store a date pattern");
		assertEquals("", testPreferences.getCurrencyCode(), "A rejected Apply must not store a currency code");
		javax.swing.JOptionPane error = com.microproject.ui.testsupport.ResourceIntervalTableFixture.visibleError();
		assertEquals(UsabilityStrings.text("preferences.invalidFormat"), String.valueOf(error.getMessage()),
			"The rejection must show the invalid-format message");
		SwingUtilities.invokeAndWait(() -> {
			AbstractButton ok = findAnyButton(error);
			if (ok != null)
				ok.doClick();
		});
		GuiAcceptanceSupport.await(() -> com.microproject.ui.testsupport.ResourceIntervalTableFixture.visibleError() == null,
			"The error dialog did not close");
		assertTrue(dialog.isVisible(), "The preferences dialog must stay open after the error is dismissed");
		enterText(robot, field, goodValue);
		click(robot, findButton(dialog, UsabilityStrings.text("preferences.apply")));
		GuiAcceptanceSupport.await(() -> findDialog() == null, "Apply must succeed once the input is corrected");
		assertTrue(com.microproject.ui.testsupport.ResourceIntervalTableFixture.visibleError() == null);
	}

	private static AbstractButton findAnyButton(java.awt.Container container) {
		for (java.awt.Component child : container.getComponents()) {
			if (child instanceof AbstractButton button) return button;
			if (child instanceof java.awt.Container nested) {
				AbstractButton found = findAnyButton(nested);
				if (found != null) return found;
			}
		}
		return null;
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

	/** Opens a combo popup with the mouse, retrying once because the first press can only move focus. */
	private static void openPopup(Robot robot, JComboBox<?> combo) throws Exception {
		for (int attempt = 0; attempt < 3 && !combo.isPopupVisible(); attempt++) {
			click(robot, combo);
			long deadline = System.nanoTime() + 1_000_000_000L;
			while (!combo.isPopupVisible() && System.nanoTime() < deadline) robot.delay(50);
		}
		GuiAcceptanceSupport.await(combo::isPopupVisible, "Combo popup did not open");
	}

	/** Clicks until the field keeps focus; Swing hands focus to the category list when a page is hidden. */
	private static void focusByClick(Robot robot, javax.swing.text.JTextComponent field) throws Exception {
		for (int attempt = 0; attempt < 5; attempt++) {
			click(robot, field);
			robot.delay(400);
			robot.waitForIdle();
			if (field.hasFocus()) return;
		}
		assertTrue(field.hasFocus(), "Field did not keep focus after clicking it");
	}

	private static void selectCategory(PreferencesDialogBox dialog, String key) throws Exception {
		JList<?> categories = findNamedComponent(dialog, JList.class, "preferencesCategories");
		assertTrue(categories != null, "Preferences dialog must expose its category list");
		String name = UsabilityStrings.text(key);
		// A real click, like a user's: the list owns focus afterwards instead of a component on the hidden page.
		Point[] row = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			int index = -1;
			for (int i = 0; i < categories.getModel().getSize(); i++)
				if (name.equals(String.valueOf(categories.getModel().getElementAt(i)))) index = i;
			assertTrue(index >= 0, "Category " + name + " is not listed");
			Rectangle cell = categories.getCellBounds(index, index);
			Point origin = categories.getLocationOnScreen();
			row[0] = new Point(origin.x + cell.x + cell.width / 2, origin.y + cell.y + cell.height / 2);
		});
		Robot clicker = new com.microproject.ui.testsupport.GuiRobot();
		clicker.mouseMove(row[0].x, row[0].y);
		clicker.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		clicker.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		clicker.waitForIdle();
		GuiAcceptanceSupport.await(() -> name.equals(categories.getSelectedValue()), "Category " + name + " was not selected");
		GuiAcceptanceSupport.await(() -> {
			for (int index = 0; index < 6; index++) {
				javax.swing.JScrollPane page = findNamedComponent(dialog, javax.swing.JScrollPane.class,
					"preferencesCategory." + index);
				if (page != null && page.isShowing() && name.equals(page.getAccessibleContext().getAccessibleName()))
					return true;
			}
			return false;
		}, "Category " + name + " page did not become visible");
		// Swing moves focus out of the hidden page asynchronously; let it settle before the next robot input.
		GuiAcceptanceSupport.await(() -> {
			Component owner = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			return owner != null && owner.isShowing();
		}, "Focus did not settle after selecting " + name);
		com.microproject.ui.testsupport.GuiRobot settle = new com.microproject.ui.testsupport.GuiRobot();
		settle.waitForIdle();
		settle.delay(400);
		settle.waitForIdle();
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

	/** A second monitor, offset from the primary one, used to prove owner-based sizing. */
	private static final class SecondaryScreen extends GraphicsConfiguration {
		@Override public GraphicsDevice getDevice() { return null; }
		@Override public ColorModel getColorModel() { return ColorModel.getRGBdefault(); }
		@Override public ColorModel getColorModel(int transparency) { return getColorModel(); }
		@Override public Rectangle getBounds() { return new Rectangle(1000, 80, 1280, 720); }
		@Override public AffineTransform getDefaultTransform() { return new AffineTransform(); }
		@Override public AffineTransform getNormalizingTransform() { return new AffineTransform(); }
	}
}
