package com.projectlibre1.pm.graphic.gantt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.projectlibre1.graphic.configuration.GanttBarFormatOverrides.BarFormat;
import com.projectlibre1.strings.Messages;

/**
 * Shared editor and preview for a task's start, middle and end bar colors.
 * Both Task Information and Format Bar use this component so their controls
 * cannot diverge in layout or color-update behavior.
 */
public final class BarColorEditorPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private final BarColorField start;
	private final BarColorField middle;
	private final BarColorField end;

	public BarColorEditorPanel(Component parent, BarFormat format, boolean milestone, boolean summary,
			Runnable changeListener) {
		this(parent, format, new GanttRenderer.DisplayedBarColors(
				BarColorField.DEFAULT_BAR_RGB, BarColorField.DEFAULT_BAR_RGB, BarColorField.DEFAULT_BAR_RGB),
				milestone, summary, changeListener);
	}

	public BarColorEditorPanel(Component parent, BarFormat format, GanttRenderer.DisplayedBarColors displayedColors,
			boolean milestone, boolean summary, Runnable changeListener) {
		super(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		BarPreview preview = new BarPreview(milestone, summary);
		Runnable onChange = () -> {
			preview.repaint();
			if (changeListener != null)
				changeListener.run();
		};
		start = new BarColorField(parent, format.getStartRgb(), displayedColors.startRgb(),
				"Gantt.FormatBar.startColor", onChange); //$NON-NLS-1$
		middle = new BarColorField(parent, format.getMiddleRgb(), displayedColors.middleRgb(),
				"Gantt.FormatBar.middleColor", onChange); //$NON-NLS-1$
		end = new BarColorField(parent, format.getEndRgb(), displayedColors.endRgb(),
				"Gantt.FormatBar.endColor", onChange); //$NON-NLS-1$
		preview.setFields(start, middle, end);

		JPanel grid = BarColorField.createThreeColumnGrid(start, middle, end);
		GridBagConstraints sampleLabel = BarColorField.constraints(0, 2, 0.0d);
		sampleLabel.anchor = GridBagConstraints.NORTHWEST;
		grid.add(new JLabel(Messages.getString("Gantt.FormatBar.sample")), sampleLabel); //$NON-NLS-1$
		GridBagConstraints sample = BarColorField.constraints(1, 2, 1.0d);
		sample.gridwidth = 3;
		sample.fill = GridBagConstraints.BOTH;
		sample.weighty = 1.0d;
		grid.add(preview, sample);
		add(grid, BorderLayout.CENTER);
	}

	public BarColorField getStart() {
		return start;
	}

	public BarColorField getMiddle() {
		return middle;
	}

	public BarColorField getEnd() {
		return end;
	}

	public BarFormat getFormat() {
		return new BarFormat(start.getRgb(), middle.getRgb(), end.getRgb());
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (start != null)
			start.setEnabled(enabled);
		if (middle != null)
			middle.setEnabled(enabled);
		if (end != null)
			end.setEnabled(enabled);
	}

	private static final class BarPreview extends JComponent {
		private static final long serialVersionUID = 1L;
		private final boolean milestone;
		private final boolean summary;
		private BarColorField start;
		private BarColorField middle;
		private BarColorField end;

		private BarPreview(boolean milestone, boolean summary) {
			this.milestone = milestone;
			this.summary = summary;
			setPreferredSize(new Dimension(390, 72));
			setMinimumSize(new Dimension(280, 58));
			setBorder(BorderFactory.createEtchedBorder());
		}

		private void setFields(BarColorField start, BarColorField middle, BarColorField end) {
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
					paintDiamond(g2, left + (right - left) / 2, centerY, resolve(start.getDisplayRgb()));
					return;
				}
				Color middleColor = resolve(middle.getDisplayRgb());
				if (summary) {
					g2.setColor(middleColor);
					g2.fillRect(left, centerY - 3, right - left, 6);
					paintSummaryEnd(g2, left, centerY, resolve(start.getDisplayRgb()), false);
					paintSummaryEnd(g2, right, centerY, resolve(end.getDisplayRgb()), true);
				} else {
					g2.setColor(middleColor);
					g2.fillRoundRect(left, centerY - 8, right - left, 16, 10, 10);
					paintDiamond(g2, left, centerY, resolve(start.getDisplayRgb()));
					paintDiamond(g2, right, centerY, resolve(end.getDisplayRgb()));
				}
			} finally {
				g2.dispose();
			}
		}

		private static Color resolve(Integer rgb) {
			return new Color(rgb == null ? BarColorField.DEFAULT_BAR_RGB : rgb);
		}

		private static void paintDiamond(Graphics2D graphics, int x, int y, Color color) {
			graphics.setColor(color);
			graphics.fillPolygon(new int[] { x, x + 9, x, x - 9 }, new int[] { y - 9, y, y + 9, y }, 4);
		}

		private static void paintSummaryEnd(Graphics2D graphics, int x, int y, Color color, boolean right) {
			int direction = right ? -1 : 1;
			graphics.setColor(color);
			graphics.fillPolygon(new int[] { x, x + direction * 10, x }, new int[] { y - 6, y - 6, y + 8 }, 3);
		}
	}
}
