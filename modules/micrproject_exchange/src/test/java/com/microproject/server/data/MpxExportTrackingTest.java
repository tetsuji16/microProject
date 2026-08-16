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
import java.util.Iterator;

import com.microproject.exchange.MicrosoftImporter;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

import junit.framework.TestCase;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.TaskMode;

public class MpxExportTrackingTest extends TestCase {
	public void testTaskTrackingModesAndActualsAreExported() {
		NormalTask source = createTask();
		source.setManuallyScheduled(true);
		source.setPercentWorkComplete(0.40d);
		source.setPhysicalPercentComplete(0.30d);
		source.setInactiveTask(true);

		net.sf.mpxj.Task target = new ProjectFile().addTask();
		MPXConverter.toMPXTask(source, target);

		assertEquals(40.0d, target.getPercentageComplete().doubleValue(), 0.00001d);
		assertEquals(40.0d, target.getPercentageWorkComplete().doubleValue(), 0.00001d);
		assertEquals(30.0d, target.getPhysicalPercentComplete().doubleValue(), 0.00001d);
		assertNotNull(target.getActualStart());
		assertNotNull(target.getActualDuration());
		assertEquals(TaskMode.MANUALLY_SCHEDULED, target.getTaskMode());
		assertFalse(target.getActive());
	}

	public void testAssignmentTrackingValuesAreExported() {
		NormalTask task = createTask();
		task.setPercentWorkComplete(0.50d);
		Iterator<?> assignments = task.getAssignments().iterator();
		Assignment source = (Assignment) assignments.next();
		ProjectFile file = new ProjectFile();
		net.sf.mpxj.Task targetTask = file.addTask();
		ResourceAssignment target = targetTask.addResourceAssignment(file.addResource());

		MPXConverter.toMPXAssignment(source, target);

		assertEquals(50.0d, target.getPercentageWorkComplete().doubleValue(), 0.00001d);
		assertNotNull(target.getActualStart());
	}

	public void testMicrosoftXmlRoundTripPreservesTrackingAndTaskModes() throws Exception {
		NormalTask sourceTask = createTask();
		Project sourceProject = sourceTask.getProject();
		sourceTask.setName("Tracking task");
		sourceTask.setManuallyScheduled(true);
		sourceTask.setPercentWorkComplete(0.40d);
		sourceTask.setPhysicalPercentComplete(0.30d);
		sourceTask.setInactiveTask(true);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName("tracking.xml");
		assertTrue(exporter.saveProject(sourceProject, output));
		net.sf.mpxj.ProjectFile exported = new net.sf.mpxj.mspdi.MSPDIReader()
				.read(new ByteArrayInputStream(output.toByteArray()));
		net.sf.mpxj.Task exportedTask = exported.getTasks().stream()
				.filter(task -> "Tracking task".equals(task.getName()))
				.findFirst().orElseThrow();
		assertEquals(40.0d, exportedTask.getPercentageComplete().doubleValue(), 0.00001d);

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

		assertEquals(0.40d, reloadedTask.getPercentComplete(), 0.00001d);
		assertEquals(0.30d, reloadedTask.getPhysicalPercentComplete(), 0.00001d);
		assertTrue(reloadedTask.isManuallyScheduled());
		assertTrue(reloadedTask.isInactiveTask());
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
