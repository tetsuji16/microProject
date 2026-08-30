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
import java.awt.geom.Path2D;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.microproject.dialog.UsabilityStrings;
import com.microproject.pm.ccpm.CriticalChainBufferHistory;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.task.Project;

/** Read-only CCPM fever chart relating project progress to buffer consumption. */
public final class CriticalChainBufferChartPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int LEFT = 64;
	private static final int RIGHT = 26;
	private static final int TOP = 30;
	private static final int BOTTOM = 52;
	private static final int MAX_HISTORY = 32;
	private static final double GREEN_INTERCEPT = 10D;
	private static final double GREEN_SLOPE = 0.50D;
	private static final double AMBER_INTERCEPT = 35D;
	private static final double AMBER_SLOPE = 0.55D;
	private static final Color GREEN = new Color(218, 239, 211);
	private static final Color AMBER = new Color(255, 242, 170);
	private static final Color RED = new Color(240, 180, 185);
	enum Zone { GREEN, AMBER, RED }
	private final Project project;
	private final List<CriticalChainBufferHistory.Point> history = new ArrayList<>();
	private CriticalChainService.Analysis analysis;
	private boolean enabled;

	public CriticalChainBufferChartPanel(Project project) {
		this.project = project;
		CriticalChainBufferHistory saved = project == null ? null : project.findTransientDocumentState(CriticalChainBufferHistory.class);
		if (saved != null) history.addAll(saved.points());
		setBackground(Color.WHITE);
		setPreferredSize(new Dimension(620, 420));
		getAccessibleContext().setAccessibleName(UsabilityStrings.text("ccpm.bufferChartAccessible"));
		setToolTipText(UsabilityStrings.text("ccpm.bufferChartTooltip"));
	}

	/** Updates the current point and retains distinct observations made during this dialog session. */
	public void setAnalysis(CriticalChainService.Analysis analysis, boolean enabled) {
		this.analysis = analysis;
		if (!enabled && this.enabled) clearHistory();
		this.enabled = enabled;
		if (enabled && analysis != null) addObservation(pointFor(project, analysis.projectBuffer()));
		repaint();
	}

	@Override protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			if (!enabled || analysis == null) {
				g.setColor(Color.DARK_GRAY);
				g.drawString(UsabilityStrings.text("ccpm.bufferChartEmpty"), LEFT, TOP + 20);
				return;
			}
			int width = Math.max(1, getWidth() - LEFT - RIGHT);
			int height = Math.max(1, getHeight() - TOP - BOTTOM);
			drawZones(g, width, height);
			drawAxes(g, width, height);
			drawHistory(g, width, height);
			drawLegend(g, width);
		} finally {
			g.dispose();
		}
	}

	private void drawZones(Graphics2D g, int width, int height) {
		fillZone(g, GREEN, width, height, GREEN_INTERCEPT, GREEN_SLOPE);
		fillBand(g, AMBER, width, height, GREEN_INTERCEPT, GREEN_SLOPE, AMBER_INTERCEPT, AMBER_SLOPE);
		fillAbove(g, RED, width, height, AMBER_INTERCEPT, AMBER_SLOPE);
	}

	private void fillZone(Graphics2D g, Color color, int width, int height, double intercept, double slope) {
		Path2D path = new Path2D.Double();
		path.moveTo(LEFT, TOP + height);
		path.lineTo(LEFT + width, TOP + height);
		path.lineTo(LEFT + width, yFor(intercept + slope * 100D, height));
		path.lineTo(LEFT, yFor(intercept, height));
		path.closePath();
		g.setColor(color);
		g.fill(path);
	}

	private void fillBand(Graphics2D g, Color color, int width, int height, double lowerIntercept, double lowerSlope,
		double upperIntercept, double upperSlope) {
		Path2D path = new Path2D.Double();
		path.moveTo(LEFT, yFor(lowerIntercept, height));
		path.lineTo(LEFT + width, yFor(lowerIntercept + lowerSlope * 100D, height));
		path.lineTo(LEFT + width, yFor(upperIntercept + upperSlope * 100D, height));
		path.lineTo(LEFT, yFor(upperIntercept, height));
		path.closePath();
		g.setColor(color);
		g.fill(path);
	}

	private void fillAbove(Graphics2D g, Color color, int width, int height, double intercept, double slope) {
		Path2D path = new Path2D.Double();
		path.moveTo(LEFT, TOP);
		path.lineTo(LEFT + width, TOP);
		path.lineTo(LEFT + width, yFor(intercept + slope * 100D, height));
		path.lineTo(LEFT, yFor(intercept, height));
		path.closePath();
		g.setColor(color);
		g.fill(path);
	}

	private void drawAxes(Graphics2D g, int width, int height) {
		g.setColor(new Color(75, 75, 75));
		g.setStroke(new BasicStroke(1f));
		g.drawLine(LEFT, TOP, LEFT, TOP + height);
		g.drawLine(LEFT, TOP + height, LEFT + width, TOP + height);
		for (int value = 0; value <= 100; value += 20) {
			int x = xFor(value, width);
			int y = yFor(value, height);
			g.drawLine(x, TOP + height, x, TOP + height + 4);
			g.drawString(value + "%", x - 10, TOP + height + 20);
			g.drawLine(LEFT - 4, y, LEFT, y);
			g.drawString(value + "%", LEFT - 45, y + 5);
		}
		g.drawString(UsabilityStrings.text("ccpm.bufferChartProgress"), LEFT + width / 2 - 75, TOP + height + 42);
		g.drawString(UsabilityStrings.text("ccpm.bufferChartConsumption"), LEFT, 17);
	}

	private void drawHistory(Graphics2D g, int width, int height) {
		if (history.isEmpty()) return;
		g.setColor(new Color(34, 94, 168));
		g.setStroke(new BasicStroke(2f));
		CriticalChainBufferHistory.Point previous = null;
		for (CriticalChainBufferHistory.Point point : history) {
			if (previous != null) g.drawLine(xFor(previous.progressPercent(), width), yFor(previous.consumptionPercent(), height),
				xFor(point.progressPercent(), width), yFor(point.consumptionPercent(), height));
			previous = point;
		}
		for (CriticalChainBufferHistory.Point point : history) {
			int x = xFor(point.progressPercent(), width);
			int y = yFor(point.consumptionPercent(), height);
			g.setColor(new Color(20, 75, 145));
			g.fillOval(x - 5, y - 5, 10, 10);
			g.setColor(Color.WHITE);
			g.drawOval(x - 3, y - 3, 6, 6);
			g.setColor(Color.DARK_GRAY);
			g.drawString(Math.round(point.progressPercent()) + "% / " + Math.round(point.consumptionPercent()) + "%", x + 8, y - 8);
		}
	}

	private void drawLegend(Graphics2D g, int width) {
		int x = LEFT + width - 196;
		legend(g, GREEN, UsabilityStrings.text("ccpm.bufferChartGreen"), x, TOP + 16);
		legend(g, AMBER, UsabilityStrings.text("ccpm.bufferChartAmber"), x, TOP + 34);
		legend(g, RED, UsabilityStrings.text("ccpm.bufferChartRed"), x, TOP + 52);
	}

	private static void legend(Graphics2D g, Color color, String label, int x, int y) {
		g.setColor(color);
		g.fillRect(x, y - 10, 12, 12);
		g.setColor(Color.DARK_GRAY);
		g.drawRect(x, y - 10, 12, 12);
		g.drawString(label, x + 18, y);
	}

	private static CriticalChainBufferHistory.Point pointFor(Project project, CriticalChainService.Buffer buffer) {
		double progress = project == null ? 0D : clamp(project.getPercentComplete() * 100D);
		return new CriticalChainBufferHistory.Point(Instant.now(), "unknown", "unknown", progress,
				clamp(buffer.consumptionRatio() * 100D), zoneForValues(progress, clamp(buffer.consumptionRatio() * 100D)).name(), "");
	}

	private static Zone zoneForValues(double progress, double consumption) {
		if (consumption <= GREEN_INTERCEPT + GREEN_SLOPE * progress) return Zone.GREEN;
		return consumption <= AMBER_INTERCEPT + AMBER_SLOPE * progress ? Zone.AMBER : Zone.RED;
	}

	static Zone zoneFor(CriticalChainBufferHistory.Point point) {
		double progress = point.progressPercent();
		double consumption = point.consumptionPercent();
		if (consumption <= GREEN_INTERCEPT + GREEN_SLOPE * progress) return Zone.GREEN;
		return consumption <= AMBER_INTERCEPT + AMBER_SLOPE * progress ? Zone.AMBER : Zone.RED;
	}

	private void addObservation(CriticalChainBufferHistory.Point point) {
		if (!history.isEmpty() && history.get(history.size() - 1).equals(point)) return;
		if (history.size() == MAX_HISTORY) history.remove(0);
		history.add(point);
		if (project != null) {
			CriticalChainBufferHistory saved = project.getOrCreateTransientDocumentState(CriticalChainBufferHistory.class, CriticalChainBufferHistory::new);
			saved.add(point);
		}
	}

	private void clearHistory() {
		history.clear();
		if (project != null) project.removeTransientDocumentState(CriticalChainBufferHistory.class);
	}

	private static int xFor(double percent, int width) { return LEFT + (int) Math.round(clamp(percent) * width / 100D); }
	private static int yFor(double percent, int height) { return TOP + height - (int) Math.round(clamp(percent) * height / 100D); }
	private static double clamp(double value) { return Double.isFinite(value) ? Math.max(0D, Math.min(100D, value)) : 0D; }

	static int observationCount(CriticalChainBufferChartPanel panel) { return panel.history.size(); }
	static CriticalChainBufferHistory.Point currentPoint(Project project, CriticalChainService.Buffer buffer) { return pointFor(project, buffer); }
}
