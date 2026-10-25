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
package com.microproject.server.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.microproject.core.pm.exchange.converters.type.DateUTCConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxImportState;
import com.microproject.exchange.MicrosoftImporter;
import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.costing.Accrual;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;
import com.microproject.pm.scheduling.SchedulingType;
import com.microproject.session.LocalSession;
import com.microproject.session.SaveOptions;
import com.microproject.session.SessionFactory;
import com.microproject.undo.DataFactoryUndoController;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.TaskMode;

class MpxExportTrackingTest {
	@Test
	void standaloneSaveJobIsCreatedWhenTargetIsNotRegisteredInPortfolio() {
		ProjectFactory factory = ProjectFactory.createInstance();
		Project unrelated = createProject();
		unrelated.setGroupDirty(true);
		factory.addProject(unrelated, false, false);

		Project standalone = createProject();
		standalone.setFileName("standalone.mpo");
		standalone.setGroupDirty(true);
		SaveOptions options = new SaveOptions();
		options.setLocal(true);
		options.setFileName("standalone.mpo");
		options.setImporter(LocalSession.MPO_PROJECT_IMPORTER);
		SessionFactory sessionFactory = SessionFactory.getInstance();
		LocalSession session = sessionFactory.getLocalSession();
		JobQueue previousFactoryQueue = sessionFactory.getJobQueue();
		JobQueue previousSessionQueue = session.getJobQueue();
		JobQueue testQueue = new JobQueue("standalone-save", false);
		sessionFactory.setJobQueue(testQueue);
		session.setJobQueue(testQueue);
		try {
			assertNotNull(factory.getSaveProjectJob(standalone, options),
					"a standalone project without a portfolio node must still produce a save job");
		} finally {
			session.setJobQueue(previousSessionQueue);
			sessionFactory.setJobQueue(previousFactoryQueue);
		}
	}

	@Test
	void taskBaselineSlotsImportAndRoundTripWithoutChangingCurrentSchedule() throws Exception {
		ProjectFile source = new ProjectFile();
		source.addDefaultBaseCalendar();
		source.getProjectProperties().setMinutesPerDay(Integer.valueOf(480));
		java.util.Date currentStart = java.util.Date.from(java.time.Instant.parse("2026-10-12T08:00:00Z"));
		java.util.Date currentFinish = java.util.Date.from(java.time.Instant.parse("2026-10-13T17:00:00Z"));
		net.sf.mpxj.Task task = source.addTask();
		task.setName("Baseline contract");
		task.setUniqueID(1);
		task.setStart(currentStart);
		task.setFinish(currentFinish);
		task.setDuration(net.sf.mpxj.Duration.getInstance(2, net.sf.mpxj.TimeUnit.DAYS));
		java.util.Date baselineZeroStart = mspdiDateAtModelTime("2026-10-05T08:00:00Z");
		java.util.Date baselineZeroFinish = mspdiDateAtModelTime("2026-10-06T17:00:00Z");
		java.util.Date baselineOneStart = mspdiDateAtModelTime("2026-10-07T08:00:00Z");
		java.util.Date milestoneBaseline = mspdiDateAtModelTime("2026-10-08T08:00:00Z");
		java.util.Date baselineTenStart = mspdiDateAtModelTime("2026-10-01T08:00:00Z");
		java.util.Date baselineTenFinish = mspdiDateAtModelTime("2026-10-02T17:00:00Z");
		task.setBaselineStart(baselineZeroStart);
		task.setBaselineFinish(baselineZeroFinish);
		task.setBaselineDuration(net.sf.mpxj.Duration.getInstance(2, net.sf.mpxj.TimeUnit.DAYS));
		task.setBaselineFixedCost(Double.valueOf(160.0D));
		task.setFixedCostAccrual(net.sf.mpxj.AccrueType.PRORATED);
		task.setBaselineStart(1, baselineOneStart); // partial baseline: finish is absent
		task.setBaselineStart(2, milestoneBaseline);
		task.setBaselineFinish(2, milestoneBaseline);
		task.setBaselineDuration(2, net.sf.mpxj.Duration.getInstance(0, net.sf.mpxj.TimeUnit.DAYS));
		task.setBaselineStart(10, baselineTenStart);
		task.setBaselineFinish(10, baselineTenFinish);
		task.setBaselineDuration(10, net.sf.mpxj.Duration.getInstance(2, net.sf.mpxj.TimeUnit.DAYS));
		ByteArrayOutputStream sourceXml = new ByteArrayOutputStream();
		new net.sf.mpxj.mspdi.MSPDIWriter().write(source, sourceXml);
		String sourceMspdi = sourceXml.toString(java.nio.charset.StandardCharsets.UTF_8);
		// MPXJ's writer omits Baseline.FixedCost, so include the published MSPDI field
		// directly in this interoperability fixture instead of weakening the expected value.
		assertTrue(sourceMspdi.contains("<Baseline>"), "the fixture must contain a baseline element");
		sourceMspdi = sourceMspdi.replaceFirst("</Baseline>", "<FixedCost>160</FixedCost></Baseline>");

		Project imported = new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(sourceMspdi.getBytes(java.nio.charset.StandardCharsets.UTF_8)), "xml",
			(progress, label) -> {});
		NormalTask importedTask = taskNamed(imported, "Baseline contract");
		assertEquals(com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(currentStart),
			importedTask.getCurrentSchedule().getStart(), "baseline conversion must leave the current schedule intact");
		assertEquals(com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(baselineZeroStart),
			importedTask.getBaselineStart());
		assertEquals(com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(baselineZeroFinish),
			importedTask.getBaselineFinish());
		assertBaselineDuration(importedTask.getBaselineDuration(0), 2.0d,
			com.microproject.datatype.TimeUnit.DAYS);
		long baselineStart = com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(baselineZeroStart);
		com.microproject.pm.task.TaskSnapshot baselineZero = (com.microproject.pm.task.TaskSnapshot)
			importedTask.getSnapshot(Integer.valueOf(0));
		assertEquals(1.6D, baselineZero.getFixedCost(), 0.00001D);
		assertNotNull(baselineZero.getFixedCostCalendar());
		assertEquals(com.microproject.pm.costing.Accrual.Kind.PRORATED.code(), baselineZero.getFixedCostAccrual());
		assertTrue(baselineZero.getFixedCostCalendar().compare(baselineStart + java.util.concurrent.TimeUnit.HOURS.toMillis(13),
			baselineStart, false) > 0L, "the captured baseline calendar must count the partial interval as work");
		assertEquals(1.6D, importedTask.getBaselineCost(0, baselineStart,
			com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(baselineZeroFinish)), 0.00001D);
		assertEquals(0.8D, importedTask.getBaselineCost(0, baselineStart,
			baselineStart + java.util.concurrent.TimeUnit.HOURS.toMillis(13)), 0.00001D,
			"an imported MSPDI fixed-cost baseline must accrue against its captured task calendar");
		assertEquals(com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(baselineOneStart),
			((com.microproject.pm.task.TaskSnapshot) importedTask.getSnapshot(Integer.valueOf(1)))
				.getCurrentSchedule().getStart());
		assertEquals(0L, ((com.microproject.pm.task.TaskSnapshot) importedTask.getSnapshot(Integer.valueOf(1)))
			.getCurrentSchedule().getFinish(), "an absent finish must stay absent");
		assertEquals(0L, ((com.microproject.pm.task.TaskSnapshot) importedTask.getSnapshot(Integer.valueOf(2)))
			.getCurrentSchedule().getRawDuration(), "zero duration is a valid milestone baseline");
		assertEquals(com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(baselineTenStart),
			((com.microproject.pm.task.TaskSnapshot) importedTask.getSnapshot(Integer.valueOf(10)))
				.getCurrentSchedule().getStart());

		ByteArrayOutputStream roundTripXml = new ByteArrayOutputStream();
		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName("baseline-round-trip.xml");
		assertTrue(exporter.saveProject(imported, roundTripXml));
		ProjectFile roundTrip = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(roundTripXml.toByteArray()));
		net.sf.mpxj.Task roundTripTask = roundTrip.getTasks().stream()
			.filter(candidate -> "Baseline contract".equals(candidate.getName())).findFirst().orElseThrow();
		assertEquals(baselineZeroStart, roundTripTask.getBaselineStart());
		assertEquals(baselineZeroFinish, roundTripTask.getBaselineFinish());
		assertEquals(2.0d, roundTripTask.getBaselineDuration().getDuration(), 0.00001d);
		assertTrue(roundTripXml.toString(java.nio.charset.StandardCharsets.UTF_8).contains("<FixedCost>160</FixedCost>"),
			"the canonical MSPDI writer must publish the baseline fixed-cost field");
		assertEquals(net.sf.mpxj.AccrueType.PRORATED, roundTripTask.getFixedCostAccrual());
		assertEquals(net.sf.mpxj.TimeUnit.DAYS, roundTripTask.getBaselineDuration().getUnits());
		assertEquals(baselineOneStart, roundTripTask.getBaselineStart(1));
		assertNull(roundTripTask.getBaselineFinish(1));
		assertEquals(milestoneBaseline, roundTripTask.getBaselineStart(2));
		assertEquals(milestoneBaseline, roundTripTask.getBaselineFinish(2));
		assertEquals(baselineTenStart, roundTripTask.getBaselineStart(10));
		assertEquals(baselineTenFinish, roundTripTask.getBaselineFinish(10));
		Project modelRoundTrip = new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(roundTripXml.toByteArray()), "xml", (progress, label) -> {});
		assertEquals(1.6D, ((com.microproject.pm.task.TaskSnapshot)
			taskNamed(modelRoundTrip, "Baseline contract").getSnapshot(Integer.valueOf(0))).getFixedCost(), 0.00001D,
			"reimport must retain the normalized baseline fixed-cost amount emitted in canonical MSPDI");
		assertEquals(2.0d, roundTripTask.getBaselineDuration(10).getDuration(), 0.00001d);

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		com.microproject.exchange.MpoFileImporter nativeWriter = new com.microproject.exchange.MpoFileImporter();
		assertTrue(nativeWriter.saveProject(imported, mpo));
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		NormalTask reopenedTask = taskNamed(reopened, "Baseline contract");
		assertEquals(importedTask.getBaselineStart(), reopenedTask.getBaselineStart());
		assertEquals(importedTask.getBaselineFinish(), reopenedTask.getBaselineFinish());
		assertEquals(((com.microproject.pm.task.TaskSnapshot) importedTask.getSnapshot(Integer.valueOf(1)))
			.getCurrentSchedule().getStart(),
			((com.microproject.pm.task.TaskSnapshot) reopenedTask.getSnapshot(Integer.valueOf(1)))
			.getCurrentSchedule().getStart());
		assertEquals(importedTask.getBaselineDuration(0), reopenedTask.getBaselineDuration(0));
		assertEquals(importedTask.getBaselineDuration(2), reopenedTask.getBaselineDuration(2));
		assertEquals(((com.microproject.pm.task.TaskSnapshot) importedTask.getSnapshot(Integer.valueOf(10)))
			.getCurrentSchedule().getFinish(),
			((com.microproject.pm.task.TaskSnapshot) reopenedTask.getSnapshot(Integer.valueOf(10)))
			.getCurrentSchedule().getFinish());
		assertEquals(importedTask.getCurrentSchedule().getStart(), reopenedTask.getCurrentSchedule().getStart());
	}

	@Test
	void assignmentBaselineScalarsRoundTripWithoutChangingCurrentSchedule() throws Exception {
		Project withBaselines = importBaselineFixture(true);
		Project withoutBaselines = importBaselineFixture(false);
		NormalTask task = taskNamed(withBaselines, "Assignment baseline contract");
		NormalTask currentOnlyTask = taskNamed(withoutBaselines, "Assignment baseline contract");
		assertEquals(currentOnlyTask.getCurrentSchedule().getStart(), task.getCurrentSchedule().getStart());
		assertEquals(currentOnlyTask.getCurrentSchedule().getFinish(), task.getCurrentSchedule().getFinish());
		assertEquals(currentOnlyTask.getCurrentSchedule().getRawDuration(), task.getCurrentSchedule().getRawDuration());

		Assignment assignment = (Assignment) task.getAssignments().iterator().next();
		Assignment currentOnlyAssignment = (Assignment) currentOnlyTask.getAssignments().iterator().next();
		assertEquals(currentOnlyAssignment.getStart(), assignment.getStart());
		assertEquals(currentOnlyAssignment.getFinish(), assignment.getFinish());
		assertEquals(currentOnlyAssignment.getWork(null), assignment.getWork(null));
		Assignment baseline = assignment.getBaselineAssignment(Integer.valueOf(0), false);
		assertNotNull(baseline);
		assertEquals(modelWallTime("2026-10-05T08:00:00"), baseline.getStart());
		assertEquals(8L * 60L * 60L * 1000L, baseline.getWork(null));
		assertNull(assignment.getBaselineAssignment(Integer.valueOf(1), false));
		Assignment baselineTen = assignment.getBaselineAssignment(Integer.valueOf(10), false);
		assertNotNull(baselineTen);
		assertEquals(Long.valueOf(modelWallTime("2026-10-07T08:00:00")), assignment.getBaselineValues(10).start());
		assertEquals(Long.valueOf(modelWallTime("2026-10-08T08:00:00")), assignment.getBaselineValues(10).finish());
		assertEquals(8L * 60L * 60L * 1000L, baselineTen.getWork(null));

		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName("assignment-baseline.xml");
		assertTrue(exporter.saveProject(withBaselines, xml));
		net.sf.mpxj.ProjectFile roundTrip = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(xml.toByteArray()));
		net.sf.mpxj.ResourceAssignment roundTripAssignment = roundTrip.getTasks().stream()
			.filter(candidate -> "Assignment baseline contract".equals(candidate.getName())).findFirst().orElseThrow()
			.getResourceAssignments().get(0);
		assertEquals(localWallDate("2026-10-05T08:00:00"),
			roundTripAssignment.getBaselineStart());
		assertEquals(8.0d, roundTripAssignment.getBaselineWork()
			.convertUnits(net.sf.mpxj.TimeUnit.HOURS, roundTrip.getProjectProperties()).getDuration(), 0.00001d);
		assertNull(roundTripAssignment.getBaselineStart(1));
		assertEquals(localWallDate("2026-10-07T08:00:00"),
			roundTripAssignment.getBaselineStart(10));
		assertEquals(localWallDate("2026-10-08T08:00:00"),
			roundTripAssignment.getBaselineFinish(10));
		assertEquals(8.0d, roundTripAssignment.getBaselineWork(10)
			.convertUnits(net.sf.mpxj.TimeUnit.HOURS, roundTrip.getProjectProperties()).getDuration(), 0.00001d);

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(withBaselines, mpo));
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		Assignment reopenedAssignment = (Assignment) taskNamed(reopened, "Assignment baseline contract")
			.getAssignments().iterator().next();
		assertEquals(baseline.getWork(null),
			reopenedAssignment.getBaselineAssignment(Integer.valueOf(0), false).getWork(null));
	}

	@Test
	void assignmentOnlyBaselineWithoutTaskBaselineSurvivesXmlAndMpoRoundTrips() throws Exception {
		Project imported = importBaselineFixture(true, false, true);
		NormalTask task = taskNamed(imported, "Assignment baseline contract");
		assertNull(task.getSnapshot(Integer.valueOf(0)));
		Assignment assignment = (Assignment) task.getAssignments().iterator().next();
		assertEquals(modelWallTime("2026-10-05T08:00:00"),
			assignment.getBaselineValues(0).start());
		assertNull(assignment.getBaselineValues(0).finish());
		assertEquals(Long.valueOf(8L * 60L * 60L * 1000L), assignment.getBaselineValues(0).work());
		assertEquals(Long.valueOf(0L), assignment.getBaselineValues(10).work());
		assertEquals(Long.valueOf(0L), assignment.getBaselineValues(9).work());

		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		assertTrue(new MicrosoftImporter().saveProject(imported, xml));
		assertAssignmentBaselineSchemaOrder(xml.toByteArray());
		net.sf.mpxj.ResourceAssignment xmlAssignment = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(xml.toByteArray())).getTasks().stream()
			.filter(candidate -> "Assignment baseline contract".equals(candidate.getName())).findFirst().orElseThrow()
			.getResourceAssignments().get(0);
		assertEquals(localWallDate("2026-10-05T08:00:00"), xmlAssignment.getBaselineStart());
		assertNull(xmlAssignment.getBaselineFinish());
		assertEquals(localWallDate("2026-10-07T08:00:00"),
			xmlAssignment.getBaselineStart(10));
		assertEquals(localWallDate("2026-10-07T08:00:00"),
			xmlAssignment.getBaselineFinish(10));
		assertEquals(0.0d, xmlAssignment.getBaselineWork(10).getDuration(), 0.00001d);
		assertEquals(8.0d, xmlAssignment.getBaselineWork()
			.getDuration(), 0.00001d);

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(imported, mpo));
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		NormalTask reopenedTask = taskNamed(reopened, "Assignment baseline contract");
		assertNull(reopenedTask.getSnapshot(Integer.valueOf(0)));
		Assignment reopenedAssignment = (Assignment) reopenedTask.getAssignments().iterator().next();
		assertEquals(assignment.getBaselineValues(0), reopenedAssignment.getBaselineValues(0));
		assertEquals(assignment.getBaselineValues(9), reopenedAssignment.getBaselineValues(9));
		assertEquals(assignment.getBaselineValues(10), reopenedAssignment.getBaselineValues(10));
	}

	private static void assertAssignmentBaselineSchemaOrder(byte[] xml) throws Exception {
		javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
		String namespace = document.getDocumentElement().getNamespaceURI();
		org.w3c.dom.NodeList assignments = document.getElementsByTagNameNS(namespace, "Assignment");
		assertTrue(assignments.getLength() > 0);
		org.w3c.dom.Element assignment = (org.w3c.dom.Element) assignments.item(0);
		java.util.List<Integer> slots = new java.util.ArrayList<>();
		boolean sawBaseline = false;
		boolean sawCustomField = false;
		for (org.w3c.dom.Node child = assignment.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (!(child instanceof org.w3c.dom.Element element)) continue;
			String name = element.getLocalName();
			if ("Baseline".equals(name)) {
				sawBaseline = true;
				org.w3c.dom.NodeList numbers = element.getElementsByTagNameNS(namespace, "Number");
				assertEquals(1, numbers.getLength());
				slots.add(Integer.valueOf(numbers.item(0).getTextContent()));
				assertFalse(sawCustomField, "MSPDI Baseline records precede assignment local custom fields");
			} else if (name != null && name.length() == 7 && name.charAt(0) == 'f') {
				sawCustomField = true;
				assertFalse(!sawBaseline, "fixture must exercise the schema-order insertion point");
			}
		}
		java.util.List<Integer> sortedSlots = new java.util.ArrayList<>(slots);
		java.util.Collections.sort(sortedSlots);
		assertEquals(sortedSlots, slots, "MSPDI assignment baseline slots are emitted in ascending order");
		assertTrue(slots.contains(Integer.valueOf(9)), "zero-only baseline must be materialized in the Baseline sequence");
	}

	@Test
	void explicitZeroAssignmentBaselineWorkIsImportedFromLiteralXml() throws Exception {
		Project imported = importBaselineFixture(true, true, true);
		Assignment assignment = (Assignment) taskNamed(imported, "Assignment baseline contract").getAssignments()
			.iterator().next();
		assertEquals(0L, assignment.getBaselineValues(10).work());
		assertEquals(assignment.getBaselineValues(10).start(), assignment.getBaselineValues(10).finish());
	}

	@Test
	void fixedAssignmentBaselineAcceptanceFixtureHasIndependentScalarExpectations() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "Assignment Baseline Scalars.xml");
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "Assignment Baseline Scalars.xml");
		Project imported = new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(Files.readAllBytes(fixture)), "xml", (progress, label) -> {});
		NormalTask task = taskNamed(imported, "Assignment baseline task");
		assertNull(task.getSnapshot(Integer.valueOf(0)));
		Assignment assignment = (Assignment) task.getAssignments().iterator().next();
		assertEquals(modelWallTime("2026-10-05T17:00:00"),
			assignment.getBaselineValues(0).start());
		assertNull(assignment.getBaselineValues(0).finish());
		assertEquals(Long.valueOf(8L * 60L * 60L * 1000L), assignment.getBaselineValues(0).work());
		assertEquals(new Assignment.BaselineValues(null, null, Long.valueOf(0L)), assignment.getBaselineValues(9));
		assertEquals(modelWallTime("2026-10-07T17:00:00"),
			assignment.getBaselineValues(10).start());
		assertEquals(modelWallTime("2026-10-08T17:00:00"),
			assignment.getBaselineValues(10).finish());
		assertEquals(Long.valueOf(4L * 60L * 60L * 1000L), assignment.getBaselineValues(10).work());
	}

	private static Project importBaselineFixture(boolean includeBaselines) throws Exception {
		return importBaselineFixture(includeBaselines, true, false);
	}

	private static java.util.Date localWallDate(String value) {
		return java.util.Date.from(java.time.LocalDateTime.parse(value)
			.atZone(java.time.ZoneId.systemDefault()).toInstant());
	}

	private static long modelWallTime(String value) {
		return java.time.LocalDateTime.parse(value).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
	}

	private static Project importBaselineFixture(boolean includeBaselines, boolean includeTaskBaseline,
			boolean zeroSlotTenWork) throws Exception {
		return importBaselineFixtureResult(includeBaselines, includeTaskBaseline, zeroSlotTenWork);
	}

	private static Project importBaselineFixtureResult(boolean includeBaselines, boolean includeTaskBaseline,
			boolean zeroSlotTenWork) throws Exception {
		net.sf.mpxj.ProjectFile source = new net.sf.mpxj.ProjectFile();
		source.addDefaultBaseCalendar();
		source.getProjectProperties().setMinutesPerDay(Integer.valueOf(480));
		java.util.Date currentStart = localWallDate("2026-10-12T08:00:00");
		java.util.Date currentFinish = localWallDate("2026-10-13T17:00:00");
		net.sf.mpxj.Task task = source.addTask();
		task.setName("Assignment baseline contract");
		task.setUniqueID(1);
		task.setStart(currentStart);
		task.setFinish(currentFinish);
		task.setDuration(net.sf.mpxj.Duration.getInstance(2, net.sf.mpxj.TimeUnit.DAYS));
		if (includeTaskBaseline) {
			task.setBaselineStart(localWallDate("2026-10-05T08:00:00"));
			task.setBaselineFinish(localWallDate("2026-10-06T08:00:00"));
		}
		net.sf.mpxj.Resource resource = source.addResource();
		resource.setName("Baseline worker");
		resource.setUniqueID(1);
		net.sf.mpxj.ResourceAssignment assignment = task.addResourceAssignment(resource);
		assignment.setUnits(100d);
		assignment.setStart(currentStart);
		assignment.setFinish(currentFinish);
		assignment.setWork(net.sf.mpxj.Duration.getInstance(16, net.sf.mpxj.TimeUnit.HOURS));
		if (includeBaselines) {
					assignment.setBaselineStart(localWallDate("2026-10-05T08:00:00"));
			assignment.setBaselineWork(net.sf.mpxj.Duration.getInstance(8, net.sf.mpxj.TimeUnit.HOURS));
			java.util.Date baselineTenStart = localWallDate("2026-10-07T08:00:00");
			java.util.Date baselineTenFinish = zeroSlotTenWork ? baselineTenStart
				: localWallDate("2026-10-08T08:00:00");
			if (includeTaskBaseline) {
				task.setBaselineStart(10, baselineTenStart);
				task.setBaselineFinish(10, baselineTenFinish);
				task.setBaselineDuration(10, net.sf.mpxj.Duration.getInstance(zeroSlotTenWork ? 0 : 1,
					net.sf.mpxj.TimeUnit.DAYS));
			}
			assignment.setBaselineStart(10, baselineTenStart);
			assignment.setBaselineFinish(10, baselineTenFinish);
			assignment.setBaselineWork(10, net.sf.mpxj.Duration.getInstance(zeroSlotTenWork ? 0 : 8,
				net.sf.mpxj.TimeUnit.HOURS));
		}
		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		new net.sf.mpxj.mspdi.MSPDIWriter().write(source, xml);
		if (zeroSlotTenWork) {
			String xmlText = xml.toString(java.nio.charset.StandardCharsets.UTF_8);
			int assignments = xmlText.indexOf("<Assignments>");
			int slotTen = xmlText.indexOf("<Number>10</Number>", assignments);
			int baselineEnd = xmlText.indexOf("</Baseline>", slotTen);
			assertTrue(assignments >= 0 && slotTen >= 0 && baselineEnd >= 0);
			xmlText = xmlText.substring(0, baselineEnd) + "<Work>PT0H0M0S</Work>" + xmlText.substring(baselineEnd);
			int assignmentEnd = xmlText.indexOf("</Assignment>", assignments);
			assertTrue(assignmentEnd >= 0);
			xmlText = xmlText.substring(0, assignmentEnd)
				+ "<Baseline><Number>9</Number><Work>PT0S</Work></Baseline>" + xmlText.substring(assignmentEnd);
			xml = new ByteArrayOutputStream();
			xml.write(xmlText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		}
		com.microproject.core.pm.exchange.MspImporter importer = new com.microproject.core.pm.exchange.MspImporter();
		Project imported = importer.importProject(new ByteArrayInputStream(xml.toByteArray()), "xml",
			(progress, label) -> {});
		return imported;
	}

	private static void assertBaselineDuration(long actual, double expectedValue, int expectedUnit) {
		assertEquals(expectedValue, com.microproject.datatype.Duration.getValue(actual), 0.00001d);
		assertEquals(expectedUnit, com.microproject.datatype.Duration.getEffectiveType(actual));
	}

	@Test
	void elapsedBaselineDurationUnitSurvivesMpxjModelConversion() {
		long imported = com.microproject.core.pm.exchange.converters.mpx.MpxUtils.toProjectDuration(
			net.sf.mpxj.Duration.getInstance(2, net.sf.mpxj.TimeUnit.ELAPSED_DAYS));
		assertBaselineDuration(imported, 2.0d, com.microproject.datatype.TimeUnit.ELAPSED_DAYS);
		net.sf.mpxj.Duration exported = MPXConverter.toMPXDuration(imported);
		assertEquals(2.0d, exported.getDuration(), 0.00001d);
		assertEquals(net.sf.mpxj.TimeUnit.ELAPSED_DAYS, exported.getUnits());
	}

	@Test
	void literalMspdiElapsedBaselineDurationUsesTaskDurationFormat() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "Assignment Baseline Scalars.xml");
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "Assignment Baseline Scalars.xml");
		String sourceXml = Files.readString(fixture);
		int taskStart = sourceXml.indexOf("<Task>");
		int taskEnd = sourceXml.indexOf("</Task>", taskStart);
		String taskXml = sourceXml.substring(taskStart, taskEnd);
		taskXml = taskXml.replace("<DurationFormat>7</DurationFormat>", "<DurationFormat>8</DurationFormat>")
			+ "<Baseline><Number>3</Number><Start>2026-10-05T08:00:00</Start>"
			+ "<Finish>2026-10-07T08:00:00</Finish><Duration>PT48H0M0S</Duration>"
			+ "<DurationFormat>8</DurationFormat></Baseline>";
		sourceXml = sourceXml.substring(0, taskStart) + taskXml + sourceXml.substring(taskEnd);

		Project imported = importMspdi(sourceXml);
		NormalTask task = taskNamed(imported, "Assignment baseline task");
		assertBaselineDuration(task.getBaselineDuration(3), 2.0d,
			com.microproject.datatype.TimeUnit.ELAPSED_DAYS);
		assertEquals(modelWallTime("2026-10-12T08:00:00"), task.getCurrentSchedule().getStart(),
			"an elapsed baseline must not modify the current schedule");
		assertEquals(modelWallTime("2026-10-12T17:00:00"), task.getCurrentSchedule().getFinish());

		ByteArrayOutputStream roundTripXml = new ByteArrayOutputStream();
		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName("elapsed-baseline.xml");
		assertTrue(exporter.saveProject(imported, roundTripXml));
		String exportedXml = roundTripXml.toString(java.nio.charset.StandardCharsets.UTF_8);
		int baselineNumber = exportedXml.indexOf("<Number>3</Number>");
		assertTrue(baselineNumber >= 0, exportedXml);
		int baselineEnd = exportedXml.indexOf("</Baseline>", baselineNumber);
		assertTrue(baselineEnd > baselineNumber, exportedXml);
		String exportedBaseline = exportedXml.substring(baselineNumber, baselineEnd);
		assertTrue(exportedBaseline.contains("<DurationFormat>8</DurationFormat>"), exportedBaseline);
		assertTrue(exportedBaseline.contains("<Duration>PT48H0M0S</Duration>"), exportedBaseline);
		Project reopened = importMspdi(exportedXml);
		assertBaselineDuration(taskNamed(reopened, "Assignment baseline task").getBaselineDuration(3), 2.0d,
			com.microproject.datatype.TimeUnit.ELAPSED_DAYS);
		String malformed = sourceXml.replace("<Duration>PT48H0M0S</Duration>", "<Duration>P</Duration>");
		assertThrows(IllegalArgumentException.class, () -> importMspdi(malformed));
	}

	@Test
	void mspdiStandardAssignmentWorkContoursSurviveImportAndNativeRoundTrip() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "Assignment Baseline Scalars.xml");
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "Assignment Baseline Scalars.xml");
		String flatXml = withAssignmentProgress(Files.readString(fixture), 8L * 60L * 60L * 1000L, 0L, 0);
		net.sf.mpxj.ProjectFile sourceFile = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(flatXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		net.sf.mpxj.ResourceAssignment sourceAssignment = sourceFile.getTasks().stream()
			.filter(candidate -> "Assignment baseline task".equals(candidate.getName())).findFirst().orElseThrow()
			.getResourceAssignments().get(0);
		assertEquals(0.0d, ((Number) sourceAssignment.get(net.sf.mpxj.AssignmentField.PERCENT_WORK_COMPLETE)).doubleValue(), 0.00001d);
		assertEquals(0.0d, sourceAssignment.getActualWork()
			.convertUnits(net.sf.mpxj.TimeUnit.HOURS, sourceFile.getProjectProperties()).getDuration(), 0.00001d);
		assertEquals(8.0d, sourceAssignment.getRemainingWork()
			.convertUnits(net.sf.mpxj.TimeUnit.HOURS, sourceFile.getProjectProperties()).getDuration(), 0.00001d);
		Project flat = importMspdi(flatXml);
		NormalTask flatTask = taskNamed(flat, "Assignment baseline task");
		Assignment flatAssignment = (Assignment) flatTask.getAssignments().iterator().next();
		assertEquals(com.microproject.pm.assignment.contour.ContourTypes.Kind.FLAT,
			flatAssignment.getWorkContourTypeKind());
		assertEquals(8L * 60L * 60L * 1000L, flatAssignment.getWork(null));
		assertEquals(0L, flatAssignment.getActualWork(null));
		assertEquals(0.0d, flatAssignment.getPercentComplete());
		long sourceStart = flatAssignment.getStart();
		long sourceFinish = flatAssignment.getFinish();
		long sourceDuration = flatAssignment.getDurationMillis();
		assertEquals(8L * 60L * 60L * 1000L, sourceDuration);

		double[] meanUnits = { 1.0d, 0.6d, 0.6d, 0.5d, 0.5d, 0.5d, 0.5d, 0.7d };
		for (int code = 0; code < meanUnits.length; code++) {
			long expectedWork = Math.round(sourceDuration * meanUnits[code]);
			String contourXml = withAssignmentContour(flatXml, code, expectedWork);
			Project imported = importMspdi(contourXml);
			NormalTask task = taskNamed(imported, "Assignment baseline task");
			Assignment assignment = (Assignment) task.getAssignments().iterator().next();
			assertEquals(com.microproject.pm.assignment.contour.ContourTypes.Kind.fromCode(code),
				assignment.getWorkContourTypeKind());
			assertEquals(expectedWork, assignment.getWork(null), "contour " + code);
			assertEquals(sourceStart, assignment.getStart(), "start contour " + code);
			assertEquals(sourceFinish, assignment.getFinish(), "finish contour " + code);
			assertEquals(sourceDuration, assignment.getDurationMillis(), "duration contour " + code);
			assertEquals(1.0d, assignment.getUnits(), 0.00001d, "units contour " + code);
			assertEquals(0L, assignment.getActualWork(null), "actual work contour " + code);
			assertEquals(expectedWork, assignment.getRemainingWork(), "remaining work contour " + code);
			assertEquals(0.0d, assignment.getPercentComplete(), "percent complete contour " + code);

			ByteArrayOutputStream roundTripXml = new ByteArrayOutputStream();
			MicrosoftImporter exporter = new MicrosoftImporter();
			exporter.setFileName("assignment-contour.xml");
			assertTrue(exporter.saveProject(imported, roundTripXml));
			net.sf.mpxj.ProjectFile xmlRoundTrip = new net.sf.mpxj.mspdi.MSPDIReader()
				.read(new ByteArrayInputStream(roundTripXml.toByteArray()));
			net.sf.mpxj.ResourceAssignment xmlAssignment = xmlRoundTrip.getTasks().stream()
				.filter(candidate -> "Assignment baseline task".equals(candidate.getName())).findFirst().orElseThrow()
				.getResourceAssignments().get(0);
			assertEquals(code, com.microproject.exchange.mpxj.MpxjApi.workContourId(xmlAssignment.getWorkContour()));
			assertEquals(expectedWork / (60.0d * 60.0d * 1000.0d), xmlAssignment.getWork()
				.convertUnits(net.sf.mpxj.TimeUnit.HOURS, xmlRoundTrip.getProjectProperties()).getDuration(), 0.00001d);

			ByteArrayOutputStream mpo = new ByteArrayOutputStream();
			assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(imported, mpo));
			byte[] mpoXmlBytes = nativeProjectXml(mpo.toByteArray());
			String rawMpoXml = new String(mpoXmlBytes, java.nio.charset.StandardCharsets.UTF_8);
			net.sf.mpxj.ProjectFile mpoProjectXml = new net.sf.mpxj.mspdi.MSPDIReader()
				.read(new ByteArrayInputStream(mpoXmlBytes));
			net.sf.mpxj.ResourceAssignment mpoAssignment = mpoProjectXml.getTasks().stream()
				.filter(candidate -> "Assignment baseline task".equals(candidate.getName())).findFirst().orElseThrow()
				.getResourceAssignments().get(0);
			assertEquals(expectedWork / (60.0d * 60.0d * 1000.0d), mpoAssignment.getWork()
				.convertUnits(net.sf.mpxj.TimeUnit.HOURS, mpoProjectXml.getProjectProperties()).getDuration(), 0.00001d);
			int rawTaskStart = rawMpoXml.indexOf("<Task>");
			int rawTaskEnd = rawMpoXml.indexOf("</Task>", rawTaskStart) + "</Task>".length();
			String rawTaskXml = rawMpoXml.substring(rawTaskStart, rawTaskEnd);
			assertTrue(rawTaskXml.contains("<Start>2026-10-12T08:00:00</Start>"));
			assertTrue(rawTaskXml.contains("<Finish>2026-10-12T17:00:00</Finish>"));
			Project directReload = importMspdi(rawMpoXml);
			NormalTask directTask = taskNamed(directReload, "Assignment baseline task");
			Assignment directAssignment = (Assignment) directTask.getAssignments().iterator().next();
			assertEquals(expectedWork, directAssignment.getWork(null), "direct MSPDI reload: units="
				+ directAssignment.getUnits() + ", actual=" + directAssignment.getActualWork(null)
				+ ", remaining=" + directAssignment.getRemainingWork() + ", contour="
				+ directAssignment.getWorkContourTypeKind() + ", duration=" + directAssignment.getDurationMillis()
				+ ", taskWork=" + directTask.getWork()
				+ ", taskDates=" + directTask.getStart() + "/" + directTask.getEnd()
				+ ", resource=" + directAssignment.getResource().getName());
			Project reopened = new com.microproject.exchange.MpoFileImporter()
				.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
			java.util.Collection reopenedAssignments = taskNamed(reopened, "Assignment baseline task").getAssignments();
			assertEquals(1, reopenedAssignments.size());
			Assignment reopenedAssignment = (Assignment) reopenedAssignments.iterator().next();
			assertEquals(assignment.getWorkContourTypeKind(), reopenedAssignment.getWorkContourTypeKind());
			assertEquals(expectedWork, reopenedAssignment.getWork(null));
		}
		UnsupportedOperationException unsupportedCustomContour = assertThrows(UnsupportedOperationException.class,
			() -> com.microproject.exchange.mpxj.MpxjApi.workContourId(net.sf.mpxj.WorkContour.CONTOURED));
		assertTrue(unsupportedCustomContour.getMessage().contains("timephased assignment import"));
	}

	@Test
	void standardAssignmentContoursPreserveProgressAndSourceBounds() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "Assignment Baseline Scalars.xml");
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "Assignment Baseline Scalars.xml");
		String baseXml = Files.readString(fixture);
		// Back Loaded's documented mean profile on this 8-hour assignment span is 0.6.
		long totalWork = Math.round(8L * 60L * 60L * 1000L * 0.6d);
		int[][] progress = { { 0, 0 }, { 50, 70 }, { 100, 100 } };
		for (int[] values : progress) {
			int percentWorkComplete = values[0];
			int percentDurationComplete = values[1];
			long actualWork = totalWork * percentWorkComplete / 100L;
			long remainingWork = totalWork - actualWork;
			String sourceXml = withAssignmentProgress(baseXml, totalWork, actualWork, percentWorkComplete);
			sourceXml = withAssignmentContour(sourceXml, 1, totalWork);
			net.sf.mpxj.ProjectFile parsed = new net.sf.mpxj.mspdi.MSPDIReader()
				.read(new ByteArrayInputStream(sourceXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
			net.sf.mpxj.ResourceAssignment sourceAssignment = parsed.getTasks().stream()
				.filter(candidate -> "Assignment baseline task".equals(candidate.getName())).findFirst().orElseThrow()
				.getResourceAssignments().get(0);
			assertEquals(actualWork / (60.0d * 60.0d * 1000.0d), sourceAssignment.getActualWork()
				.convertUnits(net.sf.mpxj.TimeUnit.HOURS, parsed.getProjectProperties()).getDuration(), 0.00001d);
			assertEquals(remainingWork / (60.0d * 60.0d * 1000.0d), sourceAssignment.getRemainingWork()
				.convertUnits(net.sf.mpxj.TimeUnit.HOURS, parsed.getProjectProperties()).getDuration(), 0.00001d);
			assertEquals(percentWorkComplete, sourceAssignment.getPercentageWorkComplete().intValue());

			Project imported = importMspdi(sourceXml);
			NormalTask task = taskNamed(imported, "Assignment baseline task");
			Assignment assignment = (Assignment) task.getAssignments().iterator().next();
			assertEquals(totalWork, assignment.getWork(null), "total work at " + percentWorkComplete + "% work complete");
			assertEquals(actualWork, assignment.getActualWork(null), "actual work at " + percentWorkComplete + "% work complete; stop="
					+ assignment.getStop() + ", start=" + assignment.getStart() + ", end=" + assignment.getEnd()
					+ ", duration=" + assignment.getDuration() + ", units=" + assignment.getUnits()
					+ ", work=" + assignment.getWork(null) + ", contour=" + assignment.getWorkContourType());
			assertEquals(remainingWork, assignment.getRemainingWork(), "remaining work at " + percentWorkComplete + "% work complete");
			assertEquals(percentDurationComplete / 100.0d, assignment.getPercentComplete(), 0.00001d,
				"duration percent at " + percentWorkComplete + "% work complete");
			assertEquals(percentWorkComplete / 100.0d, (double) assignment.getActualWork(null) / assignment.getWork(null),
				0.00001d, "work percent at " + percentWorkComplete + "%");
			assertEquals(1, assignment.getWorkContourType(), "Back Loaded contour at " + percentWorkComplete + "% work complete");
			assertEquals(modelWallTime("2026-10-12T08:00:00"), assignment.getStart(), "start at " + percentWorkComplete + "% work complete");
			assertEquals(modelWallTime("2026-10-12T17:00:00"), assignment.getEnd(), "finish at " + percentWorkComplete + "% work complete");

			ByteArrayOutputStream mpo = new ByteArrayOutputStream();
			assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(imported, mpo));
			Project reopened = new com.microproject.exchange.MpoFileImporter()
				.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
			Assignment reopenedAssignment = (Assignment) taskNamed(reopened, "Assignment baseline task")
				.getAssignments().iterator().next();
			assertEquals(totalWork, reopenedAssignment.getWork(null), "MPO total work at " + percentWorkComplete + "% work complete");
			assertEquals(actualWork, reopenedAssignment.getActualWork(null), "MPO actual work at " + percentWorkComplete + "% work complete");
			assertEquals(remainingWork, reopenedAssignment.getRemainingWork(), "MPO remaining work at " + percentWorkComplete + "% work complete");
			assertEquals(percentDurationComplete / 100.0d, reopenedAssignment.getPercentComplete(), 0.00001d,
				"MPO duration percent at " + percentWorkComplete + "% work complete");
			assertEquals(percentWorkComplete / 100.0d,
				(double) reopenedAssignment.getActualWork(null) / reopenedAssignment.getWork(null), 0.00001d,
				"MPO work percent at " + percentWorkComplete + "%");
			assertEquals(1, reopenedAssignment.getWorkContourType(), "MPO contour at " + percentWorkComplete + "% work complete");
			assertEquals(assignment.getStart(), reopenedAssignment.getStart(), "MPO start at " + percentWorkComplete + "% work complete");
			assertEquals(assignment.getEnd(), reopenedAssignment.getEnd(),
				"MPO finish at " + percentWorkComplete + "% work complete");
		}
	}

	@Test
	void customMspdiContourRoundTripsDailyWorkTotalsThroughMpo() throws Exception {
		String xml = twoDayCustomContourXml("",
			"<TimephasedData><Type>1</Type><UID>1</UID><Start>2026-10-12T08:00:00</Start><Finish>2026-10-12T17:00:00</Finish><Unit>2</Unit><Value>PT2H0M0S</Value></TimephasedData>"
				+ "<TimephasedData><Type>1</Type><UID>2</UID><Start>2026-10-13T08:00:00</Start><Finish>2026-10-13T17:00:00</Finish><Unit>2</Unit><Value>PT6H0M0S</Value></TimephasedData>");
		net.sf.mpxj.ProjectFile parsed = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		ResourceAssignment assignment = parsed.getResourceAssignments().get(0);
		assertEquals(8.0d, assignment.getWork().convertUnits(net.sf.mpxj.TimeUnit.HOURS,
			parsed.getProjectProperties()).getDuration(), 0.00001d);
		var timephasedWork = assignment.getTimephasedWork();
		assertEquals(2, timephasedWork.size(), timephasedWork.toString());
		double[] expectedHours = { 2.0d, 6.0d };
		String[] expectedDays = { "2026-10-12", "2026-10-13" };
		for (int index = 0; index < expectedHours.length; index++) {
			var item = timephasedWork.get(index);
			assertEquals(expectedHours[index], item.getTotalAmount().getDuration(), 0.00001d, timephasedWork.toString());
			assertEquals(expectedHours[index], item.getAmountPerDay().getDuration(), 0.00001d, timephasedWork.toString());
			assertEquals(modelWallTime(expectedDays[index] + "T08:00:00"), DateUTCConverter.toModelTime(item.getStart()));
			assertEquals(modelWallTime(expectedDays[index] + "T17:00:00"), DateUTCConverter.toModelTime(item.getFinish()));
		}
		Project imported = importMspdi(xml);
		Assignment modelAssignment = (Assignment) taskNamed(imported, "Assignment baseline task").getAssignments().iterator().next();
		assertEquals(8L * 60L * 60L * 1000L, modelAssignment.getWork(null));
		assertEquals(com.microproject.pm.assignment.contour.ContourTypes.Kind.CONTOURED.code(),
			modelAssignment.getWorkContourType());
		var buckets = modelAssignment.getWorkContour().getContourBuckets();
		assertEquals(2, buckets.length);
		assertEquals(8L * 60L * 60L * 1000L, buckets[0].getBucketDuration(modelAssignment.getDuration()));
		assertEquals(0.25d, buckets[0].getUnits(), 0.00001d);
		assertEquals(8L * 60L * 60L * 1000L, buckets[1].getBucketDuration(modelAssignment.getDuration()));
		assertEquals(0.75d, buckets[1].getUnits(), 0.00001d);

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(imported, mpo));
		byte[] contentXml = nativeProjectXml(mpo.toByteArray());
		String savedXml = new String(contentXml, java.nio.charset.StandardCharsets.UTF_8);
		assertEquals(4, savedXml.split("<TimephasedData>", -1).length - 1, savedXml);
		assertEquals(4, savedXml.split("<Unit>2</Unit>", -1).length - 1, savedXml);
		ProjectFile serialized = new net.sf.mpxj.mspdi.MSPDIReader().read(new ByteArrayInputStream(contentXml));
		var serializedWork = serialized.getResourceAssignments().get(0).getTimephasedWork();
		assertEquals(2, serializedWork.size(), serializedWork.toString());
		for (int index = 0; index < expectedHours.length; index++) {
			assertEquals(expectedHours[index], serializedWork.get(index).getTotalAmount().convertUnits(
				net.sf.mpxj.TimeUnit.HOURS, serialized.getProjectProperties()).getDuration(), 0.00001d);
			assertEquals(modelWallTime(expectedDays[index] + "T08:00:00"),
				DateUTCConverter.toModelTime(serializedWork.get(index).getStart()));
			assertEquals(modelWallTime(expectedDays[index] + "T17:00:00"),
				DateUTCConverter.toModelTime(serializedWork.get(index).getFinish()));
		}
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		Assignment reopenedAssignment = (Assignment) taskNamed(reopened, "Assignment baseline task").getAssignments().iterator().next();
		assertEquals(8L * 60L * 60L * 1000L, reopenedAssignment.getWork(null));
		assertEquals(8L * 60L * 60L * 1000L, reopenedAssignment.getWorkContour().calcTotalWork(reopenedAssignment.getDuration()));
		assertEquals(0.25d, reopenedAssignment.getWorkContour().getContourBuckets()[0].getUnits(), 0.00001d);
		assertEquals(0.75d, reopenedAssignment.getWorkContour().getContourBuckets()[1].getUnits(), 0.00001d);
	}

	@Test
	void assignmentBaselineTimephasedWorkRoundTripsThroughMpoAndMspdi() throws Exception {
		assertTrue(com.microproject.pm.assignment.TimeDistributedHelper.isCost(
			com.microproject.pm.assignment.TimeDistributedConstants.BASELINE_COST));
		int[] expectedWorkTypes = { 4, 16, 22, 28, 34, 40, 46, 52, 58, 64, 70 };
		for (int slot = 0; slot < expectedWorkTypes.length; slot++) {
			assertEquals(expectedWorkTypes[slot], MpxImportState.assignmentBaselineWorkType(slot));
			assertEquals(expectedWorkTypes[slot] + 1, MpxImportState.assignmentBaselineCostType(slot));
		}

		String xml = twoDayCustomContourXml("",
			"<TimephasedData><Type>1</Type><UID>1</UID><Start>2026-10-12T08:00:00</Start><Finish>2026-10-12T17:00:00</Finish><Unit>2</Unit><Value>PT2H0M0S</Value></TimephasedData>"
				+ "<TimephasedData><Type>1</Type><UID>2</UID><Start>2026-10-13T08:00:00</Start><Finish>2026-10-13T17:00:00</Finish><Unit>2</Unit><Value>PT6H0M0S</Value></TimephasedData>")
			.replace("<Baseline><Number>0</Number><Start>2026-10-05T17:00:00</Start><Work>PT8H0M0S</Work></Baseline>",
				"<Baseline><TimephasedData><Type>4</Type><UID>1</UID><Start>2026-10-05T08:00:00</Start>"
					+ "<Finish>2026-10-05T17:00:00</Finish><Unit>2</Unit><Value>PT2H0M0S</Value></TimephasedData>"
					+ "<TimephasedData><Type>4</Type><UID>1</UID><Start>2026-10-06T08:00:00</Start>"
					+ "<Finish>2026-10-06T17:00:00</Finish><Unit>2</Unit><Value>PT6H0M0S</Value></TimephasedData>"
					+ "<TimephasedData><Type>5</Type><UID>1</UID><Start>2026-10-05T08:00:00</Start>"
					+ "<Finish>2026-10-05T17:00:00</Finish><Unit>2</Unit><Value>0.1</Value></TimephasedData>"
					+ "<TimephasedData><Type>5</Type><UID>1</UID><Start>2026-10-06T08:00:00</Start>"
					+ "<Finish>2026-10-06T17:00:00</Finish><Unit>2</Unit><Value>0.3</Value></TimephasedData>"
					+ "<Number>0</Number><Start>2026-10-05T08:00:00</Start><Finish>2026-10-06T17:00:00</Finish>"
					+ "<Work>PT8H0M0S</Work></Baseline>")
			.replace("<Baseline><Number>9</Number>",
				"<Baseline><TimephasedData><Type>17</Type><UID>1</UID><Start>2026-10-07T08:00:00</Start>"
					+ "<Finish>2026-10-07T17:00:00</Finish><Unit>2</Unit><Value>17</Value></TimephasedData>"
					+ "<Number>1</Number><Start>2026-10-07T08:00:00</Start><Finish>2026-10-07T17:00:00</Finish></Baseline>"
					+ "<Baseline><TimephasedData><Type>22</Type><UID>1</UID><Start>2026-10-07T08:00:00</Start>"
					+ "<Finish>2026-10-07T17:00:00</Finish><Unit>2</Unit><Value>PT1H0M0S</Value></TimephasedData>"
					+ "<TimephasedData><Type>23</Type><UID>1</UID><Start>2026-10-07T08:00:00</Start>"
					+ "<Finish>2026-10-07T17:00:00</Finish><Unit>2</Unit><Value>17</Value></TimephasedData>"
					+ "<Number>2</Number><Start>2026-10-07T08:00:00</Start><Finish>2026-10-07T17:00:00</Finish>"
					+ "<Work>PT1H0M0S</Work></Baseline><Baseline><Number>9</Number>");
		assertTrue(xml.contains("<Type>4</Type><UID>1</UID><Start>2026-10-05T08:00:00</Start>"
			+ "<Finish>2026-10-05T17:00:00</Finish><Unit>2</Unit><Value>PT2H0M0S</Value>"));
		assertTrue(xml.contains("<Type>4</Type><UID>1</UID><Start>2026-10-06T08:00:00</Start>"
			+ "<Finish>2026-10-06T17:00:00</Finish><Unit>2</Unit><Value>PT6H0M0S</Value>"));

		Project project = importMspdi(xml);
		Assignment assignment = (Assignment) taskNamed(project, "Assignment baseline task").getAssignments().iterator().next();
		Assignment baseline = assignment.getBaselineAssignment(Integer.valueOf(0), false);
		assertNotNull(baseline, "An assignment baseline with timephased work creates its baseline snapshot");
		assertTrue(baseline.getWorkContour().isPersonal(), baseline.getWorkContour().toString(baseline.getDuration()));
		assertEquals(8L * 60L * 60L * 1000L, baseline.getWork(null));
		assertEquals(2L * 60L * 60L * 1000L,
			baseline.work(modelWallTime("2026-10-05T08:00:00"), modelWallTime("2026-10-05T17:00:00")));
		assertEquals(6L * 60L * 60L * 1000L,
			baseline.work(modelWallTime("2026-10-06T08:00:00"), modelWallTime("2026-10-06T17:00:00")));
		assertEquals(0.004D, baseline.calcAll(Assignment.COST), 0.0000001D,
			"The snapshot cost contour should contain both imported intervals");
		assertEquals(0.001D, baseline.cost(modelWallTime("2026-10-05T08:00:00"),
			modelWallTime("2026-10-05T17:00:00")), 0.0000001D,
			"The baseline snapshot query should return the first cost interval");
		assertEquals(0.001D, assignment.baselineCost(modelWallTime("2026-10-05T08:00:00"),
			modelWallTime("2026-10-05T17:00:00")), 0.0000001D);
		assertEquals(0.003D, assignment.baselineCost(modelWallTime("2026-10-06T08:00:00"),
			modelWallTime("2026-10-06T17:00:00")), 0.0000001D);
		Assignment secondBaseline = assignment.getBaselineAssignment(Integer.valueOf(2), false);
		assertNotNull(secondBaseline);
		assertEquals(1L * 60L * 60L * 1000L, secondBaseline.work(
			modelWallTime("2026-10-07T08:00:00"), modelWallTime("2026-10-07T17:00:00")));
		assertEquals(0.17D, secondBaseline.cost(modelWallTime("2026-10-07T08:00:00"),
			modelWallTime("2026-10-07T17:00:00")), 0.00001D);
		Assignment costOnlyBaseline = assignment.getBaselineAssignment(Integer.valueOf(1), false);
		assertNotNull(costOnlyBaseline);
		assertNull(assignment.getBaselineValues(1).work(), "Cost-only baselines must preserve missing Work");
		assertEquals(modelWallTime("2026-10-07T08:00:00"), costOnlyBaseline.getStart());
		assertEquals(modelWallTime("2026-10-07T17:00:00"), costOnlyBaseline.getEnd());
		assertEquals(0.17D, costOnlyBaseline.cost(modelWallTime("2026-10-07T08:00:00"),
			modelWallTime("2026-10-07T17:00:00")), 0.00001D);
		assertEquals(0L, assignment.getBaselineValues(9).work(), "Explicit zero remains distinct from a missing slot");
		assertNull(assignment.getBaselineAssignment(Integer.valueOf(3), false), "Missing slots remain absent");

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(project, mpo));
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		Assignment reopenedAssignment = (Assignment) taskNamed(reopened, "Assignment baseline task").getAssignments().iterator().next();
		Assignment reopenedBaseline = reopenedAssignment.getBaselineAssignment(Integer.valueOf(0), false);
		assertNotNull(reopenedBaseline);
		assertEquals(8L * 60L * 60L * 1000L, reopenedBaseline.getWork(null));
		assertEquals(0.001D, reopenedAssignment.baselineCost(modelWallTime("2026-10-05T08:00:00"),
			modelWallTime("2026-10-05T17:00:00")), 0.0000001D);
		assertEquals(0.003D, reopenedAssignment.baselineCost(modelWallTime("2026-10-06T08:00:00"),
			modelWallTime("2026-10-06T17:00:00")), 0.0000001D);
		Assignment reopenedSecondBaseline = reopenedAssignment.getBaselineAssignment(Integer.valueOf(2), false);
		assertNotNull(reopenedSecondBaseline);
		assertEquals(1L * 60L * 60L * 1000L, reopenedSecondBaseline.work(
			modelWallTime("2026-10-07T08:00:00"), modelWallTime("2026-10-07T17:00:00")));
		assertEquals(0.17D, reopenedSecondBaseline.cost(modelWallTime("2026-10-07T08:00:00"),
			modelWallTime("2026-10-07T17:00:00")), 0.00001D);
		Assignment reopenedCostOnlyBaseline = reopenedAssignment.getBaselineAssignment(Integer.valueOf(1), false);
		assertNotNull(reopenedCostOnlyBaseline);
		assertNull(reopenedAssignment.getBaselineValues(1).work(), "MPO preserves missing baseline Work");
		assertEquals(modelWallTime("2026-10-07T08:00:00"), reopenedCostOnlyBaseline.getStart());
		assertEquals(modelWallTime("2026-10-07T17:00:00"), reopenedCostOnlyBaseline.getEnd());
		assertEquals(0.17D, reopenedCostOnlyBaseline.cost(modelWallTime("2026-10-07T08:00:00"),
			modelWallTime("2026-10-07T17:00:00")), 0.00001D);
		assertEquals(0L, reopenedAssignment.getBaselineValues(9).work(), "MPO retains explicit zero baseline work");
		assertNull(reopenedAssignment.getBaselineAssignment(Integer.valueOf(3), false), "MPO keeps missing slots absent");

		ByteArrayOutputStream roundTripXml = new ByteArrayOutputStream();
		assertTrue(new MicrosoftImporter().saveProject(reopened, roundTripXml));
		String exportedXml = roundTripXml.toString(java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(exportedXml.contains("<TimephasedData><Type>4</Type><UID>1</UID>"
			+ "<Start>2026-10-05T08:00:00</Start><Finish>2026-10-05T17:00:00</Finish>"
			+ "<Unit>2</Unit><Value>PT2H0M0S</Value></TimephasedData>"), exportedXml);
		assertTrue(exportedXml.contains("<TimephasedData><Type>4</Type><UID>1</UID>"
			+ "<Start>2026-10-06T08:00:00</Start><Finish>2026-10-06T17:00:00</Finish>"
			+ "<Unit>2</Unit><Value>PT6H0M0S</Value></TimephasedData>"), exportedXml);
		assertTrue(exportedXml.contains("<TimephasedData><Type>5</Type><UID>1</UID>"
			+ "<Start>2026-10-05T08:00:00</Start><Finish>2026-10-05T17:00:00</Finish>"
			+ "<Unit>2</Unit><Value>0.1</Value></TimephasedData>"), exportedXml);
		assertTrue(exportedXml.contains("<TimephasedData><Type>5</Type><UID>1</UID>"
			+ "<Start>2026-10-06T08:00:00</Start><Finish>2026-10-06T17:00:00</Finish>"
			+ "<Unit>2</Unit><Value>0.3</Value></TimephasedData>"), exportedXml);
		assertTrue(exportedXml.contains("<TimephasedData><Type>23</Type><UID>1</UID>"
			+ "<Start>2026-10-07T08:00:00</Start><Finish>2026-10-07T17:00:00</Finish>"
			+ "<Unit>2</Unit><Value>17</Value></TimephasedData>"), exportedXml);
		assertTrue(exportedXml.contains("<TimephasedData><Type>17</Type><UID>1</UID>"
			+ "<Start>2026-10-07T08:00:00</Start><Finish>2026-10-07T17:00:00</Finish>"
			+ "<Unit>2</Unit><Value>17</Value></TimephasedData>"), exportedXml);
		assertTrue(exportedXml.contains("<TimephasedData><Type>22</Type><UID>1</UID>"
			+ "<Start>2026-10-07T08:00:00</Start><Finish>2026-10-07T17:00:00</Finish>"
			+ "<Unit>2</Unit><Value>PT1H0M0S</Value></TimephasedData>"), exportedXml);
		Project roundTrip = importMspdi(exportedXml);
		Assignment roundTripAssignment = (Assignment) taskNamed(roundTrip, "Assignment baseline task").getAssignments().iterator().next();
		assertEquals(0.001D, roundTripAssignment.baselineCost(modelWallTime("2026-10-05T08:00:00"),
			modelWallTime("2026-10-05T17:00:00")), 0.0000001D);
		assertEquals(0.003D, roundTripAssignment.baselineCost(modelWallTime("2026-10-06T08:00:00"),
			modelWallTime("2026-10-06T17:00:00")), 0.0000001D);
		Assignment roundTripBaseline = roundTripAssignment.getBaselineAssignment(Integer.valueOf(0), false);
		assertNotNull(roundTripBaseline);
		assertEquals(2L * 60L * 60L * 1000L,
			roundTripBaseline.work(modelWallTime("2026-10-05T08:00:00"), modelWallTime("2026-10-05T17:00:00")));
		assertEquals(6L * 60L * 60L * 1000L,
			roundTripBaseline.work(modelWallTime("2026-10-06T08:00:00"), modelWallTime("2026-10-06T17:00:00")));
		Assignment roundTripSecondBaseline = roundTripAssignment.getBaselineAssignment(Integer.valueOf(2), false);
		assertNotNull(roundTripSecondBaseline);
		assertEquals(1L * 60L * 60L * 1000L, roundTripSecondBaseline.work(
			modelWallTime("2026-10-07T08:00:00"), modelWallTime("2026-10-07T17:00:00")));
		Assignment roundTripCostOnlyBaseline = roundTripAssignment.getBaselineAssignment(Integer.valueOf(1), false);
		assertNotNull(roundTripCostOnlyBaseline);
		assertNull(roundTripAssignment.getBaselineValues(1).work());
		assertEquals(0.17D, roundTripCostOnlyBaseline.cost(modelWallTime("2026-10-07T08:00:00"),
			modelWallTime("2026-10-07T17:00:00")), 0.00001D);
		assertEquals(0L, roundTripAssignment.getBaselineValues(9).work());
		assertNull(roundTripAssignment.getBaselineAssignment(Integer.valueOf(3), false));
	}

	@Test
	void importedMppAssignmentsCanBeSavedAndReloadedAsMpo() throws Exception {
		Path sample = Path.of("modules", "microproject_exchange", "testdata", "New Product.mpp");
		if (!Files.exists(sample))
			sample = Path.of("testdata", "New Product.mpp");
		assertTrue(Files.exists(sample), sample.toString());
		String mppHeader = new String(Files.readAllBytes(sample), java.nio.charset.StandardCharsets.ISO_8859_1);
		assertTrue(mppHeader.contains("Microsoft Project 9.0") && mppHeader.contains("MSProject.MPP9"),
			"fixture identifies itself as Microsoft Project 9.0 MPP");
		net.sf.mpxj.ProjectFile rawSource = new net.sf.mpxj.mpp.MPPReader().read(sample.toFile());
		assertEquals(38, rawSource.getResourceAssignments().size(), "fixture source assignment inventory");
		assertTrue(rawSource.getResourceAssignments().stream().noneMatch(assignment ->
			com.microproject.exchange.mpxj.MpxjApi.hasCustomWorkContour(assignment)
				|| com.microproject.exchange.mpxj.MpxjApi.workContourId(assignment) == 8),
			"New Product.mpp does not contain custom-contour assignments");
		long sourceResourceAssignments = rawSource.getResourceAssignments().stream()
			.filter(assignment -> assignment.getResourceUniqueID() != null
				&& assignment.getResourceUniqueID().intValue() != -65535).count();
		com.microproject.core.pm.exchange.MspImporter importer = new com.microproject.core.pm.exchange.MspImporter();
		Project imported = importer.importProject(sample.toAbsolutePath().toString(),
			new com.microproject.core.pm.exchange.MspImporter.ProgressClosure() {
				@Override public void updateProgress(float progress, String label) { }
			});
		java.util.Iterator<Task> taskIterator = imported.getTaskOutlineIterator();
		assertTrue(taskIterator.hasNext(), "sample MPP must contain an editable task");
		Task editedTask = taskIterator.next();
		assertEquals("Begin project", editedTask.getName());
		Assignment sourceAssignment = (Assignment) ((NormalTask) editedTask).getAssignments().iterator().next();
		assertEquals(978336000000L, sourceAssignment.getStart());
		assertEquals(978368400000L, sourceAssignment.getEnd());
		assertEquals(28_800_000L, sourceAssignment.getWork(null));
		assertEquals(0L, sourceAssignment.getActualWork(null));
		assertEquals(28_800_000L, sourceAssignment.getRemainingWork());
		assertEquals(1.0d, sourceAssignment.getUnits(), 0.00001d);
		editedTask.setName("Edited first imported task");
		editedTask.setNotes("Saved through the native MPO workflow");
		List<String> expected = assignmentScheduleSemantics(imported);
		assertFalse(expected.isEmpty(), "sample MPP must contain resource assignments");
		assertEquals(sourceResourceAssignments, expected.size(), "all source resource assignments are imported");
		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(imported, mpo));
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		assertEquals(expected, assignmentScheduleSemantics(reopened),
			"MPP assignment work, bounds, and progress must survive MPO persistence");
	}

	/**
	 * Microsoft Project Desktop documents negative lead as overlapping work between a task and its predecessor;
	 * this MPP9 import contract is document-derived, not a modern MSP run. Preserve the source's signed FS lead
	 * through import and MPO so the successor's overlap date remains unchanged.
	 * Source: https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
	 * (Project Online Desktop Client, Project Standard/Professional 2016–2024).
	 */
	@Test
	void mpp9NegativeLeadLinkUsesEightHourWorkdayAndSurvivesMpoReload() throws Exception {
		Path sample = Path.of("modules", "microproject_exchange", "testdata", "New Product.mpp");
		if (!Files.isRegularFile(sample)) sample = Path.of("testdata", "New Product.mpp");
		assertTrue(Files.isRegularFile(sample), sample.toString());

		net.sf.mpxj.ProjectFile oracle = new net.sf.mpxj.mpp.MPPReader().read(sample.toFile());
		assertEquals(Integer.valueOf(480), oracle.getProjectProperties().getMinutesPerDay(),
			"MPP9 project uses an eight-hour workday");
		assertEquals(Integer.valueOf(2400), oracle.getProjectProperties().getMinutesPerWeek(),
			"MPP9 project uses a forty-hour workweek");
		net.sf.mpxj.Task rawPredecessor = oracle.getTaskByUniqueID(Integer.valueOf(24));
		net.sf.mpxj.Task rawSuccessor = oracle.getTaskByUniqueID(Integer.valueOf(27));
		assertNotNull(rawPredecessor);
		assertNotNull(rawSuccessor);
		assertEquals(modelWallTime("2001-03-12T08:00:00"), DateUTCConverter.toModelTime(rawPredecessor.getStart()));
		assertEquals(modelWallTime("2001-04-13T17:00:00"), DateUTCConverter.toModelTime(rawPredecessor.getFinish()));
		assertEquals(modelWallTime("2001-03-26T08:00:00"), DateUTCConverter.toModelTime(rawSuccessor.getStart()));
		assertEquals(modelWallTime("2001-04-13T17:00:00"), DateUTCConverter.toModelTime(rawSuccessor.getFinish()));
		net.sf.mpxj.Relation rawLink = rawSuccessor.getPredecessors().stream()
			.filter(link -> link.getTargetTask().getUniqueID().intValue() == 24).findFirst()
			.orElseThrow(() -> new AssertionError("Missing MPXJ predecessor UID 24 for task UID 27"));
		assertEquals(net.sf.mpxj.RelationType.FINISH_START, rawLink.getType());
		assertEquals(-15.0d, rawLink.getLag().getDuration(), 0.00001d, "raw MPXJ lead duration");
		assertEquals(net.sf.mpxj.TimeUnit.DAYS, rawLink.getLag().getUnits(), "raw MPXJ lead units");

		Project imported = importMppFixture(sample);
		assertNegativeLeadProject(imported);
		Project reopened = roundTripMpo(imported);
		assertNegativeLeadProject(reopened);
	}

	private void assertNegativeLeadProject(Project project) {
		assertEquals(8.0d, project.getCalendarOption().getHoursPerDay(), 0.00001d,
			"the imported project keeps its 480-minute workday");
		assertEquals(40.0d, project.getCalendarOption().getHoursPerWeek(), 0.00001d,
			"the imported project keeps its 2400-minute workweek");
		Task predecessor = project.findByUniqueId(24);
		Task successor = project.findByUniqueId(27);
		assertNotNull(predecessor, "MPP9 task UID 24");
		assertNotNull(successor, "MPP9 task UID 27");
		assertEquals(720_000_000L, predecessor.getDurationMillis(), "predecessor duration");
		assertEquals(modelWallTime("2001-03-12T08:00:00"), predecessor.getStart(), "predecessor start");
		assertEquals(modelWallTime("2001-04-13T17:00:00"), predecessor.getEnd(), "predecessor finish");
		assertEquals(432_000_000L, successor.getDurationMillis(), "successor duration");
		assertEquals(modelWallTime("2001-03-26T08:00:00"), successor.getStart(), "successor start");
		assertEquals(modelWallTime("2001-04-13T17:00:00"), successor.getEnd(), "successor finish");
		assertEquals(1, successor.getPredecessorList().size(), "task UID 27 has one imported dependency");
		Dependency dependency = (Dependency) successor.getPredecessorList().get(0);
		assertEquals(24L, ((Task) dependency.getPredecessor()).getUniqueId(), "imported predecessor UID");
		assertEquals(DependencyType.Kind.FS.code(), dependency.getDependencyType(), "imported dependency type");
		assertEquals(-432_000_000L, dependency.getLag(), "15 eight-hour workdays of lead");
		assertEquals(successor.getStart(),
			dependency.calcForwardDependencyDate(predecessor.getStart(), predecessor.getEnd(), true),
			"the imported FS lead schedules the successor at its fixture date");
	}

	@Test
	void mpp9CustomAssignmentContourMatchesIndependentManifestAndMpoReload() throws Exception {
		Path sample = Path.of("modules", "microproject_exchange", "testdata", "mpxj", "mpp9timephasedsegments.mpp");
		if (!Files.isRegularFile(sample))
			sample = Path.of("testdata", "mpxj", "mpp9timephasedsegments.mpp");
		assertTrue(Files.isRegularFile(sample), sample.toString());

		Project imported = new com.microproject.core.pm.exchange.MspImporter().importProject(
			sample.toAbsolutePath().toString(), new com.microproject.core.pm.exchange.MspImporter.ProgressClosure() {
				@Override public void updateProgress(float progress, String label) { }
			});
		assertMpp9ContourManifest(imported);

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(imported, mpo));
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		assertMpp9ContourManifest(reopened);
	}

	@Test
	void project2019Mpp14FixturesMatchIndependentManifestAndMpoReload() throws Exception {
		Path fixtureDirectory = Path.of("modules", "microproject_exchange", "testdata", "mpxj", "project2019");
		if (!Files.isDirectory(fixtureDirectory))
			fixtureDirectory = Path.of("testdata", "mpxj", "project2019");
		net.sf.mpxj.ProjectFile rawLinks = new net.sf.mpxj.mpp.MPPReader()
			.read(fixtureDirectory.resolve("task-links-project2019-mpp14.mpp").toFile());
		net.sf.mpxj.Duration rawOneDayLag = rawLinks.getTaskByUniqueID(Integer.valueOf(4)).getPredecessors().get(0).getLag();
		assertEquals(1.0d, rawOneDayLag.getDuration(), "MPX14 raw one-day link lag");
		assertEquals(net.sf.mpxj.TimeUnit.DAYS, rawOneDayLag.getUnits(), "MPX14 raw one-day link lag unit");
		assertEquals(Integer.valueOf(480), rawLinks.getProjectProperties().getMinutesPerDay(),
			"independent MSPDI source declares an eight-hour workday");
		String sourceMspdi = Files.readString(fixtureDirectory.resolve("task-links-project2019-mspdi.xml"));
		assertTrue(sourceMspdi.contains("<MinutesPerDay>480</MinutesPerDay>"));
		assertTrue(sourceMspdi.contains("<MinutesPerWeek>2400</MinutesPerWeek>"));
		assertTrue(sourceMspdi.contains("<LinkLag>4800</LinkLag>"), "independent MSPDI one-day lag is 480 minutes");

		Project linkedTasks = importMppFixture(fixtureDirectory.resolve("task-links-project2019-mpp14.mpp"));
		assertProject2019TaskLinks(linkedTasks);
		assertProject2019TaskLinks(roundTripMpo(linkedTasks));
		assertProject2019EditedLagSurvivesMpo(linkedTasks);

		Project assignments = importMppFixture(fixtureDirectory.resolve("assignment-assignments-project2019-mpp14.mpp"));
		assertProject2019Assignments(assignments);
		assertProject2019Assignments(roundTripMpo(assignments));

		Project assignmentMspdi = importMspdi(Files.readString(
			fixtureDirectory.resolve("assignment-assignments-project2019-mspdi.xml")));
		assertProject2019Assignments(assignmentMspdi);
		assertProject2019Assignments(roundTripMpo(assignmentMspdi));

		Project calendars = importMppFixture(fixtureDirectory.resolve("calendar-calendars-project2019-mpp14.mpp"));
		String calendarSource = Files.readString(fixtureDirectory.resolve("calendar-calendars-project2019-mspdi.xml"));
		assertTrue(calendarSource.contains("<DayType>2</DayType>"));
		assertTrue(calendarSource.contains("<DayType>1</DayType>"));
		assertTrue(calendarSource.contains("<DayType>7</DayType>"));
		assertTrue(calendarSource.contains("<FromTime>08:00:00</FromTime>"));
		assertTrue(calendarSource.contains("<ToTime>12:00:00</ToTime>"));
		assertTrue(calendarSource.contains("<FromTime>13:00:00</FromTime>"));
		assertTrue(calendarSource.contains("<ToTime>17:00:00</ToTime>"));
		assertProject2019Calendars(calendars);
		assertProject2019CalendarInheritanceTracksBaseChange(calendars);
		assertProject2019Calendars(calendars);
		assertProject2019Calendars(roundTripMpo(calendars));
	}

	@Test
	void project2003Mpp8AndProject2007Mpp12LinksMatchIndependentManifestAndMpoReload() throws Exception {
		Path fixtureRoot = Path.of("modules", "microproject_exchange", "testdata", "mpxj");
		if (!Files.isDirectory(fixtureRoot)) fixtureRoot = Path.of("testdata", "mpxj");
		assertTaskLinkFamilyFixture(fixtureRoot.resolve("project2003"), "project2003", "mpp8");
		assertTaskLinkFamilyFixture(fixtureRoot.resolve("project2007"), "project2007", "mpp12");
	}

	private void assertTaskLinkFamilyFixture(Path directory, String source, String family) throws Exception {
		Path sourceMspdi = directory.resolve("task-links-" + source + "-mspdi.xml");
		String independentSource = Files.readString(sourceMspdi);
		assertTrue(independentSource.contains("<MinutesPerDay>480</MinutesPerDay>"), source + " workday source literal");
		assertTrue(independentSource.contains("<MinutesPerWeek>2400</MinutesPerWeek>"), source + " workweek source literal");
		Project imported = importMppFixture(directory.resolve("task-links-" + source + "-" + family + ".mpp"));
		assertTaskLinkFamilyManifest(imported, family);
		assertTaskLinkFamilyManifest(roundTripMpo(imported), family);
	}

	private void assertTaskLinkFamilyManifest(Project project, String family) {
		int[] predecessors = {1, 3, 5, 7, 9, 11, 13, 15};
		int[] successors = {2, 4, 6, 8, 10, 12, 14, 16};
		int[] types = {1, 1, 1, 1, 1, 2, 3, 0}; // FS, FS, FS, FS, FS, SF, SS, FF
		long workday = 8L * 60L * 60L * 1000L; // MSPDI source: 480 minutes/day, 2400 minutes/week
		long[] lags = {0L, workday, 2L * workday, 5L * workday, 10L * workday,
			2L * workday, 2L * workday, 2L * workday};
		assertEquals(16, project.getTaskList().size(), family + " source task count");
		for (int uid = 1; uid <= 16; uid++) {
			Task task = project.findByUniqueId(uid);
			assertNotNull(task, family + " source task UID " + uid);
			assertEquals((uid & 1) == 1 ? "Task 1" : "Task 2", task.getName(), family + " source task UID " + uid);
			assertEquals(0, task.getOutlineLevel(), family + " top-level task UID " + uid);
		}
		assertNull(project.findByUniqueId(17), family + " fixture has no extra source task UID");
		for (int index = 0; index < predecessors.length; index++) {
			Task successor = project.findByUniqueId(successors[index]);
			assertEquals(1, successor.getPredecessorList().size(), family + " predecessor count UID " + successors[index]);
			Dependency dependency = (Dependency) successor.getPredecessorList().get(0);
			assertEquals(predecessors[index], ((Task) dependency.getPredecessor()).getUniqueId(),
				family + " predecessor UID for successor " + successors[index]);
			assertEquals(types[index], dependency.getDependencyType(), family + " relation type " + index);
			assertEquals(lags[index], dependency.getLag(), family + " scheduled lag milliseconds " + index);
		}
	}

	@Test
	void malformedAndUnknownFamilyMppFailAtImporterBoundaryWithoutPublicationOrInputMutation() throws Exception {
		Path fixtureRoot = Path.of("modules", "microproject_exchange", "testdata", "mpxj", "project2019");
		if (!Files.isDirectory(fixtureRoot)) fixtureRoot = Path.of("testdata", "mpxj", "project2019");
		byte[] validOleUnknownFamily = Files.readAllBytes(fixtureRoot.resolve("task-links-project2019-mpp14.mpp"));
		byte[] unknownToken = "MSProject.MPP14".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
		byte[] replacementToken = "MSProject.MPP99".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
		int tokenOffset = indexOf(validOleUnknownFamily, unknownToken);
		assertTrue(tokenOffset >= 0, "MPP14 OLE CompObj family token exists");
		assertEquals(-1, indexOf(java.util.Arrays.copyOfRange(validOleUnknownFamily, tokenOffset + 1,
			validOleUnknownFamily.length), unknownToken), "only one dispatch token appears in the fixed OLE fixture");
		assertTrue(tokenOffset > 27 && new String(validOleUnknownFamily, tokenOffset - 27, 22,
			java.nio.charset.StandardCharsets.US_ASCII).contains("Microsoft.Project 16.0"),
			"the changed token is the CompObj application-format field");
		System.arraycopy(replacementToken, 0, validOleUnknownFamily, tokenOffset, replacementToken.length);
		assertMppParseFailure(validOleUnknownFamily, "unknown family MSProject.MPP99");
		assertMppParseFailure(new byte[] {(byte) 0xD0, (byte) 0xCF, 0x11}, "truncated OLE header");
	}

	private int indexOf(byte[] source, byte[] target) {
		outer: for (int index = 0; index <= source.length - target.length; index++) {
			for (int offset = 0; offset < target.length; offset++)
				if (source[index + offset] != target[offset]) continue outer;
			return index;
		}
		return -1;
	}

	private void assertMppParseFailure(byte[] data, String scenario) {
		byte[] original = data.clone();
		List<String> progress = new java.util.ArrayList<>();
		net.sf.mpxj.MPXJException failure = assertThrows(net.sf.mpxj.MPXJException.class,
			() -> new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(data), "mpp", (value, label) -> progress.add(label)), scenario);
		if (scenario.contains("MPP99")) {
			assertTrue(failure.getMessage().contains("MSProject.MPP99"), failure.toString());
		}
		assertArrayEquals(original, data, scenario + " input bytes remain unchanged");
		assertEquals(List.of("Start"), progress, scenario + " fails before the importer publishes parsed/model state");
	}

	@Test
	void project2013Mpp14TaskConstraintMatchesUpstreamManifestAndMpoReload() throws Exception {
		Path fixtureDirectory = Path.of("modules", "microproject_exchange", "testdata", "mpxj", "project2013");
		if (!Files.isDirectory(fixtureDirectory))
			fixtureDirectory = Path.of("testdata", "mpxj", "project2013");
		Path mpp = fixtureDirectory.resolve("mpp14task-from2013.mpp");
		assertTrue(Files.isRegularFile(mpp), mpp.toString());
		net.sf.mpxj.ProjectFile mppOracle = new net.sf.mpxj.mpp.MPPReader().read(mpp.toFile());
		net.sf.mpxj.Task rawTask = mppOracle.getTaskByUniqueID(Integer.valueOf(1));
		assertNotNull(rawTask);
		assertEquals(net.sf.mpxj.ConstraintType.MUST_START_ON, rawTask.getConstraintType(),
			"MPXJ pinned MppTaskTest.testMpp14TaskFromProject2013 expects MUST_START_ON");
		assertEquals("23/08/2006", new java.text.SimpleDateFormat("dd/MM/yyyy").format(rawTask.getConstraintDate()),
			"MPXJ pinned MppTaskTest.testMpp14TaskFromProject2013 literal date oracle");

		Project imported = importMppFixture(mpp);
		assertProject2013TaskConstraint(imported, rawTask);
		assertProject2013TaskConstraint(roundTripMpo(imported), rawTask);
	}

	private void assertProject2013TaskConstraint(Project project, net.sf.mpxj.Task rawTask) {
		Task task = project.getTaskList().stream()
			.filter(candidate -> candidate.getUniqueId() == rawTask.getUniqueID().longValue()).findFirst()
			.orElseThrow(() -> new AssertionError("Missing imported task UID " + rawTask.getUniqueID()));
		assertEquals(ConstraintType.Kind.MSO, task.getConstraintTypeKind());
		assertEquals(DateUTCConverter.toModelTime(rawTask.getConstraintDate()), task.getConstraintDate(),
			"MPP dates are converted to the model's local-time epoch representation");
	}

	@Test
	void nondefaultMspdiDurationDefaultsSurviveNativeMpoAndNewTaskCreation() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "Assignment Baseline Scalars.xml");
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "Assignment Baseline Scalars.xml");
		String sourceXml = Files.readString(fixture)
			.replace("<MinutesPerDay>480</MinutesPerDay>", "<MinutesPerDay>360</MinutesPerDay>")
			.replace("<MinutesPerWeek>2400</MinutesPerWeek>", "<MinutesPerWeek>1800</MinutesPerWeek>");
		net.sf.mpxj.ProjectFile source = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(sourceXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		source.getProjectProperties().setDefaultDurationUnits(net.sf.mpxj.TimeUnit.HOURS);
		source.getProjectProperties().setDefaultWorkUnits(net.sf.mpxj.TimeUnit.MINUTES);
		ByteArrayOutputStream configuredSource = new ByteArrayOutputStream();
		new net.sf.mpxj.mspdi.MSPDIWriter().write(source, configuredSource);
		sourceXml = configuredSource.toString(java.nio.charset.StandardCharsets.UTF_8);
		assertEquals(Integer.valueOf(360), source.getProjectProperties().getMinutesPerDay());
		assertEquals(Integer.valueOf(1800), source.getProjectProperties().getMinutesPerWeek());

		Project imported = importMspdi(sourceXml);
		assertEquals(360.0d, imported.getDurationDefaults().minutesPerDay(), 0.00001d);
		assertEquals(1800.0d, imported.getDurationDefaults().minutesPerWeek(), 0.00001d);
		assertEquals(20.0d, imported.getDurationDefaults().daysPerMonth(), 0.00001d);
		assertEquals(com.microproject.datatype.TimeUnit.HOURS, imported.getDurationDefaults().durationEnteredIn());
		assertEquals(com.microproject.datatype.TimeUnit.MINUTES, imported.getDurationDefaults().workUnit(),
			"MSPDI default work units are independent from default duration units");
		NormalTask importedTask = taskNamed(imported, "Assignment baseline task");
		assertEquals(com.microproject.datatype.TimeUnit.DAYS, importedTask.getDurationDisplayUnit(),
			"an explicit source task duration unit is retained independently of the project default");
		assertEquals(8L * 60L * 60L * 1000L,
			com.microproject.datatype.Duration.millis(importedTask.getRawDuration()),
			"the imported explicit task duration is independent of the six-hour default day; source="
				+ source.getTaskByUniqueID(Integer.valueOf(1)).getDuration() + " projectDefaults="
				+ imported.getDurationDefaults());
		NormalTask newTask = new NormalTask(imported);
		assertEquals(6L * 60L * 60L * 1000L, com.microproject.datatype.Duration.millis(newTask.getRawDuration()),
			"new one-day task follows the MSPDI project's six-hour duration default");

		Project reopened = roundTripMpo(imported);
		assertEquals(360.0d, reopened.getDurationDefaults().minutesPerDay(), 0.00001d);
		assertEquals(1800.0d, reopened.getDurationDefaults().minutesPerWeek(), 0.00001d);
		assertEquals(com.microproject.datatype.TimeUnit.HOURS, reopened.getDurationDefaults().durationEnteredIn());
		assertEquals(com.microproject.datatype.TimeUnit.MINUTES, reopened.getDurationDefaults().workUnit());
		assertEquals(com.microproject.datatype.TimeUnit.DAYS,
			taskNamed(reopened, "Assignment baseline task").getDurationDisplayUnit(),
			"native MPO preserves the MSPDI task DurationFormat as task display metadata");
		assertEquals(6L * 60L * 60L * 1000L,
			com.microproject.datatype.Duration.millis(new NormalTask(reopened).getRawDuration()));
	}

	@Test
	void sixHourDurationAndProgressSurviveMpoReloadAlongsideHierarchyEdits() throws Exception {
		Project project = createProject();
		project.setDurationDefaults(new com.microproject.datatype.DurationDefaults(360.0d, 1800.0d, 20.0d,
			com.microproject.datatype.TimeUnit.HOURS, com.microproject.datatype.TimeUnit.HOURS));
		NormalTask task = project.createScriptedTask();
		task.setName("Ten-hour task");
		task.getCurrentSchedule().setStart(modelWallTime("2026-10-12T08:00:00"));
		task.setDuration(com.microproject.datatype.Duration.getInstance(10.0d,
			com.microproject.datatype.TimeUnit.HOURS, project.getDurationDefaults()));
		task.setPercentComplete(1.0d);
		assertEquals(10L * 60L * 60L * 1000L,
			com.microproject.datatype.Duration.millis(task.getRawDuration()), "10h is physical elapsed duration");

		NormalTask structuralParent = project.createScriptedTask();
		structuralParent.setName("Structural parent");
		structuralParent.getCurrentSchedule().setStart(modelWallTime("2026-10-12T08:00:00"));
		structuralParent.setDuration(com.microproject.datatype.Duration.getInstance(6.0d,
			com.microproject.datatype.TimeUnit.HOURS, project.getDurationDefaults()));
		structuralParent.setPercentComplete(1.0d);
		NormalTask structuralChild = project.createScriptedTask();
		structuralChild.setName("Structural child");
		structuralChild.setDuration(com.microproject.datatype.Duration.getInstance(6.0d,
			com.microproject.datatype.TimeUnit.HOURS, project.getDurationDefaults()));
		structuralChild.setPercentComplete(1.0d);
		project.setLocalParent(structuralChild, structuralParent);
		assertSame(structuralParent, structuralChild.getWbsParentTask());
		assertTrue(structuralParent.getWbsChildrenTasks().contains(structuralChild));
		long summaryDuration = com.microproject.datatype.Duration.millis(structuralParent.getRawDuration());
		project.setLocalParent(structuralChild, null);
		assertNull(structuralChild.getWbsParentTask());
		assertTrue(structuralParent.getWbsChildrenTasks().isEmpty(), "outdent updates the old parent's cached WBS snapshot");
		assertTrue(com.microproject.datatype.Duration.millis(structuralParent.getRawDuration()) >= 0,
			"an empty summary becomes a valid leaf schedule");
		assertEquals(summaryDuration, com.microproject.datatype.Duration.millis(structuralParent.getRawDuration()),
			"the leaf duration is recalculated from the last summary schedule span");
		assertEquals(1.0d, structuralParent.getPercentComplete(), 0.00001d,
			"the summary's 100% progress remains valid when it becomes a leaf");
		for (Task outlineTask : project.getTasks()) {
			assertTrue(outlineTask.getEnd() >= outlineTask.getStart(), "valid schedule bounds for " + outlineTask.getName());
		}

		Project reopened = roundTripMpo(project);
		NormalTask reopenedTask = taskNamed(reopened, "Ten-hour task");
		assertEquals(10L * 60L * 60L * 1000L,
			com.microproject.datatype.Duration.millis(reopenedTask.getRawDuration()),
			"MPO preserves physical duration after hierarchy edits");
		assertEquals(1.0d, reopenedTask.getPercentComplete(), 0.00001d, "MPO preserves 100% progress");
		NormalTask reopenedParent = taskNamed(reopened, "Structural parent");
		assertNull(reopenedTask.getWbsParentTask(), "MPO preserves the edited task's root placement");
		assertTrue(reopenedParent.getWbsChildrenTasks().isEmpty());
		assertEquals(summaryDuration, com.microproject.datatype.Duration.millis(reopenedParent.getRawDuration()),
			"MPO preserves the summary-to-leaf physical duration");
		assertEquals(1.0d, reopenedParent.getPercentComplete(), 0.00001d,
			"MPO preserves progress after the summary becomes a leaf");
	}

	@Test
	void project2019TaskBaselineSlotsMatchIndependentManifestAndMpoReload() throws Exception {
		Path fixtureDirectory = Path.of("modules", "microproject_exchange", "testdata", "mpxj", "project2019");
		if (!Files.isDirectory(fixtureDirectory))
			fixtureDirectory = Path.of("testdata", "mpxj", "project2019");
		Path sourceMspdi = fixtureDirectory.resolve("task-baselines-project2019-mspdi.xml");
		assertTrue(Files.isRegularFile(sourceMspdi), sourceMspdi.toString());
		String mspdi = Files.readString(sourceMspdi);
		assertTrue(mspdi.contains("<Name>Baseline Duration - Task</Name>"), "source baseline task is present");
		assertTrue(mspdi.contains("<Number>10</Number>"), "source contains numbered baseline 10");
		assertTrue(mspdi.contains("<Duration>PT88H0M0S</Duration>"), "source baseline zero duration is 88 hours");
		assertTrue(mspdi.contains("<Name>Baseline Cost - Task</Name>"), "source aggregate baseline cost task is present");
		assertTrue(mspdi.contains("<Cost>100</Cost>"), "source explicitly records baseline cost 100");
		String duplicateCostOnlySlot = mspdi.replaceFirst("</Baseline>",
			"</Baseline><Baseline><Number>0</Number><Cost>100</Cost></Baseline>");
		assertThrows(IllegalArgumentException.class, () -> importMspdi(duplicateCostOnlySlot),
			"Duplicate baseline slots must be rejected even when the duplicate only contains total Cost");
		Project mppImport = importMppFixture(fixtureDirectory.resolve("task-baselines-project2019-mpp14.mpp"));
		assertBaselineDuration(taskNamed(mppImport, "Baseline Duration - Task").getBaselineDuration(0),
			11.0d, com.microproject.datatype.TimeUnit.DAYS);
		assertBaselineDuration(taskNamed(mppImport, "Baseline1 Duration - Task").getBaselineDuration(1),
			12.0d, com.microproject.datatype.TimeUnit.DAYS);
		assertBaselineDuration(taskNamed(mppImport, "Baseline10 Duration - Task").getBaselineDuration(10),
			21.0d, com.microproject.datatype.TimeUnit.DAYS);
		assertEquals(1.0d, taskNamed(mppImport, "Baseline Cost - Task").getBaselineCost(0), 0.00001d,
			"MPP14 scalar baseline cost follows the pinned MPXJ MPP oracle");
		assertEquals(2.0d, taskNamed(mppImport, "Baseline1 Cost - Task").getBaselineCost(1), 0.00001d);
		assertEquals(11.0d, taskNamed(mppImport, "Baseline10 Cost - Task").getBaselineCost(10), 0.00001d);
		assertProject2019MppTaskBaselines(mppImport);
		assertProject2019MppTaskBaselines(roundTripMpo(mppImport));

		NormalTask importedCostTask = (NormalTask) taskNamed(importMspdi(mspdi), "Baseline Cost - Task");
		Project lifecycleProject = (Project) importedCostTask.getProject();
		assertEquals(1.0d, importedCostTask.getBaselineCost(0), 0.00001d);
		importedCostTask.setFixedCost(9.0d);
		var overwrite = lifecycleProject.saveCurrentToSnapshot(Integer.valueOf(0), false,
			List.of(importedCostTask));
		assertEquals(9.0d, importedCostTask.getBaselineCost(0), 0.00001d,
			"Overwriting a baseline replaces its imported aggregate with the selected current snapshot");
		overwrite.undo();
		assertEquals(1.0d, importedCostTask.getBaselineCost(0), 0.00001d,
			"Undo restores the imported aggregate baseline value");
		overwrite.redo();
		assertEquals(9.0d, importedCostTask.getBaselineCost(0), 0.00001d,
			"Redo restores the overwritten current snapshot without the stale aggregate");

		Project imported = importMspdi(mspdi);
		assertProject2019TaskBaselines(imported);
		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(imported, mpo));
		String normalized = new String(nativeProjectXml(mpo.toByteArray()), java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(normalized.contains("<Cost>100</Cost>"), "standard MSPDI output preserves total baseline Cost");
		assertTrue(normalized.contains("<FixedCost>0</FixedCost>"), "standard MSPDI keeps FixedCost separate");
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		assertProject2019TaskBaselines(reopened);
		Project explicitZero = importMspdi(mspdi.replaceFirst("<Cost>100</Cost>", "<Cost>0</Cost>"));
		NormalTask zeroCostTask = (NormalTask) taskNamed(explicitZero, "Baseline Cost - Task");
		assertNotNull(zeroCostTask.getSnapshot(Integer.valueOf(0)), "explicit zero keeps its baseline snapshot");
		assertEquals(0.0d, zeroCostTask.getBaselineCost(0), 0.00001d,
			"explicit zero remains a present total baseline cost");

		Project fractionalCurrency = importMspdi(mspdi.replaceFirst("<Cost>100</Cost>", "<Cost>0.1</Cost>"));
		NormalTask fractionalTask = (NormalTask) taskNamed(fractionalCurrency, "Baseline Cost - Task");
		assertEquals(0.001d, fractionalTask.getBaselineCost(0), 0.0000001d,
			"currency precision is not limited by the UI CurrencyDigits display setting");
		ByteArrayOutputStream fractionalMpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(fractionalCurrency, fractionalMpo));
		String fractionalXml = new String(nativeProjectXml(fractionalMpo.toByteArray()),
			java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(fractionalXml.contains("<Cost>0.1</Cost>"),
			"MSPDI hundredths preserve sub-cent normalized model costs");
		Project fractionalReopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(fractionalMpo.toByteArray()));
		assertEquals(0.001d, taskNamed(fractionalReopened, "Baseline Cost - Task").getBaselineCost(0), 0.0000001d);
		Project negativeCurrency = importMspdi(mspdi.replaceFirst("<Cost>100</Cost>", "<Cost>-0.1</Cost>"));
		assertEquals(-0.001d, taskNamed(negativeCurrency, "Baseline Cost - Task").getBaselineCost(0), 0.0000001d,
			"finite negative currency values are preserved as credits");
		assertThrows(IllegalArgumentException.class,
			() -> importMspdi(mspdi.replaceFirst("<Cost>100</Cost>", "<Cost>Infinity</Cost>")),
			"non-finite baseline scalar currency is rejected");

		int resourceStart = mspdi.indexOf("<Resource>");
		int resourceEnd = mspdi.indexOf("</Resource>", resourceStart);
		assertTrue(resourceStart >= 0 && resourceEnd > resourceStart);
		String resourceXml = mspdi.substring(resourceStart, resourceEnd)
			.replace("<UID>0</UID>", "<UID>1</UID>")
			.replace("<ID>0</ID>", "<ID>1</ID>")
			.replace("<CostPerUse>0</CostPerUse>", "<CostPerUse>0.1</CostPerUse>")
			.replace("<ID>1</ID>", "<ID>1</ID><Name>Fractional Cost Resource</Name>");
		String resourceCostMspdi = mspdi.substring(0, resourceStart) + resourceXml
			+ mspdi.substring(resourceEnd);
		Project resourceCostProject = importMspdi(resourceCostMspdi);
		com.microproject.pm.resource.Resource fractionalResource =
			com.microproject.pm.resource.ResourcePool.findResourceByName("Fractional Cost Resource",
				resourceCostProject.getResourcePool());
		assertNotNull(fractionalResource);
		assertEquals(0.001d, fractionalResource.getCostPerUse(),
			0.0000001d, "raw MSPDI CostPerUse uses the same currency scale as MPXJ");
		ByteArrayOutputStream resourceCostMpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(resourceCostProject, resourceCostMpo));
		String resourceCostXml = new String(nativeProjectXml(resourceCostMpo.toByteArray()),
			java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(resourceCostXml.contains("<CostPerUse>0.1</CostPerUse>"),
			"the canonical MSPDI writer emits resource cost-per-use in hundredths");
		Project resourceCostReopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(resourceCostMpo.toByteArray()));
		assertEquals(0.001d, com.microproject.pm.resource.ResourcePool.findResourceByName(
			"Fractional Cost Resource", resourceCostReopened.getResourcePool()).getCostPerUse(),
			0.0000001d, "MPO reimport preserves normalized resource cost-per-use");
	}

	private void assertProject2019EditedLagSurvivesMpo(Project imported) throws Exception {
		Task predecessor = imported.findByUniqueId(3);
		Task successor = imported.findByUniqueId(4);
		predecessor.getCurrentSchedule().setStart(modelWallTime("2018-10-22T08:00:00"));
		predecessor.getCurrentSchedule().setFinish(modelWallTime("2018-10-22T17:00:00"));
		com.microproject.pm.dependency.Dependency dependency =
			(com.microproject.pm.dependency.Dependency) successor.getPredecessorList().get(0);
		assertEquals(8L * 60L * 60L * 1000L, dependency.getLeadValue(), "one MSP workday is eight scheduled hours");
		assertEquals(modelWallTime("2018-10-24T08:00:00"),
			dependency.calcForwardDependencyDate(predecessor.getStart(), predecessor.getEnd(), true),
			"one-workday lag after moving predecessor to Monday");

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(imported, mpo));
		String nativeMspdi = new String(nativeProjectXml(mpo.toByteArray()), java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(nativeMspdi.contains("<LinkLag>4800</LinkLag>"), "native MSPDI emits 480 source minutes, not 1440");
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		Task reopenedPredecessor = reopened.findByUniqueId(3);
		Task reopenedSuccessor = reopened.findByUniqueId(4);
		com.microproject.pm.dependency.Dependency reopenedDependency =
			(com.microproject.pm.dependency.Dependency) reopenedSuccessor.getPredecessorList().get(0);
		assertEquals(8L * 60L * 60L * 1000L, reopenedDependency.getLeadValue(), "MPO-reopened one-workday lag");
		assertEquals(modelWallTime("2018-10-24T08:00:00"),
			reopenedDependency.calcForwardDependencyDate(reopenedPredecessor.getStart(),
				reopenedPredecessor.getEnd(), true), "reopened link keeps its schedule meaning");
	}

	private Project importMppFixture(Path sample) throws Exception {
		assertTrue(Files.isRegularFile(sample), sample.toString());
		return new com.microproject.core.pm.exchange.MspImporter().importProject(sample.toAbsolutePath().toString(),
			new com.microproject.core.pm.exchange.MspImporter.ProgressClosure() {
				@Override public void updateProgress(float progress, String label) { }
			});
	}

	private Project roundTripMpo(Project project) throws Exception {
		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(project, mpo));
		return new com.microproject.exchange.MpoFileImporter().loadProject(new ByteArrayInputStream(mpo.toByteArray()));
	}

	private void assertProject2019TaskLinks(Project project) {
		long eightHours = 8L * 60L * 60L * 1000L;
		int[] predecessorUids = {1, 3, 5, 7, 9, 11, 13, 15};
		int[] successorUids = {2, 4, 6, 8, 10, 12, 14, 16};
		int[] dependencyTypes = {1, 1, 1, 1, 1, 2, 3, 0};
		String[] starts = {
			"2018-10-18T08:00:00", "2018-10-19T08:00:00", "2018-10-18T08:00:00", "2018-10-22T08:00:00",
			"2018-10-18T08:00:00", "2018-10-23T08:00:00", "2018-10-18T08:00:00", "2018-10-26T08:00:00",
			"2018-10-18T08:00:00", "2018-11-02T08:00:00", "2018-10-18T08:00:00", "2018-10-19T08:00:00",
			"2018-10-18T08:00:00", "2018-10-22T08:00:00", "2018-10-18T08:00:00", "2018-10-22T08:00:00"
		};
		long workDayMillis = 8L * 60L * 60L * 1000L;
		long[] expectedLagMillis = {0L, workDayMillis, 2L * workDayMillis, 5L * workDayMillis,
			10L * workDayMillis, 2L * workDayMillis, 2L * workDayMillis, 2L * workDayMillis};
		for (int uid = 1; uid <= 16; uid++) {
			Task task = project.findByUniqueId(uid);
			assertNotNull(task, "MPP14 task UID " + uid);
			assertEquals((uid & 1) == 1 ? "Task 1" : "Task 2", task.getName(), "task UID " + uid);
			assertEquals(0, task.getOutlineLevel(), "MPP14 top-level task for UID " + uid);
			assertEquals(eightHours, task.getDurationMillis(), "MPP14 duration for UID " + uid);
			assertEquals(modelWallTime(starts[uid - 1]), task.getStart(), "MPP14 source start for UID " + uid);
			assertEquals(modelWallTime(starts[uid - 1].substring(0, 11) + "17:00:00"), task.getEnd(),
				"MPP14 source finish for UID " + uid);
		}
		for (int index = 0; index < predecessorUids.length; index++) {
			Task successor = project.findByUniqueId(successorUids[index]);
			assertEquals(1, successor.getPredecessorList().size(), "predecessor count for UID " + successorUids[index]);
			com.microproject.pm.dependency.Dependency dependency =
				(com.microproject.pm.dependency.Dependency) successor.getPredecessorList().get(0);
			assertEquals(predecessorUids[index], ((Task) dependency.getPredecessor()).getUniqueId());
			assertEquals(dependencyTypes[index], dependency.getDependencyType(), "dependency type at pair " + index);
			assertEquals(expectedLagMillis[index], dependency.getLag(), "dependency lag at pair " + index);
		}
	}

	private void assertProject2019Assignments(Project project) {
		String[] resourceNames = {"Resource 1", "Resource 2", "Resource 3"};
		double[] taskProgress = {0.0d, 0.25d, 0.5d};
		long[] actualWork = {0L, 20L, 40L};
		for (int uid = 1; uid <= resourceNames.length; uid++) {
			Task task = project.findByUniqueId(uid);
			assertNotNull(task, "MPP14 assignment task UID " + uid);
			assertEquals("Task " + uid, task.getName());
			assertTrue(task instanceof NormalTask);
			assertEquals(1, ((NormalTask) task).getAssignments().size(), "assignment count for task UID " + uid);
			Assignment assignment = (Assignment) ((NormalTask) task).getAssignments().iterator().next();
			assertEquals(resourceNames[uid - 1], assignment.getResource().getName());
			assertEquals(80L * 60L * 60L * 1000L, assignment.getWork(null), "assignment work for task UID " + uid);
			assertEquals(taskProgress[uid - 1], task.getPercentComplete(), 0.00001d,
				"task progress for UID " + uid);
			assertEquals(actualWork[uid - 1] * 60L * 60L * 1000L, assignment.getActualWork(null),
				"assignment actual work for task UID " + uid);
			assertEquals((80L - actualWork[uid - 1]) * 60L * 60L * 1000L, assignment.getRemainingWork(),
				"assignment remaining work for task UID " + uid);
			assertEquals(taskProgress[uid - 1], assignment.getPercentComplete(), 0.00001d,
				"assignment progress for task UID " + uid);
		}
	}

	private void assertProject2019TaskBaselines(Project project) {
		assertBaselineDuration(taskNamed(project, "Baseline Duration - Task").getBaselineDuration(0),
			11.0d, com.microproject.datatype.TimeUnit.DAYS);
		assertBaselineDuration(taskNamed(project, "Baseline1 Duration - Task").getBaselineDuration(1),
			12.0d, com.microproject.datatype.TimeUnit.DAYS);
		assertBaselineDuration(taskNamed(project, "Baseline10 Duration - Task").getBaselineDuration(10),
			21.0d, com.microproject.datatype.TimeUnit.DAYS);
		NormalTask totalCostTask = (NormalTask) taskNamed(project, "Baseline Cost - Task");
		assertEquals(1.0d, totalCostTask.getBaselineCost(0), 0.00001d,
			"baseline total cost is preserved separately from baseline fixed cost");
		assertEquals(0.0d, ((com.microproject.pm.task.TaskSnapshot) totalCostTask.getSnapshot(Integer.valueOf(0)))
			.getFixedCost(), 0.00001d,
			"task total cost does not become own baseline fixed cost");
		assertEquals(2.0d, taskNamed(project, "Baseline1 Cost - Task").getBaselineCost(1), 0.00001d);
		assertEquals(11.0d, taskNamed(project, "Baseline10 Cost - Task").getBaselineCost(10), 0.00001d);
		assertNull(taskNamed(project, "Baseline Fixed Cost - Task").getSnapshot(Integer.valueOf(0)),
			"an absent baseline stays absent rather than becoming an explicit zero-cost baseline");
	}

	private void assertProject2019MppTaskBaselines(Project project) {
		assertBaselineDuration(taskNamed(project, "Baseline Duration - Task").getBaselineDuration(0),
			11.0d, com.microproject.datatype.TimeUnit.DAYS);
		assertBaselineDuration(taskNamed(project, "Baseline1 Duration - Task").getBaselineDuration(1),
			12.0d, com.microproject.datatype.TimeUnit.DAYS);
		assertBaselineDuration(taskNamed(project, "Baseline10 Duration - Task").getBaselineDuration(10),
			21.0d, com.microproject.datatype.TimeUnit.DAYS);
		assertEquals(1.0d, taskNamed(project, "Baseline Cost - Task").getBaselineCost(0), 0.00001d);
		assertEquals(2.0d, taskNamed(project, "Baseline1 Cost - Task").getBaselineCost(1), 0.00001d);
		assertEquals(11.0d, taskNamed(project, "Baseline10 Cost - Task").getBaselineCost(10), 0.00001d);
		assertMppBaselineFixedCost(project, "Baseline Fixed Cost - Task", 0, 11.0d);
		assertMppBaselineFixedCost(project, "Baseline1 Fixed Cost - Task", 1, 12.0d);
		assertMppBaselineFixedCost(project, "Baseline10 Fixed Cost - Task", 10, 21.0d);
	}

	private void assertMppBaselineFixedCost(Project project, String name, int slot, double expected) {
		com.microproject.pm.snapshot.DataSnapshot baseline = taskNamed(project, name).getSnapshot(Integer.valueOf(slot));
		assertInstanceOf(com.microproject.pm.task.TaskSnapshot.class, baseline, name + " baseline slot " + slot);
		assertEquals(expected, ((com.microproject.pm.task.TaskSnapshot) baseline).getFixedCost(),
			"MPXJ v11.5.4 TaskBaselinesTest fixed cost at slot " + slot);
	}

	private void assertProject2019Calendars(Project project) {
		long fourHours = 4L * 60L * 60L * 1000L;
		long eightHours = 8L * 60L * 60L * 1000L;
		long mondayKey = modelWallTime("2019-05-06T00:00:00");
		long sundayKey = modelWallTime("2019-05-05T00:00:00");
		long saturdayKey = modelWallTime("2019-05-11T00:00:00");
		for (String resourceName : List.of("Resource One", "Resource Two")) {
			com.microproject.pm.resource.Resource resource =
				com.microproject.pm.resource.ResourcePool.findResourceByName(resourceName, project.getResourcePool());
			assertNotNull(resource, "MPP14 calendar fixture resource " + resourceName);
			assertEquals(resourceName, resource.getWorkCalendar().getName(), "resource calendar for " + resourceName);
			var calendar = (com.microproject.pm.calendar.WorkingCalendar) resource.getWorkCalendar();
			var monday = com.microproject.pm.calendar.CalendarService.getInstance()
				.getDay(calendar, mondayKey);
			assertTrue(monday.isWorking(), "effective Monday date key resolves to work");
			assertEquals(eightHours, monday.getWorkingHours().getDuration(),
				"source calendar has two four-hour Monday work periods");
			var firstPeriod = monday.getWorkingHours().getInterval(0);
			var secondPeriod = monday.getWorkingHours().getInterval(1);
			assertNotNull(firstPeriod);
			assertNotNull(secondPeriod);
			assertEquals(com.microproject.pm.calendar.WorkingHours.hourTime(8), firstPeriod.getStart(),
				"first period starts at 08:00");
			assertEquals(com.microproject.pm.calendar.WorkingHours.hourTime(12), firstPeriod.getEnd(),
				"first period ends at 12:00");
			assertEquals(com.microproject.pm.calendar.WorkingHours.hourTime(13), secondPeriod.getStart(),
				"second period starts at 13:00");
			assertEquals(com.microproject.pm.calendar.WorkingHours.hourTime(17), secondPeriod.getEnd(),
				"second period ends at 17:00");
			assertEquals(fourHours, firstPeriod.getElapsedDuration(), "first period is 08:00–12:00");
			assertEquals(fourHours, secondPeriod.getElapsedDuration(), "second period is 13:00–17:00");
			assertEquals(60L * 60L * 1000L, secondPeriod.getStart() - firstPeriod.getEnd(), "lunch gap is one hour");
			assertEquals(eightHours, calendar.compare(modelWallTime("2019-05-06T17:00:00"),
				modelWallTime("2019-05-06T08:00:00"), false), "Monday worktime excludes the lunch gap");
			assertEquals(0L, calendar.compare(modelWallTime("2019-05-06T13:00:00"),
				modelWallTime("2019-05-06T12:00:00"), false), "lunch interval is nonworking");
			assertEquals(0L, calendar.compare(modelWallTime("2019-05-11T17:00:00"),
				modelWallTime("2019-05-11T08:00:00"), false), "Saturday daytime is nonworking");
			assertEquals(0L, calendar.compare(modelWallTime("2019-05-05T17:00:00"),
				modelWallTime("2019-05-05T08:00:00"), false), "Sunday daytime is nonworking");
			assertFalse(com.microproject.pm.calendar.CalendarService.getInstance()
				.getDay(calendar, sundayKey).isWorking(), "Sunday date key is nonworking");
			assertFalse(com.microproject.pm.calendar.CalendarService.getInstance()
				.getDay(calendar, saturdayKey).isWorking(), "Saturday date key is nonworking");
		}
	}

	private void assertProject2019CalendarInheritanceTracksBaseChange(Project project) throws Exception {
		com.microproject.pm.resource.Resource resource = com.microproject.pm.resource.ResourcePool.findResourceByName(
			"Resource One", project.getResourcePool());
		assertNotNull(resource);
		com.microproject.pm.calendar.WorkCalendar base = resource.getWorkCalendar().getBaseCalendar();
		assertInstanceOf(com.microproject.pm.calendar.WorkingCalendar.class, base);
		var standard = (com.microproject.pm.calendar.WorkingCalendar) base;
		com.microproject.pm.calendar.WorkDay originalMonday = standard.getWeekDay(1);
		com.microproject.pm.calendar.WorkingHours sixHourDay = new com.microproject.pm.calendar.WorkingHours();
		sixHourDay.setInterval(0, com.microproject.pm.calendar.WorkingHours.hourTime(8),
			com.microproject.pm.calendar.WorkingHours.hourTime(11));
		sixHourDay.setInterval(1, com.microproject.pm.calendar.WorkingHours.hourTime(13),
			com.microproject.pm.calendar.WorkingHours.hourTime(16));
		com.microproject.pm.calendar.WorkDay changedMonday = new com.microproject.pm.calendar.WorkDay();
		changedMonday.setWorkingHours(sixHourDay);
		try {
			standard.setWeekDay(1, changedMonday);
			standard.invalidate();
			for (String resourceName : List.of("Resource One", "Resource Two")) {
				com.microproject.pm.resource.Resource derived = com.microproject.pm.resource.ResourcePool.findResourceByName(
					resourceName, project.getResourcePool());
				var derivedCalendar = (com.microproject.pm.calendar.WorkingCalendar) derived.getWorkCalendar();
				assertEquals(6L * 60L * 60L * 1000L,
					com.microproject.pm.calendar.CalendarService.getInstance().getDay(derivedCalendar,
						modelWallTime("2019-05-06T00:00:00")).getWorkingHours().getDuration(),
					"derived resource calendar follows the edited base weekday");
			}
		} finally {
			standard.setWeekDay(1, originalMonday);
			standard.invalidate();
		}
	}

	@Test
	void switchingImportedMppTaskToManualKeepsItsCurrentScheduleAndAssignments() throws Exception {
		Path sample = Path.of("modules", "microproject_exchange", "testdata", "New Product.mpp");
		if (!Files.exists(sample))
			sample = Path.of("testdata", "New Product.mpp");
		Project project = new com.microproject.core.pm.exchange.MspImporter().importProject(
			sample.toAbsolutePath().toString(), new com.microproject.core.pm.exchange.MspImporter.ProgressClosure() {
				@Override public void updateProgress(float progress, String label) { }
			});
		java.util.Iterator<Task> taskIterator = project.getTaskOutlineIterator();
		assertTrue(taskIterator.hasNext(), "sample MPP must contain a task to change to manual scheduling");
		Task task = taskIterator.next();
		assertFalse(task.isManuallyScheduled(), "fixture task must begin in automatic scheduling mode");
		long start = task.getStart();
		long finish = task.getEnd();
		List<String> assignments = assignmentScheduleSemantics(project);
		task.setManuallyScheduled(true);
		assertEquals(start, task.getManualStart(), "manual scheduling must capture the current start");
		assertEquals(finish, task.getManualFinish(), "manual scheduling must capture the current finish");
		assertEquals(start, task.getStart(), "manual scheduling must retain the current start");
		assertEquals(finish, task.getEnd(), "manual scheduling must retain the current finish");
		assertEquals(assignments, assignmentScheduleSemantics(project),
			"manual scheduling must preserve imported assignment bounds and work");
	}

	@Test
	void manuallyScheduledImportedMppTaskKeepsAssignmentsAfterMpoReload() throws Exception {
		Path sample = Path.of("modules", "microproject_exchange", "testdata", "New Product.mpp");
		if (!Files.exists(sample))
			sample = Path.of("testdata", "New Product.mpp");
		Project project = new com.microproject.core.pm.exchange.MspImporter().importProject(
			sample.toAbsolutePath().toString(), new com.microproject.core.pm.exchange.MspImporter.ProgressClosure() {
				@Override public void updateProgress(float progress, String label) { }
			});
		java.util.Iterator<Task> taskIterator = project.getTaskOutlineIterator();
		assertTrue(taskIterator.hasNext(), "sample MPP must contain a task to change to manual scheduling");
		Task task = taskIterator.next();
		assertFalse(task.isManuallyScheduled(), "fixture task must begin in automatic scheduling mode");
		task.setManuallyScheduled(true);
		List<String> expected = assignmentScheduleSemantics(project);
		assertFalse(expected.isEmpty(), "manual task must retain its imported assignments");
		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(project, mpo));
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		assertEquals(expected, assignmentScheduleSemantics(reopened),
			"manual scheduling must preserve imported assignment work through MPO reload");
	}

	@Test
	void completedImportedMilestoneKeepsTaskProgressThroughMpoReload() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "Assignment Baseline Scalars.xml");
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "Assignment Baseline Scalars.xml");
		String xml = Files.readString(fixture).replace("\r\n", "\n")
			.replace("<Start>2026-10-12T08:00:00</Start><Finish>2026-10-12T17:00:00</Finish>\n            <Duration>PT8H0M0S</Duration><DurationFormat>7</DurationFormat><Work>PT8H0M0S</Work>",
				"<Start>2026-10-12T08:00:00</Start><Finish>2026-10-12T08:00:00</Finish>\n            <Duration>PT0S</Duration><DurationFormat>7</DurationFormat><Work>PT0S</Work>")
			.replace("<PercentComplete>0</PercentComplete><PercentWorkComplete>0</PercentWorkComplete>",
				"<PercentComplete>100</PercentComplete><PercentWorkComplete>0</PercentWorkComplete>")
			.replace("<Summary>0</Summary><Milestone>0</Milestone>", "<Summary>0</Summary><Milestone>1</Milestone>")
			.replace("<Start>2026-10-12T08:00:00</Start><Finish>2026-10-12T17:00:00</Finish>\n            <Work>PT8H0M0S</Work>",
				"<Start>2026-10-12T08:00:00</Start><Finish>2026-10-12T08:00:00</Finish>\n            <Work>PT0S</Work>")
			.replace("<Baseline><Number>0</Number><Start>2026-10-05T17:00:00</Start><Work>PT8H0M0S</Work></Baseline>",
				"<Baseline><Number>0</Number><Start>2026-10-05T17:00:00</Start><Work>PT0S</Work></Baseline>")
			.replaceAll("(?s)<Assignments>.*?</Assignments>", "<Assignments/>");
		assertTrue(xml.contains("<Duration>PT0S</Duration><DurationFormat>7</DurationFormat><Work>PT0S</Work>"),
			"fixture rewrite must produce a zero-duration milestone regardless of checkout line endings");
		assertTrue(xml.contains("<PercentComplete>100</PercentComplete><PercentWorkComplete>0</PercentWorkComplete>"));
		net.sf.mpxj.ProjectFile source = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		assertEquals(100.0d, source.getTasks().get(0).getPercentageComplete().doubleValue(), 0.00001d);
		assertEquals(0.0d, source.getTasks().get(0).getDuration().getDuration(), 0.00001d);
		assertEquals(source.getTasks().get(0).getStart(), source.getTasks().get(0).getFinish());
		assertTrue(source.getResourceAssignments().isEmpty(), "milestone has no resource-assignment progress");
		Project imported = importMspdi(xml);
		Task task = taskNamed(imported, "Assignment baseline task");

		assertEquals(0L, task.getDurationMillis());
		assertEquals(1.0d, task.getPercentComplete(), 0.00001d);

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(imported, mpo));
		byte[] savedContent = nativeProjectXml(mpo.toByteArray());
		net.sf.mpxj.ProjectFile savedSource = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(savedContent));
		net.sf.mpxj.Resource unassigned = savedSource.getResources().stream()
			.filter(resource -> resource.getID() != null && resource.getID().intValue() == 0)
			.findFirst().orElseThrow();
		assertEquals(com.microproject.pm.resource.EnterpriseResource.UNASSIGNED_ID,
			unassigned.getUniqueID().intValue(), "unassigned resource must retain its sentinel UID");
		assertTrue(savedSource.getResourceAssignments().stream().allMatch(assignment ->
			assignment.getResourceUniqueID().equals(unassigned.getUniqueID())),
			"default assignments must reference the exported unassigned sentinel");
		assertEquals(100.0d, savedSource.getTasks().stream()
			.filter(sourceTask -> "Assignment baseline task".equals(sourceTask.getName())).findFirst().orElseThrow()
			.getPercentageComplete().doubleValue(), 0.00001d, "MPO content must write milestone progress");
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		NormalTask reopenedTask = (NormalTask) taskNamed(reopened, "Assignment baseline task");
		assertTrue(reopenedTask.getAssignments().stream().allMatch(value -> ((Assignment) value).isDefault()),
			"unassigned milestone must not acquire a real resource during MPO reload");
		assertEquals(1.0d, reopenedTask.getImportedPercentCompleteOverride(), 0.00001d);
		assertEquals(1.0d, reopenedTask.getPercentComplete(), 0.00001d);
	}

	@Test
	void committedSaturdayCalendarRecalculatesAndPersistsTaskAssignmentThroughMpo() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("MPO-saturday-calendar", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		pool.addProject(project);
		var baseCalendar = com.microproject.pm.calendar.WorkingCalendar.getStandardBasedInstance();
		baseCalendar.setName("MPO committed Saturday calendar");
		baseCalendar.setUniqueId(-9000111L);
		var taskCalendar = com.microproject.pm.calendar.WorkingCalendar.getInstanceBasedOn(baseCalendar);
		project.setWorkCalendar(taskCalendar);
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		project.getSchedulingAlgorithm().addObject(task);
		task.setName("MPO Saturday schedule");
		task.setWorkCalendar(taskCalendar);
		ResourceImpl resource = pool.newResourceInstance();
		resource.setName("MPO Saturday assignment resource");
		resource.setWorkCalendar(baseCalendar);
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0d, 0L, this);
		java.util.GregorianCalendar fridayStart = com.microproject.util.DateTime.calendarInstance(
			2026, java.util.Calendar.OCTOBER, 16);
		fridayStart.set(java.util.Calendar.HOUR_OF_DAY, 8);
		long fridayAtEight = fridayStart.getTimeInMillis();
		task.setScheduleConstraint(ConstraintType.Kind.MSO, fridayAtEight);
		task.getCurrentSchedule().setStart(fridayAtEight);
		assignment.setWork(16L * 60L * 60L * 1000L, null);
		assignment.setActualWork(2L * 60L * 60L * 1000L, null);
		task.setDuration(16L * 60L * 60L * 1000L);
		project.recalculate();
		long expectedBeforeCommit = calendarTime(2026, java.util.Calendar.OCTOBER, 19, 17);
		long expectedAfterCommit = calendarTime(2026, java.util.Calendar.OCTOBER, 17, 17);
		assertEquals(expectedBeforeCommit, task.getEnd());
		assertEquals(expectedBeforeCommit, assignment.getEnd());
		taskCalendar.getConcreteInstance();
		baseCalendar.getConcreteInstance();

		var staged = com.microproject.pm.calendar.CalendarService.getInstance().makeScratchCopy(baseCalendar);
		var saturdayHours = new com.microproject.pm.calendar.WorkingHours();
		saturdayHours.setInterval(0, com.microproject.pm.calendar.WorkingHours.hourTime(8),
			com.microproject.pm.calendar.WorkingHours.hourTime(12));
		saturdayHours.setInterval(1, com.microproject.pm.calendar.WorkingHours.hourTime(13),
			com.microproject.pm.calendar.WorkingHours.hourTime(17));
		var saturday = new com.microproject.pm.calendar.WorkDay();
		saturday.setWorkingHours(saturdayHours);
		staged.setWeekDay(java.util.Calendar.SATURDAY - 1, saturday);
		assertTrue(com.microproject.pm.calendar.CalendarService.getInstance().commitCalendars(
			java.util.Map.of(baseCalendar, staged), undo.getEditSupport()));
		assertEquals(expectedAfterCommit, task.getEnd(), "calendar commit must recalculate before returning");
		assertEquals(expectedAfterCommit, assignment.getEnd());

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(project, mpo));
		String rawMpoXml = new String(nativeProjectXml(mpo.toByteArray()), java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(rawMpoXml.contains("MPO committed Saturday calendar"), rawMpoXml);
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		NormalTask reopenedTask = taskNamed(reopened, "MPO Saturday schedule");
		Assignment reopenedAssignment = (Assignment) reopenedTask.getAssignments().iterator().next();
		var reopenedCalendar = reopenedTask.getEffectiveWorkCalendar().getConcreteInstance();
		assertTrue(reopenedCalendar.getWeekDay(java.util.Calendar.SATURDAY - 1).isWorking());
		assertEquals(8L * 60L * 60L * 1000L,
			reopenedCalendar.getWeekDay(java.util.Calendar.SATURDAY - 1).getWorkingHours().getDuration());
		assertEquals(expectedAfterCommit, reopenedTask.getEnd());
		assertEquals(expectedAfterCommit, reopenedAssignment.getEnd());
		assertEquals(16L * 60L * 60L * 1000L, reopenedAssignment.getWork(null));
		assertEquals(2L * 60L * 60L * 1000L, reopenedAssignment.getActualWork(null));
		assertEquals(14L * 60L * 60L * 1000L, reopenedAssignment.getRemainingWork());
	}

	@Test
	void ganttProgressAndTaskSplitPreserveCompletedThroughAcrossMpo() throws Exception {
		Project project = createProject();
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName("Gantt progress persistence");
		task.getCurrentSchedule().setStart(com.microproject.util.DateTime
			.calendarInstance(2026, java.util.Calendar.JUNE, 8).getTimeInMillis());
		task.setDuration(3L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		successor.setName("Gantt progress successor");
		successor.getCurrentSchedule().setStart(project.getStart());
		successor.setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		Assignment assignment = AssignmentService.getInstance().newAssignment(task,
			project.getResourcePool().newResourceInstance(), 1.0d, 0L, this);
		project.recalculate();
		long originalStart = task.getStart();
		long originalFinish = task.getEnd();
		var calendar = task.getEffectiveWorkCalendar();
		long earlierStart = calendar.adjustInsideCalendar(originalStart
			- 3L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay(), false);
		ScheduleService.getInstance().setInterval(this, task, earlierStart, originalFinish,
			new com.microproject.pm.scheduling.ScheduleInterval(originalStart, originalFinish), null);
		task.setScheduleConstraint(ConstraintType.Kind.SNET, task.getStart());
		long resizedStart = task.getStart();
		long laterFinish = originalFinish + com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		ScheduleService.getInstance().setInterval(this, task, resizedStart, laterFinish,
			new com.microproject.pm.scheduling.ScheduleInterval(resizedStart, originalFinish), null);
		project.recalculate();
		task.setPercentComplete(0.4d);
		project.recalculate();
		long initialProgress = task.getCompletedThrough();
		long requested = initialProgress + (task.getEnd() - initialProgress) / 6L;
		assertTrue(ScheduleService.getInstance().setCompleted(this, task, requested, null));
		long progressBeforeSplit = task.getCompletedThrough();
		List<com.microproject.pm.scheduling.ScheduleInterval> taskIntervals = new java.util.ArrayList<>();
		task.consumeIntervals(taskIntervals::add);
		long remainingWorkStart = Math.max(progressBeforeSplit, task.getResume());
		long splitAt = taskIntervals.stream().mapToLong(interval -> {
			long start = Math.max(interval.getStart(), remainingWorkStart);
			long end = interval.getEnd();
			return end - start > 1L ? start + (end - start) / 3L : Long.MIN_VALUE;
		}).filter(candidate -> candidate != Long.MIN_VALUE).findFirst().orElseThrow();
		assertTrue(ScheduleService.getInstance().split(this, task, splitAt, splitAt, null));
		long progressAfterSplit = task.getCompletedThrough();

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(project, mpo));
		byte[] contentXml = nativeProjectXml(mpo.toByteArray());
		net.sf.mpxj.ProjectFile snapshot = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(contentXml));
		net.sf.mpxj.Task savedTask = snapshot.getTasks().stream()
			.filter(candidate -> "Gantt progress persistence".equals(candidate.getName())).findFirst().orElseThrow();
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		NormalTask reopenedTask = taskNamed(reopened, "Gantt progress persistence");
		assertEquals(progressBeforeSplit, progressAfterSplit, "inserting a remaining-work split must not move completed-through");
		assertEquals(task.getPercentComplete(), savedTask.getPercentageComplete().doubleValue() / 100.0d, 0.00001d,
			"MSPDI must persist the task's elapsed-duration percentage");
		assertEquals(task.getPercentComplete(), reopenedTask.getPercentComplete(), 0.00001d,
			"MPO reload must preserve the task's elapsed-duration percentage");
		assertEquals(progressBeforeSplit, reopenedTask.getCompletedThrough(),
			"MPO reload must preserve the Gantt completion boundary: saved=" + progressBeforeSplit
				+ " restored=" + reopenedTask.getCompletedThrough() + " percent=" + savedTask.getPercentageComplete()
				+ " sourcePercent=" + task.getPercentComplete() + " reopenedPercent=" + reopenedTask.getPercentComplete()
				+ " actualDuration=" + savedTask.getActualDuration() + " stop=" + savedTask.getStop()
				+ " start=" + savedTask.getStart() + " finish=" + savedTask.getFinish() + " assignment="
				+ assignment.getStart() + ".." + assignment.getEnd() + " work=" + assignment.getWork(null)
				+ "/" + assignment.getActualWork(null) + "/" + assignment.getRemainingWork());
	}

	private List<String> assignmentScheduleSemantics(Project project) {
		List<String> result = new java.util.ArrayList<>();
		for (Task task : project.getTaskList()) {
			if (!(task instanceof NormalTask normalTask))
				continue;
			for (Object value : normalTask.getAssignments()) {
				Assignment assignment = (Assignment) value;
				if (assignment.isDefault())
					continue;
				result.add(task.getName() + "|" + assignment.getResource().getName() + "|"
					+ assignment.getStart() + "|" + assignment.getEnd() + "|"
					+ assignment.getWork(null) + "|" + assignment.getActualWork(null) + "|"
					+ assignment.getRemainingWork() + "|" + assignment.getUnits());
			}
		}
		result.sort(String::compareTo);
		return result;
	}

	private void assertMpp9ContourManifest(Project project) {
		Assignment assignment = (Assignment) taskNamed(project, "Task Sixteen").getAssignments().iterator().next();
		assertEquals(com.microproject.pm.assignment.contour.ContourTypes.Kind.CONTOURED.code(),
			assignment.getWorkContourType(), "the MPP fixture's manually shaped work must stay a custom contour");
		assertEquals(55L * 60L * 60L * 1000L, assignment.getWork(null));
		assertEquals(0L, assignment.getActualWork(null));
		assertEquals(55L * 60L * 60L * 1000L, assignment.getRemainingWork());

		String[] workdays = {
			"2011-02-07", "2011-02-08", "2011-02-09", "2011-02-10", "2011-02-11",
			"2011-02-14", "2011-02-15", "2011-02-16", "2011-02-17", "2011-02-18"
		};
		for (int index = 0; index < workdays.length; index++) {
			com.microproject.field.FieldContext interval = new com.microproject.field.FieldContext();
			interval.setInterval(new com.microproject.pm.time.ImmutableInterval(
				modelWallTime(workdays[index] + "T08:00:00"), modelWallTime(workdays[index] + "T17:00:00")));
			assertEquals((index + 1L) * 60L * 60L * 1000L, assignment.getWork(interval),
				"upstream MPP9 manifest workday " + workdays[index]);
		}
	}

	@Test
	void exportsMultiDayCustomContourAsDailyWorkAmounts() throws Exception {
		String xml = twoDayCustomContourXml("",
			"<TimephasedData><Type>1</Type><UID>1</UID><Start>2026-10-12T08:00:00</Start><Finish>2026-10-12T17:00:00</Finish><Unit>2</Unit><Value>PT4H0M0S</Value></TimephasedData>"
				+ "<TimephasedData><Type>1</Type><UID>2</UID><Start>2026-10-13T08:00:00</Start><Finish>2026-10-13T17:00:00</Finish><Unit>2</Unit><Value>PT4H0M0S</Value></TimephasedData>");
		Project imported = importMspdi(xml);
		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(imported, mpo));
		byte[] contentXml = nativeProjectXml(mpo.toByteArray());
		String savedXml = new String(contentXml, java.nio.charset.StandardCharsets.UTF_8);
		assertEquals(4, savedXml.split("<TimephasedData>", -1).length - 1, savedXml);
		assertEquals(4, savedXml.split("<Unit>2</Unit>", -1).length - 1, savedXml);
		assertEquals(2, savedXml.split("<Value>PT4H0M0S</Value>", -1).length - 1, savedXml);
		ProjectFile serialized = new net.sf.mpxj.mspdi.MSPDIReader().read(new ByteArrayInputStream(contentXml));
		var work = serialized.getResourceAssignments().get(0).getTimephasedWork();
		assertEquals(1, work.size(), work.toString()); // MPXJ merges adjacent daily entries with equal rates.
		assertEquals(8.0d, work.get(0).getTotalAmount().convertUnits(net.sf.mpxj.TimeUnit.HOURS,
			serialized.getProjectProperties()).getDuration(), 0.00001d, work.toString());
		assertEquals(4.0d, work.get(0).getAmountPerDay().convertUnits(net.sf.mpxj.TimeUnit.HOURS,
			serialized.getProjectProperties()).getDuration(), 0.00001d, work.toString());
		assertEquals(modelWallTime("2026-10-12T08:00:00"), DateUTCConverter.toModelTime(work.get(0).getStart()));
		assertEquals(modelWallTime("2026-10-13T17:00:00"), DateUTCConverter.toModelTime(work.get(0).getFinish()));
	}

	@Test
	void exportsFractionalMultiDayWorkWithoutLosingTheIntegerTotal() throws Exception {
		Project project = createProject();
		long start = modelWallTime("2026-10-12T08:00:00");
		long end = modelWallTime("2026-10-13T17:00:00");
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName("Fractional multi-day contour");
		project.setStartDate(start);
		task.getCurrentSchedule().setStart(start);
		task.setScheduleConstraint(ConstraintType.Kind.SNET, start);
		task.setDuration(16L * 60L * 60L * 1000L);
		Assignment assignment = AssignmentService.getInstance().newAssignment(task,
			project.getResourcePool().newResourceInstance(), 1.0d, 0L, this);
		long requestedWork = 8L * 60L * 60L * 1000L + 1L;
		assignment.setInterval(Assignment.WORK, start, end, requestedWork);
		project.recalculate();
		long scheduledWork = assignment.getWork(null);
		assertEquals(requestedWork, scheduledWork);

		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName("fractional-multi-day-contour.xml");
		assertTrue(exporter.saveProject(project, xml));
		String exportedXml = xml.toString(java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(exportedXml.contains("<Value>PT4H0M0.001S</Value>"), exportedXml);
		assertTrue(exportedXml.contains("<Value>PT4H0M0S</Value>"), exportedXml);

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(project, mpo));
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		Assignment restored = (Assignment) taskNamed(reopened, "Fractional multi-day contour")
			.getAssignments().iterator().next();
		assertEquals(scheduledWork, restored.getWork(null), "MPO reload must conserve scheduled assignment work");

		List<net.sf.mpxj.TimephasedWork> allocated = new java.util.ArrayList<>();
		MPXConverter.appendDailyTimephasedWork(allocated, start, end, requestedWork,
			project.getEffectiveWorkCalendar());
		assertEquals(2, allocated.size());
		long[] dailyMillis = allocated.stream().mapToLong(work -> Math.round(work.getTotalAmount()
			.convertUnits(net.sf.mpxj.TimeUnit.HOURS, null).getDuration() * 60L * 60L * 1000L)).toArray();
		assertArrayEquals(new long[] {14_400_001L, 14_400_000L}, dailyMillis);
		assertEquals(requestedWork, java.util.Arrays.stream(dailyMillis).sum());
	}

	@Test
	void exportsPositiveSpanZeroWorkPersonalContourAndReloadsIt() throws Exception {
		String sourceXml = zeroWorkPersonalContourXml();
		ProjectFile parsedSource = new net.sf.mpxj.mspdi.MSPDIReader().read(
			new ByteArrayInputStream(sourceXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		net.sf.mpxj.Task sourceTask = parsedSource.getTasks().stream()
			.filter(candidate -> "Zero-work personal contour".equals(candidate.getName())).findFirst().orElseThrow();
		ResourceAssignment sourceAssignment = parsedSource.getResourceAssignments().get(0);
		assertEquals(modelWallTime("2026-10-12T08:00:00"), DateUTCConverter.toModelTime(sourceTask.getStart()));
		assertEquals(modelWallTime("2026-10-12T17:00:00"), DateUTCConverter.toModelTime(sourceTask.getFinish()));
		assertEquals(8.0d, sourceTask.getDuration().convertUnits(net.sf.mpxj.TimeUnit.HOURS,
			parsedSource.getProjectProperties()).getDuration(), 0.00001d);
		assertEquals(modelWallTime("2026-10-12T08:00:00"), DateUTCConverter.toModelTime(sourceAssignment.getStart()));
		assertEquals(modelWallTime("2026-10-12T17:00:00"), DateUTCConverter.toModelTime(sourceAssignment.getFinish()));
		assertEquals(0.0d, sourceAssignment.getWork().convertUnits(net.sf.mpxj.TimeUnit.HOURS,
			parsedSource.getProjectProperties()).getDuration(), 0.00001d);
		Project project = importMspdi(sourceXml);
		NormalTask task = taskNamed(project, "Zero-work personal contour");
		Assignment assignment = (Assignment) task.getAssignments().iterator().next();
		assertTrue(assignment.getWorkContour().isPersonal());
		assertFalse(assignment.isDefault(), "zero work does not make a real resource unassigned");
		assertEquals(1L, assignment.getResource().getUniqueId());
		assertEquals(0L, assignment.getWork(null));
		assertTrue(assignment.getEnd() > assignment.getStart(), "zero work must retain its positive assignment span: "
			+ assignment.getStart() + ".." + assignment.getEnd() + ", task=" + task.getStart() + ".." + task.getEnd());

		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName("zero-work-contour.xml");
		assertTrue(exporter.saveProject(project, xml));
		String savedXml = xml.toString(java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(savedXml.contains("<Type>1</Type>"), "zero contour requires a remaining-work timephased row");
		assertTrue(savedXml.contains("<Unit>2</Unit>"), savedXml);
		assertTrue(savedXml.contains("<Value>PT0H0M0S</Value>"), savedXml);
		ResourceAssignment xmlAssignment = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(xml.toByteArray())).getResourceAssignments().get(0);
		assertEquals(modelWallTime("2026-10-12T08:00:00"), DateUTCConverter.toModelTime(xmlAssignment.getStart()));
		assertEquals(modelWallTime("2026-10-12T17:00:00"), DateUTCConverter.toModelTime(xmlAssignment.getFinish()));
		assertEquals(0.0d, xmlAssignment.getTimephasedWork().get(0).getTotalAmount()
			.convertUnits(net.sf.mpxj.TimeUnit.HOURS, xmlAssignment.getParentFile().getProjectProperties())
			.getDuration(), 0.00001d);

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(project, mpo));
		String contentXml = new String(nativeProjectXml(mpo.toByteArray()), java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(contentXml.contains("<Type>1</Type>"), "zero contour requires a remaining-work timephased row");
		assertTrue(contentXml.contains("<Unit>2</Unit>"), contentXml);
		assertTrue(contentXml.contains("<Value>PT0H0M0S</Value>"), contentXml);

		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		Assignment restored = (Assignment) taskNamed(reopened, "Zero-work personal contour")
			.getAssignments().iterator().next();
		assertTrue(restored.getWorkContour().isPersonal());
		assertFalse(restored.isDefault(), "real UID 1 must remain assigned after MPO reload");
		assertEquals(1L, restored.getResource().getUniqueId());
		assertEquals(0L, restored.getWork(null));
		assertTrue(restored.getEnd() > restored.getStart());
	}

	private String zeroWorkPersonalContourXml() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "Assignment Baseline Scalars.xml");
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "Assignment Baseline Scalars.xml");
		String xml = Files.readString(fixture);
		int taskStart = xml.indexOf("<Task>");
		int taskEnd = xml.indexOf("</Task>", taskStart);
		String taskXml = xml.substring(taskStart, taskEnd);
		taskXml = taskXml.replace("<Name>Assignment baseline task</Name>", "<Name>Zero-work personal contour</Name>")
			.replace("<Type>0</Type>", "<Type>1</Type>")
			.replace("<Work>PT8H0M0S</Work>", "<Work>PT0H0M0S</Work>");
		xml = xml.substring(0, taskStart) + taskXml + xml.substring(taskEnd);
		int assignmentStart = xml.indexOf("<Assignment>");
		int assignmentEnd = xml.indexOf("</Assignment>", assignmentStart);
		String assignmentXml = xml.substring(assignmentStart, assignmentEnd);
		assignmentXml = assignmentXml.replace("<Work>PT8H0M0S</Work>",
			"<ActualWork>PT0H0M0S</ActualWork><RemainingWork>PT0H0M0S</RemainingWork>"
				+ "<PercentWorkComplete>0</PercentWorkComplete><Work>PT0H0M0S</Work>")
			.replace("<Units>1</Units>", "<Units>1</Units><WorkContour>8</WorkContour>"
				+ "<TimephasedData><Type>2</Type><UID>1</UID><Start>2026-10-12T08:00:00</Start>"
				+ "<Finish>2026-10-12T17:00:00</Finish><Unit>2</Unit><Value>PT0H0M0S</Value></TimephasedData>"
				+ "<TimephasedData><Type>1</Type><UID>2</UID><Start>2026-10-12T08:00:00</Start>"
				+ "<Finish>2026-10-12T17:00:00</Finish><Unit>2</Unit><Value>PT0H0M0S</Value></TimephasedData>");
		return xml.substring(0, assignmentStart) + assignmentXml + xml.substring(assignmentEnd);
	}

	@Test
	void customContourPreservesCalendarGapsAndRejectsIncompleteOrNonPrefixProgress() throws Exception {
		String progress = "<ActualWork>PT2H0M0S</ActualWork><RemainingWork>PT6H0M0S</RemainingWork>"
			+ "<PercentWorkComplete>25</PercentWorkComplete>";
		String gapRows = "<TimephasedData><Type>2</Type><UID>1</UID><Start>2026-10-12T08:00:00</Start><Finish>2026-10-12T12:00:00</Finish><Unit>2</Unit><Value>PT2H0M0S</Value></TimephasedData>"
			+ "<TimephasedData><Type>1</Type><UID>2</UID><Start>2026-10-13T08:00:00</Start><Finish>2026-10-13T17:00:00</Finish><Unit>2</Unit><Value>PT6H0M0S</Value></TimephasedData>";
		Project imported = importMspdi(twoDayCustomContourXml(progress, gapRows));
		Assignment assignment = (Assignment) taskNamed(imported, "Assignment baseline task").getAssignments().iterator().next();
		assertEquals(2L * 60L * 60L * 1000L,
			assignment.work(modelWallTime("2026-10-12T08:00:00"), modelWallTime("2026-10-12T12:00:00")));
		assertEquals(0L, assignment.work(modelWallTime("2026-10-12T13:00:00"), modelWallTime("2026-10-12T17:00:00")),
			"The source gap must remain zero-work");
		assertEquals(6L * 60L * 60L * 1000L,
			assignment.work(modelWallTime("2026-10-13T08:00:00"), modelWallTime("2026-10-13T17:00:00")));
		assertEquals(2L * 60L * 60L * 1000L,
			assignment.actualWork(modelWallTime("2026-10-12T08:00:00"), modelWallTime("2026-10-12T12:00:00")));

		assertThrows(IllegalArgumentException.class,
			() -> importMspdi(twoDayCustomContourXml(progress, "")), "Custom contours need complete timephased source data");
		String nonPrefixRows = "<TimephasedData><Type>1</Type><UID>1</UID><Start>2026-10-12T08:00:00</Start><Finish>2026-10-12T12:00:00</Finish><Unit>2</Unit><Value>PT2H0M0S</Value></TimephasedData>"
			+ "<TimephasedData><Type>2</Type><UID>2</UID><Start>2026-10-12T13:00:00</Start><Finish>2026-10-12T17:00:00</Finish><Unit>2</Unit><Value>PT2H0M0S</Value></TimephasedData>"
			+ "<TimephasedData><Type>1</Type><UID>3</UID><Start>2026-10-13T08:00:00</Start><Finish>2026-10-13T17:00:00</Finish><Unit>2</Unit><Value>PT4H0M0S</Value></TimephasedData>";
		Project nonPrefix = importMspdi(twoDayCustomContourXml(progress, nonPrefixRows));
		Assignment nonPrefixAssignment = (Assignment) taskNamed(nonPrefix, "Assignment baseline task").getAssignments().iterator().next();
		long firstMorning = modelWallTime("2026-10-12T08:00:00");
		long firstLunch = modelWallTime("2026-10-12T12:00:00");
		long firstAfternoon = modelWallTime("2026-10-12T13:00:00");
		long firstFinish = modelWallTime("2026-10-12T17:00:00");
		assertEquals(8L * 60L * 60L * 1000L, nonPrefixAssignment.getWork(null));
		assertEquals(2L * 60L * 60L * 1000L, nonPrefixAssignment.getActualWork(null));
		assertEquals(6L * 60L * 60L * 1000L, nonPrefixAssignment.getRemainingWork());
		assertEquals(firstFinish, nonPrefixAssignment.getStop());
		assertEquals(0.5d, nonPrefixAssignment.getPercentComplete(), 0.00001d,
			"The model duration-complete marker follows the last source actual interval");
		assertEquals(0L, nonPrefixAssignment.actualWork(firstMorning, firstLunch));
		assertEquals(2L * 60L * 60L * 1000L, nonPrefixAssignment.actualWork(firstAfternoon, firstFinish));
		assertEquals(2L * 60L * 60L * 1000L, nonPrefixAssignment.remainingWork(firstMorning, firstLunch));
		assertEquals(0L, nonPrefixAssignment.remainingWork(firstAfternoon, firstFinish));
		Assignment cloned = new Assignment((com.microproject.pm.assignment.AssignmentDetail) nonPrefixAssignment.getDetail().clone());
		assertTrue(cloned.hasImportedTimephasedWork());
		assertEquals(2L * 60L * 60L * 1000L, cloned.actualWork(firstAfternoon, firstFinish));
		long nextDayStart = nonPrefixAssignment.getEffectiveWorkCalendar().add(nonPrefixAssignment.getStart(),
			8L * 60L * 60L * 1000L, false);
		cloned.setStart(nextDayStart);
		assertTrue(cloned.hasImportedTimephasedWork(), "Moving an assignment preserves its relative timephased history");
		assertEquals(2L * 60L * 60L * 1000L,
			cloned.actualWork(modelWallTime("2026-10-13T13:00:00"), modelWallTime("2026-10-13T17:00:00")));
		Assignment extended = new Assignment((com.microproject.pm.assignment.AssignmentDetail) nonPrefixAssignment.getDetail().clone());
		long extendedFinish = extended.getEffectiveWorkCalendar().add(extended.getEnd(), 8L * 60L * 60L * 1000L, true);
		extended.setEnd(extendedFinish);
		assertTrue(extended.hasImportedTimephasedWork(), "Extending the assignment preserves imported work history");
		assertEquals(2L * 60L * 60L * 1000L, extended.getActualWork(null));
		assertEquals(6L * 60L * 60L * 1000L, extended.getRemainingWork());
		Assignment editedRemaining = new Assignment((com.microproject.pm.assignment.AssignmentDetail) nonPrefixAssignment.getDetail().clone());
		editedRemaining.setRemainingWork(4L * 60L * 60L * 1000L + 30L * 60L * 1000L, null);
		assertTrue(editedRemaining.hasImportedTimephasedWork(), "Changing remaining work preserves imported interval profiles");
		assertEquals(2L * 60L * 60L * 1000L, editedRemaining.getActualWork(null));
		assertEquals(4L * 60L * 60L * 1000L + 30L * 60L * 1000L, editedRemaining.getRemainingWork(),
			"work=" + editedRemaining.getWork(null) + ", actual=" + editedRemaining.getActualWork(null)
				+ ", duration=" + editedRemaining.getDuration() + ", actualDuration=" + editedRemaining.getActualDuration()
				+ ", units=" + editedRemaining.getUnits() + ", contour="
				+ editedRemaining.getWorkContour().toString(editedRemaining.getDuration()));
		assertEquals(6L * 60L * 60L * 1000L + 30L * 60L * 1000L, editedRemaining.getWork(null));
		assertEquals(2L * 60L * 60L * 1000L, editedRemaining.actualWork(firstAfternoon, firstFinish));
		assertEquals(90L * 60L * 1000L, editedRemaining.remainingWork(firstMorning, firstLunch));
		long secondDayStart = modelWallTime("2026-10-13T08:00:00");
		long secondDayEditEnd = editedRemaining.getEffectiveWorkCalendar().add(editedRemaining.getStart(),
			13L * 60L * 60L * 1000L, false);
		assertEquals(3L * 60L * 60L * 1000L, editedRemaining.remainingWork(secondDayStart, secondDayEditEnd));
		Assignment editedUnits = new Assignment((com.microproject.pm.assignment.AssignmentDetail) nonPrefixAssignment.getDetail().clone());
		editedUnits.forceUnits(0.5d);
		assertTrue(editedUnits.hasImportedTimephasedWork(), "A units edit retains imported interval profiles");
		assertEquals(0L, editedUnits.actualWork(firstMorning, firstLunch));
		assertEquals(2L * 60L * 60L * 1000L, editedUnits.actualWork(firstAfternoon, firstFinish));
		Object undoDetail = nonPrefixAssignment.backupDetail();
		nonPrefixAssignment.setPercentComplete(0.25d);
		assertFalse(nonPrefixAssignment.hasImportedTimephasedWork(), "An explicit scalar progress edit returns to the canonical prefix model");
		nonPrefixAssignment.restoreDetail(undoDetail);
		assertTrue(nonPrefixAssignment.hasImportedTimephasedWork(), "Undo restores the imported profiles with the assignment detail");
		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(nonPrefix, mpo));
		Project reopened = new com.microproject.exchange.MpoFileImporter().loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		Assignment restored = (Assignment) taskNamed(reopened, "Assignment baseline task").getAssignments().iterator().next();
		assertEquals(8L * 60L * 60L * 1000L, restored.getWork(null));
		assertEquals(2L * 60L * 60L * 1000L, restored.getActualWork(null));
		assertEquals(6L * 60L * 60L * 1000L, restored.getRemainingWork());
		assertEquals(firstFinish, restored.getStop());
		assertEquals(0L, restored.actualWork(firstMorning, firstLunch));
		assertEquals(2L * 60L * 60L * 1000L, restored.actualWork(firstAfternoon, firstFinish));
		assertEquals(2L * 60L * 60L * 1000L, restored.remainingWork(firstMorning, firstLunch));
		assertEquals(0L, restored.remainingWork(firstAfternoon, firstFinish));
	}

	@Test
	void customContourImportCombinesActualAndRemainingIntervalsOnDifferentBounds() throws Exception {
		String progress = "<ActualWork>PT2H0M0S</ActualWork><RemainingWork>PT6H0M0S</RemainingWork>"
			+ "<PercentWorkComplete>25</PercentWorkComplete>";
		String rows = "<TimephasedData><Type>2</Type><UID>1</UID><Start>2026-10-12T08:00:00</Start><Finish>2026-10-12T12:00:00</Finish><Unit>2</Unit><Value>PT2H0M0S</Value></TimephasedData>"
			+ "<TimephasedData><Type>1</Type><UID>2</UID><Start>2026-10-12T12:00:00</Start><Finish>2026-10-12T17:00:00</Finish><Unit>2</Unit><Value>PT2H0M0S</Value></TimephasedData>"
			+ "<TimephasedData><Type>1</Type><UID>3</UID><Start>2026-10-13T08:00:00</Start><Finish>2026-10-13T17:00:00</Finish><Unit>2</Unit><Value>PT4H0M0S</Value></TimephasedData>";
		String xml = twoDayCustomContourXml(progress, rows);
		Project imported = importMspdi(xml);
		Assignment assignment = (Assignment) taskNamed(imported, "Assignment baseline task").getAssignments().iterator().next();
		assertEquals(8L * 60L * 60L * 1000L, assignment.getWork(null));
		assertEquals(2L * 60L * 60L * 1000L, assignment.getActualWork(null));
		assertEquals(6L * 60L * 60L * 1000L, assignment.getRemainingWork());
		long sourceActualBoundary = modelWallTime("2026-10-12T12:00:00");
		assertEquals(assignment.getEffectiveWorkCalendar().compare(sourceActualBoundary, assignment.getStart(), false),
			assignment.getEffectiveWorkCalendar().compare(assignment.getStop(), assignment.getStart(), false),
			"Stop preserves the source actual-work boundary's working-time coordinate across lunch");
		assertEquals(modelWallTime("2026-10-12T13:00:00"), assignment.getStop(),
			"The model represents the post-lunch boundary after the source's 08:00–12:00 work interval");
		assertEquals(0L, assignment.getEffectiveWorkCalendar().compare(assignment.getStop(), sourceActualBoundary, false),
			"The normalized stop adds no work during the lunch gap");
		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(imported, mpo));
		byte[] contentXml = nativeProjectXml(mpo.toByteArray());
		String savedXml = new String(contentXml, java.nio.charset.StandardCharsets.UTF_8);
		assertEquals(6, savedXml.split("<TimephasedData>", -1).length - 1, savedXml);
		assertEquals(6, savedXml.split("<Unit>2</Unit>", -1).length - 1, savedXml);
		ProjectFile serialized = new net.sf.mpxj.mspdi.MSPDIReader().read(new ByteArrayInputStream(contentXml));
		ResourceAssignment serializedAssignment = serialized.getResourceAssignments().get(0);
		assertEquals(2.0d, timephasedHours(serializedAssignment.getTimephasedActualWork(), serialized), 0.00001d);
		assertEquals(6.0d, timephasedHours(serializedAssignment.getTimephasedWork(), serialized), 0.00001d);
		var actualRows = serializedAssignment.getTimephasedActualWork();
		var remainingRows = serializedAssignment.getTimephasedWork();
		var actualWorkRows = actualRows.stream()
			.filter(row -> timephasedHours(java.util.List.of(row), serialized) > 0.0d).toList();
		var remainingWorkRows = remainingRows.stream()
			.filter(row -> timephasedHours(java.util.List.of(row), serialized) > 0.0d).toList();
		assertEquals(2, actualRows.size(), actualRows.toString());
		assertEquals(1, actualWorkRows.size(), actualRows.toString());
		assertEquals(modelWallTime("2026-10-12T08:00:00"), DateUTCConverter.toModelTime(actualWorkRows.get(0).getStart()));
		assertEquals(modelWallTime("2026-10-12T13:00:00"), DateUTCConverter.toModelTime(actualWorkRows.get(0).getFinish()));
		assertEquals(2, remainingRows.size(), remainingRows.toString());
		assertEquals(2, remainingWorkRows.size(), remainingRows.toString());
		assertEquals(modelWallTime("2026-10-12T12:00:00"), DateUTCConverter.toModelTime(remainingWorkRows.get(0).getStart()),
			remainingRows.toString());
		assertEquals(0L, assignment.getEffectiveWorkCalendar().compare(
			DateUTCConverter.toModelTime(actualWorkRows.get(0).getFinish()),
			DateUTCConverter.toModelTime(remainingWorkRows.get(0).getStart()), false),
			"Actual and remaining rows only overlap across the nonworking lunch boundary");
		assertEquals(modelWallTime("2026-10-12T17:00:00"), DateUTCConverter.toModelTime(remainingWorkRows.get(0).getFinish()));
		assertEquals(modelWallTime("2026-10-13T08:00:00"), DateUTCConverter.toModelTime(remainingWorkRows.get(1).getStart()));
		assertEquals(modelWallTime("2026-10-13T17:00:00"), DateUTCConverter.toModelTime(remainingWorkRows.get(1).getFinish()));
		Project reopened = new com.microproject.exchange.MpoFileImporter().loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		Assignment restored = (Assignment) taskNamed(reopened, "Assignment baseline task").getAssignments().iterator().next();
		assertEquals(8L * 60L * 60L * 1000L, restored.getWork(null));
		assertEquals(2L * 60L * 60L * 1000L, restored.getActualWork(null));
		assertEquals(6L * 60L * 60L * 1000L, restored.getRemainingWork());
		assertEquals(assignment.getEffectiveWorkCalendar().compare(assignment.getStop(), assignment.getStart(), false),
			restored.getEffectiveWorkCalendar().compare(restored.getStop(), restored.getStart(), false));
		Object originalDetail = restored.backupDetail();
		restored.setRemainingWork(4L * 60L * 60L * 1000L + 30L * 60L * 1000L, null);
		assertEquals(2L * 60L * 60L * 1000L, restored.getActualWork(null));
		assertEquals(4L * 60L * 60L * 1000L + 30L * 60L * 1000L, restored.getRemainingWork());
		assertEquals(6L * 60L * 60L * 1000L + 30L * 60L * 1000L, restored.getWork(null));
		ByteArrayOutputStream editedMpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(reopened, editedMpo));
		Project editedReopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(editedMpo.toByteArray()));
		Assignment editedRestored = (Assignment) taskNamed(editedReopened, "Assignment baseline task").getAssignments().iterator().next();
		assertEquals(2L * 60L * 60L * 1000L, editedRestored.getActualWork(null));
		assertEquals(4L * 60L * 60L * 1000L + 30L * 60L * 1000L, editedRestored.getRemainingWork());
		assertEquals(6L * 60L * 60L * 1000L + 30L * 60L * 1000L, editedRestored.getWork(null));
		restored.restoreDetail(originalDetail);
		assertEquals(2L * 60L * 60L * 1000L, restored.getActualWork(null), "Undo restores the absolute actual-work scalar");
		assertEquals(6L * 60L * 60L * 1000L, restored.getRemainingWork(), "Undo restores remaining timephased work");
	}

	private double timephasedHours(java.util.List<net.sf.mpxj.TimephasedWork> intervals, ProjectFile project) {
		return intervals.stream().mapToDouble(interval -> interval.getTotalAmount()
			.convertUnits(net.sf.mpxj.TimeUnit.HOURS, project.getProjectProperties()).getDuration()).sum();
	}

	private String twoDayCustomContourXml(String progressFields, String timephasedRows) throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "Assignment Baseline Scalars.xml");
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "Assignment Baseline Scalars.xml");
		String xml = Files.readString(fixture)
			.replace("2026-10-12T17:00:00", "2026-10-13T17:00:00")
			.replace("<Duration>PT8H0M0S</Duration><DurationFormat>7", "<Duration>PT16H0M0S</Duration><DurationFormat>7");
		int assignmentStart = xml.indexOf("<Assignment>");
		int assignmentEnd = xml.indexOf("</Assignment>", assignmentStart);
		String assignmentXml = xml.substring(assignmentStart, assignmentEnd);
		assignmentXml = assignmentXml.replace("<Work>PT8H0M0S</Work>", progressFields + "<Work>PT8H0M0S</Work>");
		assignmentXml = assignmentXml.replace("<Units>1</Units>", "<Units>1</Units><WorkContour>8</WorkContour>" + timephasedRows);
		return xml.substring(0, assignmentStart) + assignmentXml + xml.substring(assignmentEnd);
	}

	private Project importMspdi(String xml) throws Exception {
		return new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)), "xml", (progress, label) -> {});
	}

	private String withAssignmentContour(String sourceXml, int contourCode, long work) {
		String duration = mspdiDuration(work);
		int taskStart = sourceXml.indexOf("<Task>");
		int taskEnd = sourceXml.indexOf("</Task>", taskStart);
		String taskXml = sourceXml.substring(taskStart, taskEnd);
		taskXml = taskXml.replace("<Work>PT8H0M0S</Work>", "<Work>" + duration + "</Work>");
		String result = sourceXml.substring(0, taskStart) + taskXml + sourceXml.substring(taskEnd);
		int assignmentsStart = result.indexOf("<Assignment>");
		int assignmentEnd = result.indexOf("</Assignment>", assignmentsStart);
		String assignmentXml = result.substring(assignmentsStart, assignmentEnd);
		assignmentXml = assignmentXml.replaceFirst("<Work>PT8H0M0S</Work>", "<Work>" + duration + "</Work>");
		assignmentXml = assignmentXml.replace("<RemainingWork>PT8H0M0S</RemainingWork>",
			"<RemainingWork>" + duration + "</RemainingWork>");
		assignmentXml = assignmentXml.replace("<Units>1</Units>",
			"<Units>1</Units><WorkContour>" + contourCode + "</WorkContour>");
		return result.substring(0, assignmentsStart) + assignmentXml + result.substring(assignmentEnd);
	}

	private String withAssignmentProgress(String sourceXml, long work, long actualWork, int percent) {
		int assignmentStart = sourceXml.indexOf("<Assignment>");
		int assignmentEnd = sourceXml.indexOf("</Assignment>", assignmentStart);
		String assignmentXml = sourceXml.substring(assignmentStart, assignmentEnd);
		assignmentXml = assignmentXml.replace("<Work>PT8H0M0S</Work>",
			"<ActualWork>" + mspdiDuration(actualWork) + "</ActualWork><RemainingWork>"
				+ mspdiDuration(work - actualWork) + "</RemainingWork><PercentWorkComplete>" + percent
				+ "</PercentWorkComplete><Work>" + mspdiDuration(work) + "</Work>");
		return sourceXml.substring(0, assignmentStart) + assignmentXml + sourceXml.substring(assignmentEnd);
	}

	private String mspdiDuration(long work) {
		return "PT" + (work / (60L * 60L * 1000L)) + "H" + ((work / (60L * 1000L)) % 60L) + "M0S";
	}

	private byte[] nativeProjectXml(byte[] mpo) throws Exception {
		try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(new ByteArrayInputStream(mpo))) {
			java.util.zip.ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.getName().endsWith("content.xml"))
					return zip.readAllBytes();
			}
		}
		throw new AssertionError("MPO content.xml missing");
	}

	@Test
	void mspdiElapsedBaselineDurationUsesDeclaredFormatAfterMpxjRoundTrip() throws Exception {
		ProjectFile source = new ProjectFile();
		source.addDefaultBaseCalendar();
		source.getProjectProperties().setMinutesPerDay(Integer.valueOf(480));
		net.sf.mpxj.Task task = source.addTask();
		task.setName("Elapsed baseline limitation");
		task.setUniqueID(1);
		task.setBaselineDuration(3,
			net.sf.mpxj.Duration.getInstance(2, net.sf.mpxj.TimeUnit.ELAPSED_DAYS));
		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		new net.sf.mpxj.mspdi.MSPDIWriter().write(source, xml);

		Project imported = new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(xml.toByteArray()), "xml", (progress, label) -> {});
		long baselineDuration = taskNamed(imported, "Elapsed baseline limitation").getBaselineDuration(3);
		assertBaselineDuration(baselineDuration, 2.0d, com.microproject.datatype.TimeUnit.ELAPSED_DAYS);
	}

	@Test
	void standardAssignmentImportPreservesConsistentBoundsAndProgress() throws Exception {
		ProjectFile source = new ProjectFile();
		source.addDefaultBaseCalendar();
		java.util.Date start = localWallDate("2026-10-12T08:00:00");
		java.util.Date finish = localWallDate("2026-10-16T10:00:00");
		source.getProjectProperties().setStartDate(start);
		net.sf.mpxj.Task task = source.addTask();
		task.setName("Standard assignment with actual work");
		task.setUniqueID(1);
		task.setStart(start);
		task.setFinish(finish);
		task.setDuration(net.sf.mpxj.Duration.getInstance(34, net.sf.mpxj.TimeUnit.HOURS));
		task.setType(net.sf.mpxj.TaskType.FIXED_DURATION);
		net.sf.mpxj.Resource resource = source.addResource();
		resource.setName("Worker");
		resource.setUniqueID(1);
		ResourceAssignment assignment = task.addResourceAssignment(resource);
		assignment.setUnits(100d);
		assignment.setStart(start);
		assignment.setFinish(finish);
		assignment.setWork(net.sf.mpxj.Duration.getInstance(34, net.sf.mpxj.TimeUnit.HOURS));
		assignment.setActualWork(net.sf.mpxj.Duration.getInstance(32, net.sf.mpxj.TimeUnit.HOURS));
		assignment.setRemainingWork(net.sf.mpxj.Duration.getInstance(2, net.sf.mpxj.TimeUnit.HOURS));
		assignment.setPercentageWorkComplete(94);
		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		new net.sf.mpxj.mspdi.MSPDIWriter().write(source, xml);

		Project imported = new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(xml.toByteArray()), "xml", (progress, label) -> {});
		NormalTask importedTask = taskNamed(imported, "Standard assignment with actual work");
		Assignment importedAssignment = (Assignment) importedTask.getAssignments().iterator().next();
		assertEquals(0L, importedAssignment.getDelay());
		assertEquals(34L * 60L * 60L * 1000L, importedAssignment.getWork(null));
		assertEquals(32L * 60L * 60L * 1000L, importedAssignment.getActualWork(null));
		assertEquals(2L * 60L * 60L * 1000L, importedAssignment.getRemainingWork());
		assertEquals(1.0d, importedAssignment.getUnits(), 0.00001d);
		assertEquals(com.microproject.pm.scheduling.SchedulingType.Kind.FIXED_DURATION.code(),
			importedTask.getSchedulingType());
		assertEquals(modelWallTime("2026-10-12T08:00:00"), importedAssignment.getStart());
		assertEquals(modelWallTime("2026-10-16T10:00:00"), importedAssignment.getEnd());
	}

	@Test
	void standardZeroWorkAssignmentKeepsFixedDurationSourceBoundsAndZeroUnits() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "Assignment Baseline Scalars.xml");
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "Assignment Baseline Scalars.xml");
		String sourceXml = withAssignmentProgress(Files.readString(fixture), 0L, 0L, 0)
			.replaceFirst("<Type>0</Type>", "<Type>1</Type>")
			.replace("<Units>1</Units>", "<Units>0</Units>");
		Project imported = importMspdi(sourceXml);
		NormalTask task = taskNamed(imported, "Assignment baseline task");
		Assignment assignment = (Assignment) task.getAssignments().iterator().next();
		assertEquals(com.microproject.pm.scheduling.SchedulingType.Kind.FIXED_DURATION.code(), task.getSchedulingType());
		assertEquals(0L, assignment.getWork(null));
		assertEquals(0L, assignment.getActualWork(null));
		assertEquals(0L, assignment.getRemainingWork());
		assertEquals(0.0d, assignment.getUnits(), 0.00001d);
		assertEquals(modelWallTime("2026-10-12T08:00:00"), assignment.getStart());
		assertEquals(modelWallTime("2026-10-12T17:00:00"), assignment.getEnd());

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(imported, mpo));
		Project reopened = new com.microproject.exchange.MpoFileImporter()
			.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		NormalTask reopenedTask = taskNamed(reopened, "Assignment baseline task");
		Assignment reopenedAssignment = (Assignment) reopenedTask.getAssignments().iterator().next();
		assertEquals(0L, reopenedAssignment.getWork(null));
		assertEquals(0.0d, reopenedAssignment.getUnits(), 0.00001d);
		assertEquals(modelWallTime("2026-10-12T08:00:00"), reopenedAssignment.getStart());
		assertEquals(modelWallTime("2026-10-12T17:00:00"), reopenedAssignment.getEnd());
	}

	@Test
	void exportKeepsPersistedTaskUidWhenNewParentPrecedesIt() throws Exception {
		Project project = createProject();
		NormalTask child = (NormalTask) project.createLocalTaskNode(null).getImpl();
		child.setName("Persisted child");
		child.setUniqueId(2L);
		NormalTask parent = (NormalTask) project.createLocalTaskNode(null).getImpl();
		parent.setName("New parent");
		parent.setUniqueId(-42L);
		project.setLocalParent(child, parent);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName("identity.xml");
		assertTrue(exporter.saveProject(project, output));
		ProjectFile snapshot = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(output.toByteArray()));
		net.sf.mpxj.Task exportedChild = snapshot.getTasks().stream()
			.filter(task -> "Persisted child".equals(task.getName())).findFirst().orElseThrow();
		net.sf.mpxj.Task exportedParent = snapshot.getTasks().stream()
			.filter(task -> "New parent".equals(task.getName())).findFirst().orElseThrow();
		assertEquals(2, exportedChild.getUniqueID().intValue());
		assertNotEquals(exportedChild.getUniqueID(), exportedParent.getUniqueID());
		assertSame(exportedParent, exportedChild.getParentTask());
	}

	@Test
	void microsoftExportJobReportsCompletionForEmptyProject() throws Exception {
		JobQueue queue = new JobQueue("microsoft-export-progress", false);
		JobQueue previousQueue = SessionFactory.getInstance().getJobQueue();
		Path output = Files.createTempFile("microproject-empty-export-", ".xml");
		Files.deleteIfExists(output);

		try {
			SessionFactory.getInstance().setJobQueue(queue);
			MicrosoftImporter exporter = new MicrosoftImporter();
			exporter.setFileName(output.toString());
			exporter.setProject(createProject());
			Job job = exporter.getExportFileJob();
			CountDownLatch completed = new CountDownLatch(1);
			job.addCompletionRunnable(completed::countDown);
			job.execute();

			assertTrue(completed.await(15, TimeUnit.SECONDS), "empty-project export job did not complete");
			assertEquals(1.0f, job.getProgress(), 0.00001f);
			assertTrue(Files.size(output) > 0L, "empty-project export did not create a file");
		} finally {
			SessionFactory.getInstance().setJobQueue(previousQueue);
			Files.deleteIfExists(output);
		}
	}

	@Test
	void taskTrackingModesAndActualsAreExported() {
		NormalTask source = createTask();
		source.setManuallyScheduled(true);
		source.setPercentWorkComplete(0.40d);
		source.setPhysicalPercentComplete(0.30d);
		source.setInactiveTask(true);

		net.sf.mpxj.Task target = new ProjectFile().addTask();
		MPXConverter.toMPXTask(source, target);

		assertEquals(0.0d, target.getPercentageComplete().doubleValue(), 0.00001d);
		assertEquals(40.0d, target.getPercentageWorkComplete().doubleValue(), 0.00001d);
		assertEquals(30.0d, target.getPhysicalPercentComplete().doubleValue(), 0.00001d);
		assertNotNull(target.getActualStart());
		assertNotNull(target.getActualDuration());
		assertEquals(TaskMode.MANUALLY_SCHEDULED, target.getTaskMode());
		assertFalse(target.getActive());
	}

	@Test
	void assignmentTrackingValuesAreExported() {
		NormalTask task = createTask();
		task.getProject().setDurationDefaults(new com.microproject.datatype.DurationDefaults(
			360.0d, 1800.0d, 20.0d, com.microproject.datatype.TimeUnit.HOURS,
			com.microproject.datatype.TimeUnit.MINUTES));
		task.setPercentWorkComplete(0.50d);
		Iterator<?> assignments = task.getAssignments().iterator();
		Assignment source = (Assignment) assignments.next();
		source.setDelay(3L * 60L * 60L * 1000L);
		source.setLevelingDelay(2L * 60L * 60L * 1000L);
		com.microproject.options.CalendarOption globalCalendarOptions =
			com.microproject.options.CalendarOption.getInstance();
		ProjectFile file = new ProjectFile();
		net.sf.mpxj.Task targetTask = file.addTask();
		ResourceAssignment target = targetTask.addResourceAssignment(file.addResource());

		MPXConverter.toMPXAssignment(source, target);

		assertEquals(50.0d, target.getPercentageWorkComplete().doubleValue(), 0.00001d);
		assertNotNull(target.getActualStart());
		assertEquals(120.0d, target.getLevelingDelay().getDuration(), 0.00001d);
		assertEquals(net.sf.mpxj.TimeUnit.MINUTES, target.getLevelingDelay().getUnits());
		assertEquals(3.0d, target.getDelay().getDuration(), 0.00001d);
		assertEquals(net.sf.mpxj.TimeUnit.HOURS, target.getDelay().getUnits());
		assertSame(globalCalendarOptions, com.microproject.options.CalendarOption.getInstance());
	}

	@Test
	void levelingDelayRoundTripsInMspdiMinutes() throws Exception {
		NormalTask source = createTask();
		long delay = 3L * 60L * 60L * 1000L;
		source.setLevelingDelay(delay);

		net.sf.mpxj.Task target = new ProjectFile().addTask();
		MPXConverter.toMPXTask(source, target);
		assertEquals(180.0d, target.getLevelingDelay().getDuration(), 0.00001d);
		assertEquals(net.sf.mpxj.TimeUnit.MINUTES, target.getLevelingDelay().getUnits());

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName("leveling-delay.xml");
		assertTrue(exporter.saveProject(source.getProject(), output));
		net.sf.mpxj.ProjectFile exported = new net.sf.mpxj.mspdi.MSPDIReader()
				.read(new ByteArrayInputStream(output.toByteArray()));
		net.sf.mpxj.Task exportedTask = exported.getTasks().stream()
				.filter(task -> task.getID() != null && task.getID().longValue() == source.getId())
				.findFirst().orElseThrow();
		assertEquals(180.0d, exportedTask.getLevelingDelay().getDuration(), 0.00001d);

		MicrosoftImporter importer = new MicrosoftImporter();
		importer.setFileName("leveling-delay.xml");
		importer.setProjectFactory(com.microproject.pm.task.ProjectFactory.getInstance());
		Project reloaded = importer.loadProject(new ByteArrayInputStream(output.toByteArray()));
		NormalTask reloadedTask = (NormalTask) reloaded.getTasks().stream()
				.filter(task -> task.getId() == source.getId()).findFirst().orElseThrow();
		assertEquals(delay, reloadedTask.getLevelingDelay());
	}

	@Test
	void microsoftXmlRoundTripPreservesTrackingAndTaskModes() throws Exception {
		NormalTask sourceTask = createTask();
		Project sourceProject = sourceTask.getProject();
		sourceTask.setName("Tracking task");
		sourceTask.setManuallyScheduled(true);
		sourceTask.setPercentWorkComplete(0.40d);
		sourceTask.setPhysicalPercentComplete(0.30d);
		sourceTask.setInactiveTask(true);
		sourceTask.setEffortDriven(false);
		sourceTask.setSchedulingType(SchedulingType.Kind.FIXED_DURATION.code());
		sourceTask.setPriority(800);
		sourceTask.setFixedCost(1234.5d);
		sourceTask.setFixedCostAccrual(Accrual.Kind.START.code());

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName("tracking.xml");
		assertTrue(exporter.saveProject(sourceProject, output));
		net.sf.mpxj.ProjectFile exported = new net.sf.mpxj.mspdi.MSPDIReader()
				.read(new ByteArrayInputStream(output.toByteArray()));
		net.sf.mpxj.Task exportedTask = exported.getTasks().stream()
				.filter(task -> "Tracking task".equals(task.getName()))
				.findFirst().orElseThrow();
		assertEquals(0.0d, exportedTask.getPercentageComplete().doubleValue(), 0.00001d);
		assertEquals(40.0d, exportedTask.getPercentageWorkComplete().doubleValue(), 0.00001d);

		MicrosoftImporter importer = new MicrosoftImporter();
		importer.setFileName("tracking.xml");
		importer.setProject(createProject());
		Project reloaded = importer.loadProject(new ByteArrayInputStream(output.toByteArray()));
		assertFalse(reloaded.getTasks().isEmpty());
		NormalTask reloadedTask = null;
		for (Object candidate : reloaded.getTasks()) {
			NormalTask task = (NormalTask) candidate;
			if ("Tracking task".equals(task.getName())) {
				reloadedTask = task;
				break;
			}
		}
		assertNotNull(reloadedTask);

		assertEquals(0.0d, reloadedTask.getPercentComplete(), 0.00001d);
		assertEquals(0.40d, reloadedTask.getPercentWorkComplete(), 0.00001d);
		assertEquals(0.30d, reloadedTask.getPhysicalPercentComplete(), 0.00001d);
		assertTrue(reloadedTask.isManuallyScheduled());
		assertTrue(reloadedTask.isInactiveTask());
		assertFalse(reloadedTask.isEffortDriven());
		assertEquals(SchedulingType.Kind.FIXED_DURATION.code(), reloadedTask.getSchedulingType());
		assertEquals(800, reloadedTask.getPriority());
		assertEquals(1234.5d, reloadedTask.getFixedCost(), 0.00001d);
		assertEquals(Accrual.Kind.START.code(), reloadedTask.getFixedCostAccrual());
	}

	@Test
	void microsoftXmlRoundTripPreservesCrossProjectDependencies() throws Exception {
		Project local = createProject();
		Project external = createProject();
		NormalTask localSuccessor = (NormalTask) local.createLocalTaskNode(null).getImpl();
		localSuccessor.setName("Local successor");
		NormalTask externalPredecessor = (NormalTask) external.createLocalTaskNode(null).getImpl();
		externalPredecessor.setName("External predecessor");
		DependencyService.getInstance().newDependency(externalPredecessor, localSuccessor, DependencyType.Kind.SS.code(), 0L, this);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName("cross-project.xml");
		assertTrue(exporter.saveProject(local, output));
		net.sf.mpxj.ProjectFile exported = new net.sf.mpxj.mspdi.MSPDIReader()
				.read(new ByteArrayInputStream(output.toByteArray()));
		assertTrue(exported.getTasks().stream().anyMatch(net.sf.mpxj.Task::getExternalTask));
		for (net.sf.mpxj.Task task : exported.getTasks()) {
			if (task.getExternalTask()) {
				assertEquals(Long.toString(external.getUniqueId()), task.getExternalTaskProject());
			}
		}
		net.sf.mpxj.Task exportedSuccessor = exported.getTasks().stream()
				.filter(task -> "Local successor".equals(task.getName())).findFirst().orElseThrow();
		assertEquals(1, exportedSuccessor.getPredecessors().size());

		com.microproject.core.pm.exchange.MspImporter importer = new com.microproject.core.pm.exchange.MspImporter();
		Project reloaded = importer.importProject(new ByteArrayInputStream(output.toByteArray()), "xml", (progress, label) -> {});
		NormalTask reloadedLocalSuccessor = taskNamed(reloaded, "Local successor");
		Dependency incoming = (Dependency) reloadedLocalSuccessor.getPredecessorList().iterator().next();
		assertTrue(((Task) incoming.getPredecessor()).isExternal());
		assertEquals(external.getUniqueId(), ((Task) incoming.getPredecessor()).getProjectId());
		assertEquals(DependencyType.Kind.SS.code(), incoming.getDependencyType());
	}

	@Test
	void microsoftXmlRoundTripKeepsTaskBeforeProjectHeaderStart() throws Exception {
		String expectedTestTimeZone = System.getProperty("expected.test.timezone");
		if (expectedTestTimeZone != null)
			assertEquals(expectedTestTimeZone, java.util.TimeZone.getDefault().getID(),
				"the test worker must run in the explicitly selected zone");
		Project project = createProject();
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName("Earlier than project header");
		// Keep both boundaries on working days so the test isolates project-boundary
		// import ordering from normal calendar adjustment.
		long projectStart = modelWallTime("2026-10-05T08:00:00");
		long taskStart = modelWallTime("2026-10-02T08:00:00");
		project.setStartDate(projectStart);
		task.setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		// Gantt interval edits can legitimately move a scheduled task earlier than
		// the project header boundary. Preserve the canonical date and SNET rule as
		// the MSPDI snapshot input, just as a completed Gantt drag does.
		task.getCurrentSchedule().setStart(taskStart);
		task.setScheduleConstraint(ConstraintType.Kind.SNET, taskStart);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName("task-before-project-start.xml");
		assertTrue(exporter.saveProject(project, output));
		net.sf.mpxj.ProjectFile snapshot = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(output.toByteArray()));
		net.sf.mpxj.Task snapshotTask = snapshot.getTasks().stream()
			.filter(candidate -> "Earlier than project header".equals(candidate.getName())).findFirst().orElseThrow();
		assertTrue(snapshotTask.getStart().before(snapshot.getProjectProperties().getStartDate()),
			"fixture must contain a task start before the project header start");
		assertEquals(localWallDate("2026-10-02T08:00:00"), snapshotTask.getConstraintDate(),
			"MSPDI SNET constraint must preserve the model's local wall time");
		assertEquals(localWallDate("2026-10-05T08:00:00"), snapshot.getProjectProperties().getStartDate(),
			"MSPDI project header must preserve the model's local wall time");

		Project reloaded = new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(output.toByteArray()), "xml", (progress, label) -> {});
		NormalTask reloadedTask = taskNamed(reloaded, "Earlier than project header");
		long normalizedTaskStart = DateUTCConverter.toModelTime(snapshotTask.getStart());
		assertEquals(normalizedTaskStart, reloadedTask.getStart(),
			"earlier scheduled task start must survive MSPDI reload");
		assertEquals(normalizedTaskStart, reloadedTask.getConstraintDate(),
			"SNET constraint must survive MSPDI reload without a time-zone offset");
		assertEquals(reloadedTask.getStart(), reloaded.getStart(),
			"project boundary must follow its earliest imported task");
	}

	@Test
	void microsoftXmlPreservesExternalProjectFileWhenUidIsAlsoPresent() throws Exception {
		Project local = createProject();
		Project external = createProject();
		NormalTask localSuccessor = (NormalTask) local.createLocalTaskNode(null).getImpl();
		localSuccessor.setName("Local successor");
		NormalTask externalPredecessor = (NormalTask) external.createLocalTaskNode(null).getImpl();
		externalPredecessor.setName("External predecessor");
		externalPredecessor.setExternalProjectFile("C:/plans/external-project.xml");
		DependencyService.getInstance().newDependency(externalPredecessor, localSuccessor,
			DependencyType.Kind.FS.code(), 0L, this);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName("external-project-path.xml");
		assertTrue(exporter.saveProject(local, output));
		ProjectFile exported = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(output.toByteArray()));
		net.sf.mpxj.Task externalTask = exported.getTasks().stream()
			.filter(net.sf.mpxj.Task::getExternalTask).findFirst().orElseThrow();
		assertEquals("C:/plans/external-project.xml", externalTask.getExternalTaskProject());
		assertEquals("C:/plans/external-project.xml", externalTask.getSubprojectFile());
		Project reloaded = new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(output.toByteArray()), "xml", (progress, label) -> {});
		Dependency reloadedLink = (Dependency) taskNamed(reloaded, "Local successor").getPredecessorList().iterator().next();
		assertEquals("C:/plans/external-project.xml",
			((Task) reloadedLink.getPredecessor()).getExternalProjectFile());
	}

	private NormalTask taskNamed(Project project, String name) {
		for (Task task : project.getTaskList()) {
			if (name.equals(task.getName())) return (NormalTask) task;
		}
		throw new AssertionError("Missing task: " + name);
	}

	private NormalTask createTask() {
		Project project = createProject();
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0d, 0L, this);
		assignment.setWork(8L * 60L * 60L * 1000L, null);
		return task;
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private long calendarTime(int year, int month, int day, int hour) {
		java.util.GregorianCalendar calendar = com.microproject.util.DateTime.calendarInstance(year, month, day);
		calendar.set(java.util.Calendar.HOUR_OF_DAY, hour);
		return calendar.getTimeInMillis();
	}

	private static java.util.Date mspdiDateAtModelTime(String instant) {
		long modelTime = java.time.Instant.parse(instant).toEpochMilli();
		return new java.util.Date(com.microproject.core.time.TimeUtil.removeTimeZoneOffset(modelTime));
	}
}
