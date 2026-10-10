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
import com.microproject.pm.task.Task;
import com.microproject.pm.scheduling.SchedulingType;
import com.microproject.session.SessionFactory;
import com.microproject.undo.DataFactoryUndoController;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.TaskMode;

class MpxExportTrackingTest {
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
