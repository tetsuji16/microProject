/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.render;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import com.microproject.reports.adapter.ReportViewer;

/** Scrollable page preview for report Pageable content. */
public final class ReportPreview extends JPanel {
	private final JPanel pages = new JPanel();
	private final List<PagePanel> pagePanels = new ArrayList<>();
	private float zoom = 1f;

	public ReportPreview(ReportPrintable report) {
		super(new BorderLayout());
		pages.setBackground(new Color(0xE8E8E8));
		pages.setLayout(new javax.swing.BoxLayout(pages, javax.swing.BoxLayout.Y_AXIS));
		add(new JScrollPane(pages), BorderLayout.CENTER);
		setReport(report);
	}
	public void setReport(ReportPrintable report) {
		pages.removeAll(); pagePanels.clear();
		for (int i = 0; i < report.getNumberOfPages(); i++) {
			PagePanel page = new PagePanel(report, i);
			pagePanels.add(page); pages.add(page);
		}
		updateSize();
	}
	public void zoomIn() { setZoomRatio(zoom * 1.1f); }
	public void zoomOut() { setZoomRatio(zoom / 1.1f); }
	public float getZoomRatio() { return zoom; }
	public void setZoomRatio(float ratio) { zoom = ReportViewer.clampZoomRatio(ratio); updateSize(); }
	public boolean canZoomIn() { return zoom < 4f; }
	public boolean canZoomOut() { return zoom > 0.1f; }
	private void updateSize() {
		for (PagePanel page : pagePanels) page.updateSize(zoom);
		pages.revalidate(); pages.repaint();
	}

	private static final class PagePanel extends JPanel {
		private final ReportPrintable report;
		private final int index;
		PagePanel(ReportPrintable report, int index) {
			this.report = report; this.index = index;
			setLayout(new BorderLayout());
			add(new JLabel("Page " + (index + 1), SwingConstants.CENTER), BorderLayout.SOUTH);
			setBackground(Color.WHITE);
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12),
				BorderFactory.createLineBorder(Color.GRAY)));
		}
		void updateSize(float zoom) {
			PageFormat f = report.getPageFormat(index);
			Dimension size = new Dimension((int)(f.getWidth() * zoom), (int)(f.getHeight() * zoom) + 32);
			setPreferredSize(size); setMinimumSize(size); setMaximumSize(size); setSize(size);
		}
		@Override protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			PageFormat f = report.getPageFormat(index);
			Graphics2D g = (Graphics2D) graphics.create();
			try {
				g.translate(12, 12);
				double scale = Math.max(0.1, (getWidth() - 24d) / f.getWidth());
				g.scale(scale, scale);
				report.print(g, f, index);
			} catch (java.awt.print.PrinterException ignored) {
				g.setColor(Color.RED); g.drawString("Unable to render page", 10, 20);
			} finally { g.dispose(); }
		}
	}
}
