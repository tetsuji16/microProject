/*
 * MIT License
 *
 * Copyright (c) 2026 microProject
 */
package com.microproject.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class UiInputArchitectureTest {
	@Test
	void productionCommandsDoNotUseKeyListenersOrDeprecatedKeyboardRegistration() throws Exception {
		Path sources = repositoryRoot().resolve("modules/micrproject_ui/src/main/java/com/microproject");
		try (var files = Files.walk(sources)) {
			List<Path> violations = files.filter(path -> path.toString().endsWith(".java"))
					.filter(path -> containsAny(path, "addKeyListener(", "new KeyListener(",
							"new KeyAdapter(", "registerKeyboardAction("))
					.toList();
			assertTrue(violations.isEmpty(), () -> "Use InputMap/ActionMap or DocumentListener: " + violations);
		}
	}

	@Test
	void flamingoIsTheOnlyRibbonImplementation() throws Exception {
		Path ribbon = repositoryRoot().resolve("modules/micrproject_ui/src/main/java/com/microproject/ui/ribbon");
		for (String legacy : List.of("ModernRibbonPanel.java", "SwingRibbonFactory.java", "SwingRibbonModel.java",
				"RibbonButtonStyler.java", "RibbonCommandCatalog.java", "RibbonIconRegistry.java"))
			assertFalse(Files.exists(ribbon.resolve(legacy)), legacy);
		Path adapter = ribbon.resolve("FlamingoRibbonPanel.java");
		assertTrue(Files.exists(adapter));
		assertTrue(Files.readString(adapter).contains("new JRibbon()"));
	}

	@Test
	void wheelRoutingHasNoGraphOwnerRecursiveRegistrationOrEventIdentityDedupe() throws Exception {
		Path root = repositoryRoot();
		String interactor = Files.readString(root.resolve(
				"modules/micrproject_ui/src/main/java/com/microproject/pm/graphic/graph/GraphInteractor.java"));
		String synchronizer = Files.readString(root.resolve(
				"modules/micrproject_ui/src/main/java/com/microproject/pm/graphic/views/synchro/ScrollPaneSynchronizer.java"));
		assertFalse(interactor.contains("MouseWheelListener"));
		assertFalse(interactor.contains("mouseWheelMoved"));
		assertFalse(interactor.contains("getGraphics()"));
		assertFalse(interactor.contains("setXORMode"));
		assertFalse(synchronizer.contains("registerMouseWheelTargets"));
		assertFalse(synchronizer.contains("LastWheelEvent"));
	}

	@Test
	void focusedComponentsDoNotCompeteWithTheDocumentShortcutLayer() throws Exception {
		Path root = repositoryRoot();
		String gantt = Files.readString(root.resolve(
				"modules/micrproject_ui/src/main/java/com/microproject/pm/graphic/gantt/Gantt.java"));
		String commonSheet = Files.readString(root.resolve(
				"modules/micrproject_ui/src/main/java/com/microproject/pm/graphic/spreadsheet/common/CommonSpreadSheet.java"));
		assertFalse(gantt.contains("getInputMap(WHEN_IN_FOCUSED_WINDOW)"));
		assertFalse(commonSheet.contains("handleHierarchyNavigationKeyEvent"));
	}

	private static boolean containsAny(Path path, String... needles) {
		try {
			String source = Files.readString(path);
			return java.util.Arrays.stream(needles).anyMatch(source::contains);
		} catch (IOException exception) {
			throw new java.io.UncheckedIOException(exception);
		}
	}

	private static Path repositoryRoot() {
		for (Path current = Path.of("").toAbsolutePath(); current != null; current = current.getParent())
			if (Files.exists(current.resolve("settings.gradle.kts"))) return current;
		throw new IllegalStateException("repository root not found");
	}
}
