/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.model;

import java.util.ArrayList;
import java.util.List;

/** Deterministic page and row layout for printable reports. Measurements use points. */
public final class ReportLayout {
	public static final double LANDSCAPE_WIDTH = 1008;
	public static final double LANDSCAPE_HEIGHT = 612;
	public static final double PORTRAIT_WIDTH = 612;
	public static final double PORTRAIT_HEIGHT = 1008;
	public static final double LEFT_MARGIN = 20;
	public static final double RIGHT_MARGIN = 20;
	public static final double TOP_MARGIN = 30;
	public static final double BOTTOM_MARGIN = 30;
	public static final double CONTENT_WIDTH = LANDSCAPE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN;
	private static final double TABLE_ROW_HEIGHT = 15;
	private static final double TABLE_GROUP_HEIGHT = 21;

	public record Segment(int startColumn, int endColumn, double[] widths) {
		public Segment { widths = widths.clone(); }
		@Override public double[] widths() { return widths.clone(); }
	}
	public record Page(int segmentIndex, int firstRow, int endRow, boolean repeatsHeader,
		int firstRowLine, int endRowLine) {
		public Page(int segmentIndex, int firstRow, int endRow, boolean repeatsHeader) {
			this(segmentIndex, firstRow, endRow, repeatsHeader, -1, -1);
		}
		public boolean hasRowFragment() { return firstRowLine >= 0; }
	}

	private final List<Segment> segments;
	private final List<Page> pages;

	public ReportLayout(ReportModel model) {
		segments = model.labelValueSheet() ? List.of(new Segment(0, model.columns().size(),
			java.util.Arrays.stream(model.columnWidths()).mapToDouble(width -> Math.max(1, width)).toArray()))
			: splitColumns(model.columnWidths());
		ArrayList<Page> calculated = new ArrayList<>();
		double pageHeight = model.portrait() ? PORTRAIT_HEIGHT : LANDSCAPE_HEIGHT;
		double bottom = pageHeight - BOTTOM_MARGIN - 38;
		int count = model.labelValueSheet() ? model.columns().size() : model.rows().size();
		for (int segment = 0; segment < segments.size(); segment++) {
			if (count == 0) calculated.add(new Page(segment, 0, 0, true));
			for (int first = 0; first < count;) {
				double y = 117;
				int end = first;
				boolean splitRow = false;
				while (end < count) {
					double height = model.labelValueSheet() ? labelHeight(model, end)
						: tableRowHeight(model, segments.get(segment), end);
					boolean groupHeader = !model.labelValueSheet() && !model.summaryRow(end)
						&& model.groupColumn() >= 0 && (end == first
							|| !java.util.Objects.equals(model.groupValue(end - 1), model.groupValue(end)));
					if (groupHeader) height += TABLE_GROUP_HEIGHT;
					if (end > first && y + height > bottom) break;
					if (end == first && y + height > bottom) {
						int lines = model.labelValueSheet() ? labelLineCount(model, end)
							: tableLineCount(model, segments.get(segment), end);
						int sectionHeader = model.labelValueSheet() && model.sectionStartingAt(end) != null ? 20 : 0;
						int lineCapacity = availableLines(bottom, model.labelValueSheet(), groupHeader, sectionHeader);
						int lineStart = 0;
						while (lineStart < lines) {
							int lineEnd = Math.min(lines, lineStart + lineCapacity);
							calculated.add(new Page(segment, end, end + 1, lineStart > 0, lineStart, lineEnd));
							lineStart = lineEnd;
						}
						end++;
						splitRow = true;
						break;
					}
					y += height;
					end++;
				}
				if (splitRow) first = end;
				else {
					calculated.add(new Page(segment, first, end, first > 0));
					first = end;
				}
			}
		}
		pages = List.copyOf(calculated);
	}

	public static int labelHeight(ReportModel model, int row) {
		int lines = labelLineCount(model, row);
		int contentHeight = lines * 12 + 8;
		int rowHeight = model.columns().get(row).getId().equals("Field.notes") ? Math.max(135, contentHeight) : Math.max(29, contentHeight);
		return rowHeight + (model.sectionStartingAt(row) == null ? 0 : 20);
	}

	private static int labelLineCount(ReportModel model, int row) {
		Object value = model.rows().isEmpty() ? null : model.rows().get(0).get(row);
		return ReportTextLayout.lineCount(value == null ? "" : value.toString(), 350);
	}

	public static double tableRowHeight(ReportModel model, Segment segment, int row) {
		return Math.max(TABLE_ROW_HEIGHT, tableLineCount(model, segment, row) * 12d + 3d);
	}

	private static int tableLineCount(ReportModel model, Segment segment, int row) {
		double[] widths = segment.widths();
		int maxLines = 1;
		for (int column = segment.startColumn(); column < segment.endColumn(); column++) {
			Object value = model.rows().get(row).get(column);
			if (value == null) continue;
			double columnWidth = widths[column - segment.startColumn()];
			maxLines = Math.max(maxLines, ReportTextLayout.lineCount(value.toString(), (int)columnWidth - 6));
		}
		return maxLines;
	}

	private static int availableLines(double bottom, boolean labelValueSheet, boolean groupHeader, int sectionHeader) {
		double y = 117;
		if (groupHeader) y += TABLE_GROUP_HEIGHT + 3;
		return Math.max(1, (int)Math.floor((bottom - y - (labelValueSheet ? 8 : 3) - sectionHeader) / 12d));
	}

	private static List<Segment> splitColumns(int[] widths) {
		ArrayList<Segment> result = new ArrayList<>();
		int start = 0;
		while (start < widths.length) {
			int end = start;
			int sum = 0;
			while (end < widths.length && (sum + Math.max(1, widths[end]) <= CONTENT_WIDTH || end == start)) {
				sum += Math.max(1, widths[end++]);
			}
			if (end == start) end++;
			int[] raw = java.util.Arrays.copyOfRange(widths, start, end);
			double total = java.util.Arrays.stream(raw).map(value -> Math.max(1, value)).sum();
			double[] scaled = new double[raw.length];
			for (int i = 0; i < raw.length; i++) scaled[i] = Math.max(1, raw[i]) * CONTENT_WIDTH / total;
			result.add(new Segment(start, end, scaled));
			start = end;
		}
		if (result.isEmpty()) result.add(new Segment(0, 0, new double[0]));
		return List.copyOf(result);
	}

	public int pageCount() { return pages.size(); }
	public Page page(int index) { return pages.get(index); }
	public Segment segment(int index) { return segments.get(index); }
	public int segmentCount() { return segments.size(); }
	public List<Page> pages() { return pages; }
	public double pageWidth(ReportModel model) { return model.portrait() ? PORTRAIT_WIDTH : LANDSCAPE_WIDTH; }
	public double pageHeight(ReportModel model) { return model.portrait() ? PORTRAIT_HEIGHT : LANDSCAPE_HEIGHT; }
}
