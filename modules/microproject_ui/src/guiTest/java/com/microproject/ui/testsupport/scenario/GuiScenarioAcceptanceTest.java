/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport.scenario;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Runs every {@code *.json} scenario in {@code src/guiTest/resources/scenarios} (or in the
 * directory passed as {@code -PguiScenarioDir}) as its own test, in file-name order. Each scenario
 * is a separate invocation so it gets its own run record under the GUI artifacts directory.
 */
class GuiScenarioAcceptanceTest {
	static final String SCENARIO_DIRECTORY_PROPERTY = "microproject.gui.scenarioDir";

	static Stream<Arguments> scenarios() throws IOException, URISyntaxException {
		List<Path> files = new ArrayList<>();
		String configured = System.getProperty(SCENARIO_DIRECTORY_PROPERTY);
		if (configured != null && !configured.isBlank()) {
			collect(Path.of(configured), files);
		} else {
			for (URL url : Collections.list(GuiScenarioAcceptanceTest.class.getClassLoader().getResources("scenarios"))) {
				if ("file".equals(url.getProtocol())) collect(Path.of(url.toURI()), files);
			}
		}
		if (files.isEmpty()) throw new IllegalStateException("no scenario *.json files were found");
		files.sort(Comparator.comparing(file -> file.getFileName().toString()));
		return files.stream().map(file -> Arguments.of(file.getFileName().toString(), file));
	}

	private static void collect(Path directory, List<Path> files) throws IOException {
		if (!Files.isDirectory(directory)) throw new IllegalStateException("scenario directory does not exist: " + directory);
		try (Stream<Path> children = Files.list(directory)) {
			children.filter(file -> file.getFileName().toString().endsWith(".json")).forEach(files::add);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("scenarios")
	void scenarioHolds(String fileName, Path file) throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		GuiScenarioRunner.run(GuiScenario.load(file));
	}
}
