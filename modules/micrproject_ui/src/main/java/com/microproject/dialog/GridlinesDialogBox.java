/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.views.GanttView;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.PopupDialogSupport;

/** MSP-style line-specific formatting for the active Gantt chart. */
public final class GridlinesDialogBox extends JDialog {
	private static final long serialVersionUID = 1L;
	private enum Target { TASK_ROWS, PROJECT_START, CURRENT_DATE, STATUS_DATE, TIMESCALE_MAJOR, TIMESCALE_MINOR, NONWORKING_BOUNDARY }

	public static void show(Frame owner, GanttView view) {
		if (view == null || java.awt.GraphicsEnvironment.isHeadless()) return;
		new GridlinesDialogBox(owner, view).setVisible(true);
	}

	private GridlinesDialogBox(Frame owner, GanttView view) {
		super(owner, UsabilityStrings.text("gridlines.title"), true);
		FlatUiSupport.styleDialogRoot(getRootPane());
		PopupDialogSupport.bindEscapeToDispose(this);
		Gantt gantt = view.getGantt();
		JComboBox<Target> target = new JComboBox<>(Target.values());
		target.setRenderer(new javax.swing.DefaultListCellRenderer() {
			@Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
				boolean selected, boolean focused) {
				return super.getListCellRendererComponent(list, value == Target.TASK_ROWS ? UsabilityStrings.text("gridlines.taskRows")
					: value == Target.PROJECT_START ? UsabilityStrings.text("gridlines.projectStart")
					: value == Target.CURRENT_DATE ? UsabilityStrings.text("gridlines.currentDate")
					: value == Target.STATUS_DATE ? UsabilityStrings.text("gridlines.statusDate")
					: value == Target.TIMESCALE_MAJOR ? UsabilityStrings.text("gridlines.timescaleMajor")
					: value == Target.TIMESCALE_MINOR ? UsabilityStrings.text("gridlines.timescaleMinor")
					: UsabilityStrings.text("gridlines.nonWorkingBoundary"), index, selected, focused);
			}
		});
		JCheckBox visible = new JCheckBox();
		JComboBox<String> style = new JComboBox<>(new String[] {
			UsabilityStrings.text("gridlines.solid"), UsabilityStrings.text("gridlines.dash"), UsabilityStrings.text("gridlines.dot")
		});
		JButton color = new JButton(UsabilityStrings.text("gridlines.chooseColor"));
		final Color[] selectedColor = { gColor(gantt, Target.TASK_ROWS) };
		color.setBackground(selectedColor[0]);
		color.addActionListener(event -> {
			Color chosen = JColorChooser.showDialog(this, UsabilityStrings.text("gridlines.chooseColor"), selectedColor[0]);
			if (chosen != null) { selectedColor[0] = chosen; color.setBackground(chosen); }
		});
		Runnable load = () -> {
			Target value = (Target) target.getSelectedItem();
			visible.setSelected(value == Target.TASK_ROWS ? view.isSpreadsheetGridVisible()
				: value == Target.PROJECT_START ? gantt.isProjectStartLineVisible()
				: value == Target.CURRENT_DATE ? gantt.isCurrentDateLineVisible()
				: value == Target.STATUS_DATE ? gantt.isStatusDateLineVisible()
				: value == Target.TIMESCALE_MAJOR ? gantt.isTimescaleMajorLineVisible()
				: value == Target.TIMESCALE_MINOR ? gantt.isTimescaleMinorLineVisible() : gantt.isNonWorkingBoundaryVisible());
			style.setSelectedIndex(styleIndex(styleValue(gantt, value)));
			selectedColor[0] = gColor(gantt, value);
			color.setBackground(selectedColor[0]);
		};
		target.addActionListener(event -> load.run());
		load.run();

		JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));
		GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(4, 4, 4, 8); c.anchor = GridBagConstraints.WEST;
		c.gridx = 0; c.gridy = 0; form.add(new JLabel(UsabilityStrings.text("gridlines.target")), c);
		c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; form.add(target, c);
		c.gridx = 0; c.gridy++; c.weightx = 0; c.fill = GridBagConstraints.NONE; form.add(new JLabel(UsabilityStrings.text("gridlines.visible")), c);
		c.gridx = 1; form.add(visible, c);
		c.gridx = 0; c.gridy++; form.add(new JLabel(UsabilityStrings.text("gridlines.style")), c);
		c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; form.add(style, c);
		c.gridx = 0; c.gridy++; c.fill = GridBagConstraints.NONE; form.add(new JLabel(UsabilityStrings.text("gridlines.color")), c);
		c.gridx = 1; form.add(color, c);
		JButton ok = new JButton(UsabilityStrings.text("common.apply"));
		JButton cancel = new JButton(UsabilityStrings.text("common.close"));
		ok.addActionListener(event -> { apply(view, gantt, (Target) target.getSelectedItem(), visible.isSelected(), style.getSelectedIndex(), selectedColor[0]); dispose(); });
		cancel.addActionListener(event -> dispose());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(ok); buttons.add(cancel);
		setLayout(new BorderLayout()); add(form, BorderLayout.CENTER); add(buttons, BorderLayout.SOUTH);
		setPreferredSize(new java.awt.Dimension(460, 245)); pack(); setLocationRelativeTo(owner);
	}

	private static Color gColor(Gantt gantt, Target target) {
		Color color = target == Target.PROJECT_START ? gantt.getProjectLineColor()
			: target == Target.STATUS_DATE ? gantt.getStatusDateLineColor()
			: target == Target.CURRENT_DATE ? gantt.getCurrentDateLineColor()
				: target == Target.TIMESCALE_MAJOR ? gantt.getTimescaleMajorLineColor()
				: target == Target.TIMESCALE_MINOR ? gantt.getTimescaleMinorLineColor()
				: target == Target.NONWORKING_BOUNDARY ? gantt.getNonWorkingBoundaryColor() : gantt.getGridLineColor();
		return color == null ? gantt.getGridLineColor() : color;
	}
	private static int styleIndex(int style) { return style == 6 ? 2 : style == 7 ? 2 : style == 5 ? 1 : 0; }
	private static int styleValue(int index) { return index == 1 ? 5 : index == 2 ? 7 : 0; }
	private static int styleValue(Gantt gantt, Target target) {
		return target == Target.PROJECT_START ? gantt.getProjectLineStyle()
			: target == Target.CURRENT_DATE ? gantt.getCurrentDateLineStyle()
			: target == Target.STATUS_DATE ? gantt.getStatusDateLineStyle()
			: target == Target.TIMESCALE_MAJOR ? gantt.getTimescaleMajorLineStyle()
			: target == Target.TIMESCALE_MINOR ? gantt.getTimescaleMinorLineStyle()
			: target == Target.NONWORKING_BOUNDARY ? gantt.getNonWorkingBoundaryStyle() : 0;
	}
	private static void apply(GanttView view, Gantt gantt, Target target, boolean visible, int styleIndex, Color color) {
		if (target == Target.TASK_ROWS) view.setSpreadsheetGridVisible(visible);
		else if (target == Target.PROJECT_START) { gantt.setProjectStartLineVisible(visible); gantt.setProjectLineStyle(styleValue(styleIndex)); }
		else if (target == Target.CURRENT_DATE) { gantt.setCurrentDateLineVisible(visible); gantt.setCurrentDateLineStyle(styleValue(styleIndex)); gantt.setCurrentDateLineColor(color); }
		else if (target == Target.STATUS_DATE) { gantt.setStatusDateLineVisible(visible); gantt.setStatusDateLineStyle(styleValue(styleIndex)); gantt.setStatusDateLineColor(color); }
		else if (target == Target.TIMESCALE_MAJOR) { gantt.setTimescaleMajorLineVisible(visible); gantt.setTimescaleMajorLineStyle(styleValue(styleIndex)); gantt.setTimescaleMajorLineColor(color); }
		else if (target == Target.TIMESCALE_MINOR) { gantt.setTimescaleMinorLineVisible(visible); gantt.setTimescaleMinorLineStyle(styleValue(styleIndex)); gantt.setTimescaleMinorLineColor(color); }
		else { gantt.setNonWorkingBoundaryVisible(visible); gantt.setNonWorkingBoundaryStyle(styleValue(styleIndex)); gantt.setNonWorkingBoundaryColor(color); }
		if (target == Target.PROJECT_START) gantt.setProjectLineColor(color);
		if (target == Target.TASK_ROWS) gantt.setGridLineColor(color);
		gantt.repaint();
	}
}
