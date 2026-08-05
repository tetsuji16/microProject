package com.projectlibre1.pm.graphic.gantt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ListCellRenderer;

import com.projectlibre1.graphic.configuration.GanttBarFormatOverrides.BarFormat;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.strings.Messages;

/**
 * Individual Gantt bar formatting, corresponding to Microsoft Project's
 * Format Bar dialog. The current feature scope exposes the color properties.
 */
final class GanttBarFormatDialog {
	private static final int DEFAULT_BAR_RGB = 0x5B9BD5;
	private static final int[] STANDARD_COLORS = {
		0x000000, 0x7F7F7F, 0xA5A5A5, 0xFFFFFF,
		0xC00000, 0xED7D31, 0xFFC000, 0x70AD47,
		0x5B9BD5, 0x4472C4, 0x7030A0
	};

	private GanttBarFormatDialog() {
	}

	static void show(Component parent, Gantt gantt, Task task) {
		BarFormat original = gantt.getBarFormat(task);
		FormatPanel fields = new FormatPanel(parent, original, task.isMilestone(), task.isSummary());

		int result = JOptionPane.showConfirmDialog(
				parent,
				fields,
				Messages.getString("Gantt.FormatBar.title"),
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (result != JOptionPane.OK_OPTION)
			return;

		gantt.applyBarFormat(task, fields.getFormat());
	}

	static Color chooseColor(Component parent, Integer currentRgb) {
		Color initial = currentRgb == null ? new Color(DEFAULT_BAR_RGB) : new Color(currentRgb);
		return JColorChooser.showDialog(parent, Messages.getString("Gantt.FormatBar.chooseColor"), initial);
	}

	static String colorLabel(Integer rgb) {
		return rgb == null ? Messages.getString("Gantt.FormatBar.automatic") : String.format("#%06X", rgb & 0x00FFFFFF);
	}

	static JPanel createPanelForTest(BarFormat format, boolean milestone, boolean summary) {
		return new FormatPanel(null, format, milestone, summary);
	}

	private static final class FormatPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private final ColorField start;
		private final ColorField middle;
		private final ColorField end;

		private FormatPanel(Component parent, BarFormat format, boolean milestone, boolean summary) {
			super(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

			BarPreview preview = new BarPreview(milestone, summary);
			Runnable refreshPreview = preview::repaint;
			start = new ColorField(parent, format.getStartRgb(), refreshPreview);
			middle = new ColorField(parent, format.getMiddleRgb(), refreshPreview);
			end = new ColorField(parent, format.getEndRgb(), refreshPreview);
			preview.setFields(start, middle, end);

			JPanel shapePanel = new JPanel(new GridBagLayout());
			shapePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			addCell(shapePanel, new JLabel(), 0, 0, 0.0d);
			addCell(shapePanel, centeredLabel("Gantt.FormatBar.start"), 1, 0, 1.0d);
			addCell(shapePanel, centeredLabel("Gantt.FormatBar.middle"), 2, 0, 1.0d);
			addCell(shapePanel, centeredLabel("Gantt.FormatBar.end"), 3, 0, 1.0d);
			addCell(shapePanel, new JLabel(Messages.getString("Gantt.FormatBar.color")), 0, 1, 0.0d);
			addCell(shapePanel, start, 1, 1, 1.0d);
			addCell(shapePanel, middle, 2, 1, 1.0d);
			addCell(shapePanel, end, 3, 1, 1.0d);

			GridBagConstraints sampleLabel = constraints(0, 2, 0.0d);
			sampleLabel.anchor = GridBagConstraints.NORTHWEST;
			shapePanel.add(new JLabel(Messages.getString("Gantt.FormatBar.sample")), sampleLabel);
			GridBagConstraints sample = constraints(1, 2, 1.0d);
			sample.gridwidth = 3;
			sample.fill = GridBagConstraints.BOTH;
			sample.weighty = 1.0d;
			shapePanel.add(preview, sample);

			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab(Messages.getString("Gantt.FormatBar.barColor"), shapePanel);
			add(tabs, BorderLayout.CENTER);

			JButton reset = new JButton(Messages.getString("Gantt.FormatBar.reset"));
			reset.addActionListener(event -> {
				start.setRgb(null);
				middle.setRgb(null);
				end.setRgb(null);
			});
			JPanel resetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
			resetPanel.add(reset);
			add(resetPanel, BorderLayout.SOUTH);
		}

		private static JLabel centeredLabel(String key) {
			return new JLabel(Messages.getString(key), JLabel.CENTER);
		}

		private static void addCell(JPanel panel, JComponent component, int x, int y, double weightX) {
			panel.add(component, constraints(x, y, weightX));
		}

		private static GridBagConstraints constraints(int x, int y, double weightX) {
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.gridx = x;
			constraints.gridy = y;
			constraints.weightx = weightX;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			constraints.anchor = GridBagConstraints.WEST;
			constraints.insets = new Insets(4, x == 0 ? 0 : 6, 4, 0);
			return constraints;
		}

		private BarFormat getFormat() {
			return new BarFormat(start.getRgb(), middle.getRgb(), end.getRgb());
		}
	}

	private static final class ColorField extends JComboBox<ColorChoice> {
		private static final long serialVersionUID = 1L;
		private final Component parent;
		private final Runnable changeListener;
		private Integer rgb;
		private boolean adjusting;

		private ColorField(Component parent, Integer rgb, Runnable changeListener) {
			this.parent = parent;
			this.changeListener = changeListener;
			setRenderer(new ColorChoiceRenderer());
			setPreferredSize(new Dimension(126, getPreferredSize().height));
			addActionListener(event -> selectionChanged());
			setRgb(rgb);
		}

		private void selectionChanged() {
			if (adjusting)
				return;
			ColorChoice choice = (ColorChoice) getSelectedItem();
			if (choice == null)
				return;
			if (choice.moreColors) {
				selectRgb(rgb);
				Color chosen = chooseColor(parent, rgb);
				if (chosen != null)
					setRgb(chosen.getRGB() & 0x00FFFFFF);
				return;
			}
			rgb = choice.rgb;
			changeListener.run();
		}

		private void setRgb(Integer rgb) {
			this.rgb = rgb == null ? null : rgb & 0x00FFFFFF;
			adjusting = true;
			removeAllItems();
			addItem(ColorChoice.automatic());
			boolean standard = false;
			for (int color : STANDARD_COLORS) {
				addItem(ColorChoice.color(color));
				standard |= this.rgb != null && this.rgb == color;
			}
			if (this.rgb != null && !standard)
				addItem(ColorChoice.color(this.rgb));
			addItem(ColorChoice.moreColors());
			selectRgb(this.rgb);
			adjusting = false;
			changeListener.run();
		}

		private void selectRgb(Integer selectedRgb) {
			adjusting = true;
			for (int index = 0; index < getItemCount(); index++) {
				ColorChoice choice = getItemAt(index);
				if (!choice.moreColors && Objects.equals(choice.rgb, selectedRgb)) {
					setSelectedIndex(index);
					break;
				}
			}
			adjusting = false;
		}

		private Integer getRgb() {
			return rgb;
		}
	}

	private static final class ColorChoice {
		private final Integer rgb;
		private final boolean moreColors;

		private ColorChoice(Integer rgb, boolean moreColors) {
			this.rgb = rgb;
			this.moreColors = moreColors;
		}

		private static ColorChoice automatic() {
			return new ColorChoice(null, false);
		}

		private static ColorChoice color(int rgb) {
			return new ColorChoice(rgb & 0x00FFFFFF, false);
		}

		private static ColorChoice moreColors() {
			return new ColorChoice(null, true);
		}
	}

	private static final class ColorChoiceRenderer extends JLabel implements ListCellRenderer<ColorChoice> {
		private static final long serialVersionUID = 1L;

		private ColorChoiceRenderer() {
			setOpaque(true);
			setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		}

		@Override
		public Component getListCellRendererComponent(
				JList<? extends ColorChoice> list,
				ColorChoice choice,
				int index,
				boolean selected,
				boolean focused) {
			setBackground(selected ? list.getSelectionBackground() : list.getBackground());
			setForeground(selected ? list.getSelectionForeground() : list.getForeground());
			if (choice == null) {
				setText("");
				setIcon(null);
			} else if (choice.moreColors) {
				setText(Messages.getString("Gantt.FormatBar.moreColors"));
				setIcon(null);
			} else {
				setText(colorLabel(choice.rgb));
				setIcon(choice.rgb == null ? null : new ColorSwatchIcon(new Color(choice.rgb)));
			}
			return this;
		}
	}

	private static final class ColorSwatchIcon implements javax.swing.Icon {
		private final Color color;

		private ColorSwatchIcon(Color color) {
			this.color = color;
		}

		@Override
		public int getIconWidth() {
			return 18;
		}

		@Override
		public int getIconHeight() {
			return 12;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			graphics.setColor(color);
			graphics.fillRect(x, y, getIconWidth(), getIconHeight());
			graphics.setColor(Color.DARK_GRAY);
			graphics.drawRect(x, y, getIconWidth() - 1, getIconHeight() - 1);
		}
	}

	private static final class BarPreview extends JComponent {
		private static final long serialVersionUID = 1L;
		private final boolean milestone;
		private final boolean summary;
		private ColorField start;
		private ColorField middle;
		private ColorField end;

		private BarPreview(boolean milestone, boolean summary) {
			this.milestone = milestone;
			this.summary = summary;
			setPreferredSize(new Dimension(390, 72));
			setMinimumSize(new Dimension(280, 58));
			setBorder(BorderFactory.createEtchedBorder());
		}

		private void setFields(ColorField start, ColorField middle, ColorField end) {
			this.start = start;
			this.middle = middle;
			this.end = end;
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			if (start == null || middle == null || end == null)
				return;
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int centerY = getHeight() / 2;
				int left = 42;
				int right = Math.max(left + 40, getWidth() - 42);
				if (milestone) {
					paintDiamond(g2, left + (right - left) / 2, centerY, resolve(start.getRgb()));
					return;
				}

				Color middleColor = resolve(middle.getRgb());
				if (summary) {
					g2.setColor(middleColor);
					g2.fillRect(left, centerY - 3, right - left, 6);
					paintSummaryEnd(g2, left, centerY, resolve(start.getRgb()), false);
					paintSummaryEnd(g2, right, centerY, resolve(end.getRgb()), true);
				} else {
					g2.setColor(middleColor);
					g2.fillRoundRect(left, centerY - 8, right - left, 16, 10, 10);
					if (start.getRgb() != null)
						paintDiamond(g2, left, centerY, resolve(start.getRgb()));
					if (end.getRgb() != null)
						paintDiamond(g2, right, centerY, resolve(end.getRgb()));
				}
			} finally {
				g2.dispose();
			}
		}

		private static Color resolve(Integer rgb) {
			return new Color(rgb == null ? DEFAULT_BAR_RGB : rgb);
		}

		private static void paintDiamond(Graphics2D graphics, int x, int y, Color color) {
			graphics.setColor(color);
			graphics.fillPolygon(
					new int[] { x, x + 9, x, x - 9 },
					new int[] { y - 9, y, y + 9, y },
					4);
		}

		private static void paintSummaryEnd(Graphics2D graphics, int x, int y, Color color, boolean right) {
			int direction = right ? -1 : 1;
			graphics.setColor(color);
			graphics.fillPolygon(
					new int[] { x, x + direction * 10, x },
					new int[] { y - 6, y - 6, y + 8 },
					3);
		}
	}
}
