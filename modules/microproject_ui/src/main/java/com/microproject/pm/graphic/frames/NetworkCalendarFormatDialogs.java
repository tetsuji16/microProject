/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.microproject.dialog.CalendarViewSettings;
import com.microproject.pm.graphic.network.NetworkDisplayOptions;

/**
 * Option panels of the Network Diagram and Calendar Format tabs (Box Styles, Layout, Bar Styles,
 * Text Styles, Calendar Layout). Each panel reads the current choices on creation and writes them
 * back through {@code apply()} when the user confirms.
 */
final class NetworkCalendarFormatDialogs {
	/** Marks the "keep the theme colour" entry of a colour list. */
	static final int DEFAULT_COLOR = Integer.MIN_VALUE;
	private static final int[] PALETTE = {
		DEFAULT_COLOR, 0xC0392B, 0xE67E22, 0xF1C40F, 0x27AE60, 0x2980B9, 0x8E44AD, 0x7F8C8D, 0x000000 };

	private NetworkCalendarFormatDialogs() {
	}

	/** A confirmable option panel. */
	static final class OptionPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private transient Runnable applier = () -> { };

		OptionPanel() {
			super(new GridBagLayout());
		}

		void apply() {
			applier.run();
		}
	}

	static JComboBox<Integer> colorCombo(Integer current, String accessibleName, String defaultText) {
		JComboBox<Integer> combo = new JComboBox<>();
		for (int rgb : PALETTE) combo.addItem(rgb);
		int value = current == null ? DEFAULT_COLOR : current;
		boolean known = false;
		for (int rgb : PALETTE) known |= rgb == value;
		if (!known) combo.addItem(value);
		combo.setSelectedItem(value);
		combo.getAccessibleContext().setAccessibleName(accessibleName);
		combo.setRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			@Override public Component getListCellRendererComponent(JList<?> list, Object item, int index, boolean selected, boolean focus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, item, index, selected, focus);
				int rgb = (Integer) item;
				label.setText(rgb == DEFAULT_COLOR ? defaultText : "");
				label.setIcon(rgb == DEFAULT_COLOR ? null : swatch(new Color(rgb)));
				return label;
			}
		});
		return combo;
	}

	private static Icon swatch(Color color) {
		return new Icon() {
			@Override public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
				g.setColor(color);
				g.fillRect(x, y, getIconWidth(), getIconHeight());
				g.setColor(Color.GRAY);
				g.drawRect(x, y, getIconWidth() - 1, getIconHeight() - 1);
			}
			@Override public int getIconWidth() { return 40; }
			@Override public int getIconHeight() { return 14; }
		};
	}

	static Integer colorOf(JComboBox<Integer> combo) {
		int rgb = (Integer) combo.getSelectedItem();
		return rgb == DEFAULT_COLOR ? null : rgb;
	}

	private static void row(JPanel panel, int row, Component... cells) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = row;
		c.insets = new Insets(2, 4, 2, 4);
		c.anchor = GridBagConstraints.WEST;
		for (int i = 0; i < cells.length; i++) {
			c.gridx = i;
			panel.add(cells[i], c);
		}
	}

	/** Box Styles: border colour and shape per kind of box; only the given kinds get a row. */
	static OptionPanel boxStyles(NetworkDisplayOptions options, List<NetworkDisplayOptions.BoxType> types, Function<String, String> text) {
		OptionPanel panel = new OptionPanel();
		Map<NetworkDisplayOptions.BoxType, JComboBox<Integer>> colors = new EnumMap<>(NetworkDisplayOptions.BoxType.class);
		Map<NetworkDisplayOptions.BoxType, JComboBox<NetworkDisplayOptions.BoxShape>> shapes = new EnumMap<>(NetworkDisplayOptions.BoxType.class);
		int rowIndex = 0;
		row(panel, rowIndex++, new JLabel(""), new JLabel(text.apply("FormatDialog.borderColor")), new JLabel(text.apply("FormatDialog.shape")));
		for (NetworkDisplayOptions.BoxType type : types) {
			NetworkDisplayOptions.BoxStyle style = options.getStyle(type);
			String typeText = text.apply("FormatDialog.box." + type.name());
			JComboBox<Integer> color = colorCombo(style.rgb(), typeText + " " + text.apply("FormatDialog.borderColor"), text.apply("FormatDialog.default"));
			JComboBox<NetworkDisplayOptions.BoxShape> shape = new JComboBox<>(NetworkDisplayOptions.BoxShape.values());
			shape.setSelectedItem(style.shape());
			shape.getAccessibleContext().setAccessibleName(typeText + " " + text.apply("FormatDialog.shape"));
			shape.setRenderer(new DefaultListCellRenderer() {
				private static final long serialVersionUID = 1L;
				@Override public Component getListCellRendererComponent(JList<?> list, Object item, int index, boolean selected, boolean focus) {
					return super.getListCellRendererComponent(list, text.apply("FormatDialog.shape." + item), index, selected, focus);
				}
			});
			colors.put(type, color);
			shapes.put(type, shape);
			row(panel, rowIndex++, new JLabel(typeText), color, shape);
		}
		panel.applier = () -> {
			for (NetworkDisplayOptions.BoxType type : types)
				options.setStyle(type, new NetworkDisplayOptions.BoxStyle(colorOf(colors.get(type)), (NetworkDisplayOptions.BoxShape) shapes.get(type).getSelectedItem()));
		};
		return panel;
	}

	/** Network Layout: link style, box spacing and summary task boxes. */
	record NetworkLayoutChoice(boolean straight, int spacing, boolean summaryTasks) { }

	static OptionPanel networkLayout(NetworkLayoutChoice current, Function<String, String> text, java.util.function.Consumer<NetworkLayoutChoice> onApply) {
		OptionPanel panel = new OptionPanel();
		JComboBox<String> style = new JComboBox<>(new String[] { text.apply("FormatDialog.linkOrthogonal"), text.apply("FormatDialog.linkStraight") });
		style.setSelectedIndex(current.straight() ? 1 : 0);
		style.getAccessibleContext().setAccessibleName(text.apply("FormatDialog.linkStyle"));
		JSpinner spacing = new JSpinner(new SpinnerNumberModel(current.spacing(), 0, NetworkDisplayOptions.MAX_SPACING, 5));
		spacing.getAccessibleContext().setAccessibleName(text.apply("FormatDialog.spacing"));
		JCheckBox summary = new JCheckBox(text.apply("FormatDialog.showSummary"), current.summaryTasks());
		row(panel, 0, new JLabel(text.apply("FormatDialog.linkStyle")), style);
		row(panel, 1, new JLabel(text.apply("FormatDialog.spacing")), spacing);
		row(panel, 2, summary);
		panel.applier = () -> onApply.accept(new NetworkLayoutChoice(style.getSelectedIndex() == 1, (Integer) spacing.getValue(), summary.isSelected()));
		return panel;
	}

	/** Calendar Bar Styles: bar colour per kind of task. */
	static OptionPanel calendarBarStyles(CalendarViewSettings settings, Function<String, String> text) {
		OptionPanel panel = new OptionPanel();
		Map<CalendarViewSettings.BarType, JComboBox<Integer>> colors = new EnumMap<>(CalendarViewSettings.BarType.class);
		int rowIndex = 0;
		for (CalendarViewSettings.BarType type : CalendarViewSettings.BarType.values()) {
			String typeText = text.apply("FormatDialog.box." + type.name());
			JComboBox<Integer> color = colorCombo(settings.getBarColor(type), typeText + " " + text.apply("FormatDialog.barColor"), text.apply("FormatDialog.default"));
			colors.put(type, color);
			row(panel, rowIndex++, new JLabel(typeText), color);
		}
		panel.applier = () -> {
			for (CalendarViewSettings.BarType type : CalendarViewSettings.BarType.values()) settings.setBarColor(type, colorOf(colors.get(type)));
		};
		return panel;
	}

	/** Calendar Text Styles: size and weight of the bar text and the day numbers. */
	static OptionPanel calendarTextStyles(CalendarViewSettings settings, Function<String, String> text) {
		OptionPanel panel = new OptionPanel();
		Map<CalendarViewSettings.TextItem, JSpinner> sizes = new EnumMap<>(CalendarViewSettings.TextItem.class);
		Map<CalendarViewSettings.TextItem, JCheckBox> bolds = new EnumMap<>(CalendarViewSettings.TextItem.class);
		int rowIndex = 0;
		for (CalendarViewSettings.TextItem item : CalendarViewSettings.TextItem.values()) {
			CalendarViewSettings.TextStyle style = settings.getTextStyle(item);
			String itemText = text.apply("FormatDialog.text." + item.name());
			JSpinner size = new JSpinner(new SpinnerNumberModel(style.size(), 0, 36, 1));
			size.getAccessibleContext().setAccessibleName(itemText + " " + text.apply("FormatDialog.fontSize"));
			JCheckBox bold = new JCheckBox(text.apply("FormatDialog.bold"), style.bold());
			bold.getAccessibleContext().setAccessibleName(itemText + " " + text.apply("FormatDialog.bold"));
			sizes.put(item, size);
			bolds.put(item, bold);
			row(panel, rowIndex++, new JLabel(itemText), size, bold);
		}
		panel.applier = () -> {
			for (CalendarViewSettings.TextItem item : CalendarViewSettings.TextItem.values())
				settings.setTextStyle(item, new CalendarViewSettings.TextStyle((Integer) sizes.get(item).getValue(), bolds.get(item).isSelected()));
		};
		return panel;
	}

	/** Calendar Layout: fit as many tasks as possible, and automatic layout. */
	static OptionPanel calendarLayout(CalendarViewSettings settings, Function<String, String> text) {
		OptionPanel panel = new OptionPanel();
		JCheckBox fit = new JCheckBox(text.apply("FormatDialog.fitMany"), settings.isFitAsManyTasksAsPossible());
		JCheckBox auto = new JCheckBox(text.apply("FormatDialog.autoLayout"), settings.isAutomaticLayout());
		row(panel, 0, fit);
		row(panel, 1, auto);
		panel.applier = () -> {
			settings.setFitAsManyTasksAsPossible(fit.isSelected());
			settings.setAutomaticLayout(auto.isSelected());
		};
		return panel;
	}
}
