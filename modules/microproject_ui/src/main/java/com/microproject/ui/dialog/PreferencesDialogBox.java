/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.dialog;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.microproject.preference.GlobalPreferences;
import com.microproject.ui.view.gantt.MilestoneShapeChoice;
import com.microproject.ui.shell.AutoSaveControl;
import com.microproject.ui.util.FlatLafSupport;
import com.microproject.ui.util.FlatUiSupport;
import com.microproject.ui.util.FlatLafDialog;
import com.microproject.ui.util.PopupDialogSupport;

/** User-level settings which are independent of a project file. */
public final class PreferencesDialogBox extends FlatLafDialog {
	private static final long serialVersionUID = 1L;
	/** Category IDs for {@link #showDialog(Frame, GlobalPreferences, Runnable, AutoSaveControl, String)}. */
	public static final String CATEGORY_GENERAL = "general";
	public static final String CATEGORY_DISPLAY = "display";
	public static final String CATEGORY_SAVE = "save";
	public static final String CATEGORY_CUSTOMIZE_RIBBON = "customizeRibbon";
	public static final String CATEGORY_QUICK_ACCESS = "quickAccess";
	public static final String CATEGORY_LANGUAGE = "language";
	/** Smallest page width that still shows both customization columns and their buttons. */
	private static final int MINIMUM_PAGE_WIDTH = 560;
	/** Smallest height that keeps the customization action row and the Apply/Cancel row visible. */
	private static final int MINIMUM_HEIGHT = 360;

	private boolean applied;

	public static void showDialog(Frame owner, GlobalPreferences preferences) {
		showDialog(owner, preferences, null, null, false);
	}

	/** Opens preferences and optionally exposes the application-wide locale action. */
	public static void showDialog(Frame owner, GlobalPreferences preferences, Runnable localeAction) {
		showDialog(owner, preferences, localeAction, null, false);
	}

	public static void showDialog(Frame owner, GlobalPreferences preferences, Runnable localeAction,
			AutoSaveControl autoSaveControl) {
		showDialog(owner, preferences, localeAction, autoSaveControl, false);
	}

	public static void showDialog(Frame owner, GlobalPreferences preferences, Runnable localeAction,
			AutoSaveControl autoSaveControl, boolean showLocaleTab) {
		showDialog(owner, preferences, localeAction, autoSaveControl, showLocaleTab ? CATEGORY_LANGUAGE : CATEGORY_GENERAL);
	}

	/** Opens preferences with the category named by one of the {@code CATEGORY_*} IDs selected; an unknown ID selects the first. */
	public static void showDialog(Frame owner, GlobalPreferences preferences, Runnable localeAction,
			AutoSaveControl autoSaveControl, String categoryId) {
		PreferencesDialogBox dialog = new PreferencesDialogBox(owner, preferences, autoSaveControl, categoryId);
		dialog.setVisible(true);
		if (dialog.applied && localeAction != null) localeAction.run();
	}

	private PreferencesDialogBox(Frame owner, GlobalPreferences preferences, AutoSaveControl autoSaveControl,
			String initialCategory) {
		super(owner, UsabilityStrings.text("preferences.title"), true);
		FlatUiSupport.styleDialogRoot(getRootPane());
		PopupDialogSupport.bindEscapeToDispose(this);
		JTextField userName = new JTextField(preferences.getUserName(), 24);
		JCheckBox rowLines = new JCheckBox(UsabilityStrings.text("preferences.rowLines"), preferences.isShowRowLines());
		JCheckBox checkUpdates = UpdateChecker.isStoreDistribution() ? null
				: new JCheckBox(UsabilityStrings.text("preferences.checkUpdates"), preferences.isCheckForUpdates());
		String lightTheme = UsabilityStrings.text("preferences.themeLight");
		String darkTheme = UsabilityStrings.text("preferences.themeDark");
		JComboBox<String> theme = new JComboBox<>(new String[] { lightTheme, darkTheme });
		theme.setName("preferencesTheme");
		theme.setSelectedItem(preferences.isDarkTheme() ? darkTheme : lightTheme);
		FormatChoice datePattern = new FormatChoice("preferencesDatePattern", UsabilityStrings.text("preferences.datePattern"),
			16, preferences.getDatePattern(), UsabilityStrings.text("preferences.datePatternHelp"),
			() -> osDatePattern(false), GlobalPreferences::isValidDatePattern);
		FormatChoice dateTimePattern = new FormatChoice("preferencesDateTimePattern",
			UsabilityStrings.text("preferences.dateTimePattern"), 16, preferences.getDateTimePattern(),
			UsabilityStrings.text("preferences.datePatternHelp"), () -> osDatePattern(true),
			GlobalPreferences::isValidDatePattern);
		FormatChoice currencyCode = new FormatChoice("preferencesCurrencyCode", UsabilityStrings.text("preferences.currencyCode"),
			8, preferences.getCurrencyCode(), UsabilityStrings.text("preferences.currencyCodeHelp"),
			PreferencesDialogBox::osCurrencyCode, GlobalPreferences::isValidCurrencyCode);
		JSpinner recoveryInterval = autoSaveControl == null ? null
				: new JSpinner(new SpinnerNumberModel(autoSaveControl.getIntervalMinutes(), 1, 1440, 1));
		if (recoveryInterval != null) recoveryInterval.setName("preferencesAutoSaveInterval");
		String[] fonts = java.awt.GraphicsEnvironment.isHeadless()
			? new String[] { "" }
			: java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		String systemFont = UsabilityStrings.text("preferences.fontAutomatic");
		JComboBox<String> font = new JComboBox<>(fontChoices(fonts, preferences.getFontFamily(), systemFont));
		font.setName("preferencesFontFamily");
		font.setSelectedItem(fontChoiceOf(preferences.getFontFamily(), systemFont));
		JSpinner size = new JSpinner(new FontSizeModel(preferences.getFontSize()));
		JSpinner.NumberEditor sizeEditor = new JSpinner.NumberEditor(size);
		sizeEditor.getTextField().setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(
			new FontSizeFormatter(UsabilityStrings.text("preferences.fontSizeAutomatic"))));
		size.setEditor(sizeEditor);
		String resourceNames = UsabilityStrings.text("preferences.ganttBarTextResourceNames");
		String taskName = UsabilityStrings.text("preferences.ganttBarTextTaskName");
		JComboBox<String> ganttBarText = new JComboBox<>(new String[] { resourceNames, taskName });
		ganttBarText.setSelectedItem(GlobalPreferences.GANTT_BAR_TEXT_TASK_NAME.equals(preferences.getDefaultGanttBarText())
				? taskName : resourceNames);
		String automaticPosition = UsabilityStrings.text("preferences.ganttBarTextPositionAutomatic");
		String rightPosition = UsabilityStrings.text("preferences.ganttBarTextPositionRight");
		String leftPosition = UsabilityStrings.text("preferences.ganttBarTextPositionLeft");
		JComboBox<String> ganttBarTextPosition = new JComboBox<>(new String[] { automaticPosition, rightPosition, leftPosition });
		String savedPosition = preferences.getDefaultGanttBarTextPosition();
		ganttBarTextPosition.setSelectedItem(GlobalPreferences.GANTT_BAR_TEXT_POSITION_LEFT.equals(savedPosition) ? leftPosition
				: GlobalPreferences.GANTT_BAR_TEXT_POSITION_RIGHT.equals(savedPosition) ? rightPosition : automaticPosition);
		JComboBox<MilestoneShapeChoice> milestoneShape = new JComboBox<>(MilestoneShapeChoice.values());
		milestoneShape.setName("preferencesMilestoneShape");
		milestoneShape.setSelectedItem(MilestoneShapeChoice.forName(preferences.getDefaultMilestoneShape()));
		ColorChoice gridColor = new ColorChoice(this, UsabilityStrings.text("preferences.gridColor"),
				UsabilityStrings.text("preferences.gridColorAutomatic"), preferences.getGridLineColor());
		ColorChoice barColor = new ColorChoice(this, UsabilityStrings.text("preferences.ganttBarColor"),
				UsabilityStrings.text("preferences.ganttBarColorAutomatic"), preferences.getDefaultGanttBarColor());
		JPanel general = newCategoryForm();
		addRow(general, UsabilityStrings.text("preferences.userName"), userName);
		addRow(general, UsabilityStrings.text("preferences.theme"), theme);
		addRow(general, UsabilityStrings.text("preferences.font"), font);
		addRow(general, UsabilityStrings.text("preferences.fontSize"), size);
		if (checkUpdates != null) addRow(general, null, checkUpdates);
		JPanel display = newCategoryForm();
		addRow(display, null, rowLines);
		addRow(display, UsabilityStrings.text("preferences.ganttBarText"), ganttBarText);
		addRow(display, UsabilityStrings.text("preferences.ganttBarTextPosition"), ganttBarTextPosition);
		addRow(display, UsabilityStrings.text("preferences.milestoneShape"), milestoneShape);
		addRow(display, UsabilityStrings.text("preferences.ganttBarColor"), barColor.controls());
		addRow(display, UsabilityStrings.text("preferences.gridColor"), gridColor.controls());
		addRow(display, UsabilityStrings.text("preferences.datePattern"), datePattern.controls());
		addRow(display, UsabilityStrings.text("preferences.dateTimePattern"), dateTimePattern.controls());
		addRow(display, UsabilityStrings.text("preferences.currencyCode"), currencyCode.controls());
		JPanel save = null;
		if (recoveryInterval != null) {
			save = newCategoryForm();
			addRow(save, UsabilityStrings.text("preferences.autoSaveInterval"), recoveryInterval);
		}
		for (JPanel panel : save == null ? new JPanel[] { general, display } : new JPanel[] { general, display, save }) {
			// Absorb spare height so rows keep their natural size inside the scroll pane.
			GridBagConstraints filler = new GridBagConstraints();
			filler.gridx = 0; filler.gridy = GridBagConstraints.RELATIVE; filler.gridwidth = 2; filler.weighty = 1;
			panel.add(new JPanel(), filler);
		}
		LocaleDialog localeSettings = LocaleDialog.createPreferencesSection(owner);
		// Like File > Options in Microsoft Project: categories on the left, the chosen one on the right.
		java.util.List<String> categoryNames = new java.util.ArrayList<>();
		java.util.List<String> categoryIds = new java.util.ArrayList<>();
		CardLayout cards = new CardLayout();
		JPanel cardPanel = new JPanel(cards);
		RibbonCustomizationPanel ribbonPage = new RibbonCustomizationPanel();
		QuickAccessCustomizationPanel quickAccessPage = new QuickAccessCustomizationPanel();
		CustomizationTransfer transfer = new CustomizationTransfer(ribbonPage::workingCustomization,
			quickAccessPage::workingCommands, (customization, commands) -> {
				ribbonPage.replaceWorking(customization);
				quickAccessPage.replaceWorking(commands);
			});
		ribbonPage.attachTransfer(transfer);
		quickAccessPage.attachTransfer(transfer);
		addCategory(cardPanel, categoryNames, UsabilityStrings.text("preferences.general"), general);
		categoryIds.add(CATEGORY_GENERAL);
		addCategory(cardPanel, categoryNames, UsabilityStrings.text("preferences.display"), display);
		categoryIds.add(CATEGORY_DISPLAY);
		if (save != null) {
			addCategory(cardPanel, categoryNames, UsabilityStrings.text("preferences.save"), save);
			categoryIds.add(CATEGORY_SAVE);
		}
		addCategory(cardPanel, categoryNames, UsabilityStrings.text("preferences.customizeRibbon"), ribbonPage);
		categoryIds.add(CATEGORY_CUSTOMIZE_RIBBON);
		addCategory(cardPanel, categoryNames, UsabilityStrings.text("preferences.quickAccess"), quickAccessPage);
		categoryIds.add(CATEGORY_QUICK_ACCESS);
		addCategory(cardPanel, categoryNames, UsabilityStrings.text("preferences.language"),
			localeSettings.createPreferencesContent());
		categoryIds.add(CATEGORY_LANGUAGE);
		JList<String> categories = new JList<>(categoryNames.toArray(new String[0]));
		categories.setName("preferencesCategories");
		categories.getAccessibleContext().setAccessibleName(UsabilityStrings.text("preferences.categories"));
		categories.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		categories.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		categories.addListSelectionListener(event -> {
			String chosen = categories.getSelectedValue();
			if (!event.getValueIsAdjusting() && chosen != null) cards.show(cardPanel, chosen);
		});
		categories.setSelectedIndex(Math.max(0, categoryIds.indexOf(initialCategory)));
		JScrollPane categoryScroll = new JScrollPane(categories);
		categoryScroll.setName("preferencesCategoriesScroll");
		JSplitPane tabs = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, categoryScroll, cardPanel);
		tabs.setName("preferencesCategorySplit");
		tabs.getAccessibleContext().setAccessibleName(UsabilityStrings.text("preferences.categories"));
		tabs.setContinuousLayout(true);
		tabs.setDividerSize(4);
		// Keep the category list at its preferred width: a wide page must scroll instead of squeezing the list away.
		int categoryWidth = categoryScroll.getPreferredSize().width;
		categoryScroll.setMinimumSize(new java.awt.Dimension(categoryWidth, 0));
		tabs.setResizeWeight(0);
		tabs.setDividerLocation(categoryWidth);

		JButton apply = new JButton(UsabilityStrings.text("preferences.apply"));
		apply.addActionListener(event -> {
			if (!datePattern.isValid() || !dateTimePattern.isValid() || !currencyCode.isValid()) {
				JOptionPane.showMessageDialog(this, UsabilityStrings.text("preferences.invalidFormat"),
					UsabilityStrings.text("preferences.title"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (!ribbonPage.canSave()) {
				JOptionPane.showMessageDialog(this, UsabilityStrings.text("ribbonCustomize.tooLarge"),
					UsabilityStrings.text("preferences.title"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (!localeSettings.savePreferencesContent()) return;
			applied = true;
			preferences.setUserName(userName.getText());
			preferences.setShowRowLines(rowLines.isSelected());
			String previousFontFamily = preferences.getFontFamily();
			int previousFontSize = preferences.getFontSize();
			preferences.setFontFamily(fontFamilyOf(font.getSelectedItem(), systemFont));
			preferences.setFontSize(((Number) size.getValue()).intValue());
			boolean fontChanged = !preferences.getFontFamily().equals(previousFontFamily)
					|| preferences.getFontSize() != previousFontSize;
			preferences.setDefaultGanttBarText(taskName.equals(ganttBarText.getSelectedItem())
					? GlobalPreferences.GANTT_BAR_TEXT_TASK_NAME : GlobalPreferences.GANTT_BAR_TEXT_RESOURCE_NAMES);
			preferences.setDefaultGanttBarTextPosition(leftPosition.equals(ganttBarTextPosition.getSelectedItem())
					? GlobalPreferences.GANTT_BAR_TEXT_POSITION_LEFT
					: rightPosition.equals(ganttBarTextPosition.getSelectedItem())
						? GlobalPreferences.GANTT_BAR_TEXT_POSITION_RIGHT : GlobalPreferences.GANTT_BAR_TEXT_POSITION_AUTO);
			preferences.setDefaultMilestoneShape(((MilestoneShapeChoice)milestoneShape.getSelectedItem()).getShapeName());
			preferences.setGridLineColor(gridColor.rgb());
			preferences.setDefaultGanttBarColor(barColor.rgb());
			if (checkUpdates != null) preferences.setCheckForUpdates(checkUpdates.isSelected());
			boolean themeChanged = preferences.isDarkTheme() != darkTheme.equals(theme.getSelectedItem());
			preferences.setDarkTheme(darkTheme.equals(theme.getSelectedItem()));
			boolean formatsChanged = !preferences.getDatePattern().equals(datePattern.value())
					|| !preferences.getDateTimePattern().equals(dateTimePattern.value())
					|| !preferences.getCurrencyCode().equals(currencyCode.value().toUpperCase(java.util.Locale.ROOT));
			preferences.setDatePattern(datePattern.value());
			preferences.setDateTimePattern(dateTimePattern.value());
			preferences.setCurrencyCode(currencyCode.value());
			// Formats are read at display time, so re-applying them is enough for every open window.
			if (formatsChanged)
				preferences.applyFormatPreferences();
			if (recoveryInterval != null)
				autoSaveControl.setIntervalMinutes(((Number) recoveryInterval.getValue()).intValue());
			boolean ribbonChanged = ribbonPage.saveIfChanged();
			boolean quickAccessChanged = quickAccessPage.saveIfChanged();
			dispose();
			// Ribbon and Quick Access edits take effect in every open window once they are stored.
			if (ribbonChanged || quickAccessChanged)
				com.microproject.ui.shell.MicroProjectShell.rebuildOpenRibbons();
			// Typography and display formats are applied once, after the modal dialog is gone, and only
			// when they changed. A theme switch below refreshes every window itself.
			if (fontChanged)
				FlatLafSupport.applyUserFontPreference(preferences.getFontFamily(), preferences.getFontSize());
			if ((fontChanged || formatsChanged) && !themeChanged)
				FlatUiSupport.updateAllWindowTrees();
			// The theme applies live to every open window; no restart is needed.
			if (themeChanged)
				com.microproject.ui.theme.ThemeManager.applyTheme(preferences.isDarkTheme());
		});
		JButton cancel = new JButton(UsabilityStrings.text("preferences.cancel"));
		getRootPane().setDefaultButton(apply); // Enter presses the primary command (Windows/Office dialog rule)
		cancel.addActionListener(event -> dispose());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(cancel); buttons.add(apply);
		setLayout(new BorderLayout());
		add(tabs, BorderLayout.CENTER); add(buttons, BorderLayout.SOUTH);
		pack();
		// The packed size is only an upper bound: clamp it to the owner's monitor
		// with the same margin and screen insets every other large tool dialog uses. The minimum keeps the
		// category list and a usable page beside it, so the window cannot be squeezed until pages are cut off;
		// DialogSizing clamps the minimum to the monitor as well.
		DialogSizing.applyInitialSize(this, new Dimension(categoryWidth + MINIMUM_PAGE_WIDTH, MINIMUM_HEIGHT),
			getSize(), owner);
	}

	private static JPanel newCategoryForm() {
		JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
		return form;
	}

	/** Adds a category page; the page is named and labelled for assistive technology. */
	private static void addCategory(JPanel cardPanel, java.util.List<String> names, String name, java.awt.Component page) {
		JScrollPane scroll = new JScrollPane(new CategoryPageHolder(page));
		scroll.setName("preferencesCategory." + names.size());
		scroll.getAccessibleContext().setAccessibleName(name);
		cardPanel.add(scroll, name);
		names.add(name);
	}

	/**
	 * Holds a category page in its scroll pane. The page is laid out at the viewport size whenever the
	 * viewport is at least the page's minimum size, so a narrow or short dialog shrinks the page's
	 * lists rather than scrolling its buttons out of view; only a viewport below that minimum scrolls.
	 */
	static final class CategoryPageHolder extends JPanel implements javax.swing.Scrollable {
		private static final long serialVersionUID = 1L;

		CategoryPageHolder(java.awt.Component page) {
			super(new java.awt.BorderLayout());
			add(page, java.awt.BorderLayout.CENTER);
		}

		@Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
		@Override public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientation, int direction) { return 16; }
		@Override public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientation, int direction) {
			return Math.max(16, orientation == javax.swing.SwingConstants.VERTICAL ? visible.height : visible.width);
		}
		@Override public boolean getScrollableTracksViewportWidth() {
			return getParent() instanceof javax.swing.JViewport viewport && viewport.getWidth() >= getMinimumSize().width;
		}
		@Override public boolean getScrollableTracksViewportHeight() {
			return getParent() instanceof javax.swing.JViewport viewport && viewport.getHeight() >= getMinimumSize().height;
		}
	}

	private static void addRow(JPanel form, String label, java.awt.Component field) {
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = 0; labelConstraints.gridy = GridBagConstraints.RELATIVE;
		labelConstraints.anchor = GridBagConstraints.WEST;
		labelConstraints.insets = new Insets(4, 0, 4, 8);
		form.add(label == null ? new JLabel() : new JLabel(label), labelConstraints);
		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridx = 1; fieldConstraints.gridy = GridBagConstraints.RELATIVE;
		fieldConstraints.weightx = 1; fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraints.anchor = GridBagConstraints.WEST;
		fieldConstraints.insets = new Insets(4, 0, 4, 0);
		form.add(field, fieldConstraints);
	}

	/** A button showing the chosen colour (or an automatic label) together with a reset button. */
	private static final class ColorChoice {
		private final JButton choose;
		private final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		private final String automaticText;
		private Color selected;

		ColorChoice(java.awt.Component parent, String chooserTitle, String automaticText, Integer saved) {
			this.automaticText = automaticText;
			selected = saved == null ? null : new Color(saved.intValue());
			choose = new JButton(automaticText);
			JButton reset = new JButton(UsabilityStrings.text("preferences.reset"));
			refresh();
			choose.addActionListener(event -> {
				Color chosen = JColorChooser.showDialog(parent, chooserTitle, selected);
				if (chosen != null) { selected = chosen; refresh(); }
			});
			reset.addActionListener(event -> { selected = null; refresh(); });
			controls.add(choose);
			controls.add(reset);
		}

		JPanel controls() { return controls; }

		/** Returns the selected RGB value, or null when the automatic colour is kept. */
		Integer rgb() { return selected == null ? null : Integer.valueOf(selected.getRGB()); }

		private void refresh() {
			choose.setBackground(selected);
			choose.setText(selected == null ? automaticText : String.format("#%06X", selected.getRGB() & 0x00ffffff));
		}
	}

	/**
	 * Font list shown to the user: the system-default entry first, then the
	 * installed families, then a stored family that is no longer installed so
	 * that opening and applying the dialog never replaces it silently.
	 */
	static String[] fontChoices(String[] families, String storedFamily, String systemDefaultLabel) {
		java.util.LinkedHashSet<String> choices = new java.util.LinkedHashSet<>();
		choices.add(systemDefaultLabel);
		if (families != null) {
			for (String family : families) {
				if (family != null && !family.isBlank())
					choices.add(family);
			}
		}
		if (storedFamily != null && !storedFamily.isBlank())
			choices.add(storedFamily.trim());
		return choices.toArray(new String[0]);
	}

	/** Combo entry that represents a stored family; empty means system default. */
	static String fontChoiceOf(String storedFamily, String systemDefaultLabel) {
		return storedFamily == null || storedFamily.isBlank() ? systemDefaultLabel : storedFamily.trim();
	}

	/** Stored family for a combo entry; the system-default entry is stored as empty. */
	static String fontFamilyOf(Object selected, String systemDefaultLabel) {
		if (selected == null || systemDefaultLabel.equals(selected.toString()))
			return "";
		return selected.toString().trim();
	}

	/** Font size 0 means automatic; sizes 1-7 are not usable and snap to {@link #MIN_FONT_SIZE}. */
	static final class FontSizeModel extends SpinnerNumberModel {
		private static final long serialVersionUID = 1L;
		static final int MIN_FONT_SIZE = 8;

		FontSizeModel(int value) {
			super(snap(value), 0, 32, 1);
		}

		static int snap(int value) {
			return value <= 0 ? 0 : Math.max(MIN_FONT_SIZE, Math.min(32, value));
		}

		@Override
		public void setValue(Object value) {
			super.setValue(value instanceof Number number ? Integer.valueOf(snap(number.intValue())) : value);
		}

		@Override
		public Object getNextValue() {
			int current = ((Number) getValue()).intValue();
			return current >= 32 ? null : Integer.valueOf(current == 0 ? MIN_FONT_SIZE : current + 1);
		}

		@Override
		public Object getPreviousValue() {
			int current = ((Number) getValue()).intValue();
			return current <= 0 ? null : Integer.valueOf(current <= MIN_FONT_SIZE ? 0 : current - 1);
		}
	}

	/** Shows font size 0 as the localized "Auto" label. */
	private static final class FontSizeFormatter extends javax.swing.JFormattedTextField.AbstractFormatter {
		private static final long serialVersionUID = 1L;
		private final String automaticText;

		FontSizeFormatter(String automaticText) {
			this.automaticText = automaticText;
		}

		@Override
		public Object stringToValue(String text) throws java.text.ParseException {
			String trimmed = text == null ? "" : text.trim();
			if (trimmed.isEmpty() || trimmed.equalsIgnoreCase(automaticText))
				return Integer.valueOf(0);
			try {
				return Integer.valueOf(FontSizeModel.snap(Integer.parseInt(trimmed)));
			} catch (NumberFormatException ex) {
				throw new java.text.ParseException(trimmed, 0);
			}
		}

		@Override
		public String valueToString(Object value) {
			int size = value instanceof Number number ? number.intValue() : 0;
			return size <= 0 ? automaticText : Integer.toString(size);
		}
	}

	/**
	 * The pattern used when the stored value is empty. It comes from the same factories as
	 * {@code GlobalPreferences.applyFormatPreferences()}, so the preview matches the runtime.
	 */
	static String osDatePattern(boolean withTime) {
		java.text.DateFormat format = withTime
			? com.microproject.util.DateTime.utcDateFormatInstance()
			: com.microproject.util.DateTime.utcShortDateFormatInstance();
		return format instanceof java.text.SimpleDateFormat simple ? simple.toPattern() : "";
	}

	/** The currency code used when the stored value is empty: the one of {@code Money}'s currency format. */
	static String osCurrencyCode() {
		try {
			return java.text.NumberFormat.getCurrencyInstance().getCurrency().getCurrencyCode();
		} catch (RuntimeException noCurrency) {
			return "";
		}
	}

	/**
	 * A format setting with a "follow the OS" and a "custom" choice. An empty stored value means
	 * "follow the OS": the field then shows the OS default read-only and {@link #value()} is empty.
	 */
	private static final class FormatChoice {
		private final JRadioButton followOs;
		private final JRadioButton custom;
		private final JTextField field;
		private final JPanel controls = new JPanel(new GridBagLayout());
		private final java.util.function.Supplier<String> osDefault;
		private final java.util.function.Predicate<String> validator;
		private String customText;
		private boolean showingCustom;

		FormatChoice(String name, String label, int columns, String stored, String help,
				java.util.function.Supplier<String> osDefault, java.util.function.Predicate<String> validator) {
			this.osDefault = osDefault;
			this.validator = validator;
			boolean isCustom = stored != null && !stored.isBlank();
			customText = isCustom ? stored : osDefault.get();
			followOs = new JRadioButton(UsabilityStrings.text("preferences.formatFollowOs"), !isCustom);
			custom = new JRadioButton(UsabilityStrings.text("preferences.formatCustom"), isCustom);
			followOs.setName(name + "FollowOs");
			custom.setName(name + "Custom");
			followOs.getAccessibleContext().setAccessibleName(label + ": " + followOs.getText());
			custom.getAccessibleContext().setAccessibleName(label + ": " + custom.getText());
			javax.swing.ButtonGroup group = new javax.swing.ButtonGroup();
			group.add(followOs);
			group.add(custom);
			field = new JTextField(isCustom ? stored : osDefault.get(), columns);
			field.setName(name);
			field.setToolTipText(help);
			field.getAccessibleContext().setAccessibleName(label);
			java.awt.event.ActionListener modeChanged = event -> refresh();
			followOs.addActionListener(modeChanged);
			custom.addActionListener(modeChanged);
			GridBagConstraints radios = new GridBagConstraints();
			radios.gridx = 0; radios.gridy = 0; radios.anchor = GridBagConstraints.WEST;
			JPanel radioRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			radioRow.add(followOs);
			radioRow.add(custom);
			controls.add(radioRow, radios);
			GridBagConstraints input = new GridBagConstraints();
			input.gridx = 0; input.gridy = 1; input.anchor = GridBagConstraints.WEST;
			input.insets = new Insets(2, 0, 0, 0);
			controls.add(field, input);
			showingCustom = isCustom;
			applyMode();
		}

		private void refresh() {
			// A radio button fires again when it is clicked while selected; only a real mode change moves text.
			if (custom.isSelected() == showingCustom) return;
			showingCustom = custom.isSelected();
			if (custom.isSelected()) {
				field.setText(customText);
			} else {
				customText = field.getText();
				field.setText(osDefault.get());
			}
			applyMode();
		}

		private void applyMode() {
			field.setEditable(custom.isSelected());
			field.setEnabled(custom.isSelected());
		}

		JPanel controls() { return controls; }

		/** The value to store: empty when following the OS. */
		String value() { return followOs.isSelected() ? "" : field.getText().trim(); }

		/** Custom values must be non-empty and valid; following the OS is always valid. */
		boolean isValid() {
			return followOs.isSelected() || (!field.getText().isBlank() && validator.test(field.getText()));
		}
	}
}
