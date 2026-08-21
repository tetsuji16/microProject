/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.views;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.dialog.UsabilityStrings;

/** Lightweight, read-only CCPM network view including resource constraints. */
public final class CriticalChainGraphPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int NODE_WIDTH = 170;
	private static final int NODE_HEIGHT = 46;
	private static final int HORIZONTAL_GAP = 70;
	private static final int VERTICAL_GAP = 28;
	private final Project project;
	private CriticalChainService.Analysis analysis;
	private static final DecimalFormat BUFFER_DAYS = new DecimalFormat("0.0");

	public CriticalChainGraphPanel(Project project) {
		this.project = project;
		setBackground(Color.WHITE);
		setPreferredSize(new Dimension(760, 420));
		getAccessibleContext().setAccessibleName(UsabilityStrings.text("ccpm.graphAccessible"));
		setToolTipText(UsabilityStrings.text("ccpm.graphTooltip"));
	}

	public void setAnalysis(CriticalChainService.Analysis analysis) {
		this.analysis = analysis;
		revalidate();
		repaint();
	}

	@Override protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		if (analysis == null || analysis.criticalTaskIds().isEmpty()) return;
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Map<Long, String> names = taskNames();
			Map<Long, Integer> columns = columns(analysis.criticalTaskIds(), analysis.graphEdges());
			Map<Long, Integer> rows = new LinkedHashMap<>(analysis.criticalTaskIds().size());
			Map<Integer, Integer> nextRow = new LinkedHashMap<>(Math.max(4, analysis.criticalTaskIds().size() / 2));
			for (Long id : analysis.criticalTaskIds()) rows.put(id, nextRow.merge(columns.getOrDefault(id, 0), 1, Integer::sum) - 1);
			Map<Long, java.awt.Rectangle> bounds = new LinkedHashMap<>(analysis.criticalTaskIds().size());
			for (Long id : analysis.criticalTaskIds()) {
				int column = columns.getOrDefault(id, 0);
				int row = rows.getOrDefault(id, 0);
				bounds.put(id, new java.awt.Rectangle(20 + column * (NODE_WIDTH + HORIZONTAL_GAP),
					20 + row * (NODE_HEIGHT + VERTICAL_GAP), NODE_WIDTH, NODE_HEIGHT));
			}
			for (CriticalChainService.ChainEdge edge : analysis.graphEdges()) {
				java.awt.Rectangle from = bounds.get(edge.predecessorTaskId());
				java.awt.Rectangle to = bounds.get(edge.successorTaskId());
				if (from == null || to == null) continue;
				g.setColor(edge.kind() == CriticalChainService.ChainEdge.Kind.RESOURCE_CONSTRAINT ? new Color(220, 125, 35) : new Color(55, 105, 180));
				g.setStroke(new BasicStroke(2f));
				int x1 = from.x + from.width;
				int y1 = from.y + from.height / 2;
				int x2 = to.x;
				int y2 = to.y + to.height / 2;
				g.draw(new Line2D.Double(x1, y1, x2, y2));
				g.fillPolygon(new int[] { x2, x2 - 8, x2 - 8 }, new int[] { y2, y2 - 5, y2 + 5 }, 3);
			}
			for (Long id : analysis.criticalTaskIds()) {
				java.awt.Rectangle box = bounds.get(id);
				g.setColor(new Color(255, 238, 190));
				g.fillRoundRect(box.x, box.y, box.width, box.height, 10, 10);
				g.setColor(new Color(170, 100, 20));
				g.drawRoundRect(box.x, box.y, box.width, box.height, 10, 10);
				g.setColor(Color.DARK_GRAY);
				String label = names.getOrDefault(id, UsabilityStrings.text("common.task") + " " + id);
				g.drawString(trim(label), box.x + 8, box.y + 19);
				g.drawString("#" + id, box.x + 8, box.y + 36);
			}
			// Buffer nodes are deliberately rendered outside the task chain so the
			// graph communicates the CCPM control points, rather than only the
			// dependency graph.  They are read-only and therefore safe for the
			// lightweight embedded view used by the leveling dialog.
			int maxColumn = columns.values().stream().mapToInt(Integer::intValue).max().orElse(0);
			int bufferX = 20 + (maxColumn + 1) * (NODE_WIDTH + HORIZONTAL_GAP);
			int bufferY = 20;
			java.awt.Rectangle projectBuffer = new java.awt.Rectangle(bufferX, bufferY, NODE_WIDTH, NODE_HEIGHT);
			drawBuffer(g, projectBuffer, UsabilityStrings.text("ccpm.projectBuffer"), analysis.projectBuffer());
			int feederRow = 1;
			for (Map.Entry<Long, CriticalChainService.Buffer> entry : analysis.feedingBuffers().entrySet()) {
				java.awt.Rectangle target = bounds.get(entry.getKey());
				if (target == null) continue;
				java.awt.Rectangle feeder = new java.awt.Rectangle(bufferX, 20 + feederRow++ * (NODE_HEIGHT + VERTICAL_GAP), NODE_WIDTH, NODE_HEIGHT);
				drawBuffer(g, feeder, UsabilityStrings.text("ccpm.feedingBuffer") + " #" + entry.getKey(), entry.getValue());
				g.setColor(new Color(70, 145, 85));
				g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[] { 5f, 4f }, 0f));
				g.draw(new Line2D.Double(feeder.x, feeder.y + feeder.height / 2.0, target.x + target.width, target.y + target.height / 2.0));
			}
		} finally {
			g.dispose();
		}
	}

	private static void drawBuffer(Graphics2D g, java.awt.Rectangle box, String label, CriticalChainService.Buffer buffer) {
		g.setColor(new Color(224, 242, 227));
		g.fillRoundRect(box.x, box.y, box.width, box.height, 10, 10);
		g.setColor(new Color(70, 145, 85));
		g.drawRoundRect(box.x, box.y, box.width, box.height, 10, 10);
		g.setColor(Color.DARK_GRAY);
		g.drawString(trim(label), box.x + 8, box.y + 18);
		long plannedDays = Math.max(0L, buffer.plannedMillis()) / (24L * 60L * 60L * 1000L);
		g.drawString(MessageFormat.format(UsabilityStrings.text("ccpm.bufferDays"),
			BUFFER_DAYS.format(plannedDays), buffer.status()), box.x + 8, box.y + 36);
	}

	private Map<Long, String> taskNames() {
		Map<Long, String> result = new LinkedHashMap<>(Math.max(4, analysis == null ? 4 : analysis.criticalTaskIds().size() * 4 / 3 + 1));
		if (project == null) return result;
		for (var iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Task task = (Task) iterator.next();
			result.put(Long.valueOf(task.getUniqueId()), task.getName());
		}
		return result;
	}

	private static Map<Long, Integer> columns(List<Long> ids, List<CriticalChainService.ChainEdge> edges) {
		Map<Long, Integer> result = new LinkedHashMap<>(Math.max(4, ids.size() * 4 / 3 + 1));
		for (Long id : ids) result.put(id, 0);
		boolean changed;
		int rounds = 0;
		do {
			changed = false;
			for (CriticalChainService.ChainEdge edge : edges) {
				if (!result.containsKey(edge.predecessorTaskId()) || !result.containsKey(edge.successorTaskId())) continue;
				int candidate = result.get(edge.predecessorTaskId()) + 1;
				if (candidate > result.get(edge.successorTaskId())) { result.put(edge.successorTaskId(), candidate); changed = true; }
			}
		} while (changed && ++rounds <= ids.size());
		return result;
	}

	private static String trim(String value) {
		if (value == null) return "";
		return value.length() <= 24 ? value : value.substring(0, 21) + "...";
	}
}
