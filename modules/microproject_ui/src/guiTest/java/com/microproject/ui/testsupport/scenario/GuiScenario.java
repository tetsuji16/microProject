/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport.scenario;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A GUI scenario read from JSON: a name, a fixture that says which project to open, and an ordered
 * list of steps. Each step is a JSON object with exactly one key, the step kind. The format is
 * documented in {@code docs/gui-run-harness.md}.
 */
public record GuiScenario(String name, String description, JsonNode fixture, List<JsonNode> steps) {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public GuiScenario {
		if (name == null || name.isBlank()) throw new IllegalArgumentException("a scenario needs a name");
		if (fixture == null || !fixture.isObject()) throw new IllegalArgumentException("scenario \"" + name + "\" needs a fixture object");
		if (steps == null || steps.isEmpty()) throw new IllegalArgumentException("scenario \"" + name + "\" needs steps");
		for (int index = 0; index < steps.size(); index++) {
			JsonNode step = steps.get(index);
			if (!step.isObject() || step.size() != 1) {
				throw new IllegalArgumentException("scenario \"" + name + "\" step " + (index + 1)
					+ " must be an object with exactly one key: " + step);
			}
		}
		steps = List.copyOf(steps);
	}

	/** Reads a scenario from its JSON text. */
	public static GuiScenario parse(String json) {
		try {
			JsonNode root = MAPPER.readTree(json);
			List<JsonNode> steps = new ArrayList<>();
			if (root.path("steps").isArray()) root.get("steps").forEach(steps::add);
			return new GuiScenario(text(root, "name"), text(root, "description"), root.get("fixture"), steps);
		} catch (IOException failure) {
			throw new IllegalArgumentException("scenario is not valid JSON: " + failure.getMessage(), failure);
		}
	}

	/** Reads a scenario from a UTF-8 JSON file; a leading byte order mark is ignored. */
	public static GuiScenario load(Path file) throws IOException {
		String text = Files.readString(file, StandardCharsets.UTF_8);
		return parse(text.startsWith("﻿") ? text.substring(1) : text);
	}

	/** The one key of {@code step}: the step kind. */
	public static String kind(JsonNode step) {
		return step.fieldNames().next();
	}

	/** The value of {@code step}'s only key. */
	public static JsonNode argument(JsonNode step) {
		return step.get(kind(step));
	}

	private static String text(JsonNode root, String field) {
		return root.hasNonNull(field) ? root.get(field).asText() : null;
	}
}
