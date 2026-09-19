/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.gantt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import java.awt.RenderingHints;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.microproject.strings.Messages;

/**
 * A control that selects a bar color (start, middle, or end), matching the
 * Microsoft Project "Bar color" layout: a color swatch plus an "Automatic"
 * checkbox. When "Automatic" is checked the color is null and the rendering
 * falls back to the view palette; unchecking it (or picking a color) stores an
 * explicit RGB. Implemented as a JPanel so the swatch paints reliably under any
 * Look&Feel (issue #16).
 */
public final class BarColorField extends JPanel {
	private static final long serialVersionUID = 1L;
	static final int DEFAULT_BAR_RGB = 0x5B9BD5;
	static final int[] STANDARD_COLORS = {
		0x000000, 0x7F7F7F, 0xA5A5A5, 0xFFFFFF,
		0xC00000, 0xED7D31, 0xFFC000, 0x70AD47,
		0x5B9BD5, 0x4472C4, 0x7030A0
	};

	private final ColorSwatchButton swatch;
	private final JLabel label;
	private final JCheckBox automatic;
	private final String colorRole;
	private Integer rgb;
	private final Integer automaticRgb;
	private final Runnable changeListener;
	private boolean adjusting;
	private boolean initialized;

	public BarColorField(Component parent, Integer rgb, Runnable changeListener) {
		this(parent, rgb, DEFAULT_BAR_RGB, "Gantt.FormatBar.color", changeListener); //$NON-NLS-1$
	}

	public BarColorField(Component parent, Integer rgb, Integer automaticRgb, Runnable changeListener) {
		this(parent, rgb, automaticRgb, "Gantt.FormatBar.color", changeListener); //$NON-NLS-1$
	}

	public BarColorField(Component parent, Integer rgb, Integer automaticRgb, String colorRoleKey,
			Runnable changeListener) {
		super(new BorderLayout(4, 0));
		this.changeListener = changeListener;
		this.automaticRgb = automaticRgb == null ? DEFAULT_BAR_RGB : automaticRgb & 0x00FFFFFF;
		colorRole = Messages.getString(colorRoleKey);
		swatch = new ColorSwatchButton(parent, this::onColorChosen);
		swatch.setToolTipText(colorChooserDescription());
		swatch.getAccessibleContext().setAccessibleName(colorChooserDescription());
		swatch.getAccessibleContext().setAccessibleDescription(colorChooserDescription());
		label = new JLabel();
		label.setLabelFor(swatch);
		automatic = new JCheckBox(Messages.getString("Gantt.FormatBar.automatic"));
		automatic.setToolTipText(automaticDescription());
		automatic.getAccessibleContext().setAccessibleName(automaticDescription());
		automatic.getAccessibleContext().setAccessibleDescription(automaticDescription());
		automatic.addActionListener(e -> automaticToggled());
		add(swatch, BorderLayout.WEST);
		add(label, BorderLayout.CENTER);
		add(automatic, BorderLayout.EAST);
		setOpaque(false);
		setRgb(rgb);
		initialized = true;
	}

	private void onColorChosen(Color color) {
		if (color != null) {
			setRgb(color.getRGB() & 0x00FFFFFF);
			automatic.setSelected(false);
		}
	}

	private void automaticToggled() {
		if (adjusting)
			return;
		if (automatic.isSelected())
			setRgb(null);
		else
			setRgb(getDisplayRgb());
	}

	public void setRgb(Integer rgb) {
		this.rgb = rgb == null ? null : rgb & 0x00FFFFFF;
		adjusting = true;
		swatch.setColor(getDisplayRgb());
		label.setText(colorLabel(this.rgb));
		automatic.setSelected(this.rgb == null);
		adjusting = false;
		// Initial values are model state, not a user edit. In particular, a
		// dialog creates three of these controls one by one; notifying here
		// would read the two controls that have not yet been assigned.
		if (initialized && changeListener != null)
			changeListener.run();
	}

	public Integer getRgb() {
		return rgb;
	}

	/** Returns the actual color currently displayed by the Gantt renderer. */
	public Integer getDisplayRgb() {
		return rgb == null ? automaticRgb : rgb;
	}

	/**
	 * A disabled container does not disable its Swing children automatically.
	 * Keep the swatch and Automatic checkbox in the same state so a read-only
	 * task cannot appear editable.
	 */
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (swatch != null)
			swatch.setEnabled(enabled);
		if (label != null)
			label.setEnabled(enabled);
		if (automatic != null)
			automatic.setEnabled(enabled);
	}

	JButton getSwatchButton() {
		return swatch;
	}

	JCheckBox getAutomaticCheckBox() {
		return automatic;
	}

	private String colorChooserDescription() {
		return Messages.format("Format.labelValue", Messages.getString("Gantt.FormatBar.chooseColor"), colorRole); //$NON-NLS-1$
	}

	private String automaticDescription() {
		return Messages.format("Format.labelValue", colorRole, Messages.getString("Gantt.FormatBar.automatic")); //$NON-NLS-1$
	}

	static Color chooseColor(Component parent, Integer currentRgb) {
		Color initial = currentRgb == null ? new Color(DEFAULT_BAR_RGB) : new Color(currentRgb);
		return JColorChooser.showDialog(parent, Messages.getString("Gantt.FormatBar.chooseColor"), initial);
	}

	static String colorLabel(Integer rgb) {
		// When automatic (rgb == null) the label is redundant with the "Automatic"
		// checkbox, so leave it blank. Only show the hex code for an explicit color.
		return rgb == null ? "" : String.format(Locale.ROOT, "#%06X", rgb & 0x00FFFFFF);
	}

	/**
	 * Builds the three-column grid (start / middle / end) used to display the bar
	 * colors. This mirrors the proven {@code GanttBarFormatDialog.FormatPanel}
	 * layout (centered column headers over each color field), so the Task
	 * Information dialog and the Format Bar dialog render identically. Returns a
	 * panel whose three color fields are laid out in a single nested grid.
	 */
	public static JPanel createThreeColumnGrid(BarColorField start, BarColorField middle, BarColorField end) {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
		addCell(panel, new JLabel(), 0, 0, 0.0d);
		addCell(panel, labeledHeader("Gantt.FormatBar.start", start), 1, 0, 1.0d); //$NON-NLS-1$
		addCell(panel, labeledHeader("Gantt.FormatBar.middle", middle), 2, 0, 1.0d); //$NON-NLS-1$
		addCell(panel, labeledHeader("Gantt.FormatBar.end", end), 3, 0, 1.0d); //$NON-NLS-1$
		addCell(panel, new JLabel(Messages.getString("Gantt.FormatBar.color")), 0, 1, 0.0d); //$NON-NLS-1$
		addCell(panel, start, 1, 1, 1.0d);
		addCell(panel, middle, 2, 1, 1.0d);
		addCell(panel, end, 3, 1, 1.0d);
		return panel;
	}

	private static JLabel centeredLabel(String key) {
		return new JLabel(Messages.getString(key), JLabel.CENTER);
	}

	private static JLabel labeledHeader(String key, BarColorField field) {
		JLabel header = centeredLabel(key);
		header.setLabelFor(field.getSwatchButton());
		return header;
	}

	static void addCell(JPanel panel, JComponent component, int x, int y, double weightX) {
		panel.add(component, constraints(x, y, weightX));
	}

	static GridBagConstraints constraints(int x, int y, double weightX) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.weightx = weightX;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(4, x == 0 ? 0 : 6, 4, 0);
		return constraints;
	}

	/** A button that paints its current color as a swatch and opens a color chooser on click. */
	private static final class ColorSwatchButton extends JButton {
		private static final long serialVersionUID = 1L;
		private Integer rgb;
		private final Component parent;
		private final Consumer<Color> onChosen;

		ColorSwatchButton(Component parent, Consumer<Color> onChosen) {
			super();
			this.parent = parent;
			this.onChosen = onChosen;
			setPreferredSize(new Dimension(42, 22));
			addActionListener(e -> {
				Color initial = this.rgb == null ? new Color(DEFAULT_BAR_RGB) : new Color(this.rgb);
				Color chosen = JColorChooser.showDialog(parent, Messages.getString("Gantt.FormatBar.chooseColor"), initial);
				if (chosen != null)
					onChosen.accept(chosen);
			});
		}

		void setColor(Integer rgb) {
			this.rgb = rgb;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int x = 4, y = 4, w = getWidth() - 8, h = getHeight() - 8;
				Color fill = this.rgb == null ? new Color(DEFAULT_BAR_RGB) : new Color(this.rgb);
				g2.setColor(fill);
				g2.fillRect(x, y, w, h);
				g2.setColor(Color.DARK_GRAY);
				g2.drawRect(x, y, w, h);
			} finally {
				g2.dispose();
			}
		}
	}
}
