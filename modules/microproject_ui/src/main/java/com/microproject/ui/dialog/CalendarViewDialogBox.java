/*******************************************************************************
 * MIT License
 *
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
package com.microproject.ui.dialog;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.microproject.model.resource.TeamPlannerService;
import com.microproject.contrib.calendar.CalendarRenderingSupport;
import com.microproject.model.task.Project;
import com.microproject.model.task.Task;
import com.microproject.ui.privacy.PrivacyDisplayMode;
import com.microproject.ui.shell.WindowBoundsSupport;
import com.microproject.model.dependency.Dependency;
import com.microproject.model.dependency.DependencyService;
import com.microproject.model.dependency.HasDependencies;
import com.microproject.util.Alert;
import com.microproject.ui.help.HelpUtil;
import com.microproject.ui.util.PopupDialogSupport;
import com.microproject.ui.util.FlatUiSupport;
import com.microproject.ui.util.FlatLafDialog;
import com.microproject.util.DateTime;
import com.microproject.ui.util.AlternativeCalendarDisplay;

/** Month calendar with task cards and drag-to-reschedule support. */
public final class CalendarViewDialogBox extends FlatLafDialog {
	private static final long serialVersionUID = 1L;
	private final JLabel monthLabel = new JLabel();
	private final CalendarCanvas canvas;
	private final CalendarViewSettings settings = new CalendarViewSettings();
	private final javax.swing.DefaultListModel<String> taskListModel = new javax.swing.DefaultListModel<>();
	private final javax.swing.JList<String> taskList = new javax.swing.JList<>(taskListModel);
	private final JScrollPane taskListScroll = new JScrollPane(taskList);
	private final ZoneId scheduleZone = DateTime.calendarInstance().getTimeZone().toZoneId();
	private YearMonth month = YearMonth.now(scheduleZone);

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
		today.addActionListener(event -> setMonth(YearMonth.now(scheduleZone)));
		next.addActionListener(event -> setMonth(month.plusMonths(1)));
		monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 16F));
		navigation.add(previous); navigation.add(today); navigation.add(next); navigation.add(monthLabel);
		add(navigation, BorderLayout.NORTH);
		add(new JScrollPane(canvas), BorderLayout.CENTER);
		taskListScroll.setVisible(false);
		taskListScroll.setPreferredSize(new Dimension(220, 100));
		taskList.getAccessibleContext().setAccessibleName(UsabilityStrings.text("calendar.accessible"));
		add(taskListScroll, BorderLayout.WEST);
		JLabel hint = new JLabel(UsabilityStrings.text("calendar.hint"));
		add(hint, BorderLayout.SOUTH);
		setMonth(month);
		DialogSizing.applyInitialSize(this, new Dimension(900, 600), new Dimension(1100, 760), owner);
		FlatUiSupport.styleDialogRoot(getRootPane());
		FlatUiSupport.styleDialogComponents(getContentPane());
	}

	/** Creates the same calendar UI for embedding in a document view. */
	public static javax.swing.JPanel createEmbeddedPanel(java.awt.Frame owner, Project project) {
		CalendarViewDialogBox dialog = new CalendarViewDialogBox(owner, project);
		javax.swing.JPanel panel = PopupDialogSupport.detachContentPanel(dialog, "Calendar view");
		panel.putClientProperty(DIALOG_PROPERTY, dialog);
		panel.putClientProperty(ViewSettingsState.PROPERTY, dialog.settings);
		panel.putClientProperty(ViewSettingsState.REFRESH_PROPERTY, (Runnable) dialog::settingsChanged);
		return panel;
	}

	/** Client property of an embedded calendar panel that carries its {@link CalendarViewDialogBox}. */
	public static final String DIALOG_PROPERTY = "microproject.calendarViewDialog";

	/** Shows or hides the grid lines between the day cells of the month grid. */
	public void setGridlinesVisible(boolean visible) {
		gridlinesVisible = visible;
		canvas.repaint();
	}

	public boolean isGridlinesVisible() {
		return gridlinesVisible;
	}

	private boolean gridlinesVisible = true;

	/** The view's Format tab choices; call {@link #settingsChanged()} after changing them. */
	public CalendarViewSettings getSettings() {
		return settings;
	}

	/** Applies changed settings (row height, colours, text styles, task list) to the month grid. */
	public void settingsChanged() {
		taskListScroll.setVisible(settings.isTaskListVisible());
		refreshTaskList();
		canvas.updateSize();
		revalidate();
		canvas.repaint();
	}

	/** Week Height Best Fit: the row height that shows the busiest day completely. */
	public void weekHeightBestFit() {
		settings.setRowHeight(canvas.bestFitHeight());
		settingsChanged();
	}

	/** Layout Now: orders the task bars of every day by the current layout rule and keeps that order. */
	public void layoutNow() {
		canvas.layoutNow();
		canvas.repaint();
	}

	/** Height of one week row in pixels. */
	public int getRowHeight() {
		return settings.getRowHeight();
	}

	/** The task names the Task List pane shows (tasks that touch the displayed month). */
	public java.util.List<String> getTaskListEntries() {
		return java.util.Collections.list(taskListModel.elements());
	}

	/** Rebuilds the Task List, e.g. after the privacy mask changes the displayed names. */
	public void refreshTaskList() {
		taskListModel.clear();
		if (!settings.isTaskListVisible()) return;
		for (Task task : canvas.tasksInMonth()) taskListModel.addElement(PrivacyDisplayMode.taskName(task));
	}

	@Override
	public void setLocationRelativeTo(Component reference) {
		super.setLocationRelativeTo(reference);
		WindowBoundsSupport.fitWithinUsableScreen(this);
	}

	private void setMonth(YearMonth value) {
		month = value;
		monthLabel.setText(AlternativeCalendarDisplay.monthLabel(month, Locale.getDefault(Locale.Category.FORMAT)));
		refreshTaskList();
		canvas.repaint();
	}

	/** Refreshes both the standalone and embedded calendar after project changes. */
	public void refreshProjectView() {
		refreshTaskList();
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

	/** Returns the on-canvas link marker bounds for Robot/accessibility verification. */
	public Rectangle getDependencyMarkerBounds(Task task) {
		if (task == null || canvas == null) return null;
		for (Card card : canvas.cards)
			if (card.task() == task && card.linkBounds() != null) return new Rectangle(card.linkBounds());
		return null;
	}

	private final class CalendarCanvas extends JPanel {
		private static final long serialVersionUID = 1L;
		private static final int HEADER = 34;
		private final java.util.Map<Task, Integer> frozenOrder = new java.util.HashMap<>();
		private static final int DAYS_IN_GRID = 42;
		private static final int MAX_VISIBLE_TASKS_PER_DAY = 3;
		private final Project project;
		private final ZoneId zone = scheduleZone;
		private final List<Card> cards = new ArrayList<>(DAYS_IN_GRID * MAX_VISIBLE_TASKS_PER_DAY);
		private Task draggedTask;
		private Point dragPoint;

		CalendarCanvas(Project project) {
			this.project = project;
			setBackground(FlatUiSupport.dataSurfaceBackground());
			updateSize();
			MouseAdapter mouse = new MouseAdapter() {
				@Override public void mousePressed(MouseEvent event) {
					for (Card card : cards) {
						if (card.linkBounds() != null && card.linkBounds().contains(event.getPoint())) {
							showDependencyEditor(card.task());
							return;
						}
						if (card.bounds().contains(event.getPoint())) { draggedTask = card.task(); dragPoint = event.getPoint(); break; }
					}
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

		void updateSize() {
			setPreferredSize(new Dimension(980, HEADER + settings.getRowHeight() * 6));
		}

		private LocalDate gridStart() {
			LocalDate first = month.atDay(1);
			return first.minusDays(first.getDayOfWeek().getValue() - 1L);
		}

		private boolean companionShown() {
			return !AlternativeCalendarDisplay.companionDay(month.atDay(1), Locale.getDefault(Locale.Category.FORMAT)).isEmpty();
		}

		int bestFitHeight() {
			int most = 0;
			LocalDate start = gridStart();
			for (int index = 0; index < DAYS_IN_GRID; index++) most = Math.max(most, tasksOn(start.plusDays(index)).size());
			return CalendarViewSettings.bestFitRowHeight(most, companionShown(), barPitch());
		}

		private int barFontHeight() {
			return getFontMetrics(styled(getFont(), CalendarViewSettings.TextItem.BAR_TEXT)).getHeight();
		}

		private int barPitch() {
			return CalendarViewSettings.barPitch(barFontHeight());
		}

		List<Task> tasksInMonth() {
			java.util.Set<Task> result = new java.util.LinkedHashSet<>();
			LocalDate start = gridStart();
			for (int index = 0; index < DAYS_IN_GRID; index++) result.addAll(tasksOn(start.plusDays(index)));
			return new ArrayList<>(result);
		}

		void layoutNow() {
			frozenOrder.clear();
			List<Task> all = new ArrayList<>();
			for (var iterator = project.getTaskOutlineIterator(); iterator.hasNext();) all.add(iterator.next());
			sortForLayout(all);
			for (int i = 0; i < all.size(); i++) frozenOrder.put(all.get(i), i);
		}

		private void sortForLayout(List<Task> tasks) {
			if (settings.isFitAsManyTasksAsPossible())
				tasks.sort(java.util.Comparator.comparingLong(CalendarViewSettings::slackSortKey));
		}

		private List<Task> orderedTasksOn(LocalDate day) {
			List<Task> result = tasksOn(day);
			if (settings.isAutomaticLayout() || frozenOrder.isEmpty()) sortForLayout(result);
			else result.sort(java.util.Comparator.comparingInt(task -> frozenOrder.getOrDefault(task, Integer.MAX_VALUE)));
			return result;
		}

		private Font styled(Font base, CalendarViewSettings.TextItem item) {
			CalendarViewSettings.TextStyle style = settings.getTextStyle(item);
			return base.deriveFont(style.bold() ? Font.BOLD : Font.PLAIN, style.size() > 0 ? (float) style.size() : base.getSize2D());
		}

		private boolean isLight(int rgb) {
			Color color = new Color(rgb);
			return color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114 > 160;
		}

		@Override protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			try {
				CalendarRenderingSupport.applyQualityHints(g);
				g.setFont(FlatUiSupport.uiFont());
				cards.clear();
				int width = Math.max(1, getWidth() / 7);
				for (int column = 0; column < 7; column++) {
					DayOfWeek day = DayOfWeek.of(column + 1);
					g.setColor(FlatUiSupport.headerForeground());
					g.drawString(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), column * width + 8, 22);
				}
				LocalDate gridStart = gridStart();
				for (int index = 0; index < DAYS_IN_GRID; index++) paintDay(g, gridStart.plusDays(index), index, width);
				if (draggedTask != null && dragPoint != null) {
					Color accent = FlatUiSupport.accentColor();
					g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160));
					g.fillRoundRect(dragPoint.x - 55, dragPoint.y - 10, 110, 20, 8, 8);
					g.setColor(FlatUiSupport.tableSelectionForeground()); g.drawString(PrivacyDisplayMode.taskName(draggedTask), dragPoint.x - 50, dragPoint.y + 5);
				}
			} finally { g.dispose(); }
		}

		private void paintDay(Graphics2D g, LocalDate day, int index, int width) {
			int cellHeight = settings.getRowHeight();
			int column = index % 7, row = index / 7, x = column * width, y = HEADER + row * cellHeight;
			boolean currentMonth = YearMonth.from(day).equals(month);
			g.setColor(currentMonth ? FlatUiSupport.dataSurfaceBackground() : FlatUiSupport.panelBackground()); g.fillRect(x, y, width, cellHeight);
			if (gridlinesVisible) { g.setColor(FlatUiSupport.borderColor()); g.drawRect(x, y, width, cellHeight); }
			if (day.equals(LocalDate.now(zone))) { g.setColor(FlatUiSupport.errorForeground()); g.setStroke(new BasicStroke(2F)); g.drawRect(x + 1, y + 1, width - 2, cellHeight - 2); }
			g.setColor(currentMonth ? FlatUiSupport.tableForeground() : FlatUiSupport.disabledForeground());
			Font dayFont = g.getFont();
			g.setFont(styled(dayFont, CalendarViewSettings.TextItem.DAY_NUMBERS));
			g.drawString(Integer.toString(day.getDayOfMonth()), x + 7, y + 17);
			g.setFont(dayFont);
			String companionDay = AlternativeCalendarDisplay.companionDay(day, Locale.getDefault(Locale.Category.FORMAT));
			int taskCardTop = 23;
			Font barFont = styled(g.getFont(), CalendarViewSettings.TextItem.BAR_TEXT);
			FontMetrics barMetrics = g.getFontMetrics(barFont);
			int barHeight = CalendarViewSettings.barHeight(barMetrics.getHeight());
			int pitch = CalendarViewSettings.barPitch(barMetrics.getHeight());
			int maxVisibleTasks = CalendarViewSettings.visibleTaskCount(cellHeight, false, pitch);
			if (!companionDay.isEmpty()) {
				Font baseFont = g.getFont();
				g.setFont(baseFont.deriveFont(Math.max(9F, baseFont.getSize2D() - 2F)));
				g.setColor(FlatUiSupport.disabledForeground());
				g.drawString(companionDay, x + 7, y + 31);
				g.setFont(baseFont);
				taskCardTop = 38;
				maxVisibleTasks = CalendarViewSettings.visibleTaskCount(cellHeight, true, pitch);
			}
			List<Task> onDay = orderedTasksOn(day);
			int visible = Math.min(maxVisibleTasks, onDay.size());
			for (int i = 0; i < visible; i++) {
				Task task = onDay.get(i); Rectangle bounds = new Rectangle(x + 5, y + taskCardTop + i * pitch, Math.max(20, width - 10), barHeight);
				Integer barRgb = settings.getBarColor(CalendarViewSettings.barType(task));
				g.setColor(barRgb != null ? new Color(barRgb) : task.isInactiveTask() ? FlatUiSupport.disabledForeground() : task.isManuallyScheduled() ? FlatUiSupport.accentColor().darker() : FlatUiSupport.ribbonAccentColor());
				g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 7, 7);
				g.setColor(barRgb != null && isLight(barRgb) ? Color.BLACK : FlatUiSupport.tableSelectionForeground()); String name = PrivacyDisplayMode.taskName(task);
				Font previousFont = g.getFont(); g.setFont(barFont);
				while (name.length() > 2 && g.getFontMetrics().stringWidth(name) > bounds.width - 8) name = name.substring(0, name.length() - 2) + "…";
				g.drawString(name, bounds.x + 4, bounds.y + (bounds.height + barMetrics.getAscent() - barMetrics.getDescent()) / 2); cards.add(new Card(task, bounds));
				g.setFont(previousFont);
				if (hasDependencies(task)) {
					g.setColor(FlatUiSupport.ribbonTabHoverColor());
					g.drawString("↗", bounds.x + bounds.width - 15, bounds.y + (bounds.height + barMetrics.getAscent() - barMetrics.getDescent()) / 2);
					cards.set(cards.size() - 1, new Card(task, bounds,
						new Rectangle(bounds.x + bounds.width - 22, bounds.y, 22, bounds.height)));
				}
			}
			if (onDay.size() > visible) { g.setColor(FlatUiSupport.disabledForeground()); g.drawString("+" + (onDay.size() - visible) + " more", x + 7, y + cellHeight - 9); }
		}

		private List<Task> tasksOn(LocalDate day) {
			List<Task> result = new ArrayList<>();
			for (var iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
				Task task = iterator.next();
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
			int row = Math.max(0, Math.min(5, (point.y - HEADER) / settings.getRowHeight()));
			return gridStart().plusDays(row * 7L + column);
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

		private boolean hasDependencies(Task task) {
			return task instanceof HasDependencies
				&& !DependencyService.getInstance().getIncidentDependencies((HasDependencies) task).isEmpty();
		}

		private void showDependencyEditor(Task task) {
			if (!(task instanceof HasDependencies)) return;
			List<Dependency> dependencies = DependencyService.getInstance()
				.getIncidentDependencies((HasDependencies) task);
			if (dependencies.isEmpty()) return;
			Dependency dependency = dependencies.size() == 1 ? dependencies.get(0)
				: DependencyDialog.chooseDependency(CalendarViewDialogBox.this, dependencies);
			if (dependency == null) return;
			java.awt.Frame owner = getOwner() instanceof java.awt.Frame frame ? frame : null;
			DependencyDialog.doDialog(new DependencyDialog(owner, dependency), dependency);
			project.recalculate();
			repaint();
		}
	}

	private record Card(Task task, Rectangle bounds, Rectangle linkBounds) {
		Card(Task task, Rectangle bounds) { this(task, bounds, null); }
	}
}
