/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.datatype.Duration;
import com.microproject.datatype.TimeUnit;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;
import com.microproject.pm.calendar.WorkingCalendar;

class MpoHistoricalFixtureTest {
	private static final String FIXTURE = "ccpm-sample-previous-release.mpo";
	private static final String FIXED_FIXTURE = "fixed-schedule-previous-writer.mpo";
	private static final String V024_FIXTURE = "v024-1265-previous-writer.mpo";
	private static final long HOUR = 60L * 60L * 1000L;

	@AfterEach
	void closeOwnedExtractionSessions() {
		MpoExtractionOwnershipRegistry.closeAll();
	}

	@Test
	void priorReleaseMpoFixturePreservesSelectedFieldsAndReopens() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "mpo", FIXTURE);
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "mpo", FIXTURE);

		Project imported = load(Files.readAllBytes(fixture));
		assertProjectSemantics(imported);

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(imported, saved));
		assertProjectSemantics(load(saved.toByteArray()));
	}

	@Test
	void fixedSchedulePreviousWriterFixturePreservesLiteralScheduleAndWorkAfterReopen() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "mpo", FIXED_FIXTURE);
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "mpo", FIXED_FIXTURE);

		Project imported = load(Files.readAllBytes(fixture));
		assertFixedScheduleSemantics(imported);

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(imported, saved));
		assertFixedScheduleSemantics(load(saved.toByteArray()));
	}

	@Test
	void v024TaggedWriterFixturePreservesLiteralNativePayloadsAndReopens() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "mpo", V024_FIXTURE);
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "mpo", V024_FIXTURE);
		byte[] archive = Files.readAllBytes(fixture);
		assertEquals("0CBB233D8836B472F80FBC8932CAE99CC9858670A61AC1F4DC70E31B7C1DFDD8",
			sha256(archive));
		Map<String, byte[]> entries = readEntries(archive);
		assertEquals(List.of("mimetype", "META-INF/manifest.xml", "meta.xml", "content.xml", "settings.xml",
			"layout.json", "operations/log.jsonl", "changes/task-identities.json", "microproject/visibility.json",
			"microproject/task-splits.json", "microproject/task-leveling-options.json",
			"microproject/task-text-styles.json", "microproject/task-schedules.json",
			"microproject/baseline-profiles.json", "microproject/assignment-leveling-splits.json",
			"ccpm/history.jsonl"), List.copyOf(entries.keySet()));
		assertEquals("DDB6A49BD2F7D5CA2E6432FE29B5711D08E8DA406EDB43664E9E4C29C1E34A7A",
			sha256(entries.get("content.xml")));
		assertTrue(new String(entries.get("meta.xml"), java.nio.charset.StandardCharsets.UTF_8)
			.contains("applicationVersion=\"0.0.24\""));
		var json = new com.fasterxml.jackson.databind.ObjectMapper();
		var layout = json.readTree(entries.get("layout.json")).path("fields");
		assertEquals(List.of("Field.name", "Field.duration", "Field.percentComplete"),
			java.util.stream.StreamSupport.stream(layout.spliterator(), false)
				.map(field -> field.path("id").textValue()).toList());
		assertEquals(240, layout.get(0).path("width").intValue());
		assertTrue(layout.get(0).path("manual").booleanValue());
		assertEquals(4201L, json.readTree(entries.get("microproject/visibility.json"))
			.path("hiddenTaskUniqueIds").get(0).longValue());
		var split = json.readTree(entries.get("microproject/task-splits.json")).path("tasks").get(0);
		assertEquals(4202L, split.path("taskUid").longValue());
		assertEquals(14_400_000L, split.path("manualIntervals").get(0).path("startOffset").longValue());
		assertEquals(18_000_000L, split.path("manualIntervals").get(0).path("endOffset").longValue());
		var leveling = json.readTree(entries.get("microproject/task-leveling-options.json")).path("tasks").get(0);
		assertEquals(4202L, leveling.path("taskUid").longValue());
		assertFalse(leveling.path("levelingCanSplit").booleanValue());
		assertFalse(leveling.path("levelAssignments").booleanValue());
		var style = json.readTree(entries.get("microproject/task-text-styles.json")).path("tasks").get(0);
		assertEquals("Dialog", style.path("fontFamily").textValue());
		assertEquals(15, style.path("fontSize").intValue());
		assertEquals(0x2457A6, style.path("fontColor").intValue());
		assertTrue(style.path("bold").booleanValue() && style.path("italic").booleanValue()
			&& style.path("strikethrough").booleanValue());
		var baseline = json.readTree(entries.get("microproject/baseline-profiles.json")).path("tasks").get(0);
		assertEquals(4202L, baseline.path("taskUid").longValue());
		assertEquals(0, baseline.path("slot").intValue());
		assertEquals(modelTime("2030-06-04T08:00:00"), baseline.path("profiles").get(0).path("start").longValue());
		assertEquals(modelTime("2030-06-04T17:00:00"), baseline.path("profiles").get(0).path("finish").longValue());
		assertEquals(7L, baseline.path("profiles").get(0).path("resourceUid").longValue());
		assertEquals(28_800_000L, baseline.path("profiles").get(0).path("duration").longValue());
		assertEquals(1.0D, baseline.path("profiles").get(0).path("units").doubleValue(), 0.00001D);
		assertEquals(28_800_000L, baseline.path("profiles").get(0).path("workContour").path("buckets")
			.get(0).path("duration").longValue());
		assertEquals("Fixture Mon-Fri 08:00-12:00 13:00-17:00",
			baseline.path("profiles").get(0).path("calendar").path("name").textValue());
		assertTrue(json.readTree(entries.get("microproject/task-schedules.json")).path("projectCalendar")
			.path("week").get(1).path("ranges").get(0).path("start").longValue() == 28_800_000L);
		assertEquals(2, json.readTree(entries.get("microproject/assignment-leveling-splits.json"))
			.path("version").intValue());

		Project imported = load(archive);
		assertFixedScheduleSemantics(imported);
		assertNativeSemantics(imported);
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(imported, saved));
		Project reopened = load(saved.toByteArray());
		assertFixedScheduleSemantics(reopened);
		assertNativeSemantics(reopened);
	}

	@Test
	void v024TaggedWriterFixtureUsesSourceCalendarWhenGlobalDefaultsChange() throws Exception {
		// Document-derived contract: MSPDI CalendarUID references a valid UID within
		// this project, and UID uniqueness is project-scoped. Resolve this source
		// calendar by identity before consulting process-global defaults.
		// https://learn.microsoft.com/en-us/office-project/xml-data-interchange/calendaruid-element?view=project-client-2016
		// https://learn.microsoft.com/en-us/office-project/xml-data-interchange/uid-element?view=project-client-2016
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "mpo", V024_FIXTURE);
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "mpo", V024_FIXTURE);
		WorkingCalendar standard = CalendarService.getInstance().getStandardInstance();
		WorkingCalendar defaultCalendar = CalendarService.getInstance().getDefaultInstance();
		WorkingCalendar originalStandard = standard.makeScratchCopy();
		WorkingCalendar originalDefault = defaultCalendar.makeScratchCopy();
		assertNotSame(standard, defaultCalendar,
			"the standard base and named default calendars are separate global objects");
		try {
			setNonWorkingWeek(standard);
			setNonWorkingWeek(defaultCalendar);
			Project imported = load(Files.readAllBytes(fixture));
			assertFixedScheduleSemantics(imported);
			assertNativeSemantics(imported);
		} finally {
			restoreCalendar(standard, originalStandard);
			restoreCalendar(defaultCalendar, originalDefault);
		}
	}

	@Test
	void missingTaskScheduleEntryAndCalendarFieldsFallBackToMspdi() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "mpo", V024_FIXTURE);
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "mpo", V024_FIXTURE);
		Map<String, byte[]> original = readEntries(Files.readAllBytes(fixture));
		Map<String, byte[]> missingEntry = new LinkedHashMap<>(original);
		missingEntry.remove("microproject/task-schedules.json");
		String manifest = new String(missingEntry.get("META-INF/manifest.xml"),
			java.nio.charset.StandardCharsets.UTF_8);
		manifest = manifest.replaceAll("(?m)\\s*<entry path=\"microproject/task-schedules\\.json\"[^>]*/>", "");
		missingEntry.put("META-INF/manifest.xml", manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8));

		// Synthetic probes: one removes the optional entry; the other keeps v1
		// schedule values but omits only its calendar definitions/references.
		assertFixedScheduleSemantics(load(writeEntries(missingEntry)));
		Map<String, byte[]> missingCalendarFields = new LinkedHashMap<>(original);
		var json = new com.fasterxml.jackson.databind.ObjectMapper();
		var schedule = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(
			missingCalendarFields.get("microproject/task-schedules.json"));
		schedule.remove("projectCalendar");
		for (var task : schedule.withArray("tasks")) ((com.fasterxml.jackson.databind.node.ObjectNode) task).remove("calendar");
		byte[] scheduleBytes = json.writeValueAsBytes(schedule);
		missingCalendarFields.put("microproject/task-schedules.json", scheduleBytes);
		manifest = new String(missingCalendarFields.get("META-INF/manifest.xml"),
			java.nio.charset.StandardCharsets.UTF_8);
		manifest = manifest.replaceAll("(<entry path=\"microproject/task-schedules\\.json\" sha256=\")[^\"]*(\"/>)",
			"$1" + sha256(scheduleBytes).toLowerCase(java.util.Locale.ROOT) + "$2");
		missingCalendarFields.put("META-INF/manifest.xml", manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		assertFixedScheduleSemantics(load(writeEntries(missingCalendarFields)));
	}

	private static Project load(byte[] bytes) throws Exception {
		MpoFileImporter importer = new MpoFileImporter();
		importer.setProjectFactory(ProjectFactory.getInstance());
		Project project = importer.loadProject(new ByteArrayInputStream(bytes));
		assertNotNull(project);
		return project;
	}

	private static Map<String, byte[]> readEntries(byte[] archive) throws Exception {
		Map<String, byte[]> entries = new LinkedHashMap<>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) entries.put(entry.getName(), zip.readAllBytes());
		}
		return entries;
	}

	private static String sha256(byte[] bytes) throws Exception {
		return java.util.HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}

	private static void setNonWorkingWeek(WorkingCalendar calendar) {
		for (int day = 0; day < 7; day++) {
			WorkDay nonWorking = new WorkDay();
			nonWorking.getWorkingHours().setNonWorking();
			calendar.setWeekDay(day, nonWorking);
		}
	}

	private static void restoreCalendar(WorkingCalendar calendar, WorkingCalendar snapshot) {
		calendar.assignFrom(snapshot);
	}

	private static byte[] writeEntries(Map<String, byte[]> entries) throws Exception {
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(archive, java.nio.charset.StandardCharsets.UTF_8)) {
			for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
				zip.putNextEntry(new ZipEntry(entry.getKey()));
				zip.write(entry.getValue());
				zip.closeEntry();
			}
		}
		return archive.toByteArray();
	}

	private static void assertNativeSemantics(Project project) {
		NormalTask predecessor = (NormalTask) taskByUid(project, 4201L);
		NormalTask successor = (NormalTask) taskByUid(project, 4202L);
		assertTrue(predecessor.isHiddenTask());
		assertEquals("Dialog", successor.getFontFamily());
		assertEquals(15, successor.getFontSize());
		assertEquals(Integer.valueOf(0x2457A6), successor.getFontColor());
		assertTrue(successor.isFontBold() && successor.isFontItalic() && successor.isFontStrikethrough());
		assertFalse(successor.isLevelingCanSplit());
		assertFalse(successor.isLevelAssignments());
		assertEquals(List.of(new com.microproject.pm.task.TaskSplitInterval(14_400_000L, 18_000_000L)),
			successor.getManualTaskSplitIntervals());
		com.microproject.pm.task.TaskSnapshot snapshot = (com.microproject.pm.task.TaskSnapshot) successor
			.getSnapshot(Integer.valueOf(com.microproject.pm.snapshot.Snapshottable.BASELINE));
		assertNotNull(snapshot);
		var profiles = snapshot.captureBaselineProfiles(successor, com.microproject.pm.snapshot.Snapshottable.BASELINE);
		assertEquals(1, profiles.size());
		var profile = profiles.get(0);
		assertEquals(21L, profile.profileId());
		assertEquals(7L, profile.resourceUniqueId());
		assertEquals(modelTime("2030-06-04T08:00:00"), profile.start());
		assertEquals(modelTime("2030-06-04T17:00:00"), profile.finish());
		assertEquals(28_800_000L, profile.duration());
		assertEquals(1.0D, profile.units(), 0.00001D);
		assertEquals(List.of(new com.microproject.pm.assignment.BaselineAssignmentProfile.Bucket(28_800_000L, 1.0D)),
			profile.workContour().buckets());
		var baselineCalendar = (com.microproject.pm.calendar.WorkingCalendar) profile.calendarCopy();
		assertEquals("Fixture Mon-Fri 08:00-12:00 13:00-17:00", baselineCalendar.getName());
		assertEquals(com.microproject.pm.calendar.WorkingHours.hourTime(8),
			baselineCalendar.getWeekDay(2).getWorkingHours().getInterval(0).getStart());
		assertEquals(com.microproject.pm.calendar.WorkingHours.hourTime(17),
			baselineCalendar.getWeekDay(2).getWorkingHours().getInterval(1).getEnd());
	}

	private static void assertProjectSemantics(Project project) {
		assertEquals("CCPM Visualization Sample", project.getName());
		assertEquals(8, project.getTaskList().size());
		List<Task> tasks = project.getTaskList();
		Task phaseTwo = taskNamed(tasks, "Phase 2: Design and Verification");
		NormalTask phaseTwoNormal = (NormalTask) phaseTwo;
		long encodedDuration = phaseTwoNormal.getRawDuration();
		assertEquals(TimeUnit.DAYS, Duration.getType(encodedDuration));
		assertEquals(34L * HOUR, Duration.millis(encodedDuration));
		assertEquals(0.25D, phaseTwo.getPercentComplete(), 0.00001D);

		Assignment assignment = (Assignment) ((NormalTask) phaseTwo).getAssignments().get(0);
		assertEquals(34L * HOUR, assignment.getWork(null));
		assertEquals(32L * HOUR, assignment.getActualWork(null));
		assertEquals(2L * HOUR, assignment.getRemainingWork(null));
		assertEquals(1.0D, assignment.getUnits(), 0.00001D);

		Task phaseFive = taskNamed(tasks, "Phase 5: Design and Verification");
		assertEquals(1, phaseFive.getPredecessorList().size());
		Dependency predecessor = (Dependency) phaseFive.getPredecessorList().iterator().next();
		assertEquals("Phase 4: Design and Verification", ((Task) predecessor.getPredecessor()).getName());
		assertEquals(DependencyType.Kind.FS, predecessor.getDependencyKind());
		assertEquals(0L, predecessor.getLag());
	}

	private static void assertFixedScheduleSemantics(Project project) {
		long monday0800 = modelTime("2030-06-03T08:00:00");
		long monday1700 = modelTime("2030-06-03T17:00:00");
		long tuesday0800 = modelTime("2030-06-04T08:00:00");
		long tuesday1700 = modelTime("2030-06-04T17:00:00");
		long eightHours = 8L * HOUR;
		assertEquals("Fixed schedule native fixture", project.getName());
		assertEquals(monday0800, project.getStart());
		assertEquals(tuesday1700, project.getEnd());
		assertEquals(42L, project.getWorkCalendar().getUniqueId());
		assertEquals(2, project.getTaskList().size());

		NormalTask predecessor = (NormalTask) taskByUid(project, 4201L);
		assertEquals(1, predecessor.getId());
		assertEquals("Predecessor — 8h", predecessor.getName());
		assertEquals(monday0800, predecessor.getStart());
		assertEquals(monday1700, predecessor.getEnd());
		assertEquals(eightHours, predecessor.getDuration());
		assertEquals(eightHours, Duration.millis(predecessor.getRawDuration()));
		assertEquals(0.0D, predecessor.getPercentComplete(), 0.00001D);
		assertEquals(com.microproject.pm.scheduling.ConstraintType.SNET, predecessor.getConstraintType());
		assertEquals(monday0800, predecessor.getConstraintDate());
		assertFalse(predecessor.isManuallyScheduled());
		assertEquals(42L, predecessor.getWorkCalendar().getUniqueId());
		assertSame(project.getWorkCalendar(), predecessor.getWorkCalendar(),
			"previous-writer tasks sharing the project calendar must retain one calendar instance");
		assertFixtureAssignment(predecessor, 0L, 8L, 8L);

		NormalTask successor = (NormalTask) taskByUid(project, 4202L);
		assertEquals(2, successor.getId());
		assertEquals("Successor — partial progress", successor.getName());
		assertEquals(tuesday0800, successor.getStart());
		assertEquals(tuesday1700, successor.getEnd());
		assertEquals(eightHours, successor.getDuration());
		assertEquals(eightHours, Duration.millis(successor.getRawDuration()));
		assertEquals(0.25D, successor.getPercentComplete(), 0.00001D);
		assertEquals(com.microproject.pm.scheduling.ConstraintType.SNET, successor.getConstraintType());
		assertEquals(tuesday0800, successor.getConstraintDate());
		assertFalse(successor.isManuallyScheduled());
		assertEquals(42L, successor.getWorkCalendar().getUniqueId());
		assertSame(project.getWorkCalendar(), successor.getWorkCalendar(),
			"previous-writer tasks sharing the project calendar must retain one calendar instance");
		assertFixtureAssignment(successor, 2L, 6L, 8L);

		assertEquals(1, successor.getPredecessorList().size());
		Dependency predecessorLink = (Dependency) successor.getPredecessorList().iterator().next();
		assertEquals(4201L, ((Task) predecessorLink.getPredecessor()).getUniqueId());
		assertEquals(DependencyType.Kind.FS, predecessorLink.getDependencyKind());
		assertEquals(0L, predecessorLink.getLag());
	}

	private static long modelTime(String floatingWallTime) {
		// MSPDI dates are floating wall times. The model stores the same wall
		// components in a UTC-like epoch so expectations stay host-zone neutral.
		return LocalDateTime.parse(floatingWallTime).toInstant(ZoneOffset.UTC).toEpochMilli();
	}

	private static Task taskByUid(Project project, long uniqueId) {
		for (Task task : project.getTaskList())
			if (task.getUniqueId() == uniqueId) return task;
		throw new AssertionError("Missing task UID " + uniqueId);
	}

	private static void assertFixtureAssignment(NormalTask task, long actualHours, long remainingHours,
		long workHours) {
		assertEquals(1, task.getAssignments().size());
		Assignment assignment = (Assignment) task.getAssignments().get(0);
		assertEquals(7L, assignment.getResource().getUniqueId());
		assertEquals("Fixture resource", assignment.getResource().getName());
		assertEquals(0.5D, assignment.getResource().getMaximumUnits(), 0.00001D);
		assertEquals(workHours * HOUR, assignment.getWork(null));
		assertEquals(actualHours * HOUR, assignment.getActualWork(null));
		assertEquals(remainingHours * HOUR, assignment.getRemainingWork(null));
		assertEquals(1.0D, assignment.getUnits(), 0.00001D);
	}

	private static Task taskNamed(List<Task> tasks, String name) {
		return tasks.stream().filter(task -> name.equals(task.getName())).findFirst()
			.orElseThrow(() -> new AssertionError("Missing task from historical MPO fixture: " + name));
	}

}
