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
import java.awt.FlowLayout;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ToolTipManager;

import com.microproject.options.CalendarOption;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.TeamPlannerService;
import com.microproject.help.HelpUtil;
import com.microproject.util.PopupDialogSupport;
import com.microproject.pm.task.Project;
import com.microproject.util.FlatUiSupport;

/** Resource-row timeline supporting drag rescheduling and reassignment. */
public final class TeamPlannerDialogBox extends JDialog {
	private static final long serialVersionUID = 1L;
	private final Project project;
	private final TeamPlannerCanvas canvas;

	public static TeamPlannerDialogBox getInstance(java.awt.Frame owner, Project project) {
		return new TeamPlannerDialogBox(owner, project);
	}

	private TeamPlannerDialogBox(java.awt.Frame owner, Project project) {
		super(owner, UsabilityStrings.text("team.title"), true);
		HelpUtil.addDocHelp(getRootPane(), "Team_Planner");
		getAccessibleContext().setAccessibleDescription(UsabilityStrings.text("team.hint"));
		PopupDialogSupport.bindEscapeToDispose(this);
		this.project = project;
		this.canvas = new TeamPlannerCanvas(project);
		buildUi();
	}

	private void buildUi() {
		FlatUiSupport.styleDialogRoot(getRootPane());
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		toolbar.add(new JLabel("Zoom:"));
		JSlider zoom = new JSlider(8, 48, canvas.getPixelsPerDay());
		zoom.setPreferredSize(new Dimension(180, 32));
		zoom.addChangeListener(event -> canvas.setPixelsPerDay(zoom.getValue()));
		toolbar.add(zoom);
		JButton today = new JButton("Today");
		JScrollPane scrollPane = new JScrollPane(canvas);
		today.addActionListener(event -> canvas.scrollDateToVisible(System.currentTimeMillis()));
		toolbar.add(today);
		JButton level = new JButton(UsabilityStrings.text("team.resolve"));
		level.addActionListener(event -> ResourceLevelingDialogBox.getInstance(
			(java.awt.Frame) getOwner(), project).setVisible(true));
		toolbar.add(level);
		JButton close = new JButton("Close");
		close.addActionListener(event -> dispose());
		JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		footer.add(new JLabel(UsabilityStrings.text("team.hint") + " "));
		footer.add(close);

		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
		add(footer, BorderLayout.SOUTH);
		setPreferredSize(new Dimension(1120, 680));
		pack();
		setLocationRelativeTo(getOwner());
	}

	static final class TeamPlannerCanvas extends JPanel {
		private static final long serialVersionUID = 1L;
		private static final int HEADER_HEIGHT = 42;
		private static final int ROW_HEIGHT = 54;
		private static final int LABEL_WIDTH = 180;
		private static final int BAR_HEIGHT = 28;
		private static final long DAY = 24L * 60L * 60L * 1000L;
		private final Project project;
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

		TeamPlannerCanvas(Project project) {
			this.project = project;
			setOpaque(true);
			setBackground(Color.WHITE);
			setToolTipText("");
			ToolTipManager.sharedInstance().registerComponent(this);
			MouseAdapter mouse = new MouseAdapter() {
				public void mousePressed(MouseEvent event) {
					dragged = slotAt(event.getPoint());
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
			revalidate();
			repaint();
		}

		void reload() {
			slots = service.slots(project);
			List<Resource> resources = new ArrayList<>();
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
			updatePreferredSize();
			revalidate();
			repaint();
		}

		void scrollDateToVisible(long date) {
			int x = dateToX(date);
			scrollRectToVisible(new Rectangle(Math.max(0, x - 200), 0, 400, HEADER_HEIGHT));
		}

		private void updatePreferredSize() {
			long days = Math.max(30L, (timelineEnd - timelineStart + DAY - 1L) / DAY);
			setPreferredSize(new Dimension(LABEL_WIDTH + (int) Math.min(100_000L, days * pixelsPerDay),
				HEADER_HEIGHT + Math.max(1, rows.size()) * ROW_HEIGHT));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				paintGrid(g2);
				paintSlots(g2);
				paintDragGhost(g2);
			} finally {
				g2.dispose();
			}
		}

		private void paintGrid(Graphics2D g2) {
			g2.setColor(new Color(0xF4F6F8));
			g2.fillRect(0, 0, getWidth(), HEADER_HEIGHT);
			for (int row = 0; row < rows.size(); row++) {
				int y = HEADER_HEIGHT + row * ROW_HEIGHT;
				g2.setColor(row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA));
				g2.fillRect(0, y, getWidth(), ROW_HEIGHT);
				g2.setColor(new Color(0xDDE1E6));
				g2.drawLine(0, y + ROW_HEIGHT - 1, getWidth(), y + ROW_HEIGHT - 1);
				g2.setColor(new Color(0x202124));
				g2.drawString(TeamPlannerService.displayName(rows.get(row)), 10, y + 31);
			}
			for (long date = timelineStart; date <= timelineEnd; date += DAY) {
				int x = dateToX(date);
				g2.setColor(new Color(0xE5E8EB));
				g2.drawLine(x, HEADER_HEIGHT, x, getHeight());
				if (((date - timelineStart) / DAY) % 7L == 0L) {
					g2.setColor(new Color(0x4A4D50));
					g2.drawString(dateFormat.format(Instant.ofEpochMilli(date)), x + 3, 26);
				}
			}
			g2.setColor(new Color(0xC8CDD2));
			g2.drawLine(LABEL_WIDTH, 0, LABEL_WIDTH, getHeight());
		}

		private void paintSlots(Graphics2D g2) {
			hitAreas.clear();
			for (TeamPlannerService.Slot slot : slots) {
				int row = rowOf(slot.resource());
				if (row < 0) {
					continue;
				}
				int x = dateToX(slot.start());
				int width = Math.max(18, dateToX(slot.end()) - x);
				int y = HEADER_HEIGHT + row * ROW_HEIGHT + (ROW_HEIGHT - BAR_HEIGHT) / 2;
				Rectangle rectangle = new Rectangle(x, y, width, BAR_HEIGHT);
				hitAreas.put(rectangle, slot);
				g2.setColor(slot.overallocated() ? new Color(0xD93025) : new Color(0x3B78C8));
				g2.fillRoundRect(x, y, width, BAR_HEIGHT, 8, 8);
				g2.setColor(Color.WHITE);
				FontMetrics metrics = g2.getFontMetrics();
				String text = ellipsize(slot.task().getName(), metrics, width - 10);
				g2.drawString(text, x + 5, y + 19);
			}
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
			int y = targetRow < 0 ? original.y : HEADER_HEIGHT + targetRow * ROW_HEIGHT + (ROW_HEIGHT - BAR_HEIGHT) / 2;
			int x = original.x + dragPoint.x - dragOrigin.x;
			g2.setColor(new Color(0x13, 0x67, 0xD1, 180));
			g2.setStroke(new BasicStroke(2F));
			g2.drawRoundRect(x, y, original.width, original.height, 8, 8);
		}

		private void commitDrag(Point release) {
			int dayDelta = Math.round((release.x - dragOrigin.x) / (float) pixelsPerDay);
			int row = targetRow(release.y);
			try {
				if (row >= 0) {
					Resource target = rows.get(row);
					if (target != dragged.resource()) {
						service.reassign(dragged.assignment(), target, this);
					}
				}
				if (dayDelta != 0) {
					long work = dayDelta * CalendarOption.getInstance().getMillisPerDay();
					long newStart = dragged.task().getEffectiveWorkCalendar().add(dragged.task().getStart(), work, false);
					service.reschedule(dragged.task(), newStart, this);
				}
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
			return "<html><b>" + escape(slot.task().getName()) + "</b><br>"
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
			int row = (y - HEADER_HEIGHT) / ROW_HEIGHT;
			return y < HEADER_HEIGHT || row < 0 || row >= rows.size() ? -1 : row;
		}

		private int dateToX(long date) {
			return LABEL_WIDTH + (int) Math.round((date - timelineStart) / (double) DAY * pixelsPerDay);
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

		private static String escape(String value) {
			return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		}
	}
}
