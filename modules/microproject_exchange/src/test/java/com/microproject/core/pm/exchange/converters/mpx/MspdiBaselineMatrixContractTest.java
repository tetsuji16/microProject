/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.core.pm.exchange.converters.mpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import com.microproject.exchange.MicrosoftImporter;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.TaskSnapshot;

/**
 * Independent fixed-value coverage for baseline fields and assignment timephased types.
 * Microsoft Baseline defines task/assignment scalar and optional timephased fields;
 * Type defines the numeric work/cost identities:
 * https://learn.microsoft.com/en-us/office-project/xml-data-interchange/baseline-element?view=project-client-2016
 * https://learn.microsoft.com/en-us/office-project/xml-data-interchange/type-element-multiple-parents?view=project-client-2016
 */
class MspdiBaselineMatrixContractTest {
	private static final int[] WORK_TYPES = { 4, 16, 22, 28, 34, 40, 46, 52, 58, 64, 70 };
	private static final int[] WORK_HOURS = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 11 };
	private static final int[] TASK_COSTS = { 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110 };
	private static final int[] ASSIGNMENT_COSTS = { 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210 };
	private static final int[] DAY_OFFSETS = { -70, -63, -56, -49, -42, -35, -28, -21, -14, -7, 0 };
	private static final int[] FUTURE_OFFSETS = { 0, 7, 14, 21, 28, 35, 42, 49, 56, 63, 70 };

	@Test
	void allTaskAndAssignmentBaselineSlotsMatchLiteralTableThroughMpoAndMspdi() throws Exception {
		String source = literalBaselineTableXml();
		Project imported = importMspdi(source);
		assertManifest(imported);

		long taskUidBeforeSave = taskNamed(imported).getUniqueId();
		long resourceUidBeforeSave = ((Assignment) taskNamed(imported).getAssignments().iterator().next())
			.getResource().getUniqueId();
		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(imported, mpo));
		Project reopened = new MpoFileImporter().loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		NormalTask reopenedTask = taskNamed(reopened);
		Assignment reopenedAssignment = assignment(reopenedTask);
		assertEquals(taskUidBeforeSave, reopenedTask.getUniqueId(), "persisted task UID");
		assertEquals(resourceUidBeforeSave, reopenedAssignment.getResource().getUniqueId(), "persisted resource UID");
		assertEquals(reopenedTask.getUniqueId(), reopenedAssignment.getTask().getUniqueId(), "assignment canonical task identity");
		assertEquals(reopenedAssignment.getResource().getUniqueId(),
			ResourcePool.findResourceByName("Baseline test resource", reopened.getResourcePool()).getUniqueId(),
			"assignment canonical resource identity after MPO reload");
		assertManifest(reopened);

		ByteArrayOutputStream exported = new ByteArrayOutputStream();
		assertTrue(new MicrosoftImporter().saveProject(reopened, exported));
		String exportedXml = exported.toString(StandardCharsets.UTF_8);
		assertAllTimephasedTypes(exportedXml);
		Project reimported = importMspdi(exportedXml);
		assertManifest(reimported);
	}

	/**
	 * Document-derived compatibility decision: baseline fields preserve the task and
	 * assignment state saved with the baseline, independently of current schedule edits.
	 * This is based on Microsoft's Baseline XML definition and desktop guidance, not an
	 * empirical Microsoft Project run:
	 * https://learn.microsoft.com/en-us/office-project/xml-data-interchange/baseline-element?view=project-client-2016
	 * https://support.microsoft.com/en-us/project/create-or-update-a-baseline-or-an-interim-plan-in-project-desktop
	 */
	@Test
	void futureDatedTimephasedBaselineDoesNotReplaceCurrentTaskOrAssignmentSchedule() throws Exception {
		for (int[] offsets : List.of(FUTURE_OFFSETS, DAY_OFFSETS)) {
			String xml = literalBaselineTableXml(offsets);
			net.sf.mpxj.ProjectFile raw = new net.sf.mpxj.mspdi.MSPDIReader().read(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
			net.sf.mpxj.Task rawTask = raw.getTasks().stream()
				.filter(candidate -> "Assignment baseline task".equals(candidate.getName())).findFirst().orElseThrow();
			net.sf.mpxj.ResourceAssignment rawAssignment = rawTask.getResourceAssignments().get(0);
			assertEquals(localWallDate("2026-10-12T17:00:00"), rawTask.getFinish(), "MPXJ task current finish from literal direct child");
			assertEquals(localWallDate("2026-10-12T08:00:00"), rawAssignment.getStart(), "MPXJ assignment current start");
			assertEquals(localWallDate("2026-10-12T17:00:00"), rawAssignment.getFinish(), "MPXJ assignment current finish");

			TrackingMspImporter importer = new TrackingMspImporter();
			Project imported = importer.importProject(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "xml",
				(progress, label) -> {});
			NormalTask task = taskNamed(imported);
			Assignment currentAssignment = assignment(task);
			TaskSnapshot baselineSnapshot = (TaskSnapshot) task.getSnapshot(Integer.valueOf(10));
			Assignment baselineAssignment = currentAssignment.getBaselineAssignment(Integer.valueOf(10), false);
			assertNotNull(baselineSnapshot);
			assertNotNull(baselineAssignment);
			assertSame(task, currentAssignment.getTask(), "assignment retains its canonical owner task");
			assertNotSame(task.getCurrentSchedule(), baselineSnapshot.getCurrentSchedule(),
				"task current schedule and baseline slot 10 must have independent schedule objects");
			assertNotSame(currentAssignment, baselineAssignment, "current and slot 10 assignment profiles are distinct");
			List<ImportStage> stages = importer.stages();
			assertEquals(List.of("importTasks", "importDependencies", "importProjectHeader"),
				stages.stream().map(ImportStage::name).toList(), "captured importer stage order");
			assertAll("current model state remains independent from baseline intervals",
				() -> assertEquals(wallTime("2026-10-12T08:00:00"), task.getCurrentSchedule().getStart(), "model current task start"),
				() -> assertEquals(wallTime("2026-10-12T17:00:00"), task.getCurrentSchedule().getFinish(), "model current task finish"),
				() -> assertEquals(wallTime("2026-10-12T08:00:00"), currentAssignment.getStart(), "model current assignment start"),
				() -> assertEquals(wallTime("2026-10-12T17:00:00"), currentAssignment.getFinish(), "model current assignment finish"),
				() -> assertEquals(8L * 3_600_000L, currentAssignment.getWork(null), "current assignment work"),
				() -> assertEquals(stages.get(0).constraint(), task.getConstraintTypeKind(), "current task constraint"),
				() -> assertEquals(wallTime(slotStart(10, offsets)), baselineSnapshot.getCurrentSchedule().getStart(), "task baseline slot 10 start"),
				() -> assertEquals(wallTime(slotFinish(10, offsets)), baselineSnapshot.getCurrentSchedule().getFinish(), "task baseline slot 10 finish"),
				() -> assertEquals(wallTime(slotStart(10, offsets)), baselineAssignment.getStart(), "assignment baseline slot 10 start"),
				() -> assertEquals(wallTime(slotFinish(10, offsets)), baselineAssignment.getFinish(), "assignment baseline slot 10 finish"),
				() -> assertEquals(wallTime("2026-10-12T17:00:00"), stages.get(0).currentTaskFinish(), "finish immediately after importTasks"),
				() -> assertEquals(wallTime("2026-10-12T17:00:00"), stages.get(0).currentAssignmentFinish(), "assignment finish immediately after importTasks"),
				() -> assertEquals(8L * 3_600_000L, stages.get(0).currentAssignmentWork(), "current work immediately after importTasks"),
				() -> assertEquals(stages.get(0).constraint(), stages.get(1).constraint(), "constraint after dependency import"),
				() -> assertEquals(stages.get(1).constraint(), stages.get(2).constraint(), "constraint after project header import"),
				() -> assertEquals(stages.get(0).currentTaskFinish(), stages.get(1).currentTaskFinish(), "finish after dependency import"),
				() -> assertEquals(stages.get(1).currentTaskFinish(), stages.get(2).currentTaskFinish(), "finish after project header import"));
		}
	}

	private record ImportStage(String name, long currentTaskFinish, long currentAssignmentFinish, long currentAssignmentWork,
		com.microproject.pm.scheduling.ConstraintType.Kind constraint,
		Object currentTaskSchedule, Object taskBaselineSchedule, Object currentAssignment,
		Object assignmentBaseline, long taskBaselineFinish, long assignmentBaselineFinish) { }

	private static final class TrackingMspImporter extends com.microproject.core.pm.exchange.MspImporter {
		private final java.util.ArrayList<ImportStage> stages = new java.util.ArrayList<>();
		@Override protected void importTasks(Project project) {
			super.importTasks(project);
			capture("importTasks", project);
		}
		@Override protected void importDependencies(Project project) {
			super.importDependencies(project);
			capture("importDependencies", project);
		}
		@Override protected void importProjectHeader(Project project) {
			super.importProjectHeader(project);
			capture("importProjectHeader", project);
		}
		private void capture(String stage, Project project) {
			NormalTask task = taskNamed(project);
			Assignment assignment = assignment(task);
			TaskSnapshot snapshot = (TaskSnapshot) task.getSnapshot(Integer.valueOf(10));
			Assignment baseline = assignment.getBaselineAssignment(Integer.valueOf(10), false);
			stages.add(new ImportStage(stage, task.getCurrentSchedule().getFinish(), assignment.getFinish(),
				assignment.getWork(null), task.getConstraintTypeKind(),
				task.getCurrentSchedule(), snapshot == null ? null : snapshot.getCurrentSchedule(), assignment, baseline,
				snapshot == null ? 0L : snapshot.getCurrentSchedule().getFinish(), baseline == null ? 0L : baseline.getFinish()));
		}
		List<ImportStage> stages() { return List.copyOf(stages); }
	}

	private static void assertManifest(Project project) {
		assertNoBaselineControl(project);
		NormalTask task = taskNamed(project);
		long currentStart = wallTime("2026-10-12T08:00:00");
		long currentFinish = wallTime("2026-10-12T17:00:00");
		assertEquals(currentStart, task.getCurrentSchedule().getStart(), "baseline import must preserve current task start");
		assertEquals(currentFinish, task.getCurrentSchedule().getFinish(), "baseline import must preserve current task finish");
		Assignment assignment = assignment(task);
		assertEquals(wallTime("2026-10-12T08:00:00"), assignment.getStart(), "baseline import must preserve current assignment start");
		assertEquals(wallTime("2026-10-12T17:00:00"), assignment.getFinish(), "baseline import must preserve current assignment finish");
		assertEquals(8L * 3_600_000L, assignment.getWork(null), "baseline import must preserve current assignment work");
		for (int slot = 0; slot <= 10; slot++) {
			String start = slotStart(slot);
			String finish = slotFinish(slot);
			long expectedStart = wallTime(start);
			long expectedFinish = wallTime(finish);
			TaskSnapshot snapshot = (TaskSnapshot) task.getSnapshot(Integer.valueOf(slot));
			assertNotNull(snapshot, "task baseline slot " + slot);
			assertEquals(expectedStart, snapshot.getCurrentSchedule().getStart(), "task baseline start slot " + slot);
			assertEquals(expectedFinish, snapshot.getCurrentSchedule().getFinish(), "task baseline finish slot " + slot);
			assertEquals(16L * 3_600_000L, com.microproject.datatype.Duration.millis(
				snapshot.getCurrentSchedule().getRawDuration()), "task baseline duration slot " + slot);
			assertEquals((double) TASK_COSTS[slot], snapshot.getImportedBaselineCost().doubleValue(),
				"task baseline cost slot " + slot);

			Assignment.BaselineValues scalar = assignment.getBaselineValues(slot);
			assertEquals(Long.valueOf(expectedStart), scalar.start(), "assignment baseline start slot " + slot);
			assertEquals(Long.valueOf(expectedFinish), scalar.finish(), "assignment baseline finish slot " + slot);
			long workMillis = WORK_HOURS[slot] * 3_600_000L;
			assertEquals(Long.valueOf(workMillis), scalar.work(), "assignment scalar work slot " + slot);
			Assignment baseline = assignment.getBaselineAssignment(Integer.valueOf(slot), false);
			assertNotNull(baseline, "assignment timephased baseline slot " + slot);
			assertEquals(workMillis, baseline.getWork(null), "assignment timephased work slot " + slot);
			assertEquals((double) ASSIGNMENT_COSTS[slot], baseline.cost(expectedStart, expectedFinish),
				"assignment timephased cost slot " + slot);
		}
	}

	private static void assertNoBaselineControl(Project project) {
		NormalTask task = project.getTaskList().stream()
			.filter(candidate -> "No baseline control task".equals(candidate.getName()))
			.map(NormalTask.class::cast).findFirst().orElseThrow();
		Assignment assignment = (Assignment) task.getAssignments().iterator().next();
		assertEquals(wallTime("2026-10-12T08:00:00"), task.getCurrentSchedule().getStart(), "no-baseline task current start");
		assertEquals(wallTime("2026-10-12T17:00:00"), task.getCurrentSchedule().getFinish(), "no-baseline task current finish");
		assertEquals(wallTime("2026-10-12T08:00:00"), assignment.getStart(), "no-baseline assignment current start");
		assertEquals(wallTime("2026-10-12T17:00:00"), assignment.getFinish(), "no-baseline assignment current finish");
		for (int slot = 0; slot <= 10; slot++) {
			assertTrue(task.getSnapshot(Integer.valueOf(slot)) == null, "task baseline slot " + slot + " must be absent");
			Assignment.BaselineValues values = assignment.getBaselineValues(slot);
			assertTrue(values == null || (values.start() == null && values.finish() == null && values.work() == null),
				"assignment baseline values must be absent for slot " + slot);
			assertEquals(null, assignment.getBaselineAssignment(Integer.valueOf(slot), false),
				"assignment baseline slot " + slot + " must be absent");
		}
	}

	private static void assertAllTimephasedTypes(String xml) throws Exception {
		javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		org.w3c.dom.Document document = factory.newDocumentBuilder()
			.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		String namespace = document.getDocumentElement().getNamespaceURI();
		org.w3c.dom.NodeList assignments = document.getElementsByTagNameNS(namespace, "Assignment");
		Element baselineAssignment = null;
		for (int index = 0; index < assignments.getLength(); index++) {
			Element candidate = (Element) assignments.item(index);
			if ("1".equals(childText(candidate, "TaskUID", namespace))) baselineAssignment = candidate;
		}
		assertNotNull(baselineAssignment);
		org.w3c.dom.NodeList rows = baselineAssignment.getElementsByTagNameNS(namespace, "TimephasedData");
		Set<Integer> actualTypes = new HashSet<>();
		for (int index = 0; index < rows.getLength(); index++) {
			Element row = (Element) rows.item(index);
			String type = childText(row, "Type", namespace);
			if (type != null) actualTypes.add(Integer.valueOf(type));
		}
		Set<Integer> expected = new HashSet<>();
		for (int slot = 0; slot <= 10; slot++) {
			if (WORK_HOURS[slot] != 0) expected.add(Integer.valueOf(WORK_TYPES[slot]));
			expected.add(Integer.valueOf(WORK_TYPES[slot] + 1));
		}
		assertTrue(actualTypes.containsAll(expected), "exported assignment baseline TimephasedData type IDs: " + actualTypes);
	}

	private static String childText(Element parent, String name, String namespace) {
		org.w3c.dom.NodeList children = parent.getElementsByTagNameNS(namespace, name);
		return children.getLength() == 0 ? null : children.item(0).getTextContent();
	}

	private static NormalTask taskNamed(Project project) {
		return project.getTaskList().stream().filter(task -> "Assignment baseline task".equals(task.getName()))
			.map(NormalTask.class::cast).findFirst().orElseThrow();
	}

	private static Assignment assignment(NormalTask task) {
		return (Assignment) task.getAssignments().iterator().next();
	}

	private static Project importMspdi(String xml) throws Exception {
		return new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "xml", (progress, label) -> {});
	}

	private static String literalBaselineTableXml() throws Exception {
		return literalBaselineTableXml(DAY_OFFSETS);
	}

	private static String literalBaselineTableXml(int[] dayOffsets) throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "Assignment Baseline Scalars.xml");
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "Assignment Baseline Scalars.xml");
		String xml = Files.readString(fixture);
		xml = xml.replaceAll("(?s)<Baseline>.*?</Baseline>", "");
		int taskEnd = xml.indexOf("</Task>");
		int assignmentEnd = xml.indexOf("</Assignment>");
		assertTrue(taskEnd >= 0 && assignmentEnd >= 0, "fixed standard-calendar input fixture shape");
		StringBuilder taskBaselines = new StringBuilder();
		StringBuilder assignmentBaselines = new StringBuilder();
		for (int slot = 0; slot <= 10; slot++) {
			String start = slotStart(slot, dayOffsets);
			String finish = slotFinish(slot, dayOffsets);
			int xmlTaskCost = TASK_COSTS[slot] * 100;
			int xmlAssignmentCost = ASSIGNMENT_COSTS[slot] * 100;
			taskBaselines.append("<Baseline><Number>").append(slot).append("</Number><Start>")
				.append(start).append("</Start><Finish>").append(finish)
				.append("</Finish><Duration>PT16H0M0S</Duration><DurationFormat>5</DurationFormat><Cost>")
				.append(xmlTaskCost).append("</Cost></Baseline>");
			assignmentBaselines.append("<Baseline><Number>").append(slot).append("</Number><Start>")
				.append(start).append("</Start><Finish>").append(finish).append("</Finish><Work>")
				.append("PT").append(WORK_HOURS[slot]).append("H0M0S</Work>")
				.append("<TimephasedData><Type>").append(WORK_TYPES[slot]).append("</Type><UID>1</UID><Start>")
				.append(start).append("</Start><Finish>").append(finish)
				.append("</Finish><Unit>2</Unit><Value>PT").append(WORK_HOURS[slot])
				.append("H0M0S</Value></TimephasedData><TimephasedData><Type>")
				.append(WORK_TYPES[slot] + 1).append("</Type><UID>1</UID><Start>").append(start)
				.append("</Start><Finish>").append(finish).append("</Finish><Unit>2</Unit><Value>")
				.append(xmlAssignmentCost).append("</Value></TimephasedData></Baseline>");
		}
		xml = xml.substring(0, taskEnd) + taskBaselines + xml.substring(taskEnd);
		String controlTask = "<Task><UID>2</UID><ID>2</ID><Name>No baseline control task</Name><Type>0</Type>"
			+ "<IsNull>0</IsNull><OutlineLevel>1</OutlineLevel><Priority>500</Priority>"
			+ "<Start>2026-10-12T08:00:00</Start><Finish>2026-10-12T17:00:00</Finish>"
			+ "<Duration>PT8H0M0S</Duration><DurationFormat>7</DurationFormat><Work>PT8H0M0S</Work>"
			+ "<PercentComplete>0</PercentComplete><PercentWorkComplete>0</PercentWorkComplete>"
			+ "<Summary>0</Summary><Milestone>0</Milestone><CalendarUID>1</CalendarUID></Task>";
		int tasksEnd = xml.indexOf("</Tasks>");
		xml = xml.substring(0, tasksEnd) + controlTask + xml.substring(tasksEnd);
		assignmentEnd = xml.indexOf("</Assignment>");
		String controlledAssignment = "<Assignment><UID>2</UID><TaskUID>2</TaskUID><ResourceUID>1</ResourceUID>"
			+ "<Start>2026-10-12T08:00:00</Start><Finish>2026-10-12T17:00:00</Finish>"
			+ "<Work>PT8H0M0S</Work><Units>1</Units></Assignment>";
		String withBaselines = xml.substring(0, assignmentEnd) + assignmentBaselines + xml.substring(assignmentEnd);
		int assignmentsEnd = withBaselines.indexOf("</Assignments>");
		return withBaselines.substring(0, assignmentsEnd) + controlledAssignment + withBaselines.substring(assignmentsEnd);
	}

	private static String slotStart(int slot) {
		return slotStart(slot, DAY_OFFSETS);
	}

	private static String slotStart(int slot, int[] offsets) {
		return LocalDate.parse("2026-10-05").plusDays(offsets[slot]) + "T08:00:00";
	}

	private static String slotFinish(int slot) {
		return slotFinish(slot, DAY_OFFSETS);
	}

	private static String slotFinish(int slot, int[] offsets) {
		return LocalDate.parse("2026-10-06").plusDays(offsets[slot]) + "T17:00:00";
	}

	private static java.util.Date localWallDate(String value) {
		return java.util.Date.from(java.time.LocalDateTime.parse(value)
			.atZone(java.time.ZoneId.systemDefault()).toInstant());
	}

	private static long wallTime(String value) {
		return java.time.LocalDateTime.parse(value).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
	}
}
