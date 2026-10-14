/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.core.pm.exchange.converters.mpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

import net.sf.mpxj.ProjectFile;

class MpxResourceConverterTest {
	@Test
	void importsMaximumUnitsAsPercentAndPreservesAbsentValue() {
		Project project = project();
		Resource target = project.getResourcePool().newResourceInstance();
		net.sf.mpxj.Resource source = new ProjectFile().addResource();
		MpxResourceConverter converter = new MpxResourceConverter();
		MpxImportState state = state(project);

		target.getAvailabilityTable().setMaximumUnits(0.75D);
		source.setMaxUnits(null);
		converter.from(source, target, state);
		assertEquals(0.75D, target.getMaximumUnits(), 0.000001D);

		for (double[] value : new double[][] {{0.0D, 0.0D}, {50.0D, 0.5D}, {100.0D, 1.0D}, {150.0D, 1.5D}}) {
			source.setMaxUnits(value[0]);
			converter.from(source, target, state);
			assertEquals(value[1], target.getMaximumUnits(), 0.000001D,
				"MPXJ MaxUnits percent " + value[0]);
		}
	}

	@Test
	void rejectsInvalidMaximumUnits() {
		Project project = project();
		Resource target = project.getResourcePool().newResourceInstance();
		net.sf.mpxj.Resource source = new ProjectFile().addResource();
		MpxResourceConverter converter = new MpxResourceConverter();
		MpxImportState state = state(project);
		target.setName("Existing target");
		target.setId(11L);
		target.setUniqueId(12L);
		target.getAvailabilityTable().setMaximumUnits(0.75D);
		source.setName("Should not be applied");
		source.setID(21);
		source.setUniqueID(22);

		for (double value : new double[] {-1.0D, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
			source.setMaxUnits(value);
			assertThrows(IllegalArgumentException.class, () -> converter.from(source, target, state));
			assertEquals("Existing target", target.getName());
			assertEquals(11L, target.getId());
			assertEquals(12L, target.getUniqueId());
			assertEquals(0.75D, target.getMaximumUnits(), 0.000001D);
		}
	}

	private static Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("mpx-resource-import", undo), undo);
		project.initialize(false, false);
		return project;
	}

	private static MpxImportState state(Project project) {
		MpxImportState state = new MpxImportState();
		state.setProjectBaseCalendar(project.getWorkCalendar());
		return state;
	}
}
