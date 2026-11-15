/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.microproject.dialog.CalendarViewSettings.TextStyle;
import com.microproject.pm.graphic.chart.ResourceGraphSettings;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.BarStyle;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.Item;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.Pattern;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.ShowAs;

/** Option panels of the Resource Graph Format tab. Each panel edits the settings only when it is applied. */
final class ResourceGraphFormatDialogs {
	private ResourceGraphFormatDialogs() {
	}

	/** Bar Styles: shape, colour and pattern of each of the four items, and the availability line. */
	static NetworkCalendarFormatDialogs.OptionPanel barStyles(ResourceGraphSettings settings, Function<String, String> text) {
		NetworkCalendarFormatDialogs.OptionPanel panel = new NetworkCalendarFormatDialogs.OptionPanel();
		NetworkCalendarFormatDialogs.row(panel, 0, new JLabel(text.apply("ResourceGraph.dialog.item")),
			new JLabel(text.apply("ResourceGraph.dialog.showAs")), new JLabel(text.apply("ResourceGraph.dialog.color")),
			new JLabel(text.apply("ResourceGraph.dialog.pattern")));
		Map<Item, JComboBox<ShowAs>> shapes = new EnumMap<>(Item.class);
		Map<Item, JComboBox<Integer>> colors = new EnumMap<>(Item.class);
		Map<Item, JComboBox<Pattern>> patterns = new EnumMap<>(Item.class);
		int row = 1;
		for (Item item : Item.values()) {
			String name = text.apply(item.messageKey());
			BarStyle style = settings.getStyle(item);
			JComboBox<ShowAs> shape = new JComboBox<>(ShowAs.values());
			shape.setSelectedItem(style.showAs());
			shape.setRenderer(new javax.swing.DefaultListCellRenderer() {
				private static final long serialVersionUID = 1L;
				@Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
						boolean selected, boolean focus) {
					return super.getListCellRendererComponent(list, text.apply("ResourceGraph.showAs." + value), index, selected, focus);
				}
			});
			shape.getAccessibleContext().setAccessibleName(name + " " + text.apply("ResourceGraph.dialog.showAs"));
			JComboBox<Integer> color = NetworkCalendarFormatDialogs.colorCombo(style.rgb(),
				name + " " + text.apply("ResourceGraph.dialog.color"), text.apply("ResourceGraph.dialog.defaultColor"));
			JComboBox<Pattern> pattern = new JComboBox<>(Pattern.values());
			pattern.setSelectedItem(style.pattern());
			pattern.setRenderer(new javax.swing.DefaultListCellRenderer() {
				private static final long serialVersionUID = 1L;
				@Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
						boolean selected, boolean focus) {
					return super.getListCellRendererComponent(list, text.apply("ResourceGraph.pattern." + value), index, selected, focus);
				}
			});
			pattern.getAccessibleContext().setAccessibleName(name + " " + text.apply("ResourceGraph.dialog.pattern"));
			shapes.put(item, shape);
			colors.put(item, color);
			patterns.put(item, pattern);
			NetworkCalendarFormatDialogs.row(panel, row++, new JLabel(name), shape, color, pattern);
		}
		JCheckBox availability = new JCheckBox(text.apply("ResourceGraph.dialog.availabilityLine"), settings.isShowAvailabilityLine());
		JCheckBox reset = new JCheckBox(text.apply("ResourceGraph.dialog.reset"), false);
		NetworkCalendarFormatDialogs.row(panel, row++, availability);
		NetworkCalendarFormatDialogs.row(panel, row, reset);
		panel.applier = () -> {
			settings.setShowAvailabilityLine(availability.isSelected());
			if (reset.isSelected()) {
				settings.resetBarStyles();
				return;
			}
			for (Item item : Item.values()) {
				Integer rgb = NetworkCalendarFormatDialogs.colorOf(colors.get(item));
				settings.setStyle(item, new BarStyle((ShowAs) shapes.get(item).getSelectedItem(),
					rgb == null ? item.defaultStyle().rgb() : rgb, (Pattern) patterns.get(item).getSelectedItem()));
			}
		};
		return panel;
	}

	/** Gridlines: which of the horizontal and vertical lines the graph draws. */
	static NetworkCalendarFormatDialogs.OptionPanel gridlines(ResourceGraphSettings settings, Function<String, String> text) {
		NetworkCalendarFormatDialogs.OptionPanel panel = new NetworkCalendarFormatDialogs.OptionPanel();
		JCheckBox horizontal = new JCheckBox(text.apply("ResourceGraph.dialog.horizontalGridlines"), settings.isHorizontalGridlines());
		JCheckBox vertical = new JCheckBox(text.apply("ResourceGraph.dialog.verticalGridlines"), settings.isVerticalGridlines());
		NetworkCalendarFormatDialogs.row(panel, 0, horizontal);
		NetworkCalendarFormatDialogs.row(panel, 1, vertical);
		panel.applier = () -> {
			settings.setHorizontalGridlines(horizontal.isSelected());
			settings.setVerticalGridlines(vertical.isSelected());
		};
		return panel;
	}

	/** Text Styles: size and weight of the axis labels and of the value labels. */
	static NetworkCalendarFormatDialogs.OptionPanel textStyle(ResourceGraphSettings settings, Function<String, String> text) {
		NetworkCalendarFormatDialogs.OptionPanel panel = new NetworkCalendarFormatDialogs.OptionPanel();
		TextStyle current = settings.getTextStyle();
		String label = text.apply("ResourceGraph.dialog.textSize");
		JSpinner size = new JSpinner(new SpinnerNumberModel(current.size(), 0, 36, 1));
		size.getAccessibleContext().setAccessibleName(label);
		JCheckBox bold = new JCheckBox(text.apply("FormatDialog.bold"), current.bold());
		bold.getAccessibleContext().setAccessibleName(label + " " + text.apply("FormatDialog.bold"));
		NetworkCalendarFormatDialogs.row(panel, 0, new JLabel(label), size, bold);
		panel.applier = () -> settings.setTextStyle(new TextStyle((Integer) size.getValue(), bold.isSelected()));
		return panel;
	}
}
