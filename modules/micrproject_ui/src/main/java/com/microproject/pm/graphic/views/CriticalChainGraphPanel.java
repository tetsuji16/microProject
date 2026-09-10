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

import javax.swing.JPanel;

import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.task.Project;
import com.microproject.dialog.UsabilityStrings;

/** Lightweight, read-only CCPM network view including resource constraints. */
public final class CriticalChainGraphPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private final Project project;
	private CriticalChainGraphScene scene = CriticalChainGraphScene.from(null, null);

	public CriticalChainGraphPanel(Project project) {
		this.project = project;
		setBackground(Color.WHITE);
		setPreferredSize(scene.preferredSize());
		getAccessibleContext().setAccessibleName(UsabilityStrings.text("ccpm.graphAccessible"));
		setToolTipText(UsabilityStrings.text("ccpm.graphTooltip"));
	}

	public void setAnalysis(CriticalChainService.Analysis analysis) {
		scene = CriticalChainGraphScene.from(project, analysis);
		setPreferredSize(scene.preferredSize());
		revalidate();
		repaint();
	}

	/**
	 * Computes the scrollable canvas required by the current network.  The
	 * graph is embedded in a JScrollPane, so leaving the original fixed size
	 * here silently clips chains with more than three columns or several
	 * feeding buffers.
	 */
	static Dimension preferredSizeFor(CriticalChainService.Analysis analysis) {
		return CriticalChainGraphScene.from(null, analysis).preferredSize();
	}

	@Override protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		if (scene.nodes().isEmpty()) return;
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			for (CriticalChainGraphScene.Edge edge : scene.edges()) drawEdge(g, edge);
			for (CriticalChainGraphScene.Node node : scene.nodes()) drawNode(g, node);
		} finally {
			g.dispose();
		}
	}

	private void drawEdge(Graphics2D g, CriticalChainGraphScene.Edge edge) {
		CriticalChainGraphScene.Node from = scene.node(edge.sourceKey());
		CriticalChainGraphScene.Node to = scene.node(edge.targetKey());
		if (from == null || to == null) return;
		CriticalChainGraphScene.Bounds source = from.bounds();
		CriticalChainGraphScene.Bounds target = to.bounds();
		g.setColor(edge.kind() == CriticalChainGraphScene.EdgeKind.RESOURCE_CONSTRAINT ? new Color(220, 125, 35)
			: edge.kind() == CriticalChainGraphScene.EdgeKind.BUFFER_PROTECTION ? new Color(70, 145, 85) : new Color(55, 105, 180));
		g.setStroke(edge.kind() == CriticalChainGraphScene.EdgeKind.BUFFER_PROTECTION
			? new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[] { 5f, 4f }, 0f) : new BasicStroke(2f));
		int x1 = source.x() + source.width();
		int y1 = source.y() + source.height() / 2;
		int x2 = target.x();
		int y2 = target.y() + target.height() / 2;
		g.draw(new Line2D.Double(x1, y1, x2, y2));
		int tail = x2 >= x1 ? x2 - 8 : x2 + 8;
		g.fillPolygon(new int[] { x2, tail, tail }, new int[] { y2, y2 - 5, y2 + 5 }, 3);
	}

	private static void drawNode(Graphics2D g, CriticalChainGraphScene.Node node) {
		CriticalChainGraphScene.Bounds box = node.bounds();
		boolean buffer = node.kind() != CriticalChainGraphScene.NodeKind.TASK;
		g.setColor(buffer ? new Color(224, 242, 227) : new Color(255, 238, 190));
		g.fillRoundRect(box.x(), box.y(), box.width(), box.height(), 10, 10);
		g.setColor(buffer ? new Color(70, 145, 85) : new Color(170, 100, 20));
		g.drawRoundRect(box.x(), box.y(), box.width(), box.height(), 10, 10);
		g.setColor(Color.DARK_GRAY);
		g.drawString(trim(node.title()), box.x() + 8, box.y() + 19);
		g.drawString(node.detail(), box.x() + 8, box.y() + 36);
	}

	private static String trim(String value) {
		if (value == null) return "";
		return value.length() <= 24 ? value : value.substring(0, 21) + "...";
	}
}
