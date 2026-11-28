/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport.scenario;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.model.dependency.DependencyService;
import com.microproject.model.dependency.DependencyType;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.model.task.ProjectFactory;
import com.microproject.node.Node;
import com.microproject.node.model.NodeModel;
import com.microproject.options.CalendarOption;
import com.microproject.undo.DataFactoryUndoController;

/** Builds the project a scenario's {@code fixture} describes, without touching the checked-in samples. */
final class GuiScenarioFixtures {
	/** The default project start of an in-memory fixture: a Monday morning, as GMT-read milliseconds. */
	static final String DEFAULT_START = "2026-01-05T08:00";

	private GuiScenarioFixtures() {
	}

	/** Parses an ISO date or date-time as UTC and returns the GMT-read milliseconds (ADR-0003). */
	static long millis(String iso) {
		LocalDateTime time = iso.contains("T") ? LocalDateTime.parse(iso) : LocalDate.parse(iso).atStartOfDay();
		return time.toInstant(ZoneOffset.UTC).toEpochMilli();
	}

	/**
	 * Opens the fixture. A {@code file} fixture copies the repository file into {@code workDirectory}
	 * first, so the original is never rewritten.
	 */
	static Project build(JsonNode fixture, Path workDirectory) throws Exception {
		if (fixture.hasNonNull("file")) return fromFile(fixture.get("file").asText(), workDirectory);
		if (fixture.has("tasks")) return inMemory(fixture);
		throw new IllegalArgumentException("a fixture needs \"file\" or \"tasks\": " + fixture);
	}

	private static Project fromFile(String repositoryPath, Path workDirectory) throws Exception {
		Path source = repositoryRoot().resolve(repositoryPath).normalize();
		if (!Files.isRegularFile(source)) throw new IOException("fixture file does not exist: " + source);
		Path copy = workDirectory.resolve(source.getFileName().toString());
		Files.copy(source, copy, StandardCopyOption.REPLACE_EXISTING);
		MpoFileImporter importer = new MpoFileImporter();
		importer.setFileName(copy.toString());
		importer.setProjectFactory(ProjectFactory.getInstance());
		importer.importFile();
		return importer.getProject();
	}

	private static Path repositoryRoot() {
		String configured = System.getProperty("microproject.project.dir");
		if (configured != null && !configured.isBlank()) return Path.of(configured);
		File directory = new File("").getAbsoluteFile();
		for (; directory != null; directory = directory.getParentFile()) {
			if (new File(directory, "samples").isDirectory()) return directory.toPath();
		}
		throw new IllegalStateException("cannot locate the repository root; set microproject.project.dir");
	}

	private static Project inMemory(JsonNode fixture) throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("gui-scenario", undo), undo);
		project.initialize(false, false);
		long day = CalendarOption.getInstance().getMillisPerDay();
		NodeModel model = project.getTaskModel();
		Map<String, NormalTask> byName = new HashMap<>();
		List<NormalTask> created = new ArrayList<>();
		List<Integer> levels = new ArrayList<>();
		for (JsonNode spec : fixture.get("tasks")) {
			NormalTask task = project.createScriptedTask();
			task.setName(spec.get("name").asText());
			project.connectTask(task);
			project.getTaskOutlines().addToAll(task, null);
			task.setDuration(Math.round(spec.path("durationDays").asDouble(1) * day));
			byName.put(task.getName(), task);
			created.add(task);
			levels.add(spec.path("outlineLevel").asInt(1));
		}
		// Indent after every task exists, in order, so each task finds its previous sibling.
		for (int index = 0; index < created.size(); index++) {
			for (int step = 1; step < levels.get(index); step++) {
				model.indent(List.of((Node) model.search(created.get(index))), 1, NodeModel.SILENT);
			}
		}
		for (JsonNode link : fixture.path("links")) {
			NormalTask from = byName.get(link.get("from").asText());
			NormalTask to = byName.get(link.get("to").asText());
			if (from == null || to == null) throw new IllegalArgumentException("fixture link names an unknown task: " + link);
			DependencyType.Kind kind = DependencyType.Kind.valueOf(link.path("type").asText("FS"));
			DependencyService.getInstance().newDependency(from, to, kind, 0L, project);
		}
		project.setStartDate(millis(fixture.path("start").asText(DEFAULT_START)));
		project.recalculate();
		// Building the fixture is not something a scenario can undo.
		project.getUndoController().clear();
		return project;
	}
}
