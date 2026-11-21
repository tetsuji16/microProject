/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import com.microproject.menu.MenuManager;

/**
 * Read-only view of the default ribbon for the settings dialog: the canonical commands with their display names,
 * and the default tabs with their groups and commands. The names come from the menu resource bundle, so they
 * follow the application language.
 */
public final class RibbonCustomizationCatalog {
	/** A canonical command; its string form is the display name. */
	public record Command(String id, String name) {
		@Override
		public String toString() { return name; }
	}

	/** A default group and the canonical command IDs it holds. */
	public record DefaultGroup(String id, String title, List<String> commandIds) { }

	private RibbonCustomizationCatalog() { }

	/** All canonical commands, ordered by display name. */
	public static List<Command> commands() {
		List<Command> commands = new ArrayList<>();
		for (String id : RibbonCommandCatalog.quickAccessCandidates()) commands.add(new Command(id, commandName(id)));
		Collator collator = Collator.getInstance();
		commands.sort((first, second) -> {
			int byName = collator.compare(first.name(), second.name());
			return byName != 0 ? byName : first.id().compareTo(second.id());
		});
		return commands;
	}

	public static String commandName(String commandId) {
		String text = lookup(commandId + ".text");
		if (text == null) text = lookup(commandId + ".tooltip");
		return text == null || text.isBlank() ? commandId : text.trim();
	}

	/** Default tab IDs in default order, starting with the fixed File tab. */
	public static List<String> defaultTabIds() { return RibbonCommandCatalog.defaultTabIds(); }

	public static String tabTitle(String tabId) {
		String title = lookup(tabId + ".title");
		return title == null || title.isBlank() ? tabId : title.trim();
	}

	/** The default groups of a default tab, with the commands they show. */
	public static List<DefaultGroup> defaultGroups(String tabId) {
		List<DefaultGroup> groups = new ArrayList<>();
		for (String bandId : tokens(lookup(tabId))) {
			List<String> commands = new ArrayList<>();
			for (String token : tokens(lookup(bandId))) {
				String id = token.endsWith(".TOP") || token.endsWith(".LOW") ? token.substring(0, token.length() - 4) : token;
				if (!"-".equals(id) && RibbonCommandCatalog.isQuickAccessCandidate(id)) commands.add(id);
			}
			String title = lookup(bandId + ".title");
			groups.add(new DefaultGroup(bandId, title == null || title.isBlank() ? bandId : title.trim(), commands));
		}
		return groups;
	}

	private static List<String> tokens(String value) {
		List<String> result = new ArrayList<>();
		if (value == null) return result;
		for (String token : value.trim().split("\\s+")) if (!token.isEmpty()) result.add(token);
		return result;
	}

	private static String lookup(String key) {
		try {
			return MenuManager.getMenuString(key);
		} catch (MissingResourceException missing) {
			return null;
		}
	}
}
