/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.model;

import java.util.List;

import com.microproject.field.Field;

/** Immutable tabular report content and layout metadata. */
public record ReportModel(String title, List<Field> columns, List<List<Object>> rows,
		int[] columnWidths, int groupColumn, List<Object> groupKeys, String projectName,
		boolean totalRow, boolean labelValueSheet, boolean portrait, List<String> displayLabels,
		List<Section> sections) {
	public ReportModel(String title, List<Field> columns, List<List<Object>> rows, int[] columnWidths,
			int groupColumn, List<Object> groupKeys, String projectName, boolean totalRow,
			boolean labelValueSheet, boolean portrait) {
		this(title, columns, rows, columnWidths, groupColumn, groupKeys, projectName, totalRow,
			labelValueSheet, portrait, columns.stream().map(Field::getName).toList(), List.of());
	}

	public ReportModel {
		columns = List.copyOf(columns);
		rows = rows.stream().map(row -> java.util.Collections.unmodifiableList(new java.util.ArrayList<>(row))).toList();
		columnWidths = columnWidths.clone();
		groupKeys = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(groupKeys));
		displayLabels = List.copyOf(displayLabels);
		sections = List.copyOf(sections);
		if (columns.size() != columnWidths.length) throw new IllegalArgumentException("Column widths must match columns");
		if (columns.size() != displayLabels.size()) throw new IllegalArgumentException("Display labels must match columns");
		if (rows.size() != groupKeys.size()) throw new IllegalArgumentException("Every row requires a group key slot");
		if (groupColumn < -1 || groupColumn >= columns.size()) throw new IllegalArgumentException("Invalid group column");
	}

	@Override public int[] columnWidths() { return columnWidths.clone(); }

	public record GroupKey(Object value, boolean summary) { }
	public record Section(String title, int startRow, int endRow) {
		public Section {
			if (startRow < 0 || endRow <= startRow) throw new IllegalArgumentException("Invalid report section range");
		}
		public boolean startsAt(int row) { return startRow == row; }
	}
	public String displayLabel(int index) { return displayLabels.get(index); }
	public Section sectionStartingAt(int row) {
		return sections.stream().filter(section -> section.startsAt(row)).findFirst().orElse(null);
	}
	public Object groupValue(int row) {
		Object key = groupKeys.get(row);
		return key instanceof GroupKey grouped ? grouped.value() : key;
	}
	public boolean summaryRow(int row) {
		Object key = groupKeys.get(row);
		return key instanceof GroupKey grouped && grouped.summary();
	}
}
