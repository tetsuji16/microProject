/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.persistence;

import java.util.Map;
import java.util.Objects;

/**
 * Class-level renames applied when reading persisted MPO/POD/workspace streams.
 *
 * <p>Java serialization stores the fully qualified name of every class in the
 * stream, so a class moved to another package must be mapped from its old name
 * to its new name here. Keys and values are binary class names of top-level or
 * member classes ({@code com.example.Outer}); member classes
 * ({@code com.example.Outer$Inner}) and array descriptors
 * ({@code [Lcom.example.Outer;}) follow their outer class automatically.
 * A moved class must keep its simple name and its pinned
 * {@code serialVersionUID}.
 *
 * <p>Each entry stays for as long as older files must remain readable.
 */
public final class ClassRelocations {
	/** Old binary class name to new binary class name. */
	private static final Map<String, String> RELOCATIONS = Map.ofEntries(
		Map.entry("com.microproject.pm.assignment.Assignment", "com.microproject.model.assignment.Assignment"),
		Map.entry("com.microproject.pm.assignment.AssignmentDetail", "com.microproject.model.assignment.AssignmentDetail"),
		Map.entry("com.microproject.pm.assignment.AssignmentFormat", "com.microproject.model.assignment.AssignmentFormat"),
		Map.entry("com.microproject.pm.assignment.HasAssignmentsImpl", "com.microproject.model.assignment.HasAssignmentsImpl"),
		Map.entry("com.microproject.pm.assignment.contour.AbstractContourBucket", "com.microproject.model.assignment.contour.AbstractContourBucket"),
		Map.entry("com.microproject.pm.assignment.contour.FillerContourBucket", "com.microproject.model.assignment.contour.FillerContourBucket"),
		Map.entry("com.microproject.pm.assignment.contour.PersonalContourBucket", "com.microproject.model.assignment.contour.PersonalContourBucket"),
		Map.entry("com.microproject.pm.assignment.contour.StandardContourBucket", "com.microproject.model.assignment.contour.StandardContourBucket"),
		Map.entry("com.microproject.pm.availability.Availability", "com.microproject.model.availability.Availability"),
		Map.entry("com.microproject.pm.availability.AvailabilityTable", "com.microproject.model.availability.AvailabilityTable"),
		Map.entry("com.microproject.pm.calendar.CalendarDefinition", "com.microproject.model.calendar.CalendarDefinition"),
		Map.entry("com.microproject.pm.calendar.CalendarEvent", "com.microproject.model.calendar.CalendarEvent"),
		Map.entry("com.microproject.pm.calendar.CalendarRecurrence", "com.microproject.model.calendar.CalendarRecurrence"),
		Map.entry("com.microproject.pm.calendar.CalendarService", "com.microproject.model.calendar.CalendarService"),
		Map.entry("com.microproject.pm.calendar.InvalidCalendarError", "com.microproject.model.calendar.InvalidCalendarError"),
		Map.entry("com.microproject.pm.calendar.InvalidCalendarException", "com.microproject.model.calendar.InvalidCalendarException"),
		Map.entry("com.microproject.pm.calendar.InvalidCalendarIntersectionException", "com.microproject.model.calendar.InvalidCalendarIntersectionException"),
		Map.entry("com.microproject.pm.calendar.RecurringCalendarException", "com.microproject.model.calendar.RecurringCalendarException"),
		Map.entry("com.microproject.pm.calendar.WorkDay", "com.microproject.model.calendar.WorkDay"),
		Map.entry("com.microproject.pm.calendar.WorkRange", "com.microproject.model.calendar.WorkRange"),
		Map.entry("com.microproject.pm.calendar.WorkRangeException", "com.microproject.model.calendar.WorkRangeException"),
		Map.entry("com.microproject.pm.calendar.WorkWeek", "com.microproject.model.calendar.WorkWeek"),
		Map.entry("com.microproject.pm.calendar.WorkWeekPeriod", "com.microproject.model.calendar.WorkWeekPeriod"),
		Map.entry("com.microproject.pm.calendar.WorkingCalendar", "com.microproject.model.calendar.WorkingCalendar"),
		Map.entry("com.microproject.pm.calendar.WorkingHours", "com.microproject.model.calendar.WorkingHours"),
		Map.entry("com.microproject.pm.ccpm.CriticalChainBufferHistoryService", "com.microproject.schedule.ccpm.CriticalChainBufferHistoryService"),
		Map.entry("com.microproject.pm.ccpm.CriticalChainService", "com.microproject.schedule.ccpm.CriticalChainService"),
		Map.entry("com.microproject.pm.costing.CostRate", "com.microproject.model.costing.CostRate"),
		Map.entry("com.microproject.pm.costing.CostRateTable", "com.microproject.model.costing.CostRateTable"),
		Map.entry("com.microproject.pm.costing.CostRateTables", "com.microproject.model.costing.CostRateTables"),
		Map.entry("com.microproject.pm.criticalpath.PredecessorTaskTree", "com.microproject.schedule.criticalpath.PredecessorTaskTree"),
		Map.entry("com.microproject.pm.dependency.Dependency", "com.microproject.model.dependency.Dependency"),
		Map.entry("com.microproject.pm.dependency.DependencyFormat", "com.microproject.model.dependency.DependencyFormat"),
		Map.entry("com.microproject.pm.dependency.DependencyService", "com.microproject.model.dependency.DependencyService"),
		Map.entry("com.microproject.pm.dependency.DependencyType", "com.microproject.model.dependency.DependencyType"),
		Map.entry("com.microproject.pm.dependency.HasDependenciesImpl", "com.microproject.model.dependency.HasDependenciesImpl"),
		Map.entry("com.microproject.pm.key.HasCommonKeyImpl", "com.microproject.model.key.HasCommonKeyImpl"),
		Map.entry("com.microproject.pm.key.HasKeyImpl", "com.microproject.model.key.HasKeyImpl"),
		Map.entry("com.microproject.pm.key.HasUniqueIdImpl", "com.microproject.model.key.HasUniqueIdImpl"),
		Map.entry("com.microproject.pm.key.uniqueid.UniqueIdException", "com.microproject.model.key.uniqueid.UniqueIdException"),
		Map.entry("com.microproject.pm.resource.EnterpriseResource", "com.microproject.model.resource.EnterpriseResource"),
		Map.entry("com.microproject.pm.resource.ResourceFieldMutationService", "com.microproject.model.resource.ResourceFieldMutationService"),
		Map.entry("com.microproject.pm.resource.ResourceImpl", "com.microproject.model.resource.ResourceImpl"),
		Map.entry("com.microproject.pm.resource.ResourceLevelingService", "com.microproject.model.resource.ResourceLevelingService"),
		Map.entry("com.microproject.pm.scheduling.BarClosure", "com.microproject.schedule.BarClosure"),
		Map.entry("com.microproject.pm.scheduling.DelayableImpl", "com.microproject.schedule.DelayableImpl"),
		Map.entry("com.microproject.pm.scheduling.ScheduleEvent", "com.microproject.schedule.ScheduleEvent"),
		Map.entry("com.microproject.pm.snapshot.SnapshotEmptyException", "com.microproject.model.snapshot.SnapshotEmptyException"),
		Map.entry("com.microproject.pm.snapshot.SnapshottableImpl", "com.microproject.model.snapshot.SnapshottableImpl"),
		Map.entry("com.microproject.pm.task.DefaultSubProj", "com.microproject.model.task.DefaultSubProj"),
		Map.entry("com.microproject.pm.task.NormalTask", "com.microproject.model.task.NormalTask"),
		Map.entry("com.microproject.pm.task.ProgressLineSettings", "com.microproject.model.task.ProgressLineSettings"),
		Map.entry("com.microproject.pm.task.Project", "com.microproject.model.task.Project"),
		Map.entry("com.microproject.pm.task.Project$BaselineFacade", "com.microproject.model.task.Project$BaselineFacade"),
		Map.entry("com.microproject.pm.task.Project$IdentityFacade", "com.microproject.model.task.Project$IdentityFacade"),
		Map.entry("com.microproject.pm.task.Project$ScheduleFacade", "com.microproject.model.task.Project$ScheduleFacade"),
		Map.entry("com.microproject.pm.task.Project$SubprojectFacade", "com.microproject.model.task.Project$SubprojectFacade"),
		Map.entry("com.microproject.pm.task.Project$TaskAggregationFacade", "com.microproject.model.task.Project$TaskAggregationFacade"),
		Map.entry("com.microproject.pm.task.Project$TaskLifecycleFacade", "com.microproject.model.task.Project$TaskLifecycleFacade"),
		Map.entry("com.microproject.pm.task.Project$Workspace", "com.microproject.model.task.Project$Workspace"),
		Map.entry("com.microproject.pm.task.ProjectEvent", "com.microproject.model.task.ProjectEvent"),
		Map.entry("com.microproject.pm.task.RollupSpan", "com.microproject.model.task.RollupSpan"),
		Map.entry("com.microproject.pm.task.SummaryEnvelope", "com.microproject.model.task.SummaryEnvelope"),
		Map.entry("com.microproject.pm.task.Task", "com.microproject.model.task.Task"),
		Map.entry("com.microproject.pm.task.TaskLinkReferenceImpl", "com.microproject.model.task.TaskLinkReferenceImpl"),
		Map.entry("com.microproject.pm.task.TaskSnapshot", "com.microproject.model.task.TaskSnapshot"),
		Map.entry("com.microproject.pm.task.TaskSplitInterval", "com.microproject.model.task.TaskSplitInterval"),
		Map.entry("com.microproject.pm.time.ImmutableInterval", "com.microproject.model.time.ImmutableInterval"),
		Map.entry("com.microproject.pm.time.Interval", "com.microproject.model.time.Interval"),
		Map.entry("com.microproject.pm.time.MutableInterval", "com.microproject.model.time.MutableInterval"),
		Map.entry("com.microproject.grouping.core.GroupNodeImpl", "com.microproject.node.GroupNodeImpl"),
		Map.entry("com.microproject.grouping.core.Node", "com.microproject.node.Node"),
		Map.entry("com.microproject.grouping.core.NodeBridge", "com.microproject.node.NodeBridge"),
		Map.entry("com.microproject.grouping.core.NodeException", "com.microproject.node.NodeException"),
		Map.entry("com.microproject.grouping.core.NodeList", "com.microproject.node.NodeList"),
		Map.entry("com.microproject.grouping.core.OutlineCode", "com.microproject.node.OutlineCode"),
		Map.entry("com.microproject.grouping.core.ProjectSummaryRow", "com.microproject.node.ProjectSummaryRow"),
		Map.entry("com.microproject.grouping.core.VoidNodeImpl", "com.microproject.node.VoidNodeImpl"),
		Map.entry("com.microproject.grouping.core.event.HierarchyEvent", "com.microproject.node.event.HierarchyEvent"),
		Map.entry("com.microproject.grouping.core.transform.ViewTransformerEvent", "com.microproject.node.transform.ViewTransformerEvent"),
		Map.entry("com.microproject.server.access.PartnerInfo", "com.microproject.persistence.access.PartnerInfo"),
		Map.entry("com.microproject.graphic.configuration.BarStyles", "com.microproject.view.configuration.BarStyles"),
		Map.entry("com.microproject.graphic.configuration.GanttBarFormatOverrides", "com.microproject.view.configuration.GanttBarFormatOverrides"),
		Map.entry("com.microproject.graphic.configuration.SpreadSheetFieldArray", "com.microproject.view.configuration.SpreadSheetFieldArray"),
		Map.entry("com.microproject.server.data.AssignmentData", "com.microproject.exchange.serialize.AssignmentData"),
		Map.entry("com.microproject.server.data.ByteArrayObject", "com.microproject.exchange.serialize.ByteArrayObject"),
		Map.entry("com.microproject.server.data.CalendarData", "com.microproject.persistence.data.CalendarData"),
		Map.entry("com.microproject.server.data.CommonDataObject", "com.microproject.persistence.data.CommonDataObject"),
		Map.entry("com.microproject.server.data.CompanyData", "com.microproject.exchange.serialize.CompanyData"),
		Map.entry("com.microproject.server.data.DataObject", "com.microproject.persistence.data.DataObject"),
		Map.entry("com.microproject.server.data.DataObjectImpl", "com.microproject.exchange.serialize.DataObjectImpl"),
		Map.entry("com.microproject.server.data.DistributionData", "com.microproject.persistence.data.DistributionData"),
		Map.entry("com.microproject.server.data.DocumentData", "com.microproject.persistence.data.DocumentData"),
		Map.entry("com.microproject.server.data.EnterpriseResourceData", "com.microproject.exchange.serialize.EnterpriseResourceData"),
		Map.entry("com.microproject.server.data.ExtendedDistributionData", "com.microproject.persistence.data.ExtendedDistributionData"),
		Map.entry("com.microproject.server.data.GroupData", "com.microproject.exchange.serialize.GroupData"),
		Map.entry("com.microproject.server.data.IncrementalData", "com.microproject.exchange.serialize.IncrementalData"),
		Map.entry("com.microproject.server.data.LinkData", "com.microproject.exchange.serialize.LinkData"),
		Map.entry("com.microproject.server.data.LockException", "com.microproject.exchange.serialize.LockException"),
		Map.entry("com.microproject.server.data.ProjectData", "com.microproject.persistence.data.ProjectData"),
		Map.entry("com.microproject.server.data.ResourceData", "com.microproject.exchange.serialize.ResourceData"),
		Map.entry("com.microproject.server.data.RoleData", "com.microproject.exchange.serialize.RoleData"),
		Map.entry("com.microproject.server.data.SerializedDataObject", "com.microproject.persistence.data.SerializedDataObject"),
		Map.entry("com.microproject.server.data.TaskData", "com.microproject.exchange.serialize.TaskData"),
		Map.entry("com.microproject.server.data.UserData", "com.microproject.exchange.serialize.UserData"));

	/** The JVM accepts at most this many dimensions in an array descriptor. */
	private static final int MAX_ARRAY_DIMENSIONS = 255;

	private static volatile Map<String, String> table = RELOCATIONS;

	private ClassRelocations() {
	}

	/** Returns the current name for {@code name}, or {@code name} itself when it has not moved. */
	public static String relocate(String name) {
		if (name == null || name.isEmpty())
			return name;
		if (name.charAt(0) == '[')
			return relocateArrayDescriptor(name);
		return relocateClassName(name);
	}

	private static String relocateClassName(String name) {
		Map<String, String> current = table;
		String moved = current.get(name);
		if (moved != null)
			return moved;
		// A member class follows its enclosing class, and a member of that member follows too:
		// search the enclosing prefixes from the innermost one outward.
		for (int nested = name.lastIndexOf('$'); nested > 0; nested = name.lastIndexOf('$', nested - 1)) {
			String enclosing = current.get(name.substring(0, nested));
			if (enclosing != null)
				return enclosing + name.substring(nested);
		}
		return name;
	}

	private static String relocateArrayDescriptor(String name) {
		int dimensions = 0;
		while (dimensions < name.length() && name.charAt(dimensions) == '[')
			dimensions++;
		if (dimensions > MAX_ARRAY_DIMENSIONS || dimensions >= name.length() - 2
				|| name.charAt(dimensions) != 'L' || !name.endsWith(";"))
			return name;
		String component = name.substring(dimensions + 1, name.length() - 1);
		// A descriptor has exactly one component. Anything else (a ';' or '[' inside the
		// component, as in a crafted "[L[L...x;;;..." name) is malformed and is returned
		// unchanged instead of being parsed as another descriptor.
		if (component.isEmpty() || component.indexOf(';') >= 0 || component.indexOf('[') >= 0)
			return name;
		String moved = relocateClassName(component);
		if (moved.equals(component))
			return name;
		return name.substring(0, dimensions + 1) + moved + ";";
	}

	/** Replaces the table until the returned handle is closed. Tests only. */
	static AutoCloseable overrideForTesting(Map<String, String> replacement) {
		Map<String, String> copy = Map.copyOf(Objects.requireNonNull(replacement));
		Map<String, String> previous = table;
		table = copy;
		return () -> table = previous;
	}
}
