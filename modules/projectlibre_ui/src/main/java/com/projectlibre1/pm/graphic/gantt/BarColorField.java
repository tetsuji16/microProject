package com.projectlibre1.pm.graphic.gantt;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import com.projectlibre1.strings.Messages;

/**
 * A drop-down that selects a bar color (start, middle, or end). The "Automatic"
 * choice maps to {@code null} so the renderer can fall back to the view palette.
 * Reused by the Format Bar dialog and the Task Information dialog so the same
 * color selection UI is available in both places (issue #16).
 */
public final class BarColorField extends JComboBox<BarColorField.ColorChoice> {
	private static final long serialVersionUID = 1L;
	static final int DEFAULT_BAR_RGB = 0x5B9BD5;
	static final int[] STANDARD_COLORS = {
		0x000000, 0x7F7F7F, 0xA5A5A5, 0xFFFFFF,
		0xC00000, 0xED7D31, 0xFFC000, 0x70AD47,
		0x5B9BD5, 0x4472C4, 0x7030A0
	};

	private final Component parent;
	private final Runnable changeListener;
	private Integer rgb;
	private boolean adjusting;

	public BarColorField(Component parent, Integer rgb, Runnable changeListener) {
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

	public void setRgb(Integer rgb) {
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

	public Integer getRgb() {
		return rgb;
	}

	static Color chooseColor(Component parent, Integer currentRgb) {
		Color initial = currentRgb == null ? new Color(DEFAULT_BAR_RGB) : new Color(currentRgb);
		return JColorChooser.showDialog(parent, Messages.getString("Gantt.FormatBar.chooseColor"), initial);
	}

	static String colorLabel(Integer rgb) {
		return rgb == null ? Messages.getString("Gantt.FormatBar.automatic") : String.format("#%06X", rgb & 0x00FFFFFF);
	}

	static final class ColorChoice {
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
			setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));
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

	private static final class ColorSwatchIcon implements Icon {
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
}
