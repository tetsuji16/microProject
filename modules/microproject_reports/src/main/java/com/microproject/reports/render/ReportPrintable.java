/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.List;
import java.util.Locale;

import com.microproject.field.Field;
import com.microproject.reports.model.ReportLayout;
import com.microproject.reports.model.ReportModel;
import com.microproject.reports.model.ReportTextLayout;

/** Pageable Java2D renderer with composite-font Japanese glyph support. */
public final class ReportPrintable implements Printable, Pageable {
	private static final Font BODY = ReportFontProvider.regular(10);
	private static final Font HEADER = ReportFontProvider.bold(10);
	private static final Font TITLE = ReportFontProvider.bold(22);
	private final ReportModel model;
	private final ReportLayout layout;
	private final PageFormat pageFormat;

	public ReportPrintable(ReportModel model) {
		this.model = model;
		this.layout = new ReportLayout(model);
		PageFormat format = new PageFormat();
		format.setOrientation(model.portrait() ? PageFormat.PORTRAIT : PageFormat.LANDSCAPE);
		java.awt.print.Paper paper = new java.awt.print.Paper();
		paper.setSize(layout.pageWidth(model), layout.pageHeight(model));
		paper.setImageableArea(20, 20, layout.pageWidth(model) - 40, layout.pageHeight(model) - 40);
		format.setPaper(paper);
		pageFormat = format;
	}

	@Override public int getNumberOfPages() { return layout.pageCount(); }
	@Override public PageFormat getPageFormat(int pageIndex) {
		if (pageIndex < 0 || pageIndex >= getNumberOfPages()) throw new IndexOutOfBoundsException(pageIndex);
		return (PageFormat) pageFormat.clone();
	}
	@Override public Printable getPrintable(int pageIndex) {
		getPageFormat(pageIndex);
		return this;
	}

	@Override public int print(Graphics graphics, PageFormat format, int pageIndex) throws PrinterException {
			if (pageIndex < 0 || pageIndex >= getNumberOfPages()) return NO_SUCH_PAGE;
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.translate(format.getImageableX(), format.getImageableY());
			g.scale(format.getImageableWidth() / layout.pageWidth(model), format.getImageableHeight() / layout.pageHeight(model));
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setColor(Color.BLACK);
			ReportLayout.Page page = layout.page(pageIndex);
			ReportLayout.Segment segment = layout.segment(page.segmentIndex());
			drawTitle(g);
			if (!model.labelValueSheet()) drawHeader(g, segment);
			drawRows(g, page, segment);
		if (!model.labelValueSheet()) drawFooter(g, pageIndex, page.segmentIndex());
			return PAGE_EXISTS;
		} finally {
			g.dispose();
		}
	}

	private void drawTitle(Graphics2D g) {
		g.setFont(TITLE);
		FontMetrics fm = g.getFontMetrics();
		drawClipped(g, model.labelValueSheet() ? model.projectName() : model.title(), 30, 65,
			(int) (layout.pageWidth(model) - 60), fm);
	}

	private void drawHeader(Graphics2D g, ReportLayout.Segment segment) {
		g.setFont(HEADER);
		double[] widths = segment.widths();
		double x = 20;
		for (int i = segment.startColumn(); i < segment.endColumn(); i++) {
			int local = i - segment.startColumn();
			int width = (int) Math.round(widths[local]);
			g.setColor(new Color(0x33, 0x33, 0x33));
			g.fillRect((int)x, 82, width, 20);
			g.setColor(Color.WHITE);
			String title = model.columns().get(i).getName();
			drawClipped(g, title, (int)x + 3, 97, width - 6, g.getFontMetrics());
			x += widths[local];
		}
	}

	private void drawRows(Graphics2D g, ReportLayout.Page page, ReportLayout.Segment segment) {
		g.setFont(BODY);
		FontMetrics fm = g.getFontMetrics();
		double[] widths = segment.widths();
		int y = 117;
		if (model.labelValueSheet()) {
			for (int i = page.firstRow(); i < Math.min(model.columns().size(), page.endRow()); i++) {
				ReportModel.Section section = model.sectionStartingAt(i);
				boolean drawSection = section != null && (!page.hasRowFragment() || page.firstRowLine() == 0);
				if (drawSection) {
					g.setFont(HEADER);
					g.setColor(Color.BLACK);
					g.drawString(section.title(), 28, y + 14);
					y += 20;
				}
				Field field = model.columns().get(i);
				Object value = model.rows().isEmpty() ? null : model.rows().get(0).get(i);
				List<String> lines = ReportTextLayout.wrap(format(value, field), 350);
				int from = page.hasRowFragment() ? page.firstRowLine() : 0;
				int to = page.hasRowFragment() ? page.endRowLine() : lines.size();
				int height = page.hasRowFragment() ? Math.max(29, (to - from) * 12 + 8)
					: ReportLayout.labelHeight(model, i) - 3;
				g.setColor(new Color(0xF0, 0xF0, 0xF0));
				g.fillRect(28, y, 556, height);
				g.setColor(Color.BLACK);
				g.setFont(HEADER);
				drawClipped(g, model.displayLabel(i), 36, y + 16, 175, g.getFontMetrics());
				g.setFont(BODY);
				drawWrapped(g, lines, from, to, 220, y + 16, 350);
				y += height + 3;
			}
			return;
		}
		for (int rowIndex = page.firstRow(); rowIndex < page.endRow(); rowIndex++) {
			List<Object> row = model.rows().get(rowIndex);
			Object groupKey = model.groupValue(rowIndex);
			boolean summary = model.summaryRow(rowIndex);
			if (!summary && model.groupColumn() >= 0
					&& (rowIndex == page.firstRow() || !java.util.Objects.equals(model.groupValue(rowIndex - 1), groupKey))) {
				y += 3;
				g.setColor(new Color(0x99, 0x99, 0x99));
				g.fillRect(20, y, (int) ReportLayout.CONTENT_WIDTH, 18);
				g.setColor(Color.WHITE);
				g.setFont(HEADER);
				g.drawString(String.valueOf(groupKey), 23, y + 13);
				g.setFont(BODY);
				y += 21;
			}
			if (summary) {
				y += 2;
				g.setColor(new Color(0x66, 0x66, 0x66));
				g.drawLine(20, y, 20 + (int)ReportLayout.CONTENT_WIDTH, y);
				g.setFont(HEADER);
				g.setColor(Color.BLACK);
			}
			double x = 20;
			for (int i = segment.startColumn(); i < segment.endColumn(); i++) {
				int local = i - segment.startColumn();
				int width = (int) Math.round(widths[local]);
				Object value = row.get(i);
				List<String> lines = ReportTextLayout.wrap(format(value, model.columns().get(i)), width - 6);
				int from = page.hasRowFragment() ? Math.min(page.firstRowLine(), lines.size()) : 0;
				int to = page.hasRowFragment() ? Math.min(page.endRowLine(), lines.size()) : lines.size();
				drawWrapped(g, lines, from, to, (int)x + 3, y + 11, width - 6);
				x += widths[local];
			}
			y += ReportLayout.tableRowHeight(model, segment, rowIndex);
		}
		if (model.totalRow() && page.endRow() == model.rows().size()) {
			y += 3;
			g.drawLine(20, y, 20 + (int) ReportLayout.CONTENT_WIDTH, y);
			g.setFont(HEADER);
			g.drawString(Locale.getDefault().getLanguage().equals("ja") ? "合計" : "Total", 23, y + 16);
		}
	}

	private void drawFooter(Graphics2D g, int pageIndex, int segmentIndex) {
		g.setFont(BODY);
		double bottom = layout.pageHeight(model) - 58;
		g.drawLine(20, (int)bottom, (int)layout.pageWidth(model) - 20, (int)bottom);
		String page = Locale.getDefault().getLanguage().equals("ja") ? "ページ " : "Page ";
		String continuation = layout.segmentCount() > 1
			? " · " + (segmentIndex + 1) + "/" + layout.segmentCount() : "";
		String text = page + (pageIndex + 1) + continuation;
		FontMetrics fm = g.getFontMetrics();
		g.drawString(text, (int)layout.pageWidth(model) / 2 - fm.stringWidth(text) / 2, (int)bottom + 20);
	}

	private static String format(Object value, Field field) {
		if (value == null) return "";
		if (value instanceof Number && (field.isDurationOrWork() || field.isMoney())) return value.toString();
		return String.valueOf(value);
	}

	private static void drawClipped(Graphics2D g, String text, int x, int baseline, int width, FontMetrics fm) {
		if (text == null || width <= 0) return;
		int measured = fm.stringWidth(text);
		if (measured <= width) { g.drawString(text, x, baseline); return; }
		Graphics2D scaled = (Graphics2D)g.create();
		try {
			scaled.translate(x, 0);
			scaled.scale(Math.max(0.35, (double)width / measured), 1d);
			scaled.drawString(text, 0, baseline);
		} finally { scaled.dispose(); }
	}

	private static void drawWrapped(Graphics2D g, List<String> lines, int from, int to, int x, int baseline, int width) {
		FontMetrics fm = g.getFontMetrics();
		for (int i = Math.max(0, from); i < Math.min(lines.size(), to); i++)
			drawClipped(g, lines.get(i), x, baseline + (i - from) * 12, width, fm);
	}

	public ReportLayout layout() { return layout; }
	public ReportModel model() { return model; }
}
