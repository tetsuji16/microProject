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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.microproject.pm.resource.TeamPlannerService;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.util.Alert;
import com.microproject.help.HelpUtil;
import com.microproject.util.PopupDialogSupport;

/** Month calendar with task cards and drag-to-reschedule support. */
public final class CalendarViewDialogBox extends JDialog {
	private static final long serialVersionUID = 1L;
	private final JLabel monthLabel = new JLabel();
	private final CalendarCanvas canvas;
	private YearMonth month = YearMonth.now();

	public CalendarViewDialogBox(java.awt.Frame owner, Project project) {
		super(owner, UsabilityStrings.text("calendar.title"), false);
		HelpUtil.addDocHelp(getRootPane(), "Calendar_View");
		getAccessibleContext().setAccessibleDescription(UsabilityStrings.text("calendar.hint"));
		PopupDialogSupport.bindEscapeToDispose(this);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(6, 6));
		canvas = new CalendarCanvas(project);
		JPanel navigation = new JPanel();
		JButton previous = new JButton(UsabilityStrings.text("common.previous"));
		JButton today = new JButton(UsabilityStrings.text("common.today"));
		JButton next = new JButton(UsabilityStrings.text("common.next"));
		previous.addActionListener(event -> setMonth(month.minusMonths(1)));
		today.addActionListener(event -> setMonth(YearMonth.now()));
		next.addActionListener(event -> setMonth(month.plusMonths(1)));
		monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 16F));
		navigation.add(previous); navigation.add(today); navigation.add(next); navigation.add(monthLabel);
		add(navigation, BorderLayout.NORTH);
		add(new JScrollPane(canvas), BorderLayout.CENTER);
		JLabel hint = new JLabel(UsabilityStrings.text("calendar.hint"));
		add(hint, BorderLayout.SOUTH);
		setMonth(month);
		setMinimumSize(new Dimension(900, 600));
		setSize(1100, 760);
		setLocationRelativeTo(owner);
	}

	private void setMonth(YearMonth value) {
		month = value;
		monthLabel.setText(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())));
		canvas.repaint();
	}

	/** Returns the number of task cards rendered during the latest repaint. */
	public int getVisibleTaskCardCount() {
		return canvas.cards.size();
	}

	/** Returns the month currently shown by the calendar navigation. */
	public String getDisplayedMonth() {
		return monthLabel.getText();
	}

	private final class CalendarCanvas extends JPanel {
		private static final long serialVersionUID = 1L;
		private static final int HEADER = 34;
		private static final int CELL_HEIGHT = 105;
		private final Project project;
		private final ZoneId zone = ZoneId.systemDefault();
		private final List<Card> cards = new ArrayList<>();
		private Task draggedTask;
		private Point dragPoint;

		CalendarCanvas(Project project) {
			this.project = project;
			setBackground(Color.WHITE);
			setPreferredSize(new Dimension(980, HEADER + CELL_HEIGHT * 6));
			MouseAdapter mouse = new MouseAdapter() {
				@Override public void mousePressed(MouseEvent event) {
					for (Card card : cards) if (card.bounds().contains(event.getPoint())) { draggedTask = card.task(); dragPoint = event.getPoint(); break; }
				}
				@Override public void mouseDragged(MouseEvent event) { if (draggedTask != null) { dragPoint = event.getPoint(); repaint(); } }
				@Override public void mouseReleased(MouseEvent event) {
					if (draggedTask != null) moveTask(draggedTask, dateAt(event.getPoint()));
					draggedTask = null; dragPoint = null; repaint();
				}
			};
			addMouseListener(mouse); addMouseMotionListener(mouse);
			getAccessibleContext().setAccessibleName(UsabilityStrings.text("calendar.accessible"));
		}

		@Override protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				cards.clear();
				int width = Math.max(1, getWidth() / 7);
				for (int column = 0; column < 7; column++) {
					DayOfWeek day = DayOfWeek.of(column + 1);
					g.setColor(new Color(0x455A64));
					g.drawString(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), column * width + 8, 22);
				}
				LocalDate first = month.atDay(1);
				LocalDate gridStart = first.minusDays(first.getDayOfWeek().getValue() - 1L);
				for (int index = 0; index < 42; index++) paintDay(g, gridStart.plusDays(index), index, width);
				if (draggedTask != null && dragPoint != null) {
					g.setColor(new Color(0x15, 0x65, 0xC0, 160));
					g.fillRoundRect(dragPoint.x - 55, dragPoint.y - 10, 110, 20, 8, 8);
					g.setColor(Color.WHITE); g.drawString(draggedTask.getName(), dragPoint.x - 50, dragPoint.y + 5);
				}
			} finally { g.dispose(); }
		}

		private void paintDay(Graphics2D g, LocalDate day, int index, int width) {
			int column = index % 7, row = index / 7, x = column * width, y = HEADER + row * CELL_HEIGHT;
			boolean currentMonth = YearMonth.from(day).equals(month);
			g.setColor(currentMonth ? Color.WHITE : new Color(0xF4F5F7)); g.fillRect(x, y, width, CELL_HEIGHT);
			g.setColor(new Color(0xD6DADE)); g.drawRect(x, y, width, CELL_HEIGHT);
			if (day.equals(LocalDate.now())) { g.setColor(new Color(0xD32F2F)); g.setStroke(new BasicStroke(2F)); g.drawRect(x + 1, y + 1, width - 2, CELL_HEIGHT - 2); }
			g.setColor(currentMonth ? new Color(0x263238) : Color.GRAY); g.drawString(Integer.toString(day.getDayOfMonth()), x + 7, y + 17);
			List<Task> onDay = tasksOn(day);
			int visible = Math.min(3, onDay.size());
			for (int i = 0; i < visible; i++) {
				Task task = onDay.get(i); Rectangle bounds = new Rectangle(x + 5, y + 23 + i * 23, Math.max(20, width - 10), 19);
				g.setColor(task.isInactiveTask() ? new Color(0x9E9E9E) : task.isManuallyScheduled() ? new Color(0x2E7D32) : new Color(0x1565C0));
				g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 7, 7);
				g.setColor(Color.WHITE); String name = task.getName();
				while (name.length() > 2 && g.getFontMetrics().stringWidth(name) > bounds.width - 8) name = name.substring(0, name.length() - 2) + "…";
				g.drawString(name, bounds.x + 4, bounds.y + 14); cards.add(new Card(task, bounds));
			}
			if (onDay.size() > visible) { g.setColor(Color.DARK_GRAY); g.drawString("+" + (onDay.size() - visible) + " more", x + 7, y + 96); }
		}

		private List<Task> tasksOn(LocalDate day) {
			List<Task> result = new ArrayList<>();
			for (var iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
				Task task = (Task) iterator.next();
				if (task.isSummary()) continue;
				LocalDate start = Instant.ofEpochMilli(task.getStart()).atZone(zone).toLocalDate();
				LocalDate end = Instant.ofEpochMilli(task.getEnd()).atZone(zone).toLocalDate();
				if (!day.isBefore(start) && !day.isAfter(end)) result.add(task);
			}
			return result;
		}

		private LocalDate dateAt(Point point) {
			if (point == null || point.y < HEADER) return null;
			int column = Math.max(0, Math.min(6, point.x / Math.max(1, getWidth() / 7)));
			int row = Math.max(0, Math.min(5, (point.y - HEADER) / CELL_HEIGHT));
			LocalDate first = month.atDay(1);
			return first.minusDays(first.getDayOfWeek().getValue() - 1L).plusDays(row * 7L + column);
		}

		private void moveTask(Task task, LocalDate day) {
			if (day == null || task.isReadOnly() || task.inProgress() || task.isInactiveTask()) return;
			try {
				long rawStart = day.atStartOfDay(zone).toInstant().toEpochMilli();
				long newStart = task.getEffectiveWorkCalendar().adjustInsideCalendar(rawStart, false);
				if (task.isManuallyScheduled()) task.setManualDates(newStart, newStart + Math.max(0L, task.getEnd() - task.getStart()));
				else new TeamPlannerService().reschedule(task, newStart, this);
				project.recalculate();
			} catch (RuntimeException exception) { Alert.error(exception.getMessage()); }
		}
	}

	private record Card(Task task, Rectangle bounds) { }
}
