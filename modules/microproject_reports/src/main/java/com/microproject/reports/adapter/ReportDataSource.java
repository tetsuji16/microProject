/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.adapter;

import java.util.List;
import java.util.function.Predicate;

import com.microproject.field.Field;
import com.microproject.grouping.core.model.WalkersNodeModel;
import com.microproject.grouping.core.transform.filtering.PredicatedNodeFilterIterator;
import com.microproject.pm.task.Project;

/** Renderer-independent access to report rows and configured field values. */
public final class ReportDataSource {
	private final DataSource fields;
	private final PredicatedNodeFilterIterator iterator;

	public ReportDataSource(Project project, PredicatedNodeFilterIterator iterator,
			WalkersNodeModel nodeModel, boolean nodeBased, boolean timeBased,
			Predicate<Object> predicate) {
		this.iterator = iterator;
		this.fields = new DataSource();
		fields.setProject(project);
		fields.setIterator(iterator);
		fields.setNodeModel(nodeModel);
		fields.setNodeBased(nodeBased);
		fields.setTimeBased(timeBased);
		if (predicate != null) fields.setPredicate(predicate);
	}

	public boolean next() {
		return fields.next();
	}

	public Object currentRow() {
		return fields.getObject();
	}

	public void setGroupKey(String fieldId) { fields.setGroupKey(fieldId); }
	public Object groupKey() { return fields.getGroupKey(); }

	public Object value(Field field, boolean formatted) {
		return fields.getFieldValue((formatted ? "MODTextFIELD" : "FIELD")
			+ field.getId().substring("Field.".length()));
	}

	public Object rawValue(Field field) { return fields.getRawFieldValue(field); }

	public String formatAggregate(Field field, Number value) {
		if (field.isWork()) return com.microproject.datatype.DurationFormat.formatWork(value.longValue(), fields.project.getDurationDefaults());
		if (field.isDuration()) return com.microproject.datatype.DurationFormat.format(value.longValue(), fields.project.getDurationDefaults());
		if (field.isMoney()) return java.text.NumberFormat.getCurrencyInstance(java.util.Locale.getDefault()).format(value.doubleValue());
		return value.toString();
	}

	public List<Row> rows(List<Field> selectedFields, boolean formatted, String groupFieldId) {
		java.util.ArrayList<Row> rows = new java.util.ArrayList<>();
		while (next()) {
			if (groupFieldId != null) setGroupKey(groupFieldId);
			java.util.ArrayList<Object> row = new java.util.ArrayList<>(selectedFields.size());
			for (Field field : selectedFields) row.add(value(field, formatted));
			java.util.ArrayList<Object> raw = new java.util.ArrayList<>(selectedFields.size());
			for (Field field : selectedFields) raw.add(rawValue(field));
			rows.add(new Row(groupKey(), row, raw));
		}
		return List.copyOf(rows);
	}

	public record Row(Object groupKey, List<Object> values, List<Object> rawValues) {
		public Row {
			values = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(values));
			rawValues = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(rawValues));
		}
	}

}
