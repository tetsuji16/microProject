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

		private FormatPanel(Component parent, BarFormat format, GanttRenderer.DisplayedBarColors displayedColors,
				boolean milestone, boolean summary) {
			super(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

			editor = new BarColorEditorPanel(parent, format, displayedColors, milestone, summary, null);

			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab(Messages.getString("Gantt.FormatBar.barColor"), editor);
			add(tabs, BorderLayout.CENTER);

			JButton reset = new JButton(Messages.getString("Gantt.FormatBar.reset"));
			reset.addActionListener(event -> {
				editor.getStart().setRgb(null);
				editor.getMiddle().setRgb(null);
				editor.getEnd().setRgb(null);
			});
			JPanel resetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
			resetPanel.add(reset);
			add(resetPanel, BorderLayout.SOUTH);
		}

		private BarFormat getFormat() {
			return editor.getFormat();
		}
	}
}
