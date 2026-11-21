/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Immutable user customization of the ribbon: default tab order and visibility, custom tabs, and custom groups.
 * Default groups and commands are never part of this model, so they cannot be renamed, removed or reordered.
 * The File tab is fixed: it can be neither hidden nor moved.
 */
public final class RibbonCustomization {
	public static final String FILE_TAB_ID = "FileRibbonTask";
	static final String FORMAT_VERSION_LINE = "microProject.ribbonCustomization.v1";

	private static final Logger logger = Logger.getLogger(RibbonCustomization.class.getName());
	private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_.-]+");
	private static final RibbonCustomization NONE = new RibbonCustomization(List.of(), Set.of(), List.of(), Map.of());

	/** A user-defined group holding canonical command IDs. */
	public record CustomGroup(String id, String name, List<String> commandIds) {
		public CustomGroup {
			requireId(id, "group id");
			requireName(name, "group name");
			LinkedHashSet<String> commands = new LinkedHashSet<>();
			for (String commandId : commandIds) {
				if (!RibbonCommandCatalog.quickAccessCandidates().contains(commandId)) {
					logger.warning("Ribbon customization: dropping unknown command ID " + commandId);
				} else if (!commands.add(commandId)) {
					throw new IllegalArgumentException("Duplicate command ID in group " + id + ": " + commandId);
				}
			}
			commandIds = List.copyOf(commands);
		}
	}

	/** A user-defined tab with its custom groups. */
	public record CustomTab(String id, String name, List<CustomGroup> groups) {
		public CustomTab {
			requireId(id, "tab id");
			requireName(name, "tab name");
			groups = List.copyOf(groups);
			requireUniqueGroupIds(groups);
		}
	}

	private final List<String> defaultTabOrder;
	private final Set<String> hiddenDefaultTabs;
	private final List<CustomTab> customTabs;
	private final Map<String, List<CustomGroup>> defaultTabGroups;

	/**
	 * @param defaultTabOrder default tab IDs the user moved, in display order; unlisted tabs keep their default position
	 * @param hiddenDefaultTabs default tab IDs hidden by the user
	 * @param customTabs user-defined tabs
	 * @param defaultTabGroups custom groups added to default tabs, keyed by default tab ID
	 */
	public RibbonCustomization(List<String> defaultTabOrder, Set<String> hiddenDefaultTabs,
		List<CustomTab> customTabs, Map<String, List<CustomGroup>> defaultTabGroups) {
		this.defaultTabOrder = List.copyOf(knownDefaultTabs(defaultTabOrder, "order"));
		this.hiddenDefaultTabs = Set.copyOf(knownDefaultTabs(hiddenDefaultTabs, "hidden"));
		this.customTabs = List.copyOf(customTabs);
		Set<String> tabIds = new LinkedHashSet<>();
		Set<String> groupIds = new LinkedHashSet<>();
		for (CustomTab tab : this.customTabs) {
			if (RibbonCommandCatalog.defaultTabIds().contains(tab.id()) || !tabIds.add(tab.id())) {
				throw new IllegalArgumentException("Duplicate tab ID: " + tab.id());
			}
			for (CustomGroup group : tab.groups()) {
				if (!groupIds.add(group.id())) throw new IllegalArgumentException("Duplicate group ID: " + group.id());
			}
		}
		Map<String, List<CustomGroup>> groups = new LinkedHashMap<>();
		for (Map.Entry<String, List<CustomGroup>> entry : defaultTabGroups.entrySet()) {
			if (!RibbonCommandCatalog.defaultTabIds().contains(entry.getKey())) {
				logger.warning("Ribbon customization: dropping groups for unknown tab ID " + entry.getKey());
				continue;
			}
			requireUniqueGroupIds(entry.getValue());
			for (CustomGroup group : entry.getValue()) {
				if (!groupIds.add(group.id())) throw new IllegalArgumentException("Duplicate group ID: " + group.id());
			}
			if (!entry.getValue().isEmpty()) groups.put(entry.getKey(), List.copyOf(entry.getValue()));
		}
		this.defaultTabGroups = Map.copyOf(groups);
	}

	public static RibbonCustomization none() { return NONE; }

	public boolean isEmpty() { return equals(NONE); }

	public List<String> defaultTabOrder() { return defaultTabOrder; }

	public Set<String> hiddenDefaultTabs() { return hiddenDefaultTabs; }

	public List<CustomTab> customTabs() { return customTabs; }

	public Map<String, List<CustomGroup>> defaultTabGroups() { return defaultTabGroups; }

	/** Returns a copy with everything for the given tab (default or custom) removed. */
	public RibbonCustomization withoutTab(String tabId) {
		List<String> order = new ArrayList<>(defaultTabOrder);
		order.remove(tabId);
		Set<String> hidden = new LinkedHashSet<>(hiddenDefaultTabs);
		hidden.remove(tabId);
		List<CustomTab> tabs = new ArrayList<>(customTabs);
		tabs.removeIf(tab -> tab.id().equals(tabId));
		Map<String, List<CustomGroup>> groups = new LinkedHashMap<>(defaultTabGroups);
		groups.remove(tabId);
		return new RibbonCustomization(order, hidden, tabs, groups);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof RibbonCustomization that
			&& defaultTabOrder.equals(that.defaultTabOrder)
			&& hiddenDefaultTabs.equals(that.hiddenDefaultTabs)
			&& customTabs.equals(that.customTabs)
			&& defaultTabGroups.equals(that.defaultTabGroups);
	}

	@Override
	public int hashCode() {
		return Objects.hash(defaultTabOrder, hiddenDefaultTabs, customTabs, defaultTabGroups);
	}

	@Override
	public String toString() {
		return "RibbonCustomization[" + serialize().replace('\n', ' ') + "]";
	}

	/**
	 * Serializes to version 1 text: a header line, then one record per line. Fields are separated by ':', lists by
	 * ',', and names are URL-encoded (UTF-8).
	 * <pre>
	 * microProject.ribbonCustomization.v1
	 * order:&lt;tabId&gt;,&lt;tabId&gt;
	 * hidden:&lt;tabId&gt;,&lt;tabId&gt;
	 * tab:&lt;tabId&gt;:&lt;name&gt;
	 * group:&lt;tabId&gt;:&lt;groupId&gt;:&lt;name&gt;:&lt;commandId&gt;,&lt;commandId&gt;
	 * </pre>
	 * A {@code group} record belongs to the default or custom tab named by its first field.
	 */
	String serialize() {
		StringBuilder text = new StringBuilder(FORMAT_VERSION_LINE).append('\n');
		if (!defaultTabOrder.isEmpty()) text.append("order:").append(String.join(",", defaultTabOrder)).append('\n');
		if (!hiddenDefaultTabs.isEmpty()) {
			text.append("hidden:").append(String.join(",", new TreeSet<>(hiddenDefaultTabs))).append('\n');
		}
		for (CustomTab tab : customTabs) {
			text.append("tab:").append(tab.id()).append(':').append(encode(tab.name())).append('\n');
			for (CustomGroup group : tab.groups()) appendGroup(text, tab.id(), group);
		}
		for (Map.Entry<String, List<CustomGroup>> entry : new TreeMap<>(defaultTabGroups).entrySet()) {
			for (CustomGroup group : entry.getValue()) appendGroup(text, entry.getKey(), group);
		}
		return text.toString();
	}

	/**
	 * Parses version 1 text. A missing, unknown-version or malformed value yields {@link #none()}; this never throws.
	 */
	static RibbonCustomization parse(String text) {
		if (text == null) return NONE;
		try {
			String[] lines = text.split("\n", -1);
			if (!FORMAT_VERSION_LINE.equals(lines[0])) {
				logger.warning("Ribbon customization: unknown or missing version, ignoring saved value");
				return NONE;
			}
			List<String> order = new ArrayList<>();
			Set<String> hidden = new LinkedHashSet<>();
			Map<String, String> tabNames = new LinkedHashMap<>();
			Map<String, List<CustomGroup>> groupsByTab = new LinkedHashMap<>();
			for (int i = 1; i < lines.length; i++) {
				String line = lines[i];
				if (line.isEmpty()) continue;
				String[] f = line.split(":", -1);
				switch (f[0]) {
					case "order" -> order.addAll(splitList(f[1]));
					case "hidden" -> hidden.addAll(splitList(f[1]));
					case "tab" -> tabNames.put(f[1], decode(f[2]));
					case "group" -> groupsByTab.computeIfAbsent(f[1], k -> new ArrayList<>())
						.add(new CustomGroup(f[2], decode(f[3]), splitList(f[4])));
					default -> throw new IllegalArgumentException("Unknown record: " + f[0]);
				}
			}
			List<CustomTab> tabs = new ArrayList<>();
			for (Map.Entry<String, String> tab : tabNames.entrySet()) {
				List<CustomGroup> groups = groupsByTab.remove(tab.getKey());
				tabs.add(new CustomTab(tab.getKey(), tab.getValue(), groups == null ? List.of() : groups));
			}
			return new RibbonCustomization(order, hidden, tabs, groupsByTab);
		} catch (RuntimeException invalid) {
			logger.warning("Ribbon customization: ignoring malformed saved value (" + invalid.getMessage() + ")");
			return NONE;
		}
	}

	private static void appendGroup(StringBuilder text, String tabId, CustomGroup group) {
		text.append("group:").append(tabId).append(':').append(group.id()).append(':').append(encode(group.name()))
			.append(':').append(String.join(",", group.commandIds())).append('\n');
	}

	private static Set<String> knownDefaultTabs(Collection<String> tabIds, String purpose) {
		LinkedHashSet<String> known = new LinkedHashSet<>();
		for (String tabId : tabIds) {
			if (FILE_TAB_ID.equals(tabId)) {
				logger.warning("Ribbon customization: the File tab cannot be hidden or moved (" + purpose + ")");
			} else if (!RibbonCommandCatalog.defaultTabIds().contains(tabId)) {
				logger.warning("Ribbon customization: dropping unknown tab ID " + tabId);
			} else {
				known.add(tabId);
			}
		}
		return known;
	}

	private static void requireUniqueGroupIds(List<CustomGroup> groups) {
		Set<String> ids = new LinkedHashSet<>();
		for (CustomGroup group : groups) {
			if (!ids.add(group.id())) throw new IllegalArgumentException("Duplicate group ID: " + group.id());
		}
	}

	private static void requireId(String id, String what) {
		if (id == null || !ID_PATTERN.matcher(id).matches()) throw new IllegalArgumentException("Invalid " + what + ": " + id);
	}

	private static void requireName(String name, String what) {
		if (name == null || name.isBlank()) throw new IllegalArgumentException("Empty " + what);
	}

	private static List<String> splitList(String value) {
		return value.isEmpty() ? List.of() : List.of(value.split(",", -1));
	}

	private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

	private static String decode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
}
