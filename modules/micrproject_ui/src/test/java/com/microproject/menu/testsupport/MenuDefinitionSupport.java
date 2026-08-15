package com.microproject.menu.testsupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import com.microproject.menu.MenuManager;

public final class MenuDefinitionSupport {
	private static final String MENU_BUNDLE = "com.microproject.menu.menu";
	private static final String MENU_INTERNAL_BUNDLE = "com.microproject.menu.menuInternal";

	private MenuDefinitionSupport() {
	}

	public static ResourceBundle menuBundle(Locale locale) {
		return ResourceBundle.getBundle(MENU_BUNDLE, locale);
	}

	public static ResourceBundle menuInternalBundle() {
		return ResourceBundle.getBundle(MENU_INTERNAL_BUNDLE, Locale.ROOT);
	}

	public static ResourceBundle[] ribbonBundles(Locale locale) {
		return new ResourceBundle[] { menuInternalBundle(), menuBundle(locale) };
	}

	public static Set<String> ribbonButtonIds() {
		ResourceBundle internal = menuInternalBundle();
		Set<String> ids = new LinkedHashSet<>();
		for (String taskId : tokens(internal, "StandardRibbon")) {
			for (String bandId : tokens(internal, taskId)) {
				for (String token : tokens(internal, bandId)) {
					String normalized = normalizeButtonToken(token);
					if (normalized != null) {
						ids.add(normalized);
					}
				}
			}
		}
		return ids;
	}

	public static Set<String> ribbonUiButtonIds() {
		ResourceBundle internal = menuInternalBundle();
		Set<String> ids = new LinkedHashSet<>(ribbonButtonIds());
		for (String token : tokens(internal, "StandardRibbon.TaskBar")) {
			String normalized = normalizeButtonToken(token);
			if (normalized != null) {
				ids.add(normalized);
			}
		}
		return ids;
	}

	public static List<String> ribbonTaskIds() {
		return Collections.unmodifiableList(tokens(menuInternalBundle(), "StandardRibbon"));
	}

	public static List<String> ribbonBandIds(String taskId) {
		return Collections.unmodifiableList(tokens(menuInternalBundle(), taskId));
	}

	public static List<String> ribbonButtonIds(String bandId) {
		List<String> ids = new ArrayList<>();
		for (String token : tokens(menuInternalBundle(), bandId)) {
			String normalized = normalizeButtonToken(token);
			if (normalized != null) {
				ids.add(normalized);
			}
		}
		return Collections.unmodifiableList(ids);
	}

	public static Set<String> ribbonButtonIdsForTask(String taskId) {
		Set<String> ids = new LinkedHashSet<>();
		for (String bandId : ribbonBandIds(taskId)) {
			ids.addAll(ribbonButtonIds(bandId));
		}
		return Collections.unmodifiableSet(ids);
	}

	public static Map<String, List<String>> ribbonBandsByTask() {
		Map<String, List<String>> result = new LinkedHashMap<>();
		for (String taskId : ribbonTaskIds()) {
			result.put(taskId, ribbonBandIds(taskId));
		}
		return Collections.unmodifiableMap(result);
	}

	public static Set<String> toolBarButtonIds(String toolbarId) {
		ResourceBundle internal = menuInternalBundle();
		Set<String> ids = new LinkedHashSet<>();
		for (String token : tokens(internal, toolbarId)) {
			String normalized = normalizeButtonToken(token);
			if (normalized != null) {
				ids.add(normalized);
			}
		}
		return ids;
	}

	public static Set<String> displayedRibbonUiButtonIds() {
		Set<String> ids = new LinkedHashSet<>(ribbonUiButtonIds());
		ids.addAll(toolBarButtonIds(MenuManager.RIBBON_VIEW_BAR));
		ids.addAll(toolBarButtonIds(MenuManager.PRINT_PREVIEW_TOOL_BAR));
		return Collections.unmodifiableSet(ids);
	}

	public static boolean hasLocalizedLabel(ResourceBundle bundle, String id) {
		return hasKey(bundle, id + ".text") || hasKey(bundle, id + ".tooltip");
	}

	private static List<String> tokens(ResourceBundle bundle, String key) {
		String value = bundle.getString(key);
		String[] pieces = value.trim().split("\\s+");
		List<String> tokens = new ArrayList<>();
		for (String piece : pieces) {
			if (!piece.isBlank()) {
				tokens.add(piece);
			}
		}
		return tokens;
	}

	private static String normalizeButtonToken(String token) {
		if (token == null || token.isBlank() || "-".equals(token) || "|".equals(token) || "\\".equals(token)) {
			return null;
		}
		if (token.endsWith(".TOP") || token.endsWith(".LOW")) {
			return token.substring(0, token.lastIndexOf('.'));
		}
		return token;
	}

	private static boolean hasKey(ResourceBundle bundle, String key) {
		try {
			return bundle.getString(key) != null;
		} catch (MissingResourceException ex) {
			return false;
		}
	}
}
