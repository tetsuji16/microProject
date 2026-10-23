/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectStreamClass;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.ccpm.CriticalChainBufferHistory;
import com.microproject.collaboration.CollaborationSession;
import com.microproject.collaboration.OperationLog;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.BaselineRollupOptions;
import com.microproject.pm.task.TaskSnapshot;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.TaskSplitInterval;
import com.microproject.pm.task.TaskModeService;
import com.microproject.pm.task.TaskProgressService;
import com.microproject.pm.task.RespectLinksService;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.task.ScheduleDiagnosticsService;
import com.microproject.pm.task.UpdateProjectRequest;
import com.microproject.command.UpdateProjectCommand;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceType;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.resource.SharedResourcePoolService;
import com.microproject.pm.resource.TeamPlannerService;
import com.microproject.pm.resource.ResourceLevelingService;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.configuration.FieldDictionary;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MpoFileImporterTest {
	@Test
	void taskSheetDurationEditSurvivesMpoSaveReloadAndRecalculate() throws Exception {
		Project project = projectForRoundTrip();
		project.setStartDate(java.time.Instant.parse("2030-06-03T08:00:00Z").toEpochMilli());
		NormalTask predecessor = (NormalTask) firstTask(project);
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		predecessor.setName("Task Sheet duration predecessor");
		successor.setName("Task Sheet duration successor");
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(day);
		successor.setDuration(day);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		project.recalculate();
		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		long start = predecessor.getStart();
		long newDuration = 2L * day;
		com.microproject.field.FieldContext context = new com.microproject.field.FieldContext();
		context.setTaskSheetUpdate(true);
		predecessor.setDuration(newDuration, context);
		long finish = predecessor.getEnd();
		long successorStart = successor.getStart();

		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) reopened.findByUniqueId(predecessorId);
		NormalTask restoredSuccessor = (NormalTask) reopened.findByUniqueId(successorId);
		assertNotNull(restoredPredecessor, "predecessor ID " + predecessorId + " missing after MPO reload; outline="
			+ taskOutlineIdentitySummary(reopened));
		assertNotNull(restoredSuccessor, "successor ID " + successorId + " missing after MPO reload; outline="
			+ taskOutlineIdentitySummary(reopened));
		assertEquals(newDuration, restoredPredecessor.getDuration());
		assertEquals(start, restoredPredecessor.getStart());
		assertEquals(finish, restoredPredecessor.getEnd());
		assertEquals(successorStart, restoredSuccessor.getStart());
		assertTrue(restoredSuccessor.getStart() >= restoredPredecessor.getEnd());
		assertEquals(List.of(restoredSuccessor), restoredPredecessor.getSuccessorList().stream()
			.map(association -> (Task) ((Dependency) association).getSuccessor()).toList());
	}

	@Test
	void mpoRoundTripPreservesFixedDateSsFiftyPercentDurationLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long mondayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 13);
		long tuesdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 13);
		long halfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, 0.5F));
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO percent SS predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO percent SS successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO percent SS unrelated");
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(mondayEight, mondaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SS, halfDurationPercent, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO percent SS predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO percent SS successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO percent SS unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(mondayEight, restoredPredecessor.getStart());
		assertEquals(mondaySeventeen, restoredPredecessor.getEnd());
		assertEquals(mondayThirteen, restoredSuccessor.getStart());
		assertEquals(tuesdayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.SS, restoredDependency.getDependencyType());
		assertEquals(halfDurationPercent, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(restoredDependency.getLag()));
		assertEquals(0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(restoredDependency.getLag()));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateSsNegativeFiftyPercentDurationLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondayTwelve = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 12);
		long mondayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 13);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 8);
		long tuesdaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 17);
		long tuesdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 13);
		long negativeHalfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, -0.5F));
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO negative percent SS predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO negative percent SS successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO negative percent SS unrelated");
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(tuesdayEight, tuesdaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SS, negativeHalfDurationPercent, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO negative percent SS predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO negative percent SS successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO negative percent SS unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(tuesdayEight, restoredPredecessor.getStart());
		assertEquals(tuesdaySeventeen, restoredPredecessor.getEnd());
		assertEquals(mondayThirteen, restoredSuccessor.getStart());
		assertEquals(tuesdayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.SS, restoredDependency.getDependencyType());
		assertEquals(negativeHalfDurationPercent, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(restoredDependency.getLag()));
		assertEquals(-0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(restoredDependency.getLag()));
		assertEquals(-oneDay / 2, com.microproject.datatype.Duration.millis(restoredDependency.getLeadValue()));
		assertEquals(mondayTwelve, restoredDependency.calcForwardDependencyDate(
			restoredPredecessor.getStart(), restoredPredecessor.getEnd(), true));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateFsFiftyPercentDurationLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 13);
		long wednesdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 13);
		long halfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, 0.5F));
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO percent FS predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO percent FS successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO percent FS unrelated");
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(mondayEight, mondaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, halfDurationPercent, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(tuesdayThirteen, successor.getStart());
		assertEquals(wednesdayThirteen, successor.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO percent FS predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO percent FS successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO percent FS unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(mondayEight, restoredPredecessor.getStart());
		assertEquals(mondaySeventeen, restoredPredecessor.getEnd());
		assertEquals(tuesdayThirteen, restoredSuccessor.getStart());
		assertEquals(wednesdayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.FS, restoredDependency.getDependencyType());
		assertEquals(halfDurationPercent, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(restoredDependency.getLag()));
		assertEquals(0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(restoredDependency.getLag()));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateFfFiftyPercentDurationLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 13);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 13);
		long halfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, 0.5F));
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO percent FF predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO percent FF successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO percent FF unrelated");
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(mondayEight, mondaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FF, halfDurationPercent, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO percent FF predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO percent FF successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO percent FF unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(mondayEight, restoredPredecessor.getStart());
		assertEquals(mondaySeventeen, restoredPredecessor.getEnd());
		assertEquals(mondayThirteen, restoredSuccessor.getStart());
		assertEquals(tuesdayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.FF, restoredDependency.getDependencyType());
		assertEquals(halfDurationPercent, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(restoredDependency.getLag()));
		assertEquals(0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(restoredDependency.getLag()));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateFfNegativeFiftyPercentDurationLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 13);
		long wednesdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 8);
		long wednesdayTwelve = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 12);
		long wednesdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 13);
		long wednesdaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 17);
		long negativeHalfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, -0.5F));
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO negative percent FF predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO negative percent FF successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO negative percent FF unrelated");
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(wednesdayEight, wednesdaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FF, negativeHalfDurationPercent, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(wednesdayTwelve, predecessor.getEffectiveWorkCalendar().add(
			predecessor.getEnd(), dependency.getLeadValue(), true));
		assertEquals(tuesdayThirteen, successor.getStart());
		assertEquals(wednesdayThirteen, successor.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO negative percent FF predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO negative percent FF successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO negative percent FF unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(wednesdayEight, restoredPredecessor.getStart());
		assertEquals(wednesdaySeventeen, restoredPredecessor.getEnd());
		assertEquals(tuesdayThirteen, restoredSuccessor.getStart());
		assertEquals(wednesdayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.FF, restoredDependency.getDependencyType());
		assertEquals(negativeHalfDurationPercent, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(restoredDependency.getLag()));
		assertEquals(-0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(restoredDependency.getLag()));
		assertEquals(-oneDay / 2, com.microproject.datatype.Duration.millis(restoredDependency.getLeadValue()));
		assertEquals(wednesdayTwelve, restoredPredecessor.getEffectiveWorkCalendar().add(
			restoredPredecessor.getEnd(), restoredDependency.getLeadValue(), true));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateFsNegativeFiftyPercentDurationLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondayTwelve = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 12);
		long mondayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 13);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 13);
		long negativeHalfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, -0.5F));
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO negative percent FS predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO negative percent FS successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO negative percent FS unrelated");
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(mondayEight, mondaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, negativeHalfDurationPercent, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO negative percent FS predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO negative percent FS successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO negative percent FS unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(mondayEight, restoredPredecessor.getStart());
		assertEquals(mondaySeventeen, restoredPredecessor.getEnd());
		assertEquals(mondayThirteen, restoredSuccessor.getStart());
		assertEquals(tuesdayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.FS, restoredDependency.getDependencyType());
		assertEquals(negativeHalfDurationPercent, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(restoredDependency.getLag()));
		assertEquals(-0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(restoredDependency.getLag()));
		assertEquals(-oneDay / 2, com.microproject.datatype.Duration.millis(restoredDependency.getLeadValue()));
		assertEquals(mondayTwelve, restoredDependency.calcForwardDependencyDate(
			restoredPredecessor.getStart(), restoredPredecessor.getEnd(), true));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateSfFiftyPercentDurationLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 13);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 8);
		long tuesdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 13);
		long tuesdaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 17);
		long halfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, 0.5F));
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO percent SF predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO percent SF successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO percent SF unrelated");
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(tuesdayEight, tuesdaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SF, halfDurationPercent, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(tuesdayThirteen, predecessor.getEffectiveWorkCalendar().add(
			predecessor.getStart(), dependency.getLeadValue(), false));
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO percent SF predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO percent SF successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO percent SF unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(tuesdayEight, restoredPredecessor.getStart());
		assertEquals(tuesdaySeventeen, restoredPredecessor.getEnd());
		assertEquals(mondayThirteen, restoredSuccessor.getStart());
		assertEquals(tuesdayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.SF, restoredDependency.getDependencyType());
		assertEquals(halfDurationPercent, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(restoredDependency.getLag()));
		assertEquals(0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(restoredDependency.getLag()));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateSfNegativeFiftyPercentDurationLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 13);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayTwelve = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 12);
		long tuesdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 13);
		long wednesdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 8);
		long wednesdaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 17);
		long negativeHalfDurationPercent = com.microproject.datatype.Duration.setAsPercent(
			com.microproject.datatype.Duration.setPercentAsDecimal(0L, -0.5F));
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO negative percent SF predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO negative percent SF successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO negative percent SF unrelated");
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(wednesdayEight, wednesdaySeventeen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SF, negativeHalfDurationPercent, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(-oneDay / 2, com.microproject.datatype.Duration.millis(dependency.getLeadValue()));
		assertEquals(tuesdayTwelve, predecessor.getEffectiveWorkCalendar().add(
			predecessor.getStart(), dependency.getLeadValue(), false));
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO negative percent SF predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO negative percent SF successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO negative percent SF unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(wednesdayEight, restoredPredecessor.getStart());
		assertEquals(wednesdaySeventeen, restoredPredecessor.getEnd());
		assertEquals(mondayThirteen, restoredSuccessor.getStart());
		assertEquals(tuesdayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.SF, restoredDependency.getDependencyType());
		assertEquals(negativeHalfDurationPercent, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isPercent(restoredDependency.getLag()));
		assertEquals(-0.5F, com.microproject.datatype.Duration.getPercentAsDecimal(restoredDependency.getLag()));
		assertEquals(-oneDay / 2, com.microproject.datatype.Duration.millis(restoredDependency.getLeadValue()));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateFsFourElapsedHourLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondayTwelve = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 12);
		long mondaySixteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 16);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdaySixteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 16);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO elapsed FS predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO elapsed FS successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO elapsed FS unrelated");
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(mondayEight, mondayTwelve);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, fourElapsedHours, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(mondaySixteen, successor.getStart());
		assertEquals(tuesdaySixteen, successor.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO elapsed FS predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO elapsed FS successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO elapsed FS unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(mondayEight, restoredPredecessor.getStart());
		assertEquals(mondayTwelve, restoredPredecessor.getEnd());
		assertEquals(mondaySixteen, restoredSuccessor.getStart());
		assertEquals(tuesdaySixteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.FS, restoredDependency.getDependencyType());
		assertEquals(fourElapsedHours, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(restoredDependency.getLag()));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateFsNegativeFourElapsedHourLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondayFourteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 14);
		long mondayTen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 10);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO negative elapsed FS predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO negative elapsed FS successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO negative elapsed FS unrelated");
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(oneDay);
		successor.setDuration(3L * 60L * 60L * 1000L);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(mondayEight, mondayFourteen);
		predecessor.setDuration(5L * 60L * 60L * 1000L);
		successor.setManualDates(mondayEight, mondaySeventeen);
		successor.setDuration(3L * 60L * 60L * 1000L);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, -fourElapsedHours, this);
		successor.setManuallyScheduled(false);
		project.recalculate();
		long successorFinish = mondayFourteen;
		assertEquals(mondayTen, predecessor.getEnd() + com.microproject.datatype.Duration.millis(dependency.getLag()));
		assertEquals(mondayTen, successor.getStart());
		assertEquals(successorFinish, successor.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO negative elapsed FS predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO negative elapsed FS successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO negative elapsed FS unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(mondayEight, restoredPredecessor.getStart());
		assertEquals(mondayFourteen, restoredPredecessor.getEnd());
		assertEquals(mondayTen, restoredSuccessor.getStart());
		assertEquals(successorFinish, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.FS, restoredDependency.getDependencyType());
		assertEquals(-fourElapsedHours, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(restoredDependency.getLag()));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateSsNegativeFourElapsedHourLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayFour = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 4);
		long tuesdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 8);
		long tuesdayEleven = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 11);
		long tuesdayFourteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 14);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		// Document-derived: Microsoft defines SS endpoints and negative lag as lead/overlap.
		// Exact subtraction and standard-calendar normalization are inferred, not measured.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO negative elapsed SS predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO negative elapsed SS successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO negative elapsed SS unrelated");
		predecessor.setDuration(5L * 60L * 60L * 1000L);
		successor.setDuration(3L * 60L * 60L * 1000L);
		unrelated.setDuration(3L * 60L * 60L * 1000L);
		predecessor.setManualDates(tuesdayEight, tuesdayFourteen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SS, -fourElapsedHours, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(tuesdayFour, predecessor.getStart() + com.microproject.datatype.Duration.millis(dependency.getLag()));
		assertEquals(tuesdayEight, successor.getStart());
		assertEquals(tuesdayEleven, successor.getEnd());
		assertTrue(successor.getStart() > mondayEight);
		assertEquals(tuesdayEight, predecessor.getStart());
		assertEquals(tuesdayFourteen, predecessor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO negative elapsed SS predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO negative elapsed SS successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO negative elapsed SS unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(tuesdayEight, restoredPredecessor.getStart());
		assertEquals(tuesdayFourteen, restoredPredecessor.getEnd());
		assertEquals(tuesdayEight, restoredSuccessor.getStart());
		assertEquals(tuesdayEleven, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.SS, restoredDependency.getDependencyType());
		assertEquals(-fourElapsedHours, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(restoredDependency.getLag()));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateFfNegativeFourElapsedHourLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayFour = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 4);
		long tuesdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 8);
		long tuesdayTen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 10);
		long tuesdayFourteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 14);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		// Document-derived: Microsoft defines FF endpoint ordering and negative lag as lead.
		// The precise wall-clock boundary and calendar normalization here are inferred.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO negative elapsed FF predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO negative elapsed FF successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO negative elapsed FF unrelated");
		predecessor.setDuration(5L * 60L * 60L * 1000L);
		successor.setDuration(2L * 60L * 60L * 1000L);
		unrelated.setDuration(3L * 60L * 60L * 1000L);
		predecessor.setManualDates(tuesdayEight, tuesdayFourteen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FF, -fourElapsedHours, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(tuesdayTen, predecessor.getEnd() + com.microproject.datatype.Duration.millis(dependency.getLag()));
		assertEquals(tuesdayEight, successor.getStart());
		assertEquals(tuesdayTen, successor.getEnd());
		assertTrue(successor.getStart() > mondayEight);
		assertEquals(tuesdayEight, predecessor.getStart());
		assertEquals(tuesdayFourteen, predecessor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO negative elapsed FF predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO negative elapsed FF successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO negative elapsed FF unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(tuesdayEight, restoredPredecessor.getStart());
		assertEquals(tuesdayFourteen, restoredPredecessor.getEnd());
		assertEquals(tuesdayEight, restoredSuccessor.getStart());
		assertEquals(tuesdayTen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.FF, restoredDependency.getDependencyType());
		assertEquals(-fourElapsedHours, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(restoredDependency.getLag()));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateSfNegativeFourElapsedHourLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayFour = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 4);
		long tuesdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 8);
		long tuesdayEleven = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 11);
		long tuesdayFourteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 14);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		// Document-derived: Microsoft defines SF endpoint ordering and negative lag as lead/overlap.
		// The exact wall-clock boundary and standard-calendar normalization below are inferred.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO negative elapsed SF predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO negative elapsed SF successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO negative elapsed SF unrelated");
		predecessor.setDuration(5L * 60L * 60L * 1000L);
		successor.setDuration(3L * 60L * 60L * 1000L);
		unrelated.setDuration(3L * 60L * 60L * 1000L);
		predecessor.setManualDates(tuesdayEight, tuesdayFourteen);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SF, -fourElapsedHours, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(tuesdayFour, predecessor.getStart() + com.microproject.datatype.Duration.millis(dependency.getLag()));
		assertEquals(tuesdayEight, successor.getStart());
		assertEquals(tuesdayEleven, successor.getEnd());
		assertEquals(tuesdayEight, predecessor.getStart());
		assertEquals(tuesdayFourteen, predecessor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO negative elapsed SF predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO negative elapsed SF successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO negative elapsed SF unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(tuesdayEight, restoredPredecessor.getStart());
		assertEquals(tuesdayFourteen, restoredPredecessor.getEnd());
		assertEquals(tuesdayEight, restoredSuccessor.getStart());
		assertEquals(tuesdayEleven, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.SF, restoredDependency.getDependencyType());
		assertEquals(-fourElapsedHours, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(restoredDependency.getLag()));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateSsFourElapsedHourLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondayTwelve = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 12);
		long mondayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 13);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 13);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO elapsed SS predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO elapsed SS successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO elapsed SS unrelated");
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(mondayEight, mondayTwelve);
		successor.setManualDates(mondayEight, mondaySeventeen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SS, fourElapsedHours, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO elapsed SS predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO elapsed SS successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO elapsed SS unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(mondayEight, restoredPredecessor.getStart());
		assertEquals(mondayTwelve, restoredPredecessor.getEnd());
		assertEquals(mondayThirteen, restoredSuccessor.getStart());
		assertEquals(tuesdayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.SS, restoredDependency.getDependencyType());
		assertEquals(fourElapsedHours, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(restoredDependency.getLag()));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateFfFourElapsedHourLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondayTwelve = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 12);
		long mondayFourteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 14);
		long mondaySixteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 16);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO elapsed FF predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO elapsed FF successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO elapsed FF unrelated");
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		long twoHours = oneDay / 4;
		predecessor.setDuration(oneDay);
		successor.setDuration(twoHours);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(mondayEight, mondayTwelve);
		successor.setManualDates(mondayEight, mondayTwelve);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FF, fourElapsedHours, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(mondayFourteen, successor.getStart());
		assertEquals(mondaySixteen, successor.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO elapsed FF predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO elapsed FF successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO elapsed FF unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(mondayEight, restoredPredecessor.getStart());
		assertEquals(mondayTwelve, restoredPredecessor.getEnd());
		assertEquals(mondayFourteen, restoredSuccessor.getStart());
		assertEquals(mondaySixteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.FF, restoredDependency.getDependencyType());
		assertEquals(fourElapsedHours, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(restoredDependency.getLag()));
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFixedDateSfFourElapsedHourLag() throws Exception {
		Project project = projectForRoundTrip();
		project.setWorkCalendar(com.microproject.pm.calendar.CalendarService.getInstance().getStandardInstance()
			.copyForBaselineSnapshot());
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondayTen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 10);
		long mondayTwelve = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 12);
		long mondayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 13);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long fourElapsedHours = com.microproject.datatype.Duration.setAsElapsed(4L * 60L * 60L * 1000L);
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO elapsed SF predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO elapsed SF successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO elapsed SF unrelated");
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		long twoHours = oneDay / 4;
		predecessor.setDuration(oneDay);
		successor.setDuration(twoHours);
		unrelated.setDuration(oneDay);
		predecessor.setManualDates(mondayEight, mondayTwelve);
		successor.setManualDates(mondayEight, mondayTen);
		unrelated.setManualDates(mondayEight, mondaySeventeen);
		DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SF, fourElapsedHours, this);
		successor.setManuallyScheduled(false);
		project.setStart(mondayEight);
		project.recalculate();
		assertEquals(mondayTen, successor.getStart());
		assertEquals(mondayThirteen, successor.getEnd());

		assignPositiveUniqueIds(project);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO elapsed SF predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO elapsed SF successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO elapsed SF unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(predecessorId, restoredPredecessor.getUniqueId());
		assertEquals(successorId, restoredSuccessor.getUniqueId());
		assertEquals(mondayEight, restoredPredecessor.getStart());
		assertEquals(mondayTwelve, restoredPredecessor.getEnd());
		assertEquals(mondayTen, restoredSuccessor.getStart());
		assertEquals(mondayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.SF, restoredDependency.getDependencyType());
		assertEquals(fourElapsedHours, restoredDependency.getLag());
		assertTrue(com.microproject.datatype.Duration.isElapsed(restoredDependency.getLag()));
		assertEquals(mondayTwelve, restoredPredecessor.getStart()
			+ com.microproject.datatype.Duration.millis(restoredDependency.getLag()),
			"reloaded SF boundary is predecessor start plus the persisted elapsed lag");
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void successfulRespectLinksManualFsWorkingLagSurvivesMpoRoundTrip() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask predecessor = (NormalTask) firstTask(original);
		predecessor.setName("Respect Links predecessor");
		NormalTask successor = (NormalTask) original.createLocalTaskNode(null).getImpl();
		successor.setName("Respect Links successor");
		long projectStart = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long staleFinish = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long predecessorFinish = staleFinish;
		long predecessorStart = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long successorStart = predecessorStart;
		long successorFinish = staleFinish;
		long eightHours = 8L * 60L * 60L * 1000L;
		original.setStartDate(projectStart);
		predecessor.setDuration(eightHours);
		predecessor.setManualDates(predecessorStart, predecessorFinish);
		predecessor.setManuallyScheduled(true);
		successor.setDuration(eightHours);
		successor.setManualDates(successorStart, successorFinish);
		successor.setManuallyScheduled(true);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, eightHours, this);

		RespectLinksService.Result result = new RespectLinksService().apply(original, List.of(successor));

		assertEquals(1, result.movedCount());
		assertTrue(successor.isManuallyScheduled(), "Respect Links preserves manual scheduling mode");
		long expectedStart = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 8);
		long expectedFinish = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 17);
		assertEquals(expectedStart, successor.getStart());
		assertEquals(expectedFinish, successor.getEnd());

		Project reopened = loadFromBytes(saveProjectBytes(original));
		NormalTask restoredPredecessor = null;
		NormalTask restoredSuccessor = null;
		for (java.util.Iterator<?> tasks = reopened.getTaskOutlineIterator(); tasks.hasNext();) {
			Task candidate = (Task) tasks.next();
			if ("Respect Links predecessor".equals(candidate.getName())) restoredPredecessor = (NormalTask) candidate;
			if ("Respect Links successor".equals(candidate.getName())) restoredSuccessor = (NormalTask) candidate;
		}
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertTrue(restoredSuccessor.isManuallyScheduled());
		assertEquals(expectedStart, restoredSuccessor.getStart());
		assertEquals(expectedFinish, restoredSuccessor.getEnd());
		assertEquals(1, restoredSuccessor.getPredecessorList().size());
		Dependency restoredDependency = (Dependency) restoredSuccessor.getPredecessorList().iterator().next();
		assertEquals(DependencyType.Kind.FS, restoredDependency.getDependencyKind());
		assertEquals(eightHours, restoredDependency.getLag());
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
	}

	@Test
	void mpoRoundTripPreservesSfDependencyAndSuccessorStartConstraint() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask predecessor = (NormalTask) firstTask(original);
		predecessor.setName("SF predecessor");
		NormalTask successor = (NormalTask) original.createLocalTaskNode(null).getImpl();
		successor.setName("SNET successor");
		long start = java.time.Instant.parse("2030-06-03T08:00:00Z").toEpochMilli();
		long successorStart = java.time.Instant.parse("2030-06-04T08:00:00Z").toEpochMilli();
		long eightHours = 8L * 60L * 60L * 1000L;
		long fourHours = 4L * 60L * 60L * 1000L;
		original.setStartDate(start);
		predecessor.setDuration(eightHours);
		successor.setDuration(fourHours);
		predecessor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, start);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.SF, 0L, this);
		successor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, successorStart);
		original.recalculate();
		long predecessorEnd = java.time.Instant.parse("2030-06-03T17:00:00Z").toEpochMilli();
		long successorEnd = java.time.Instant.parse("2030-06-04T13:00:00Z").toEpochMilli();
		assertEquals(start, predecessor.getStart());
		assertEquals(predecessorEnd, predecessor.getEnd());
		assertEquals(successorStart, successor.getStart());
		assertEquals(successorEnd, successor.getEnd());

		byte[] archive = saveProjectBytes(original);
		Project reopened = loadFromBytes(archive);
		reopened.recalculate();
		NormalTask restoredPredecessor = null;
		NormalTask restoredSuccessor = null;
		for (java.util.Iterator<?> tasks = reopened.getTaskOutlineIterator(); tasks.hasNext();) {
			Task candidate = (Task) tasks.next();
			if ("SF predecessor".equals(candidate.getName())) restoredPredecessor = (NormalTask) candidate;
			if ("SNET successor".equals(candidate.getName())) restoredSuccessor = (NormalTask) candidate;
		}
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, restoredSuccessor.getConstraintTypeKind());
		assertEquals(successorStart, restoredSuccessor.getConstraintDate());
		assertEquals(start, restoredPredecessor.getStart());
		assertEquals(predecessorEnd, restoredPredecessor.getEnd());
		assertEquals(successorStart, restoredSuccessor.getStart());
		assertEquals(successorEnd, restoredSuccessor.getEnd());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.Kind.SF, restoredDependency.getDependencyKind());
		assertEquals(0L, restoredDependency.getLag());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertEquals(1, restoredSuccessor.getPredecessorList().size());
		assertSame(restoredPredecessor, ((Dependency) restoredSuccessor.getPredecessorList().iterator().next()).getPredecessor());
	}

	@Test
	void mpoRoundTripPreservesSnetAndPositiveSfLagAcrossProjectCalendarException() throws Exception {
		Project project = projectForRoundTrip();
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 13);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayMidnight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 0);
		long wednesdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 8);
		long wednesdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 13);
		long fourHours = 4L * 60L * 60L * 1000L;
		long eightHours = 8L * 60L * 60L * 1000L;
		project.setStartDate(mondayEight);
		var calendar = (com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar();
		calendar.addOrReplaceException(new com.microproject.pm.calendar.WorkDay(tuesdayMidnight));
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO SF lag predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO SF lag successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO SF lag unrelated");
		predecessor.setDuration(fourHours);
		successor.setDuration(fourHours);
		unrelated.setDuration(fourHours);
		predecessor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayThirteen);
		successor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		unrelated.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SF, eightHours, this);
		project.recalculate();
		assertEquals(mondayThirteen, predecessor.getStart());
		assertEquals(mondaySeventeen, predecessor.getEnd());
		assertEquals(wednesdayEight, successor.getStart());
		assertEquals(wednesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondayThirteen, unrelated.getEnd());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, successor.getConstraintTypeKind());
		assertEquals(mondayEight, successor.getConstraintDate());
		assertEquals(DependencyType.SF, dependency.getDependencyType());
		assertEquals(eightHours, dependency.getLag());
		assertFalse(calendar.getConcreteInstance().getWorkDay(tuesdayMidnight).isWorking());

		assignPositiveUniqueIds(project);
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO SF lag predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO SF lag successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO SF lag unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(mondayThirteen, restoredPredecessor.getStart());
		assertEquals(mondaySeventeen, restoredPredecessor.getEnd());
		assertEquals(wednesdayEight, restoredSuccessor.getStart());
		assertEquals(wednesdayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondayThirteen, restoredUnrelated.getEnd());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, restoredSuccessor.getConstraintTypeKind());
		assertEquals(mondayEight, restoredSuccessor.getConstraintDate());
		assertFalse(((com.microproject.pm.calendar.WorkingCalendar) reopened.getWorkCalendar())
			.getConcreteInstance().getWorkDay(tuesdayMidnight).isWorking());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.SF, restoredDependency.getDependencyType());
		assertEquals(eightHours, restoredDependency.getLag());
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertEquals(1, restoredSuccessor.getPredecessorList().size());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesSnetAndFinishToFinishBoundary() throws Exception {
		verifySnetFfMpoRoundTrip(1,
			mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17),
			mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 8),
			mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 17));
		verifySnetFfMpoRoundTrip(2,
			mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 17),
			mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 8),
			mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 17));
	}

	@Test
	void mpoRoundTripPreservesSnetAndStartToStartBoundary() throws Exception {
		verifySnetSsMpoRoundTrip(mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8),
			mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 8),
			mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17));
		verifySnetSsMpoRoundTrip(mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 8),
			mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8),
			mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 17));
	}

	@Test
	void mpoRoundTripPreservesSnetAndPositiveFsLagAcrossProjectCalendarException() throws Exception {
		long tuesdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 8);
		long thursdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 6, 8);
		long thursdaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 6, 17);
		long fridayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 7, 8);
		long fridaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 7, 17);
		verifySnetFsLagAcrossProjectException(tuesdayEight,
			mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 17), thursdayEight, thursdaySeventeen);
		verifySnetFsLagAcrossProjectException(fridayEight, fridaySeventeen, fridayEight, fridaySeventeen);
	}

	@Test
	void mpoRoundTripPreservesFinishToStartLeadAndSnetAcrossProjectCalendarException() throws Exception {
		Project project = projectForRoundTrip();
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayMidnight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 0);
		long thursdayTwelve = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 6, 12);
		long thursdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 6, 13);
		long fridayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 7, 13);
		long fridayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 7, 8);
		long fridaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 7, 17);
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		long twelveHourLead = -12L * 60L * 60L * 1000L;
		project.setWorkCalendar(((com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar()).makeScratchCopy());
		var calendar = (com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar();
		calendar.setName("MPO project FS Tuesday exception");
		calendar.addOrReplaceException(new com.microproject.pm.calendar.WorkDay(tuesdayMidnight));
		project.setStart(mondayEight);
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO FS lead predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO FS lead successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO FS lead unrelated");
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, fridayEight);
		successor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		unrelated.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, twelveHourLead, this);
		project.recalculate();
		assertEquals(fridayEight, predecessor.getStart());
		assertEquals(fridaySeventeen, predecessor.getEnd());
		assertEquals(thursdayTwelve,
			dependency.getEffectiveWorkCalendar().add(fridaySeventeen, dependency.getLeadValue(), false));
		assertEquals(thursdayThirteen, successor.getStart());
		assertEquals(fridayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, successor.getConstraintTypeKind());
		assertEquals(mondayEight, successor.getConstraintDate());
		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertEquals(twelveHourLead, dependency.getLag());
		assertFalse(calendar.getConcreteInstance().getWorkDay(tuesdayMidnight).isWorking());

		assignPositiveUniqueIds(project);
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO FS lead predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO FS lead successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO FS lead unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(fridayEight, restoredPredecessor.getStart());
		assertEquals(fridaySeventeen, restoredPredecessor.getEnd());
		assertEquals(thursdayThirteen, restoredSuccessor.getStart());
		assertEquals(fridayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, restoredSuccessor.getConstraintTypeKind());
		assertEquals(mondayEight, restoredSuccessor.getConstraintDate());
		assertFalse(((com.microproject.pm.calendar.WorkingCalendar) reopened.getWorkCalendar())
			.getConcreteInstance().getWorkDay(tuesdayMidnight).isWorking());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.FS, restoredDependency.getDependencyType());
		assertEquals(twelveHourLead, restoredDependency.getLag());
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertEquals(1, restoredSuccessor.getPredecessorList().size());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFinishToFinishLeadAcrossProjectCalendarException() throws Exception {
		Project project = projectForRoundTrip();
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayMidnight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 0);
		long wednesdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 8);
		long wednesdaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 17);
		long eightHours = 8L * 60L * 60L * 1000L;
		long twelveHourLead = -12L * 60L * 60L * 1000L;
		var calendar = (com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar();
		calendar.addOrReplaceException(new com.microproject.pm.calendar.WorkDay(tuesdayMidnight));
		project.setStart(mondayEight);
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO FF lead predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO FF lead successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO FF lead unrelated");
		predecessor.setDuration(eightHours);
		successor.setDuration(eightHours);
		unrelated.setDuration(eightHours);
		predecessor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, wednesdayEight);
		successor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		unrelated.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FF, twelveHourLead, this);
		project.recalculate();
		assertEquals(wednesdayEight, predecessor.getStart());
		assertEquals(wednesdaySeventeen, predecessor.getEnd());
		assertEquals(mondayEight, successor.getStart());
		assertEquals(mondaySeventeen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertEquals(DependencyType.FF, dependency.getDependencyType());
		assertEquals(twelveHourLead, dependency.getLag());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, successor.getConstraintTypeKind());
		assertEquals(mondayEight, successor.getConstraintDate());
		assertFalse(calendar.getConcreteInstance().getWorkDay(tuesdayMidnight).isWorking(),
			"Tuesday remains a nonworking project-calendar exception");

		assignPositiveUniqueIds(project);
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO FF lead predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO FF lead successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO FF lead unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(wednesdayEight, restoredPredecessor.getStart());
		assertEquals(wednesdaySeventeen, restoredPredecessor.getEnd());
		assertEquals(mondayEight, restoredSuccessor.getStart());
		assertEquals(mondaySeventeen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, restoredSuccessor.getConstraintTypeKind());
		assertEquals(mondayEight, restoredSuccessor.getConstraintDate());
		assertFalse(((com.microproject.pm.calendar.WorkingCalendar) reopened.getWorkCalendar())
			.getConcreteInstance().getWorkDay(tuesdayMidnight).isWorking(),
			"MPO reload preserves the Tuesday nonworking exception");
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.FF, restoredDependency.getDependencyType());
		assertEquals(twelveHourLead, restoredDependency.getLag());
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertEquals(1, restoredSuccessor.getPredecessorList().size());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesFinishToFinishLeadAndSnetAcrossProjectCalendarException() throws Exception {
		Project project = projectForRoundTrip();
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondayTen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 10);
		long mondayTwelve = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 12);
		long mondayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 13);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayMidnight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 0);
		long wednesdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 8);
		long wednesdaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 17);
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		long twelveHourLead = -12L * 60L * 60L * 1000L;
		long twoHours = 2L * 60L * 60L * 1000L;
		project.setWorkCalendar(((com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar()).makeScratchCopy());
		var calendar = (com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar();
		calendar.setName("MPO project FF Tuesday exception with SNET");
		calendar.addOrReplaceException(new com.microproject.pm.calendar.WorkDay(tuesdayMidnight));
		project.setStart(mondayEight);
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO FF lead SNET predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO FF lead SNET successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO FF lead SNET unrelated");
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, wednesdayEight);
		successor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		unrelated.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		successor.setDuration(twoHours);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FF, twelveHourLead, this);
		project.recalculate();
		assertEquals(wednesdayEight, predecessor.getStart());
		assertEquals(wednesdaySeventeen, predecessor.getEnd());
		assertEquals(mondayTwelve,
			dependency.getEffectiveWorkCalendar().add(wednesdaySeventeen, dependency.getLeadValue(), false));
		assertEquals(mondayTen, successor.getStart());
		assertEquals(mondayThirteen, successor.getEnd());
		assertTrue(successor.getEnd() >= mondayTwelve, "FF successor finish meets the required Monday 12:00 boundary");
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, successor.getConstraintTypeKind());
		assertEquals(mondayEight, successor.getConstraintDate());
		assertEquals(DependencyType.FF, dependency.getDependencyType());
		assertEquals(twelveHourLead, dependency.getLag());
		assertFalse(calendar.getConcreteInstance().getWorkDay(tuesdayMidnight).isWorking());

		assignPositiveUniqueIds(project);
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO FF lead SNET predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO FF lead SNET successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO FF lead SNET unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(wednesdayEight, restoredPredecessor.getStart());
		assertEquals(wednesdaySeventeen, restoredPredecessor.getEnd());
		assertEquals(mondayTen, restoredSuccessor.getStart());
		assertEquals(mondayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, restoredSuccessor.getConstraintTypeKind());
		assertEquals(mondayEight, restoredSuccessor.getConstraintDate());
		assertFalse(((com.microproject.pm.calendar.WorkingCalendar) reopened.getWorkCalendar())
			.getConcreteInstance().getWorkDay(tuesdayMidnight).isWorking());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.FF, restoredDependency.getDependencyType());
		assertEquals(twelveHourLead, restoredDependency.getLag());
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertEquals(1, restoredSuccessor.getPredecessorList().size());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesStartToFinishLeadAndSnetAcrossProjectCalendarException() throws Exception {
		Project project = projectForRoundTrip();
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 13);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayMidnight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 0);
		long wednesdayTwelve = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 12);
		long wednesdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 13);
		long fridayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 7, 8);
		long fridaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 7, 17);
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		long twelveHourLead = -12L * 60L * 60L * 1000L;
		project.setWorkCalendar(((com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar()).makeScratchCopy());
		var calendar = (com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar();
		calendar.setName("MPO project SF Tuesday exception");
		calendar.addOrReplaceException(new com.microproject.pm.calendar.WorkDay(tuesdayMidnight));
		project.setStart(mondayEight);
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO SF lead predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO SF lead successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO SF lead unrelated");
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, fridayEight);
		successor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		unrelated.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SF, twelveHourLead, this);
		project.recalculate();
		assertEquals(fridayEight, predecessor.getStart());
		assertEquals(fridaySeventeen, predecessor.getEnd());
		assertEquals(wednesdayTwelve,
			dependency.getEffectiveWorkCalendar().add(fridayEight, dependency.getLeadValue(), false));
		assertEquals(mondayThirteen, successor.getStart());
		assertEquals(wednesdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, successor.getConstraintTypeKind());
		assertEquals(mondayEight, successor.getConstraintDate());
		assertEquals(DependencyType.SF, dependency.getDependencyType());
		assertEquals(twelveHourLead, dependency.getLag());
		assertFalse(calendar.getConcreteInstance().getWorkDay(tuesdayMidnight).isWorking());

		assignPositiveUniqueIds(project);
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO SF lead predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO SF lead successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO SF lead unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(fridayEight, restoredPredecessor.getStart());
		assertEquals(fridaySeventeen, restoredPredecessor.getEnd());
		assertEquals(mondayThirteen, restoredSuccessor.getStart());
		assertEquals(wednesdayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, restoredSuccessor.getConstraintTypeKind());
		assertEquals(mondayEight, restoredSuccessor.getConstraintDate());
		assertFalse(((com.microproject.pm.calendar.WorkingCalendar) reopened.getWorkCalendar())
			.getConcreteInstance().getWorkDay(tuesdayMidnight).isWorking());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.SF, restoredDependency.getDependencyType());
		assertEquals(twelveHourLead, restoredDependency.getLag());
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertEquals(1, restoredSuccessor.getPredecessorList().size());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesStartToStartLeadAndSnetAcrossProjectCalendarException() throws Exception {
		Project project = projectForRoundTrip();
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayMidnight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 0);
		long wednesdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 8);
		long wednesdayTwelve = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 12);
		long wednesdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 13);
		long wednesdaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 17);
		long thursdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 6, 13);
		long fridayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 7, 8);
		long fridaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 7, 17);
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		long twelveHourLead = -12L * 60L * 60L * 1000L;
		var calendar = (com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar();
		calendar.addOrReplaceException(new com.microproject.pm.calendar.WorkDay(tuesdayMidnight));
		project.setStart(mondayEight);
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO SS lead predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO SS lead successor");
		NormalTask unrelated = (NormalTask) project.createLocalTaskNode(null).getImpl();
		unrelated.setName("MPO SS lead unrelated");
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		unrelated.setDuration(oneDay);
		predecessor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, fridayEight);
		successor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		unrelated.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SS, twelveHourLead, this);
		project.recalculate();
		assertEquals(fridayEight, predecessor.getStart());
		assertEquals(fridaySeventeen, predecessor.getEnd());
		assertEquals(wednesdayTwelve,
			dependency.getEffectiveWorkCalendar().add(fridayEight, dependency.getLeadValue(), false));
		assertEquals(wednesdayThirteen, successor.getStart());
		assertEquals(thursdayThirteen, successor.getEnd());
		assertEquals(mondayEight, unrelated.getStart());
		assertEquals(mondaySeventeen, unrelated.getEnd());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, successor.getConstraintTypeKind());
		assertEquals(mondayEight, successor.getConstraintDate());
		assertEquals(DependencyType.SS, dependency.getDependencyType());
		assertEquals(twelveHourLead, dependency.getLag());
		assertFalse(calendar.getConcreteInstance().getWorkDay(tuesdayMidnight).isWorking());

		assignPositiveUniqueIds(project);
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO SS lead predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO SS lead successor");
		NormalTask restoredUnrelated = (NormalTask) findByName(reopened, "MPO SS lead unrelated");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertNotNull(restoredUnrelated);
		assertEquals(fridayEight, restoredPredecessor.getStart());
		assertEquals(fridaySeventeen, restoredPredecessor.getEnd());
		assertEquals(wednesdayThirteen, restoredSuccessor.getStart());
		assertEquals(thursdayThirteen, restoredSuccessor.getEnd());
		assertEquals(mondayEight, restoredUnrelated.getStart());
		assertEquals(mondaySeventeen, restoredUnrelated.getEnd());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, restoredSuccessor.getConstraintTypeKind());
		assertEquals(mondayEight, restoredSuccessor.getConstraintDate());
		assertFalse(((com.microproject.pm.calendar.WorkingCalendar) reopened.getWorkCalendar())
			.getConcreteInstance().getWorkDay(tuesdayMidnight).isWorking());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.SS, restoredDependency.getDependencyType());
		assertEquals(twelveHourLead, restoredDependency.getLag());
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertEquals(1, restoredSuccessor.getPredecessorList().size());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	private void verifySnetFsLagAcrossProjectException(long snet, long expectedInitialSuccessorFinish,
		long expectedSuccessorStart, long expectedSuccessorFinish) throws Exception {
		Project project = projectForRoundTrip();
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayMidnight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 0);
		project.setWorkCalendar(((com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar()).makeScratchCopy());
		((com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar()).setName(
			"MPO project Tuesday exception");
		((com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar()).addOrReplaceException(
			new com.microproject.pm.calendar.WorkDay(tuesdayMidnight));
		project.setStart(mondayEight);
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO SNET lag predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO SNET lag successor");
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		predecessor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		successor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, snet);
		project.recalculate();
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(mondaySeventeen, predecessor.getEnd());
		assertEquals(snet, successor.getStart());
		assertEquals(expectedInitialSuccessorFinish, successor.getEnd());
		var originalEffectiveCalendar = predecessor.getEffectiveWorkCalendar();
		String originalTuesdayWorkDay = originalEffectiveCalendar.getConcreteInstance().getWorkDay(tuesdayMidnight).toString();
		long originalLagBoundary = originalEffectiveCalendar.add(mondaySeventeen, oneDay, true);

		long positiveLag = oneDay;
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, positiveLag, this);
		project.recalculate();
		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertEquals(positiveLag, dependency.getLag());
		assertEquals(mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 6, 8), dependency.getDate(true));
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(mondaySeventeen, predecessor.getEnd());
		assertEquals(expectedSuccessorStart, successor.getStart());
		assertEquals(expectedSuccessorFinish, successor.getEnd());
		assertNull(predecessor.getWorkCalendar(), "source predecessor inherits the project calendar");
		assertNull(successor.getWorkCalendar(), "source successor inherits the project calendar");

		assignPositiveUniqueIds(project);
		Project reopened = loadFromBytes(saveProjectBytes(project));
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO SNET lag predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO SNET lag successor");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		long restoredDependencyBoundaryBeforeRecalculation = restoredDependency.getDate(true);
		org.junit.jupiter.api.Assertions.assertAll("restored calendar and dependency diagnostics before recalculation",
			() -> assertNull(restoredPredecessor.getWorkCalendar(), "restored predecessor inherits project calendar"),
			() -> assertNull(restoredSuccessor.getWorkCalendar(), "restored successor inherits project calendar"),
			() -> assertEquals(originalEffectiveCalendar.getName(),
				restoredPredecessor.getEffectiveWorkCalendar().getName(), "effective calendar owner/name"),
			() -> assertEquals(originalTuesdayWorkDay, restoredPredecessor.getEffectiveWorkCalendar()
				.getConcreteInstance().getWorkDay(tuesdayMidnight).toString(), "date-specific Tuesday lookup"),
			() -> assertEquals(originalLagBoundary, restoredPredecessor.getEffectiveWorkCalendar()
				.add(mondaySeventeen, oneDay, true), "positive-lag add result"),
			() -> assertEquals("MPO project Tuesday exception", reopened.getWorkCalendar().getName()));
		reopened.recalculate();
		assertEquals(mondayEight, restoredPredecessor.getStart());
		assertEquals(mondaySeventeen, restoredPredecessor.getEnd());
		assertEquals(expectedSuccessorStart, restoredSuccessor.getStart(),
			"recalculated schedule after pre-recalculation dependency boundary "
				+ restoredDependencyBoundaryBeforeRecalculation);
		assertEquals(expectedSuccessorFinish, restoredSuccessor.getEnd());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET,
			restoredSuccessor.getConstraintTypeKind());
		assertEquals(snet, restoredSuccessor.getConstraintDate());
		assertEquals("MPO project Tuesday exception", reopened.getWorkCalendar().getName());
		assertTrue(java.util.Arrays.stream(((com.microproject.pm.calendar.WorkingCalendar)
			reopened.getWorkCalendar()).getExceptionDays())
			.anyMatch(exception -> exception.getStart() == tuesdayMidnight));
		assertEquals(DependencyType.FS, restoredDependency.getDependencyType());
		assertEquals(positiveLag, restoredDependency.getLag());
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertEquals(1, restoredSuccessor.getPredecessorList().size());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	private void verifySnetSsMpoRoundTrip(long predecessorStart, long snet, long expectedPredecessorFinish)
		throws Exception {
		Project project = projectForRoundTrip();
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long mondaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 17);
		long tuesdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 8);
		long tuesdaySeventeen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 17);
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO SNET SS predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO SNET SS successor");
		project.setStart(mondayEight);
		long oneDay = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		predecessor.setDuration(oneDay);
		successor.setDuration(oneDay);
		predecessor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, predecessorStart);
		successor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, snet);
		project.recalculate();
		assertEquals(predecessorStart, predecessor.getStart());
		assertEquals(expectedPredecessorFinish, predecessor.getEnd());
		assertEquals(snet, successor.getStart());
		assertEquals(snet == mondayEight ? mondaySeventeen : tuesdaySeventeen, successor.getEnd());

		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.SS, 0L, this);
		project.recalculate();
		assertEquals(predecessorStart, dependency.getDate(true));
		long expectedSuccessorFinish = tuesdaySeventeen;
		assertEquals(predecessorStart, predecessor.getStart());
		assertEquals(expectedPredecessorFinish, predecessor.getEnd());
		assertEquals(tuesdayEight, successor.getStart());
		assertEquals(expectedSuccessorFinish, successor.getEnd());

		assignPositiveUniqueIds(project);
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO SNET SS predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO SNET SS successor");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertEquals(predecessorStart, restoredPredecessor.getStart());
		assertEquals(expectedPredecessorFinish, restoredPredecessor.getEnd());
		assertEquals(tuesdayEight, restoredSuccessor.getStart());
		assertEquals(expectedSuccessorFinish, restoredSuccessor.getEnd());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET,
			restoredSuccessor.getConstraintTypeKind());
		assertEquals(snet, restoredSuccessor.getConstraintDate());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.SS, restoredDependency.getDependencyType());
		assertEquals(0L, restoredDependency.getLag());
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertEquals(1, restoredSuccessor.getPredecessorList().size());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	private void verifySnetFfMpoRoundTrip(int predecessorDurationDays, long expectedPredecessorFinish,
		long expectedSuccessorStart, long expectedSuccessorFinish) throws Exception {
		Project project = projectForRoundTrip();
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long tuesdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 8);
		NormalTask predecessor = (NormalTask) firstTask(project);
		predecessor.setName("MPO SNET FF predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("MPO SNET FF successor");
		project.setStart(mondayEight);
		predecessor.setDuration(predecessorDurationDays
			* com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		successor.setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		predecessor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		successor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, tuesdayEight);
		project.recalculate();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FF, 0L, this);
		project.recalculate();
		assertEquals(mondayEight, predecessor.getStart());
		assertEquals(expectedPredecessorFinish, predecessor.getEnd());
		assertEquals(expectedSuccessorStart, successor.getStart());
		assertEquals(expectedSuccessorFinish, successor.getEnd());
		assertEquals(DependencyType.FF, dependency.getDependencyType());
		assertEquals(0L, dependency.getLag());

		assignPositiveUniqueIds(project);
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "MPO SNET FF predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "MPO SNET FF successor");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertEquals(mondayEight, restoredPredecessor.getStart());
		assertEquals(expectedPredecessorFinish, restoredPredecessor.getEnd());
		assertEquals(expectedSuccessorStart, restoredSuccessor.getStart());
		assertEquals(expectedSuccessorFinish, restoredSuccessor.getEnd());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET,
			restoredSuccessor.getConstraintTypeKind());
		assertEquals(tuesdayEight, restoredSuccessor.getConstraintDate());
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.FF, restoredDependency.getDependencyType());
		assertEquals(0L, restoredDependency.getLag());
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertEquals(1, restoredSuccessor.getPredecessorList().size());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
	}

	@Test
	void mpoRoundTripPreservesSetFieldsSsWorkingLagAndRecalculatedSchedule() throws Exception {
		long mondayEight = java.time.Instant.parse("2030-06-03T08:00:00Z").toEpochMilli();
		long tuesdayEight = java.time.Instant.parse("2030-06-04T08:00:00Z").toEpochMilli();
		long tuesdayThirteen = java.time.Instant.parse("2030-06-04T13:00:00Z").toEpochMilli();
		long fourWorkingHours = 4L * 60L * 60L * 1000L;
		long workingDayLag = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("set-fields-mpo", undo), undo);
		project.initialize(false, false);
		project.setStart(mondayEight);

		NormalTask predecessor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		predecessor.setName("SetFields SS predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("SetFields SS successor");
		predecessor.setDuration(fourWorkingHours);
		successor.setDuration(fourWorkingHours);
		predecessor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		successor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		predecessor.getCurrentSchedule().setStart(mondayEight);
		successor.getCurrentSchedule().setStart(mondayEight);
		project.recalculate();
		Dependency dependency = DependencyService.getInstance().newDependency(
			predecessor, successor, DependencyType.FS, 0L, this);
		project.recalculate();
		DependencyService.getInstance().setFields(
			dependency, workingDayLag, DependencyType.Kind.SS, this);
		assertEquals(DependencyType.SS, dependency.getDependencyType());
		assertEquals(workingDayLag, dependency.getLag());
		assertEquals(tuesdayEight, dependency.getDate(true), "the converted SS link's standard-calendar date");
		project.recalculate();
		assertEquals(tuesdayEight, successor.getStart());
		assertEquals(tuesdayThirteen, successor.getEnd());

		assignPositiveUniqueIds(project);
		Project reopened = loadFromBytes(saveProjectBytes(project));
		reopened.recalculate();
		Task restoredPredecessor = findByName(reopened, "SetFields SS predecessor");
		Task restoredSuccessor = findByName(reopened, "SetFields SS successor");
		assertNotNull(restoredPredecessor);
		assertNotNull(restoredSuccessor);
		assertEquals(1, restoredPredecessor.getSuccessorList().size());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.SS, restoredDependency.getDependencyType());
		assertEquals(workingDayLag, restoredDependency.getLag());
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		assertEquals(1, restoredSuccessor.getPredecessorList().size());
		assertSame(restoredDependency, restoredSuccessor.getPredecessorList().iterator().next());
		assertEquals(tuesdayEight, restoredSuccessor.getStart());
		assertEquals(tuesdayThirteen, restoredSuccessor.getEnd());
	}

	@Test
	void mpoRoundTripPreservesProjectAndExplicitTaskCalendarsForSetFieldsSsLag() throws Exception {
		// Match TaskLinkAndBarScheduleTransitionsTest's document-derived predecessor-calendar contract;
		// Microsoft defines dependency endpoints and documents calendars/lag, but not which calendar owns lag.
		// https://support.microsoft.com/en-us/project/link-tasks-in-a-project
		// https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
		// https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
		long mondayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 3, 8);
		long tuesday = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 4, 0);
		long wednesday = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 5, 0);
		long thursdayEight = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 6, 8);
		long thursdayThirteen = mpoCalendarTimestamp(2030, java.util.Calendar.JUNE, 6, 13);
		long fourHours = 4L * 60L * 60L * 1000L;
		long eightHours = 8L * 60L * 60L * 1000L;
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("calendar-mpo", undo), undo);
		project.initialize(false, false);
		project.setWorkCalendar(((com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar()).makeScratchCopy());
		((com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar()).setName("MPO project Tuesday exception");
		((com.microproject.pm.calendar.WorkingCalendar) project.getWorkCalendar()).addOrReplaceException(
			new com.microproject.pm.calendar.WorkDay(tuesday));
		project.setStart(mondayEight);
		NormalTask predecessor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		predecessor.setName("Calendar SS predecessor");
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("Calendar SS successor");
		var predecessorCalendar = com.microproject.pm.calendar.WorkingCalendar.getInstanceBasedOn(project.getWorkCalendar());
		predecessorCalendar.setName("MPO predecessor Wednesday exception");
		predecessorCalendar.addOrReplaceException(new com.microproject.pm.calendar.WorkDay(wednesday));
		predecessor.setWorkCalendar(predecessorCalendar);
		predecessor.setDuration(fourHours);
		successor.setDuration(fourHours);
		predecessor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		successor.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, mondayEight);
		assertNull(successor.getWorkCalendar(), "source successor inherits the project calendar");
		predecessor.getCurrentSchedule().setStart(mondayEight);
		successor.getCurrentSchedule().setStart(mondayEight);
		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor,
			DependencyType.FS, 0L, this);
		DependencyService.getInstance().setFields(dependency, eightHours, DependencyType.Kind.SS, this);
		project.recalculate();
		assertEquals(thursdayEight, successor.getStart());
		assertEquals(thursdayThirteen, successor.getEnd());

		assignPositiveUniqueIds(project);
		Map<String, byte[]> saved = readEntries(saveProjectBytes(project));
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		var metadata = mapper.readTree(saved.get(MpoFileImporter.TASK_SCHEDULES_ENTRY));
		assertEquals("MPO project Tuesday exception", metadata.path("projectCalendar").path("name").asText());
		var serializedTaskCalendar = metadata.path("tasks").findValues("calendar").stream()
			.filter(value -> value.path("name").asText().equals("MPO predecessor Wednesday exception"))
			.findFirst().orElseThrow();
		assertTrue(serializedTaskCalendar.path("baseProjectCalendar").asBoolean(),
			"explicit task calendar must retain its project-calendar base reference");
		assertMalformedTaskCalendarInheritanceRejected(saved, mapper,
			"MPO predecessor Wednesday exception");
		Map<String, byte[]> lateMalformed = new LinkedHashMap<>(saved);
		var lateRoot = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(
			lateMalformed.get(MpoFileImporter.TASK_SCHEDULES_ENTRY));
		((com.fasterxml.jackson.databind.node.ObjectNode) lateRoot.path("tasks")
			.get(lateRoot.path("tasks").size() - 1)).put("constraintType", Integer.MAX_VALUE);
		lateMalformed.put(MpoFileImporter.TASK_SCHEDULES_ENTRY, mapper.writeValueAsBytes(lateRoot));
		updateEntryChecksum(lateMalformed, MpoFileImporter.TASK_SCHEDULES_ENTRY);
		Map<String, byte[]> noScheduleSidecar = new LinkedHashMap<>(lateMalformed);
		var emptySchedule = mapper.createObjectNode();
		emptySchedule.put("version", 1);
		emptySchedule.putArray("tasks");
		noScheduleSidecar.put(MpoFileImporter.TASK_SCHEDULES_ENTRY, mapper.writeValueAsBytes(emptySchedule));
		updateEntryChecksum(noScheduleSidecar, MpoFileImporter.TASK_SCHEDULES_ENTRY);
		int calendarsBefore = com.microproject.pm.calendar.CalendarService.getInstance().getDerivedCalendars().size();
		loadFromBytes(zip(noScheduleSidecar).toByteArray());
		int calendarsAfterWithoutSidecar = com.microproject.pm.calendar.CalendarService.getInstance()
			.getDerivedCalendars().size();
		assertThrows(IOException.class, () -> loadFromBytes(zip(lateMalformed).toByteArray()),
			"a late malformed task must reject the complete schedule sidecar");
		int calendarsAfterMalformed = com.microproject.pm.calendar.CalendarService.getInstance()
			.getDerivedCalendars().size();
		assertEquals(calendarsAfterWithoutSidecar - calendarsBefore,
			calendarsAfterMalformed - calendarsAfterWithoutSidecar,
			"malformed schedule metadata must not register calendars beyond ordinary MPO loading");
		Project reopened = loadFromBytes(zip(saved).toByteArray());
		reopened.recalculate();
		NormalTask restoredPredecessor = (NormalTask) findByName(reopened, "Calendar SS predecessor");
		NormalTask restoredSuccessor = (NormalTask) findByName(reopened, "Calendar SS successor");
		assertEquals("MPO project Tuesday exception", reopened.getWorkCalendar().getName());
		assertEquals("MPO predecessor Wednesday exception", restoredPredecessor.getWorkCalendar().getName());
		assertNull(restoredSuccessor.getWorkCalendar(), "restored successor inherits the project calendar");
		assertSame(reopened.getWorkCalendar(), restoredPredecessor.getWorkCalendar().getBaseCalendar(),
			"explicit task calendar must inherit from the restored project calendar instance");
		assertTrue(java.util.Arrays.stream(((com.microproject.pm.calendar.WorkingCalendar)
			reopened.getWorkCalendar()).getExceptionDays()).anyMatch(day -> day.getStart() == tuesday));
		assertTrue(java.util.Arrays.stream(((com.microproject.pm.calendar.WorkingCalendar)
			restoredPredecessor.getWorkCalendar()).getExceptionDays()).anyMatch(day -> day.getStart() == wednesday));
		assertFalse(metadata.path("tasks").findValues("taskUid").isEmpty());
		assertFalse(metadata.path("tasks").findParents("calendar").stream()
			.anyMatch(taskNode -> taskNode.path("taskUid").asLong() == successor.getUniqueId()),
			"an inherited task must not acquire explicit calendar metadata");
		assertEquals(thursdayEight, restoredSuccessor.getStart());
		assertEquals(thursdayThirteen, restoredSuccessor.getEnd());
		Dependency restoredDependency = (Dependency) restoredPredecessor.getSuccessorList().iterator().next();
		assertEquals(DependencyType.SS, restoredDependency.getDependencyType());
		assertEquals(eightHours, restoredDependency.getLag());
		assertSame(restoredPredecessor, restoredDependency.getPredecessor());
		assertSame(restoredSuccessor, restoredDependency.getSuccessor());
		Map<String, byte[]> legacy = new LinkedHashMap<>(saved);
		var legacyMetadata = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(
			legacy.get(MpoFileImporter.TASK_SCHEDULES_ENTRY));
		legacyMetadata.remove("projectCalendar");
		for (var taskNode : legacyMetadata.path("tasks"))
			((com.fasterxml.jackson.databind.node.ObjectNode) taskNode).remove("calendar");
		legacy.put(MpoFileImporter.TASK_SCHEDULES_ENTRY, mapper.writeValueAsBytes(legacyMetadata));
		updateEntryChecksum(legacy, MpoFileImporter.TASK_SCHEDULES_ENTRY);
		assertDoesNotThrow(() -> loadFromBytes(zip(legacy).toByteArray()),
			"old MPO without optional calendar metadata must retain content.xml-derived calendars");
	}

	@Test
	void releasedMpoWriterBaselineCostFixtureKeepsItsModelCurrencyAndUpgradesOnSave() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "mpo",
			"legacy-assignment-baseline-cost-probe.mpo");
		if (!Files.isRegularFile(fixture))
			fixture = Path.of("testdata", "mpo", "legacy-assignment-baseline-cost-probe.mpo");
		byte[] archive = Files.readAllBytes(fixture);
		assertEquals("f13568bbcae62cfa4973d3bf471c1c5a0a2eff6be2dec24fc4e122369d95d3ad", sha256(archive));
		Map<String, byte[]> entries = readEntries(archive);
		byte[] oldSchedules = entries.get(MpoFileImporter.TASK_SCHEDULES_ENTRY);
		if (oldSchedules != null) {
			var oldMetadata = new com.fasterxml.jackson.databind.ObjectMapper().readTree(oldSchedules);
			assertFalse(oldMetadata.has("projectCalendar"), "released fixture predates optional project calendar data");
			assertTrue(oldMetadata.path("tasks").findValues("calendar").isEmpty(),
				"released fixture predates optional task calendar data");
		}
		String meta = new String(entries.get(MpoFileImporter.META_ENTRY), StandardCharsets.UTF_8);
		assertTrue(meta.contains("applicationVersion=\"0.0.24\""));
		assertFalse(meta.contains("assignmentBaselineCostUnit"), "Released archive predates the unit marker");
		String projectXml = new String(entries.get(MpoFileImporter.PROJECT_ENTRY), StandardCharsets.UTF_8);
		assertTrue(projectXml.contains("<Type>5</Type><UID>1</UID>"));
		assertTrue(projectXml.contains("<Value>125</Value>"), "Released writer stored model currency directly");
		String manifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		assertEquals(sha256(entries.get(MpoFileImporter.PROJECT_ENTRY)),
			manifest.replaceFirst("(?s).*projectSha256=\"([^\"]+)\".*", "$1"));
		assertEquals(sha256(entries.get(MpoFileImporter.PROJECT_ENTRY)),
			manifest.replaceFirst("(?s).*path=\"content.xml\" sha256=\"([^\"]+)\".*", "$1"));

		Project reopened = loadFromBytes(archive);
		assertEquals(125.0d, ((NormalTask) firstTask(reopened)).getBaselineCost(0), 0.00001d,
			"MPO migration restores the released writer's model-currency baseline contour");
		NormalTask nativeTask = (NormalTask) firstTask(reopened);
		com.microproject.pm.assignment.Assignment nativeAssignment =
			(com.microproject.pm.assignment.Assignment) nativeTask.getAssignments().iterator().next();
		assertEquals(7.5d, nativeAssignment.getResource().getCostPerUse(), 0.00001d,
			"The released standard MSPDI resource writer already stored CostPerUse in hundredths");
		Project external = new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(entries.get(MpoFileImporter.PROJECT_ENTRY)), "xml", (progress, label) -> { });
		assertEquals(1.25d, ((NormalTask) firstTask(external)).getBaselineCost(0), 0.00001d,
			"The same literal 125 in ordinary MSPDI means 1.25 normalized currency units");
		assertEquals(7.5d, ((com.microproject.pm.assignment.Assignment) ((NormalTask) firstTask(external))
			.getAssignments().iterator().next()).getResource().getCostPerUse(), 0.00001d,
			"Ordinary MSPDI resource CostPerUse keeps the standard hundredths conversion");
		ByteArrayOutputStream upgraded = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(reopened, upgraded));
		Map<String, byte[]> upgradedEntries = readEntries(upgraded.toByteArray());
		assertTrue(new String(upgradedEntries.get(MpoFileImporter.META_ENTRY), StandardCharsets.UTF_8)
			.contains("assignmentBaselineCostUnit=\"mspdi-hundredths\""));
		Project upgradedReload = loadFromBytes(upgraded.toByteArray());
		assertEquals(125.0d, ((NormalTask) firstTask(upgradedReload)).getBaselineCost(0), 0.00001d,
			"Resaving marks converted rows, preventing a second 100x conversion");
	}

	@Test
	void legacyMpoAssignmentBaselineCostRowsUpgradeToMspdiCurrencyUnits() throws Exception {
		String xml = "<Project><Assignment>"
			+ "<TimephasedData><Type>5</Type><Value>0.001</Value></TimephasedData>"
			+ "<TimephasedData><Type>4</Type><Value>8</Value></TimephasedData>"
			+ "<TimephasedData><Type>23</Type><Value>1.25</Value></TimephasedData>"
			+ "</Assignment><Task><TimephasedData><Type>5</Type><Value>0.001</Value></TimephasedData></Task></Project>";
		String migrated = new String(MpoFileImporter.migrateLegacyAssignmentBaselineCosts(
			xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)), java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(migrated.contains("<Type>5</Type><Value>0.1</Value>"),
			"Legacy slot-zero baseline currency rows gain MSPDI hundredths while preserving sub-cent value");
		assertTrue(migrated.contains("<Type>4</Type><Value>8</Value>"),
			"Baseline work rows remain unchanged");
		assertTrue(migrated.contains("<Type>23</Type><Value>125</Value>"),
			"Numbered baseline currency rows use the same conversion");
		assertTrue(migrated.contains("<Task><TimephasedData><Type>5</Type><Value>0.001</Value></TimephasedData></Task>"),
			"Type 5 rows outside Assignment elements are not migrated");
	}

	@Test
	void saveFailureFeedbackDistinguishesConflictFromLockFailureAndNamesRecoveryCopy() {
		Path recoveryCopy = Path.of("C:/projects/recovery.mpo");
		MpoConflictRecoveryException conflict = new MpoConflictRecoveryException(recoveryCopy, List.of());
		MpoConflictRecoveryException lockFailure = MpoConflictRecoveryException.lockUnavailable(
			recoveryCopy, new IOException("locked"));

		assertTrue(conflict.userFacingMessage().contains(recoveryCopy.toString()));
		assertTrue(lockFailure.userFacingMessage().contains(recoveryCopy.toString()));
		assertNotEquals(conflict.userFacingMessage(), lockFailure.userFacingMessage());
	}

	@AfterEach
	void closeOwnedExtractionSessions() {
		MpoExtractionOwnershipRegistry.closeAll();
		MpoFileImporter.setExtractionWorkspaceRoot(null);
	}

	@Test
	void mpoRoundTripPreservesPerTaskTextStyle() throws Exception {
		Project project = projectForRoundTrip();
		Task styled = firstTask(project);
		styled.setFontFamily("Dialog");
		styled.setFontSize(15);
		styled.setFontColor(Integer.valueOf(0x2457A6));
		styled.setFontBold(true);
		styled.setFontItalic(true);
		styled.setFontStrikethrough(true);
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, archive));

		Task reopened = firstTask(new MpoFileImporter().loadProject(new ByteArrayInputStream(archive.toByteArray())));
		assertEquals("Dialog", reopened.getFontFamily());
		assertEquals(15, reopened.getFontSize());
		assertEquals(Integer.valueOf(0x2457A6), reopened.getFontColor());
		assertTrue(reopened.isFontBold() && reopened.isFontItalic() && reopened.isFontStrikethrough(),
			"MPO must keep the task text style that Format Painter and Text Styles write");
	}

	@Test
	void mpoRejectsCorruptedTaskTextStyleInsteadOfClampingIt() throws Exception {
		Project project = projectForRoundTrip();
		firstTask(project).setFontBold(true);
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, archive));
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		String styles = new String(entries.get(MpoFileImporter.TASK_TEXT_STYLES_ENTRY), StandardCharsets.UTF_8);
		assertTrue(styles.contains("\"fontSize\":0"), styles);
		for (String corrupted : List.of("\"fontSize\":99", "\"fontSize\":1.5")) {
			byte[] tampered = styles.replace("\"fontSize\":0", corrupted).getBytes(StandardCharsets.UTF_8);
			entries.put(MpoFileImporter.TASK_TEXT_STYLES_ENTRY, tampered);
			updateManifestChecksum(entries, MpoFileImporter.TASK_TEXT_STYLES_ENTRY, tampered);
			byte[] mpo = zip(entries).toByteArray();
			assertThrows(Exception.class, () -> loadFromBytes(mpo),
				"MPO must reject a task text style outside the model's 0..72 point range: " + corrupted);
		}
	}

	@Test
	void loadProjectDoesNotCloseCallerOwnedInputStream() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, archive));
		class CloseTrackingInputStream extends ByteArrayInputStream {
			boolean closed;
			CloseTrackingInputStream(byte[] data) { super(data); }
			@Override public void close() { closed = true; }
		}
		CloseTrackingInputStream input = new CloseTrackingInputStream(archive.toByteArray());
		assertTrue(new MpoFileImporter().loadProject(input) != null);
		assertFalse(input.closed, "MPO loader must not close a caller-owned stream");
	}

	@Test
	void mpoSnapshotSerializationRetainsTheUnderlyingFailureCause() {
		IOException failure = assertThrows(IOException.class,
				() -> new MpoFileImporter().saveProject(null, new ByteArrayOutputStream()));
		assertTrue(failure.getCause() != null,
				"MPO snapshot serialization must retain the MSPDI failure cause");
	}

	@Test
	void operationLogSemanticLimitsAreRejectedBeforeProjectImport() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, archive));
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		String documentId = manifestDocumentId(entries);
		String parent = "00000000-0000-0000-0000-000000000001";
		StringBuilder parents = new StringBuilder("[");
		for (int i = 0; i < MpoFileImporter.MAX_OPERATION_PARENTS + 1; i++) {
			if (i > 0) parents.append(',');
			parents.append('"').append(parent).append('"');
		}
		parents.append(']');
		String operation = "{\"id\":\"00000000-0000-0000-0000-000000000002\","
			+ "\"actorId\":\"00000000-0000-0000-0000-000000000003\",\"sequence\":1,"
			+ "\"parents\":" + parents + ",\"kind\":\"task.update\","
			+ "\"entityId\":\"00000000-0000-0000-0000-000000000004\",\"payload\":{}}\n";
		entries.put(MpoFileImporter.OPERATIONS_ENTRY,
			("{\"type\":\"header\",\"schemaVersion\":1,\"documentId\":\"" + documentId + "\"}\n" + operation)
				.getBytes(StandardCharsets.UTF_8));
		updateManifestChecksum(entries, MpoFileImporter.OPERATIONS_ENTRY, entries.get(MpoFileImporter.OPERATIONS_ENTRY));
		assertThrows(IOException.class, () -> new MpoFileImporter().loadProject(new ByteArrayInputStream(zip(entries).toByteArray())));
	}

	@Test
	void ccpmHistorySemanticLimitIsRejectedBeforeProjectImport() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, archive));
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		StringBuilder history = new StringBuilder();
		for (int i = 0; i < MpoFileImporter.MAX_CCPM_HISTORY_ENTRIES + 1; i++) history.append("{}\n");
		byte[] historyBytes = history.toString().getBytes(StandardCharsets.UTF_8);
		entries.put(MpoFileImporter.CCPM_HISTORY_ENTRY, historyBytes);
		updateManifestChecksum(entries, MpoFileImporter.CCPM_HISTORY_ENTRY, historyBytes);
		assertThrows(IOException.class, () -> new MpoFileImporter().loadProject(new ByteArrayInputStream(zip(entries).toByteArray())));
	}

	@Test
	void mpoSaveCreatesANewDestinationDirectory() throws Exception {
		Project project = projectForRoundTrip();
		File root = java.nio.file.Files.createTempDirectory("mpo-new-folder-").toFile();
		root.deleteOnExit();
		File destination = new File(root, "nested" + File.separator + "plan.mpo");
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(destination.getAbsolutePath());
		writer.setProject(project);

		writer.exportFile();

		assertTrue(destination.isFile(), "MPO save must create a missing destination directory");
		MpoFileImporter reader = new MpoFileImporter();
		reader.setFileName(destination.getAbsolutePath());
		try (java.io.FileInputStream input = new java.io.FileInputStream(destination)) {
			assertTrue(reader.loadProject(input) != null, "newly saved MPO must be readable");
		}
	}

	@Test
	void hiddenTaskVisibilitySurvivesMpoSaveAndReload() throws Exception {
		Project original = projectForRoundTrip();
		Task task = firstTask(original);
		task.setHiddenTask(true);

		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, archive));
		Project reopened = loadFromBytes(archive.toByteArray());

		Task restored = firstTask(reopened);
		assertTrue(restored.isHiddenTask(), "MPO reload must preserve hidden task visibility");
	}

	@Test
	void taskColumnLayoutSurvivesMpoSaveAndReload() throws Exception {
		Project original = projectForRoundTrip();
		FieldDictionary dictionary = FieldDictionary.getInstance();
		SpreadSheetFieldArray layout = new SpreadSheetFieldArray();
		layout.add(dictionary.getFieldFromId("Field.name"));
		layout.add(dictionary.getFieldFromId("Field.duration"));
		layout.add(dictionary.getFieldFromId("Field.percentComplete"));
		layout.setWidths(new ArrayList<>(List.of(240, 88, 72)));
		layout.setManualWidths(new ArrayList<>(List.of(true, false, true)));
		original.setFieldArray(layout);

		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, archive));
		Project reopened = loadFromBytes(archive.toByteArray());
		SpreadSheetFieldArray restored = reopened.getFieldArray();

		assertEquals(List.of("Field.name", "Field.duration", "Field.percentComplete"),
			restored.stream().map(Field::getId).toList());
		assertEquals(240, restored.getWidth(0));
		assertEquals(88, restored.getWidth(1));
		assertEquals(72, restored.getWidth(2));
		assertTrue(restored.isManualWidth(0));
		assertFalse(restored.isManualWidth(1));
		assertTrue(restored.isManualWidth(2));
	}

	@Test
	void mpoPreservesUnknownExtensionBytesAcrossLoadAndSave() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		String extensionPath = "future/vendor-state.bin";
		byte[] extension = new byte[] { 0, 1, 2, (byte) 0xFE, (byte) 0xFF };
		entries.put(extensionPath, extension);
		String manifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		manifest = manifest.replace("</manifest>", "<entry path=\"" + extensionPath + "\" sha256=\""
			+ sha256(extension) + "\"/></manifest>");
		entries.put(MpoFileImporter.MANIFEST_ENTRY, manifest.getBytes(StandardCharsets.UTF_8));

		Project reopened = loadFromBytes(zip(entries).toByteArray());
		ByteArrayOutputStream resaved = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(reopened, resaved);

		assertArrayEquals(extension, readEntries(resaved.toByteArray()).get(extensionPath));
	}

	@Test
	void taskOwnedSplitIntervalsSurviveMpoSaveAndReload() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(3L * day);
		long splitFrom = task.getStart() + 4L * 60L * 60L * 1000L;
		long splitTo = splitFrom + 60L * 60L * 1000L;
		List<TaskSplitInterval> expected = List.of(new TaskSplitInterval(
			splitFrom - task.getStart(), splitTo - task.getStart()));
		task.restoreTaskSplitIntervals(this, expected);

		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, archive));
		Project reopened = loadFromBytes(archive.toByteArray());
		NormalTask restored = (NormalTask) firstTask(reopened);

		assertEquals(expected, restored.getTaskSplitIntervals());
		List<String> projected = new ArrayList<>();
		restored.consumeIntervals(interval -> projected.add(interval.getStart() + ":" + interval.getEnd()));
		assertTrue(projected.size() > 1, "reloaded task projection must render the split gap: " + projected
			+ " / " + restored.getStart() + ".." + restored.getEnd() + " / " + expected);
	}

	@Test
	void assignmentLevelingSplitStateSurvivesMpoSaveReloadAndClear() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		boolean manuallyScheduled = task.isManuallyScheduled();
		Resource resource = original.getResourcePool().newResourceInstance();
		resource.setName("Leveling resource");
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, this);
		long manualFrom = assignment.getEffectiveWorkCalendar().add(assignment.getStart(),
			com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR, false);
		long manualTo = assignment.getEffectiveWorkCalendar().add(manualFrom,
			com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR, false);
		assertTrue(com.microproject.pm.scheduling.ScheduleService.getInstance().split(this, assignment, manualFrom,
			manualTo, null));
		assertEquals(assignment.getWork(null), assignment.getWorkContour().calcTotalWork(assignment.getDurationMillis()),
			"manual assignment contour must match scalar work before leveling");
		long beforeDuration = assignment.getDurationMillis();
		String beforeContour = assignment.getWorkContour().toString(beforeDuration);
		long beforeStart = assignment.getStart();
		long beforeEnd = assignment.getEnd();
		List<com.microproject.pm.task.TaskSplitInterval> manualTaskSplits = task.getManualTaskSplitIntervals();
		long from = assignment.getEffectiveWorkCalendar().add(assignment.getStart(),
			5L * com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR, false);
		long to = assignment.getEffectiveWorkCalendar().add(from,
			com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR, false);
		assertTrue(com.microproject.pm.scheduling.ScheduleService.getInstance().splitForLeveling(this, assignment, from, to));
		assertEquals(assignment.getWork(null), assignment.getWorkContour().calcTotalWork(assignment.getDurationMillis()),
			"manual and generated split contours must match scalar work before MPO export");
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, archive));
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		com.fasterxml.jackson.databind.JsonNode sidecar = mapper.readTree(entries.get(MpoFileImporter.ASSIGNMENT_LEVELING_SPLITS_ENTRY));
		assertEquals(1, sidecar.path("assignments").size());
		long serializedAssignmentUid = sidecar.path("assignments").get(0).path("assignmentUid").longValue();
		assertTrue(serializedAssignmentUid > 0L);
		Map<String, byte[]> legacyEntries = new LinkedHashMap<>(entries);
		com.fasterxml.jackson.databind.node.ObjectNode legacySidecar = (com.fasterxml.jackson.databind.node.ObjectNode)
			mapper.readTree(entries.get(MpoFileImporter.ASSIGNMENT_LEVELING_SPLITS_ENTRY));
		legacySidecar.put("version", 1);
		for (com.fasterxml.jackson.databind.JsonNode record : legacySidecar.path("assignments"))
			((com.fasterxml.jackson.databind.node.ObjectNode) record).remove("sourceContourMatchesGenerated");
		byte[] legacySidecarBytes = mapper.writeValueAsBytes(legacySidecar);
		legacyEntries.put(MpoFileImporter.ASSIGNMENT_LEVELING_SPLITS_ENTRY, legacySidecarBytes);
		updateManifestChecksum(legacyEntries, MpoFileImporter.ASSIGNMENT_LEVELING_SPLITS_ENTRY, legacySidecarBytes);
		Project legacyReopened = loadFromBytes(zip(legacyEntries).toByteArray());
		NormalTask legacyTask = (NormalTask) firstTask(legacyReopened);
		Assignment legacyAssignment = null;
		for (Object value : legacyTask.getAssignments()) {
			Assignment candidate = (Assignment) value;
			if (candidate.getUniqueId() == serializedAssignmentUid) legacyAssignment = candidate;
		}
		assertNotNull(legacyAssignment, "v1 sidecar must still load and restore assignment provenance");
		assertNotNull(legacyAssignment.getLevelingSplitState(), "v1 provenance must remain available for Clear");
		legacyAssignment.extend(legacyAssignment.getStart(), legacyAssignment.getEnd(),
			com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR);
		assertThrows(IllegalStateException.class,
			() -> new ResourceLevelingService().clear(legacyReopened, null, null),
			"v1 sidecars without the normalization marker must retain strict contour-edit rejection");

		Project reopened = loadFromBytes(archive.toByteArray());
		NormalTask restoredTask = (NormalTask) firstTask(reopened);
		Assignment restored = null;
		for (Object value : restoredTask.getAssignments()) {
			Assignment candidate = (Assignment) value;
			if (candidate.getUniqueId() == serializedAssignmentUid) restored = candidate;
		}
		assertNotNull(restored, "the sidecar must follow the serializer's native assignment UID mapping");
		assertNotNull(restored.getLevelingSplitState());
		assertTrue(restored.hasImportedTimephasedWork(), "MPO reload must exercise imported split work profiles");
		assertEquals(manuallyScheduled, restoredTask.isManuallyScheduled(), "MPO reload must preserve task scheduling mode");
		assertEquals(manualTaskSplits, restoredTask.getManualTaskSplitIntervals(),
			"MPO reload must preserve task-owned manual split state");
		new ResourceLevelingService().clear(reopened, reopened.getUndoController().getEditSupport());
		assertNull(restored.getLevelingSplitState());
		assertFalse(restored.hasImportedTimephasedWork(),
			"clearing generated splits must discard stale imported profiles when no actual work exists");
		assertEquals(beforeDuration, restored.getDurationMillis());
		assertEquals(beforeContour, restored.getWorkContour().toString(beforeDuration));
		assertEquals(beforeStart, restored.getStart(), "clear must retain the pre-leveling assignment start");
		assertEquals(beforeEnd, restored.getEnd(), "clear must retain the pre-leveling assignment finish");
		assertEquals(manuallyScheduled, restoredTask.isManuallyScheduled(), "clear must retain the task scheduling mode");
		assertEquals(manualTaskSplits, restoredTask.getManualTaskSplitIntervals(),
			"clear must retain task-owned manual split state");

		reopened.getUndoController().undo();
		assertNotNull(restored.getLevelingSplitState(), "Undo Clear Leveling must restore generated split provenance");
		assertEquals(beforeContour, restored.getLevelingSplitState().beforeContourCopy()
			.toString(restored.getLevelingSplitState().beforeDuration()),
			"Undo must retain the manual assignment contour as the generated split's baseline");
		reopened.getUndoController().redo();
		assertNull(restored.getLevelingSplitState(), "Redo Clear Leveling must remove generated provenance again");
		assertEquals(beforeContour, restored.getWorkContour().toString(restored.getDurationMillis()),
			"Redo must preserve the user-created assignment split");

		ByteArrayOutputStream clearedArchive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(reopened, clearedArchive));
		Project clearedReload = loadFromBytes(clearedArchive.toByteArray());
		NormalTask clearedTask = (NormalTask) firstTask(clearedReload);
		Assignment clearedAssignment = null;
		for (Object value : clearedTask.getAssignments()) {
			Assignment candidate = (Assignment) value;
			if (candidate.getUniqueId() == serializedAssignmentUid) clearedAssignment = candidate;
		}
		assertNotNull(clearedAssignment, "cleared MPO must preserve the native assignment identity");
		assertNull(clearedAssignment.getLevelingSplitState(), "cleared MPO must not restore generated provenance");
		assertEquals(beforeDuration, clearedAssignment.getDurationMillis());
		assertEquals(beforeContour, clearedAssignment.getWorkContour().toString(beforeDuration),
			"cleared MPO must preserve the manual assignment contour");
		assertEquals(manuallyScheduled, clearedTask.isManuallyScheduled());
		assertEquals(manualTaskSplits, clearedTask.getManualTaskSplitIntervals());

		Map<String, byte[]> malformed = new LinkedHashMap<>(entries);
		com.fasterxml.jackson.databind.node.ObjectNode malformedSidecar = (com.fasterxml.jackson.databind.node.ObjectNode)
			mapper.readTree(entries.get(MpoFileImporter.ASSIGNMENT_LEVELING_SPLITS_ENTRY));
		((com.fasterxml.jackson.databind.node.ObjectNode) malformedSidecar.path("assignments").get(0))
			.put("assignmentUid", Long.MAX_VALUE);
		byte[] malformedBytes = mapper.writeValueAsBytes(malformedSidecar);
		malformed.put(MpoFileImporter.ASSIGNMENT_LEVELING_SPLITS_ENTRY, malformedBytes);
		updateManifestChecksum(malformed, MpoFileImporter.ASSIGNMENT_LEVELING_SPLITS_ENTRY, malformedBytes);
		assertThrows(IOException.class, () -> loadFromBytes(zip(malformed).toByteArray()),
			"unknown assignment identities are rejected before split ownership is restored");
	}

	@Test
	void clearingAppliedLevelingAfterImportedActualWorkPreservesMpoState() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask targetTask = (NormalTask) firstTask(original);
		targetTask.setManuallyScheduled(false);
		targetTask.setDuration(5L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		Resource resource = original.getResourcePool().newResourceInstance();
		resource.setName("Selected leveling resource");
		Assignment target = AssignmentService.getInstance().newAssignment(targetTask, resource, 1D, 0L, this);
		NormalTask blockerTask = (NormalTask) original.createLocalTaskNode(null).getImpl();
		blockerTask.setName("Unselected blocker");
		blockerTask.setManuallyScheduled(false);
		blockerTask.setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		blockerTask.setPriority(900);
		blockerTask.setStart(original.getEffectiveWorkCalendar().add(original.getStartDate(),
			com.microproject.options.CalendarOption.getInstance().getMillisPerDay(), false));
		Assignment blocker = AssignmentService.getInstance().newAssignment(blockerTask, resource, 1D, 0L, this);
		String beforeContour = target.getWorkContour().toString(target.getDurationMillis());
		long blockerStart = blocker.getStart();
		long blockerEnd = blocker.getEnd();
		long blockerDelay = blocker.getLevelingDelay();
		long actualWork = com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		long totalWork = target.getWork(null);
		long remainingWork = totalWork - actualWork;
		ResourceLevelingService service = new ResourceLevelingService();
		var undo = original.getUndoController();
		ResourceLevelingService.Plan plan = service.previewSelectedTasks(original, List.of(resource),
			new ResourceLevelingService.Options(ResourceLevelingService.Order.PRIORITY_STANDARD, false, true,
				Long.MIN_VALUE, Long.MAX_VALUE, false, ResourceLevelingService.Sensitivity.HOUR, true, false),
			List.of(targetTask));
		assertEquals(1, plan.assignmentSplits().size(), "Apply must split the selected assignment around the fixed blocker");
		plan.apply(undo.getEditSupport());
		assertNotNull(target.getLevelingSplitState());
		var actualProfile = com.microproject.pm.assignment.contour.PersonalContour.getInstance(
			new com.microproject.pm.assignment.contour.AbstractContourBucket[] {
				com.microproject.pm.assignment.contour.PersonalContourBucket.getInstance(actualWork, 1.0D) });
		var remainingProfile = com.microproject.pm.assignment.contour.PersonalContour.getInstance(
			new com.microproject.pm.assignment.contour.AbstractContourBucket[] {
				com.microproject.pm.assignment.contour.PersonalContourBucket.getInstance(remainingWork, 1.0D) });
		target.setImportedTimephasedWork(actualProfile, remainingProfile);
		long statusDate = targetTask.getEnd();
		original.setStatusDate(statusDate);
		long expectedStatusDate = original.getStatusDate();

		byte[] appliedArchive = saveProjectBytes(original);
		com.fasterxml.jackson.databind.JsonNode appliedSplit = new com.fasterxml.jackson.databind.ObjectMapper()
			.readTree(readEntries(appliedArchive).get(MpoFileImporter.ASSIGNMENT_LEVELING_SPLITS_ENTRY))
			.path("assignments").get(0);
		assertTrue(appliedSplit.path("sourceContourMatchesGenerated").booleanValue());
		Project reopened = loadFromBytes(appliedArchive);
		NormalTask restoredTask = (NormalTask) findByName(reopened, targetTask.getName());
		assertEquals(1, restoredTask.getAssignments().size());
		Assignment restored = (Assignment) restoredTask.getAssignments().iterator().next();
		assertNotNull(restored.getLevelingSplitState(), "MPO reload must retain generated split ownership");
		assertTrue(restored.hasImportedTimephasedWork());
		assertEquals(actualWork, restored.getActualWork(null));
		assertEquals(remainingWork, restored.getRemainingWork());
		assertEquals(totalWork, restored.getWork(null));
		assertTrue(reopened.isStatusDateSet());
		assertEquals(expectedStatusDate, reopened.getStatusDate());
		assertTrue(restored.importedProfilesMatchWorkTotal(restored.getLevelingSplitState().generatedContourCopy(),
			restored.getLevelingSplitState().generatedDuration()),
			"imported profiles, persisted leveling contour, and current work must agree on the exact total");
		String generatedContour = restored.getWorkContour().toString(restored.getDurationMillis());
		var restoredUndo = reopened.getUndoController();
		Project editedContourProject = loadFromBytes(appliedArchive);
		NormalTask editedContourTask = (NormalTask) findByName(editedContourProject, targetTask.getName());
		Assignment editedContourAssignment = (Assignment) editedContourTask.getAssignments().iterator().next();
		var editedBuckets = editedContourAssignment.getWorkContour().getContourBuckets();
		assertEquals(2, editedBuckets.length);
		var firstBucket = (com.microproject.pm.assignment.contour.PersonalContourBucket) editedBuckets[0];
		var lastBucket = (com.microproject.pm.assignment.contour.PersonalContourBucket) editedBuckets[1];
		long redistributedWork = firstBucket.calcWork() - firstBucket.getDuration();
		editedBuckets[0] = com.microproject.pm.assignment.contour.PersonalContourBucket.getInstance(
			firstBucket.getDuration(), 1.0D);
		editedBuckets[1] = com.microproject.pm.assignment.contour.PersonalContourBucket.getInstance(
			lastBucket.getDuration(), lastBucket.getUnits() + (double) redistributedWork / lastBucket.getDuration());
		assertNotEquals(restored.getWorkContour().toString(restored.getDurationMillis()),
			editedContourAssignment.getWorkContour().toString(editedContourAssignment.getDurationMillis()));
		assertEquals(restored.getLevelingSplitState().generatedContourCopy().calcTotalWork(
			restored.getLevelingSplitState().generatedDuration()),
			editedContourAssignment.getWorkContour().calcTotalWork(editedContourAssignment.getDurationMillis()),
			"the rejection regression must change contour shape without changing total work");
		assertThrows(IllegalStateException.class,
			() -> service.clear(editedContourProject, List.of(editedContourTask), null),
			"a same-total contour shape edit after MPO reload must still reject Clear");

		service.clear(reopened, List.of(restoredTask), restoredUndo.getEditSupport());
		assertNull(restored.getLevelingSplitState());
		assertTrue(restored.hasImportedTimephasedWork(), "Clear retains imported actual and remaining profiles");
		assertEquals(actualWork, restored.getActualWork(null));
		assertEquals(remainingWork, restored.getRemainingWork());
		assertEquals(totalWork, restored.getWork(null));
		assertEquals(beforeContour, restored.getWorkContour().toString(restored.getDurationMillis()));
		assertEquals(expectedStatusDate, reopened.getStatusDate());
		NormalTask restoredBlockerTask = (NormalTask) findByName(reopened, blockerTask.getName());
		Assignment restoredBlocker = (Assignment) restoredBlockerTask.getAssignments().iterator().next();
		assertEquals(blockerStart, restoredBlocker.getStart());
		assertEquals(blockerEnd, restoredBlocker.getEnd());
		assertEquals(blockerDelay, restoredBlocker.getLevelingDelay());

		restoredUndo.undo();
		assertNotNull(restored.getLevelingSplitState(), "Undo Clear restores generated split provenance");
		assertEquals(generatedContour, restored.getWorkContour().toString(restored.getDurationMillis()));
		assertEquals(actualWork, restored.getActualWork(null));
		assertEquals(remainingWork, restored.getRemainingWork());
		assertEquals(expectedStatusDate, reopened.getStatusDate());
		restoredUndo.redo();
		assertNull(restored.getLevelingSplitState(), "Redo Clear removes generated split provenance");
		assertEquals(actualWork, restored.getActualWork(null));
		assertEquals(remainingWork, restored.getRemainingWork());
		assertEquals(totalWork, restored.getWork(null));
		assertEquals(beforeContour, restored.getWorkContour().toString(restored.getDurationMillis()));
		assertEquals(expectedStatusDate, reopened.getStatusDate());

		Project clearedReload = loadFromBytes(saveProjectBytes(reopened));
		NormalTask clearedTask = (NormalTask) findByName(clearedReload, targetTask.getName());
		assertEquals(1, clearedTask.getAssignments().size());
		Assignment cleared = (Assignment) clearedTask.getAssignments().iterator().next();
		assertNull(cleared.getLevelingSplitState());
		assertEquals(actualWork, cleared.getActualWork(null));
		assertEquals(remainingWork, cleared.getRemainingWork());
		assertEquals(totalWork, cleared.getWork(null));
		assertEquals(expectedStatusDate, clearedReload.getStatusDate());
	}

	@Test
	void manualAssignmentSplitSurvivesMpoSaveAndReloadWithExactWorkAndGap() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("manual-assignment-split-mpo", undo);
		pool.setLocal(true);
		Project original = Project.createProject(pool, undo);
		original.initialize(false, false);
		NormalTask task = (NormalTask) original.createLocalTaskNode(null).getImpl();
		task.setName("Manual split target");
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		task.getCurrentSchedule().setStart(original.getStart());
		task.setDuration(5L * day);
		NormalTask blockerTask = (NormalTask) original.createLocalTaskNode(null).getImpl();
		blockerTask.setName("Higher priority blocker");
		blockerTask.setDuration(day);
		blockerTask.setPriority(900);
		blockerTask.setStart(original.getEffectiveWorkCalendar().add(original.getStart(), day, false));
		Resource resource = pool.newResourceInstance();
		resource.setName("Manual split resource");
		((com.microproject.pm.resource.ResourceImpl) resource).setBookingTypeKind(
			com.microproject.pm.resource.BookingType.Kind.COMMITTED);
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, this);
		long durationBefore = assignment.getDurationMillis();
		long from = assignment.getStart() + com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		long to = from + com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		Resource other = pool.newResourceInstance();
		other.setName("Other resource");
		AssignmentService.getInstance().newAssignment(task, other, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(blockerTask, resource, 1D, 0L, this);
		original.recalculate();
		assignment.split(this, from, to);
		String expectedContour = assignment.getWorkContour().toString(assignment.getDurationMillis());
		long expectedWork = assignment.getWork(null);
		long contourWork = assignment.getWorkContour().calcTotalWork(assignment.getDurationMillis());
		assertNotEquals(durationBefore, assignment.getDurationMillis(), "manual split must create a work contour gap");
		assertEquals(expectedWork, contourWork,
			"manual split contour work must equal assignment work before MPO export; contour=" + expectedContour);

		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, archive));
		Project reopened = loadFromBytes(archive.toByteArray());
		NormalTask restoredTask = (NormalTask) firstTask(reopened);
		Assignment restored = (Assignment) restoredTask.getAssignments().iterator().next();
		assertEquals(expectedWork, restored.getWork(null), "MPO reload changed scalar assignment work");
		assertEquals(expectedContour, restored.getWorkContour().toString(restored.getDurationMillis()),
			"MPO reload must preserve the exact user-created assignment contour and gap");
	}

	@Test
	void mpoPreservesManualAndLevelingSplitProvenanceForClearLeveling() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(5L * day);
		long start = task.getStart();
		long manualFrom = task.getEffectiveWorkCalendar().add(start, day, false);
		long manualTo = task.getEffectiveWorkCalendar().add(manualFrom, day, false);
		long levelingFrom = task.getEffectiveWorkCalendar().add(start, 3L * day, false);
		long levelingTo = task.getEffectiveWorkCalendar().add(levelingFrom, day, false);
		assertTrue(com.microproject.pm.scheduling.ScheduleService.getInstance().split(this, task, manualFrom, manualTo, null));
		assertTrue(com.microproject.pm.scheduling.ScheduleService.getInstance().splitForLeveling(this, task, levelingFrom, levelingTo));

		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, archive));
		Project reopened = loadFromBytes(archive.toByteArray());
		NormalTask restored = (NormalTask) firstTask(reopened);
		assertEquals(task.getManualTaskSplitIntervals(), restored.getManualTaskSplitIntervals());
		assertEquals(task.getLevelingTaskSplitIntervals(), restored.getLevelingTaskSplitIntervals());

		new com.microproject.pm.resource.ResourceLevelingService().clear(reopened);
		assertEquals(task.getManualTaskSplitIntervals(), restored.getTaskSplitIntervals(),
			"clear after MPO reload must retain manual gaps and remove only generated leveling gaps");
		assertTrue(restored.getLevelingTaskSplitIntervals().isEmpty());
	}

	@Test
	void mpoClearRestoresOptInLevelingManualDatesAfterReload() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		long start = task.getStart();
		long finish = task.getEnd();
		long shift = com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		task.setManuallyScheduled(true);
		task.setManualDates(start + shift, finish + shift);
		task.setLevelingDelay(com.microproject.datatype.Duration.millis(shift));
		task.setLevelingManualDateState(new Task.LevelingManualDateState(start, finish, start + shift, finish + shift));

		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, archive));
		Project reopened = loadFromBytes(archive.toByteArray());
		NormalTask restored = (NormalTask) firstTask(reopened);
		assertEquals(start + shift, restored.getManualStart());
		assertEquals(start, restored.getLevelingManualDateState().originalStart());

		new com.microproject.pm.resource.ResourceLevelingService().clear(reopened);

		assertEquals(start, restored.getManualStart(), "Clear after MPO reload restores the pre-leveling date");
		assertEquals(finish, restored.getManualFinish());
		org.junit.jupiter.api.Assertions.assertNull(restored.getLevelingManualDateState());
	}

	@Test
	void mpoTaskSplitMetadataWithoutProvenanceTreatsExistingGapsAsManual() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(4L * day);
		long start = task.getStart();
		long manualFrom = task.getEffectiveWorkCalendar().add(start, day, false);
		long manualTo = task.getEffectiveWorkCalendar().add(manualFrom, day, false);
		assertTrue(com.microproject.pm.scheduling.ScheduleService.getInstance().split(this, task, manualFrom, manualTo, null));
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, archive));
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		com.fasterxml.jackson.databind.node.ObjectNode root = (com.fasterxml.jackson.databind.node.ObjectNode)
			mapper.readTree(entries.get(MpoFileImporter.TASK_SPLITS_ENTRY));
		for (com.fasterxml.jackson.databind.JsonNode node : root.path("tasks")) {
			((com.fasterxml.jackson.databind.node.ObjectNode) node).remove(List.of("manualIntervals", "levelingIntervals"));
		}
		byte[] legacySplits = mapper.writeValueAsBytes(root);
		entries.put(MpoFileImporter.TASK_SPLITS_ENTRY, legacySplits);
		updateManifestChecksum(entries, MpoFileImporter.TASK_SPLITS_ENTRY, legacySplits);
		Project reopened = loadFromBytes(zip(entries).toByteArray());
		NormalTask restored = (NormalTask) firstTask(reopened);
		assertEquals(task.getTaskSplitIntervals(), restored.getManualTaskSplitIntervals());
		assertTrue(restored.getLevelingTaskSplitIntervals().isEmpty(),
			"version-1 MPO task split records without provenance are treated as user-owned splits");
	}

	@Test
	void levelingSplitProvenanceDoesNotChangeLegacyTaskSerializationDescriptor() {
		ObjectStreamClass taskDescriptor = ObjectStreamClass.lookup(com.microproject.pm.task.Task.class);
		assertEquals(786665335611L, taskDescriptor.getSerialVersionUID());
		assertTrue(java.util.Arrays.stream(taskDescriptor.getFields())
			.noneMatch(field -> field.getName().equals("levelingCanSplit")
				|| field.getName().equals("levelAssignments")),
			"legacy Task descriptors must not acquire POD fields for transient leveling defaults");
		ObjectStreamClass descriptor = ObjectStreamClass.lookup(NormalTask.class);
		assertEquals(273898992929L, descriptor.getSerialVersionUID());
		assertTrue(java.util.Arrays.stream(descriptor.getFields())
			.noneMatch(field -> field.getName().equals("manualTaskSplitIntervals")
				|| field.getName().equals("levelingTaskSplitIntervals")
				|| field.getName().equals("levelingCanSplit")
				|| field.getName().equals("levelAssignments")
				|| field.getName().equals("recurringTask")),
			"transient split and task-leveling options remain outside the immutable POD object descriptor");
		assertTrue(java.util.Arrays.stream(ObjectStreamClass.lookup(Task.class).getFields())
			.noneMatch(field -> field.getName().equals("levelingManualDateState")),
			"manual-date provenance must stay outside the immutable POD task descriptor");
	}

	@Test
	void summaryBaselineRollupPreservesSameResourceProfilesAcrossMpoAndUndoRedo() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask summary = (NormalTask) firstTask(project);
		NormalTask first = project.createScriptedTask();
		NormalTask second = project.createScriptedTask();
		first.setName("Contributor one");
		second.setName("Contributor two");
		var model = project.getTaskModel();
		var summaryNode = model.search(summary);
		assertTrue(model.relocate(List.of(model.search(first), model.search(second)), summaryNode, 0,
			com.microproject.grouping.core.model.NodeModel.SILENT));
		summary.setWbsChildrenNodes(model.getHierarchy().getChildren(summaryNode));
		first.setWbsParent(summary);
		second.setWbsParent(summary);

		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		long start = project.getStartDate();
		first.getCurrentSchedule().setStart(start);
		first.getCurrentSchedule().setFinish(start + day);
		first.getCurrentSchedule().setRawDuration(day);
		second.getCurrentSchedule().setStart(start + 2L * day);
		second.getCurrentSchedule().setFinish(start + 4L * day);
		second.getCurrentSchedule().setRawDuration(2L * day);
		first.setFixedCost(11.0d);
		second.setFixedCost(22.0d);
		first.setIgnoreResourceCalendar(false);
		second.setIgnoreResourceCalendar(false);
		summary.setFixedCost(7.0d);
		summary.setFixedCostAccrual(com.microproject.pm.costing.Accrual.Kind.START.code());
		Resource resource = project.getResourcePool().newResourceInstance();
		long firstCalendarException = start + 10L * day;
		long secondCalendarException = start + 20L * day;
		com.microproject.pm.calendar.WorkingCalendar firstCalendar =
			com.microproject.pm.calendar.WorkingCalendar.getInstanceBasedOn(project.getWorkCalendar());
		firstCalendar.setName("Baseline profile calendar one");
		firstCalendar.addOrReplaceException(new com.microproject.pm.calendar.WorkDay(firstCalendarException,
			firstCalendarException + day, "First profile exception"));
		((com.microproject.pm.resource.ResourceImpl) resource).setWorkCalendar(firstCalendar);
		Assignment firstAssignment = AssignmentService.getInstance().newAssignment(first, resource, 1D, 0L,
			MpoFileImporterTest.class);
		assertTrue(java.util.Arrays.stream(((com.microproject.pm.calendar.WorkingCalendar)
			firstAssignment.getEffectiveWorkCalendar()).getConcreteInstance().getExceptionDays())
			.anyMatch(exception -> exception.getStart() == firstCalendarException),
			"Fixture assignment must resolve the first resource-calendar exception");
		firstAssignment.setWork(8L * 60L * 60L * 1000L, null);
		firstAssignment.setRate(new com.microproject.datatype.Rate(5.0D, com.microproject.datatype.TimeUnit.HOURS));
		firstAssignment.setOvertimeWork(60L * 60L * 1000L);
		first.saveCurrentToSnapshot(Integer.valueOf(Snapshottable.BASELINE));
		TaskSnapshot firstBaseline = (TaskSnapshot) first.getSnapshot(Integer.valueOf(Snapshottable.BASELINE));
		firstAssignment.setInterval(Assignment.BASELINE_COST, firstAssignment.getStart(), firstAssignment.getFinish(), 125.0D);
		var firstProfile = firstBaseline.captureBaselineProfiles(first, Snapshottable.BASELINE).get(0);
		firstAssignment.setRate(new com.microproject.datatype.Rate(9.0D, com.microproject.datatype.TimeUnit.HOURS));
		assertTrue(java.util.Arrays.stream(firstProfile.calendarCopy().getExceptionDays())
			.anyMatch(value -> value.getStart() == firstCalendarException),
			"The first saved profile lost its exception: profile=" + firstProfile.profileId() + " resource="
				+ firstProfile.resourceUniqueId() + " calendar=" + firstProfile.calendarCopy().getName() + " exceptions="
				+ java.util.Arrays.toString(java.util.Arrays.stream(firstProfile.calendarCopy().getExceptionDays())
					.mapToLong(com.microproject.pm.calendar.WorkDay::getStart).toArray()));
		com.microproject.pm.calendar.WorkingCalendar secondCalendar =
			com.microproject.pm.calendar.WorkingCalendar.getInstanceBasedOn(project.getWorkCalendar());
		secondCalendar.setName("Baseline profile calendar two");
		secondCalendar.addOrReplaceException(new com.microproject.pm.calendar.WorkDay(secondCalendarException,
			secondCalendarException + day, "Second profile exception"));
		((com.microproject.pm.resource.ResourceImpl) resource).setWorkCalendar(secondCalendar);
		Assignment secondAssignment = AssignmentService.getInstance().newAssignment(second, resource, 1D, 0L,
			MpoFileImporterTest.class);
		secondAssignment.setWork(16L * 60L * 60L * 1000L, null);
		secondAssignment.setRate(new com.microproject.datatype.Rate(3.0D, com.microproject.datatype.TimeUnit.HOURS));
		secondAssignment.setOvertimeWork(2L * 60L * 60L * 1000L);
		second.saveCurrentToSnapshot(Integer.valueOf(Snapshottable.BASELINE));
		TaskSnapshot secondBaseline = (TaskSnapshot) second.getSnapshot(Integer.valueOf(Snapshottable.BASELINE));
		secondAssignment.setInterval(Assignment.BASELINE_COST, secondAssignment.getStart(), secondAssignment.getFinish(), 375.0D);
		var secondProfile = secondBaseline.captureBaselineProfiles(second, Snapshottable.BASELINE).get(0);
		assertTrue(java.util.Arrays.stream(secondProfile.calendarCopy().getExceptionDays())
			.anyMatch(value -> value.getStart() == secondCalendarException),
			"The second saved profile must contain its explicit exception");
		var firstProfileAfterSecondCalendarChange = firstBaseline.captureBaselineProfiles(first,
			Snapshottable.BASELINE).get(0);
		assertEquals(firstProfile.profileId(), firstProfileAfterSecondCalendarChange.profileId());
		assertTrue(java.util.Arrays.stream(firstProfileAfterSecondCalendarChange.calendarCopy().getExceptionDays())
			.anyMatch(value -> value.getStart() == firstCalendarException),
			"Changing the shared resource calendar must not alter the first saved profile");
		assertEquals(5.0D, firstProfile.rate(), "Saved profile rate must not follow a later live rate change");
		assertEquals(3.0D, secondProfile.rate());
		assertEquals(60L * 60L * 1000L, firstProfile.overtimeWork());
		assertEquals(2L * 60L * 60L * 1000L, secondProfile.overtimeWork());
		Assignment firstSavedBaseline = (Assignment) firstBaseline.getAssignments().iterator().next();
		Assignment secondSavedBaseline = (Assignment) secondBaseline.getAssignments().iterator().next();
		assertEquals(9L * 60L * 60L * 1000L, firstSavedBaseline.getWork(null),
			"The first saved baseline total includes eight regular hours and one overtime hour");
		assertEquals(18L * 60L * 60L * 1000L, secondSavedBaseline.getWork(null),
			"The second saved baseline total includes sixteen regular hours and two overtime hours");
		assertEquals(60L * 60L * 1000L, firstSavedBaseline.getDetail().getOvertimeWork());
		assertEquals(2L * 60L * 60L * 1000L, secondSavedBaseline.getDetail().getOvertimeWork());
		assertEquals(27L * 60L * 60L * 1000L,
			firstSavedBaseline.calcAll(Assignment.WORK) + secondSavedBaseline.calcAll(Assignment.WORK),
			"Expected baseline total is regular work plus the separately preserved overtime values");
		assertEquals(125.0D, firstSavedBaseline.calcAll(Assignment.COST), 0.00001D,
			"The first saved baseline must retain its explicit cost contour before roll-up: profile="
				+ firstProfile.costContour() + " duration=" + firstProfile.duration() + " start=" + firstProfile.start()
				+ " finish=" + firstProfile.finish());
		assertEquals(375.0D, secondSavedBaseline.calcAll(Assignment.COST), 0.00001D,
			"The second saved baseline must retain its explicit cost contour before roll-up");
		assertNotEquals(firstProfile.costContour(), secondProfile.costContour(),
			"Same-resource contributors retain distinct saved cost contours");
		summary.saveCurrentToSnapshot(Integer.valueOf(Snapshottable.BASELINE));
		TaskSnapshot originalSummaryBaseline = (TaskSnapshot) summary
			.getSnapshot(Integer.valueOf(Snapshottable.BASELINE));
		int originalSummaryBaselineAssignmentCount = originalSummaryBaseline.getAssignments().size();
		originalSummaryBaseline.setImportedBaselineCost(Double.valueOf(1.0d));
		assertEquals(1.0d, summary.getBaselineCost(Snapshottable.BASELINE),
			"The imported whole-task aggregate is authoritative before roll-up");

		var change = project.saveCurrentToSnapshot(Integer.valueOf(Snapshottable.BASELINE), false,
			List.of(summary), new BaselineRollupOptions(false, true));
		TaskSnapshot rolled = (TaskSnapshot) summary.getSnapshot(Integer.valueOf(Snapshottable.BASELINE));
		assertTrue(rolled.getImportedBaselineCost() == null,
			"A recomputed summary must clear the previous imported aggregate override");
		assertEquals(2, rolled.getAssignments().size());
		assertEquals(540.0d, summary.getBaselineCost(Snapshottable.BASELINE), 0.001d,
			"The recomputed roll-up replaces the imported scalar with its derived total");
		assertEquals(7.0d, rolled.getFixedCost(), "The summary retains its own Fixed Cost field value");
		assertTrue(rolled.captureBaselineProfiles(summary, Snapshottable.BASELINE).stream()
			.anyMatch(profile -> java.util.Arrays.stream(profile.calendarCopy().getExceptionDays())
				.anyMatch(value -> value.getStart() == firstCalendarException)),
			"Initial roll-up preserves the first contributor calendar");
		change.undo();
		TaskSnapshot restoredSummaryBaseline = (TaskSnapshot) summary
			.getSnapshot(Integer.valueOf(Snapshottable.BASELINE));
		assertEquals(originalSummaryBaselineAssignmentCount, restoredSummaryBaseline.getAssignments().size());
		assertEquals(1.0d, summary.getBaselineCost(Snapshottable.BASELINE),
			"Undo restores the imported aggregate that preceded roll-up");
		change.redo();
		rolled = (TaskSnapshot) summary.getSnapshot(Integer.valueOf(Snapshottable.BASELINE));
		assertTrue(rolled.getImportedBaselineCost() == null, "Redo restores the recomputed summary authority");
		assertEquals(540.0d, summary.getBaselineCost(Snapshottable.BASELINE), 0.001d,
			"Redo recomputes the aggregate from the rolled profiles");
		assertEquals(2, rolled.getAssignments().size(), "Redo restores each contributor profile");
		var rolledProfiles = rolled.captureBaselineProfiles(summary, Snapshottable.BASELINE);
		assertTrue(rolledProfiles.stream().anyMatch(profile -> java.util.Arrays.stream(profile.calendarCopy()
			.getExceptionDays()).anyMatch(value -> value.getStart() == firstCalendarException)),
			"Roll-up preserves the first contributor calendar before serialization");
		assertTrue(rolledProfiles.stream().anyMatch(profile -> java.util.Arrays.stream(profile.calendarCopy()
			.getExceptionDays()).anyMatch(value -> value.getStart() == secondCalendarException)),
			"Roll-up preserves the second contributor calendar before serialization");
		assertTrue(summary.isDirty(), "A summary whose baseline profiles changed must be serialized");
		java.util.ArrayList<Integer> serializedSnapshotSlots = new java.util.ArrayList<>();
		com.microproject.server.data.Serializer.forAssignments(summary,
			(assignment, snapshotId) -> serializedSnapshotSlots.add(snapshotId));
		assertEquals(2, serializedSnapshotSlots.stream().filter(slot -> slot == Snapshottable.BASELINE).count(),
			"The baseline assignment walker must see both summary profiles");
		assertEquals(27L * 60L * 60L * 1000L, rolled.work(0L, Long.MAX_VALUE));
		assertEquals(500.0D, rolled.getAssignments().stream().map(Assignment.class::cast)
			.mapToDouble(value -> value.calcAll(Assignment.COST)).sum(), 0.00001D,
			"The rolled baseline preserves both independent assignment cost contours");
		assertEquals(7.0d, rolled.getFixedCost());
		long baselineWindowStart = start - day;
		long baselineWindowEnd = start + 5L * day;
		java.util.List<Double> rolledAssignmentIntervalCosts = rolled.getAssignments().stream().map(Assignment.class::cast)
			.map(value -> value.baselineCost(baselineWindowStart, baselineWindowEnd)).sorted().toList();
		assertEquals(List.of(125.0d, 375.0d), rolledAssignmentIntervalCosts,
			"Each rolled same-resource baseline profile must query its own saved contour");
		double rolledAssignmentIntervalCost = rolledAssignmentIntervalCosts.stream().mapToDouble(Double::doubleValue).sum();
		assertEquals(40.0d, summary.getBaselineCost(Snapshottable.BASELINE,
			baselineWindowStart, baselineWindowEnd) - rolledAssignmentIntervalCost, 0.001d,
			"The rolled summary interval adds both frozen descendant fixed costs and its own once; "
				+ "independent child calcAll contours remain 125+375="
				+ (firstSavedBaseline.calcAll(Assignment.COST) + secondSavedBaseline.calcAll(Assignment.COST))
				+ ", rolled assignment intervals=" + rolled.getAssignments().stream().map(Assignment.class::cast)
					.map(value -> value.baselineCost(baselineWindowStart, baselineWindowEnd) + "/"
						+ value.calcAll(Assignment.COST)).toList());
		assertEquals(540.0d, summary.getBaselineCost(Snapshottable.BASELINE,
			baselineWindowStart, baselineWindowEnd), 0.001d,
			"All-time summary baseline cost must equal 500 in child assignment contours plus 40 fixed costs; "
				+ "rolled assignment intervals=" + rolled.getAssignments().stream().map(Assignment.class::cast)
					.map(value -> value.baselineCost(baselineWindowStart, baselineWindowEnd) + "/"
						+ value.calcAll(Assignment.COST)).toList());
		assertEquals(0, summary.getAssignments().stream()
			.filter(value -> !((Assignment) value).isDefault()).count(),
			"Baseline roll-up must not create current summary assignments");

		byte[] savedArchive = saveProjectBytes(project);
		java.util.Map<String, byte[]> savedEntries = readEntries(savedArchive);
		String currentMeta = new String(savedEntries.get(MpoFileImporter.META_ENTRY), StandardCharsets.UTF_8);
		assertTrue(currentMeta.contains("assignmentBaselineCostUnit=\"mspdi-hundredths\""),
			"New MPO output declares the standard currency unit so legacy migration cannot apply twice");
		java.util.Map<String, byte[]> unknownCostUnitEntries = new java.util.HashMap<>(savedEntries);
		byte[] unknownCostMeta = currentMeta.replace("mspdi-hundredths", "unknown-units")
			.getBytes(StandardCharsets.UTF_8);
		unknownCostUnitEntries.put(MpoFileImporter.META_ENTRY, unknownCostMeta);
		updateManifestChecksum(unknownCostUnitEntries, MpoFileImporter.META_ENTRY, unknownCostMeta);
		assertThrows(java.io.IOException.class, () -> loadFromBytes(zip(unknownCostUnitEntries).toByteArray()),
			"Unknown MPO currency-unit markers fail clearly before project import");
		java.util.Map<String, byte[]> legacyCostEntries = new java.util.HashMap<>(savedEntries);
		byte[] legacyProjectXml = legacyAssignmentCostXml(savedEntries.get(MpoFileImporter.PROJECT_ENTRY));
		legacyCostEntries.put(MpoFileImporter.PROJECT_ENTRY, legacyProjectXml);
		String legacyMeta = currentMeta.replace(" assignmentBaselineCostUnit=\"mspdi-hundredths\"", "");
		byte[] legacyMetaBytes = legacyMeta.getBytes(StandardCharsets.UTF_8);
		legacyCostEntries.put(MpoFileImporter.META_ENTRY, legacyMetaBytes);
		updateManifestChecksum(legacyCostEntries, MpoFileImporter.PROJECT_ENTRY, legacyProjectXml);
		updateManifestChecksum(legacyCostEntries, MpoFileImporter.META_ENTRY, legacyMetaBytes);
		String legacyManifest = new String(legacyCostEntries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8)
			.replaceFirst("(?s)(projectSha256=\")[^\"]*(\")", "$1" + sha256(legacyProjectXml) + "$2");
		legacyCostEntries.put(MpoFileImporter.MANIFEST_ENTRY, legacyManifest.getBytes(StandardCharsets.UTF_8));
		Project legacyCostReload = loadFromBytes(zip(legacyCostEntries).toByteArray());
		assertEquals(540.0d, ((NormalTask) findByName(legacyCostReload, "Mpo task"))
			.getBaselineCost(Snapshottable.BASELINE),
			0.001d, "A checksum-valid archive made with the released raw-row writer retains assignment baseline cost");
		com.fasterxml.jackson.databind.ObjectMapper baselineMapper = new com.fasterxml.jackson.databind.ObjectMapper();
		com.fasterxml.jackson.databind.JsonNode baselineMetadata = baselineMapper
			.readTree(savedEntries.get(MpoFileImporter.BASELINE_PROFILES_ENTRY));
		java.util.Map<String, byte[]> invalidSlotEntries = new java.util.HashMap<>(savedEntries);
		com.fasterxml.jackson.databind.node.ObjectNode invalidSlotMetadata = (com.fasterxml.jackson.databind.node.ObjectNode)
			baselineMetadata.deepCopy();
		((com.fasterxml.jackson.databind.node.ObjectNode) invalidSlotMetadata.path("tasks").get(0)).put("slot", 11);
		invalidSlotEntries.put(MpoFileImporter.BASELINE_PROFILES_ENTRY,
			baselineMapper.writeValueAsBytes(invalidSlotMetadata));
		assertThrows(java.io.IOException.class,
			() -> loadFromBytes(zip(invalidSlotEntries).toByteArray()),
			"The native baseline-profile codec must reject slots outside 0..10");
		java.util.Map<String, byte[]> invalidProfileEntries = new java.util.HashMap<>(savedEntries);
		com.fasterxml.jackson.databind.node.ObjectNode invalidProfileMetadata = (com.fasterxml.jackson.databind.node.ObjectNode)
			baselineMetadata.deepCopy();
		com.fasterxml.jackson.databind.node.ObjectNode profileRecord =
			(com.fasterxml.jackson.databind.node.ObjectNode) java.util.stream.StreamSupport
				.stream(invalidProfileMetadata.path("tasks").spliterator(), false)
				.filter(value -> value.path("profiles").size() == 2).findFirst().orElseThrow();
		((com.fasterxml.jackson.databind.node.ObjectNode) profileRecord.path("profiles").get(0)).put("profileUid", 0L);
		invalidProfileEntries.put(MpoFileImporter.BASELINE_PROFILES_ENTRY,
			baselineMapper.writeValueAsBytes(invalidProfileMetadata));
		assertThrows(java.io.IOException.class,
			() -> loadFromBytes(zip(invalidProfileEntries).toByteArray()),
			"The native baseline-profile codec must reject a zero profile identity");
		Project validationTarget = loadFromBytes(savedArchive);
		com.fasterxml.jackson.databind.node.ObjectNode fractionalUidMetadata = (com.fasterxml.jackson.databind.node.ObjectNode)
			baselineMetadata.deepCopy();
		com.fasterxml.jackson.databind.node.ObjectNode fractionalUidRecord =
			(com.fasterxml.jackson.databind.node.ObjectNode) java.util.stream.StreamSupport
				.stream(fractionalUidMetadata.path("tasks").spliterator(), false)
				.filter(value -> value.path("profiles").size() > 0).findFirst().orElseThrow();
		((com.fasterxml.jackson.databind.node.ObjectNode) fractionalUidRecord.path("profiles").get(0))
			.put("profileUid", 1.5d);
		assertMalformedBaselineMetadataPreservesTarget(validationTarget, baselineMapper, fractionalUidMetadata);
		com.fasterxml.jackson.databind.node.ObjectNode fractionalSlotMetadata = (com.fasterxml.jackson.databind.node.ObjectNode)
			baselineMetadata.deepCopy();
		((com.fasterxml.jackson.databind.node.ObjectNode) fractionalSlotMetadata.path("tasks").get(0)).put("slot", 0.5d);
		assertMalformedBaselineMetadataPreservesTarget(validationTarget, baselineMapper, fractionalSlotMetadata);
		com.fasterxml.jackson.databind.node.ObjectNode invalidCalendarMetadata =
			(com.fasterxml.jackson.databind.node.ObjectNode) baselineMetadata.deepCopy();
		com.fasterxml.jackson.databind.node.ObjectNode calendarRecord =
			(com.fasterxml.jackson.databind.node.ObjectNode) java.util.stream.StreamSupport
				.stream(invalidCalendarMetadata.path("tasks").spliterator(), false)
				.filter(value -> value.has("fixedCostCalendar")).findFirst().orElseThrow();
		((com.fasterxml.jackson.databind.node.ObjectNode) calendarRecord.path("fixedCostCalendar")
			.path("week").get(0)).remove("defined");
		assertMalformedBaselineMetadataPreservesTarget(validationTarget, baselineMapper, invalidCalendarMetadata);
		net.sf.mpxj.ProjectFile mspdi = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(savedEntries.get(MpoFileImporter.PROJECT_ENTRY)));
		assertEquals(700.0d, Double.parseDouble(taskBaselineXmlField(
			savedEntries.get(MpoFileImporter.PROJECT_ENTRY), "Mpo task", 0, "FixedCost")),
			"Summary fixed cost uses the standard MSPDI Baseline.FixedCost field");
		long profileResourceUid = baselineMetadata.path("tasks").findValues("resourceUid").stream()
			.filter(com.fasterxml.jackson.databind.JsonNode::isNumber).findFirst().orElseThrow().longValue();
		assertTrue(mspdi.getResources().stream().anyMatch(resourceData -> resourceData.getUniqueID() != null
			&& resourceData.getUniqueID().longValue() == profileResourceUid),
			"MPO baseline resource IDs must use the same canonical traversal and MSPDI mapping as XML export");
		assertTrue(mspdi.getTasks().stream().filter(task -> "Mpo task".equals(task.getName())
				&& task.getResourceAssignments() != null).findFirst().orElseThrow().getResourceAssignments().stream()
			.noneMatch(assignment -> assignment.getResource() != null
				&& assignment.getResource().getUniqueID().longValue() == profileResourceUid),
			"Detached baseline profiles must stay out of MSPDI current assignments");
		com.fasterxml.jackson.databind.JsonNode summaryRecord = null;
		for (com.fasterxml.jackson.databind.JsonNode record : baselineMetadata.path("tasks")) {
			if (record.path("slot").asInt(-1) == Snapshottable.BASELINE
					&& record.path("profiles").size() == 2) summaryRecord = record;
		}
		assertNotNull(summaryRecord, "The MPO sidecar must contain both detached summary profiles");
		var savedProfiles = summaryRecord.path("profiles");
		assertEquals(savedProfiles.get(0).path("resourceUid").longValue(),
			savedProfiles.get(1).path("resourceUid").longValue(), "The child profiles share one resource");
		assertNotEquals(savedProfiles.get(0).path("profileUid").longValue(),
			savedProfiles.get(1).path("profileUid").longValue(), "Contributor profiles retain independent IDs");
		var firstRolledProfile = rolledProfiles.stream().filter(profile -> java.util.Arrays.stream(profile.calendarCopy()
			.getExceptionDays()).anyMatch(value -> value.getStart() == firstCalendarException)).findFirst().orElseThrow();
		var secondRolledProfile = rolledProfiles.stream().filter(profile -> java.util.Arrays.stream(profile.calendarCopy()
			.getExceptionDays()).anyMatch(value -> value.getStart() == secondCalendarException)).findFirst().orElseThrow();
		com.fasterxml.jackson.databind.JsonNode savedFirstProfile = java.util.stream.StreamSupport
			.stream(savedProfiles.spliterator(), false)
			.filter(value -> value.path("profileUid").asLong(-1L) == firstRolledProfile.profileId())
			.findFirst().orElseThrow();
		com.fasterxml.jackson.databind.JsonNode savedSecondProfile = java.util.stream.StreamSupport
			.stream(savedProfiles.spliterator(), false)
			.filter(value -> value.path("profileUid").asLong(-1L) == secondRolledProfile.profileId())
			.findFirst().orElseThrow();
		assertTrue(savedFirstProfile.path("calendar").path("exceptions").findValues("start").stream()
			.anyMatch(value -> value.longValue() == firstCalendarException),
			"The first contributor keeps its captured calendar exception");
		assertTrue(savedSecondProfile.path("calendar").path("exceptions").findValues("start").stream()
			.anyMatch(value -> value.longValue() == secondCalendarException),
			"The second contributor keeps its captured calendar exception");
		Project reopened = loadFromBytes(savedArchive);
		NormalTask restored = (NormalTask) findByName(reopened, "Mpo task");
		TaskSnapshot restoredBaseline = (TaskSnapshot) restored.getSnapshot(Integer.valueOf(Snapshottable.BASELINE));
		assertNotNull(restoredBaseline);
		assertEquals(2, restoredBaseline.getAssignments().size(),
			"MPO reload must preserve both same-resource contributor rows as baseline-only profiles");
		assertEquals(27L * 60L * 60L * 1000L, restoredBaseline.work(0L, Long.MAX_VALUE),
			"MPO reload preserves the contributors' regular and overtime baseline work");
		assertEquals(7.0d, restoredBaseline.getFixedCost());
		double restoredAssignmentIntervalCost = restoredBaseline.getAssignments().stream().map(Assignment.class::cast)
			.mapToDouble(value -> value.baselineCost(baselineWindowStart, baselineWindowEnd)).sum();
		assertEquals(40.0d, restored.getBaselineCost(Snapshottable.BASELINE,
			baselineWindowStart, baselineWindowEnd) - restoredAssignmentIntervalCost, 0.001d,
			"MPO reload preserves the rolled-up total fixed-cost contribution profiles");
		assertEquals(540.0d, restored.getBaselineCost(Snapshottable.BASELINE,
			baselineWindowStart, baselineWindowEnd), 0.001d,
			"MPO reload preserves the 500 assignment plus 40 fixed-cost summary baseline total");
		assertEquals(com.microproject.pm.costing.Accrual.Kind.START.code(), restoredBaseline.getFixedCostAccrual());
		assertEquals(0, restored.getAssignments().stream()
			.filter(value -> !((Assignment) value).isDefault()).count(),
			"Saved baseline profiles must not become current assignments");
		assertEquals(500.0D, restoredBaseline.getAssignments().stream().map(Assignment.class::cast)
			.mapToDouble(value -> value.calcAll(Assignment.COST)).sum(), 0.00001D,
			"MPO reload retains both contributors' absolute baseline cost contours");
		java.util.Set<Long> restoredCalendarExceptions = new java.util.HashSet<>();
		for (Object value : restoredBaseline.getAssignments()) {
			Assignment profile = (Assignment) value;
			com.microproject.pm.calendar.WorkingCalendar profileCalendar =
				(com.microproject.pm.calendar.WorkingCalendar) profile.getEffectiveWorkCalendar();
			for (com.microproject.pm.calendar.WorkDay exception : profileCalendar.getConcreteInstance().getExceptionDays()) {
				restoredCalendarExceptions.add(Long.valueOf(exception.getStart()));
				if (exception.getStart() == firstCalendarException) {
					assertEquals(5.0D, profile.getRate().getValue(),
						"MPO must retain the first saved rate after its live assignment rate changed");
					assertEquals(60L * 60L * 1000L, profile.getDetail().getOvertimeWork());
				} else if (exception.getStart() == secondCalendarException) {
					assertEquals(3.0D, profile.getRate().getValue());
					assertEquals(2L * 60L * 60L * 1000L, profile.getDetail().getOvertimeWork());
				}
			}
		}
		assertTrue(restoredCalendarExceptions.contains(Long.valueOf(firstCalendarException)));
		assertTrue(restoredCalendarExceptions.contains(Long.valueOf(secondCalendarException)));
	}

	@Test
	void fixedCostOnlyBaselineSlotsRoundTripThroughMspdiAndMpo() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(project);
		task.setFixedCost(99.0d);
		int[] accruals = {
			com.microproject.pm.costing.Accrual.Kind.START.code(),
			com.microproject.pm.costing.Accrual.Kind.PRORATED.code(),
			com.microproject.pm.costing.Accrual.Kind.END.code(),
			com.microproject.pm.costing.Accrual.Kind.START.code(),
			com.microproject.pm.costing.Accrual.Kind.PRORATED.code(),
			com.microproject.pm.costing.Accrual.Kind.END.code(),
			com.microproject.pm.costing.Accrual.Kind.START.code(),
			com.microproject.pm.costing.Accrual.Kind.PRORATED.code(),
			com.microproject.pm.costing.Accrual.Kind.END.code(),
			com.microproject.pm.costing.Accrual.Kind.START.code(),
			com.microproject.pm.costing.Accrual.Kind.END.code()
		};
		for (int slot = 0; slot <= 10; slot++) {
			TaskSnapshot snapshot = new TaskSnapshot();
			snapshot.getHasAssignments();
			com.microproject.pm.criticalpath.TaskSchedule schedule = new com.microproject.pm.criticalpath.TaskSchedule(
				task, com.microproject.pm.criticalpath.TaskSchedule.Kind.CURRENT);
			schedule.setStart(0L);
			schedule.setFinish(0L);
			schedule.setRawDuration(0L);
			snapshot.setCurrentSchedule(schedule);
			snapshot.setFixedCost(slot == 0 ? 0.0d : slot * 10.0d);
			snapshot.setFixedCostAccrual(accruals[slot]);
			task.setSnapshot(Integer.valueOf(slot), snapshot);
		}

		byte[] archive = saveProjectBytes(project);
		byte[] projectXml = readEntries(archive).get(MpoFileImporter.PROJECT_ENTRY);
		net.sf.mpxj.ProjectFile mspdi = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(projectXml));
		net.sf.mpxj.Task xmlTask = mspdi.getTasks().stream()
			.filter(candidate -> "Mpo task".equals(candidate.getName())).findFirst().orElseThrow();
		for (int slot = 0; slot <= 10; slot++) {
			assertEquals(slot == 0 ? 0.0d : slot * 1000.0d,
				Double.parseDouble(taskBaselineXmlField(projectXml, "Mpo task", slot, "FixedCost")));
			assertTrue((slot == 0 ? xmlTask.getBaselineStart() : xmlTask.getBaselineStart(slot)) == null,
				"Cost-only snapshots must not gain a baseline start date");
			assertTrue((slot == 0 ? xmlTask.getBaselineFinish() : xmlTask.getBaselineFinish(slot)) == null,
				"Cost-only snapshots must not gain a baseline finish date");
		}

		Project reopened = loadFromBytes(archive);
		NormalTask restored = (NormalTask) findByName(reopened, "Mpo task");
		for (int slot = 0; slot <= 10; slot++) {
			TaskSnapshot snapshot = (TaskSnapshot) restored.getSnapshot(Integer.valueOf(slot));
			assertNotNull(snapshot, "Cost-only baseline slot must import: " + slot);
			assertEquals(slot == 0 ? 0.0d : slot * 10.0d, snapshot.getFixedCost());
			assertEquals(accruals[slot], snapshot.getFixedCostAccrual());
			assertEquals(0L, snapshot.getCurrentSchedule().getStart());
			assertEquals(0L, snapshot.getCurrentSchedule().getFinish());
		}
	}

	@Test
	void baselineProratedFixedCostCalendarSurvivesCalendarChangeAndMpoRoundTrip() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(project);
		long start = project.getStartDate();
		long hour = 60L * 60L * 1000L;
		task.getCurrentSchedule().setStart(start);
		task.getCurrentSchedule().setFinish(start + 8L * hour);
		task.getCurrentSchedule().setRawDuration(8L * hour);
		task.setFixedCost(240.0d);
		task.setFixedCostAccrual(com.microproject.pm.costing.Accrual.Kind.PRORATED);
		int slot = com.microproject.pm.snapshot.Snapshottable.BASELINE_1;
		task.saveCurrentToSnapshot(Integer.valueOf(slot));
		double expectedFirstHalf = task.getBaselineCost(slot, start, start + 4L * hour);
		assertEquals(120.0d, expectedFirstHalf, 0.001d);

		com.microproject.pm.calendar.WorkingCalendar changed =
			((com.microproject.pm.calendar.WorkingCalendar) task.getEffectiveWorkCalendar()).copyForBaselineSnapshot();
		for (int day = 0; day < com.microproject.pm.calendar.WorkWeek.DAYS_IN_WEEK; day++)
			changed.setWeekDay(day, null);
		task.setWorkCalendar(changed);
		assertEquals(expectedFirstHalf, task.getBaselineCost(slot, start, start + 4L * hour), 0.001d,
			"Live calendar changes must not alter a saved baseline's fixed-cost profile");

		Project reopened = loadFromBytes(saveProjectBytes(project));
		NormalTask restored = (NormalTask) firstTask(reopened);
		assertEquals(expectedFirstHalf, restored.getBaselineCost(slot, start, start + 4L * hour), 0.001d,
			"MPO must restore the captured baseline fixed-cost calendar and accrual");
		assertEquals(240.0d, restored.getBaselineCost(slot, start - 1L, start + 8L * hour + 1L), 0.001d,
			"The complete baseline interval must retain the full prorated fixed cost");
	}

	private static String taskBaselineXmlField(byte[] xml, String taskName, int slot, String field)
		throws Exception {
		javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
		String namespace = document.getDocumentElement().getNamespaceURI();
		org.w3c.dom.NodeList tasks = document.getElementsByTagNameNS(namespace, "Task");
		for (int index = 0; index < tasks.getLength(); index++) {
			org.w3c.dom.Element task = (org.w3c.dom.Element) tasks.item(index);
			if (!taskName.equals(xmlDirectChildText(task, "Name", namespace))) continue;
			org.w3c.dom.NodeList children = task.getElementsByTagNameNS(namespace, "Baseline");
			for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
				org.w3c.dom.Element baseline = (org.w3c.dom.Element) children.item(childIndex);
				if (!Integer.toString(slot).equals(xmlDirectChildText(baseline, "Number", namespace))) continue;
				return xmlDirectChildText(baseline, field, namespace);
			}
			return null;
		}
		throw new AssertionError("MSPDI task not found: " + taskName);
	}

	private static String xmlDirectChildText(org.w3c.dom.Element parent, String name, String namespace) {
		for (org.w3c.dom.Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof org.w3c.dom.Element element
					&& name.equals(element.getLocalName())
					&& java.util.Objects.equals(namespace, element.getNamespaceURI()))
				return element.getTextContent();
		}
		return null;
	}

	@Test
	void nativeMpoPreservesExactTaskScheduleSnapshot() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		NormalTask secondTask = (NormalTask) original.createLocalTaskNode(null).getImpl();
		secondTask.setName("Materialized after blank row");
		secondTask.setUniqueId(9_823_471L);
		com.microproject.grouping.core.model.NodeModel taskModel = original.getTaskModel();
		com.microproject.grouping.core.Node secondNode = taskModel.search(secondTask);
		com.microproject.grouping.core.Node blankRow = NodeFactory.getInstance().createVoidNode();
		Object outlineRoot = taskModel.getHierarchy().getRoot();
		taskModel.add((com.microproject.grouping.core.Node) outlineRoot, blankRow,
			taskModel.getIndexOfChild(outlineRoot, secondNode),
			com.microproject.grouping.core.model.NodeModel.SILENT);
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(3L * day);
		secondTask.setDuration(2L * day);
		Resource resource = original.getResourcePool().newResourceInstance();
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L,
			MpoFileImporterTest.class);
		assignment.setWork(3L * day, null);
		long start = task.getStart() - day;
		long finish = start + 4L * day;
		long secondStart = secondTask.getStart() - 2L * day;
		long secondFinish = secondStart + 5L * day;
		long constraintDate = start + 30L * 60L * 1000L;
		task.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, constraintDate);
		task.getCurrentSchedule().setStart(start);
		task.getCurrentSchedule().setFinish(finish);
		task.setActualStartNoEvent(start);
		secondTask.getCurrentSchedule().setStart(secondStart);
		secondTask.getCurrentSchedule().setFinish(secondFinish);

		byte[] archive = saveProjectBytes(original);
		String projectXml = new String(readEntries(archive).get(MpoFileImporter.PROJECT_ENTRY), StandardCharsets.UTF_8);
		assertTrue(projectXml.contains("<IsNull>1</IsNull>"), "the test fixture must export its interleaved blank row");
		Project reopened = loadFromBytes(archive);
		NormalTask restored = (NormalTask) firstTask(reopened);
		NormalTask restoredSecond = null;
		for (java.util.Iterator<?> tasks = reopened.getTaskOutlineIterator(); tasks.hasNext();) {
			Task candidate = (Task) tasks.next();
			if ("Materialized after blank row".equals(candidate.getName())) restoredSecond = (NormalTask) candidate;
		}
		assertEquals(start, restored.getStart());
		assertEquals(finish, restored.getEnd());
		assertEquals(start, restored.getActualStart());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, restored.getConstraintTypeKind());
		assertEquals(constraintDate, restored.getConstraintDate());
		assertNotNull(restoredSecond, "the high-runtime-UID task after a blank row must be restored");
		assertEquals(secondStart, restoredSecond.getStart());
		assertEquals(secondFinish, restoredSecond.getEnd());
	}

	@Test
	void mpoTaskCreateOperationCanBeReplayedWithoutChangingTheResult() throws Exception {
		Project project = projectForRoundTrip();
		String operationId = java.util.UUID.randomUUID().toString();
		OperationLog.Operation operation = new OperationLog.Operation(operationId,
			java.util.UUID.randomUUID().toString(), 1L, java.util.Set.of(), "task.create",
			java.util.UUID.randomUUID().toString(), Map.of("legacyUniqueId", 987654321L,
				"name", "Idempotent task", "notes", "", "percentComplete", 0D));
		com.microproject.collaboration.MpoTaskOperationService service = new com.microproject.collaboration.MpoTaskOperationService();
		int taskCountBefore = project.getTaskList().size();

		service.apply(project, List.of(operation));
		Task created = project.findByUniqueId(987654321L);
		service.apply(project, List.of(operation));

		assertEquals(taskCountBefore + 1, project.getTaskList().size());
		assertSame(created, project.findByUniqueId(987654321L));
		assertEquals("Idempotent task", created.getName());
	}

	@Test
	void mpoDocumentIdentitySurvivesSaveReloadSave() throws Exception {
		Project original = projectForRoundTrip();
		ByteArrayOutputStream first = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(original, first);
		String firstManifest = new String(readEntries(first.toByteArray()).get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		String firstDocumentId = firstManifest.replaceFirst("(?s).*documentId=\\\"([0-9a-f-]{36})\\\".*", "$1");
		org.junit.jupiter.api.Assertions.assertNotEquals(firstManifest, firstDocumentId, "MPO must contain a UUID document identity");

		Project reopened = loadFromBytes(first.toByteArray());
		org.junit.jupiter.api.Assertions.assertEquals(firstDocumentId, reopened.getDocumentId());
		ByteArrayOutputStream second = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(reopened, second);
		String secondManifest = new String(readEntries(second.toByteArray()).get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		String secondDocumentId = secondManifest.replaceFirst("(?s).*documentId=\\\"([0-9a-f-]{36})\\\".*", "$1");
		org.junit.jupiter.api.Assertions.assertEquals(firstDocumentId, secondDocumentId);
	}

	@Test
	void repeatedMpoSaveUsesDeterministicArchiveEntryOrder() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream first = new ByteArrayOutputStream();
		ByteArrayOutputStream second = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, first);
		new MpoFileImporter().saveProject(project, second);
		Map<String, byte[]> firstEntries = readEntries(first.toByteArray());
		Map<String, byte[]> secondEntries = readEntries(second.toByteArray());
		assertEquals(firstEntries.keySet(), secondEntries.keySet());
	}

	@Test
	void masterMpoEmbedsLinkedProjectAndRestoresAUsableReference() throws Exception {
		Project child = projectForRoundTrip();
		File childFile = File.createTempFile("mpo-linked-child-", ".mpo");
		childFile.deleteOnExit();
		child.setFileName(childFile.getAbsolutePath());
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(childFile)) {
			new MpoFileImporter().saveProject(child, output);
		}

		Project master = projectForRoundTrip();
		master.setMaster(true);
		DefaultSubProj reference = new DefaultSubProj(master, child.getUniqueId());
		reference.setReferenceId("00000000-0000-0000-0000-000000000081");
		reference.setName("Embedded child");
		reference.setSubprojectFile(childFile.getAbsolutePath());
		master.connectTask(reference);
		master.addToDefaultOutline(null, NodeFactory.getInstance().createNode(reference));

		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(master, archive);
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		String embeddedEntry = entries.keySet().stream()
				.filter(name -> name.startsWith(MpoFileImporter.EMBEDDED_PROJECT_PREFIX)).findFirst().orElseThrow();
		org.junit.jupiter.api.Assertions.assertArrayEquals(java.nio.file.Files.readAllBytes(childFile.toPath()), entries.get(embeddedEntry));

		Project reopened = loadFromBytes(archive.toByteArray());
		DefaultSubProj restored = null;
		for (java.util.Iterator<?> tasks = reopened.getTaskOutlineIterator(); tasks.hasNext();) {
			Object task = tasks.next();
			if (task instanceof DefaultSubProj value) { restored = value; break; }
		}
		org.junit.jupiter.api.Assertions.assertNotNull(restored);
		org.junit.jupiter.api.Assertions.assertEquals(reference.getReferenceId(), restored.getReferenceId());
		org.junit.jupiter.api.Assertions.assertTrue(new File(restored.getSubprojectFile()).isFile());
		org.junit.jupiter.api.Assertions.assertArrayEquals(java.nio.file.Files.readAllBytes(childFile.toPath()),
				java.nio.file.Files.readAllBytes(new File(restored.getSubprojectFile()).toPath()));
	}

	@Test
	void masterMpoPersistsLinkedChildReplacementFromUpdatedChildFile() throws Exception {
		File childFile = File.createTempFile("mpo-replaced-child-", ".mpo");
		childFile.deleteOnExit();
		ProjectFactory factory = ProjectFactory.getInstance();
		java.lang.reflect.Field portfolioField = ProjectFactory.class.getDeclaredField("portfolio");
		portfolioField.setAccessible(true);
		Object originalPortfolio = portfolioField.get(factory);
		com.microproject.pm.resource.ResourcePoolFactory resourcePoolFactory = com.microproject.pm.resource.ResourcePoolFactory.getInstance();
		List<com.microproject.pm.resource.ResourcePool> originalPools = new ArrayList<>(resourcePoolFactory.getResourcePools());
		Project originalChild = null;
		Project master = null;
		Project replacement = null;
		try {
			portfolioField.set(factory, new com.microproject.pm.task.Portfolio(factory));
			originalChild = projectForRoundTrip();
			master = projectForRoundTrip();
			originalChild.setUniqueId(java.util.UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
			master.setUniqueId(java.util.UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
			originalChild.setFileName(childFile.getAbsolutePath());
			NormalTask staleTask = (NormalTask) originalChild.createLocalTaskNode(null).getImpl();
			staleTask.setName("Stale child task");
			try (java.io.FileOutputStream output = new java.io.FileOutputStream(childFile)) {
				new MpoFileImporter().saveProject(originalChild, output);
			}

			master.setMaster(true);
			DefaultSubProj reference = new DefaultSubProj(master, originalChild.getUniqueId());
			reference.setReferenceId("00000000-0000-0000-0000-000000000082");
			reference.setLoadStatus(com.microproject.pm.task.SubProj.LoadStatus.INVALID);
			reference.setSubprojectFile(childFile.getAbsolutePath());
			master.connectTask(reference);
			com.microproject.grouping.core.Node referenceNode = NodeFactory.getInstance().createNode(reference);
			master.addToDefaultOutline(null, referenceNode);
			factory.addProject(master, false, false);
			factory.addProject(originalChild, false, false);
			master.getSubprojectHandler().addSubproject(originalChild, referenceNode, true, false);
			reference.setLoadStatus(com.microproject.pm.task.SubProj.LoadStatus.INVALID);

			Project diskChild = projectForRoundTrip();
			diskChild.setFileName(childFile.getAbsolutePath());
			NormalTask replacementTask = (NormalTask) diskChild.createLocalTaskNode(null).getImpl();
			replacementTask.setName("Replacement child task");
			try (java.io.FileOutputStream output = new java.io.FileOutputStream(childFile)) {
				new MpoFileImporter().saveProject(diskChild, output);
			}
			byte[] updatedChildBytes = Files.readAllBytes(childFile.toPath());
			replacement = loadFromBytes(updatedChildBytes);

			assertSame(originalChild, factory.getPortfolio().findByUniqueId(originalChild.getUniqueId()));
			assertTrue(factory.replaceOpenSubproject(master, reference, replacement));
			assertSame(replacement, reference.getSubproject());
			Task loadedReplacementTask = findByName(replacement, "Replacement child task");
			assertNotNull(loadedReplacementTask);
			assertFalse(master.getTasks().stream().anyMatch(task -> "Stale child task".equals(task.getName())));

			ByteArrayOutputStream savedMaster = new ByteArrayOutputStream();
			new MpoFileImporter().saveProject(master, savedMaster);
			Project restoredMaster = loadFromBytes(savedMaster.toByteArray());
			DefaultSubProj restoredReference = findSubproject(restoredMaster);
			assertEquals(reference.getReferenceId(), restoredReference.getReferenceId());
			File extractedChild = new File(restoredReference.getSubprojectFile());
			assertTrue(extractedChild.isFile());
			assertArrayEquals(updatedChildBytes, Files.readAllBytes(extractedChild.toPath()),
					"master MPO embeds the child MPO already written to the linked file");
			Project restoredChild = loadFromBytes(Files.readAllBytes(extractedChild.toPath()));
			Task restoredReplacementTask = findByName(restoredChild, "Replacement child task");
			assertNotNull(restoredReplacementTask);
			assertEquals(loadedReplacementTask.getUniqueId(), restoredReplacementTask.getUniqueId());
			assertFalse(restoredChild.getTasks().stream().anyMatch(task -> "Stale child task".equals(task.getName())));
		} finally {
			MpoExtractionOwnershipRegistry.closeAll();
			if (originalChild != null) originalChild.getResourcePool().removeProject(originalChild);
			if (master != null) master.getResourcePool().removeProject(master);
			if (replacement != null) replacement.getResourcePool().removeProject(replacement);
			portfolioField.set(factory, originalPortfolio);
			resourcePoolFactory.getResourcePools().removeIf(pool -> !originalPools.contains(pool));
			Files.deleteIfExists(childFile.toPath());
		}
	}

	@Test
	void fileImportOwnsExtractionUntilTheLoadedProjectIsClosed() throws Exception {
		File childFile = File.createTempFile("mpo-lifecycle-child-", ".mpo");
		File masterFile = File.createTempFile("mpo-lifecycle-master-", ".mpo");
		java.nio.file.Path extractionRoot = java.nio.file.Files.createTempDirectory("mpo-lifecycle-workspace-");
		try {
			Project child = projectForRoundTrip();
			child.setFileName(childFile.getAbsolutePath());
			MpoFileImporter childWriter = new MpoFileImporter();
			childWriter.setFileName(childFile.getAbsolutePath());
			childWriter.setProject(child);
			childWriter.exportFile();

			Project master = projectForRoundTrip();
			master.setMaster(true);
			addEmbeddedReference(master, child, childFile);
			MpoFileImporter masterWriter = new MpoFileImporter();
			masterWriter.setFileName(masterFile.getAbsolutePath());
			masterWriter.setProject(master);
			masterWriter.exportFile();

			MpoFileImporter.setExtractionWorkspaceRoot(extractionRoot);
			MpoFileImporter reader = new MpoFileImporter();
			reader.setFileName(masterFile.getAbsolutePath());
			reader.setProjectFactory(ProjectFactory.getInstance());
			reader.importFile();
			Project loaded = reader.getProject();
			DefaultSubProj restored = findSubproject(loaded);
			File extracted = new File(restored.getSubprojectFile());
			assertTrue(extracted.isFile(), "real file import must materialize the embedded child");
			assertEquals(1, MpoExtractionOwnershipRegistry.size());

			// This is the same ownership boundary used by DocumentFrame/GraphicManager
			// when the loaded project document is closed.
			assertTrue(MpoExtractionOwnershipRegistry.close(loaded));
			assertTrue(!extracted.exists(), "closing the project must remove extracted children");
			assertEquals(0, MpoExtractionOwnershipRegistry.size());
		} finally {
			MpoExtractionOwnershipRegistry.closeAll();
			MpoFileImporter.setExtractionWorkspaceRoot(null);
			try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(extractionRoot)) {
				paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
					try { java.nio.file.Files.deleteIfExists(path); } catch (IOException ignored) { }
				});
			}
			java.nio.file.Files.deleteIfExists(childFile.toPath());
			java.nio.file.Files.deleteIfExists(masterFile.toPath());
		}
	}

	@Test
	void masterMpoKeepsTheMasterOpenWhenAnEmbeddedProjectIsTampered() throws Exception {
		Project child = projectForRoundTrip();
		File childFile = File.createTempFile("mpo-linked-child-", ".mpo");
		childFile.deleteOnExit();
		child.setFileName(childFile.getAbsolutePath());
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(childFile)) {
			new MpoFileImporter().saveProject(child, output);
		}
		Project master = projectForRoundTrip();
		master.setMaster(true);
		DefaultSubProj reference = new DefaultSubProj(master, child.getUniqueId());
		reference.setSubprojectFile(childFile.getAbsolutePath());
		master.connectTask(reference);
		master.addToDefaultOutline(null, NodeFactory.getInstance().createNode(reference));
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(master, archive);
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		String embeddedEntry = entries.keySet().stream()
				.filter(name -> name.startsWith(MpoFileImporter.EMBEDDED_PROJECT_PREFIX)).findFirst().orElseThrow();
		entries.put(embeddedEntry, new byte[] { 1, 2, 3 });
		Project reopened = loadFromBytes(zip(entries).toByteArray());
		DefaultSubProj restored = findSubproject(reopened);
		org.junit.jupiter.api.Assertions.assertNotNull(restored);
		assertEquals(com.microproject.pm.task.SubProj.LoadStatus.INVALID, restored.getLoadStatus());
	}

	@Test
	void masterMpoKeepsTheMasterOpenWhenAnEmbeddedProjectEntryIsMissing() throws Exception {
		Project child = projectForRoundTrip();
		File childFile = File.createTempFile("mpo-missing-child-", ".mpo");
		childFile.deleteOnExit();
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(childFile)) {
			new MpoFileImporter().saveProject(child, output);
		}
		Project master = projectForRoundTrip();
		master.setMaster(true);
		addEmbeddedReference(master, child, childFile);
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(master, archive);
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		entries.keySet().removeIf(name -> name.startsWith(MpoFileImporter.EMBEDDED_PROJECT_PREFIX));
		Project reopened = loadFromBytes(zip(entries).toByteArray());
		assertEquals(com.microproject.pm.task.SubProj.LoadStatus.MISSING, findSubproject(reopened).getLoadStatus());
	}

	@Test
	void masterMpoRestoresAValidChildWhenAnotherEmbeddedChildIsTampered() throws Exception {
		Project validChild = projectForRoundTrip();
		File validChildFile = File.createTempFile("mpo-valid-child-", ".mpo");
		validChildFile.deleteOnExit();
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(validChildFile)) {
			new MpoFileImporter().saveProject(validChild, output);
		}
		Project invalidChild = projectForRoundTrip();
		File invalidChildFile = File.createTempFile("mpo-invalid-child-", ".mpo");
		invalidChildFile.deleteOnExit();
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(invalidChildFile)) {
			new MpoFileImporter().saveProject(invalidChild, output);
		}
		Project master = projectForRoundTrip();
		master.setMaster(true);
		addEmbeddedReference(master, validChild, validChildFile);
		addEmbeddedReference(master, invalidChild, invalidChildFile);
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(master, archive);
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		String invalidEntry = entries.keySet().stream()
				.filter(name -> name.startsWith(MpoFileImporter.EMBEDDED_PROJECT_PREFIX + "2-")).findFirst().orElseThrow();
		entries.put(invalidEntry, new byte[] { 1, 2, 3 });
		Project reopened = loadFromBytes(zip(entries).toByteArray());
		java.util.List<DefaultSubProj> references = new java.util.ArrayList<>();
		for (java.util.Iterator<?> tasks = reopened.getTaskOutlineIterator(); tasks.hasNext();) {
			Object task = tasks.next();
			if (task instanceof DefaultSubProj reference) references.add(reference);
		}
		assertEquals(2, references.size());
		org.junit.jupiter.api.Assertions.assertTrue(references.stream().anyMatch(reference -> reference.getLoadStatus() == com.microproject.pm.task.SubProj.LoadStatus.INVALID));
		org.junit.jupiter.api.Assertions.assertTrue(references.stream().anyMatch(reference -> reference.getLoadStatus() == com.microproject.pm.task.SubProj.LoadStatus.NOT_LOADED
				&& new File(reference.getSubprojectFile()).isFile()));
	}

	@Test
	void masterMpoMarksAWellChecksummedButMalformedChildInvalid() throws Exception {
		Project child = projectForRoundTrip();
		File childFile = File.createTempFile("mpo-malformed-child-", ".mpo");
		childFile.deleteOnExit();
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(childFile)) {
			new MpoFileImporter().saveProject(child, output);
		}
		Project master = projectForRoundTrip();
		master.setMaster(true);
		addEmbeddedReference(master, child, childFile);
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(master, archive);
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		String embeddedEntry = entries.keySet().stream()
				.filter(name -> name.startsWith(MpoFileImporter.EMBEDDED_PROJECT_PREFIX)).findFirst().orElseThrow();
		Map<String, byte[]> malformed = readEntries(entries.get(embeddedEntry));
		byte[] malformedXml = "<Project>".getBytes(StandardCharsets.UTF_8);
		malformed.put(MpoFileImporter.PROJECT_ENTRY, malformedXml);
		malformed.put(MpoFileImporter.MANIFEST_ENTRY,
				MpoFileImporter.manifestFor(malformedXml).getBytes(StandardCharsets.UTF_8));
		byte[] malformedArchive = zip(malformed).toByteArray();
		String outerManifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		outerManifest = outerManifest.replace(sha256(childFile), sha256(malformedArchive));
		entries.put(MpoFileImporter.MANIFEST_ENTRY, outerManifest.getBytes(StandardCharsets.UTF_8));
		entries.put(embeddedEntry, malformedArchive);

		Project reopened = loadFromBytes(zip(entries).toByteArray());
		assertEquals(com.microproject.pm.task.SubProj.LoadStatus.INVALID, findSubproject(reopened).getLoadStatus());
	}

	@Test
	void masterMpoMarksAChildWithTamperedStandardMetadataInvalid() throws Exception {
		Project child = projectForRoundTrip();
		File childFile = File.createTempFile("mpo-child-metadata-", ".mpo");
		childFile.deleteOnExit();
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(childFile)) {
			new MpoFileImporter().saveProject(child, output);
		}
		Project master = projectForRoundTrip();
		master.setMaster(true);
		addEmbeddedReference(master, child, childFile);
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(master, archive);
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		String embeddedEntry = entries.keySet().stream()
				.filter(name -> name.startsWith(MpoFileImporter.EMBEDDED_PROJECT_PREFIX)).findFirst().orElseThrow();
		Map<String, byte[]> altered = readEntries(entries.get(embeddedEntry));
		altered.put("meta.xml", "<meta formatVersion=\"1.0\" tampered=\"true\"/>".getBytes(StandardCharsets.UTF_8));
		byte[] alteredArchive = zip(altered).toByteArray();
		String outerManifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8)
				.replace(sha256(childFile), sha256(alteredArchive));
		entries.put(MpoFileImporter.MANIFEST_ENTRY, outerManifest.getBytes(StandardCharsets.UTF_8));
		entries.put(embeddedEntry, alteredArchive);

		Project reopened = loadFromBytes(zip(entries).toByteArray());
		assertEquals(com.microproject.pm.task.SubProj.LoadStatus.INVALID, findSubproject(reopened).getLoadStatus());
	}

	@Test
	void mpoEmbedsAndRestoresTheReferencedSharedResourcePoolFile() throws Exception {
		Project pool = projectForRoundTrip();
		File poolFile = File.createTempFile("mpo-resource-pool-", ".mpo");
		poolFile.deleteOnExit();
		pool.setFileName(poolFile.getAbsolutePath());
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(poolFile)) {
			new MpoFileImporter().saveProject(pool, output);
		}
		Project sharer = projectForRoundTrip();
		sharer.setSharedResourcePoolFile(poolFile.getAbsolutePath());
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(sharer, archive);

		Project reopened = loadFromBytes(archive.toByteArray());
		org.junit.jupiter.api.Assertions.assertTrue(new File(reopened.getSharedResourcePoolFile()).isFile());
		org.junit.jupiter.api.Assertions.assertArrayEquals(java.nio.file.Files.readAllBytes(poolFile.toPath()),
				java.nio.file.Files.readAllBytes(new File(reopened.getSharedResourcePoolFile()).toPath()));
	}

	@Test
	void sharedPoolSharerRoundTripPreservesResourceIdentityAndAssignmentUnits() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("shared-pool", undo);
		Resource resource = pool.createScriptedResource();
		resource.setResourceTypeKind(ResourceType.Kind.WORK);
		((com.microproject.pm.resource.ResourceImpl) resource).setUniqueId(88001L);
		Project poolProject = Project.createProject(pool, undo);
		File poolFile = File.createTempFile("mpo-shared-pool-", ".mpo");
		poolFile.deleteOnExit();
		poolProject.setFileName(poolFile.getAbsolutePath());
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(poolFile)) {
			new MpoFileImporter().saveProject(poolProject, output);
		}
		Project first = sharedSharer(pool, poolFile, resource, "First sharer task");
		Project second = sharedSharer(pool, poolFile, resource, "Second sharer task");
		SharedResourcePoolService.getInstance().share(first, poolProject,
				SharedResourcePoolService.ConflictPolicy.POOL_TAKES_PRECEDENCE);
		SharedResourcePoolService.getInstance().share(second, poolProject,
				SharedResourcePoolService.ConflictPolicy.POOL_TAKES_PRECEDENCE);
		File firstFile = File.createTempFile("mpo-shared-first-", ".mpo");
		File secondFile = File.createTempFile("mpo-shared-second-", ".mpo");
		firstFile.deleteOnExit();
		secondFile.deleteOnExit();
		first.setFileName(firstFile.getAbsolutePath());
		second.setFileName(secondFile.getAbsolutePath());
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(firstFile)) {
			new MpoFileImporter().saveProject(first, output);
		}
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(secondFile)) {
			new MpoFileImporter().saveProject(second, output);
		}
		Project reopenedPool = load(poolFile);
		reopenedPool.setFileName(poolFile.getAbsolutePath());
		Project reopenedFirst = load(firstFile);
		Project reopenedSecond = load(secondFile);
		Assignment firstAssignment = firstRealAssignment(reopenedFirst);
		Assignment secondAssignment = firstRealAssignment(reopenedSecond);
		assertEquals(88001L, firstAssignment.getResource().getUniqueId());
		assertEquals(firstAssignment.getResource().getUniqueId(), secondAssignment.getResource().getUniqueId());
		assertEquals(1D, firstAssignment.getUnits(), 0.0001D);
		assertEquals(1D, secondAssignment.getUnits(), 0.0001D);
		assertTrue(new File(reopenedFirst.getSharedResourcePoolFile()).isFile());
		assertTrue(new File(reopenedSecond.getSharedResourcePoolFile()).isFile());
		assertEquals(reopenedPool.getUniqueId(), reopenedFirst.getSharedResourcePoolProjectId());
		assertEquals(reopenedPool.getUniqueId(), reopenedSecond.getSharedResourcePoolProjectId());
		assertTrue(SharedResourcePoolService.getInstance().resolve(reopenedFirst,
				List.of(reopenedPool, reopenedSecond)));
		assertTrue(SharedResourcePoolService.getInstance().resolve(reopenedSecond,
				List.of(reopenedPool, reopenedFirst)));
		List<TeamPlannerService.Slot> slots = new TeamPlannerService().slots(reopenedPool);
		assertEquals(2, slots.size(), "restarted pool must aggregate both sharer assignments");
	}

	private static Project sharedSharer(ResourcePool pool, File poolFile, Resource resource, String taskName) {
		Project project = Project.createProject(pool, new DataFactoryUndoController());
		project.initialize(false, false);
		project.setSharedResourcePoolFile(poolFile.getAbsolutePath());
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName(taskName);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, MpoFileImporterTest.class);
		return project;
	}

	private static byte[] saveProjectBytes(Project project) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, output);
		return output.toByteArray();
	}

	private static void assertMalformedBaselineMetadataPreservesTarget(Project target,
		com.fasterxml.jackson.databind.ObjectMapper mapper, com.fasterxml.jackson.databind.JsonNode malformed) throws Exception {
		java.util.Map<String, java.util.List<Long>> originalProfileIds = new java.util.LinkedHashMap<>();
		java.util.Map<String, com.microproject.pm.calendar.WorkCalendar> originalFixedCostCalendars =
			new java.util.LinkedHashMap<>();
		for (com.fasterxml.jackson.databind.JsonNode taskNode : malformed.path("tasks")) {
			long taskUid = taskNode.path("taskUid").longValue();
			int slot = taskNode.path("slot").intValue();
			Task task = target.findByUniqueId(taskUid);
			if (!(task instanceof NormalTask normalTask)
					|| !(normalTask.getSnapshot(Integer.valueOf(slot)) instanceof TaskSnapshot snapshot)) continue;
			originalProfileIds.put(taskUid + ":" + slot, snapshot.captureBaselineProfiles(normalTask, slot).stream()
				.map(profile -> Long.valueOf(profile.profileId())).toList());
			if (snapshot.getFixedCostCalendar() != null)
				originalFixedCostCalendars.put(taskUid + ":" + slot, snapshot.getFixedCostCalendar());
		}
		java.lang.reflect.Method restore = MpoFileImporter.class.getDeclaredMethod("restoreBaselineProfiles",
			Project.class, byte[].class);
		restore.setAccessible(true);
		assertThrows(java.io.IOException.class, () -> {
			try {
				restore.invoke(null, target, mapper.writeValueAsBytes(malformed));
			} catch (java.lang.reflect.InvocationTargetException exception) {
				if (exception.getCause() instanceof java.io.IOException ioException) throw ioException;
				throw new RuntimeException(exception.getCause());
			}
		});
		for (com.fasterxml.jackson.databind.JsonNode taskNode : malformed.path("tasks")) {
			long taskUid = taskNode.path("taskUid").longValue();
			int slot = taskNode.path("slot").intValue();
			String key = taskUid + ":" + slot;
			if (!originalProfileIds.containsKey(key)) continue;
			NormalTask task = (NormalTask) target.findByUniqueId(taskUid);
			TaskSnapshot snapshot = (TaskSnapshot) task.getSnapshot(Integer.valueOf(slot));
			assertEquals(originalProfileIds.get(key), snapshot.captureBaselineProfiles(task, slot).stream()
				.map(profile -> Long.valueOf(profile.profileId())).toList(),
				"Malformed MPO sidecar data must not partially replace target baseline profiles");
			if (originalFixedCostCalendars.containsKey(key))
				assertSame(originalFixedCostCalendars.get(key), snapshot.getFixedCostCalendar(),
					"Malformed MPO sidecar data must not replace a saved baseline fixed-cost calendar");
		}
	}

	private static Assignment firstRealAssignment(Project project) {
		for (java.util.Iterator<?> iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Object value = iterator.next();
			if (value instanceof NormalTask task)
				for (Object candidate : task.getAssignments()) {
					Assignment assignment = (Assignment) candidate;
					if (!assignment.isDefault()) return assignment;
				}
		}
		throw new AssertionError("No real assignment was restored");
	}

	@Test
	void portableMasterPreservesChildCrossProjectAndSharedPoolReferencesAfterReopen() throws Exception {
		Project target = projectForRoundTrip();
		target.setUniqueId(9102L);
		File targetFile = File.createTempFile("mpo-portable-target-", ".mpo");
		targetFile.deleteOnExit();
		target.setFileName(targetFile.getAbsolutePath());
		NormalTask targetTask = (NormalTask) target.createLocalTaskNode(null).getImpl();
		targetTask.setName("External dependency endpoint");
		targetTask.setExternalProjectFile(targetFile.getAbsolutePath());
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(targetFile)) {
			new MpoFileImporter().saveProject(target, output);
		}

		Project source = projectForRoundTrip();
		source.setUniqueId(9101L);
		NormalTask sourceTask = (NormalTask) source.createLocalTaskNode(null).getImpl();
		sourceTask.setName("Local cross-project successor");
		DependencyService.getInstance().newDependency(targetTask, sourceTask,
				com.microproject.pm.dependency.DependencyType.Kind.FS.code(), 0L, this);
		source.setSharedResourcePoolFile(targetFile.getAbsolutePath());
		File sourceFile = File.createTempFile("mpo-portable-source-", ".mpo");
		sourceFile.deleteOnExit();
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(sourceFile)) {
			new MpoFileImporter().saveProject(source, output);
		}

		Project master = projectForRoundTrip();
		master.setUniqueId(9100L);
		master.setMaster(true);
		addEmbeddedReference(master, source, sourceFile);
		addEmbeddedReference(master, target, targetFile);
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(master, archive);
		Project reopened = loadFromBytes(archive.toByteArray());
		DefaultSubProj extractedSource = findSubprojectForId(reopened, source.getUniqueId());
		DefaultSubProj extractedTarget = findSubprojectForId(reopened, target.getUniqueId());
		Project reopenedSource;
		try (java.io.FileInputStream input = new java.io.FileInputStream(extractedSource.getSubprojectFile())) {
			reopenedSource = new MpoFileImporter().loadProject(input);
		}
		NormalTask restoredTask = null;
		for (java.util.Iterator<?> tasks = reopenedSource.getTaskOutlineIterator(); tasks.hasNext();) {
			Object value = tasks.next();
			if (value instanceof NormalTask task && "Local cross-project successor".equals(task.getName())) {
				restoredTask = task;
				break;
			}
		}
		org.junit.jupiter.api.Assertions.assertNotNull(restoredTask);
		org.junit.jupiter.api.Assertions.assertEquals(1, restoredTask.getPredecessorList().size());
		Task restoredExternal = (Task) ((com.microproject.pm.dependency.Dependency)
				restoredTask.getPredecessorList().iterator().next()).getPredecessor();
		org.junit.jupiter.api.Assertions.assertNotNull(restoredExternal.getExternalProjectFile(),
				() -> "restored external task=" + restoredExternal.getName() + ", external="
						+ restoredExternal.isExternal() + ", projectId=" + restoredExternal.getProjectId());
		File restoredExternalProject = new File(restoredExternal.getExternalProjectFile());
		org.junit.jupiter.api.Assertions.assertTrue(restoredExternalProject.isFile());
		org.junit.jupiter.api.Assertions.assertArrayEquals(java.nio.file.Files.readAllBytes(targetFile.toPath()),
				java.nio.file.Files.readAllBytes(restoredExternalProject.toPath()));
		File restoredPool = new File(reopenedSource.getSharedResourcePoolFile());
		org.junit.jupiter.api.Assertions.assertTrue(restoredPool.isFile());
		org.junit.jupiter.api.Assertions.assertArrayEquals(java.nio.file.Files.readAllBytes(targetFile.toPath()),
				java.nio.file.Files.readAllBytes(restoredPool.toPath()));
	}
	@Test
	void manifestValidatesTheExactProjectPayload() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		assertDoesNotThrow(() -> MpoFileImporter.validateManifest(MpoFileImporter.manifestFor(projectXml), projectXml));
	}

	@Test
	void manifestRejectsChangedProjectPayload() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		byte[] changedXml = "<Project><Name>changed</Name></Project>".getBytes(StandardCharsets.UTF_8);
		assertThrows(IOException.class, () -> MpoFileImporter.validateManifest(MpoFileImporter.manifestFor(projectXml), changedXml));
	}

	@Test
	void mpoRejectsTamperedStandardArchiveEntry() throws Exception {
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(projectForRoundTrip(), generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		entries.put("meta.xml", "<meta formatVersion=\"1.0\" tampered=\"true\"/>".getBytes(StandardCharsets.UTF_8));
		assertThrows(IOException.class, () -> loadFromBytes(zip(entries).toByteArray()));
	}

	@Test
	void manifestRejectsUnsupportedVersion() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		String unsupported = MpoFileImporter.manifestFor(projectXml).replace("formatVersion=\"1.0\"", "formatVersion=\"2.0\"");
		assertThrows(IOException.class, () -> MpoFileImporter.validateManifest(unsupported, projectXml));
	}

	@Test
	void manifestReportsAnUnsupportedMajorVersionExplicitly() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		String newerMajor = MpoFileImporter.manifestFor(projectXml).replace("formatVersion=\"1.0\"", "formatVersion=\"2.3\"");
		IOException failure = assertThrows(IOException.class, () -> MpoFileImporter.validateManifest(newerMajor, projectXml));
		org.junit.jupiter.api.Assertions.assertTrue(failure.getMessage().contains("Unsupported MPOF format version 2.3"), failure.getMessage());
	}

	@Test
	void manifestRejectsMissingOrMalformedFormatVersion() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		String missing = MpoFileImporter.manifestFor(projectXml).replace(" formatVersion=\"1.0\"", "");
		assertThrows(IOException.class, () -> MpoFileImporter.validateManifest(missing, projectXml));
		String malformed = MpoFileImporter.manifestFor(projectXml).replace("formatVersion=\"1.0\"", "formatVersion=\"1\"");
		assertThrows(IOException.class, () -> MpoFileImporter.validateManifest(malformed, projectXml));
	}

	/** Issue #356: an mpo written by a different minor revision of the same major still opens. */
	@Test
	void mpoOpensAnOtherMinorRevisionAndUpgradesItToTheCurrentVersionOnSave() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		String manifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		org.junit.jupiter.api.Assertions.assertTrue(manifest.contains("formatVersion=\"1.0\""), manifest);
		entries.put(MpoFileImporter.MANIFEST_ENTRY, manifest.replace("formatVersion=\"1.0\"", "formatVersion=\"1.7\"").getBytes(StandardCharsets.UTF_8));

		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project reopened = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertNotNull(reopened);

		ByteArrayOutputStream resaved = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(reopened, resaved);
		String upgraded = new String(readEntries(resaved.toByteArray()).get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		org.junit.jupiter.api.Assertions.assertTrue(upgraded.contains("formatVersion=\"1.0\""), upgraded);
		org.junit.jupiter.api.Assertions.assertFalse(upgraded.contains("formatVersion=\"1.7\""), upgraded);
	}

	@Test
	void mpoOpensAnOtherMinorMetaRevisionButRejectsAnotherMajor() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		String meta = new String(entries.get("meta.xml"), StandardCharsets.UTF_8);
		byte[] minorMeta = meta.replace("formatVersion=\"1.0\"", "formatVersion=\"1.4\"").getBytes(StandardCharsets.UTF_8);
		entries.put("meta.xml", minorMeta);
		updateManifestChecksum(entries, "meta.xml", minorMeta);
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		org.junit.jupiter.api.Assertions.assertNotNull(reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray())));

		byte[] majorMeta = meta.replace("formatVersion=\"1.0\"", "formatVersion=\"9.0\"").getBytes(StandardCharsets.UTF_8);
		entries.put("meta.xml", majorMeta);
		updateManifestChecksum(entries, "meta.xml", majorMeta);
		MpoFileImporter rejecting = new MpoFileImporter();
		rejecting.setProjectFactory(ProjectFactory.getInstance());
		assertThrows(IOException.class, () -> rejecting.loadProject(new ByteArrayInputStream(zip(entries).toByteArray())));
	}

	@Test
	void legacyMicroprojectMpoMigratesMillisecondLevelingDelayBeforeScheduling() throws Exception {
		Project project = projectForRoundTrip();
		long delay = 3L * 60L * 60L * 1000L;
		((NormalTask) firstTask(project)).setLevelingDelay(delay);
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		String currentXml = new String(entries.get(MpoFileImporter.PROJECT_ENTRY), StandardCharsets.UTF_8);
		org.junit.jupiter.api.Assertions.assertTrue(currentXml.contains("<LevelingDelay>"), currentXml);
		String legacyXml = currentXml.replaceFirst("<LevelingDelay>\\d+</LevelingDelay>", "<LevelingDelay>10800000</LevelingDelay>");
		entries.put(MpoFileImporter.PROJECT_ENTRY, legacyXml.getBytes(StandardCharsets.UTF_8));
		entries.put(MpoFileImporter.MANIFEST_ENTRY, MpoFileImporter.manifestFor(entries.get(MpoFileImporter.PROJECT_ENTRY)).getBytes(StandardCharsets.UTF_8));
		String legacyMeta = new String(entries.get(MpoFileImporter.META_ENTRY), StandardCharsets.UTF_8)
				.replace(" levelingDelayUnit=\"minutes\"", "");
		entries.put(MpoFileImporter.META_ENTRY, legacyMeta.getBytes(StandardCharsets.UTF_8));

		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertEquals(delay, firstTask(loaded).getLevelingDelay());

		ByteArrayOutputStream resaved = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(loaded, resaved);
		String migratedMeta = new String(readEntries(resaved.toByteArray()).get(MpoFileImporter.META_ENTRY), StandardCharsets.UTF_8);
		org.junit.jupiter.api.Assertions.assertTrue(migratedMeta.contains("levelingDelayUnit=\"minutes\""), migratedMeta);
	}

	@Test
	void legacyMicroprojectMpoMigratesAssignmentLevelingDelay() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(project);
		Resource resource = project.getResourcePool().newResourceInstance();
		resource.setName("Legacy assignment resource");
		com.microproject.pm.assignment.Assignment assignment =
			AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, null, false);
		long delay = 2L * 60L * 60L * 1000L;
		assignment.setLevelingDelay(delay);

		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		String currentXml = new String(entries.get(MpoFileImporter.PROJECT_ENTRY), StandardCharsets.UTF_8);
		org.junit.jupiter.api.Assertions.assertTrue(currentXml.contains("<LevelingDelay>1200</LevelingDelay>"), currentXml);
		String legacyXml = currentXml.replace("<LevelingDelay>1200</LevelingDelay>", "<LevelingDelay>7200000</LevelingDelay>");
		entries.put(MpoFileImporter.PROJECT_ENTRY, legacyXml.getBytes(StandardCharsets.UTF_8));
		entries.put(MpoFileImporter.MANIFEST_ENTRY, MpoFileImporter.manifestFor(entries.get(MpoFileImporter.PROJECT_ENTRY)).getBytes(StandardCharsets.UTF_8));
		String legacyMeta = new String(entries.get(MpoFileImporter.META_ENTRY), StandardCharsets.UTF_8)
			.replace(" levelingDelayUnit=\"minutes\"", "");
		entries.put(MpoFileImporter.META_ENTRY, legacyMeta.getBytes(StandardCharsets.UTF_8));

		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		NormalTask loadedTask = (NormalTask) firstTask(loaded);
		com.microproject.pm.assignment.Assignment loadedAssignment =
			(com.microproject.pm.assignment.Assignment) loadedTask.getAssignments().iterator().next();
		org.junit.jupiter.api.Assertions.assertEquals("Legacy assignment resource", loadedAssignment.getResource().getName());
		org.junit.jupiter.api.Assertions.assertEquals(delay, loadedAssignment.getLevelingDelay());
	}

	@Test
	void manifestRejectsDuplicateRequiredFields() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		String duplicate = MpoFileImporter.manifestFor(projectXml).replace("format=\"mpof\"", "format=\"mpof\" format=\"mpof\"");
		assertThrows(IOException.class, () -> MpoFileImporter.validateManifest(duplicate, projectXml));
	}

	@Test
	void mpoRejectsMalformedOperationLogsBeforeLoadingTheSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		entries.put("operations/log.jsonl", "{\"type\":\"header\",\"schemaVersion\":1,\"documentId\":\"not-a-uuid\"}\n".getBytes(StandardCharsets.UTF_8));
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		assertThrows(IOException.class, () -> reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray())));
	}

	@Test
	void mpoDoesNotReplayTaskUpdatesOverItsMaterializedSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(project);
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation update = new OperationLog.Operation("00000000-0000-0000-0000-000000000011", "00000000-0000-0000-0000-000000000012", 1, java.util.Set.of(), "task.update", "00000000-0000-0000-0000-000000000013", Map.of("legacyUniqueId", Long.valueOf(task.getUniqueId()), "name", "Merged task"));
		byte[] operations = new OperationLog().writeJsonl(manifestDocumentId(entries), java.util.List.of(update));
		entries.put("operations/log.jsonl", operations);
		updateManifestChecksum(entries, "operations/log.jsonl", operations);
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertEquals(task.getName(), firstTask(loaded).getName());
		ByteArrayOutputStream resaved = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(loaded, resaved);
		java.util.List<OperationLog.Operation> retained = new OperationLog().readJsonl(
				readEntries(resaved.toByteArray()).get("operations/log.jsonl")).operations();
		org.junit.jupiter.api.Assertions.assertEquals(1, retained.size());
		org.junit.jupiter.api.Assertions.assertEquals("task.update", retained.get(0).kind());
	}

	@Test
	void mpoDoesNotReplayTaskCreatesOverItsMaterializedSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation create = new OperationLog.Operation("00000000-0000-0000-0000-000000000021", "00000000-0000-0000-0000-000000000022", 1, java.util.Set.of(), "task.create", "00000000-0000-0000-0000-000000000023", Map.of("legacyUniqueId", Long.valueOf(9001L), "name", "Created task"));
		byte[] operations = new OperationLog().writeJsonl(manifestDocumentId(entries), java.util.List.of(create, create));
		entries.put("operations/log.jsonl", operations);
		updateManifestChecksum(entries, "operations/log.jsonl", operations);
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertNull(loaded.findByUniqueId(9001L));
		org.junit.jupiter.api.Assertions.assertEquals(taskCount(project), taskCount(loaded));
	}

	@Test
	void mpoDoesNotReplayTaskDeletesOverItsMaterializedSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		long taskId = firstTask(project).getUniqueId();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation delete = new OperationLog.Operation("00000000-0000-0000-0000-000000000031", "00000000-0000-0000-0000-000000000032", 1, java.util.Set.of(), "task.delete", "00000000-0000-0000-0000-000000000033", Map.of("legacyUniqueId", Long.valueOf(taskId)));
		byte[] operations = new OperationLog().writeJsonl(manifestDocumentId(entries), java.util.List.of(delete, delete));
		entries.put("operations/log.jsonl", operations);
		updateManifestChecksum(entries, "operations/log.jsonl", operations);
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertEquals(taskCount(project), taskCount(loaded));
	}

	@Test
	void mpoSaveAppendsTaskUpdatesAfterTheInitialSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		MpoFileImporter writer = new MpoFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		((NormalTask) firstTask(project)).setName("Edited after save");
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		byte[] operations = readEntries(second.toByteArray()).get("operations/log.jsonl");
		org.junit.jupiter.api.Assertions.assertEquals(1, new OperationLog().readJsonl(operations).operations().size());
	}

	@Test
	void mpoSaveAppendsTaskCreatesAfterTheInitialSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		MpoFileImporter writer = new MpoFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		((NormalTask) project.createLocalTaskNode(null).getImpl()).setName("Added after save");
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		byte[] operations = readEntries(second.toByteArray()).get("operations/log.jsonl");
		org.junit.jupiter.api.Assertions.assertEquals("task.create", new OperationLog().readJsonl(operations).operations().get(0).kind());
	}

	@Test
	void mpoDoesNotReplayTaskMovesOverItsMaterializedSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask child = (NormalTask) firstTask(project);
		NormalTask parent = (NormalTask) project.createLocalTaskNode(null).getImpl(); parent.setName("Parent");
		assignPositiveUniqueIds(project);
		ByteArrayOutputStream generated = new ByteArrayOutputStream(); new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation move = new OperationLog.Operation("00000000-0000-0000-0000-000000000041", "00000000-0000-0000-0000-000000000042", 1, java.util.Set.of(), "task.move", "00000000-0000-0000-0000-000000000043", Map.of("legacyUniqueId", Long.valueOf(child.getUniqueId()), "parentLegacyUniqueId", Long.valueOf(parent.getUniqueId())));
		byte[] operations = new OperationLog().writeJsonl(manifestDocumentId(entries), java.util.List.of(move));
		entries.put("operations/log.jsonl", operations);
		updateManifestChecksum(entries, "operations/log.jsonl", operations);
		MpoFileImporter reader = new MpoFileImporter(); reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertNotSame(loaded.findByUniqueId(parent.getUniqueId()),
				loaded.findByUniqueId(child.getUniqueId()).getWbsParentTask());
	}

	@Test
	void mpoSaveAppendsTaskMovesAfterTheInitialSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask child = (NormalTask) firstTask(project);
		NormalTask parent = (NormalTask) project.createLocalTaskNode(null).getImpl(); parent.setName("Parent");
		assignPositiveUniqueIds(project);
		MpoFileImporter writer = new MpoFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		project.setLocalParent(child, parent);
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		java.util.List<OperationLog.Operation> operations = new OperationLog().readJsonl(readEntries(second.toByteArray()).get("operations/log.jsonl")).operations();
		org.junit.jupiter.api.Assertions.assertEquals("task.move", operations.get(0).kind());
		org.junit.jupiter.api.Assertions.assertEquals(parent.getUniqueId(), ((Number) operations.get(0).payload().get("parentLegacyUniqueId")).longValue());
	}

	@Test
	void mpoSavesAndReplaysDependencyOperations() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask predecessor = (NormalTask) firstTask(project);
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl(); successor.setName("Successor");
		MpoFileImporter writer = new MpoFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor,
			DependencyType.FS, 0L, null);
		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertEquals(0L, dependency.getLag());
		ByteArrayOutputStream fsZeroLag = new ByteArrayOutputStream(); writer.saveProject(project, fsZeroLag);
		MpoFileImporter fsReader = new MpoFileImporter(); fsReader.setProjectFactory(ProjectFactory.getInstance());
		Project fsLoaded = fsReader.loadProject(new ByteArrayInputStream(fsZeroLag.toByteArray()));
		Task fsPredecessor = findByName(fsLoaded, predecessor.getName());
		Task fsSuccessor = findByName(fsLoaded, successor.getName());
		Dependency fsDependency = (Dependency) fsPredecessor.getSuccessorList().iterator().next();
		assertEquals(fsSuccessor, fsDependency.getSuccessor());
		assertEquals(DependencyType.FS, fsDependency.getDependencyType());
		assertEquals(0L, fsDependency.getLag());
		long workingLag = 8L * 60L * 60L * 1000L;
		DependencyService.getInstance().setFields(dependency, workingLag, DependencyType.Kind.SS, new Object());
		project.getUndoController().undo();
		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertEquals(0L, dependency.getLag());
		project.getUndoController().redo();
		assertEquals(DependencyType.SS, dependency.getDependencyType());
		assertEquals(workingLag, dependency.getLag());
		project.recalculate();
		long savedSuccessorStart = successor.getStart();
		long savedSuccessorFinish = successor.getEnd();
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		java.util.List<OperationLog.Operation> operations = new OperationLog().readJsonl(readEntries(second.toByteArray()).get("operations/log.jsonl")).operations();
		org.junit.jupiter.api.Assertions.assertEquals("dependency.add", operations.get(0).kind());
		MpoFileImporter reader = new MpoFileImporter(); reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(second.toByteArray()));
		Task loadedPredecessor = findByName(loaded, predecessor.getName());
		Task loadedSuccessor = findByName(loaded, successor.getName());
		Dependency loadedDependency = null;
		for (java.util.Iterator<?> links = loadedPredecessor.getSuccessorList().iterator(); links.hasNext();) {
			Dependency candidate = (Dependency) links.next();
			if (candidate.getSuccessor() == loadedSuccessor) loadedDependency = candidate;
		}
		org.junit.jupiter.api.Assertions.assertNotNull(loadedDependency);
		assertEquals(loadedSuccessor, loadedDependency.getSuccessor());
		assertEquals(loadedPredecessor, loadedDependency.getPredecessor());
		org.junit.jupiter.api.Assertions.assertTrue(loadedSuccessor.getPredecessorList().contains(loadedDependency));
		assertEquals(DependencyType.SS, loadedDependency.getDependencyType());
		assertEquals(workingLag, loadedDependency.getLag());
		loaded.recalculate();
		assertEquals(savedSuccessorStart, loadedSuccessor.getStart());
		assertEquals(savedSuccessorFinish, loadedSuccessor.getEnd());
		Dependency loadedLink = loadedDependency;
		DependencyService.getInstance().remove(loadedLink, this, true);
		org.junit.jupiter.api.Assertions.assertFalse(loadedPredecessor.getSuccessorList().contains(loadedLink));
		org.junit.jupiter.api.Assertions.assertFalse(loadedSuccessor.getPredecessorList().contains(loadedLink));
		loaded.getUndoController().undo();
		org.junit.jupiter.api.Assertions.assertTrue(loadedPredecessor.getSuccessorList().contains(loadedLink));
		org.junit.jupiter.api.Assertions.assertTrue(loadedSuccessor.getPredecessorList().contains(loadedLink));
		loaded.getUndoController().redo();
		org.junit.jupiter.api.Assertions.assertFalse(loadedPredecessor.getSuccessorList().contains(loadedLink));
		org.junit.jupiter.api.Assertions.assertFalse(loadedSuccessor.getPredecessorList().contains(loadedLink));
		loaded.recalculate();
		long deletedSuccessorStart = loadedSuccessor.getStart();
		long deletedSuccessorFinish = loadedSuccessor.getEnd();
		ByteArrayOutputStream deleted = new ByteArrayOutputStream(); reader.saveProject(loaded, deleted);
		java.util.List<OperationLog.Operation> deletedOperations = new OperationLog().readJsonl(readEntries(deleted.toByteArray()).get("operations/log.jsonl")).operations();
		org.junit.jupiter.api.Assertions.assertTrue(deletedOperations.stream().anyMatch(operation -> operation.kind().equals("dependency.delete")));
		MpoFileImporter deletedReader = new MpoFileImporter(); deletedReader.setProjectFactory(ProjectFactory.getInstance());
		Project deletedLoaded = deletedReader.loadProject(new ByteArrayInputStream(deleted.toByteArray()));
		Task deletedPredecessor = findByName(deletedLoaded, predecessor.getName());
		Task deletedSuccessor = findByName(deletedLoaded, successor.getName());
		org.junit.jupiter.api.Assertions.assertTrue(deletedPredecessor.getSuccessorList().isEmpty());
		org.junit.jupiter.api.Assertions.assertTrue(deletedSuccessor.getPredecessorList().isEmpty());
		deletedLoaded.recalculate();
		assertEquals(deletedSuccessorStart, deletedSuccessor.getStart());
		assertEquals(deletedSuccessorFinish, deletedSuccessor.getEnd());
	}

	@Test
	void mpoSavesAndReplaysAssignmentOperations() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(project);
		Resource resource = project.getResourcePool().newResourceInstance(); resource.setName("Engineer");
		assignPositiveUniqueIds(project);
		MpoFileImporter writer = new MpoFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		AssignmentService.getInstance().newAssignment(task, resource, 1.0D, 0L, null, false);
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		java.util.List<OperationLog.Operation> operations = new OperationLog().readJsonl(readEntries(second.toByteArray()).get("operations/log.jsonl")).operations();
		org.junit.jupiter.api.Assertions.assertEquals("assignment.add", operations.get(0).kind());
		MpoFileImporter reader = new MpoFileImporter(); reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(second.toByteArray()));
		org.junit.jupiter.api.Assertions.assertTrue(((NormalTask) loaded.findByUniqueId(task.getUniqueId())).getAssignments().iterator().hasNext());
	}

	@Test
	void mpoSequentialSharedFolderSavesMergeIndependentTaskEdits() throws Exception {
		Project initial = projectForRoundTrip();
		NormalTask second = (NormalTask) initial.createLocalTaskNode(null).getImpl(); second.setName("Second");
		assignPositiveUniqueIds(initial);
		File shared = File.createTempFile("mpo-shared", ".mpo"); shared.deleteOnExit();
		MpoFileImporter initialWriter = new MpoFileImporter(); initialWriter.setFileName(shared.getAbsolutePath()); initialWriter.setProject(initial); initialWriter.exportFile();
		org.junit.jupiter.api.Assertions.assertTrue(new File(shared.getAbsolutePath() + ".lock").isFile(), "shared saves use a stable transaction lock");
		Project firstEditor = load(shared); Project secondEditor = load(shared);
		firstTask(firstEditor).setName("First editor");
		secondEditor.findByUniqueId(second.getUniqueId()).setName("Second editor");
		MpoFileImporter firstWriter = new MpoFileImporter(); firstWriter.setFileName(shared.getAbsolutePath()); firstWriter.setProject(firstEditor); firstWriter.exportFile();
		MpoFileImporter secondWriter = new MpoFileImporter(); secondWriter.setFileName(shared.getAbsolutePath()); secondWriter.setProject(secondEditor); secondWriter.exportFile();
		Project merged = load(shared);
		org.junit.jupiter.api.Assertions.assertEquals("First editor", firstTask(merged).getName());
		org.junit.jupiter.api.Assertions.assertEquals("Second editor", merged.findByUniqueId(second.getUniqueId()).getName());
	}

	@Test
	void mpoSaveAfterHierarchyChangeAcceptsAnEquivalentArchiveOperationWithDifferentNumberTypes() throws Exception {
		Project initial = projectForRoundTrip();
		File shared = File.createTempFile("mpo-number-normalization", ".mpo");
		shared.deleteOnExit();
		MpoFileImporter initialWriter = new MpoFileImporter();
		initialWriter.setFileName(shared.getAbsolutePath());
		initialWriter.setProject(initial);
		initialWriter.exportFile();

		Map<String, byte[]> entries = readEntries(java.nio.file.Files.readAllBytes(shared.toPath()));
		OperationLog.Operation archiveOperation = new OperationLog.Operation(
				"00000000-0000-0000-0000-000000000081", "00000000-0000-0000-0000-000000000082", 1,
				java.util.Set.of(), "assignment.delete", "00000000-0000-0000-0000-000000000083",
				Map.of("taskLegacyUniqueId", Integer.valueOf(1), "resourceUniqueId", Integer.valueOf(1)));
		byte[] archiveLog = new OperationLog().writeJsonl(manifestDocumentId(entries), java.util.List.of(archiveOperation));
		entries.put(MpoFileImporter.OPERATIONS_ENTRY, archiveLog);
		updateManifestChecksum(entries, MpoFileImporter.OPERATIONS_ENTRY, archiveLog);
		java.nio.file.Files.write(shared.toPath(), zip(entries).toByteArray());

		Project editor = load(shared);
		NormalTask child = (NormalTask) firstTask(editor);
		NormalTask parent = (NormalTask) editor.createLocalTaskNode(null).getImpl();
		parent.setName("New parent");
		parent.setUniqueId(-91001L);
		editor.setLocalParent(child, parent);
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(shared.getAbsolutePath());
		writer.setProject(editor);

		assertDoesNotThrow(writer::exportFile);
		Project reloaded = load(shared);
		Task reloadedChild = reloaded.findByUniqueId(child.getUniqueId());
		assertNotNull(reloadedChild, "persisted child UID must survive a hierarchy change");
		assertEquals("New parent", reloadedChild.getWbsParentTask().getName());
	}

	@Test
	void mpoConcurrentSharedFolderSavesSerializeAndMergeBothEditors() throws Exception {
		Project initial = projectForRoundTrip();
		NormalTask second = (NormalTask) initial.createLocalTaskNode(null).getImpl(); second.setName("Second");
		assignPositiveUniqueIds(initial);
		File shared = File.createTempFile("mpo-concurrent", ".mpo"); shared.deleteOnExit();
		MpoFileImporter initialWriter = new MpoFileImporter(); initialWriter.setFileName(shared.getAbsolutePath()); initialWriter.setProject(initial); initialWriter.exportFile();
		Project firstEditor = load(shared); Project secondEditor = load(shared);
		firstTask(firstEditor).setName("Concurrent first");
		secondEditor.findByUniqueId(second.getUniqueId()).setName("Concurrent second");
		java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
		try {
			java.util.concurrent.Future<?> first = executor.submit(() -> saveAfter(start, shared, firstEditor));
			java.util.concurrent.Future<?> secondSave = executor.submit(() -> saveAfter(start, shared, secondEditor));
			start.countDown();
			first.get(); secondSave.get();
		} finally {
			executor.shutdownNow();
		}
		Project merged = load(shared);
		org.junit.jupiter.api.Assertions.assertEquals("Concurrent first", firstTask(merged).getName());
		org.junit.jupiter.api.Assertions.assertEquals("Concurrent second", merged.findByUniqueId(second.getUniqueId()).getName());
	}

	@Test
	void mpoSameFieldConflictKeepsSharedFileAndWritesReloadableRecoveryCopy() throws Exception {
		Project initial = projectForRoundTrip();
		assignPositiveUniqueIds(initial);
		File shared = File.createTempFile("mpo-conflict-recovery", ".mpo");
		shared.deleteOnExit();
		new File(shared.getAbsolutePath() + ".lock").deleteOnExit();
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Project firstEditor = load(shared);
		Project secondEditor = load(shared);
		firstTask(firstEditor).setName("First committed value");
		firstTask(secondEditor).setName("Second recoverable value");
		MpoFileImporter firstWriter = new MpoFileImporter();
		firstWriter.setFileName(shared.getAbsolutePath());
		firstWriter.setProject(firstEditor);
		firstWriter.exportFile();
		byte[] committedArchive = Files.readAllBytes(shared.toPath());

		MpoFileImporter secondWriter = new MpoFileImporter();
		secondWriter.setFileName(shared.getAbsolutePath());
		secondWriter.setProject(secondEditor);
		MpoConflictRecoveryException conflict = assertThrows(MpoConflictRecoveryException.class, secondWriter::exportFile);
		Path recoveryCopy = conflict.recoveryCopy();
		Path repeatedRecoveryCopy = null;
		try {
			assertEquals(1, conflict.conflicts().size());
			assertTrue(conflict.getMessage().contains(recoveryCopy.toString()));
			org.junit.jupiter.api.Assertions.assertArrayEquals(committedArchive, Files.readAllBytes(shared.toPath()),
				"the shared archive must remain unchanged when same-field edits conflict");
			assertEquals("Second recoverable value", firstTask(secondEditor).getName(),
				"the losing editor's in-memory value must remain available");
			assertTrue(Files.isRegularFile(recoveryCopy));
			Project recovered = load(recoveryCopy.toFile());
			assertEquals("Second recoverable value", firstTask(recovered).getName());
			OperationLog.DocumentLog recoveryLog = new OperationLog().readJsonl(
				readEntries(Files.readAllBytes(recoveryCopy)).get(MpoFileImporter.OPERATIONS_ENTRY));
			assertEquals(2, recoveryLog.operations().size(), "the recovery journal must retain both concurrent operations");
			assertEquals(1, new OperationLog().merge(recoveryLog.operations()).conflicts().size());
			assertEquals(1, recoveryLog.appliedOperationIds().size(),
				"the recovery snapshot may advertise only the losing editor's materialized branch");
			MpoConflictRecoveryException retryConflict = assertThrows(MpoConflictRecoveryException.class, secondWriter::exportFile);
			repeatedRecoveryCopy = retryConflict.recoveryCopy();
			OperationLog.DocumentLog retryLog = new OperationLog().readJsonl(
				readEntries(Files.readAllBytes(repeatedRecoveryCopy)).get(MpoFileImporter.OPERATIONS_ENTRY));
			assertEquals(recoveryLog.operations().stream().map(OperationLog.Operation::id).collect(java.util.stream.Collectors.toSet()),
				retryLog.operations().stream().map(OperationLog.Operation::id).collect(java.util.stream.Collectors.toSet()),
				"a retry must reuse the same operation IDs instead of appending duplicate edits");
		} finally {
			Files.deleteIfExists(recoveryCopy);
			if (repeatedRecoveryCopy != null) Files.deleteIfExists(repeatedRecoveryCopy);
		}
	}

	@Test
	void concurrentDependencyLagChangesShareRelationshipIdentityAndConflict() throws Exception {
		Project initial = projectForRoundTrip();
		NormalTask predecessor = (NormalTask) firstTask(initial);
		NormalTask successor = (NormalTask) initial.createLocalTaskNode(null).getImpl();
		successor.setName("Successor");
		assignPositiveUniqueIds(initial);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, null);
		File shared = File.createTempFile("mpo-dependency-conflict", ".mpo");
		Path lockPath = Path.of(shared.getAbsolutePath() + ".lock");
		try {
			MpoFileImporter seed = new MpoFileImporter();
			seed.setFileName(shared.getAbsolutePath());
			seed.setProject(initial);
			seed.exportFile();
			Project left = load(shared);
			Project right = load(shared);
			changeDependencyLag(left, predecessor.getUniqueId(), successor.getUniqueId(), 1000L);
			changeDependencyLag(right, predecessor.getUniqueId(), successor.getUniqueId(), 2000L);
			byte[] leftArchive = saveMpo(left);
			byte[] rightArchive = saveMpo(right);
			List<OperationLog.Operation> concurrent = new ArrayList<>(new OperationLog().readJsonl(
					readEntries(leftArchive).get(MpoFileImporter.OPERATIONS_ENTRY)).operations());
			concurrent.addAll(new OperationLog().readJsonl(
					readEntries(rightArchive).get(MpoFileImporter.OPERATIONS_ENTRY)).operations());
			OperationLog.MergeResult merged = new OperationLog().merge(concurrent);
			List<OperationLog.Operation> additions = concurrent.stream().filter(operation ->
					"dependency.add".equals(operation.kind())).toList();
			assertEquals(2, additions.size());
			assertEquals(additions.get(0).entityId(), additions.get(1).entityId(),
				"lag is mutable relationship data and must not split the entity identity");
			assertFalse(merged.conflicts().isEmpty(), "concurrent edits to one dependency must not both merge");
		} finally {
			Files.deleteIfExists(lockPath);
			Files.deleteIfExists(shared.toPath());
		}
	}

	@Test
	void mpoLockOpenFailurePreservesLocalEditsInReloadableRecoveryCopy() throws Exception {
		Project initial = projectForRoundTrip();
		assignPositiveUniqueIds(initial);
		File shared = File.createTempFile("mpo-lock-recovery", ".mpo");
		Path lockPath = Path.of(shared.getAbsolutePath() + ".lock");
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();
		byte[] sharedArchive = Files.readAllBytes(shared.toPath());
		Project editor = load(shared);
		firstTask(editor).setName("Recover after lock failure");
		Files.delete(lockPath);
		Files.createDirectory(lockPath);
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(shared.getAbsolutePath());
		writer.setProject(editor);
		try {
			MpoConflictRecoveryException failure = assertThrows(MpoConflictRecoveryException.class, writer::exportFile);
			assertTrue(failure.conflicts().isEmpty(), "a lock failure is not an operation conflict");
			assertTrue(failure.getMessage().contains("transaction lock"));
			Path recoveryCopy = failure.recoveryCopy();
			try {
				assertTrue(Files.isRegularFile(recoveryCopy));
				org.junit.jupiter.api.Assertions.assertArrayEquals(sharedArchive, Files.readAllBytes(shared.toPath()));
				assertEquals("Recover after lock failure", firstTask(load(recoveryCopy.toFile())).getName());
				OperationLog.DocumentLog recoveryLog = new OperationLog().readJsonl(
						readEntries(Files.readAllBytes(recoveryCopy)).get(MpoFileImporter.OPERATIONS_ENTRY));
				assertEquals(1, recoveryLog.operations().size(), "the local edit must be recoverable as an operation");
				assertEquals(1, recoveryLog.appliedOperationIds().size());
			} finally {
				Files.deleteIfExists(recoveryCopy);
			}
		} finally {
			Files.deleteIfExists(lockPath);
			Files.deleteIfExists(shared.toPath());
		}
	}

	@Test
	void separateJvmSavesMergeIndependentTaskEdits() throws Exception {
		Project initial = projectForRoundTrip();
		NormalTask second = (NormalTask) initial.createLocalTaskNode(null).getImpl();
		second.setName("Second");
		assignPositiveUniqueIds(initial);
		long firstId = firstTask(initial).getUniqueId();
		long secondId = second.getUniqueId();
		File shared = File.createTempFile("mpo-process-concurrent", ".mpo");
		shared.deleteOnExit();
		new File(shared.getAbsolutePath() + ".lock").deleteOnExit();
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Path tempDirectory = Files.createTempDirectory("mpo-process-" + System.nanoTime());
		Path release = tempDirectory.resolve("release.flag");
		Path firstReady = release.resolveSibling("first-ready.flag");
		Path secondReady = release.resolveSibling("second-ready.flag");
		Path firstLog = release.resolveSibling("first-worker.log");
		Path secondLog = release.resolveSibling("second-worker.log");
		Process first = null;
		Process secondProcess = null;
		try {
			first = startMpoSaveWorker(shared, firstId, "Process first", firstReady, release, firstLog);
			secondProcess = startMpoSaveWorker(shared, secondId, "Process second", secondReady, release, secondLog);
			awaitWorkerReady(first, firstReady, firstLog);
			awaitWorkerReady(secondProcess, secondReady, secondLog);
			Files.createFile(release);
			assertWorkerSucceeded(first, firstLog);
			assertWorkerSucceeded(secondProcess, secondLog);
			Project merged = load(shared);
			assertEquals("Process first", merged.findByUniqueId(firstId).getName());
			assertEquals("Process second", merged.findByUniqueId(secondId).getName());
		} finally {
			stopWorker(first);
			stopWorker(secondProcess);
			Files.deleteIfExists(release);
			Files.deleteIfExists(firstReady);
			Files.deleteIfExists(secondReady);
			Files.deleteIfExists(firstLog);
			Files.deleteIfExists(secondLog);
			Files.deleteIfExists(tempDirectory);
		}
	}

	@Test
	void mpoRoundTripPreservesReadyButUnappliedOperationGeneration() throws Exception {
		Project source = projectForRoundTrip();
		ByteArrayOutputStream initial = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(source, initial));
		Map<String, byte[]> entries = readEntries(initial.toByteArray());
		String documentId = source.getDocumentId();
		String actorId = java.util.UUID.randomUUID().toString();
		String operationId = java.util.UUID.randomUUID().toString();
		String entityId = java.util.UUID.randomUUID().toString();
		OperationLog.Operation unapplied = new OperationLog.Operation(operationId, actorId, 1L, java.util.Set.of(),
			"task.update", entityId, Map.of("legacyUniqueId", firstTask(source).getUniqueId(), "name", "Held conflict value"));
		byte[] operationLog = new OperationLog().writeJsonl(documentId, List.of(unapplied), java.util.Set.of());
		entries.put(MpoFileImporter.OPERATIONS_ENTRY, operationLog);
		updateManifestChecksum(entries, MpoFileImporter.OPERATIONS_ENTRY, operationLog);

		File target = File.createTempFile("mpo-unapplied-generation", ".mpo");
		target.deleteOnExit();
		Files.write(target.toPath(), zip(entries).toByteArray());
		Project loaded = load(target);
		assertEquals(java.util.Set.of(), new OperationLog().readJsonl(
			readEntries(Files.readAllBytes(target.toPath())).get(MpoFileImporter.OPERATIONS_ENTRY)).appliedOperationIds());
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(target.getAbsolutePath());
		writer.setProject(loaded);
		writer.exportFile();

		OperationLog.DocumentLog persisted = new OperationLog().readJsonl(
			readEntries(Files.readAllBytes(target.toPath())).get(MpoFileImporter.OPERATIONS_ENTRY));
		assertEquals(java.util.Set.of(operationId), persisted.operations().stream().map(OperationLog.Operation::id)
			.collect(java.util.stream.Collectors.toSet()));
		assertEquals(java.util.Set.of(), persisted.appliedOperationIds());
		assertEquals("Mpo task", firstTask(load(target)).getName());
	}

	@Test
	void mpoTaskPercentCompleteOperationReplayPreservesFractionalPrecision() throws Exception {
		Project project = projectForRoundTrip();
		assignPositiveUniqueIds(project);
		MpoFileImporter writer = new MpoFileImporter();
		ByteArrayOutputStream initial = new ByteArrayOutputStream();
		writer.saveProject(project, initial);
		Project base = new MpoFileImporter().loadProject(new ByteArrayInputStream(initial.toByteArray()));

		Task source = firstTask(project);
		((NormalTask) source).setImportedPercentComplete(2D / 3D);
		ByteArrayOutputStream changed = new ByteArrayOutputStream();
		writer.saveProject(project, changed);
		java.util.List<OperationLog.Operation> operations = new OperationLog().readJsonl(
				readEntries(changed.toByteArray()).get(MpoFileImporter.OPERATIONS_ENTRY)).operations();

		new com.microproject.collaboration.MpoTaskOperationService().apply(base, operations);

		Task replayed = base.findByUniqueId(source.getUniqueId());
		assertEquals(2D / 3D, replayed.getPercentComplete(), 0D,
			"MPO operation replay must preserve the explicit task percentage without duration rounding");
		assertEquals(2D / 3D, ((NormalTask) replayed).getImportedPercentCompleteOverride(), 0D);
	}

	@Test
	void mpoTaskCreateOperationRetainsItsParentWhenReplayed() throws Exception {
		Project project = projectForRoundTrip();
		assignPositiveUniqueIds(project);
		MpoFileImporter writer = new MpoFileImporter();
		ByteArrayOutputStream initial = new ByteArrayOutputStream();
		writer.saveProject(project, initial);
		Project base = new MpoFileImporter().loadProject(new ByteArrayInputStream(initial.toByteArray()));

		NormalTask parent = (NormalTask) project.createLocalTaskNode(null).getImpl();
		parent.setName("Created parent");
		parent.setUniqueId(-91002L);
		NormalTask child = (NormalTask) project.createLocalTaskNode(null).getImpl();
		child.setName("Created child");
		child.setUniqueId(-91003L);
		project.setLocalParent(child, parent);
		ByteArrayOutputStream changed = new ByteArrayOutputStream();
		writer.saveProject(project, changed);

		java.util.List<OperationLog.Operation> operations = new OperationLog().readJsonl(
				readEntries(changed.toByteArray()).get(MpoFileImporter.OPERATIONS_ENTRY)).operations();
		new com.microproject.collaboration.MpoTaskOperationService().apply(base, operations);
		Task replayedChild = base.getTaskList().stream()
			.filter(task -> "Created child".equals(task.getName())).findFirst().orElseThrow();
		assertEquals("Created parent", replayedChild.getWbsParentTask().getName());
		assertEquals(3, base.getTaskList().size(), "replay must retain the original task and both new tasks");
	}

	@Test
	void mpoReplayUsesAlreadyAppliedParentsAsCausalContextWithoutApplyingThemTwice() throws Exception {
		Project project = projectForRoundTrip();
		OperationLog.Operation parent = new OperationLog.Operation(java.util.UUID.randomUUID().toString(),
			java.util.UUID.randomUUID().toString(), 1L, java.util.Set.of(), "task.create",
			java.util.UUID.randomUUID().toString(), Map.of("legacyUniqueId", 1001L, "name", "Parent",
				"notes", "", "percentComplete", 0D));
		OperationLog.Operation child = new OperationLog.Operation(java.util.UUID.randomUUID().toString(),
			java.util.UUID.randomUUID().toString(), 1L, java.util.Set.of(parent.id()), "task.create",
			java.util.UUID.randomUUID().toString(), Map.of("legacyUniqueId", 1002L, "name", "Child",
				"notes", "", "percentComplete", 0D, "parentLegacyUniqueId", 1001L));
		com.microproject.collaboration.MpoTaskOperationService service = new com.microproject.collaboration.MpoTaskOperationService();
		service.apply(project, List.of(parent));
		Task existingParent = project.findByUniqueId(1001L);

		service.apply(project, List.of(parent, child), java.util.Set.of(parent.id()));

		assertEquals(existingParent, project.findByUniqueId(1001L));
		assertEquals(1001L, project.findByUniqueId(1002L).getWbsParentTask().getUniqueId());
	}

	@Test
	void separateJvmSavesMergeDifferentFieldsOnTheSameTask() throws Exception {
		Project initial = projectForRoundTrip();
		assignPositiveUniqueIds(initial);
		long taskId = firstTask(initial).getUniqueId();
		File shared = File.createTempFile("mpo-process-field-merge", ".mpo");
		shared.deleteOnExit();
		new File(shared.getAbsolutePath() + ".lock").deleteOnExit();
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Path tempDirectory = Files.createTempDirectory("mpo-process-fields-" + System.nanoTime());
		Path release = tempDirectory.resolve("release.flag");
		Path firstReady = release.resolveSibling("first-ready.flag");
		Path secondReady = release.resolveSibling("second-ready.flag");
		Path firstLog = release.resolveSibling("first-worker.log");
		Path secondLog = release.resolveSibling("second-worker.log");
		Process first = null;
		Process second = null;
		try {
			first = startMpoSaveWorker(shared, taskId, "name", "Process name", firstReady, release, firstLog);
			second = startMpoSaveWorker(shared, taskId, "notes", "Process notes", secondReady, release, secondLog);
			awaitWorkerReady(first, firstReady, firstLog);
			awaitWorkerReady(second, secondReady, secondLog);
			Files.createFile(release);
			assertWorkerSucceeded(first, firstLog);
			assertWorkerSucceeded(second, secondLog);

			Task mergedTask = load(shared).findByUniqueId(taskId);
			assertEquals("Process name", mergedTask.getName());
			assertEquals("Process notes", mergedTask.getNotes());
		} finally {
			stopWorker(first);
			stopWorker(second);
			Files.deleteIfExists(release);
			Files.deleteIfExists(firstReady);
			Files.deleteIfExists(secondReady);
			Files.deleteIfExists(firstLog);
			Files.deleteIfExists(secondLog);
			Files.deleteIfExists(tempDirectory);
		}
	}

	@Test
	void separateJvmSameFieldConflictReturnsDurableRecoveryCopy() throws Exception {
		Project initial = projectForRoundTrip();
		assignPositiveUniqueIds(initial);
		long taskId = firstTask(initial).getUniqueId();
		File shared = File.createTempFile("mpo-process-conflict", ".mpo");
		shared.deleteOnExit();
		new File(shared.getAbsolutePath() + ".lock").deleteOnExit();
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Path tempDirectory = Files.createTempDirectory("mpo-process-conflict-" + System.nanoTime());
		Path release = tempDirectory.resolve("release.flag");
		Path firstReady = tempDirectory.resolve("first-ready.flag");
		Path secondReady = tempDirectory.resolve("second-ready.flag");
		Path firstLog = tempDirectory.resolve("first-worker.log");
		Path secondLog = tempDirectory.resolve("second-worker.log");
		Process first = null;
		Process second = null;
		Path recoveryCopy = null;
		try {
			first = startMpoSaveWorker(shared, taskId, "name", "Process winner A", firstReady, release, firstLog);
			second = startMpoSaveWorker(shared, taskId, "name", "Process winner B", secondReady, release, secondLog);
			awaitWorkerReady(first, firstReady, firstLog);
			awaitWorkerReady(second, secondReady, secondLog);
			Files.createFile(release);
			assertWorkerSucceeded(first, firstLog);
			assertWorkerSucceeded(second, secondLog);

			String firstOutput = readWorkerLog(firstLog);
			String secondOutput = readWorkerLog(secondLog);
			List<String> recoveryMarkers = java.util.stream.Stream.of(firstOutput, secondOutput)
				.flatMap(value -> value.lines()).filter(value -> value.startsWith("MPO_CONFLICT_RECOVERY="))
				.toList();
			assertEquals(1, recoveryMarkers.size(), "exactly one process should produce a conflict recovery copy");
			recoveryCopy = Path.of(recoveryMarkers.getFirst().substring("MPO_CONFLICT_RECOVERY=".length()));
			assertTrue(Files.isRegularFile(recoveryCopy));
			String sharedValue = firstTask(load(shared)).getName();
			String recoveredValue = firstTask(load(recoveryCopy.toFile())).getName();
			assertTrue(java.util.Set.of("Process winner A", "Process winner B").contains(sharedValue));
			assertTrue(java.util.Set.of("Process winner A", "Process winner B").contains(recoveredValue));
			org.junit.jupiter.api.Assertions.assertNotEquals(sharedValue, recoveredValue);
			OperationLog.DocumentLog recoveryLog = new OperationLog().readJsonl(
				readEntries(Files.readAllBytes(recoveryCopy)).get(MpoFileImporter.OPERATIONS_ENTRY));
			assertEquals(2, recoveryLog.operations().size());
			assertEquals(1, new OperationLog().merge(recoveryLog.operations()).conflicts().size());
		} finally {
			stopWorker(first);
			stopWorker(second);
			if (recoveryCopy != null) Files.deleteIfExists(recoveryCopy);
			Files.deleteIfExists(release);
			Files.deleteIfExists(firstReady);
			Files.deleteIfExists(secondReady);
			Files.deleteIfExists(firstLog);
			Files.deleteIfExists(secondLog);
			Files.deleteIfExists(tempDirectory);
		}
	}

	private static Process startMpoSaveWorker(File shared, long taskId, String name, Path ready,
			Path release, Path log) throws Exception {
		return startMpoSaveWorker(shared, taskId, "name", name, ready, release, log);
	}

	private static Process startMpoSaveWorker(File shared, long taskId, String field, String value,
			Path ready, Path release, Path log) throws Exception {
		return startMpoSaveWorker(shared, taskId, field, value, ready, release, log, "");
	}

	private static Process startMpoSaveWorker(File shared, long taskId, String field, String value,
			Path ready, Path release, Path log, String crashPoint) throws Exception {
		String javaExecutable = Path.of(System.getProperty("java.home"), "bin",
				System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win") ? "java.exe" : "java")
			.toString();
		List<String> command = new ArrayList<>(List.of(javaExecutable, "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-cp",
				processTestClasspath(), MpoConcurrentSaveProcess.class.getName(), shared.getAbsolutePath(),
				Long.toString(taskId), field, value, ready.toString(), release.toString()));
		if (!crashPoint.isEmpty()) command.add(crashPoint);
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);
		builder.redirectOutput(log.toFile());
		return builder.start();
	}

	private static String processTestClasspath() throws Exception {
		java.util.LinkedHashSet<String> entries = new java.util.LinkedHashSet<>();
		String systemClasspath = System.getProperty("java.class.path", "");
		for (String entry : systemClasspath.split(java.util.regex.Pattern.quote(File.pathSeparator)))
			if (!entry.isBlank()) entries.add(entry);
		for (ClassLoader loader = MpoFileImporterTest.class.getClassLoader(); loader != null; loader = loader.getParent()) {
			if (loader instanceof java.net.URLClassLoader urlClassLoader) {
				for (java.net.URL url : urlClassLoader.getURLs()) {
					if ("file".equalsIgnoreCase(url.getProtocol()))
						entries.add(Path.of(url.toURI()).toString());
				}
			}
		}
		return String.join(File.pathSeparator, entries);
	}

	private static void awaitWorkerReady(Process worker, Path ready, Path log) throws Exception {
		long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
		while (!Files.exists(ready)) {
			if (!worker.isAlive())
				throw new AssertionError("MPO worker exited before loading its editor snapshot: " + readWorkerLog(log));
			if (System.nanoTime() >= deadline)
				throw new AssertionError("MPO worker did not load its editor snapshot in time: " + readWorkerLog(log));
			Thread.sleep(20L);
		}
	}

	private static void assertWorkerSucceeded(Process worker, Path log) throws Exception {
		org.junit.jupiter.api.Assertions.assertTrue(worker.waitFor(30, java.util.concurrent.TimeUnit.SECONDS),
				"MPO worker did not finish: " + readWorkerLog(log));
		assertEquals(0, worker.exitValue(), "MPO worker failed: " + readWorkerLog(log));
	}

	private static void stopWorker(Process worker) throws InterruptedException {
		if (worker == null || !worker.isAlive())
			return;
		worker.destroyForcibly();
		worker.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
	}

	private static String readWorkerLog(Path log) throws IOException {
		return new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
	}

	private static void saveAfter(java.util.concurrent.CountDownLatch start, File shared, Project editor) {
		try {
			start.await();
			MpoFileImporter writer = new MpoFileImporter(); writer.setFileName(shared.getAbsolutePath()); writer.setProject(editor); writer.exportFile();
		} catch (Exception error) {
			throw new RuntimeException(error);
		}
	}

	@Test
	void mpoSharedFolderSavePreservesExtensionAddedAfterEditorOpened() throws Exception {
		Project initial = projectForRoundTrip();
		File shared = File.createTempFile("mpo-extension-merge", ".mpo"); shared.deleteOnExit();
		MpoFileImporter initialWriter = new MpoFileImporter(); initialWriter.setFileName(shared.getAbsolutePath()); initialWriter.setProject(initial); initialWriter.exportFile();
		Project editor = load(shared);
		Map<String, byte[]> entries = readEntries(java.nio.file.Files.readAllBytes(shared.toPath()));
		entries.put("vendor/remote.bin", new byte[] { 7, 8, 9 });
		java.nio.file.Files.write(shared.toPath(), zip(entries).toByteArray());
		firstTask(editor).setName("Edited locally");
		MpoFileImporter writer = new MpoFileImporter(); writer.setFileName(shared.getAbsolutePath()); writer.setProject(editor); writer.exportFile();
		org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { 7, 8, 9 }, readEntries(java.nio.file.Files.readAllBytes(shared.toPath())).get("vendor/remote.bin"));
	}

	@Test
	void mpoSharedFolderSaveRejectsATamperedExistingArchiveBeforeReplacingIt() throws Exception {
		Project initial = projectForRoundTrip();
		File shared = File.createTempFile("mpo-checksum-merge", ".mpo"); shared.deleteOnExit();
		MpoFileImporter initialWriter = new MpoFileImporter(); initialWriter.setFileName(shared.getAbsolutePath()); initialWriter.setProject(initial); initialWriter.exportFile();
		Project editor = load(shared);
		Map<String, byte[]> entries = readEntries(java.nio.file.Files.readAllBytes(shared.toPath()));
		entries.put("meta.xml", "<meta formatVersion=\"1.0\" tampered=\"true\"/>".getBytes(StandardCharsets.UTF_8));
		byte[] tampered = zip(entries).toByteArray();
		java.nio.file.Files.write(shared.toPath(), tampered);
		MpoFileImporter writer = new MpoFileImporter(); writer.setFileName(shared.getAbsolutePath()); writer.setProject(editor);
		assertThrows(IOException.class, writer::exportFile);
		org.junit.jupiter.api.Assertions.assertArrayEquals(tampered, java.nio.file.Files.readAllBytes(shared.toPath()));
	}

	@Test
	void mpoAtomicMoveFailureKeepsOriginalArchiveAndInMemoryProject() throws Exception {
		Project initial = projectForRoundTrip();
		File target = File.createTempFile("mpo-atomic-failure-", ".mpo");
		target.deleteOnExit();
		MpoFileImporter initialWriter = new MpoFileImporter();
		initialWriter.setFileName(target.getAbsolutePath());
		initialWriter.setProject(initial);
		initialWriter.exportFile();
		byte[] originalBytes = java.nio.file.Files.readAllBytes(target.toPath());
		Project edited = load(target);
		firstTask(edited).setName("Edit retained after failed replacement");
		assertTrue(edited.needsSaving(), "fixture edit must remain dirty before the injected move failure");

		MpoFileImporter failingWriter = new MpoFileImporter() {
			@Override
			protected void moveTemporary(java.nio.file.Path temporary, java.nio.file.Path destination) throws IOException {
				throw new IOException("injected atomic move failure");
			}
		};
		failingWriter.setFileName(target.getAbsolutePath());
		failingWriter.setProject(edited);
		assertThrows(IOException.class, failingWriter::exportFile);
		org.junit.jupiter.api.Assertions.assertArrayEquals(originalBytes,
				java.nio.file.Files.readAllBytes(target.toPath()), "failed replacement must preserve original MPO bytes");
		assertTrue(edited.needsSaving(), "failed replacement must retain the dirty in-memory project");
		assertEquals("Edit retained after failed replacement", firstTask(edited).getName());
	}

	@Test
	void mpoSaveCleansOnlyStaleStagesForItsOwnTarget() throws Exception {
		Project initial = projectForRoundTrip();
		File target = File.createTempFile("mpo-stale-stage-cleanup-", ".mpo");
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(target.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Path parent = target.toPath().toAbsolutePath().getParent();
		Path staleStage = parent.resolve(target.getName() + ".abc123.tmp");
		Path recentStage = parent.resolve(target.getName() + ".def456.tmp");
		Path otherTargetStage = parent.resolve(target.getName() + ".other.ghi789.tmp");
		Files.write(staleStage, new byte[] { 1 });
		Files.write(recentStage, new byte[] { 2 });
		Files.write(otherTargetStage, new byte[] { 3 });
		Files.setLastModifiedTime(staleStage, java.nio.file.attribute.FileTime.fromMillis(
				System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(2)));

		try {
			Project edited = load(target);
			firstTask(edited).setName("Save after abandoned stage cleanup");
			MpoFileImporter writer = new MpoFileImporter();
			writer.setFileName(target.getAbsolutePath());
			writer.setProject(edited);
			writer.exportFile();
			assertFalse(Files.exists(staleStage), "old stage for this target should be removed under its lock");
			assertTrue(Files.exists(recentStage), "a fresh stage must not be mistaken for a crashed writer artifact");
			assertTrue(Files.exists(otherTargetStage), "a stage belonging to a different target must be preserved");
			assertEquals("Save after abandoned stage cleanup", firstTask(load(target)).getName());
		} finally {
			Files.deleteIfExists(staleStage);
			Files.deleteIfExists(recentStage);
			Files.deleteIfExists(otherTargetStage);
			Files.deleteIfExists(Path.of(target.getAbsolutePath() + ".lock"));
			Files.deleteIfExists(target.toPath());
		}
	}

	@Test
	void mpoSharedFolderRejectsManifestDocumentMismatchBeforeMerge() throws Exception {
		Project initial = projectForRoundTrip();
		File shared = File.createTempFile("mpo-manifest-mismatch", ".mpo"); shared.deleteOnExit();
		MpoFileImporter initialWriter = new MpoFileImporter(); initialWriter.setFileName(shared.getAbsolutePath()); initialWriter.setProject(initial); initialWriter.exportFile();
		Project editor = load(shared);
		Map<String, byte[]> entries = readEntries(java.nio.file.Files.readAllBytes(shared.toPath()));
		String manifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8)
			.replaceFirst("documentId=\\\"[^\\\"]+\\\"", "documentId=\\\"00000000-0000-0000-0000-000000000099\\\"");
		entries.put(MpoFileImporter.MANIFEST_ENTRY, manifest.getBytes(StandardCharsets.UTF_8));
		java.nio.file.Files.write(shared.toPath(), zip(entries).toByteArray());
		firstTask(editor).setName("edited");
		MpoFileImporter writer = new MpoFileImporter(); writer.setFileName(shared.getAbsolutePath()); writer.setProject(editor);
		assertThrows(IOException.class, writer::exportFile);
	}

	@Test
	void mpoContainerUsesOdfStyleLayout() throws Exception {
		Project original = projectForRoundTrip();
		File mpo = File.createTempFile("mpo-layout", ".mpo"); mpo.deleteOnExit();
		MpoFileImporter writer = new MpoFileImporter(); writer.setFileName(mpo.getAbsolutePath()); writer.setProject(original); writer.exportFile();
		Map<String, byte[]> entries = readEntries(java.nio.file.Files.readAllBytes(mpo.toPath()));
		org.junit.jupiter.api.Assertions.assertEquals("application/vnd.microproject.openproject",
			new String(entries.get("mimetype"), StandardCharsets.UTF_8).trim());
		org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("META-INF/manifest.xml"));
		org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("meta.xml"));
		org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("content.xml"));
		org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("settings.xml"));
		org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("ccpm/history.jsonl"));
		org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("operations/log.jsonl"));
		org.junit.jupiter.api.Assertions.assertTrue(new String(entries.get("meta.xml"), StandardCharsets.UTF_8).contains("<meta "));
		String manifest = new String(entries.get("META-INF/manifest.xml"), StandardCharsets.UTF_8);
		org.junit.jupiter.api.Assertions.assertTrue(manifest.contains("format=\"mpof\""));
		org.junit.jupiter.api.Assertions.assertTrue(manifest.contains("formatVersion=\"1.0\""));
	}

	@Test
	void mpoReadsEarlierDraftLayoutAndWritesCurrentLayout() throws Exception {
		Project original = projectForRoundTrip();
		ByteArrayOutputStream current = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(original, current);
		Map<String, byte[]> entries = readEntries(current.toByteArray());
		String xmlManifest = new String(entries.remove(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		String sha256 = xmlManifest.replaceFirst("(?s).*projectSha256=\"([^\"]+)\".*", "$1");
		String documentId = xmlManifest.replaceFirst("(?s).*documentId=\"([0-9a-f-]{36})\".*", "$1");
		entries.remove("meta.xml"); entries.remove("settings.xml");
		byte[] jsonl = entries.remove("operations/log.jsonl");
		entries.put(MpoFileImporter.MANIFEST_ENTRY, ("{\"format\":\"mpof\",\"formatVersion\":\"1.0\",\"projectEntry\":\"content.xml\",\"projectSha256\":\"" + sha256 + "\",\"documentId\":\"" + documentId + "\"}\n").getBytes(StandardCharsets.UTF_8));
		entries.put("changes/operations.json", new OperationLog().write(documentId, new OperationLog().readJsonl(jsonl).operations()));
		Project loaded = loadFromBytes(zip(entries).toByteArray());
		org.junit.jupiter.api.Assertions.assertEquals(taskCount(original), taskCount(loaded));
		ByteArrayOutputStream rewritten = new ByteArrayOutputStream(); new MpoFileImporter().saveProject(loaded, rewritten);
		Map<String, byte[]> rewrittenEntries = readEntries(rewritten.toByteArray());
		org.junit.jupiter.api.Assertions.assertTrue(rewrittenEntries.containsKey("meta.xml"));
		org.junit.jupiter.api.Assertions.assertTrue(rewrittenEntries.containsKey("operations/log.jsonl"));
	}

	@Test
	void mpoTaskLevelingOptionsRoundTripAndOlderFilesKeepDefaults() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		task.setLevelingCanSplit(false);
		task.setLevelAssignments(false);
		com.microproject.pm.resource.Resource resource = original.getResourcePool().newResourceInstance();
		resource.setName("Leveling resource");
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, this);
		assignment.setLevelingDelay(8L * 60L * 60L * 1000L);
		ByteArrayOutputStream current = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(original, current);
		Map<String, byte[]> entries = readEntries(current.toByteArray());
		assertTrue(entries.containsKey(MpoFileImporter.TASK_LEVELING_OPTIONS_ENTRY));
		Project loaded = loadFromBytes(current.toByteArray());
		assertFalse(firstTask(loaded).isLevelingCanSplit());
		assertFalse(firstTask(loaded).isLevelAssignments());
		Assignment loadedAssignment = (Assignment) ((NormalTask) firstTask(loaded)).getAssignments().iterator().next();
		assertEquals(8L * 60L * 60L * 1000L,
			com.microproject.datatype.Duration.millis(loadedAssignment.getLevelingDelay()));
		new com.microproject.pm.resource.ResourceLevelingService().clear(loaded);
		assertEquals(0L, com.microproject.datatype.Duration.millis(loadedAssignment.getLevelingDelay()),
			"Clear after native reload removes generated assignment leveling delay");

		entries.remove(MpoFileImporter.TASK_LEVELING_OPTIONS_ENTRY);
		String manifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		manifest = manifest.replaceAll("(?m)^\\s*<entry\\s+path=\\\""
			+ java.util.regex.Pattern.quote(MpoFileImporter.TASK_LEVELING_OPTIONS_ENTRY)
			+ "\\\" sha256=\\\"[^\\\"]*\\\"\\s*/>\\R?", "");
		entries.put(MpoFileImporter.MANIFEST_ENTRY, manifest.getBytes(StandardCharsets.UTF_8));
		Project older = loadFromBytes(zip(entries).toByteArray());
		assertTrue(firstTask(older).isLevelingCanSplit(), "older MPO defaults to the documented Yes value");
		assertTrue(firstTask(older).isLevelAssignments(), "older MPO defaults to the documented Yes value");
	}

	@Test
	void draftManifestMustIdentifyTheProjectSnapshotEntry() throws Exception {
		Project original = projectForRoundTrip();
		ByteArrayOutputStream current = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(original, current);
		Map<String, byte[]> entries = readEntries(current.toByteArray());
		String xmlManifest = new String(entries.remove(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		String sha256 = xmlManifest.replaceFirst("(?s).*projectSha256=\\\"([^\\\"]+)\\\".*", "$1");
		String documentId = xmlManifest.replaceFirst("(?s).*documentId=\\\"([0-9a-f-]{36})\\\".*", "$1");
		entries.remove("meta.xml"); entries.remove("settings.xml"); entries.remove("operations/log.jsonl");
		entries.put(MpoFileImporter.MANIFEST_ENTRY, ("{\"format\":\"mpof\",\"formatVersion\":\"1.0\",\"projectEntry\":\"wrong.xml\",\"projectSha256\":\"" + sha256 + "\",\"documentId\":\"" + documentId + "\"}\n").getBytes(StandardCharsets.UTF_8));
		org.junit.jupiter.api.Assertions.assertThrows(IOException.class, () -> loadFromBytes(zip(entries).toByteArray()));
	}

	@Test
	void currentAndDraftCcpmSettingsCannotBeMixed() throws Exception {
		Project original = projectForRoundTrip();
		ByteArrayOutputStream current = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(original, current);
		Map<String, byte[]> entries = readEntries(current.toByteArray());
		entries.put("ccpm.json", "{\"schemaVersion\":1,\"enabled\":false,\"bufferFraction\":0.2,\"levelingOrder\":\"MIN_SLACK\",\"onlyWithinAvailableSlack\":false,\"allowTaskSplits\":false}\n".getBytes(StandardCharsets.UTF_8));
		org.junit.jupiter.api.Assertions.assertThrows(IOException.class, () -> loadFromBytes(zip(entries).toByteArray()));
	}

	@Test
	void mpoRoundTripLoadsItsMspdiSnapshot() throws Exception {
		Project original = projectForRoundTrip();
		CriticalChainService.Settings ccpm = new CriticalChainService().settings(original);
		ccpm.setEnabled(true);
		ccpm.setBufferFraction(0.4D);
		ccpm.setIncludeProposedResources(true);
		ccpm.setLevelManuallyScheduledTasks(true);
		ccpm.setLevelAssignments(false);
		ccpm.setSensitivity(com.microproject.pm.resource.ResourceLevelingService.Sensitivity.HOUR);
		new CriticalChainService().restoreBaseline(original, new CriticalChainService.Baseline(1L, 2L, 0.4D,
			java.util.List.of(), java.util.Map.of(), java.util.Map.of()));
		File mpo = File.createTempFile("mpo-roundtrip", ".mpo");
		mpo.deleteOnExit();
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(mpo.getAbsolutePath());
		writer.setProject(original);
		writer.exportFile();
		org.junit.jupiter.api.Assertions.assertTrue(readEntries(java.nio.file.Files.readAllBytes(mpo.toPath())).containsKey("operations/log.jsonl"));

		MpoFileImporter reader = new MpoFileImporter();
		reader.setFileName(mpo.getAbsolutePath());
		reader.setProjectFactory(ProjectFactory.getInstance());
		reader.importFile();

		org.junit.jupiter.api.Assertions.assertNotNull(reader.getProject());
		org.junit.jupiter.api.Assertions.assertFalse(reader.getProject().isReadOnly(),
				"a locally opened MPOF project must remain editable");
		org.junit.jupiter.api.Assertions.assertEquals(taskCount(original), taskCount(reader.getProject()));
		CriticalChainService.Settings restored = new CriticalChainService().settings(reader.getProject());
		org.junit.jupiter.api.Assertions.assertTrue(restored.isEnabled());
		org.junit.jupiter.api.Assertions.assertEquals(0.4D, restored.getBufferFraction());
		org.junit.jupiter.api.Assertions.assertTrue(restored.isIncludeProposedResources());
		org.junit.jupiter.api.Assertions.assertTrue(restored.isLevelManuallyScheduledTasks());
		org.junit.jupiter.api.Assertions.assertFalse(restored.isLevelAssignments());
		org.junit.jupiter.api.Assertions.assertEquals(com.microproject.pm.resource.ResourceLevelingService.Sensitivity.HOUR,
			restored.getSensitivity());
		org.junit.jupiter.api.Assertions.assertEquals(2L, new CriticalChainService().findBaseline(reader.getProject()).projectBufferMillis());
		byte[] operations = readEntries(java.nio.file.Files.readAllBytes(mpo.toPath())).get("operations/log.jsonl");
		ByteArrayOutputStream roundTrip = new ByteArrayOutputStream();
		writer.saveProject(reader.getProject(), roundTrip);
		org.junit.jupiter.api.Assertions.assertArrayEquals(operations, readEntries(roundTrip.toByteArray()).get("operations/log.jsonl"));
	}

	@Test
	void olderMpoCcpmSettingsWithoutProposedResourceOptionDefaultToExcluded() throws Exception {
		Project original = projectForRoundTrip();
		CriticalChainService.Settings settings = new CriticalChainService().settings(original);
		settings.setEnabled(true);
		settings.setIncludeProposedResources(true);
		settings.setLevelManuallyScheduledTasks(true);
		File mpo = File.createTempFile("mpo-old-ccpm-options", ".mpo");
		mpo.deleteOnExit();
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(mpo.getAbsolutePath());
		writer.setProject(original);
		writer.exportFile();
		Map<String, byte[]> entries = readEntries(java.nio.file.Files.readAllBytes(mpo.toPath()));
		String xml = new String(entries.get("settings.xml"), StandardCharsets.UTF_8);
		String oldXml = xml.replace(" includeProposedResources=\"true\"", "")
			.replace(" levelManuallyScheduledTasks=\"true\"", "")
			.replace(" levelAssignments=\"true\"", "")
			.replace(" sensitivity=\"DAY\"", "");
		org.junit.jupiter.api.Assertions.assertNotEquals(xml, oldXml, "fixture must contain the new XML setting");
		entries.put("settings.xml", oldXml.getBytes(StandardCharsets.UTF_8));
		updateManifestChecksum(entries, "settings.xml", entries.get("settings.xml"));

		Project reopened = loadFromBytes(zip(entries).toByteArray());
		org.junit.jupiter.api.Assertions.assertFalse(new CriticalChainService().settings(reopened).isIncludeProposedResources(),
			"MPO files written before the option existed must retain the safe excluded default");
		org.junit.jupiter.api.Assertions.assertFalse(new CriticalChainService().settings(reopened).isLevelManuallyScheduledTasks(),
			"MPO files written before the option existed must keep manual tasks fixed by default");
		org.junit.jupiter.api.Assertions.assertTrue(new CriticalChainService().settings(reopened).isLevelAssignments(),
			"MPO files written before the option existed default to individual assignment leveling");
		org.junit.jupiter.api.Assertions.assertEquals(com.microproject.pm.resource.ResourceLevelingService.Sensitivity.DAY,
			new CriticalChainService().settings(reopened).getSensitivity(),
			"MPO settings written before sensitivity must retain the day product default");
	}

	@Test
	void mpoRoundTripPreservesAppliedCcpmAndCanReanalyzeTheChain() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask first = (NormalTask) firstTask(original);
		NormalTask second = (NormalTask) original.createLocalTaskNode(null).getImpl();
		second.setName("Second mpo task");
		assignPositiveUniqueIds(original);
		Resource resource = original.getResourcePool().newResourceInstance();
		resource.setName("Shared engineer");
		MpoFileImporter writer = new MpoFileImporter();
		File mpo = File.createTempFile("mpo-ccpm-applied", ".mpo");
		mpo.deleteOnExit();
		writer.setFileName(mpo.getAbsolutePath());
		writer.setProject(original);
		writer.exportFile();
		AssignmentService.getInstance().newAssignment(first, resource, 1D, 0L, null, false);
		AssignmentService.getInstance().newAssignment(second, resource, 1D, 0L, null, false);

		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.settings(original);
		settings.setEnabled(true);
		settings.setBufferFraction(0.5D);
		CriticalChainService.Analysis applied = service.apply(original, java.util.List.of(resource), settings);
		org.junit.jupiter.api.Assertions.assertTrue(applied.criticalTaskIds().contains(Long.valueOf(second.getUniqueId())));

		writer.exportFile();

		Project loaded = load(mpo);
		CriticalChainService loadedService = new CriticalChainService();
		CriticalChainService.Settings restored = loadedService.findSettings(loaded);
		org.junit.jupiter.api.Assertions.assertNotNull(restored);
		org.junit.jupiter.api.Assertions.assertTrue(restored.isEnabled());
		org.junit.jupiter.api.Assertions.assertEquals(0.5D, restored.getBufferFraction());
		org.junit.jupiter.api.Assertions.assertNotNull(loadedService.findBaseline(loaded));
		// The importer rebuilds task-side assignments first; resource-side
		// assignment indexes are populated lazily for some MPO snapshots.  Use
		// the authoritative assignment reference from the restored task rather
		// than requiring that optional reverse index to be eagerly populated.
		Resource loadedResource = null;
		for (java.util.Iterator<?> tasks = loaded.getTaskOutlineIterator(); tasks.hasNext() && loadedResource == null;) {
			Object value = tasks.next();
			if (value instanceof NormalTask task && !task.getAssignments().isEmpty()) {
				Object assignment = task.getAssignments().get(0);
				if (assignment instanceof com.microproject.pm.assignment.Assignment a)
					loadedResource = a.getResource();
			}
		}
		org.junit.jupiter.api.Assertions.assertNotNull(loadedResource, "restored task assignment must reference a resource");
		CriticalChainService.Analysis reanalyzed = loadedService.preview(loaded, java.util.List.of(loadedResource), restored);
		org.junit.jupiter.api.Assertions.assertFalse(reanalyzed.criticalTaskIds().isEmpty());
		org.junit.jupiter.api.Assertions.assertTrue(reanalyzed.projectBuffer().plannedMillis() >= 0L);
		// CCPM Apply -> Save -> Reload -> Clear must remove only the document-scoped
		// CCPM projection and restore the ordinary schedule state.
		loadedService.clear(loaded);
		org.junit.jupiter.api.Assertions.assertNull(loadedService.findSettings(loaded));
		org.junit.jupiter.api.Assertions.assertNull(loadedService.findBaseline(loaded));
		org.junit.jupiter.api.Assertions.assertNull(loadedService.findAnalysis(loaded));
		org.junit.jupiter.api.Assertions.assertTrue(loaded.getTaskOutlineIterator().hasNext(),
			"clearing CCPM must retain the reloaded task document");
	}

	/**
	 * Exercises the complete user-facing path on a real legacy ProjectLibre sample:
	 * load POD, apply CCPM, save as MPO, reload, and preview the restored chain.
	 * This guards against the synthetic fixture hiding importer/exporter differences
	 * in task hierarchies, calendars, and resource assignments.
	 */
	@Test
	void realPodSampleCanBeConvertedToMpoAndReanalyzedWithCcpm() throws Exception {
		File source = findSample("June_1_sample.pod");
		Project original = loadPod(source);
		List<Resource> selected = new ArrayList<>();
		selected.addAll(original.getResourcePool().getResourceList());
		org.junit.jupiter.api.Assertions.assertFalse(selected.isEmpty(), "sample must contain resources");

		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.settings(original);
		settings.setEnabled(true);
		settings.setBufferFraction(0.25D);
		CriticalChainService.Analysis applied = service.apply(original, selected, settings);
		org.junit.jupiter.api.Assertions.assertFalse(applied.criticalTaskIds().isEmpty(), "sample CCPM chain must not be empty");

		File mpo = File.createTempFile("sample-ccpm", ".mpo");
		mpo.deleteOnExit();
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(mpo.getAbsolutePath());
		writer.setProject(original);
		writer.exportFile();

		Project restored = load(mpo);
		CriticalChainService.Settings restoredSettings = service.findSettings(restored);
		org.junit.jupiter.api.Assertions.assertNotNull(restoredSettings);
		org.junit.jupiter.api.Assertions.assertTrue(restoredSettings.isEnabled());
		org.junit.jupiter.api.Assertions.assertNotNull(service.findBaseline(restored));
		List<Resource> restoredResources = new ArrayList<>();
		restoredResources.addAll(restored.getResourcePool().getResourceList());
		CriticalChainService.Analysis reanalyzed = service.preview(restored, restoredResources, restoredSettings);
		org.junit.jupiter.api.Assertions.assertFalse(reanalyzed.criticalTaskIds().isEmpty());
	}

	@Test
	void mpoPreservesUnknownExtensionsOnRoundTrip() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		MpoFileImporter writer = new MpoFileImporter();
		writer.saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		byte[] extension = "opaque extension".getBytes(StandardCharsets.UTF_8);
		entries.put("vendor/example.json", extension);
		ByteArrayOutputStream input = zip(entries);

		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(input.toByteArray()));
		ByteArrayOutputStream roundTrip = new ByteArrayOutputStream();
		writer.saveProject(loaded, roundTrip);

		org.junit.jupiter.api.Assertions.assertArrayEquals(extension, readEntries(roundTrip.toByteArray()).get("vendor/example.json"));
	}

	@Test
	void mpoSessionsCoordinateTaskLocksThroughTheSharedSidecar() throws Exception {
		Project original = projectForRoundTrip();
		File mpo = File.createTempFile("mpo-collaboration", ".mpo");
		mpo.deleteOnExit();
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(mpo.getAbsolutePath());
		writer.setProject(original);
		writer.exportFile();
		Project first = load(mpo);
		Project second = load(mpo);
		CollaborationSession alice = CollaborationSession.create(first, mpo.getAbsolutePath(), "alice");
		CollaborationSession bob = CollaborationSession.create(second, mpo.getAbsolutePath(), "bob");
		org.junit.jupiter.api.Assertions.assertNotNull(alice);
		org.junit.jupiter.api.Assertions.assertNotNull(bob);
		alice.start();
		bob.start();
		try {
			org.junit.jupiter.api.Assertions.assertTrue(alice.tryAcquireTaskLock(firstTask(first)));
			org.junit.jupiter.api.Assertions.assertFalse(bob.tryAcquireTaskLock(firstTask(second)));
			alice.stop();
			org.junit.jupiter.api.Assertions.assertTrue(bob.tryAcquireTaskLock(firstTask(second)));
		} finally {
			alice.stop();
			bob.stop();
		}
	}

	@Test
	void mpoRoundTripPreservesTaskAndBaselineDurationMillisUnitAndText() throws Exception {
		// A baseline saved while the unit is the project default is re-imported through MSPDI with that
		// default made explicit in the unit bits. The encoded long may differ, but the milliseconds, the
		// effective unit, the formatted text and the exported MSPDI/MPP duration must not.
		for (int enteredIn : new int[] { com.microproject.datatype.TimeUnit.DAYS, com.microproject.datatype.TimeUnit.HOURS }) {
			Project original = projectForRoundTrip();
			com.microproject.datatype.DurationDefaults defaults = new com.microproject.datatype.DurationDefaults(480.0D,
				2400.0D, 20.0D, enteredIn, com.microproject.datatype.TimeUnit.HOURS);
			original.setDurationDefaults(defaults);
			NormalTask task = (NormalTask) firstTask(original);
			task.setDuration(8L * 60L * 60L * 1000L);
			original.recalculate();
			task.saveCurrentToSnapshot(Integer.valueOf(Snapshottable.BASELINE));
			task.saveCurrentToSnapshot(Integer.valueOf(10));
			Project reopened = loadFromBytes(saveProjectBytes(original));
			NormalTask restored = (NormalTask) firstTask(reopened);
			assertEquals(defaults, reopened.getDurationDefaults());

			com.microproject.field.FieldContext liveContext = new com.microproject.field.FieldContext();
			liveContext.setDurationDefaultsResolver(original::getDurationDefaults);
			com.microproject.field.FieldContext restoredContext = new com.microproject.field.FieldContext();
			restoredContext.setDurationDefaultsResolver(reopened::getDurationDefaults);
			assertEquals(task.getDuration(), restored.getDuration(), "current encoded duration");
			com.microproject.field.Field duration = com.microproject.configuration.Configuration.getFieldFromId("Field.duration");
			assertEquals(duration.getText(task, liveContext), duration.getText(restored, restoredContext));
			for (int slot : new int[] { 0, 10 }) {
				String id = "Field.baseline" + (slot == 0 ? "" : Integer.toString(slot)) + "Duration";
				com.microproject.field.Field field = com.microproject.configuration.Configuration.getFieldFromId(id);
				long live = task.getBaselineDuration(slot);
				long back = restored.getBaselineDuration(slot);
				String label = " slot " + slot + " entered-in " + enteredIn;
				assertEquals(com.microproject.datatype.Duration.millis(live), com.microproject.datatype.Duration.millis(back),
					"baseline milliseconds" + label);
				assertEquals(com.microproject.datatype.Duration.getType(live) == com.microproject.datatype.TimeUnit.NONE
					? enteredIn : com.microproject.datatype.Duration.getType(live),
					com.microproject.datatype.Duration.getType(back), "baseline effective unit" + label);
				assertEquals(field.getText(task, liveContext), field.getText(restored, restoredContext),
					"formatted baseline duration" + label);
				net.sf.mpxj.Duration liveExport = com.microproject.server.data.MPXConverter.toMPXDuration(live, defaults);
				net.sf.mpxj.Duration backExport = com.microproject.server.data.MPXConverter.toMPXDuration(back, defaults);
				assertEquals(liveExport, backExport, "exported baseline duration" + label);
			}
		}
	}

	private static Project projectForRoundTrip() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("mpo-test", undo), undo);
		project.initialize(false, false);
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName("Mpo task");
		return project;
	}

	/**
	 * Microsoft Project keeps a duration's estimated "?" marker until the user
	 * clears it; saving and reopening a file does not confirm it.  A new task's
	 * default "1 day?" and an explicit firm duration must both survive MPO
	 * save/reload and a no-op resave unchanged.
	 */
	@Test
	void estimatedDurationSurvivesMpoRoundTripAndResave() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask estimated = (NormalTask) findByName(project, "Mpo task");
		NormalTask firm = (NormalTask) project.createLocalTaskNode(null).getImpl();
		firm.setName("Mpo firm task");
		firm.setDuration(2L * 8L * 60L * 60L * 1000L);
		assertTrue(estimated.isEstimated(), "fixture: a new task starts as an estimated duration");
		long estimatedDuration = estimated.getDuration();
		assertTrue(com.microproject.datatype.Duration.isEstimated(estimatedDuration), "fixture: the estimated bit is set");
		assertFalse(firm.isEstimated(), "fixture: an explicit duration is firm");
		long firmDuration = firm.getDuration();

		Project reopened = loadFromBytes(saveProjectBytes(project));
		Project resaved = loadFromBytes(saveProjectBytes(reopened));
		for (Project restored : List.of(reopened, resaved)) {
			NormalTask restoredEstimated = (NormalTask) findByName(restored, "Mpo task");
			NormalTask restoredFirm = (NormalTask) findByName(restored, "Mpo firm task");
			assertTrue(restoredEstimated.isEstimated(), "MPO reload must keep the estimated flag");
			assertEquals(estimatedDuration, restoredEstimated.getDuration(),
				"MPO reload must keep the estimated 1 day? duration");
			assertFalse(restoredFirm.isEstimated(), "MPO reload must not mark a firm duration estimated");
			assertEquals(firmDuration, restoredFirm.getDuration(), "MPO reload must keep the firm duration");
		}
	}

	@Test
	void taskSchedulingModeSurvivesMpoRoundTrip() throws Exception {
		Project original = projectForRoundTrip();
		com.microproject.pm.task.Task task = firstTask(original);
		task.setManuallyScheduled(true);
		Project restored = load(writeTempMpo(original));
		assertTrue(firstTask(restored).isManuallyScheduled(),
				"MPO reload must preserve the task scheduling mode");
	}

	@Test
	void successfulManualModeChangeSurvivesUndoRedoAndMpoRecalculation() throws Exception {
		Project original = projectForRoundTrip();
		Task task = firstTask(original);
		long projectStart = java.time.Instant.parse("2030-06-03T08:00:00Z").toEpochMilli();
		original.setStartDate(projectStart);
		((NormalTask) task).setDuration(8L * 60L * 60L * 1000L);
		original.recalculate();
		long expectedStart = task.getStart();
		long expectedFinish = task.getEnd();

		TaskModeService.Result result = new TaskModeService().apply(
				List.of(task), TaskModeService.Mode.MANUAL);
		assertTrue(result.manual());
		assertTrue(task.isManuallyScheduled());
		assertEquals(expectedStart, task.getManualStart());
		assertEquals(expectedFinish, task.getManualFinish());
		result.change().undo();
		assertFalse(task.isManuallyScheduled());
		result.change().redo();
		assertTrue(task.isManuallyScheduled());
		assertEquals(expectedStart, task.getManualStart());
		assertEquals(expectedFinish, task.getManualFinish());

		Project reopened = loadFromBytes(saveProjectBytes(original));
		reopened.recalculate();
		Task restored = firstTask(reopened);
		assertTrue(restored.isManuallyScheduled());
		assertEquals(expectedStart, restored.getManualStart());
		assertEquals(expectedFinish, restored.getManualFinish());
		assertEquals(expectedStart, restored.getStart());
		assertEquals(expectedFinish, restored.getEnd());
	}

	@Test
	void selectedBaseline10OverwriteSurvivesMpoRoundTripWithoutChangingOtherSlotsOrTasks() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask selected = (NormalTask) firstTask(original);
		selected.setName("Selected Baseline 10 task");
		NormalTask outside = new NormalTask(original);
		outside.setName("Outside selection");
		original.connectTask(outside);
		original.addToDefaultOutline(null, NodeFactory.getInstance().createNode(outside));
		long start = original.getStartDate();
		selected.setStart(start);
		selected.setDuration(8L * 60L * 60L * 1000L);
		outside.setStart(start + 2L * 24L * 60L * 60L * 1000L);
		outside.setDuration(8L * 60L * 60L * 1000L);
		Integer baseline0 = Integer.valueOf(Snapshottable.BASELINE);
		Integer baseline1 = Integer.valueOf(Snapshottable.BASELINE_1);
		Integer baseline10 = Integer.valueOf(Snapshottable.BASELINE_10);
		selected.saveCurrentToSnapshot(baseline0);
		selected.saveCurrentToSnapshot(baseline1);
		selected.saveCurrentToSnapshot(baseline10);
		outside.saveCurrentToSnapshot(baseline10);
		long baseline0Start = selected.getBaselineStart(Snapshottable.BASELINE);
		long baseline1Start = selected.getBaselineStart(Snapshottable.BASELINE_1);
		long replacementStart = start + 4L * 24L * 60L * 60L * 1000L;
		selected.setStart(replacementStart);
		selected.setDuration(16L * 60L * 60L * 1000L);
		selected.setFixedCost(125.0d);
		long outsideBaseline10Start = outside.getBaselineStart(Snapshottable.BASELINE_10);
		long outsideCurrentStart = outside.getStart();

		original.saveCurrentToSnapshot(baseline10, false, List.of(selected));
		long selectedBaseline10Start = selected.getBaselineStart(Snapshottable.BASELINE_10);
		double selectedBaseline10Cost = selected.getBaselineCost(Snapshottable.BASELINE_10);
		Project reopened = loadFromBytes(saveProjectBytes(original));
		NormalTask restoredSelected = (NormalTask) findByName(reopened, "Selected Baseline 10 task");
		NormalTask restoredOutside = (NormalTask) findByName(reopened, "Outside selection");
		assertEquals(selectedBaseline10Start, restoredSelected.getBaselineStart(Snapshottable.BASELINE_10));
		assertEquals(selectedBaseline10Cost, restoredSelected.getBaselineCost(Snapshottable.BASELINE_10), 0.00001d);
		assertEquals(baseline0Start, restoredSelected.getBaselineStart(Snapshottable.BASELINE));
		assertEquals(baseline1Start, restoredSelected.getBaselineStart(Snapshottable.BASELINE_1));
		assertEquals(outsideBaseline10Start, restoredOutside.getBaselineStart(Snapshottable.BASELINE_10));
		assertEquals(outsideCurrentStart, restoredOutside.getStart());
	}

	@Test
	void failedBaselineOverwriteRollbackSurvivesMpoRoundTrip() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask existing = (NormalTask) firstTask(original);
		existing.setName("Existing baseline");
		java.util.concurrent.atomic.AtomicBoolean failOnce = new java.util.concurrent.atomic.AtomicBoolean();
		RuntimeException injected = new IllegalStateException("injected baseline overwrite failure");
		NormalTask failing = new NormalTask(original) {
			@Override public void saveCurrentToSnapshot(Object snapshotId) {
				super.saveCurrentToSnapshot(snapshotId);
				if (failOnce.compareAndSet(true, false)) throw injected;
			}
		};
		failing.setName("Previously unbaselined task");
		original.connectTask(failing);
		original.addToDefaultOutline(null, NodeFactory.getInstance().createNode(failing));
		Resource resource = original.getResourcePool().newResourceInstance();
		resource.getStandardRate().setValue(10.0d);
		Assignment assignment = AssignmentService.getInstance()
			.newAssignment(existing, resource, 1.0d, 0L, this);
		Integer slot = Integer.valueOf(Snapshottable.BASELINE_1);
		existing.saveCurrentToSnapshot(slot);
		long priorStart = existing.getBaselineStart(Snapshottable.BASELINE_1);
		Assignment.BaselineValues imported = new Assignment.BaselineValues(priorStart,
			Long.valueOf(existing.getBaselineFinish(Snapshottable.BASELINE_1)), Long.valueOf(1_800_000L));
		assignment.importBaselineValues(Snapshottable.BASELINE_1,
			imported.start(), imported.finish(), imported.work());
		double priorCost = assignment.getBaselineCost(Snapshottable.BASELINE_1, null);
		double priorTaskCost = existing.getBaselineCost(Snapshottable.BASELINE_1);
		failOnce.set(true);

		assertSame(injected, assertThrows(RuntimeException.class,
			() -> original.saveCurrentToSnapshot(slot, false, List.of(existing, failing))));
		assertNull(failing.getSnapshot(slot));
		assertEquals(imported, assignment.getBaselineValues(Snapshottable.BASELINE_1));
		assertEquals(imported, assignment.getImportedBaselineValues(Snapshottable.BASELINE_1));
		assertEquals(priorCost, assignment.getBaselineCost(Snapshottable.BASELINE_1, null));

		Project reopened = loadFromBytes(saveProjectBytes(original));
		NormalTask restoredExisting = (NormalTask) findByName(reopened, "Existing baseline");
		NormalTask restoredFailing = (NormalTask) findByName(reopened, "Previously unbaselined task");
		Assignment restoredAssignment = (Assignment) restoredExisting.getAssignments().iterator().next();
		assertNotNull(restoredExisting.getSnapshot(slot), "the pre-failure baseline slot must survive reload");
		assertEquals(priorStart, restoredExisting.getBaselineStart(Snapshottable.BASELINE_1));
		assertEquals(priorTaskCost, restoredExisting.getBaselineCost(Snapshottable.BASELINE_1), 0.00001d);
		assertEquals(imported, restoredAssignment.getBaselineValues(Snapshottable.BASELINE_1));
		assertEquals(priorCost, restoredAssignment.getBaselineCost(Snapshottable.BASELINE_1, null), 0.00001d);
		assertNull(restoredFailing.getSnapshot(slot));
	}

	@Test
	void clearingAssignmentLevelingAfterMpoReloadPreservesManualAssignmentDelay() throws Exception {
		// Document-derived MSPDI contract: Assignment Delay is waiting time after the
		// task start, while Assignment LevelingDelay is the leveler-imposed offset.
		// Both are independent fields and must not be folded together on import.
		// https://learn.microsoft.com/en-us/office-project/xml-data-interchange/delay-element?view=project-client-2016
		// https://learn.microsoft.com/en-us/office-project/xml-data-interchange/levelingdelay-element?view=project-client-2016
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		long fixtureStart = java.time.Instant.parse("2025-01-06T08:00:00Z").toEpochMilli();
		original.setStartDate(fixtureStart);
		task.getCurrentSchedule().setStart(fixtureStart);
		task.getCurrentSchedule().setFinish(fixtureStart + 2L * 24L * 60L * 60L * 1000L);
		task.getCurrentSchedule().setRawDuration(8L * 60L * 60L * 1000L);
		Resource resource = original.getResourcePool().newResourceInstance();
		resource.setStandardRate(new com.microproject.datatype.Rate(
			80D / com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS),
			com.microproject.datatype.TimeUnit.HOURS));
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, this);
		assignment.setWork(8L * 60L * 60L * 1000L, null);
		long manualDelay = com.microproject.datatype.Duration.millis(3L * 60L * 60L * 1000L);
		long levelingDelay = com.microproject.datatype.Duration.millis(60L * 60L * 1000L);
		assignment.setDelay(manualDelay);
		assignment.setLevelingDelay(levelingDelay);
		original.recalculate();
		long expectedTaskStart = task.getStart();
		long expectedTaskFinish = task.getEnd();
		long expectedAssignmentStart = assignment.getStart();
		long expectedAssignmentFinish = assignment.getFinish();
		long expectedAssignmentWork = assignment.getWork(null);
		double expectedAssignmentCost = assignment.getCost(null);
		assertEquals(fixtureStart, expectedTaskStart,
			"the deterministic fixture starts at its explicit task date");
		assertEquals(expectedAssignmentFinish, expectedTaskFinish,
			"the deterministic task finish follows its sole assignment's work projection");
		assertEquals(8L * 60L * 60L * 1000L, expectedAssignmentWork,
			"the source fixture contributes eight hours of work");
		assertEquals(640D, expectedAssignmentCost, 0.0001D,
			"the source fixture contributes eight hours at the configured rate");
		assertEquals(assignment.getEffectiveWorkCalendar().add(expectedTaskStart,
			4L * 60L * 60L * 1000L, false), expectedAssignmentStart,
			"the assignment starts after the independent three-hour manual plus one-hour leveling delay");
		assertEquals(assignment.getEffectiveWorkCalendar().add(expectedAssignmentStart, expectedAssignmentWork, false),
			expectedAssignmentFinish, "the assignment finish follows its eight-hour work projection");
		assertEquals(3L * 60L * 60L * 1000L, com.microproject.datatype.Duration.millis(assignment.getDelay()),
			"the source assignment must keep manual delay independent from leveling delay");

		File initialMpo = writeTempMpo(original);
		byte[] mspdi = readEntries(Files.readAllBytes(initialMpo.toPath())).get(MpoFileImporter.PROJECT_ENTRY);
		String mspdiXml = new String(mspdi, StandardCharsets.UTF_8);
		assertTrue(mspdiXml.contains("<Delay>1800</Delay>"),
			"the MSPDI fixture must carry its independent 1800-tenths-of-a-minute manual Delay");
		assertTrue(mspdiXml.contains("<LevelingDelay>600</LevelingDelay>"),
			"the MSPDI fixture must carry its independent 600-tenths-of-a-minute LevelingDelay");
		Project externalMspdi = new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(mspdi), "xml", (progress, label) -> { });
		Assignment externalAssignment = firstRealAssignment(externalMspdi);
		assertEquals(3L * 60L * 60L * 1000L,
			com.microproject.datatype.Duration.millis(externalAssignment.getDelay()),
			"ordinary MSPDI import must restore the published manual Delay field");
		assertEquals(60L * 60L * 1000L,
			com.microproject.datatype.Duration.millis(externalAssignment.getLevelingDelay()));
		assertEquals(expectedTaskStart, firstTask(externalMspdi).getStart(),
			"restoring independent delay fields must not shift the imported task start");
		assertEquals(expectedTaskFinish, firstTask(externalMspdi).getEnd(),
			"restoring independent delay fields must not shift the imported task finish");
		assertEquals(expectedAssignmentStart, externalAssignment.getStart(),
			"the imported assignment must retain its independently calculated start projection");
		assertEquals(expectedAssignmentFinish, externalAssignment.getFinish(),
			"the imported assignment must retain its independently calculated finish projection");
		assertEquals(expectedAssignmentWork, externalAssignment.getWork(null),
			"restoring delay fields must preserve the imported work projection");
		assertEquals(expectedAssignmentCost, externalAssignment.getCost(null), 0.0001D,
			"restoring delay fields must preserve the imported cost projection");

		Project restored = load(initialMpo);
		Assignment restoredAssignment = firstRealAssignment(restored);
		long manualDelayAfterReload = restoredAssignment.getDelay();
		assertEquals(3L * 60L * 60L * 1000L,
			com.microproject.datatype.Duration.millis(manualDelayAfterReload),
			"MPO reload must preserve the MSPDI manual Delay independently of LevelingDelay");
		assertEquals(60L * 60L * 1000L,
			com.microproject.datatype.Duration.millis(restoredAssignment.getLevelingDelay()));
		assertEquals(expectedTaskStart, firstTask(restored).getStart());
		assertEquals(expectedTaskFinish, firstTask(restored).getEnd());
		assertEquals(expectedAssignmentStart, restoredAssignment.getStart());
		assertEquals(expectedAssignmentFinish, restoredAssignment.getFinish());
		assertEquals(expectedAssignmentWork, restoredAssignment.getWork(null));
		assertEquals(expectedAssignmentCost, restoredAssignment.getCost(null), 0.0001D);

		new ResourceLevelingService().clear(restored);
		assertEquals(manualDelayAfterReload, restoredAssignment.getDelay(),
			"Clear removes generated leveling delay but preserves independent manual assignment delay");
		assertEquals(0L, com.microproject.datatype.Duration.millis(restoredAssignment.getLevelingDelay()));
		assertEquals(restoredAssignment.getEffectiveWorkCalendar().add(expectedTaskStart,
			3L * 60L * 60L * 1000L, false), restoredAssignment.getStart(),
			"Clear retains the three-hour manual assignment start offset");

		Project clearedReload = load(writeTempMpo(restored));
		Assignment reloadedAssignment = firstRealAssignment(clearedReload);
		NormalTask clearedTask = (NormalTask) firstTask(clearedReload);
		assertEquals(manualDelayAfterReload, reloadedAssignment.getDelay(),
			"MPO save/reload after Clear retains the saved manual assignment delay");
		assertEquals(0L, com.microproject.datatype.Duration.millis(reloadedAssignment.getLevelingDelay()));
		assertEquals(expectedTaskStart, clearedTask.getStart());
		assertEquals(reloadedAssignment.getEffectiveWorkCalendar().add(expectedTaskStart,
			3L * 60L * 60L * 1000L, false), reloadedAssignment.getStart());
		assertEquals(reloadedAssignment.getEffectiveWorkCalendar().add(reloadedAssignment.getStart(),
			expectedAssignmentWork, false), reloadedAssignment.getFinish());
		assertEquals(expectedAssignmentWork, reloadedAssignment.getWork(null));
		assertEquals(expectedAssignmentCost, reloadedAssignment.getCost(null), 0.0001D);
	}

	@Test
	void mpoRoundTripDoesNotDoubleCountAssignmentLevelingDelayWhenManualDelayIsAbsent() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		long fixtureStart = java.time.Instant.parse("2025-01-06T08:00:00Z").toEpochMilli();
		original.setStartDate(fixtureStart);
		task.getCurrentSchedule().setStart(fixtureStart);
		task.getCurrentSchedule().setFinish(fixtureStart + 2L * 24L * 60L * 60L * 1000L);
		task.getCurrentSchedule().setRawDuration(8L * 60L * 60L * 1000L);
		Resource resource = original.getResourcePool().newResourceInstance();
		resource.setName("Leveling-only resource");
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, this);
		assignment.setWork(8L * 60L * 60L * 1000L, null);
		assignment.setDelay(0L);
		assignment.setLevelingDelay(8L * 60L * 60L * 1000L);
		original.recalculate();
		long expectedTaskStart = task.getStart();
		long expectedAssignmentStart = assignment.getStart();
		assertEquals(fixtureStart, expectedTaskStart);
		assertEquals(assignment.getEffectiveWorkCalendar().add(expectedTaskStart,
			8L * 60L * 60L * 1000L, false), expectedAssignmentStart);

		File saved = writeTempMpo(original);
		String mspdiXml = new String(readEntries(Files.readAllBytes(saved.toPath()))
			.get(MpoFileImporter.PROJECT_ENTRY), StandardCharsets.UTF_8);
		assertTrue(mspdiXml.contains("<LevelingDelay>4800</LevelingDelay>"),
			"the fixture must serialize its eight-hour assignment LevelingDelay");
		assertFalse(mspdiXml.contains("<Delay>"), "the fixture must omit an independent manual assignment Delay");

		Project restoredProject = load(saved);
		Assignment restored = firstRealAssignment(restoredProject);
		assertEquals(0L, restored.getDelay(),
			"MPO import must not infer a manual delay from a start already explained by LevelingDelay");
		assertEquals(8L * 60L * 60L * 1000L, restored.getLevelingDelay());
		assertEquals(expectedTaskStart, firstTask(restoredProject).getStart());
		assertEquals(expectedAssignmentStart, restored.getStart(),
			"MPO reopen must not apply the leveling offset twice to the assignment start");
	}

	@Test
	void resourceSpreadsheetNameAndRateFieldsSurviveMpoRoundTrip() throws Exception {
		// Document-derived interoperability rule: the Resource scalar StandardRate is the
		// current-date rate when a rate table exists; RatesFrom/To delimit entries in
		// tables A-E, and StandardRateFormat 2 means per hour. Keep the raw scalar and
		// table values consistent. This follows Microsoft's XML schema documentation,
		// not an empirical MSP run:
		// https://learn.microsoft.com/en-us/office-project/xml-data-interchange/rate-element?view=project-client-2016
		// https://learn.microsoft.com/en-us/office-project/xml-data-interchange/standardrateformat-element?view=project-client-2016
		// https://learn.microsoft.com/en-us/office-project/xml-data-interchange/xml-schema-for-the-resources-element?view=project-client-2016
		Project original = projectForRoundTrip();
		ResourcePool pool = original.getResourcePool();
		pool.setLocal(true);
		pool.addProject(original);
		com.microproject.grouping.core.model.NodeModel resourceModel = pool.getResourceOutline();
		resourceModel.setUndoController(pool.getUndoController());
		Resource resource = pool.createScriptedResource();
		resource.setName("Initial Resource");
		AssignmentService.getInstance().newAssignment((NormalTask) firstTask(original), resource, 1.0D, 0L, this);
		com.microproject.grouping.core.Node resourceNode = resourceModel.search(resource);
		resourceModel.setFieldValue(com.microproject.configuration.Configuration.getFieldFromId("Field.name"),
			resourceNode, this, "Published Resource", null, com.microproject.grouping.core.model.NodeModel.NORMAL);
		resourceModel.setFieldValue(com.microproject.configuration.Configuration.getFieldFromId("Field.standardRate"),
			resourceNode, this, new com.microproject.datatype.Rate(
				137.5D / com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS),
				com.microproject.datatype.TimeUnit.HOURS), null,
			com.microproject.grouping.core.model.NodeModel.NORMAL);
		long futureEffectiveDate = java.time.LocalDate.of(2027, 1, 1).atStartOfDay(java.time.ZoneOffset.UTC)
			.toInstant().toEpochMilli();
		com.microproject.pm.costing.CostRate futureDefaultRate = (com.microproject.pm.costing.CostRate)
			resource.getCostRateTable(0).newValueObject(futureEffectiveDate);
		futureDefaultRate.setStandardRate(new com.microproject.datatype.Rate(
			150.0D / com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS),
			com.microproject.datatype.TimeUnit.HOURS));
		com.microproject.pm.costing.CostRate alternateTableDefault = (com.microproject.pm.costing.CostRate)
			resource.getCostRateTable(1).getList().getFirst();
		alternateTableDefault.setStandardRate(new com.microproject.datatype.Rate(
			25.0D / com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS),
			com.microproject.datatype.TimeUnit.HOURS));
		alternateTableDefault.setOvertimeRate(new com.microproject.datatype.Rate(
			40.0D / com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS),
			com.microproject.datatype.TimeUnit.HOURS));
		alternateTableDefault.setCostPerUse(7.5D);
		com.microproject.pm.costing.CostRate futureRate = (com.microproject.pm.costing.CostRate)
			resource.getCostRateTable(1).newValueObject(futureEffectiveDate);
		futureRate.setStandardRate(new com.microproject.datatype.Rate(
			30.0D / com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS),
			com.microproject.datatype.TimeUnit.HOURS));
		futureRate.setOvertimeRate(new com.microproject.datatype.Rate(
			45.0D / com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS),
			com.microproject.datatype.TimeUnit.HOURS));
		futureRate.setCostPerUse(9.0D);
		assertEquals("Published Resource", resource.getName());
		double expectedInternalRate = 137.5D
			/ com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS);
		assertEquals(expectedInternalRate, resource.getStandardRate().getValue(), 0.0000001D);
		String displayedRate = com.microproject.datatype.RateFormat.getInstance(null, false, false, true)
			.format(resource.getStandardRate()).toString();
		assertTrue(displayedRate.contains("137.5"), displayedRate);

		byte[] bytes = saveMpo(original);
		String projectXml = new String(readEntries(bytes).get(MpoFileImporter.PROJECT_ENTRY), StandardCharsets.UTF_8);
		net.sf.mpxj.Resource mpxResource = new net.sf.mpxj.ProjectFile().addResource();
		com.microproject.server.data.MPXConverter.toMPXResource((com.microproject.pm.resource.ResourceImpl) resource,
			mpxResource);
		assertEquals(com.microproject.server.data.MPXConverter.toMPXRate(resource.getStandardRate()),
			mpxResource.getStandardRate());
		assertTrue(projectXml.contains("<StandardRate>137.5</StandardRate>"), projectXml);
		assertTrue(projectXml.contains("<StandardRateFormat>2</StandardRateFormat>"), projectXml);
		assertTrue(projectXml.contains("<RateTable>0</RateTable>"), projectXml);
		assertTrue(projectXml.contains("<RateTable>1</RateTable>"), projectXml);
		Project restored = loadFromBytes(bytes);
		com.microproject.pm.assignment.Assignment restoredAssignment =
			(com.microproject.pm.assignment.Assignment) ((NormalTask) firstTask(restored)).getAssignments().iterator().next();
		Resource restoredResource = restoredAssignment.getResource();
		assertEquals("Published Resource", restoredResource.getName());
		assertEquals(expectedInternalRate, restoredResource.getStandardRate().getValue(), 0.0000001D,
			"MPO reload must restore the current-date A-table scalar from the source XML");
		assertEquals(displayedRate, com.microproject.datatype.RateFormat.getInstance(null, false, false, true)
			.format(restoredResource.getStandardRate()).toString());
		com.microproject.pm.costing.CostRate restoredFutureDefault = (com.microproject.pm.costing.CostRate)
			restoredResource.getCostRateTable(0).findActive(futureEffectiveDate);
		assertEquals(futureEffectiveDate, restoredFutureDefault.getEffectiveDate());
		assertEquals(150.0D / com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS),
			restoredFutureDefault.getStandardRate().getValue(), 0.0000001D,
			"applying the current-date scalar must preserve the future A-table interval");
		com.microproject.pm.costing.CostRate restoredAlternateDefault = (com.microproject.pm.costing.CostRate)
			restoredResource.getCostRateTable(1).getList().getFirst();
		assertEquals(25.0D / com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS),
			restoredAlternateDefault.getStandardRate().getValue(), 0.0000001D);
		assertEquals(40.0D / com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS),
			restoredAlternateDefault.getOvertimeRate().getValue(), 0.0000001D);
		assertEquals(7.5D, restoredAlternateDefault.getCostPerUse(), 0.0000001D);
		com.microproject.pm.costing.CostRate restoredFutureRate = (com.microproject.pm.costing.CostRate)
			restoredResource.getCostRateTable(1).findActive(futureEffectiveDate);
		assertEquals(futureEffectiveDate, restoredFutureRate.getEffectiveDate());
		assertEquals(30.0D / com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS),
			restoredFutureRate.getStandardRate().getValue(), 0.0000001D);
		assertEquals(45.0D / com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS),
			restoredFutureRate.getOvertimeRate().getValue(), 0.0000001D);
		assertEquals(9.0D, restoredFutureRate.getCostPerUse(), 0.0000001D);
	}

	@Test
	void unassignedLeafTaskWorkAndScheduleSurviveMpoSaveReloadAndNoOpSave() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask unassigned = (NormalTask) firstTask(original);
		unassigned.setName("Unassigned leaf");
		long expectedUnassignedWork = 57_600_000L;
		long start = original.getStartDate();
		long finish = unassigned.getEffectiveWorkCalendar().add(start, expectedUnassignedWork, false);
		unassigned.getCurrentSchedule().setStart(start);
		unassigned.getCurrentSchedule().setFinish(finish);
		unassigned.getCurrentSchedule().setRawDuration(expectedUnassignedWork);
		unassigned.setFixedCost(17.5d);
		unassigned.restoreMpoUnassignedWork(expectedUnassignedWork);

		NormalTask assigned = (NormalTask) original.createLocalTaskNode(null).getImpl();
		assigned.setName("Assigned control");
		assigned.setDuration(2L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		Resource resource = original.getResourcePool().newResourceInstance();
		Assignment assignment = AssignmentService.getInstance().newAssignment(assigned, resource, 1D, 0L, this);

		assertEquals(57_600_000L, unassigned.getWork(null), "fixture must reproduce the reported pre-save Work");
		long expectedUnassignedStart = unassigned.getStart();
		long expectedUnassignedFinish = unassigned.getEnd();
		long expectedUnassignedDuration = unassigned.getDurationMillis();
		long expectedAssignedWork = assigned.getWork(null);
		long expectedAssignmentWork = assignment.getWork(null);
		long expectedAssignedStart = assigned.getStart();
		long expectedAssignedFinish = assigned.getEnd();

		byte[] firstSave = saveProjectBytes(original);
		String exportedMspdi = new String(readEntries(firstSave).get(MpoFileImporter.PROJECT_ENTRY),
			StandardCharsets.UTF_8);
		int taskName = exportedMspdi.indexOf("<Name>Unassigned leaf</Name>");
		int taskStart = exportedMspdi.lastIndexOf("<Task>", taskName);
		int taskFinish = exportedMspdi.indexOf("</Task>", taskName) + "</Task>".length();
		String exportedTask = exportedMspdi.substring(taskStart, taskFinish);
		java.util.regex.Matcher values = java.util.regex.Pattern
			.compile("<(Work|Duration|Start|Finish|ResourceUID)>(.*?)</\\1>").matcher(exportedTask);
		List<String> exportedValues = new ArrayList<>();
		while (values.find()) exportedValues.add(values.group(1) + "=" + values.group(2));
		int assignmentStart = exportedMspdi.indexOf("<Assignment>");
		int assignmentFinish = exportedMspdi.indexOf("</Assignment>", assignmentStart) + "</Assignment>".length();
		String exportedAssignment = exportedMspdi.substring(assignmentStart, assignmentFinish);
		java.util.regex.Matcher assignmentValues = java.util.regex.Pattern
			.compile("<(Work|Start|Finish|ResourceUID)>(.*?)</\\1>").matcher(exportedAssignment);
		List<String> exportedAssignmentValues = new ArrayList<>();
		while (assignmentValues.find())
			exportedAssignmentValues.add(assignmentValues.group(1) + "=" + assignmentValues.group(2));
		Project restored = loadFromBytes(firstSave);
		NormalTask restoredUnassigned = (NormalTask) findByName(restored, "Unassigned leaf");
		NormalTask restoredAssigned = (NormalTask) findByName(restored, "Assigned control");
		assertEquals(expectedUnassignedWork, restoredUnassigned.getWork(null),
			"MPO reload must preserve implicit Work for an unassigned normal leaf; exported fields=" + exportedValues
				+ ", assigned control export=" + exportedAssignmentValues
				+ ", before Work/Duration/Start/Finish=" + expectedUnassignedWork + "/" + expectedUnassignedDuration
				+ "/" + expectedUnassignedStart + "/" + expectedUnassignedFinish + ", after Work/Duration/Start/Finish="
				+ restoredUnassigned.getWork(null) + "/" + restoredUnassigned.getDurationMillis() + "/"
				+ restoredUnassigned.getStart() + "/" + restoredUnassigned.getEnd()
				+ "; assigned control before Work/Duration/Start/Finish=" + expectedAssignedWork + "/"
				+ assigned.getDurationMillis() + "/" + expectedAssignedStart + "/" + expectedAssignedFinish
				+ ", after=" + restoredAssigned.getWork(null) + "/" + restoredAssigned.getDurationMillis() + "/"
				+ restoredAssigned.getStart() + "/" + restoredAssigned.getEnd());
		assertEquals(expectedUnassignedStart, restoredUnassigned.getStart());
		assertEquals(expectedUnassignedFinish, restoredUnassigned.getEnd());
		assertEquals(expectedUnassignedDuration, restoredUnassigned.getDurationMillis());
		assertEquals(expectedAssignedWork, restoredAssigned.getWork(null));
		assertEquals(expectedAssignmentWork,
			((Assignment) restoredAssigned.getAssignments().iterator().next()).getWork(null));
		assertEquals(expectedAssignedStart, restoredAssigned.getStart());
		assertEquals(expectedAssignedFinish, restoredAssigned.getEnd());

		Project noOpReload = loadFromBytes(saveProjectBytes(restored));
		NormalTask noOpUnassigned = (NormalTask) findByName(noOpReload, "Unassigned leaf");
		NormalTask noOpAssigned = (NormalTask) findByName(noOpReload, "Assigned control");
		assertEquals(expectedUnassignedWork, noOpUnassigned.getWork(null),
			"a no-op MPO resave must not drift implicit Work for an unassigned normal leaf");
		assertEquals(expectedUnassignedStart, noOpUnassigned.getStart());
		assertEquals(expectedUnassignedFinish, noOpUnassigned.getEnd());
		assertEquals(expectedUnassignedDuration, noOpUnassigned.getDurationMillis());
		assertEquals(expectedAssignedWork, noOpAssigned.getWork(null));
		assertEquals(expectedAssignmentWork,
			((Assignment) noOpAssigned.getAssignments().iterator().next()).getWork(null));
		assertEquals(expectedAssignedStart, noOpAssigned.getStart());
		assertEquals(expectedAssignedFinish, noOpAssigned.getEnd());
	}

	/**
	 * Provenance for #868 (document-derived compatibility decision, see
	 * https://support.microsoft.com/en-us/project/work-task-field): the Work field of a
	 * task is user-editable; MPXJ/MSPDI/MPP task Work is not imported (task Work derives
	 * from assignments), so the only production route producing an unassigned leaf whose
	 * Work differs from its duration-derived default is editing Work on the task itself
	 * (NormalTask.setWork). The value reached through that route must survive MPO
	 * save, reload and a no-op resave together with Duration, Start and Finish.
	 */
	@Test
	void unassignedLeafWorkEditedThroughTaskFieldSurvivesMpoRoundTrip() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		task.setName("Edited unassigned leaf");
		long defaultWork = task.getWork(null);
		long editedWork = defaultWork + 4L * 3_600_000L;
		task.setWork(editedWork);
		long work = task.getWork(null);
		long start = task.getStart();
		long finish = task.getEnd();
		long duration = task.getDurationMillis();
		assertNotEquals(defaultWork, work, "editing Work on an unassigned leaf must diverge from the default");

		Project restored = loadFromBytes(saveProjectBytes(original));
		NormalTask reloaded = (NormalTask) findByName(restored, "Edited unassigned leaf");
		assertEquals(work, reloaded.getWork(null));
		assertEquals(start, reloaded.getStart());
		assertEquals(finish, reloaded.getEnd());
		assertEquals(duration, reloaded.getDurationMillis());

		Project resaved = loadFromBytes(saveProjectBytes(restored));
		NormalTask again = (NormalTask) findByName(resaved, "Edited unassigned leaf");
		assertEquals(work, again.getWork(null));
		assertEquals(start, again.getStart());
		assertEquals(finish, again.getEnd());
		assertEquals(duration, again.getDurationMillis());
	}

	@Test
	void mpoTaskScheduleRejectsInvalidUnassignedWorkMetadata() throws Exception {
		Project original = projectForRoundTrip();
		Map<String, byte[]> saved = readEntries(saveProjectBytes(original));
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		com.fasterxml.jackson.databind.JsonNode source = mapper.readTree(saved.get(MpoFileImporter.TASK_SCHEDULES_ENTRY));
		assertTrue(source.path("tasks").get(0).has("unassignedWork"),
			"unassigned normal leaf must serialize its MPO work extension");

		for (String invalid : List.of("-1", "1.5", "\"invalid\"", "null")) {
			Map<String, byte[]> malformed = new LinkedHashMap<>(saved);
			com.fasterxml.jackson.databind.node.ObjectNode root = (com.fasterxml.jackson.databind.node.ObjectNode)
				mapper.readTree(malformed.get(MpoFileImporter.TASK_SCHEDULES_ENTRY));
			((com.fasterxml.jackson.databind.node.ObjectNode) root.path("tasks").get(0))
				.set("unassignedWork", mapper.readTree(invalid));
			malformed.put(MpoFileImporter.TASK_SCHEDULES_ENTRY, mapper.writeValueAsBytes(root));
			updateEntryChecksum(malformed, MpoFileImporter.TASK_SCHEDULES_ENTRY);
			assertThrows(IOException.class, () -> loadFromBytes(zip(malformed).toByteArray()),
				"invalid MPO unassigned work must fail to load: " + invalid);
		}

		NormalTask assigned = (NormalTask) original.createLocalTaskNode(null).getImpl();
		Resource resource = original.getResourcePool().newResourceInstance();
		AssignmentService.getInstance().newAssignment(assigned, resource, 1D, 0L, this);
		Map<String, byte[]> assignedMetadata = readEntries(saveProjectBytes(original));
		com.fasterxml.jackson.databind.node.ObjectNode assignedRoot = (com.fasterxml.jackson.databind.node.ObjectNode)
			mapper.readTree(assignedMetadata.get(MpoFileImporter.TASK_SCHEDULES_ENTRY));
		for (com.fasterxml.jackson.databind.JsonNode taskNode : assignedRoot.path("tasks"))
			((com.fasterxml.jackson.databind.node.ObjectNode) taskNode).put("unassignedWork", 1L);
		assignedMetadata.put(MpoFileImporter.TASK_SCHEDULES_ENTRY, mapper.writeValueAsBytes(assignedRoot));
		updateEntryChecksum(assignedMetadata, MpoFileImporter.TASK_SCHEDULES_ENTRY);
		assertThrows(IOException.class, () -> loadFromBytes(zip(assignedMetadata).toByteArray()),
			"MPO unassigned work must not be applied to a task with a real assignment");

		Project hierarchy = projectForRoundTrip();
		NormalTask summary = (NormalTask) firstTask(hierarchy);
		NormalTask child = hierarchy.createScriptedTask();
		var model = hierarchy.getTaskModel();
		var summaryNode = model.search(summary);
		assertTrue(model.relocate(List.of(model.search(child)), summaryNode, 0,
			com.microproject.grouping.core.model.NodeModel.SILENT));
		summary.setWbsChildrenNodes(model.getHierarchy().getChildren(summaryNode));
		child.setWbsParent(summary);
		Map<String, byte[]> parentMetadata = readEntries(saveProjectBytes(hierarchy));
		com.fasterxml.jackson.databind.node.ObjectNode parentRoot = (com.fasterxml.jackson.databind.node.ObjectNode)
			mapper.readTree(parentMetadata.get(MpoFileImporter.TASK_SCHEDULES_ENTRY));
		for (com.fasterxml.jackson.databind.JsonNode taskNode : parentRoot.path("tasks"))
			((com.fasterxml.jackson.databind.node.ObjectNode) taskNode).put("unassignedWork", 1L);
		parentMetadata.put(MpoFileImporter.TASK_SCHEDULES_ENTRY, mapper.writeValueAsBytes(parentRoot));
		updateEntryChecksum(parentMetadata, MpoFileImporter.TASK_SCHEDULES_ENTRY);
		assertThrows(IOException.class, () -> loadFromBytes(zip(parentMetadata).toByteArray()),
			"MPO unassigned work must not be applied to a WBS parent");
	}

	@Test
	void mpoWithoutUnassignedWorkExtensionLoadsWithLegacyComputedWork() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		task.setDuration(3L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		Map<String, byte[]> legacy = readEntries(saveProjectBytes(original));
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		com.fasterxml.jackson.databind.node.ObjectNode root = (com.fasterxml.jackson.databind.node.ObjectNode)
			mapper.readTree(legacy.get(MpoFileImporter.TASK_SCHEDULES_ENTRY));
		for (com.fasterxml.jackson.databind.JsonNode taskNode : root.path("tasks"))
			((com.fasterxml.jackson.databind.node.ObjectNode) taskNode).remove("unassignedWork");
		legacy.put(MpoFileImporter.TASK_SCHEDULES_ENTRY, mapper.writeValueAsBytes(root));
		updateEntryChecksum(legacy, MpoFileImporter.TASK_SCHEDULES_ENTRY);

		Project restored = loadFromBytes(zip(legacy).toByteArray());
		assertEquals(28_800_000L, ((NormalTask) firstTask(restored)).getWork(null),
			"an older MPO without the additive field must retain the legacy MSPDI-computed task Work");
	}

	@Test
	void changingDurationClearsImportedUnassignedWorkOverride() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		task.setDuration(3L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		byte[] saved = saveProjectBytes(original);
		Project withOverride = loadFromBytes(saved);
		NormalTask restored = (NormalTask) firstTask(withOverride);
		long importedWork = restored.getWork(null);
		assertEquals(task.getWork(null), importedWork);

		Map<String, byte[]> legacy = readEntries(saved);
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		com.fasterxml.jackson.databind.node.ObjectNode root = (com.fasterxml.jackson.databind.node.ObjectNode)
			mapper.readTree(legacy.get(MpoFileImporter.TASK_SCHEDULES_ENTRY));
		for (com.fasterxml.jackson.databind.JsonNode taskNode : root.path("tasks"))
			((com.fasterxml.jackson.databind.node.ObjectNode) taskNode).remove("unassignedWork");
		legacy.put(MpoFileImporter.TASK_SCHEDULES_ENTRY, mapper.writeValueAsBytes(root));
		updateEntryChecksum(legacy, MpoFileImporter.TASK_SCHEDULES_ENTRY);
		Project withoutOverride = loadFromBytes(zip(legacy).toByteArray());
		NormalTask legacyTask = (NormalTask) firstTask(withoutOverride);

		long newDuration = 2L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		restored.setDuration(newDuration);
		legacyTask.setDuration(newDuration);
		assertEquals(legacyTask.getWork(null), restored.getWork(null),
			"after an ordinary duration edit, MPO-restored scalar Work must not mask the recalculated task Work");
		assertNotEquals(importedWork, restored.getWork(null),
			"a duration edit must clear the imported scalar Work override");
	}

	@Test
	void addingAndRemovingRealAssignmentClearsImportedUnassignedWorkOverride() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		task.setDuration(3L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		byte[] saved = saveProjectBytes(original);
		NormalTask restored = (NormalTask) firstTask(loadFromBytes(saved));

		Map<String, byte[]> legacy = readEntries(saved);
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		com.fasterxml.jackson.databind.node.ObjectNode root = (com.fasterxml.jackson.databind.node.ObjectNode)
			mapper.readTree(legacy.get(MpoFileImporter.TASK_SCHEDULES_ENTRY));
		for (com.fasterxml.jackson.databind.JsonNode taskNode : root.path("tasks"))
			((com.fasterxml.jackson.databind.node.ObjectNode) taskNode).remove("unassignedWork");
		legacy.put(MpoFileImporter.TASK_SCHEDULES_ENTRY, mapper.writeValueAsBytes(root));
		updateEntryChecksum(legacy, MpoFileImporter.TASK_SCHEDULES_ENTRY);
		NormalTask legacyTask = (NormalTask) firstTask(loadFromBytes(zip(legacy).toByteArray()));

		Assignment added = AssignmentService.getInstance().newAssignment(restored,
			restored.getProject().getResourcePool().newResourceInstance(), 1D, 0L, this);
		added.setWork(28_800_000L, null);
		AssignmentService.getInstance().remove(added, this, true);
		Assignment legacyAdded = AssignmentService.getInstance().newAssignment(legacyTask,
			legacyTask.getProject().getResourcePool().newResourceInstance(), 1D, 0L, this);
		legacyAdded.setWork(28_800_000L, null);
		AssignmentService.getInstance().remove(legacyAdded, this, true);

		assertEquals(legacyTask.getWork(null), restored.getWork(null),
			"after a real assignment is added and removed, Work must match the legacy model without an MPO override");
		java.lang.reflect.Field importedWorkField = NormalTask.class.getDeclaredField("importedUnassignedWork");
		importedWorkField.setAccessible(true);
		assertNull(importedWorkField.get(restored),
			"adding and removing a real assignment must discard the imported scalar, even if its current value matches");
	}

	@Test
	void leafTaskPercentCompleteSurvivesMpoSaveReloadAndNoOpSave() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		task.setImportedPercentComplete(2D / 3D);

		Project restored = loadFromBytes(saveProjectBytes(original));
		assertEquals(2D / 3D, firstTask(restored).getPercentComplete(), 0D,
			"native MPO schedule metadata must retain fractional progress beyond MSPDI's integer percentage");

		Project noOpReload = loadFromBytes(saveProjectBytes(restored));
		assertEquals(2D / 3D, firstTask(noOpReload).getPercentComplete(), 0D,
			"a no-op MPO save must preserve the exact task percentage on the next reload");
	}

	@Test
	void mpoTaskScheduleWithoutPercentCompleteRetainsLegacyLoadBehavior() throws Exception {
		Project original = projectForRoundTrip();
		((NormalTask) firstTask(original)).setImportedPercentComplete(2D / 3D);
		Map<String, byte[]> legacy = readEntries(saveProjectBytes(original));
		String taskSchedules = new String(legacy.get(MpoFileImporter.TASK_SCHEDULES_ENTRY), StandardCharsets.UTF_8);
		String withoutPercent = taskSchedules.replaceAll(",\\\"percentComplete\\\":[^,}]+", "");
		assertNotEquals(taskSchedules, withoutPercent, "fixture must remove the additive native percent field");
		legacy.put(MpoFileImporter.TASK_SCHEDULES_ENTRY, withoutPercent.getBytes(StandardCharsets.UTF_8));
		updateEntryChecksum(legacy, MpoFileImporter.TASK_SCHEDULES_ENTRY);

		Project restored = loadFromBytes(zip(legacy).toByteArray());
		assertTrue(Double.isFinite(firstTask(restored).getPercentComplete()),
			"archives written before native exact percentages must continue to load");
	}

	@Test
	void mpoTaskScheduleRejectsInvalidLeafPercentCompleteMetadata() throws Exception {
		Project original = projectForRoundTrip();
		((NormalTask) firstTask(original)).setImportedPercentComplete(2D / 3D);
		Map<String, byte[]> saved = readEntries(saveProjectBytes(original));
		String taskSchedules = new String(saved.get(MpoFileImporter.TASK_SCHEDULES_ENTRY), StandardCharsets.UTF_8);
		assertTrue(taskSchedules.contains("\"percentComplete\":"), "fixture must contain native precision metadata");

		for (String invalid : List.of("null", "\"invalid\"", "-0.01", "1.01")) {
			Map<String, byte[]> malformed = new LinkedHashMap<>(saved);
			String corrupted = taskSchedules.replaceFirst("\\\"percentComplete\\\":[^,}]+",
				java.util.regex.Matcher.quoteReplacement("\"percentComplete\":" + invalid));
			malformed.put(MpoFileImporter.TASK_SCHEDULES_ENTRY, corrupted.getBytes(StandardCharsets.UTF_8));
			updateEntryChecksum(malformed, MpoFileImporter.TASK_SCHEDULES_ENTRY);
			assertThrows(IOException.class, () -> loadFromBytes(zip(malformed).toByteArray()),
				"invalid native task percentage must fail to load: " + invalid);
		}
	}

	@Test
	void statusDateSurvivesMpoRoundTrip() throws Exception {
		Project original = projectForRoundTrip();
		long expected = firstTask(original).getEnd();
		original.setStatusDate(expected);
		Project restored = load(writeTempMpo(original));
		assertTrue(restored.isStatusDateSet());
		assertEquals(original.getStatusDate(), restored.getStatusDate());
	}

	@Test
	void markOnTrackPreservesIndependentAssignmentProgressAndWorkAcrossMpoRoundTrip() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		var resource = original.getResourcePool().newResourceInstance();
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, this);
		long totalWork = assignment.getWork(null);
		assignment.setActualWork(totalWork / 4L, null);
		assignment.setRemainingWork(totalWork * 3L / 4L, null);
		assignment.setPercentComplete(0.37D);
		task.setImportedPercentComplete(0.10D);
		long statusDate = task.getStart() + (task.getEnd() - task.getStart()) / 2L;
		original.setStatusDate(statusDate);
		long expectedStatusDate = original.getStatusDate();
		double assignmentPercent = assignment.getPercentComplete();
		long actualWork = assignment.getActualWork(null);
		long remainingWork = assignment.getRemainingWork();
		long assignmentWork = assignment.getWork(null);
		long taskWork = task.getWork(null);

		new TaskProgressService().markOnTrack(original, List.of(task));
		double markedTaskPercent = task.getPercentComplete();
		assertNotEquals(0.10D, markedTaskPercent, 0.00001D);
		assertEquals(assignmentPercent, assignment.getPercentComplete(), 0.00001D);
		assertEquals(actualWork, assignment.getActualWork(null));
		assertEquals(remainingWork, assignment.getRemainingWork());

		Project reopened = loadFromBytes(saveProjectBytes(original));
		NormalTask reopenedTask = (NormalTask) firstTask(reopened);
		Assignment reopenedAssignment = (Assignment) reopenedTask.getAssignments().iterator().next();
		assertTrue(reopened.isStatusDateSet());
		assertEquals(expectedStatusDate, reopened.getStatusDate());
		assertEquals(markedTaskPercent, reopenedTask.getPercentComplete(), 0.00001D);
		assertEquals(assignmentPercent, reopenedAssignment.getPercentComplete(), 0.00001D);
		assertEquals(actualWork, reopenedAssignment.getActualWork(null));
		assertEquals(remainingWork, reopenedAssignment.getRemainingWork());
		assertEquals(assignmentWork, reopenedAssignment.getWork(null));
		assertEquals(taskWork, reopenedTask.getWork(null));
	}

	@Test
	void updateProjectRequestStateSurvivesMpoRoundTrip() throws Exception {
		Project original = projectForRoundTrip();
		Task task = firstTask(original);
		boolean statusDateWasSet = original.isStatusDateSet();
		long originalStatusDate = original.getStatusDate();
		long statusDate = task.getEnd() + 86_400_000L;
		UpdateProjectRequest request = new UpdateProjectRequest(statusDate, true, false, true);
		new UpdateProjectCommand(original, request).accept(task);
		assertEquals(1D, task.getPercentComplete(), 0.00001D,
			"Update Project must complete the source task before export");

		Project restored = load(writeTempMpo(original));
		assertEquals(statusDateWasSet, original.isStatusDateSet(),
			"Update Project uses its target date without changing the project Status Date setting");
		assertEquals(statusDateWasSet, restored.isStatusDateSet(),
			"MPO reload must preserve the unchanged Status Date setting");
		if (statusDateWasSet)
			assertEquals(originalStatusDate, restored.getStatusDate());
		assertEquals(1D, firstTask(restored).getPercentComplete(), 0.00001D,
			"Update Project actual progress must survive MPO reload");
	}

	@Test
	void mpoManifestStatusDatePreservesExplicitUnsetAndRejectsInvalidTimestamps() throws Exception {
		Project original = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(original, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		String manifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		String withExplicitUnset = manifest.replace("projectSha256=\"", "statusDate=\"0\" projectSha256=\"");
		entries.put(MpoFileImporter.MANIFEST_ENTRY, withExplicitUnset.getBytes(StandardCharsets.UTF_8));
		Project restored = loadFromBytes(zip(entries).toByteArray());
		assertFalse(restored.isStatusDateSet(), "zero in the MPO manifest means the explicit setting is unset");

		for (String invalid : java.util.List.of("-1", Long.toString(Long.MAX_VALUE))) {
			String invalidManifest = manifest.replace("projectSha256=\"", "statusDate=\"" + invalid + "\" projectSha256=\"");
			entries.put(MpoFileImporter.MANIFEST_ENTRY, invalidManifest.getBytes(StandardCharsets.UTF_8));
			assertThrows(IOException.class, () -> loadFromBytes(zip(entries).toByteArray()),
				"out-of-range persisted status date must fail before model restore: " + invalid);
		}
	}

	private static File writeTempMpo(Project project) throws Exception {
		File output = File.createTempFile("task-mode-roundtrip-", ".mpo");
		output.deleteOnExit();
		try (java.io.OutputStream stream = java.nio.file.Files.newOutputStream(output.toPath())) {
			new MpoFileImporter().saveProject(project, stream);
		}
		return output;
	}

	/** MSPDI export preserves unique ids only when they are positive (see MPXConverter.exportId). */
	private static void assignPositiveUniqueIds(Project project) {
		long next = 1L;
		for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext(); next++) {
			((com.microproject.pm.task.Task) tasks.next()).setUniqueId(next);
		}
	}

	private static int taskCount(Project project) {
		int count = 0;
		for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			tasks.next();
			count++;
		}
		return count;
	}

	private static String taskOutlineIdentitySummary(Project project) {
		List<String> identities = new ArrayList<>();
		for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			Task task = (Task) tasks.next();
			identities.add(task.getUniqueId() + ":" + task.getName());
		}
		return identities.toString();
	}

	private static Project load(File mpo) throws Exception {
		MpoFileImporter reader = new MpoFileImporter();
		reader.setFileName(mpo.getAbsolutePath());
		reader.setProjectFactory(ProjectFactory.getInstance());
		reader.importFile();
		return reader.getProject();
	}

	@Test
	void separateJvmDependencyLagConflictReturnsDurableRecoveryCopy() throws Exception {
		Project initial = projectForRoundTrip();
		NormalTask predecessor = (NormalTask) firstTask(initial);
		NormalTask successor = (NormalTask) initial.createLocalTaskNode(null).getImpl();
		successor.setName("Dependency successor");
		assignPositiveUniqueIds(initial);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, null);
		File shared = File.createTempFile("mpo-process-dependency-conflict", ".mpo");
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Path tempDirectory = Files.createTempDirectory("mpo-process-dependency-" + System.nanoTime());
		Path release = tempDirectory.resolve("release.flag");
		Path firstReady = tempDirectory.resolve("first-ready.flag");
		Path secondReady = tempDirectory.resolve("second-ready.flag");
		Path firstLog = tempDirectory.resolve("first-worker.log");
		Path secondLog = tempDirectory.resolve("second-worker.log");
		Process first = null;
		Process second = null;
		Path recoveryCopy = null;
		try {
			String field = "dependencyLag:" + successorId;
			first = startMpoSaveWorker(shared, predecessorId, field, "3600000", firstReady, release, firstLog);
			second = startMpoSaveWorker(shared, predecessorId, field, "7200000", secondReady, release, secondLog);
			awaitWorkerReady(first, firstReady, firstLog);
			awaitWorkerReady(second, secondReady, secondLog);
			Files.createFile(release);
			assertWorkerSucceeded(first, firstLog);
			assertWorkerSucceeded(second, secondLog);

			List<String> recoveryMarkers = java.util.stream.Stream.of(readWorkerLog(firstLog), readWorkerLog(secondLog))
					.flatMap(value -> value.lines()).filter(value -> value.startsWith("MPO_CONFLICT_RECOVERY=")).toList();
			assertEquals(1, recoveryMarkers.size(), "exactly one process should preserve its dependency branch");
			recoveryCopy = Path.of(recoveryMarkers.getFirst().substring("MPO_CONFLICT_RECOVERY=".length()));
			assertTrue(Files.isRegularFile(recoveryCopy));
			long sharedLag = dependencyLag(load(shared), predecessorId, successorId);
			long recoveredLag = dependencyLag(load(recoveryCopy.toFile()), predecessorId, successorId);
			assertTrue(sharedLag > 0L, "the winning dependency edit must survive reload");
			assertTrue(recoveredLag > 0L, "the recovery dependency edit must survive reload");
			org.junit.jupiter.api.Assertions.assertNotEquals(sharedLag, recoveredLag);
			OperationLog.DocumentLog recoveryLog = new OperationLog().readJsonl(
					readEntries(Files.readAllBytes(recoveryCopy)).get(MpoFileImporter.OPERATIONS_ENTRY));
			assertFalse(new OperationLog().merge(recoveryLog.operations()).conflicts().isEmpty());
		} finally {
			stopWorker(first);
			stopWorker(second);
			if (recoveryCopy != null) Files.deleteIfExists(recoveryCopy);
			Files.deleteIfExists(release);
			Files.deleteIfExists(firstReady);
			Files.deleteIfExists(secondReady);
			Files.deleteIfExists(firstLog);
			Files.deleteIfExists(secondLog);
			Files.deleteIfExists(tempDirectory);
			Files.deleteIfExists(Path.of(shared.getAbsolutePath() + ".lock"));
			Files.deleteIfExists(shared.toPath());
		}
	}

	@Test
	void separateJvmSaveWaitsForProcessHoldingTransactionLock() throws Exception {
		Project initial = projectForRoundTrip();
		assignPositiveUniqueIds(initial);
		long taskId = firstTask(initial).getUniqueId();
		File shared = File.createTempFile("mpo-process-lock-wait", ".mpo");
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();
		byte[] originalArchive = Files.readAllBytes(shared.toPath());

		Path lockPath = Path.of(shared.getAbsolutePath() + ".lock");
		Path tempDirectory = Files.createTempDirectory("mpo-process-lock-wait-" + System.nanoTime());
		Path release = tempDirectory.resolve("release.flag");
		Path ready = tempDirectory.resolve("worker-ready.flag");
		Path log = tempDirectory.resolve("worker.log");
		Process worker = null;
		try (java.nio.channels.FileChannel lockChannel = java.nio.channels.FileChannel.open(lockPath,
				java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE);
				java.nio.channels.FileLock heldLock = lockChannel.lock()) {
			worker = startMpoSaveWorker(shared, taskId, "name", "Saved after lock release", ready, release, log);
			awaitWorkerReady(worker, ready, log);
			Files.createFile(release);
			long attemptDeadline = System.nanoTime()
					+ java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
			while (!readWorkerLog(log).contains("MPO_SAVE_ATTEMPT=")) {
				if (!worker.isAlive())
					throw new AssertionError("MPO worker exited before attempting its save: " + readWorkerLog(log));
				if (System.nanoTime() >= attemptDeadline)
					throw new AssertionError("MPO worker did not attempt save in time: " + readWorkerLog(log));
				Thread.sleep(20L);
			}
			assertTrue(worker.isAlive(), "save must wait while another process owns the transaction lock");
			org.junit.jupiter.api.Assertions.assertArrayEquals(originalArchive, Files.readAllBytes(shared.toPath()),
					"the shared archive must remain unchanged while the lock is held");
			heldLock.release();
			assertWorkerSucceeded(worker, log);
			assertEquals("Saved after lock release", firstTask(load(shared)).getName());
		} finally {
			stopWorker(worker);
			Files.deleteIfExists(ready);
			Files.deleteIfExists(release);
			Files.deleteIfExists(log);
			Files.deleteIfExists(tempDirectory);
			Files.deleteIfExists(lockPath);
			Files.deleteIfExists(shared.toPath());
		}
	}

	@Test
	void separateJvmTaskDeleteConflictsWithConcurrentUpdate() throws Exception {
		Project initial = projectForRoundTrip();
		assignPositiveUniqueIds(initial);
		long taskId = firstTask(initial).getUniqueId();
		File shared = File.createTempFile("mpo-process-delete-conflict", ".mpo");
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Path tempDirectory = Files.createTempDirectory("mpo-process-delete-conflict-" + System.nanoTime());
		Path release = tempDirectory.resolve("release.flag");
		Path deleteReady = tempDirectory.resolve("delete-ready.flag");
		Path updateReady = tempDirectory.resolve("update-ready.flag");
		Path deleteLog = tempDirectory.resolve("delete-worker.log");
		Path updateLog = tempDirectory.resolve("update-worker.log");
		Process deleteWorker = null;
		Process updateWorker = null;
		Path recoveryCopy = null;
		try {
			deleteWorker = startMpoSaveWorker(shared, taskId, "delete", "", deleteReady, release, deleteLog);
			updateWorker = startMpoSaveWorker(shared, taskId, "name", "Updated concurrently", updateReady, release,
					updateLog);
			awaitWorkerReady(deleteWorker, deleteReady, deleteLog);
			awaitWorkerReady(updateWorker, updateReady, updateLog);
			Files.createFile(release);
			assertWorkerSucceeded(deleteWorker, deleteLog);
			assertWorkerSucceeded(updateWorker, updateLog);

			List<String> recoveryMarkers = java.util.stream.Stream.of(readWorkerLog(deleteLog), readWorkerLog(updateLog))
					.flatMap(value -> value.lines()).filter(value -> value.startsWith("MPO_CONFLICT_RECOVERY=")).toList();
			assertEquals(1, recoveryMarkers.size(), "one process must preserve its delete/update branch");
			recoveryCopy = Path.of(recoveryMarkers.getFirst().substring("MPO_CONFLICT_RECOVERY=".length()));
			assertTrue(Files.isRegularFile(recoveryCopy));
			Project sharedProject = load(shared);
			Project recoveredProject = load(recoveryCopy.toFile());
			assertTrue((sharedProject.findByUniqueId(taskId) == null)
					!= (recoveredProject.findByUniqueId(taskId) == null),
					"shared and recovery archives must preserve opposite delete/update outcomes");
			Project updateBranch = sharedProject.findByUniqueId(taskId) == null ? recoveredProject : sharedProject;
			assertEquals("Updated concurrently", updateBranch.findByUniqueId(taskId).getName(),
					"the surviving task branch must retain the concurrent update");
			OperationLog.DocumentLog recoveryLog = new OperationLog().readJsonl(
					readEntries(Files.readAllBytes(recoveryCopy)).get(MpoFileImporter.OPERATIONS_ENTRY));
			assertTrue(recoveryLog.operations().size() >= 2,
					"the recovery journal must retain both competing branches");
			assertTrue(recoveryLog.operations().stream().anyMatch(operation -> operation.kind().equals("task.delete")));
			assertTrue(recoveryLog.operations().stream().anyMatch(operation -> operation.kind().equals("task.update")));
			assertEquals(1, new OperationLog().merge(recoveryLog.operations()).conflicts().size());
		} finally {
			stopWorker(deleteWorker);
			stopWorker(updateWorker);
			if (recoveryCopy != null) Files.deleteIfExists(recoveryCopy);
			Files.deleteIfExists(release);
			Files.deleteIfExists(deleteReady);
			Files.deleteIfExists(updateReady);
			Files.deleteIfExists(deleteLog);
			Files.deleteIfExists(updateLog);
			Files.deleteIfExists(tempDirectory);
			Files.deleteIfExists(Path.of(shared.getAbsolutePath() + ".lock"));
			Files.deleteIfExists(shared.toPath());
		}
	}

	@Test
	void separateJvmAssignmentAddsWithDifferentUnitsConflict() throws Exception {
		Project initial = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(initial);
		Resource resource = initial.getResourcePool().newResourceInstance();
		resource.setName("Concurrent assignment resource");
		NormalTask resourceAnchor = (NormalTask) initial.createLocalTaskNode(null).getImpl();
		resourceAnchor.setName("Resource identity anchor");
		AssignmentService.getInstance().newAssignment(resourceAnchor, resource, 1D, 0L, null, false);
		String resourceName = resource.getName();
		assignPositiveUniqueIds(initial);
		long taskId = task.getUniqueId();
		File shared = File.createTempFile("mpo-process-assignment-conflict", ".mpo");
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Path tempDirectory = Files.createTempDirectory("mpo-process-assignment-conflict-" + System.nanoTime());
		Path release = tempDirectory.resolve("release.flag");
		Path firstReady = tempDirectory.resolve("first-ready.flag");
		Path secondReady = tempDirectory.resolve("second-ready.flag");
		Path firstLog = tempDirectory.resolve("first-worker.log");
		Path secondLog = tempDirectory.resolve("second-worker.log");
		Process first = null;
		Process second = null;
		Path recoveryCopy = null;
		try {
			String field = "assignmentUnits:" + resourceName;
			first = startMpoSaveWorker(shared, taskId, field, "0.5", firstReady, release, firstLog);
			second = startMpoSaveWorker(shared, taskId, field, "1.0", secondReady, release, secondLog);
			awaitWorkerReady(first, firstReady, firstLog);
			awaitWorkerReady(second, secondReady, secondLog);
			Files.createFile(release);
			assertWorkerSucceeded(first, firstLog);
			assertWorkerSucceeded(second, secondLog);

			List<String> recoveryMarkers = java.util.stream.Stream.of(readWorkerLog(firstLog), readWorkerLog(secondLog))
					.flatMap(value -> value.lines()).filter(value -> value.startsWith("MPO_CONFLICT_RECOVERY=")).toList();
			assertEquals(1, recoveryMarkers.size(), "one process must preserve its assignment branch");
			recoveryCopy = Path.of(recoveryMarkers.getFirst().substring("MPO_CONFLICT_RECOVERY=".length()));
			assertTrue(Files.isRegularFile(recoveryCopy));
			double sharedUnits = assignmentUnits(load(shared), taskId, resourceName);
			double recoveredUnits = assignmentUnits(load(recoveryCopy.toFile()), taskId, resourceName);
			OperationLog.DocumentLog recoveryLog = new OperationLog().readJsonl(
					readEntries(Files.readAllBytes(recoveryCopy)).get(MpoFileImporter.OPERATIONS_ENTRY));
			org.junit.jupiter.api.Assertions.assertNotEquals(sharedUnits, recoveredUnits,
					"the shared and recovery archives must preserve different assignment units; shared=" + sharedUnits
						+ ", recovery=" + recoveredUnits + ", operations=" + recoveryLog.operations());
			assertTrue(java.util.Set.of(0.5D, 1D).contains(sharedUnits));
			assertTrue(java.util.Set.of(0.5D, 1D).contains(recoveredUnits));
			assertTrue(recoveryLog.operations().stream().anyMatch(operation -> operation.kind().equals("assignment.add")));
			assertEquals(1, new OperationLog().merge(recoveryLog.operations()).conflicts().size());
		} finally {
			stopWorker(first);
			stopWorker(second);
			if (recoveryCopy != null) Files.deleteIfExists(recoveryCopy);
			Files.deleteIfExists(release);
			Files.deleteIfExists(firstReady);
			Files.deleteIfExists(secondReady);
			Files.deleteIfExists(firstLog);
			Files.deleteIfExists(secondLog);
			Files.deleteIfExists(tempDirectory);
			Files.deleteIfExists(Path.of(shared.getAbsolutePath() + ".lock"));
			Files.deleteIfExists(shared.toPath());
		}
	}

	@Test
	void separateJvmCrashAroundAtomicReplaceLeavesReloadableArchive() throws Exception {
		for (String crashPoint : List.of("before-replace", "after-replace")) {
			Project initial = projectForRoundTrip();
			assignPositiveUniqueIds(initial);
			long taskId = firstTask(initial).getUniqueId();
			String originalName = firstTask(initial).getName();
			File shared = File.createTempFile("mpo-process-crash-" + crashPoint, ".mpo");
			MpoFileImporter seed = new MpoFileImporter();
			seed.setFileName(shared.getAbsolutePath());
			seed.setProject(initial);
			seed.exportFile();
			byte[] originalArchive = Files.readAllBytes(shared.toPath());

			Path tempDirectory = Files.createTempDirectory("mpo-process-crash-" + crashPoint + "-" + System.nanoTime());
			Path release = tempDirectory.resolve("release.flag");
			Path ready = tempDirectory.resolve("worker-ready.flag");
			Path log = tempDirectory.resolve("worker.log");
			Process worker = null;
			try {
				worker = startMpoSaveWorker(shared, taskId, "name", "Crash edit " + crashPoint, ready, release, log,
						crashPoint);
				awaitWorkerReady(worker, ready, log);
				Files.createFile(release);
				assertTrue(worker.waitFor(30, java.util.concurrent.TimeUnit.SECONDS),
						"crash-injected worker did not terminate: " + readWorkerLog(log));
				assertEquals(crashPoint.equals("before-replace") ? 71 : 72, worker.exitValue());
				Project reopened = load(shared);
				assertEquals(crashPoint.equals("before-replace") ? originalName : "Crash edit " + crashPoint,
						firstTask(reopened).getName(), "destination must be a complete old or new archive");
				if (crashPoint.equals("before-replace")) {
					org.junit.jupiter.api.Assertions.assertArrayEquals(originalArchive, Files.readAllBytes(shared.toPath()),
							"crash before replacement must preserve the original archive byte-for-byte");
				} else {
					OperationLog.DocumentLog logAfterCrash = new OperationLog().readJsonl(
							readEntries(Files.readAllBytes(shared.toPath())).get(MpoFileImporter.OPERATIONS_ENTRY));
					String updateId = logAfterCrash.operations().stream()
							.filter(operation -> operation.kind().equals("task.update"))
							.map(OperationLog.Operation::id).findFirst()
							.orElseThrow(() -> new AssertionError("atomic replacement archive is missing its task.update"));
					assertTrue(logAfterCrash.appliedOperationIds().contains(updateId),
							"the post-crash snapshot must mark its task.update as applied");
				}
			} finally {
				stopWorker(worker);
				Files.deleteIfExists(ready);
				Files.deleteIfExists(release);
				Files.deleteIfExists(log);
				Files.deleteIfExists(tempDirectory);
				Path targetPath = shared.toPath().toAbsolutePath();
				try (java.util.stream.Stream<Path> siblings = Files.list(targetPath.getParent())) {
					for (Path sibling : siblings.filter(path -> path.getFileName().toString().startsWith(shared.getName() + ".")
							&& path.getFileName().toString().endsWith(".tmp")).toList())
						Files.deleteIfExists(sibling);
				}
				Files.deleteIfExists(Path.of(shared.getAbsolutePath() + ".lock"));
				Files.deleteIfExists(shared.toPath());
			}
		}
	}

	private static byte[] saveMpo(Project project) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, output);
		return output.toByteArray();
	}

	private static long mpoCalendarTimestamp(int year, int month, int dayOfMonth, int hourOfDay) {
		java.util.Calendar calendar = com.microproject.util.DateTime.calendarInstance(year, month, dayOfMonth);
		calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
		return calendar.getTimeInMillis();
	}

	private static void assertMalformedTaskCalendarInheritanceRejected(Map<String, byte[]> saved,
		com.fasterxml.jackson.databind.ObjectMapper mapper, String calendarName) throws Exception {
		for (String invalidMarker : List.of("\"true\"", "1", "null", "false")) {
			Map<String, byte[]> malformed = new LinkedHashMap<>(saved);
			com.fasterxml.jackson.databind.node.ObjectNode root = (com.fasterxml.jackson.databind.node.ObjectNode)
				mapper.readTree(malformed.get(MpoFileImporter.TASK_SCHEDULES_ENTRY));
			com.fasterxml.jackson.databind.node.ObjectNode taskCalendar = findScheduleCalendar(root, calendarName);
			taskCalendar.set("baseProjectCalendar", mapper.readTree(invalidMarker));
			malformed.put(MpoFileImporter.TASK_SCHEDULES_ENTRY, mapper.writeValueAsBytes(root));
			updateEntryChecksum(malformed, MpoFileImporter.TASK_SCHEDULES_ENTRY);
			assertThrows(IOException.class, () -> loadFromBytes(zip(malformed).toByteArray()),
				"invalid task calendar project-base marker must be rejected: " + invalidMarker);
		}

		Map<String, byte[]> malformed = new LinkedHashMap<>(saved);
		com.fasterxml.jackson.databind.node.ObjectNode root = (com.fasterxml.jackson.databind.node.ObjectNode)
			mapper.readTree(malformed.get(MpoFileImporter.TASK_SCHEDULES_ENTRY));
		com.fasterxml.jackson.databind.node.ObjectNode taskCalendar = findScheduleCalendar(root, calendarName);
		taskCalendar.set("baseCalendar", root.path("projectCalendar").deepCopy());
		malformed.put(MpoFileImporter.TASK_SCHEDULES_ENTRY, mapper.writeValueAsBytes(root));
		updateEntryChecksum(malformed, MpoFileImporter.TASK_SCHEDULES_ENTRY);
		assertThrows(IOException.class, () -> loadFromBytes(zip(malformed).toByteArray()),
			"task calendar must not declare both project and nested base calendars");

		Map<String, byte[]> conflictingIdentity = new LinkedHashMap<>(saved);
		com.fasterxml.jackson.databind.node.ObjectNode conflictRoot = (com.fasterxml.jackson.databind.node.ObjectNode)
			mapper.readTree(conflictingIdentity.get(MpoFileImporter.TASK_SCHEDULES_ENTRY));
		com.fasterxml.jackson.databind.node.ObjectNode conflictCalendar = findScheduleCalendar(conflictRoot, calendarName);
		conflictCalendar.set("baseCalendar", conflictCalendar.deepCopy());
		conflictingIdentity.put(MpoFileImporter.TASK_SCHEDULES_ENTRY, mapper.writeValueAsBytes(conflictRoot));
		updateEntryChecksum(conflictingIdentity, MpoFileImporter.TASK_SCHEDULES_ENTRY);
		assertThrows(IOException.class, () -> loadFromBytes(zip(conflictingIdentity).toByteArray()),
			"a nested calendar must not redefine an ancestor uniqueId");
	}

	private static com.fasterxml.jackson.databind.node.ObjectNode findScheduleCalendar(
		com.fasterxml.jackson.databind.JsonNode root, String name) {
		for (com.fasterxml.jackson.databind.JsonNode taskNode : root.path("tasks")) {
			if (name.equals(taskNode.path("calendar").path("name").asText()))
				return (com.fasterxml.jackson.databind.node.ObjectNode) taskNode.path("calendar");
		}
		throw new AssertionError("No task schedule calendar named " + name);
	}

	private static void changeDependencyLag(Project project, long predecessorId, long successorId, long lag)
			throws Exception {
		Task predecessor = project.findByUniqueId(predecessorId);
		Task successor = project.findByUniqueId(successorId);
		for (java.util.Iterator<?> links = predecessor.getSuccessorList().iterator(); links.hasNext();) {
			Dependency dependency = (Dependency) links.next();
			if (dependency.getSuccessor() == successor) {
				DependencyService.getInstance().setFields(dependency, lag, dependency.getDependencyKind(), null);
				return;
			}
		}
		throw new AssertionError("Expected dependency between tasks " + predecessorId + " and " + successorId);
	}

	private static long dependencyLag(Project project, long predecessorId, long successorId) {
		Task predecessor = project.findByUniqueId(predecessorId);
		Task successor = project.findByUniqueId(successorId);
		for (java.util.Iterator<?> links = predecessor.getSuccessorList().iterator(); links.hasNext();) {
			Dependency dependency = (Dependency) links.next();
			if (dependency.getSuccessor() == successor) return dependency.getLag();
		}
		throw new AssertionError("Expected dependency between tasks " + predecessorId + " and " + successorId);
	}

	private static double assignmentUnits(Project project, long taskId, String resourceName) {
		NormalTask task = (NormalTask) project.findByUniqueId(taskId);
		Resource resource = project.getResourcePool().getResourceList().stream()
				.filter(candidate -> candidate.getName().equals(resourceName)).findFirst()
				.orElseThrow(() -> new AssertionError("Expected resource " + resourceName));
		for (java.util.Iterator<?> assignments = task.getAssignments().iterator(); assignments.hasNext();) {
			Assignment assignment = (Assignment) assignments.next();
			if (assignment.getResource() == resource) return assignment.getUnits();
		}
		throw new AssertionError("Expected assignment for task " + taskId + " and resource " + resourceName);
	}

	@Test
	void checkedInEnglishAndJapaneseCcpmSamplesLoadForVisualization() throws Exception {
		CriticalChainService service = new CriticalChainService();
		for (String name : new String[] { "CCPM path comparison English.mpo", "CCPM path comparison 日本語.mpo" }) {
			Project loaded = load(findSample(name));
			CriticalChainService.Settings settings = service.findSettings(loaded);
			org.junit.jupiter.api.Assertions.assertNotNull(settings, name);
			org.junit.jupiter.api.Assertions.assertTrue(settings.isEnabled(), name);
			// The comparison samples are a clean pre-CCPM plan. Applying CCPM is
			// part of the walkthrough, so no stale baseline may be embedded.
			org.junit.jupiter.api.Assertions.assertNull(service.findBaseline(loaded), name);
			List<Resource> resources = new ArrayList<>(loaded.getResourcePool().getResourceList());
			CriticalChainService.Analysis analysis = service.preview(loaded, resources, settings);
			org.junit.jupiter.api.Assertions.assertFalse(analysis.criticalTaskIds().isEmpty(), name);
			org.junit.jupiter.api.Assertions.assertFalse(analysis.graphEdges().isEmpty(), name);
			org.junit.jupiter.api.Assertions.assertEquals(0D, loaded.getPercentComplete(), 0.00001D, name);
			org.junit.jupiter.api.Assertions.assertTrue(analysis.criticalTaskIds().size() < taskCount(loaded), name);
		}
	}

	@Test
	void checkedInDataDescriptorMpoLoadsThroughTheFileImportPath() throws Exception {
		Project loaded = load(findSample("CCPM sample English.mpo"));
		org.junit.jupiter.api.Assertions.assertTrue(taskCount(loaded) > 0);
		org.junit.jupiter.api.Assertions.assertTrue(new CriticalChainService().findSettings(loaded).isEnabled());
	}

	@Test
	void checkedInTwentyTaskJapaneseCcpmSampleLoadsForVisualization() throws Exception {
		Project loaded = load(findSample("CCPM 標準システム導入 20タスク.mpo"));
		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.findSettings(loaded);
		org.junit.jupiter.api.Assertions.assertNotNull(settings);
		org.junit.jupiter.api.Assertions.assertTrue(settings.isEnabled());
		org.junit.jupiter.api.Assertions.assertNull(service.findBaseline(loaded));
		org.junit.jupiter.api.Assertions.assertEquals(20, taskCount(loaded));
		org.junit.jupiter.api.Assertions.assertEquals(0D, loaded.getPercentComplete(), 0.00001D,
			"the walkthrough must begin at the 0% / 0% fever-chart checkpoint");
		org.junit.jupiter.api.Assertions.assertEquals(0D, findByName(loaded, "要件定義").getPercentComplete(), 0.00001D);
		org.junit.jupiter.api.Assertions.assertEquals(0D, findByName(loaded, "基幹機能の実装").getPercentComplete(), 0.00001D);
		org.junit.jupiter.api.Assertions.assertEquals(0D, findByName(loaded, "結合テスト").getPercentComplete(), 0.00001D);
		for (java.util.Iterator<?> tasks = loaded.getTaskOutlineIterator(); tasks.hasNext();) {
			com.microproject.pm.task.Task task = (com.microproject.pm.task.Task) tasks.next();
			org.junit.jupiter.api.Assertions.assertFalse(task.isManuallyScheduled(), task.getName());
			org.junit.jupiter.api.Assertions.assertFalse(ScheduleDiagnosticsService.hasDependencyConflict(task), task.getName());
		}
		CriticalChainService.Analysis analysis = service.preview(loaded,
			new ArrayList<>(loaded.getResourcePool().getResourceList()), settings);
		org.junit.jupiter.api.Assertions.assertFalse(analysis.criticalTaskIds().isEmpty());
		org.junit.jupiter.api.Assertions.assertFalse(analysis.graphEdges().isEmpty());
	}

	@Test
	void checkedInCcpmPathSamplesAreAutomaticallyScheduledAndHonorPredecessors() throws Exception {
		for (String name : new String[] { "CCPM path comparison English.mpo", "CCPM path comparison 日本語.mpo" }) {
			Project loaded = load(findSample(name));
			for (java.util.Iterator<?> tasks = loaded.getTaskOutlineIterator(); tasks.hasNext();) {
				com.microproject.pm.task.Task task = (com.microproject.pm.task.Task) tasks.next();
				org.junit.jupiter.api.Assertions.assertFalse(task.isManuallyScheduled(), name + ": " + task.getName());
				org.junit.jupiter.api.Assertions.assertFalse(ScheduleDiagnosticsService.hasDependencyConflict(task),
					name + ": " + task.getName());
			}
		}
	}

	@Test
	void japaneseCcpmSampleStartsWithoutImportedCompletion() throws Exception {
		Project loaded = load(findSample("CCPM path comparison 日本語.mpo"));
		com.microproject.pm.task.Task completedTask = findByName(loaded, "操作手順書");
		org.junit.jupiter.api.Assertions.assertNotNull(completedTask);
		org.junit.jupiter.api.Assertions.assertEquals(0D, completedTask.getPercentComplete(), 0.00001D);
		org.junit.jupiter.api.Assertions.assertEquals(0D, ((NormalTask) completedTask).getPercentWorkComplete(), 0.00001D);
	}

	@Test
	void historySampleRestoresFourCcpmObservations() throws Exception {
		Project loaded = load(findSample("CCPM 標準システム導入 20タスク（履歴付き）.mpo"));
		CriticalChainBufferHistory history = loaded.findTransientDocumentState(CriticalChainBufferHistory.class);
		org.junit.jupiter.api.Assertions.assertNotNull(history);
		org.junit.jupiter.api.Assertions.assertEquals(4, history.points().size());
		org.junit.jupiter.api.Assertions.assertEquals(50D, history.points().get(2).progressPercent(), 0.00001D);
		org.junit.jupiter.api.Assertions.assertEquals(55D, history.points().get(2).consumptionPercent(), 0.00001D);
	}

	@Test
	void retractedCcpmObservationSurvivesMpoSaveAndReloadAsAuditOnly() throws Exception {
		Project original = projectForRoundTrip();
		CriticalChainBufferHistory history = original.getOrCreateTransientDocumentState(
			CriticalChainBufferHistory.class, CriticalChainBufferHistory::new);
		CriticalChainBufferHistory.Point point = new CriticalChainBufferHistory.Point(
			java.time.Instant.parse("2026-09-11T00:00:00Z"), "planner", "Planner", 40D, 70D, "RED", "baseline-1");
		history.add(point);
		assertTrue(new com.microproject.pm.ccpm.CriticalChainBufferHistoryService()
			.retract(original, point.observationId(), "Accidental status refresh", "planner", "Planner").changed());

		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, archive));
		Project loaded = loadFromBytes(archive.toByteArray());
		CriticalChainBufferHistory restored = loaded.findTransientDocumentState(CriticalChainBufferHistory.class);

		org.junit.jupiter.api.Assertions.assertNotNull(restored);
		assertTrue(restored.points().isEmpty(), "retracted point must not return to the visible chart after reload");
		assertEquals(1, restored.retractions().size());
		assertEquals(point.observationId(), restored.retractions().getFirst().observationId());
		assertEquals("Accidental status refresh", restored.retractions().getFirst().reason());
	}

	private static Project loadFromBytes(byte[] mpo) throws Exception {
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		return reader.loadProject(new ByteArrayInputStream(mpo));
	}

	private static DefaultSubProj findSubproject(Project project) {
		for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			Object task = tasks.next();
			if (task instanceof DefaultSubProj reference) return reference;
		}
		throw new AssertionError("Expected a subproject reference");
	}

	private static DefaultSubProj findSubprojectForId(Project project, long projectId) {
		java.util.List<Long> availableIds = new java.util.ArrayList<>();
		for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			Object task = tasks.next();
			if (task instanceof DefaultSubProj reference) {
				availableIds.add(reference.getSubprojectUniqueId());
				if (reference.getSubprojectUniqueId() == projectId)
					return reference;
			}
		}
		throw new AssertionError("Expected extracted subproject " + projectId + "; found " + availableIds);
	}

	private static void addEmbeddedReference(Project master, Project child, File childFile) {
		DefaultSubProj reference = new DefaultSubProj(master, child.getUniqueId());
		reference.setSubprojectFile(childFile.getAbsolutePath());
		master.connectTask(reference);
		master.addToDefaultOutline(null, NodeFactory.getInstance().createNode(reference));
	}

	private static Project loadPod(File pod) throws Exception {
		LocalFileImporter reader = new LocalFileImporter();
		reader.setFileName(pod.getAbsolutePath());
		reader.setProjectFactory(ProjectFactory.getInstance());
		reader.importFile();
		org.junit.jupiter.api.Assertions.assertNotNull(reader.getProject());
		return reader.getProject();
	}

	private static File findSample(String name) {
		for (String prefix : new String[] { "samples/", "../samples/", "../../samples/" }) {
			File sample = new File(prefix + name);
			if (sample.isFile()) return sample;
		}
		throw new AssertionError("Missing POD sample: " + name);
	}

	private static com.microproject.pm.task.Task firstTask(Project project) {
		java.util.Iterator<?> tasks = project.getTaskOutlineIterator();
		if (!tasks.hasNext()) throw new AssertionError("Expected a task");
		return (com.microproject.pm.task.Task) tasks.next();
	}

	private static com.microproject.pm.task.Task findByName(Project project, String name) {
		for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			com.microproject.pm.task.Task task = (com.microproject.pm.task.Task) tasks.next();
			if (java.util.Objects.equals(name, task.getName())) return task;
		}
		throw new AssertionError("Expected task: " + name);
	}

	private static void write(ZipOutputStream zip, String name, byte[] content) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
		zip.write(content);
		zip.closeEntry();
	}

	private static Map<String, byte[]> readEntries(byte[] archive) throws IOException {
		Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive), StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				ByteArrayOutputStream content = new ByteArrayOutputStream();
				zip.transferTo(content);
				entries.put(entry.getName(), content.toByteArray());
			}
		}
		return entries;
	}

	private static ByteArrayOutputStream zip(Map<String, byte[]> entries) throws IOException {
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(archive, StandardCharsets.UTF_8)) {
			for (Map.Entry<String, byte[]> entry : entries.entrySet()) write(zip, entry.getKey(), entry.getValue());
		}
		return archive;
	}

	private static void updateEntryChecksum(Map<String, byte[]> entries, String path) {
		String manifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		String marker = "path=\"" + path + "\" sha256=\"";
		int valueStart = manifest.indexOf(marker);
		if (valueStart < 0) throw new AssertionError("Missing manifest checksum for " + path);
		valueStart += marker.length();
		int valueEnd = manifest.indexOf('"', valueStart);
		manifest = manifest.substring(0, valueStart) + sha256(entries.get(path)) + manifest.substring(valueEnd);
		entries.put(MpoFileImporter.MANIFEST_ENTRY, manifest.getBytes(StandardCharsets.UTF_8));
	}

	private static String sha256(File file) throws IOException {
		try {
			byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
					.digest(java.nio.file.Files.readAllBytes(file.toPath()));
			return java.util.HexFormat.of().formatHex(digest);
		} catch (java.security.NoSuchAlgorithmException impossible) {
			throw new AssertionError(impossible);
		}
	}

	private static String sha256(byte[] bytes) {
		try {
			return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (java.security.NoSuchAlgorithmException impossible) {
			throw new AssertionError(impossible);
		}
	}

	private static byte[] legacyAssignmentCostXml(byte[] currentXml) throws IOException {
		String xml = new String(currentXml, StandardCharsets.UTF_8);
		java.util.regex.Pattern row = java.util.regex.Pattern.compile(
			"(<TimephasedData(?:\\s[^>]*)?>[\\s\\S]*?<Type>)(\\d+)(</Type>[\\s\\S]*?<Value>)([^<]+)(</Value>[\\s\\S]*?</TimephasedData>)");
		java.util.regex.Matcher matcher = row.matcher(xml);
		StringBuffer legacy = new StringBuffer(xml.length());
		while (matcher.find()) {
			int type = Integer.parseInt(matcher.group(2));
			boolean cost = false;
			for (int slot = 0; slot <= 10; slot++)
				if (type == com.microproject.core.pm.exchange.converters.mpx.MpxImportState
						.assignmentBaselineCostType(slot)) { cost = true; break; }
			if (!cost) continue;
			String raw = new java.math.BigDecimal(matcher.group(4)).movePointLeft(2).stripTrailingZeros().toPlainString();
			matcher.appendReplacement(legacy, java.util.regex.Matcher.quoteReplacement(matcher.group(1)
				+ matcher.group(2) + matcher.group(3) + raw + matcher.group(5)));
		}
		matcher.appendTail(legacy);
		return legacy.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static void updateManifestChecksum(Map<String, byte[]> entries, String path, byte[] content) {
		String manifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		String pattern = "(path=\\\"" + java.util.regex.Pattern.quote(path) + "\\\" sha256=\\\")[^\\\"]*(\\\")";
		manifest = manifest.replaceFirst(pattern, "$1" + sha256(content) + "$2");
		entries.put(MpoFileImporter.MANIFEST_ENTRY, manifest.getBytes(StandardCharsets.UTF_8));
	}

	private static String manifestDocumentId(Map<String, byte[]> entries) {
		return new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8)
				.replaceFirst("(?s).*documentId=\\\"([0-9a-f-]{36})\\\".*", "$1");
	}
}
