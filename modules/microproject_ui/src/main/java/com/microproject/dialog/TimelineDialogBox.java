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
package com.microproject.dialog;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import com.microproject.pm.graphic.frames.ViewPictureClipboard;
import com.microproject.pm.graphic.undo.SwingUndoAdapter;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectHierarchyQueries;
import com.microproject.pm.task.ReversibleModelChange;
import com.microproject.pm.task.Task;
import com.microproject.ui.privacy.PrivacyDisplayMode;
import com.microproject.help.HelpUtil;
import com.microproject.ui.util.PopupDialogSupport;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.FlatLafDialog;

/** Concise, selectable project timeline suitable for status communication. */
public final class TimelineDialogBox extends FlatLafDialog {
	private static final long serialVersionUID = 1L;
	/** Client property of an embedded timeline panel that carries its {@link TimelineDialogBox}. */
	public static final String DIALOG_PROPERTY = "microproject.timelineDialog";
	private static final int LEFT = 80;
	private static final int AXIS_OFFSET = 56;
	private static final int BAR_TAIL = 34;
	private static final long MAX_PICTURE_PIXELS = 64L * 1024L * 1024L;
	private final Project project;
	private final List<Task> tasks = new ArrayList<>();
	private final TimelineViewSettings settings = new TimelineViewSettings();
	private final TimelineCanvas canvas = new TimelineCanvas();
	private final JTable chooser;
	private final JSlider zoomSlider = new JSlider(100, 400, 100);
	private Runnable selectionListener = () -> { };

	/** How large a copied timeline picture is. */
	public enum PictureSize { EMAIL, PRESENTATION, FULL }

	public TimelineDialogBox(Frame owner, Project project) {
		super(owner, UsabilityStrings.text("timeline.title"), false);
		HelpUtil.addDocHelp(getRootPane(), "Timeline");
		getAccessibleContext().setAccessibleDescription(UsabilityStrings.text("timeline.hint"));
		PopupDialogSupport.bindEscapeToDispose(this);
		this.project = project;
		loadTasks();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(8, 8));
		add(new JLabel(UsabilityStrings.text("timeline.hint")), BorderLayout.NORTH);
		chooser = new JTable(new TaskChoiceModel());
		chooser.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		chooser.setAutoCreateRowSorter(true);
		chooser.getColumnModel().getColumn(0).setMaxWidth(52);
		chooser.getSelectionModel().addListSelectionListener(event -> {
			if (event.getValueIsAdjusting()) return;
			canvas.repaint();
			selectionListener.run();
		});
		zoomSlider.setVisible(false);
		zoomSlider.getAccessibleContext().setAccessibleName(UsabilityStrings.text("timeline.panZoom"));
		zoomSlider.addChangeListener(event -> canvas.refresh());
		JPanel timelinePane = new JPanel(new BorderLayout());
		timelinePane.add(zoomSlider, BorderLayout.NORTH);
		timelinePane.add(new JScrollPane(canvas), BorderLayout.CENTER);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(chooser), timelinePane);
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
		getRootPane().setDefaultButton(close); // Enter presses the primary command (Windows/Office dialog rule)
		close.addActionListener(event -> dispose());
		buttons.add(selectMilestones); buttons.add(clear); buttons.add(close);
		add(buttons, BorderLayout.SOUTH);
		DialogSizing.applyInitialSize(this, new Dimension(900, 480), new Dimension(1100, 620), owner);
		FlatUiSupport.styleDialogRoot(getRootPane());
		FlatUiSupport.styleDialogComponents(getContentPane());
	}

	/** Creates the same timeline UI for embedding in a document view. */
	public static JPanel createEmbeddedPanel(Frame owner, Project project) {
		TimelineDialogBox dialog = new TimelineDialogBox(owner, project);
		JPanel panel = PopupDialogSupport.detachContentPanel(dialog, "Timeline");
		panel.putClientProperty(DIALOG_PROPERTY, dialog);
		panel.putClientProperty(ViewSettingsState.PROPERTY, dialog.settings);
		panel.putClientProperty(ViewSettingsState.REFRESH_PROPERTY, (Runnable) dialog::settingsChanged);
		return panel;
	}

	public TimelineViewSettings getSettings() { return settings; }

	/** Called when the user changes the selection of the task chooser. */
	public void setSelectionListener(Runnable listener) { selectionListener = listener == null ? () -> { } : listener; }

	private void loadTasks() {
		tasks.clear();
		for (Task task : ProjectHierarchyQueries.outline(project)) {
			if (!task.isSummary()) tasks.add(task);
		}
	}

	/** Re-reads the project's tasks after a change that adds or removes tasks. */
	public void reloadTasks() {
		loadTasks();
		((AbstractTableModel) chooser.getModel()).fireTableDataChanged();
		canvas.refresh();
	}

	/** Applies the settings after a Format command changed them. */
	public void settingsChanged() {
		zoomSlider.setVisible(settings.isShowPanZoom());
		if (!settings.isShowPanZoom()) zoomSlider.setValue(100);
		canvas.refresh();
		chooser.repaint();
	}

	/** The chooser rows the user has selected, as tasks. */
	public List<Task> getSelectedTasks() {
		List<Task> selected = new ArrayList<>();
		for (int viewRow : chooser.getSelectedRows()) {
			int row = chooser.convertRowIndexToModel(viewRow);
			if (row >= 0 && row < tasks.size()) selected.add(tasks.get(row));
		}
		return selected;
	}

	/** Selects the given tasks in the chooser. */
	public void selectTasks(List<Task> toSelect) {
		chooser.clearSelection();
		for (Task task : toSelect) {
			int row = tasks.indexOf(task);
			if (row < 0) continue;
			int viewRow = chooser.convertRowIndexToView(row);
			if (viewRow >= 0) chooser.addRowSelectionInterval(viewRow, viewRow);
		}
	}

	/** Shows or hides the given tasks on the timeline as one undoable edit. */
	public void displayTasks(List<Task> targets, boolean display) {
		List<Task> changed = targets.stream().filter(task -> !task.isReadOnly() && task.isDisplayOnTimeline() != display).toList();
		if (changed.isEmpty()) return;
		ReversibleModelChange change = ReversibleModelChange.changed(
			() -> changed.forEach(task -> task.setDisplayOnTimeline(!display)),
			() -> changed.forEach(task -> task.setDisplayOnTimeline(display)));
		changed.forEach(task -> task.setDisplayOnTimeline(display));
		if (project.getUndoController() != null) SwingUndoAdapter.post(project.getUndoController().getEditSupport(), change);
		chooser.repaint();
		canvas.refresh();
	}

	/** Shows the given tasks as callouts (or as plain bars) on the timeline. */
	public void setCallouts(List<Task> targets, boolean callout) {
		for (Task task : targets) settings.setCallout(task.getUniqueId(), callout);
		if (callout) displayTasks(targets, true);
		canvas.refresh();
	}

	/** Adds an empty timeline bar; the selected tasks move onto it. Returns the new bar's index. */
	public int insertBar() {
		int bar = settings.addBar();
		List<Task> selected = getSelectedTasks();
		for (Task task : selected) settings.setBarOf(task.getUniqueId(), bar);
		displayTasks(selected, true);
		chooser.repaint();
		canvas.refresh();
		return bar;
	}

	/** Creates a task or milestone, shows it on the timeline (as a callout when asked) and selects it. */
	public Task insertNewTask(boolean milestone, boolean callout) {
		if (project.isReadOnly()) return null;
		var editSupport = project.getUndoController() == null ? null : project.getUndoController().getEditSupport();
		if (editSupport != null) editSupport.beginUpdate();
		Task created;
		try {
			created = (Task) project.createLocalTaskNode(null).getImpl();
			created.setName(UsabilityStrings.text(milestone ? "insert.milestone.name"
				: callout ? "timeline.newCallout" : "timeline.newTask"));
			if (milestone) created.setDuration(0L);
			created.setDisplayOnTimeline(true);
		} finally {
			if (editSupport != null) editSupport.endUpdate();
		}
		settings.setCallout(created.getUniqueId(), callout);
		if (settings.getBarCount() > 1) settings.setBarOf(created.getUniqueId(), settings.getBarCount() - 1);
		reloadTasks();
		selectTasks(List.of(created));
		return created;
	}

	/** Every non-summary task of the project, in outline order, whether or not it is shown on the timeline. */
	public List<Task> allTasks() {
		return List.copyOf(tasks);
	}

	/** The tasks shown on the timeline, in outline order. */
	public List<Task> displayedTasks() {
		return tasks.stream().filter(Task::isDisplayOnTimeline).toList();
	}

	/** The start and finish the timeline axis spans, honouring an explicit date range. */
	public long[] axisRange() {
		if (settings.hasDateRange()) return new long[] { settings.getRangeStart(), settings.getRangeEnd() };
		List<Task> shown = displayedTasks();
		long min = shown.stream().mapToLong(Task::getStart).min().orElse(project.getStart());
		long max = shown.stream().mapToLong(Task::getEnd).max().orElse(project.getEnd());
		if (max <= min) max = min + 1L;
		return new long[] { min, max };
	}

	/** Renders the timeline as a picture; {@code null} when it would be too large. */
	public BufferedImage renderPicture(PictureSize size) {
		canvas.updatePreferredSize();
		Dimension preferred = canvas.getPreferredSize();
		int width = Math.max(1, preferred.width);
		int height = Math.max(1, preferred.height);
		double scale = switch (size) {
			case EMAIL -> Math.min(1D, 640D / width);
			case PRESENTATION -> Math.max(1D, 1920D / width);
			case FULL -> 1D;
		};
		int imageWidth = (int) Math.max(1, Math.round(width * scale));
		int imageHeight = (int) Math.max(1, Math.round(height * scale));
		if ((long) imageWidth * imageHeight > MAX_PICTURE_PIXELS) return null;
		BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		try {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, imageWidth, imageHeight);
			g.scale(scale, scale);
			canvas.paintForPicture(g, width, height);
		} finally {
			g.dispose();
		}
		return image;
	}

	/** Copies the timeline picture to the clipboard; false when nothing was copied. */
	public boolean copyPicture(PictureSize size) {
		if (GraphicsEnvironment.isHeadless()) return false;
		BufferedImage image = renderPicture(size);
		if (image == null) return false;
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ViewPictureClipboard.ImageTransferable(image), null);
		return true;
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
				case 1 -> PrivacyDisplayMode.taskName(task);
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
		void refresh() { updatePreferredSize(); revalidate(); repaint(); }

		private int contentWidth() { return Math.max(300, (int) Math.round(900 * zoomSlider.getValue() / 100D)); }

		/** Tasks of one timeline bar packed into lanes; a single lane when overlapped tasks are hidden. */
		private List<List<Task>> lanesOf(List<Task> barTasks) {
			List<List<Task>> lanes = new ArrayList<>();
			for (Task task : barTasks) {
				if (!settings.isShowOverlapped()) {
					if (lanes.isEmpty()) lanes.add(new ArrayList<>());
					lanes.get(0).add(task);
					continue;
				}
				List<Task> target = null;
				for (List<Task> lane : lanes) {
					if (lane.get(lane.size() - 1).getEnd() <= task.getStart()) { target = lane; break; }
				}
				if (target == null) { target = new ArrayList<>(); lanes.add(target); }
				target.add(task);
			}
			return lanes;
		}

		private int lanePitch() { return settings.laneHeight() + 22 + (settings.isShowTaskDates() ? 12 : 0) + calloutExtra(); }

		/** Height the callout text boxes hang below a bar, so a callout never overlaps the next lane. */
		private int calloutExtra() {
			int extra = 0;
			Font labelFont = styled(TimelineViewSettings.TextItem.LABEL);
			for (Task task : displayedTasks()) {
				if (!settings.isCallout(task.getUniqueId())) continue;
				int lineHeight = getFontMetrics(taskFont(labelFont, task)).getHeight();
				extra = Math.max(extra, 10 + settings.getTextLines() * lineHeight + 6);
			}
			return extra;
		}

		private int barSectionHeight(List<Task> barTasks) {
			return AXIS_OFFSET + Math.max(1, lanesOf(barTasks).size()) * lanePitch() + BAR_TAIL;
		}

		private List<Task> tasksOfBar(int bar) {
			return displayedTasks().stream().filter(task -> settings.barOf(task.getUniqueId()) == bar).toList();
		}

		void updatePreferredSize() {
			int height = 20;
			for (int bar = 0; bar < settings.getBarCount(); bar++) height += barSectionHeight(tasksOfBar(bar));
			setPreferredSize(new Dimension(contentWidth(), Math.max(220, height)));
		}

		@Override protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			paintTimeline((Graphics2D) graphics.create(), getWidth());
		}

		void paintForPicture(Graphics2D g, int width, int height) {
			g.setColor(getBackground());
			g.fillRect(0, 0, width, height);
			paintTimeline((Graphics2D) g.create(), width);
		}

		private Font styled(TimelineViewSettings.TextItem item) {
			CalendarViewSettings.TextStyle style = settings.getTextStyle(item);
			Font font = FlatUiSupport.uiFont();
			if (style.size() > 0) font = font.deriveFont((float) style.size());
			return style.bold() ? font.deriveFont(Font.BOLD) : font;
		}

		private void paintTimeline(Graphics2D g, int width) {
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Font labelFont = styled(TimelineViewSettings.TextItem.LABEL);
				Font dateFont = styled(TimelineViewSettings.TextItem.DATES);
				g.setFont(labelFont);
				if (displayedTasks().isEmpty()) { g.setColor(FlatUiSupport.disabledForeground()); g.drawString(UsabilityStrings.text("timeline.empty"), 24, 40); return; }
				long[] range = axisRange();
				long min = range[0], max = range[1];
				int right = Math.max(LEFT + 1, width - 40);
				List<Task> selected = getSelectedTasks();
				int top = 10;
				DateFormat medium = DateFormat.getDateInstance(DateFormat.MEDIUM);
				for (int bar = 0; bar < settings.getBarCount(); bar++) {
					List<Task> barTasks = tasksOfBar(bar);
					int axisY = top + AXIS_OFFSET - 10;
					g.setColor(FlatUiSupport.headerForeground()); g.setStroke(new BasicStroke(2F)); g.drawLine(LEFT, axisY, right, axisY);
					if (settings.isShowDates()) {
						g.setFont(dateFont); g.setColor(FlatUiSupport.headerForeground());
						g.drawString(medium.format(new Date(min)), LEFT, axisY - 24);
						String finish = medium.format(new Date(max));
						g.drawString(finish, right - g.getFontMetrics().stringWidth(finish), axisY - 24);
					}
					if (settings.isShowTimescale()) paintTimescale(g, dateFont, min, max, right, axisY);
					g.setFont(labelFont);
					if (barTasks.isEmpty()) {
						g.setColor(FlatUiSupport.disabledForeground());
						g.drawString(UsabilityStrings.text("timeline.emptyBar"), LEFT, axisY + 28);
					}
					int laneIndex = 0;
					for (List<Task> lane : lanesOf(barTasks)) {
						int y = axisY + 26 + laneIndex++ * lanePitch();
						for (Task task : lane) paintTask(g, task, y, min, max, right, selected.contains(task), labelFont, dateFont);
					}
					if (settings.isShowToday()) {
						long now = System.currentTimeMillis();
						if (now >= min && now <= max) {
							int today = position(now, min, max, right);
							g.setFont(dateFont);
							g.setColor(FlatUiSupport.errorForeground()); g.setStroke(new BasicStroke(1.5F));
							g.drawLine(today, axisY - 15, today, top + barSectionHeight(barTasks) - BAR_TAIL / 2);
							g.drawString(UsabilityStrings.text("common.today"), today + 4, axisY - 8);
						}
					}
					top += barSectionHeight(barTasks);
				}
			} finally { g.dispose(); }
		}

		private void paintTimescale(Graphics2D g, Font font, long min, long max, int right, int axisY) {
			ZoneId zone = ZoneId.systemDefault();
			LocalDate day = Instant.ofEpochMilli(min).atZone(zone).toLocalDate().withDayOfMonth(1);
			DateTimeFormatter label = DateTimeFormatter.ofPattern("MMM");
			g.setFont(font);
			g.setStroke(new BasicStroke(1F));
			int lastLabelEnd = -1;
			for (int guard = 0; guard < 600; guard++) {
				long at = day.atStartOfDay(zone).toInstant().toEpochMilli();
				if (at > max) break;
				if (at >= min) {
					int x = position(at, min, max, right);
					g.setColor(FlatUiSupport.headerForeground());
					g.drawLine(x, axisY - 4, x, axisY + 4);
					String text = day.format(label);
					if (x > lastLabelEnd) {
						g.setColor(FlatUiSupport.tableForeground());
						g.drawString(text, x + 2, axisY + 14);
						lastLabelEnd = x + g.getFontMetrics().stringWidth(text) + 6;
					}
				}
				day = day.plusMonths(1);
			}
		}

		private void paintTask(Graphics2D g, Task task, int y, long min, long max, int right, boolean selected, Font labelFont, Font dateFont) {
			int barHeight = settings.laneHeight();
			int x1 = position(task.getStart(), min, max, right);
			int x2 = Math.max(x1 + 8, position(task.getEnd(), min, max, right));
			Color color = task.isInactiveTask() ? FlatUiSupport.disabledForeground()
				: task.isManuallyScheduled() ? FlatUiSupport.accentColor().darker() : FlatUiSupport.ribbonAccentColor();
			g.setColor(color);
			int top = y - barHeight / 2;
			if (task.isMilestone()) {
				int half = barHeight / 2;
				g.fillPolygon(new int[] { x1, x1 + half, x1, x1 - half }, new int[] { y - half, y, y + half, y }, 4);
			} else {
				g.fillRoundRect(x1, top, x2 - x1, barHeight, 8, 8);
				if (settings.isShowProgress() && task.getPercentComplete() > 0D) {
					int done = (int) Math.round((x2 - x1) * Math.min(1D, task.getPercentComplete()));
					g.setColor(color.darker().darker());
					g.fillRoundRect(x1, top, Math.max(2, done), barHeight, 8, 8);
				}
			}
			if (selected) {
				g.setColor(FlatUiSupport.accentColor()); g.setStroke(new BasicStroke(2F));
				g.drawRoundRect(x1 - 2, top - 2, Math.max(4, x2 - x1) + 4, barHeight + 4, 10, 10);
			}
			boolean callout = settings.isCallout(task.getUniqueId());
			int below = y + barHeight / 2;
			if (callout || settings.isBarLabels()) {
				g.setFont(taskFont(labelFont, task));
				FontMetrics fm = g.getFontMetrics();
				List<String> lines = wrap(PrivacyDisplayMode.taskName(task), fm, 150, settings.getTextLines());
				int textWidth = 0;
				for (String line : lines) textWidth = Math.max(textWidth, fm.stringWidth(line));
				int textHeight = lines.size() * fm.getHeight();
				int textX = callout ? Math.max(LEFT, Math.min(x1, right - textWidth - 8)) : Math.min(x2 + 8, Math.max(LEFT, right - 160));
				int textY = callout ? below + 10 + fm.getAscent() : y - textHeight / 2 + fm.getAscent();
				if (callout) {
					g.setColor(FlatUiSupport.headerForeground()); g.setStroke(new BasicStroke(1F));
					g.drawLine(x1, below, x1, below + 10);
					g.setColor(task.getFontBackground() == null ? FlatUiSupport.dataSurfaceBackground() : new Color(task.getFontBackground()));
					g.fillRoundRect(textX - 4, textY - fm.getAscent() - 2, textWidth + 8, textHeight + 4, 6, 6);
					g.setColor(FlatUiSupport.headerForeground());
					g.drawRoundRect(textX - 4, textY - fm.getAscent() - 2, textWidth + 8, textHeight + 4, 6, 6);
					below = textY - fm.getAscent() + textHeight + 4;
				} else if (task.getFontBackground() != null) {
					g.setColor(new Color(task.getFontBackground()));
					g.fillRect(textX - 2, textY - fm.getAscent(), textWidth + 4, textHeight);
				}
				g.setColor(task.getFontColor() == null ? FlatUiSupport.tableForeground() : new Color(task.getFontColor()));
				int lineY = textY;
				for (String line : lines) {
					g.drawString(line, textX, lineY);
					if (task.isFontUnderline()) g.drawLine(textX, lineY + 2, textX + fm.stringWidth(line), lineY + 2);
					lineY += fm.getHeight();
				}
			}
			if (settings.isShowTaskDates()) {
				DateFormat shortDate = DateFormat.getDateInstance(DateFormat.SHORT);
				g.setFont(dateFont);
				g.setColor(FlatUiSupport.tableForeground());
				String dates = shortDate.format(new Date(task.getStart())) + (task.isMilestone() ? "" : " - " + shortDate.format(new Date(task.getEnd())));
				g.drawString(dates, x1, below + 14);
			}
			g.setFont(labelFont);
		}

		private Font taskFont(Font base, Task task) {
			// Keep the bold/italic of the Text Styles base font when the task has no flags of its own.
			int style = base.getStyle() | (task.isFontBold() ? Font.BOLD : Font.PLAIN) | (task.isFontItalic() ? Font.ITALIC : Font.PLAIN);
			String family = task.getFontFamily();
			float size = task.getFontSize() > 0 ? task.getFontSize() : base.getSize2D();
			Font font = family == null || family.isBlank() ? base : new Font(family, style, Math.round(size));
			return font.deriveFont(style, size);
		}

		private int position(long value, long min, long max, int right) {
			double fraction = (value - min) * (double) (right - LEFT) / (max - min);
			return LEFT + (int) Math.round(Math.max(-1000D, Math.min(fraction, (right - LEFT) + 1000D)));
		}
	}

	/** Splits a name over at most {@code maxLines} lines no wider than {@code width}; the last line is cut with an ellipsis. */
	static List<String> wrap(String text, FontMetrics metrics, int width, int maxLines) {
		List<String> lines = new ArrayList<>();
		String rest = text == null ? "" : text.trim();
		while (!rest.isEmpty() && lines.size() < maxLines) {
			if (metrics.stringWidth(rest) <= width) { lines.add(rest); rest = ""; break; }
			int cut = rest.length();
			while (cut > 1 && metrics.stringWidth(rest.substring(0, cut)) > width) cut--;
			int space = rest.lastIndexOf(' ', cut);
			if (space > 0 && lines.size() < maxLines - 1) cut = space;
			lines.add(rest.substring(0, cut).trim());
			rest = rest.substring(cut).trim();
		}
		if (!rest.isEmpty() && !lines.isEmpty()) {
			String last = lines.remove(lines.size() - 1);
			lines.add(last.length() > 1 ? last.substring(0, last.length() - 1) + "…" : last + "…");
		}
		if (lines.isEmpty()) lines.add("");
		return lines;
	}
}
