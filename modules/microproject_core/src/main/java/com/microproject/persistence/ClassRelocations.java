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
