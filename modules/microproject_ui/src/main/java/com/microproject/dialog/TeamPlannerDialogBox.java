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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ToolTipManager;

import com.microproject.options.CalendarOption;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.TeamPlannerService;
import com.microproject.pm.task.Task;
import com.microproject.help.HelpUtil;
import com.microproject.ui.util.PopupDialogSupport;
import com.microproject.pm.task.Project;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.FlatLafDialog;
import com.microproject.util.MarkupEscape;

/** Resource-row timeline supporting drag rescheduling and reassignment. */
public final class TeamPlannerDialogBox extends FlatLafDialog {
	private static final long serialVersionUID = 1L;
	private final Project project;
	private Runnable selectionListener = () -> { };
	private final TeamPlannerCanvas canvas;
	private final TeamPlannerViewSettings settings = new TeamPlannerViewSettings();
	private final DefaultListModel<String> unassignedModel = new DefaultListModel<>();
	private final DefaultListModel<String> unscheduledModel = new DefaultListModel<>();
	private JPanel unassignedPane;
	private JPanel unscheduledPane;

	/** Client property of an embedded Team Planner panel that carries its {@link TeamPlannerDialogBox}. */
	public static final String DIALOG_PROPERTY = "microproject.teamPlannerDialog";

	public static TeamPlannerDialogBox getInstance(java.awt.Frame owner, Project project) {
		return new TeamPlannerDialogBox(owner, project);
	}

	private TeamPlannerDialogBox(java.awt.Frame owner, Project project) {
		super(owner, UsabilityStrings.text("team.title"), false);
		HelpUtil.addDocHelp(getRootPane(), "Team_Planner");
		getAccessibleContext().setAccessibleDescription(UsabilityStrings.text("team.hint"));
		PopupDialogSupport.bindEscapeToDispose(this);
		this.project = project;
		this.canvas = new TeamPlannerCanvas(project, settings, () -> selectionListener.run());
		buildUi();
	}

	private void buildUi() {
		FlatUiSupport.styleDialogRoot(getRootPane());
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		toolbar.add(new JLabel(UsabilityStrings.text("team.zoom")));
		JSlider zoom = new JSlider(8, 48, canvas.getPixelsPerDay());
		zoom.setPreferredSize(new Dimension(180, 32));
		zoom.addChangeListener(event -> canvas.setPixelsPerDay(zoom.getValue()));
		toolbar.add(zoom);
		JButton today = new JButton(UsabilityStrings.text("team.today"));
		JScrollPane scrollPane = new JScrollPane(canvas);
		TeamPlannerRowHeader rowHeader = new TeamPlannerRowHeader(canvas);
		TeamPlannerDateHeader dateHeader = new TeamPlannerDateHeader(canvas);
		JPanel corner = new JPanel();
		corner.setOpaque(true);
		corner.setBackground(FlatUiSupport.panelBackground());
		corner.setPreferredSize(new Dimension(rowHeader.labelWidth(), TeamPlannerCanvas.HEADER_HEIGHT));
		scrollPane.setRowHeaderView(rowHeader);
		scrollPane.setColumnHeaderView(dateHeader);
		scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, corner);
		// The resource-name column stays on the left while the timeline scrolls and can be widened by dragging its
		// right edge, like the Gantt sheet's name column.
		rowHeader.setMaximumWidthSupplier(() ->
			Math.max(TeamPlannerRowHeader.MIN_WIDTH, scrollPane.getViewport().getWidth() / 2));
		rowHeader.setWidthListener(width -> {
			corner.setPreferredSize(new Dimension(width, TeamPlannerCanvas.HEADER_HEIGHT));
			corner.revalidate();
			scrollPane.revalidate();
			scrollPane.repaint();
		});
		canvas.setGeometryListener(() -> {
			rowHeader.revalidate();
			rowHeader.repaint();
			dateHeader.revalidate();
			dateHeader.repaint();
		});
		today.addActionListener(event -> canvas.scrollDateToVisible(System.currentTimeMillis()));
		toolbar.add(today);
		JButton level = new JButton(UsabilityStrings.text("team.resolve"));
		level.addActionListener(event -> ResourceLevelingDialogBox.getInstance(
			(java.awt.Frame) getOwner(), project).setVisible(true));
		toolbar.add(level);
		JButton close = new JButton(UsabilityStrings.text("common.close"));
		getRootPane().setDefaultButton(close); // Enter presses the primary command (Windows/Office dialog rule)
		close.addActionListener(event -> dispose());
		JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		footer.add(new JLabel(UsabilityStrings.text("team.hint") + " "));
		footer.add(close);

		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
		unassignedPane = taskPane("team.unassigned.title", unassignedModel);
		unscheduledPane = taskPane("team.unscheduled.title", unscheduledModel);
		JPanel side = new JPanel(new java.awt.GridLayout(0, 1, 0, 8));
		side.add(unassignedPane);
		side.add(unscheduledPane);
		side.setPreferredSize(new Dimension(220, 100));
		add(side, BorderLayout.EAST);
		add(footer, BorderLayout.SOUTH);
		settingsChanged();
		FlatUiSupport.styleDialogComponents(getContentPane());
		DialogSizing.applyInitialSize(this, new Dimension(720, 460), new Dimension(1120, 680), getOwner());
	}

	/** Creates the Team Planner UI for embedding in a document view. */
	public static JPanel createEmbeddedPanel(java.awt.Frame owner, Project project) {
		TeamPlannerDialogBox dialog = new TeamPlannerDialogBox(owner, project);
		JPanel panel = PopupDialogSupport.detachContentPanel(dialog, "Team Planner");
		panel.putClientProperty(DIALOG_PROPERTY, dialog);
		panel.putClientProperty(ViewSettingsState.PROPERTY, dialog.settings);
		panel.putClientProperty(ViewSettingsState.REFRESH_PROPERTY, (Runnable) dialog::settingsChanged);
		return panel;
	}

	private JPanel taskPane(String titleKey, DefaultListModel<String> model) {
		JPanel pane = new JPanel(new BorderLayout());
		pane.setBorder(BorderFactory.createTitledBorder(UsabilityStrings.text(titleKey)));
		JList<String> list = new JList<>(model);
		list.getAccessibleContext().setAccessibleName(UsabilityStrings.text(titleKey));
		pane.add(new JScrollPane(list), BorderLayout.CENTER);
		return pane;
	}

	public TeamPlannerViewSettings getSettings() { return settings; }

	/** Tasks whose bars the user has selected on the planner. */
	public List<Task> getSelectedTasks() { return canvas.selectedTasks(); }

	/** Receives selection changes from the embedded planner. */
	public void setSelectionListener(Runnable listener) { selectionListener = listener == null ? () -> { } : listener; }

	/** Selects the bars of the given tasks. */
	public void selectTasks(List<Task> tasks) { canvas.select(tasks); }

	/** Applies the settings after a Format command changed them, and refreshes the side panes. */
	public void settingsChanged() {
		canvas.reload();
		unassignedPane.setVisible(settings.isShowUnassigned());
		unscheduledPane.setVisible(settings.isShowUnscheduled());
		unassignedModel.clear();
		unscheduledModel.clear();
		TeamPlannerService service = new TeamPlannerService();
		for (Task task : service.unassignedTasks(project)) unassignedModel.addElement(task.getName());
		for (Task task : service.unscheduledTasks(project)) unscheduledModel.addElement(task.getName());
		revalidate();
	}

	/** Number of tasks listed in the Unassigned Tasks pane. */
	public int unassignedCount() { return unassignedModel.size(); }

	/** Number of tasks listed in the Unscheduled Tasks pane. */
	public int unscheduledCount() { return unscheduledModel.size(); }

	public boolean isUnassignedPaneVisible() { return unassignedPane.isVisible(); }

	public boolean isUnscheduledPaneVisible() { return unscheduledPane.isVisible(); }

	/** The fill colour a task bar is painted with, honouring the Format tab's styles. */
	public Color fillColorOf(Task task) { return canvas.fillOf(task); }

	static final class TeamPlannerCanvas extends JPanel {
		private static final long serialVersionUID = 1L;
		static final int HEADER_HEIGHT = 42;
		private static final int ROW_HEIGHT = 54;
		private static final int BAR_HEIGHT = 28;
		private static final long DAY = 24L * 60L * 60L * 1000L;
		private final Project project;
		private final TeamPlannerViewSettings settings;
		private final Runnable selectionListener;
		private Runnable geometryListener = () -> { };
		private final Set<Task> selection = Collections.newSetFromMap(new IdentityHashMap<>());
		private int[] rowTops = new int[0];
		private int[] rowHeights = new int[0];
		private final Map<TeamPlannerService.Slot, Integer> laneOf = new IdentityHashMap<>();
		private final TeamPlannerService service = new TeamPlannerService();
		private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM d")
			.withZone(ZoneId.systemDefault());
		private final Map<Rectangle, TeamPlannerService.Slot> hitAreas = new LinkedHashMap<>();
		private List<Resource> rows = List.of();
		private List<TeamPlannerService.Slot> slots = List.of();
		private int pixelsPerDay = 20;
		private long timelineStart;
		private long timelineEnd;
		private TeamPlannerService.Slot dragged;
		private Point dragOrigin;
		private Point dragPoint;

		TeamPlannerCanvas(Project project, TeamPlannerViewSettings settings, Runnable selectionListener) {
			this.project = project;
			this.settings = settings;
			this.selectionListener = selectionListener;
			setOpaque(true);
		setBackground(FlatUiSupport.dataSurfaceBackground());
			setToolTipText("");
			ToolTipManager.sharedInstance().registerComponent(this);
			MouseAdapter mouse = new MouseAdapter() {
				public void mousePressed(MouseEvent event) {
					dragged = slotAt(event.getPoint());
					if (dragged != null) {
						if (!event.isControlDown()) selection.clear();
						if (!selection.add(dragged.task())) selection.remove(dragged.task());
					} else if (!event.isControlDown()) {
						selection.clear();
					}
					selectionListener.run();
					repaint();
					dragOrigin = event.getPoint();
					dragPoint = event.getPoint();
				}

				public void mouseDragged(MouseEvent event) {
					if (dragged != null) {
						dragPoint = event.getPoint();
						repaint();
					}
				}

				public void mouseReleased(MouseEvent event) {
					if (dragged != null) {
						commitDrag(event.getPoint());
					}
					dragged = null;
					dragOrigin = null;
					dragPoint = null;
					reload();
				}
			};
			addMouseListener(mouse);
			addMouseMotionListener(mouse);
			reload();
		}

		int getPixelsPerDay() {
			return pixelsPerDay;
		}

		void setPixelsPerDay(int value) {
			pixelsPerDay = Math.max(8, value);
			updatePreferredSize();
			geometryListener.run();
			revalidate();
			repaint();
		}

		/** Called when the row geometry or the timeline range changed, so the headers can follow. */
		void setGeometryListener(Runnable listener) {
			geometryListener = listener == null ? () -> { } : listener;
		}

		void reload() {
			int level = settings.getOutlineLevel();
			slots = service.slots(project).stream()
				.filter(slot -> level == 0 || slot.task().getOutlineLevel() <= level).toList();
			List<Resource> resources = new ArrayList<>(project.getResourcePool().getResourceList().size() + 1);
			resources.add(ResourceImpl.getUnassignedInstance());
			for (Resource resource : project.getResourcePool().getResourceList()) {
				if (resource.isLabor() && !resources.contains(resource)) {
					resources.add(resource);
				}
			}
			rows = List.copyOf(resources);
			long minimum = slots.stream().mapToLong(TeamPlannerService.Slot::start).min().orElse(project.getStart());
			long maximum = slots.stream().mapToLong(TeamPlannerService.Slot::end).max().orElse(project.getEnd());
			timelineStart = startOfDay(minimum - 7L * DAY);
			timelineEnd = Math.max(timelineStart + 30L * DAY, maximum + 7L * DAY);
			layoutRows();
			updatePreferredSize();
			geometryListener.run();
			revalidate();
			repaint();
		}

		List<Task> selectedTasks() { return new ArrayList<>(selection); }

		void select(List<Task> tasks) {
			selection.clear();
			selection.addAll(tasks);
			selectionListener.run();
			repaint();
		}

		private int barHeight() { return BAR_HEIGHT + (settings.getTextLines() - 1) * 13; }

		/** Assigns slots to lanes (stacked when rows are expanded) and computes the row geometry. */
		private void layoutRows() {
			laneOf.clear();
			rowTops = new int[rows.size()];
			rowHeights = new int[rows.size()];
			int top = 0;
			for (int row = 0; row < rows.size(); row++) {
				List<Long> laneEnds = new ArrayList<>();
				for (TeamPlannerService.Slot slot : slots) {
					if (rowOf(slot.resource()) != row) continue;
					int lane = 0;
					if (settings.isExpandRows()) {
						while (lane < laneEnds.size() && laneEnds.get(lane) > slot.start()) lane++;
					}
					if (lane == laneEnds.size()) laneEnds.add(slot.end()); else laneEnds.set(lane, slot.end());
					laneOf.put(slot, lane);
				}
				int lanes = Math.max(1, laneEnds.size());
				rowTops[row] = top;
				rowHeights[row] = Math.max(ROW_HEIGHT, lanes * (barHeight() + 6) + 20);
				top += rowHeights[row];
			}
		}

		Color fillOf(Task task) {
			Integer override = settings.getTaskFill(task.getUniqueId());
			if (override != null) return new Color(override);
			Integer fill = settings.getFill(categoryOf(task));
			return fill != null ? new Color(fill) : defaultFill(categoryOf(task));
		}

		Color borderOf(Task task) {
			Integer override = settings.getTaskBorder(task.getUniqueId());
			if (override != null) return new Color(override);
			Integer border = settings.getBorder(categoryOf(task));
			return border == null ? null : new Color(border);
		}

		static Color defaultFill(TeamPlannerViewSettings.Category category) {
			return switch (category) {
				case AUTO -> FlatUiSupport.ribbonAccentColor();
				case MANUAL -> FlatUiSupport.accentColor().darker();
				case ACTUAL -> FlatUiSupport.ribbonAccentColor().darker().darker();
				case EXTERNAL -> FlatUiSupport.disabledForeground();
				case LATE -> FlatUiSupport.errorForeground();
			};
		}

		/** The style category of a task bar: external, late, in progress, manual, otherwise automatic. */
		TeamPlannerViewSettings.Category categoryOf(Task task) {
			if (task.isExternal() || task.getOwningProject() != project) return TeamPlannerViewSettings.Category.EXTERNAL;
			long deadline = task.getDeadline();
			if (deadline != 0L && task.getEnd() > deadline) return TeamPlannerViewSettings.Category.LATE;
			if (task.getPercentComplete() > 0D) return TeamPlannerViewSettings.Category.ACTUAL;
			if (task.isManuallyScheduled()) return TeamPlannerViewSettings.Category.MANUAL;
			return TeamPlannerViewSettings.Category.AUTO;
		}

		void scrollDateToVisible(long date) {
			int x = dateToX(date);
			scrollRectToVisible(new Rectangle(Math.max(0, x - 200), 0, 400, HEADER_HEIGHT));
		}

		private void updatePreferredSize() {
			long days = Math.max(30L, (timelineEnd - timelineStart + DAY - 1L) / DAY);
			setPreferredSize(new Dimension((int) Math.min(100_000L, days * pixelsPerDay),
				rows.isEmpty() ? ROW_HEIGHT : rowTops[rows.size() - 1] + rowHeights[rows.size() - 1]));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setFont(FlatUiSupport.uiFont());
				paintGrid(g2);
				paintSlots(g2);
				paintDragGhost(g2);
			} finally {
				g2.dispose();
			}
		}

		private void paintGrid(Graphics2D g2) {
			for (int row = 0; row < rows.size(); row++) {
				int y = rowTops[row];
				g2.setColor(row % 2 == 0 ? FlatUiSupport.dataSurfaceBackground() : FlatUiSupport.panelBackground());
				g2.fillRect(0, y, getWidth(), rowHeights[row]);
				g2.setColor(FlatUiSupport.borderColor());
				g2.drawLine(0, y + rowHeights[row] - 1, getWidth(), y + rowHeights[row] - 1);
			}
			for (long date = timelineStart; date <= timelineEnd; date += DAY) {
				int x = dateToX(date);
				if (settings.isGridlines()) {
					g2.setColor(FlatUiSupport.separatorColor());
					g2.drawLine(x, 0, x, getHeight());
				}
			}
		}

		private void paintSlots(Graphics2D g2) {
			hitAreas.clear();
			Font base = g2.getFont();
			CalendarViewSettings.TextStyle text = settings.getTextStyle();
			Font font = text.size() > 0 ? base.deriveFont((float) text.size()) : base;
			if (text.bold()) font = font.deriveFont(Font.BOLD);
			g2.setFont(font);
			for (TeamPlannerService.Slot slot : slots) {
				int row = rowOf(slot.resource());
				if (row < 0) {
					continue;
				}
				int x = dateToX(slot.start());
				int width = Math.max(18, dateToX(slot.end()) - x);
				int lane = laneOf.getOrDefault(slot, 0);
				int y = rowTops[row] + 10 + lane * (barHeight() + 6);
				Rectangle rectangle = new Rectangle(x, y, width, barHeight());
				hitAreas.put(rectangle, slot);
				g2.setColor(fillOf(slot.task()));
				g2.fillRoundRect(x, y, width, barHeight(), 8, 8);
				Color border = borderOf(slot.task());
				if (border != null) {
					g2.setColor(border);
					g2.setStroke(new BasicStroke(2F));
					g2.drawRoundRect(x, y, width, barHeight(), 8, 8);
				}
				if (slot.overallocated()) {
					g2.setColor(FlatUiSupport.errorForeground());
					g2.setStroke(new BasicStroke(2F));
					g2.drawRoundRect(x - 1, y - 1, width + 2, barHeight() + 2, 8, 8);
				}
				if (selection.contains(slot.task())) {
					g2.setColor(FlatUiSupport.accentColor());
					g2.setStroke(new BasicStroke(3F));
					g2.drawRoundRect(x - 2, y - 2, width + 4, barHeight() + 4, 10, 10);
				}
				g2.setColor(FlatUiSupport.tableSelectionForeground());
				FontMetrics metrics = g2.getFontMetrics();
				List<String> lines = TimelineDialogBox.wrap(slot.task().getName(), metrics, width - 10, settings.getTextLines());
				int lineY = y + metrics.getAscent() + 4;
				for (String line : lines) {
					g2.drawString(line, x + 5, lineY);
					lineY += metrics.getHeight();
				}
			}
			g2.setFont(base);
		}

		private void paintDragGhost(Graphics2D g2) {
			if (dragged == null || dragOrigin == null || dragPoint == null) {
				return;
			}
			Rectangle original = hitAreas.entrySet().stream().filter(value -> value.getValue() == dragged)
				.map(Map.Entry::getKey).findFirst().orElse(null);
			if (original == null) {
				return;
			}
			int targetRow = targetRow(dragPoint.y);
			int y = targetRow < 0 ? original.y : rowTops[targetRow] + 10;
			int x = original.x + dragPoint.x - dragOrigin.x;
			Color accent = FlatUiSupport.accentColor();
			g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 180));
			g2.setStroke(new BasicStroke(2F));
			g2.drawRoundRect(x, y, original.width, original.height, 8, 8);
		}

		private void commitDrag(Point release) {
			int dayDelta = Math.round((release.x - dragOrigin.x) / (float) pixelsPerDay);
			int row = targetRow(release.y);
			Resource sourceResource = dragged.resource();
			Resource target = row >= 0 && rows.get(row) != sourceResource ? rows.get(row) : null;
			Long newStart = null;
			if (dayDelta != 0) {
				long work = dayDelta * CalendarOption.getInstance().getMillisPerDay();
				newStart = dragged.task().getEffectiveWorkCalendar().add(dragged.task().getStart(), work, false);
			}
			if (target == null && newStart == null) return;
			try {
				service.applyDrag(dragged.assignment(), target, newStart, settings.isPreventOverallocation(), this);
			} catch (IllegalArgumentException ex) {
				javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), UsabilityStrings.text("team.title"),
					javax.swing.JOptionPane.WARNING_MESSAGE);
			}
		}

		@Override
		public String getToolTipText(MouseEvent event) {
			TeamPlannerService.Slot slot = slotAt(event.getPoint());
			if (slot == null) {
				return null;
			}
			Project sourceProject = slot.task().getOwningProject();
			String source = sourceProject == null ? null : sourceProject.getName();
			return "<html><b>" + MarkupEscape.escape(slot.task().getName()) + "</b><br>"
				+ (source == null || source.isBlank() ? "" : "Project: " + MarkupEscape.escape(source) + "<br>")
				+ dateFormat.format(Instant.ofEpochMilli(slot.start())) + " – "
				+ dateFormat.format(Instant.ofEpochMilli(slot.end())) + "<br>"
				+ Math.round(slot.units() * 100D) + "% units"
				+ (slot.overallocated() ? "<br><b>Overallocated</b>" : "") + "</html>";
		}

		private TeamPlannerService.Slot slotAt(Point point) {
			return hitAreas.entrySet().stream().filter(value -> value.getKey().contains(point))
				.map(Map.Entry::getValue).findFirst().orElse(null);
		}

		private int rowOf(Resource resource) {
			Resource normalized = resource == null ? ResourceImpl.getUnassignedInstance() : resource;
			return rows.indexOf(normalized);
		}

		private int targetRow(int y) {
			for (int row = 0; row < rows.size(); row++) {
				if (y >= rowTops[row] && y < rowTops[row] + rowHeights[row]) return row;
			}
			return -1;
		}

		private int dateToX(long date) {
			return (int) Math.round((date - timelineStart) / (double) DAY * pixelsPerDay);
		}

		private static long startOfDay(long value) {
			return Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).toLocalDate()
				.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
		}

		private static String ellipsize(String value, FontMetrics metrics, int width) {
			String text = value == null ? "" : value;
			if (metrics.stringWidth(text) <= width) {
				return text;
			}
			while (!text.isEmpty() && metrics.stringWidth(text + "…") > width) {
				text = text.substring(0, text.length() - 1);
			}
			return text + "…";
		}
	}

	/**
	 * The fixed resource-name column of the Team Planner. It shares the canvas row geometry, so a name always lines
	 * up with its bars, and its right edge can be dragged to widen the column. It is the scroll pane's row header,
	 * which keeps it in place while the timeline scrolls sideways.
	 */
	static final class TeamPlannerRowHeader extends JComponent {
		private static final long serialVersionUID = 1L;
		static final int MIN_WIDTH = 80;
		static final int DEFAULT_WIDTH = 180;
		private final TeamPlannerCanvas canvas;
		private java.util.function.IntSupplier maximumWidthSupplier = () -> Integer.MAX_VALUE;
		private java.util.function.IntConsumer widthListener = width -> { };
		private int labelWidth = DEFAULT_WIDTH;
		private boolean resizing;
		private int resizeStartWidth;
		private int resizeStartX;

		TeamPlannerRowHeader(TeamPlannerCanvas canvas) {
			this.canvas = canvas;
			setOpaque(true);
			setBackground(FlatUiSupport.panelBackground());
			setToolTipText("");
			MouseAdapter mouse = new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent event) {
					setCursor(Cursor.getPredefinedCursor(overResizeEdge(event)
						? Cursor.E_RESIZE_CURSOR : Cursor.DEFAULT_CURSOR));
				}

				@Override
				public void mousePressed(MouseEvent event) {
					if (!overResizeEdge(event)) {
						return;
					}
					resizing = true;
					resizeStartWidth = labelWidth;
					resizeStartX = event.getX();
				}

				@Override
				public void mouseDragged(MouseEvent event) {
					if (resizing) {
						setLabelWidth(resizeStartWidth + event.getX() - resizeStartX);
					}
				}

				@Override
				public void mouseReleased(MouseEvent event) {
					resizing = false;
				}

				@Override
				public void mouseExited(MouseEvent event) {
					if (!resizing) {
						setCursor(Cursor.getDefaultCursor());
					}
				}
			};
			addMouseListener(mouse);
			addMouseMotionListener(mouse);
		}

		int labelWidth() {
			return labelWidth;
		}

		/** Widens or narrows the column, clamped so the timeline always keeps at least half of the viewport. */
		void setLabelWidth(int width) {
			int maximum = Math.max(MIN_WIDTH, maximumWidthSupplier.getAsInt());
			int clamped = Math.max(MIN_WIDTH, Math.min(maximum, width));
			if (clamped == labelWidth) {
				return;
			}
			labelWidth = clamped;
			widthListener.accept(clamped);
			revalidate();
			repaint();
		}

		void setMaximumWidthSupplier(java.util.function.IntSupplier supplier) {
			maximumWidthSupplier = supplier == null ? () -> Integer.MAX_VALUE : supplier;
		}

		void setWidthListener(java.util.function.IntConsumer listener) {
			widthListener = listener == null ? width -> { } : listener;
		}

		private boolean overResizeEdge(MouseEvent event) {
			return event.getX() >= getWidth() - 4;
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(labelWidth, canvas.getPreferredSize().height);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setFont(FlatUiSupport.uiFont());
				FontMetrics metrics = g2.getFontMetrics();
				g2.setColor(FlatUiSupport.panelBackground());
				g2.fillRect(0, 0, getWidth(), getHeight());
				for (int row = 0; row < canvas.rowTops.length; row++) {
					int y = canvas.rowTops[row];
					g2.setColor(row % 2 == 0
						? FlatUiSupport.dataSurfaceBackground() : FlatUiSupport.panelBackground());
					g2.fillRect(0, y, getWidth(), canvas.rowHeights[row]);
					g2.setColor(FlatUiSupport.borderColor());
					g2.drawLine(0, y + canvas.rowHeights[row] - 1, getWidth(), y + canvas.rowHeights[row] - 1);
					g2.setColor(FlatUiSupport.tableForeground());
					String name = TeamPlannerService.displayName(canvas.rows.get(row));
					g2.drawString(TeamPlannerCanvas.ellipsize(name, metrics, getWidth() - 20), 10, y + 31);
				}
				g2.setColor(FlatUiSupport.borderColor());
				g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
			} finally {
				g2.dispose();
			}
		}
	}

	/** The date strip above the timeline. It is the scroll pane's column header, so it stays while rows scroll. */
	static final class TeamPlannerDateHeader extends JComponent {
		private static final long serialVersionUID = 1L;
		private final TeamPlannerCanvas canvas;

		TeamPlannerDateHeader(TeamPlannerCanvas canvas) {
			this.canvas = canvas;
			setOpaque(true);
			setBackground(FlatUiSupport.panelBackground());
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(canvas.getPreferredSize().width, TeamPlannerCanvas.HEADER_HEIGHT);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setFont(FlatUiSupport.uiFont());
				g2.setColor(FlatUiSupport.panelBackground());
				g2.fillRect(0, 0, getWidth(), getHeight());
				for (long date = canvas.timelineStart; date <= canvas.timelineEnd; date += TeamPlannerCanvas.DAY) {
					if (((date - canvas.timelineStart) / TeamPlannerCanvas.DAY) % 7L != 0L) {
						continue;
					}
					int x = canvas.dateToX(date);
					g2.setColor(FlatUiSupport.separatorColor());
					g2.drawLine(x, 0, x, getHeight());
					g2.setColor(FlatUiSupport.labelForeground());
					g2.drawString(canvas.dateFormat.format(Instant.ofEpochMilli(date)), x + 3, 26);
				}
				g2.setColor(FlatUiSupport.borderColor());
				g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
			} finally {
				g2.dispose();
			}
		}
	}
}
