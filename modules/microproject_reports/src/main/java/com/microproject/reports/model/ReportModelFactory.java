/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.model;

import java.util.List;

import com.microproject.configuration.ReportColumns;
import com.microproject.configuration.ReportDefinition;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.reports.adapter.ReportDataSource;
import com.microproject.reports.adapter.DataSourceProvider;

/** Turns the existing view.xml report definitions and selected rows into models. */
public final class ReportModelFactory {
	private record ProjectDetailField(String id, String label) { }
	private static final List<List<ProjectDetailField>> PROJECT_DETAILS_SECTIONS = List.of(
		List.of(detail("Field.start", "start"), detail("Field.baselineStart", "baselineStart"),
			detail("Field.actualStart", "actualStart"), detail("Field.finish", "finish"),
			detail("Field.baselineFinish", "baselineFinish"), detail("Field.actualFinish", "actualFinish")),
		List.of(detail("Field.duration", "scheduled"), detail("Field.baselineDuration", "baseline"),
			detail("Field.remainingDuration", "remaining"), detail("Field.actualDuration", "actual"),
			detail("Field.percentComplete", "percentComplete")),
		List.of(detail("Field.work", "scheduled"), detail("Field.baselineWork", "baseline"),
			detail("Field.remainingWork", "remaining"), detail("Field.actualWork", "actual")),
		List.of(detail("Field.cost", "scheduled"), detail("Field.baselineCost", "baseline"),
			detail("Field.cv", "variance"), detail("Field.remainingCost", "remaining"),
			detail("Field.actualCost", "actual")),
		List.of(detail("Field.notes", "notes")));
	private static final List<String> PROJECT_DETAILS_SECTION_NAMES = List.of("dates", "duration", "work", "costs", "notes");
	private static ProjectDetailField detail(String id, String label) { return new ProjectDetailField(id, label); }
	static List<Field> projectDetailsFields() {
		return PROJECT_DETAILS_SECTIONS.stream().flatMap(List::stream).map(ProjectDetailField::id)
			.map(com.microproject.configuration.Configuration::getFieldFromId).filter(java.util.Objects::nonNull).toList();
	}
	private ReportModelFactory() { }

	public static ReportModel create(ReportDefinition definition, SpreadSheetFieldArray selected,
			ReportDataSource source, String projectName) {
		java.util.ArrayList<ReportColumns> columnSets = definition.getColumnsList();
		if (columnSets.isEmpty()) {
			java.util.ResourceBundle labelsBundle = java.util.ResourceBundle.getBundle(
				"com.microproject.reports.model.ReportLabels", java.util.Locale.getDefault());
			java.util.ArrayList<Field> fields = new java.util.ArrayList<>();
			java.util.ArrayList<String> labels = new java.util.ArrayList<>();
			java.util.ArrayList<ReportModel.Section> sections = new java.util.ArrayList<>();
			for (int section = 0; section < PROJECT_DETAILS_SECTIONS.size(); section++) {
				int start = fields.size();
				for (ProjectDetailField item : PROJECT_DETAILS_SECTIONS.get(section)) {
					Field field = com.microproject.configuration.Configuration.getFieldFromId(item.id());
					if (field != null) {
						fields.add(field);
						labels.add(labelsBundle.getString(item.label()));
					}
				}
				if (fields.size() > start) sections.add(new ReportModel.Section(
					labelsBundle.getString(PROJECT_DETAILS_SECTION_NAMES.get(section)), start, fields.size()));
			}
			ReportDataSource.Row row = source.rows(fields, true, null).stream().findFirst().orElse(null);
			List<List<Object>> rows = row == null ? List.of() : List.of(row.values());
			return new ReportModel(definition.getName(), fields, rows,
				fields.stream().mapToInt(field -> Math.max(1, field.getColumnWidth())).toArray(),
				-1, row == null ? List.of() : java.util.Collections.singletonList(row.groupKey()),
				projectName, false, true, true, labels, sections);
		}
		boolean grouped = columnSets.size() == 2;
		List<Field> columns = DataSourceProvider.fieldsFor(definition, selected);
		String groupId = grouped ? columnSets.get(0).getGroupbyField() : null;
		List<com.microproject.reports.adapter.ReportDataSource.Row> extracted = source.rows(columns, true, groupId);
		java.util.ArrayList<List<Object>> rows = new java.util.ArrayList<>();
		java.util.ArrayList<Object> groupKeys = new java.util.ArrayList<>();
		boolean hasSums = columns.stream().anyMatch(field -> field.getSummaryForGroup() == Field.SUM);
		Object currentGroup = null;
		java.util.ArrayList<com.microproject.reports.adapter.ReportDataSource.Row> groupRows = new java.util.ArrayList<>();
		for (var row : extracted) {
			if (grouped && !groupRows.isEmpty() && !java.util.Objects.equals(currentGroup, row.groupKey())) {
				appendSummary(columns, groupRows, currentGroup, source, rows, groupKeys, hasSums);
				groupRows.clear();
			}
			currentGroup = row.groupKey();
			groupRows.add(row);
			rows.add(row.values());
			groupKeys.add(row.groupKey());
		}
		if (hasSums && !groupRows.isEmpty()) appendSummary(columns, groupRows, currentGroup, source, rows, groupKeys, true);
		else if (hasSums && !grouped && !extracted.isEmpty()) appendSummary(columns, groupRows, null, source, rows, groupKeys, true);
		int[] widths = columns.stream().mapToInt(field -> Math.max(1, field.getColumnWidth())).toArray();
		int groupColumn = grouped ? 0 : -1;
		if (groupId != null) {
			for (int i = 0; i < columns.size(); i++) {
				if (groupId.equals(columns.get(i).getId())) { groupColumn = i; break; }
			}
		}
		return new ReportModel(definition.getName(), columns, rows, widths, groupColumn, groupKeys,
			projectName, false, false, false);
	}

	private static void appendSummary(List<Field> columns,
			List<com.microproject.reports.adapter.ReportDataSource.Row> groupRows, Object groupKey,
			ReportDataSource source, List<List<Object>> rows, List<Object> groupKeys, boolean hasSums) {
		if (!hasSums || groupRows.isEmpty()) return;
		java.util.ArrayList<Object> values = new java.util.ArrayList<>(columns.size());
		for (int column = 0; column < columns.size(); column++) {
			Field field = columns.get(column);
			if (field.getSummaryForGroup() != Field.SUM) { values.add(null); continue; }
			double sum = 0d;
			for (var row : groupRows) if (row.rawValues().get(column) instanceof Number number) sum += number.doubleValue();
			values.add(source.formatAggregate(field, Double.valueOf(sum)));
		}
		rows.add(values);
		groupKeys.add(new ReportModel.GroupKey(groupKey, true));
	}

}
