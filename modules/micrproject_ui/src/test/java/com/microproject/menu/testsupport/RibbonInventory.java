/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.menu.testsupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public final class RibbonInventory {
	private final List<String> taskIds;
	private final Map<String, List<String>> bandsByTask;
	private final Map<String, List<String>> buttonsByBand;
	private final Map<String, ButtonSpec> buttons;

	private RibbonInventory(
		List<String> taskIds,
		Map<String, List<String>> bandsByTask,
		Map<String, List<String>> buttonsByBand,
		Map<String, ButtonSpec> buttons) {
		this.taskIds = taskIds;
		this.bandsByTask = bandsByTask;
		this.buttonsByBand = buttonsByBand;
		this.buttons = buttons;
	}

	public static RibbonInventory standardRibbon() {
		ResourceBundle internal = ResourceBundle.getBundle("com.microproject.menu.menuInternal", Locale.ROOT);
		ResourceBundle images = ResourceBundle.getBundle("com.microproject.pm.graphic.images", Locale.getDefault());
		List<String> taskIds = Collections.unmodifiableList(tokens(internal, "StandardRibbon"));
		Map<String, List<String>> bandsByTask = new LinkedHashMap<>();
		Map<String, List<String>> buttonsByBand = new LinkedHashMap<>();
		Map<String, ButtonSpec> buttons = new LinkedHashMap<>();

		for (String taskId : taskIds) {
			List<String> bandIds = Collections.unmodifiableList(tokens(internal, taskId));
			bandsByTask.put(taskId, bandIds);
			for (String bandId : bandIds) {
				List<String> buttonIds = new ArrayList<>();
				for (String token : tokens(internal, bandId)) {
					String normalized = normalizeButtonToken(token);
					if (normalized == null) {
						continue;
					}
					buttonIds.add(normalized);
					buttons.computeIfAbsent(normalized, buttonId -> new ButtonSpec(
						buttonId,
						getOptional(internal, buttonId + ".icon"),
						getOptional(images, getOptional(internal, buttonId + ".icon")),
						getOptional(internal, buttonId + ".action"),
						getOptional(internal, buttonId + ".type")));
				}
				buttonsByBand.put(bandId, Collections.unmodifiableList(buttonIds));
			}
		}
		return new RibbonInventory(
			taskIds,
			Collections.unmodifiableMap(bandsByTask),
			Collections.unmodifiableMap(buttonsByBand),
			Collections.unmodifiableMap(buttons));
	}

	public List<String> taskIds() {
		return taskIds;
	}

	public Map<String, List<String>> bandsByTask() {
		return bandsByTask;
	}

	public Map<String, List<String>> buttonsByBand() {
		return buttonsByBand;
	}

	public Map<String, ButtonSpec> buttons() {
		return buttons;
	}

	public Set<String> buttonIds() {
		return Collections.unmodifiableSet(new LinkedHashSet<>(buttons.keySet()));
	}

	public Set<String> buttonIdsForTask(String taskId) {
		Set<String> result = new LinkedHashSet<>();
		for (String bandId : bandsByTask.getOrDefault(taskId, List.of())) {
			result.addAll(buttonsByBand.getOrDefault(bandId, List.of()));
		}
		return Collections.unmodifiableSet(result);
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

	private static String getOptional(ResourceBundle bundle, String key) {
		if (key == null || !bundle.containsKey(key)) {
			return null;
		}
		return bundle.getString(key);
	}

	public static final class ButtonSpec {
		private final String id;
		private final String iconKey;
		private final String iconFileName;
		private final String actionKey;
		private final String type;

		private ButtonSpec(String id, String iconKey, String iconFileName, String actionKey, String type) {
			this.id = id;
			this.iconKey = iconKey;
			this.iconFileName = iconFileName;
			this.actionKey = actionKey;
			this.type = type;
		}

		public String id() {
			return id;
		}

		public String iconKey() {
			return iconKey;
		}

		public String iconFileName() {
			return iconFileName;
		}

		public String actionKey() {
			return actionKey;
		}

		public String type() {
			return type;
		}

		public boolean requiresIcon() {
			return iconKey != null && !iconKey.isBlank();
		}
	}
}
