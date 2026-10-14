/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.core.pm.exchange.converters.mpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.availability.Availability;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.server.data.MPXConverter;
import com.microproject.undo.DataFactoryUndoController;

import net.sf.mpxj.AvailabilityTable;
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

	@Test
	void mapsAvailabilityBoundariesGapsAndZeroThroughNativeMpo() throws Exception {
		TimeZone originalZone = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("GMT+09:00"));
		try {
			Project project = project();
			Resource resource = project.getResourcePool().createScriptedResource();
			resource.setName("Availability contract");
			NormalTask task = project.createScriptedTask();
			task.setName("Availability contract task");
			Assignment assignment = (Assignment) task.getAssignments().iterator().next();
			assignment.setTaskAndResource(task, resource);
			AvailabilityTable periods = new AvailabilityTable();
			periods.add(new net.sf.mpxj.Availability(net.sf.mpxj.common.DateHelper.START_DATE_NA,
				date("2030-06-03T08:59:59Z"), Double.valueOf(100.0D)));
			periods.add(new net.sf.mpxj.Availability(date("2030-06-03T09:00:00Z"),
				date("2030-06-03T09:00:03Z"), Double.valueOf(50.0D)));
			periods.add(new net.sf.mpxj.Availability(date("2030-06-03T09:00:04Z"),
				date("2030-06-03T09:00:04Z"), Double.valueOf(0.0D)));
			periods.add(new net.sf.mpxj.Availability(date("2030-06-03T09:00:08Z"),
				date("2030-06-03T09:00:10Z"), Double.valueOf(25.0D)));
			net.sf.mpxj.Resource source = new ProjectFile().addResource();
			source.setName("Availability contract");
			source.getAvailability().addAll(periods);

			new MpxResourceConverter().from(source, resource, state(project));
			assertAvailability(resource, modelTime("2030-06-03T08:59:59Z"), 1.0D);
			assertAvailability(resource, modelTime("2030-06-03T09:00:00Z"), 0.5D);
			assertAvailability(resource, modelTime("2030-06-03T09:00:03Z"), 0.5D);
			assertAvailability(resource, modelTime("2030-06-03T09:00:04Z"), 0.0D);
			assertAvailability(resource, modelTime("2030-06-03T09:00:05Z"), 0.0D);
			assertAvailability(resource, modelTime("2030-06-03T09:00:08Z"), 0.25D);
			assertAvailability(resource, modelTime("2030-06-03T09:00:11Z"), 0.0D);

			ByteArrayOutputStream mpo = new ByteArrayOutputStream();
			MpoFileImporter writer = new MpoFileImporter();
			assertTrue(writer.saveProject(project, mpo));
			byte[] archive = mpo.toByteArray();
			String content = zipEntry(archive, "content.xml");
			assertNotNull(content);
			String resourceXml = resourceElement(content, "Availability contract");
			assertEquals(6, resourceXml.split("<AvailabilityPeriod>", -1).length - 1);
			Project reopened = new MpoFileImporter().loadProject(new ByteArrayInputStream(archive));
			Resource reopenedResource = ResourcePool.findResourceByName("Availability contract", reopened.getResourcePool());
			assertNotNull(reopenedResource);
			assertAvailability(reopenedResource, modelTime("2030-06-03T08:59:59Z"), 1.0D);
			assertAvailability(reopenedResource, modelTime("2030-06-03T09:00:00Z"), 0.5D);
			assertAvailability(reopenedResource, modelTime("2030-06-03T09:00:03Z"), 0.5D);
			assertAvailability(reopenedResource, modelTime("2030-06-03T09:00:04Z"), 0.0D);
			assertAvailability(reopenedResource, modelTime("2030-06-03T09:00:08Z"), 0.25D);
			assertAvailability(reopenedResource, modelTime("2030-06-03T09:00:11Z"), 0.0D);
		} finally {
			TimeZone.setDefault(originalZone);
		}
	}

	@Test
	void validatesPeriodsBeforeChangingTheTargetAndRejectsOverlapsAndMissingUnits() {
		Project project = project();
		Resource target = project.getResourcePool().newResourceInstance();
		target.setName("Keep this resource");
		target.setId(11L);
		target.setUniqueId(12L);
		target.getAvailabilityTable().setMaximumUnits(0.75D);
		net.sf.mpxj.Resource source = new ProjectFile().addResource();
		source.setName("Do not apply");
		source.setID(21);
		source.setUniqueID(22);
		AvailabilityTable invalid = new AvailabilityTable();
		Date a = date("2030-06-03T09:00:00Z");
		Date b = date("2030-06-03T09:00:05Z");
		invalid.add(new net.sf.mpxj.Availability(a, b, Double.valueOf(50.0D)));
		invalid.add(new net.sf.mpxj.Availability(date("2030-06-03T09:00:05Z"),
			date("2030-06-03T09:00:08Z"), Double.valueOf(25.0D)));
		source.getAvailability().addAll(invalid);
		assertThrows(IllegalArgumentException.class,
			() -> new MpxResourceConverter().from(source, target, state(project)));
		assertEquals("Keep this resource", target.getName());
		assertEquals(11L, target.getId());
		assertEquals(12L, target.getUniqueId());
		assertEquals(0.75D, target.getMaximumUnits(), 0.000001D);
		assertEquals(1, target.getAvailabilityTable().getList().size());

		AvailabilityTable missingUnits = new AvailabilityTable();
		missingUnits.add(new net.sf.mpxj.Availability(a, b, null));
		source.getAvailability().clear();
		source.getAvailability().addAll(missingUnits);
		assertThrows(IllegalArgumentException.class,
			() -> new MpxResourceConverter().from(source, target, state(project)));
		assertEquals("Keep this resource", target.getName());
		assertEquals(0.75D, target.getMaximumUnits(), 0.000001D);

		long domainEnd = com.microproject.util.DateTime.getMaxDate().getTime();
		Date finiteEndAtSentinel = new Date(com.microproject.core.time.TimeUtil.removeTimeZoneOffset(domainEnd - 1_000L));
		Date finiteStartAtSentinel = new Date(com.microproject.core.time.TimeUtil.removeTimeZoneOffset(domainEnd - 10_000L));
		AvailabilityTable sentinelCollision = new AvailabilityTable();
		sentinelCollision.add(new net.sf.mpxj.Availability(finiteStartAtSentinel, finiteEndAtSentinel,
			Double.valueOf(50.0D)));
		source.getAvailability().clear();
		source.getAvailability().addAll(sentinelCollision);
		assertThrows(IllegalArgumentException.class,
			() -> new MpxResourceConverter().from(source, target, state(project)));
		assertEquals("Keep this resource", target.getName());
		assertEquals(0.75D, target.getMaximumUnits(), 0.000001D);
	}

	@Test
	void rejectsSubsecondNativeBoundariesBeforeReplacingExportPeriods() throws Exception {
		Project project = project();
		Resource source = project.getResourcePool().createScriptedResource();
		source.getAvailabilityTable().newValueObject(instant("2030-06-03T09:00:00Z") + 500L);
		net.sf.mpxj.Resource target = new ProjectFile().addResource();
		target.setName("Preserve target");
		target.getAvailability().add(new net.sf.mpxj.Availability(
			net.sf.mpxj.common.DateHelper.START_DATE_NA,
			net.sf.mpxj.common.DateHelper.END_DATE_NA, Double.valueOf(100.0D)));

		assertThrows(IllegalArgumentException.class,
			() -> MPXConverter.toMPXResource((ResourceImpl) source, target));
		assertEquals("Preserve target", target.getName(), "failed validation must precede other target fields");
		assertEquals(1, target.getAvailability().size(), "failed validation must preserve existing export values");

		Resource percentageOverflow = project.getResourcePool().createScriptedResource();
		percentageOverflow.getAvailabilityTable().setMaximumUnits(Double.MAX_VALUE);
		assertThrows(IllegalArgumentException.class,
			() -> MpxResourceAvailabilityConverter.to(percentageOverflow.getAvailabilityTable(), target));
		assertEquals(1, target.getAvailability().size(), "overflow must be rejected before replacing export values");
	}

	private static void assertAvailability(Resource resource, long time, double units) {
		Availability value = (Availability) resource.getAvailabilityTable().findActive(time);
		assertEquals(units, value.getMaximumUnits(), 0.000001D, "availability at " + Instant.ofEpochMilli(time));
	}

	private static Date date(String value) {
		return new Date(instant(value));
	}

	private static long instant(String value) {
		return Instant.parse(value).toEpochMilli();
	}

	private static long modelTime(String value) {
		return com.microproject.core.time.TimeUtil.addTimeZoneOffset(instant(value));
	}

	private static String zipEntry(byte[] archive, String name) throws Exception {
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
			java.util.zip.ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (name.equals(entry.getName())) return new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
			}
		}
		return null;
	}

	private static String resourceElement(String xml, String name) {
		int resourcesStart = xml.indexOf("<Resources>");
		int nameIndex = xml.indexOf("<Name>" + name + "</Name>", resourcesStart);
		int start = xml.lastIndexOf("<Resource>", nameIndex);
		int end = xml.indexOf("</Resource>", nameIndex);
		if (nameIndex < 0 || start < 0 || end < 0)
			throw new AssertionError("Resource missing from native XML: " + name + ", resources="
				+ xml.substring(resourcesStart, Math.min(resourcesStart + 1500, xml.length())));
		return xml.substring(start, end + "</Resource>".length());
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
