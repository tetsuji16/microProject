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
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.microproject.core.pm.exchange.converters.type.DateUTCConverter;
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
		java.util.Date baselineZeroStart = java.util.Date.from(java.time.Instant.parse("2026-10-05T08:00:00Z"));
		java.util.Date baselineZeroFinish = java.util.Date.from(java.time.Instant.parse("2026-10-06T17:00:00Z"));
		java.util.Date baselineOneStart = java.util.Date.from(java.time.Instant.parse("2026-10-07T08:00:00Z"));
		java.util.Date milestoneBaseline = java.util.Date.from(java.time.Instant.parse("2026-10-08T08:00:00Z"));
		java.util.Date baselineTenStart = java.util.Date.from(java.time.Instant.parse("2026-10-01T08:00:00Z"));
		java.util.Date baselineTenFinish = java.util.Date.from(java.time.Instant.parse("2026-10-02T17:00:00Z"));
		task.setBaselineStart(baselineZeroStart);
		task.setBaselineFinish(baselineZeroFinish);
		task.setBaselineDuration(net.sf.mpxj.Duration.getInstance(2, net.sf.mpxj.TimeUnit.DAYS));
		task.setBaselineStart(1, baselineOneStart); // partial baseline: finish is absent
		task.setBaselineStart(2, milestoneBaseline);
		task.setBaselineFinish(2, milestoneBaseline);
		task.setBaselineDuration(2, net.sf.mpxj.Duration.getInstance(0, net.sf.mpxj.TimeUnit.DAYS));
		task.setBaselineStart(10, baselineTenStart);
		task.setBaselineFinish(10, baselineTenFinish);
		task.setBaselineDuration(10, net.sf.mpxj.Duration.getInstance(2, net.sf.mpxj.TimeUnit.DAYS));
		ByteArrayOutputStream sourceXml = new ByteArrayOutputStream();
		new net.sf.mpxj.mspdi.MSPDIWriter().write(source, sourceXml);

		Project imported = new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(sourceXml.toByteArray()), "xml", (progress, label) -> {});
		NormalTask importedTask = taskNamed(imported, "Baseline contract");
		assertEquals(com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(currentStart),
			importedTask.getCurrentSchedule().getStart(), "baseline conversion must leave the current schedule intact");
		assertEquals(com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(baselineZeroStart),
			importedTask.getBaselineStart());
		assertEquals(com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(baselineZeroFinish),
			importedTask.getBaselineFinish());
		assertBaselineDuration(importedTask.getBaselineDuration(0), 2.0d,
			com.microproject.datatype.TimeUnit.DAYS);
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
		assertEquals(net.sf.mpxj.TimeUnit.DAYS, roundTripTask.getBaselineDuration().getUnits());
		assertEquals(baselineOneStart, roundTripTask.getBaselineStart(1));
		assertNull(roundTripTask.getBaselineFinish(1));
		assertEquals(milestoneBaseline, roundTripTask.getBaselineStart(2));
		assertEquals(milestoneBaseline, roundTripTask.getBaselineFinish(2));
		assertEquals(baselineTenStart, roundTripTask.getBaselineStart(10));
		assertEquals(baselineTenFinish, roundTripTask.getBaselineFinish(10));
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
		assertEquals(DateUTCConverter.toModelTime(java.util.Date.from(java.time.Instant.parse("2026-10-05T08:00:00Z"))),
			baseline.getStart());
		assertEquals(8L * 60L * 60L * 1000L, baseline.getWork(null));
		assertNull(assignment.getBaselineAssignment(Integer.valueOf(1), false));
		Assignment baselineTen = assignment.getBaselineAssignment(Integer.valueOf(10), false);
		assertNotNull(baselineTen);
		assertEquals(DateUTCConverter.toModelTime(java.util.Date.from(java.time.Instant.parse("2026-10-07T08:00:00Z"))),
			baselineTen.getStart());
		assertEquals(DateUTCConverter.toModelTime(java.util.Date.from(java.time.Instant.parse("2026-10-08T08:00:00Z"))),
			baselineTen.getFinish());
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
		assertEquals(java.util.Date.from(java.time.Instant.parse("2026-10-05T08:00:00Z")),
			roundTripAssignment.getBaselineStart());
		assertEquals(8.0d, roundTripAssignment.getBaselineWork()
			.convertUnits(net.sf.mpxj.TimeUnit.HOURS, roundTrip.getProjectProperties()).getDuration(), 0.00001d);
		assertNull(roundTripAssignment.getBaselineStart(1));
		assertEquals(java.util.Date.from(java.time.Instant.parse("2026-10-07T08:00:00Z")),
			roundTripAssignment.getBaselineStart(10));
		assertEquals(java.util.Date.from(java.time.Instant.parse("2026-10-08T08:00:00Z")),
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
		assertEquals(java.time.Instant.parse("2026-10-05T17:00:00Z").toEpochMilli(),
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
		assertEquals(java.util.Date.from(java.time.Instant.parse("2026-10-05T08:00:00Z")), xmlAssignment.getBaselineStart());
		assertNull(xmlAssignment.getBaselineFinish());
		assertEquals(java.util.Date.from(java.time.Instant.parse("2026-10-07T08:00:00Z")),
			xmlAssignment.getBaselineStart(10));
		assertEquals(java.util.Date.from(java.time.Instant.parse("2026-10-07T08:00:00Z")),
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
		assertEquals(DateUTCConverter.toModelTime(java.util.Date.from(java.time.Instant.parse("2026-10-05T08:00:00Z"))),
			assignment.getBaselineValues(0).start());
		assertNull(assignment.getBaselineValues(0).finish());
		assertEquals(Long.valueOf(8L * 60L * 60L * 1000L), assignment.getBaselineValues(0).work());
		assertEquals(new Assignment.BaselineValues(null, null, Long.valueOf(0L)), assignment.getBaselineValues(9));
		assertEquals(java.time.Instant.parse("2026-10-07T17:00:00Z").toEpochMilli(),
			assignment.getBaselineValues(10).start());
		assertEquals(java.time.Instant.parse("2026-10-08T17:00:00Z").toEpochMilli(),
			assignment.getBaselineValues(10).finish());
		assertEquals(Long.valueOf(4L * 60L * 60L * 1000L), assignment.getBaselineValues(10).work());
	}

	private static Project importBaselineFixture(boolean includeBaselines) throws Exception {
		return importBaselineFixture(includeBaselines, true, false);
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
		java.util.Date currentStart = java.util.Date.from(java.time.Instant.parse("2026-10-12T08:00:00Z"));
		java.util.Date currentFinish = java.util.Date.from(java.time.Instant.parse("2026-10-13T17:00:00Z"));
		net.sf.mpxj.Task task = source.addTask();
		task.setName("Assignment baseline contract");
		task.setUniqueID(1);
		task.setStart(currentStart);
		task.setFinish(currentFinish);
		task.setDuration(net.sf.mpxj.Duration.getInstance(2, net.sf.mpxj.TimeUnit.DAYS));
		if (includeTaskBaseline) {
			task.setBaselineStart(java.util.Date.from(java.time.Instant.parse("2026-10-05T08:00:00Z")));
			task.setBaselineFinish(java.util.Date.from(java.time.Instant.parse("2026-10-06T08:00:00Z")));
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
			assignment.setBaselineStart(java.util.Date.from(java.time.Instant.parse("2026-10-05T08:00:00Z")));
			assignment.setBaselineWork(net.sf.mpxj.Duration.getInstance(8, net.sf.mpxj.TimeUnit.HOURS));
			java.util.Date baselineTenStart = java.util.Date.from(java.time.Instant.parse("2026-10-07T08:00:00Z"));
			java.util.Date baselineTenFinish = zeroSlotTenWork ? baselineTenStart
				: java.util.Date.from(java.time.Instant.parse("2026-10-08T08:00:00Z"));
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
	void mpxj1154MspdiRoundTripNormalizesElapsedBaselineDuration() throws Exception {
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
		// MPXJ 11.5.4's MSPDI writer/reader normalization changes two elapsed days
		// to six working days when MinutesPerDay is 480. Keep this explicit known
		// limitation visible instead of claiming XML elapsed-unit round-trip parity.
		assertBaselineDuration(baselineDuration, 6.0d, com.microproject.datatype.TimeUnit.DAYS);
	}

	@Test
	void historicalAssignmentImportUsesTheTaskStartAsItsOffsetOrigin() throws Exception {
		ProjectFile source = new ProjectFile();
		source.addDefaultBaseCalendar();
		java.util.Date start = java.util.Date.from(java.time.Instant.parse("2000-01-03T08:00:00Z"));
		java.util.Date finish = java.util.Date.from(java.time.Instant.parse("2000-01-04T17:00:00Z"));
		source.getProjectProperties().setStartDate(start);
		net.sf.mpxj.Task task = source.addTask();
		task.setName("Historical assignment");
		task.setUniqueID(1);
		task.setStart(start);
		task.setFinish(finish);
		task.setDuration(net.sf.mpxj.Duration.getInstance(2, net.sf.mpxj.TimeUnit.DAYS));
		net.sf.mpxj.Resource resource = source.addResource();
		resource.setName("Worker");
		resource.setUniqueID(1);
		ResourceAssignment assignment = task.addResourceAssignment(resource);
		assignment.setUnits(100d);
		assignment.setStart(start);
		assignment.setFinish(finish);
		assignment.setWork(net.sf.mpxj.Duration.getInstance(16, net.sf.mpxj.TimeUnit.HOURS));
		assignment.setRemainingWork(net.sf.mpxj.Duration.getInstance(16, net.sf.mpxj.TimeUnit.HOURS));
		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		new net.sf.mpxj.mspdi.MSPDIWriter().write(source, xml);

		Project imported = new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(xml.toByteArray()), "xml", (progress, label) -> {});
		NormalTask importedTask = taskNamed(imported, "Historical assignment");
		Assignment importedAssignment = (Assignment) importedTask.getAssignments().iterator().next();
		assertEquals(0L, importedAssignment.getDelay());
		assertEquals(16L * 60L * 60L * 1000L, importedAssignment.getWork(null));
		assertEquals(importedTask.getStart(), importedAssignment.getStart());
		assertTrue(importedTask.getEnd() < java.util.Date.from(
			java.time.Instant.parse("2001-01-01T00:00:00Z")).getTime());
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
		task.setPercentWorkComplete(0.50d);
		Iterator<?> assignments = task.getAssignments().iterator();
		Assignment source = (Assignment) assignments.next();
		source.setLevelingDelay(2L * 60L * 60L * 1000L);
		ProjectFile file = new ProjectFile();
		net.sf.mpxj.Task targetTask = file.addTask();
		ResourceAssignment target = targetTask.addResourceAssignment(file.addResource());

		MPXConverter.toMPXAssignment(source, target);

		assertEquals(50.0d, target.getPercentageWorkComplete().doubleValue(), 0.00001d);
		assertNotNull(target.getActualStart());
		assertEquals(120.0d, target.getLevelingDelay().getDuration(), 0.00001d);
		assertEquals(net.sf.mpxj.TimeUnit.MINUTES, target.getLevelingDelay().getUnits());
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
		Project project = createProject();
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName("Earlier than project header");
		java.util.Calendar projectStartCalendar = java.util.Calendar.getInstance();
		projectStartCalendar.clear();
		projectStartCalendar.set(2026, java.util.Calendar.OCTOBER, 5, 8, 0, 0);
		long projectStart = projectStartCalendar.getTimeInMillis();
		java.util.Calendar taskStartCalendar = (java.util.Calendar) projectStartCalendar.clone();
		// Keep both boundaries on working days so the test isolates project-boundary
		// import ordering from normal calendar adjustment.
		taskStartCalendar.add(java.util.Calendar.DAY_OF_MONTH, -3);
		long taskStart = taskStartCalendar.getTimeInMillis();
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

		Project reloaded = new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(output.toByteArray()), "xml", (progress, label) -> {});
		NormalTask reloadedTask = taskNamed(reloaded, "Earlier than project header");
		long normalizedTaskStart = DateUTCConverter.toModelTime(snapshotTask.getStart());
		assertEquals(normalizedTaskStart, reloadedTask.getStart(),
			"earlier scheduled task start must survive MSPDI reload");
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
}
