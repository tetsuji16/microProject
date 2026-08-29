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
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.microproject.graphic.configuration.GanttBarFormatOverrides.BarFormat;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;

/**
 * Individual Gantt bar formatting, corresponding to Microsoft Project's
 * Format Bar dialog. The current feature scope exposes the color properties.
 */
final class GanttBarFormatDialog {

	private GanttBarFormatDialog() {
	}

	static void show(Component parent, Gantt gantt, Task task) {
		BarFormat original = gantt.getBarFormat(task);
		// Automatic fields must preview the same palette colors as the bar that
		// opened this dialog.  Do not fall back to the editor's generic blue.
		FormatPanel fields = new FormatPanel(parent, original, gantt.getDisplayedBarColors(task),
				task.isMilestone(), task.isSummary());

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

	/** Retained for the Gantt context menu; the editor owns the chooser UI. */
	static Color chooseColor(Component parent, Integer currentRgb) {
		return BarColorField.chooseColor(parent, currentRgb);
	}

	static String colorLabel(Integer rgb) {
		return BarColorField.colorLabel(rgb);
	}

	static JPanel createPanelForTest(BarFormat format, boolean milestone, boolean summary) {
		return createPanelForTest(format, new GanttRenderer.DisplayedBarColors(
				BarColorField.DEFAULT_BAR_RGB,
				BarColorField.DEFAULT_BAR_RGB,
				BarColorField.DEFAULT_BAR_RGB), milestone, summary);
	}

	static JPanel createPanelForTest(BarFormat format, GanttRenderer.DisplayedBarColors displayedColors,
			boolean milestone, boolean summary) {
		return new FormatPanel(null, format, displayedColors, milestone, summary);
	}

	private static final class FormatPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private final BarColorEditorPanel editor;
		private final JComboBox<MilestoneShape> milestoneShape;

		private FormatPanel(Component parent, BarFormat format, GanttRenderer.DisplayedBarColors displayedColors,
				boolean milestone, boolean summary) {
			super(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

			editor = new BarColorEditorPanel(parent, format, displayedColors, milestone, summary, null);
			milestoneShape = milestone ? new JComboBox<>(MilestoneShape.values()) : null;
			if (milestoneShape != null)
				milestoneShape.setSelectedItem(MilestoneShape.forName(format.getMilestoneShapeName()));

			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab(Messages.getString("Gantt.FormatBar.barColor"), editor);
			if (milestoneShape != null) {
				JPanel shapePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
				shapePanel.add(new javax.swing.JLabel(Messages.getString("Gantt.FormatBar.milestoneShape")));
				shapePanel.add(milestoneShape);
				tabs.addTab(Messages.getString("Gantt.FormatBar.barShape"), shapePanel);
			}
			add(tabs, BorderLayout.CENTER);

			JButton reset = new JButton(Messages.getString("Gantt.FormatBar.reset"));
			reset.addActionListener(event -> {
				editor.getStart().setRgb(null);
				editor.getMiddle().setRgb(null);
				editor.getEnd().setRgb(null);
				if (milestoneShape != null)
					milestoneShape.setSelectedItem(MilestoneShape.AUTOMATIC);
			});
			JPanel resetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
			resetPanel.add(reset);
			add(resetPanel, BorderLayout.SOUTH);
		}

		private BarFormat getFormat() {
			BarFormat format = editor.getFormat();
			return milestoneShape == null ? format : format.withMilestoneShapeName(
					((MilestoneShape)milestoneShape.getSelectedItem()).shapeName);
		}
	}

	enum MilestoneShape {
		AUTOMATIC(null, "Gantt.FormatBar.automatic"),
		DIAMOND("DIAMOND", "Gantt.FormatBar.shapeDiamond"),
		SQUARE("SQUARE", "Gantt.FormatBar.shapeSquare"),
		TRIANGLE_UP("TRIANGLE_UP", "Gantt.FormatBar.shapeTriangleUp"),
		TRIANGLE_DOWN("TRIANGLE_DOWN", "Gantt.FormatBar.shapeTriangleDown");

		private final String shapeName;
		private final String labelKey;

		MilestoneShape(String shapeName, String labelKey) {
			this.shapeName = shapeName;
			this.labelKey = labelKey;
		}

		static MilestoneShape forName(String shapeName) {
			for (MilestoneShape shape : values()) if (java.util.Objects.equals(shape.shapeName, shapeName)) return shape;
			return AUTOMATIC;
		}

		@Override
		public String toString() {
			return Messages.getString(labelKey);
		}
	}
}
