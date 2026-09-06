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
package com.microproject.dialog;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.help.HelpUtil;
import com.microproject.util.PopupDialogSupport;
import com.microproject.util.FlatUiSupport;

/** Concise, selectable project timeline suitable for status communication. */
public final class TimelineDialogBox extends JDialog {
	private static final long serialVersionUID = 1L;
	private final Project project;
	private final List<Task> tasks = new ArrayList<>();
	private final TimelineCanvas canvas = new TimelineCanvas();

	public TimelineDialogBox(Frame owner, Project project) {
		super(owner, UsabilityStrings.text("timeline.title"), false);
		HelpUtil.addDocHelp(getRootPane(), "Timeline");
		getAccessibleContext().setAccessibleDescription(UsabilityStrings.text("timeline.hint"));
		PopupDialogSupport.bindEscapeToDispose(this);
		this.project = project;
		for (var iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Task task = (Task) iterator.next();
			if (!task.isSummary()) tasks.add(task);
		}
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(8, 8));
		add(new JLabel(UsabilityStrings.text("timeline.hint")), BorderLayout.NORTH);
		JTable chooser = new JTable(new TaskChoiceModel());
		chooser.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		chooser.setAutoCreateRowSorter(true);
		chooser.getColumnModel().getColumn(0).setMaxWidth(52);
		JScrollPane timelineScroll = new JScrollPane(canvas);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(chooser), timelineScroll);
		split.setResizeWeight(0.28D);
		add(split, BorderLayout.CENTER);
		JPanel buttons = new JPanel();
		JButton selectMilestones = new JButton(UsabilityStrings.text("timeline.selectMilestones"));
		selectMilestones.addActionListener(event -> {
			for (Task task : tasks) if (task.isMilestone()) task.setDisplayOnTimeline(true);
			chooser.repaint(); canvas.refresh();
		});
		JButton clear = new JButton(UsabilityStrings.text("common.clear"));
		clear.addActionListener(event -> {
			for (Task task : tasks) task.setDisplayOnTimeline(false);
			chooser.repaint(); canvas.refresh();
		});
		JButton close = new JButton(UsabilityStrings.text("common.close"));
		close.addActionListener(event -> dispose());
		buttons.add(selectMilestones); buttons.add(clear); buttons.add(close);
		add(buttons, BorderLayout.SOUTH);
		setMinimumSize(new Dimension(900, 480));
		setSize(1100, 620);
		setLocationRelativeTo(owner);
		FlatUiSupport.styleDialogRoot(getRootPane());
		FlatUiSupport.styleDialogComponents(getContentPane());
	}

	/** Creates the same timeline UI for embedding in a document view. */
	public static JPanel createEmbeddedPanel(Frame owner, Project project) {
		TimelineDialogBox dialog = new TimelineDialogBox(owner, project);
		java.awt.Container content = dialog.getContentPane();
		if (!(content instanceof JPanel panel)) {
			dialog.dispose();
			throw new IllegalStateException("Timeline content is not a Swing panel");
		}
		// Detach the content from the dialog before disposing it. The panel keeps
		// all existing listeners and canvas state when adopted by a document view.
		dialog.getRootPane().setContentPane(new JPanel(new BorderLayout()));
		dialog.dispose();
		return panel;
	}

	private final class TaskChoiceModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		@Override public int getRowCount() { return tasks.size(); }
		@Override public int getColumnCount() { return 4; }
		@Override public String getColumnName(int column) {
			return switch (column) { case 0 -> UsabilityStrings.text("timeline.show"); case 1 -> UsabilityStrings.text("common.task"); case 2 -> UsabilityStrings.text("common.start"); default -> UsabilityStrings.text("common.finish"); };
		}
		@Override public Class<?> getColumnClass(int column) { return column == 0 ? Boolean.class : String.class; }
		@Override public boolean isCellEditable(int row, int column) { return column == 0 && !tasks.get(row).isReadOnly(); }
		@Override public Object getValueAt(int row, int column) {
			Task task = tasks.get(row);
			return switch (column) {
				case 0 -> task.isDisplayOnTimeline();
				case 1 -> task.getName();
				case 2 -> DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(task.getStart()));
				default -> DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(task.getEnd()));
			};
		}
		@Override public void setValueAt(Object value, int row, int column) {
			tasks.get(row).setDisplayOnTimeline(Boolean.TRUE.equals(value));
			fireTableCellUpdated(row, column);
			canvas.refresh();
		}
	}

	private final class TimelineCanvas extends JPanel {
		private static final long serialVersionUID = 1L;
		TimelineCanvas() { setBackground(FlatUiSupport.dataSurfaceBackground()); setPreferredSize(new Dimension(900, 520)); }
		void refresh() { revalidate(); repaint(); }
		@Override protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				List<Task> selected = tasks.stream().filter(Task::isDisplayOnTimeline).toList();
				if (selected.isEmpty()) { g.setColor(FlatUiSupport.disabledForeground()); g.drawString(UsabilityStrings.text("timeline.empty"), 24, 40); return; }
				long min = selected.stream().mapToLong(Task::getStart).min().orElse(project.getStart());
				long max = selected.stream().mapToLong(Task::getEnd).max().orElse(project.getEnd());
				if (max <= min) max = min + 1L;
				int left = 80, right = Math.max(left + 1, getWidth() - 40), axisY = 65;
				g.setColor(FlatUiSupport.headerForeground()); g.setStroke(new BasicStroke(2F)); g.drawLine(left, axisY, right, axisY);
				g.drawString(DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(min)), left, 42);
				String finish = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(max));
				FontMetrics fm = g.getFontMetrics(); g.drawString(finish, right - fm.stringWidth(finish), 42);
				int lane = 0;
				for (Task task : selected) {
					int y = axisY + 35 + lane++ * 42;
					int x1 = position(task.getStart(), min, max, left, right);
					int x2 = Math.max(x1 + 8, position(task.getEnd(), min, max, left, right));
					Color color = task.isInactiveTask() ? FlatUiSupport.disabledForeground()
						: task.isManuallyScheduled() ? FlatUiSupport.accentColor().darker() : FlatUiSupport.ribbonAccentColor();
					g.setColor(color);
					if (task.isMilestone()) {
						int[] xs = { x1, x1 + 8, x1, x1 - 8 }, ys = { y - 8, y, y + 8, y };
						g.fillPolygon(xs, ys, 4);
					} else g.fillRoundRect(x1, y - 8, x2 - x1, 16, 8, 8);
					g.setColor(FlatUiSupport.tableForeground()); g.drawString(task.getName(), Math.min(x2 + 8, right - 160), y + 5);
				}
				long now = System.currentTimeMillis();
				if (now >= min && now <= max) {
					int today = position(now, min, max, left, right);
					g.setColor(FlatUiSupport.errorForeground()); g.setStroke(new BasicStroke(1.5F));
					g.drawLine(today, axisY - 15, today, Math.max(axisY + 10, getHeight() - 20)); g.drawString(UsabilityStrings.text("common.today"), today + 4, axisY - 18);
				}
				setPreferredSize(new Dimension(Math.max(900, getWidth()), Math.max(520, axisY + selected.size() * 42 + 50)));
			} finally { g.dispose(); }
		}
		private int position(long value, long min, long max, int left, int right) {
			return left + (int) Math.round((value - min) * (double) (right - left) / (max - min));
		}
	}
}
