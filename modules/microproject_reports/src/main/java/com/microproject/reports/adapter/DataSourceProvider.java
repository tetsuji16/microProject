/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.adapter;

import java.util.function.Predicate;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import com.microproject.datatype.Duration;
import com.microproject.datatype.Money;
import com.microproject.datatype.Rate;
import com.microproject.field.Field;
import com.microproject.configuration.Configuration;
import com.microproject.configuration.Dictionary;
import com.microproject.configuration.ReportDefinition;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.grouping.core.model.WalkersNodeModel;
import com.microproject.grouping.core.transform.filtering.PredicatedNodeFilterIterator;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;

/** Report data selection shared by preview, printing, PDF and CSV. */
public final class DataSourceProvider {
	public static final String TIME_BASED = "timeBased";
	public static final String COLLECTION_TYPE_PROPERTY = "collectionType";
	public static final int PROJECT = 0;
	public static final int PROJECTS_TREE = 1;
	public static final int PROJECTS_FLAT = 2;
	public static final int TASKS_TREE = 3;
	public static final int TASKS_FLAT = 4;
	public static final int RESOURCES_TREE = 5;
	public static final int RESOURCES_FLAT = 6;
	public static final int ASSIGNMENTS = 7;
	public static final int PREDECESSORS = 8;
	public static final int SUCCESSORS = 9;
	public static final int TASKS_ASSIGNMENTS_TREE = 10;
	public static final int RESOURCES_ASSIGNMENTS_TREE = 11;
	public static final int TASKS_ASSIGNMENTS_FLAT = 12;
	public static final int RESOURCES_ASSIGNMENTS_FLAT = 13;
	public static final int ASSIGNMENTS_PROJECT_BASED = 14;
	public static final int ASSIGNMENTS_RESOURCE_BASED = 15;
	public static final String REPORT_VIEW = Messages.getString("View.Report");
	public static final String PROJECT_REPORT_VIEW = Messages.getString("View.ProjectReport");
	public static final String TASK_REPORT_VIEW = Messages.getString("View.TaskReport");
	public static final String RESOURCE_REPORT_VIEW = Messages.getString("View.ResourceReport");

	public static Object[] getReportDefinitions() { return Dictionary.getAll(ReportDefinition.CATEGORY); }
	public static ReportDefinition getFromName(String name) {
		return java.util.Arrays.stream(getReportDefinitions()).map(ReportDefinition.class::cast)
			.filter(definition -> definition.getName().equals(name)).findFirst().orElse(null);
	}
	public static List<Field> fieldsFor(ReportDefinition definition, SpreadSheetFieldArray selected) {
		if (definition.getColumnsList().isEmpty()) {
			Collection<Field> fields = Configuration.getAllFields();
			return fields.stream().filter(field -> !field.isGraphical() && field.isApplicable(Project.class))
				.sorted(java.util.Comparator.comparing(Field::getName)).toList();
		}
		SpreadSheetFieldArray fields = selected == null ? definition.getMainFieldArray() : selected;
		return fields.stream().filter(field -> !field.isGraphical()).toList();
	}

	public static String getViewName(int type) {
		return switch (type) {
			case PROJECT -> REPORT_VIEW;
			case PROJECTS_TREE, PROJECTS_FLAT -> PROJECT_REPORT_VIEW;
			case TASKS_TREE, TASKS_FLAT, TASKS_ASSIGNMENTS_TREE, TASKS_ASSIGNMENTS_FLAT, ASSIGNMENTS_PROJECT_BASED -> TASK_REPORT_VIEW;
			case RESOURCES_TREE, RESOURCES_FLAT, RESOURCES_ASSIGNMENTS_TREE, RESOURCES_ASSIGNMENTS_FLAT, ASSIGNMENTS_RESOURCE_BASED -> RESOURCE_REPORT_VIEW;
			default -> null;
		};
	}
	public static ReportDataSource createDataSource(int type, Project project,
			PredicatedNodeFilterIterator iterator, WalkersNodeModel model, boolean timeBased) {
		Predicate<Object> predicate = null;
		boolean tree = false;
		switch (type) {
			case PROJECT -> { }
			case PROJECTS_TREE -> { predicate = Project.instanceofPredicate(); tree = true; }
			case PROJECTS_FLAT -> predicate = Project.instanceofPredicate();
			case TASKS_TREE -> { predicate = Task.instanceofPredicate(); tree = true; }
			case TASKS_FLAT -> predicate = Task.instanceofPredicate();
			case RESOURCES_TREE -> { predicate = ResourceImpl.instanceofPredicate(); tree = true; }
			case RESOURCES_FLAT -> predicate = ResourceImpl.instanceofPredicate();
			case TASKS_ASSIGNMENTS_TREE, RESOURCES_ASSIGNMENTS_TREE -> tree = true;
			case TASKS_ASSIGNMENTS_FLAT, RESOURCES_ASSIGNMENTS_FLAT -> { }
			case ASSIGNMENTS_PROJECT_BASED, ASSIGNMENTS_RESOURCE_BASED -> predicate = Assignment.instanceofPredicate();
			default -> throw new IllegalArgumentException("Unsupported report collection type: " + type);
		}
		return new ReportDataSource(project, iterator, model, tree, timeBased, predicate);
	}
	public static ReportDataSource createProjectDataSource(Project project, boolean timeBased) {
		PredicatedNodeFilterIterator iterator = new PredicatedNodeFilterIterator() {
			private final Iterator<Object> delegate = java.util.List.<Object>of(project).iterator();
			@Override public boolean hasNext() { return delegate.hasNext(); }
			@Override public Object next() { return delegate.next(); }
			@Override public void setPredicate(Predicate<Object> predicate) { }
			@Override public void setNodeBased(boolean nodeBased) { }
		};
		return createDataSource(PROJECT, project, iterator, null, timeBased);
	}
	public static Object fieldValueConverterToPrimitiveType(Field field, Object value) {
		if (value == null) return null;
		if (field.isRate()) return Double.valueOf(((Rate)value).getValue());
		if (field.isMoney()) return Double.valueOf(((Money)value).doubleValue());
		if (field.isDurationOrWork()) return Long.valueOf(((Duration)value).longValue());
		return value;
	}
}
