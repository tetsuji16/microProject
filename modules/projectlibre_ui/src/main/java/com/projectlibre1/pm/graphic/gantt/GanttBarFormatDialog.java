package com.projectlibre1.pm.graphic.gantt;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.projectlibre1.graphic.configuration.GanttBarFormatOverrides.BarFormat;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.strings.Messages;

/**
 * Individual Gantt bar formatting, corresponding to Microsoft Project's
 * Format Bar dialog. The current feature scope exposes the color properties.
 */
final class GanttBarFormatDialog {
	private GanttBarFormatDialog() {
	}

	static void show(Component parent, Gantt gantt, Task task) {
		BarFormat original = gantt.getBarFormat(task);
		boolean milestone = task.isMilestone();

		ColorField start = new ColorField(parent, original.getStartRgb(), milestone);
		ColorField middle = new ColorField(parent, original.getMiddleRgb(), !milestone);
		ColorField end = new ColorField(parent, original.getEndRgb(), false);

		JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
		fields.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		fields.add(new JLabel(Messages.getString("Gantt.FormatBar.startColor")));
		fields.add(start);
		fields.add(new JLabel(Messages.getString("Gantt.FormatBar.middleColor")));
		fields.add(middle);
		fields.add(new JLabel(Messages.getString("Gantt.FormatBar.endColor")));
		fields.add(end);

		int result = JOptionPane.showConfirmDialog(
				parent,
				fields,
				Messages.getString("Gantt.FormatBar.title"),
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (result != JOptionPane.OK_OPTION)
			return;

		gantt.applyBarFormat(task, new BarFormat(start.getRgb(), middle.getRgb(), end.getRgb()));
	}

	static Color chooseColor(Component parent, Integer currentRgb) {
		Color initial = currentRgb == null ? new Color(0x5B9BD5) : new Color(currentRgb);
		return JColorChooser.showDialog(parent, Messages.getString("Gantt.FormatBar.chooseColor"), initial);
	}

	static String colorLabel(Integer rgb) {
		return rgb == null ? Messages.getString("Gantt.FormatBar.automatic") : String.format("#%06X", rgb & 0x00FFFFFF);
	}

	private static final class ColorField extends JPanel {
		private static final long serialVersionUID = 1L;
		private final Component parent;
		private final JButton colorButton = new JButton();
		private Integer rgb;

		private ColorField(Component parent, Integer rgb, boolean enabled) {
			super(new GridLayout(1, 2, 6, 0));
			this.parent = parent;
			this.rgb = rgb;
			JButton automaticButton = new JButton(Messages.getString("Gantt.FormatBar.automatic"));
			colorButton.addActionListener(event -> choose());
			automaticButton.addActionListener(event -> {
				this.rgb = null;
				refresh();
			});
			colorButton.setEnabled(enabled);
			automaticButton.setEnabled(enabled);
			add(colorButton);
			add(automaticButton);
			refresh();
		}

		private void choose() {
			Color chosen = chooseColor(parent, rgb);
			if (chosen == null)
				return;
			rgb = chosen.getRGB() & 0x00FFFFFF;
			refresh();
		}

		private void refresh() {
			colorButton.setText(colorLabel(rgb));
			if (rgb != null) {
				colorButton.setBackground(new Color(rgb));
				colorButton.setForeground(contrastColor(new Color(rgb)));
			} else {
				colorButton.setBackground(UIManager.getColor("Button.background"));
				colorButton.setForeground(UIManager.getColor("Button.foreground"));
			}
		}

		private Integer getRgb() {
			return rgb;
		}

		private static Color contrastColor(Color color) {
			double luminance = 0.2126d * color.getRed() + 0.7152d * color.getGreen() + 0.0722d * color.getBlue();
			return luminance < 140.0d ? Color.WHITE : Color.BLACK;
		}
	}
}
