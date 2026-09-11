/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.core.pm.exchange.converters.mpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.microproject.core.time.TimeUtil;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.server.data.MSPDISerializer;
import com.microproject.undo.DataFactoryUndoController;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.ProjectProperties;
import net.sf.mpxj.ScheduleFrom;
import net.sf.mpxj.mspdi.MSPDIReader;
import net.sf.mpxj.mspdi.MSPDIWriter;

class MpxProjectConverterTest {
	@Test
	void importsFinishScheduledProjectUsingFinishBoundary() {
		Project project = project();
		ProjectProperties properties = new ProjectFile().getProjectProperties();
		Date finish = new Date(1_800_000_000_000L);
		properties.setScheduleFrom(ScheduleFrom.FINISH);
		properties.setFinishDate(finish);

		new MpxProjectConverter().from(properties, project, state(project));

		assertFalse(project.isForward());
		assertEquals(project.getEffectiveWorkCalendar().adjustInsideCalendar(
			TimeUtil.addTimeZoneOffset(finish.getTime()), true), project.getFinishDate());
	}

	@Test
	void importsStartScheduledProjectUsingStartBoundary() {
		Project project = project();
		ProjectProperties properties = new ProjectFile().getProjectProperties();
		Date start = new Date(1_700_000_000_000L);
		properties.setScheduleFrom(ScheduleFrom.START);
		properties.setStartDate(start);

		new MpxProjectConverter().from(properties, project, state(project));

		assertTrue(project.isForward());
		assertEquals(project.getEffectiveWorkCalendar().adjustInsideCalendar(
			TimeUtil.addTimeZoneOffset(start.getTime()), false), project.getStartDate());
	}

	@Test
	void exportsScheduleDirectionForFinishScheduledProject() throws Exception {
		Project project = project();
		project.setForward(false);

		ProjectFile exported = new MSPDISerializer().serializeProject(project);

		assertEquals(ScheduleFrom.FINISH, exported.getProjectProperties().getScheduleFrom());
	}

	@Test
	void preservesFinishSchedulingAcrossMspdiFileRoundTrip() throws Exception {
		Project source = project();
		source.setForward(false);
		source.setFinishDate(1_800_000_000_000L);
		ProjectFile exported = new MSPDISerializer().serializeProject(source);
		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		new MSPDIWriter().write(exported, xml);

		ProjectFile reloaded = new MSPDIReader().read(new ByteArrayInputStream(xml.toByteArray()));
		assertEquals(ScheduleFrom.FINISH, reloaded.getProjectProperties().getScheduleFrom());
		Project imported = project();
		new MpxProjectConverter().from(reloaded.getProjectProperties(), imported, state(imported));

		assertFalse(imported.isForward());
	}

	private static Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("mspdi-project-header", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		return project;
	}

	private static MpxImportState state(Project project) {
		MpxImportState state = new MpxImportState();
		state.setProjectBaseCalendar(project.getWorkCalendar());
		return state;
	}
}
