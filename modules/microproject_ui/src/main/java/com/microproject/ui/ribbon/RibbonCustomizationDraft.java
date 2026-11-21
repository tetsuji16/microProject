/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mutable working copy of a {@link RibbonCustomization} used by the settings dialog. Edits never touch the saved
 * value; {@link #toCustomization()} produces the immutable result when the user applies them. The rules of the
 * model are enforced here so that the dialog can only build a valid customization: the File tab is fixed, default
 * groups and commands are not part of the draft, and only custom groups hold commands.
 */
public final class RibbonCustomizationDraft {
	/** A custom group and the canonical command IDs it holds. */
	public static final class Group {
		private final String id;
		private String name;
		private final List<String> commandIds = new ArrayList<>();

		private Group(String id, String name, List<String> commandIds) {
			this.id = id;
			this.name = name;
			this.commandIds.addAll(commandIds);
		}

		public String id() { return id; }

		public String name() { return name; }

		public List<String> commandIds() { return Collections.unmodifiableList(commandIds); }
	}

	/** A custom tab and its groups. */
	public static final class Tab {
		private final String id;
		private String name;
		private final List<Group> groups = new ArrayList<>();

		private Tab(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String id() { return id; }

		public String name() { return name; }
	}

	private final List<String> defaultTabOrder = new ArrayList<>();
	private final Set<String> hiddenDefaultTabs = new LinkedHashSet<>();
	private final List<Tab> customTabs = new ArrayList<>();
	private final Map<String, List<Group>> defaultTabGroups = new LinkedHashMap<>();

	public RibbonCustomizationDraft(RibbonCustomization source) {
		replaceWith(source);
	}

	/** Movable default tab IDs in their current order; the File tab is always first and not listed. */
	public List<String> defaultTabOrder() { return Collections.unmodifiableList(defaultTabOrder); }

	public List<Tab> customTabs() { return Collections.unmodifiableList(customTabs); }

	public boolean isDefaultTab(String tabId) { return RibbonCommandCatalog.defaultTabIds().contains(tabId); }

	public boolean isFileTab(String tabId) { return RibbonCustomization.FILE_TAB_ID.equals(tabId); }

	public boolean isHidden(String tabId) { return hiddenDefaultTabs.contains(tabId); }

	/** Shows or hides a default tab; the File tab cannot be hidden. */
	public boolean setHidden(String tabId, boolean hidden) {
		if (!isDefaultTab(tabId) || isFileTab(tabId)) return false;
		return hidden ? hiddenDefaultTabs.add(tabId) : hiddenDefaultTabs.remove(tabId);
	}

	public Tab customTab(String tabId) {
		for (Tab tab : customTabs) if (tab.id.equals(tabId)) return tab;
		return null;
	}

	/** The custom groups of a default or custom tab, in order; empty when the tab is unknown. */
	public List<Group> groups(String tabId) {
		Tab custom = customTab(tabId);
		if (custom != null) return Collections.unmodifiableList(custom.groups);
		return Collections.unmodifiableList(defaultTabGroups.getOrDefault(tabId, List.of()));
	}

	public Group group(String groupId) {
		for (Tab tab : customTabs) for (Group group : tab.groups) if (group.id.equals(groupId)) return group;
		for (List<Group> groups : defaultTabGroups.values()) for (Group group : groups) if (group.id.equals(groupId)) return group;
		return null;
	}

	/** The ID of the tab (default or custom) that holds the custom group, or null. */
	public String tabOfGroup(String groupId) {
		for (Tab tab : customTabs) for (Group group : tab.groups) if (group.id.equals(groupId)) return tab.id;
		for (Map.Entry<String, List<Group>> entry : defaultTabGroups.entrySet()) {
			for (Group group : entry.getValue()) if (group.id.equals(groupId)) return entry.getKey();
		}
		return null;
	}

	public Tab addTab(String name) {
		Tab tab = new Tab(nextId("custom-tab-"), requireName(name));
		customTabs.add(tab);
		return tab;
	}

	/** Adds a custom group at the end of a tab; the File tab cannot hold groups, so this returns null for it. */
	public Group addGroup(String tabId, String name) {
		if (isFileTab(tabId)) return null;
		Group group = new Group(nextId("custom-group-"), requireName(name), List.of());
		Tab custom = customTab(tabId);
		if (custom != null) custom.groups.add(group);
		else if (isDefaultTab(tabId)) defaultTabGroups.computeIfAbsent(tabId, key -> new ArrayList<>()).add(group);
		else return null;
		return group;
	}

	public boolean renameTab(String tabId, String name) {
		Tab tab = customTab(tabId);
		if (tab == null || name == null || name.isBlank()) return false;
		tab.name = name.strip();
		return true;
	}

	public boolean renameGroup(String groupId, String name) {
		Group group = group(groupId);
		if (group == null || name == null || name.isBlank()) return false;
		group.name = name.strip();
		return true;
	}

	/** Removes a custom tab; default tabs are only hidden, never removed. */
	public boolean removeTab(String tabId) {
		return customTabs.removeIf(tab -> tab.id.equals(tabId));
	}

	public boolean removeGroup(String groupId) {
		boolean removed = false;
		for (Tab tab : customTabs) removed |= tab.groups.removeIf(group -> group.id.equals(groupId));
		for (List<Group> groups : defaultTabGroups.values()) removed |= groups.removeIf(group -> group.id.equals(groupId));
		defaultTabGroups.values().removeIf(List::isEmpty);
		return removed;
	}

	/** Moves a default tab among the default tabs, or a custom tab among the custom tabs. Returns whether it moved. */
	public boolean moveTab(String tabId, int delta) {
		if (customTab(tabId) != null) return move(customTabs, indexOfTab(tabId), delta);
		int index = defaultTabOrder.indexOf(tabId);
		return index >= 0 && move(defaultTabOrder, index, delta);
	}

	/** Moves a custom group within its tab; default groups stay before it. */
	public boolean moveGroup(String groupId, int delta) {
		String tabId = tabOfGroup(groupId);
		if (tabId == null) return false;
		List<Group> groups = customTab(tabId) != null ? customTab(tabId).groups : defaultTabGroups.get(tabId);
		for (int index = 0; index < groups.size(); index++) {
			if (groups.get(index).id.equals(groupId)) return move(groups, index, delta);
		}
		return false;
	}

	/** Appends a command to a custom group. Returns false for an unknown group, an unknown command or a duplicate. */
	public boolean addCommand(String groupId, String commandId) {
		Group group = group(groupId);
		if (group == null || !RibbonCommandCatalog.quickAccessCandidates().contains(commandId)
			|| group.commandIds.contains(commandId)) return false;
		group.commandIds.add(commandId);
		return true;
	}

	public boolean removeCommand(String groupId, int index) {
		Group group = group(groupId);
		if (group == null || index < 0 || index >= group.commandIds.size()) return false;
		group.commandIds.remove(index);
		return true;
	}

	public boolean moveCommand(String groupId, int index, int delta) {
		Group group = group(groupId);
		return group != null && index >= 0 && index < group.commandIds.size() && move(group.commandIds, index, delta);
	}

	/** Restores one tab: its hidden state and position, or removes it when it is a custom tab. */
	public void resetTab(String tabId) {
		replaceWith(toCustomization().withoutTab(tabId));
	}

	public void resetAll() {
		replaceWith(RibbonCustomization.none());
	}

	public RibbonCustomization toCustomization() {
		List<String> order = defaultTabOrder.equals(naturalOrder()) ? List.of() : List.copyOf(defaultTabOrder);
		List<RibbonCustomization.CustomTab> tabs = new ArrayList<>();
		for (Tab tab : customTabs) tabs.add(new RibbonCustomization.CustomTab(tab.id, tab.name, toGroups(tab.groups)));
		Map<String, List<RibbonCustomization.CustomGroup>> groups = new LinkedHashMap<>();
		defaultTabGroups.forEach((tabId, list) -> groups.put(tabId, toGroups(list)));
		return new RibbonCustomization(order, Set.copyOf(hiddenDefaultTabs), tabs, groups);
	}

	private void replaceWith(RibbonCustomization source) {
		defaultTabOrder.clear();
		hiddenDefaultTabs.clear();
		customTabs.clear();
		defaultTabGroups.clear();
		defaultTabOrder.addAll(resolvedOrder(source));
		hiddenDefaultTabs.addAll(source.hiddenDefaultTabs());
		for (RibbonCustomization.CustomTab tab : source.customTabs()) {
			Tab copy = new Tab(tab.id(), tab.name());
			for (RibbonCustomization.CustomGroup group : tab.groups()) {
				copy.groups.add(new Group(group.id(), group.name(), group.commandIds()));
			}
			customTabs.add(copy);
		}
		source.defaultTabGroups().forEach((tabId, groups) -> {
			List<Group> copies = new ArrayList<>();
			for (RibbonCustomization.CustomGroup group : groups) {
				copies.add(new Group(group.id(), group.name(), group.commandIds()));
			}
			defaultTabGroups.put(tabId, copies);
		});
	}

	/** The default tab order the ribbon shows for {@code customization}: listed tabs take the listed slots in order. */
	private static List<String> resolvedOrder(RibbonCustomization customization) {
		List<String> slots = naturalOrder();
		java.util.Iterator<String> listed = customization.defaultTabOrder().stream().filter(slots::contains).iterator();
		Set<String> listedIds = new java.util.HashSet<>(customization.defaultTabOrder());
		List<String> order = new ArrayList<>();
		for (String slot : slots) order.add(listedIds.contains(slot) && listed.hasNext() ? listed.next() : slot);
		return order;
	}

	private static List<String> naturalOrder() {
		List<String> slots = new ArrayList<>(RibbonCommandCatalog.defaultTabIds());
		slots.remove(RibbonCustomization.FILE_TAB_ID);
		return slots;
	}

	private static List<RibbonCustomization.CustomGroup> toGroups(List<Group> groups) {
		List<RibbonCustomization.CustomGroup> result = new ArrayList<>();
		for (Group group : groups) result.add(new RibbonCustomization.CustomGroup(group.id, group.name, group.commandIds));
		return result;
	}

	private int indexOfTab(String tabId) {
		for (int index = 0; index < customTabs.size(); index++) if (customTabs.get(index).id.equals(tabId)) return index;
		return -1;
	}

	private String nextId(String prefix) {
		for (int number = 1;; number++) {
			String id = prefix + number;
			boolean taken = customTab(id) != null || group(id) != null;
			if (!taken) return id;
		}
	}

	private static String requireName(String name) {
		if (name == null || name.isBlank()) throw new IllegalArgumentException("Empty name");
		return name.strip();
	}

	private static <T> boolean move(List<T> list, int index, int delta) {
		int target = index + delta;
		if (index < 0 || target < 0 || target >= list.size()) return false;
		list.add(target, list.remove(index));
		return true;
	}
}
