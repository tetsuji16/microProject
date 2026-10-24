/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.core.pm.exchange.converters.mpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Date;
import java.util.ArrayList;
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
	void literalMspdiAvailabilityPeriodsRoundTripThroughModelMpoAndMspdi() throws Exception {
		TimeZone originalZone = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("GMT+09:00"));
		try {
			String sourceXml = """
				<Project xmlns="http://schemas.microsoft.com/project">
				  <Name>Literal availability contract</Name><ScheduleFromStart>1</ScheduleFromStart>
				  <StartDate>2030-06-03T08:00:00</StartDate><MinutesPerDay>480</MinutesPerDay>
				  <MinutesPerWeek>2400</MinutesPerWeek><DaysPerMonth>20</DaysPerMonth><DurationFormat>7</DurationFormat>
				  <Resources><Resource><UID>1</UID><ID>1</ID><Name>Availability resource</Name><Type>1</Type><MaxUnits>0.75</MaxUnits>
				    <AvailabilityPeriods>
			      <AvailabilityPeriod><AvailableTo>2030-06-03T08:59:59</AvailableTo><AvailableUnits>1.0</AvailableUnits></AvailabilityPeriod>
				      <AvailabilityPeriod><AvailableFrom>2030-06-03T09:00:00</AvailableFrom><AvailableTo>2030-06-03T09:00:03</AvailableTo><AvailableUnits>0.25</AvailableUnits></AvailabilityPeriod>
				      <AvailabilityPeriod><AvailableFrom>2030-06-03T09:00:04</AvailableFrom><AvailableTo>2030-06-03T09:00:04</AvailableTo><AvailableUnits>0.0</AvailableUnits></AvailabilityPeriod>
				      <AvailabilityPeriod><AvailableFrom>2030-06-03T09:00:08</AvailableFrom><AvailableTo>2030-06-03T09:00:10</AvailableTo><AvailableUnits>0.5</AvailableUnits></AvailabilityPeriod>
			      <AvailabilityPeriod><AvailableFrom>2030-06-03T09:00:12</AvailableFrom><AvailableUnits>1.5</AvailableUnits></AvailabilityPeriod>
				    </AvailabilityPeriods>
			  </Resource><Resource><UID>2</UID><ID>2</ID><Name>MaxUnits fallback only</Name><Type>1</Type><MaxUnits>0.75</MaxUnits></Resource></Resources>
			</Project>
			""";
			Project imported = importMspdi(sourceXml);
			Resource fallback = ResourcePool.findResourceByName("MaxUnits fallback only", imported.getResourcePool());
			assertNotNull(fallback);
			assertAvailabilityManifest(fallback, 0.75D);
			Resource resource = ResourcePool.findResourceByName("Availability resource", imported.getResourcePool());
			assertNotNull(resource);
			assertAvailabilityManifest(resource, null);

			ByteArrayOutputStream mpo = new ByteArrayOutputStream();
			assertTrue(new MpoFileImporter().saveProject(imported, mpo));
			Project reopened = new MpoFileImporter().loadProject(new ByteArrayInputStream(mpo.toByteArray()));
			Resource reopenedResource = ResourcePool.findResourceByName("Availability resource", reopened.getResourcePool());
			assertNotNull(reopenedResource);
			assertAvailabilityManifest(reopenedResource, null);
			Resource reopenedFallback = ResourcePool.findResourceByName("MaxUnits fallback only", reopened.getResourcePool());
			assertNotNull(reopenedFallback);
			assertAvailabilityManifest(reopenedFallback, 0.75D);

			ByteArrayOutputStream exported = new ByteArrayOutputStream();
			assertTrue(new com.microproject.exchange.MicrosoftImporter().saveProject(reopened, exported));
			ProjectFile parsed = new net.sf.mpxj.mspdi.MSPDIReader().read(
				new ByteArrayInputStream(exported.toByteArray()));
			net.sf.mpxj.Resource parsedResource = parsed.getResources().stream()
				.filter(candidate -> "Availability resource".equals(candidate.getName())).findFirst().orElseThrow();
			assertEquals(7, parsedResource.getAvailability().size(),
				"writer emits the five source periods and explicit zero ranges for two source gaps");
			assertEquals(25.0D, parsedResource.getAvailability().stream()
				.filter(period -> period.getUnits().doubleValue() == 25.0D).findFirst().orElseThrow()
				.getUnits().doubleValue());
			assertEquals(150.0D, parsedResource.getAvailability().stream()
				.filter(period -> period.getUnits().doubleValue() == 150.0D).findFirst().orElseThrow()
				.getUnits().doubleValue());
			assertEquals(50.0D, parsedResource.getAvailability().stream()
				.filter(period -> period.getUnits().doubleValue() == 50.0D).findFirst().orElseThrow()
				.getUnits().doubleValue());
			Project reimported = importMspdi(exported.toString(java.nio.charset.StandardCharsets.UTF_8));
			Resource finalResource = ResourcePool.findResourceByName("Availability resource", reimported.getResourcePool());
			assertNotNull(finalResource);
			assertAvailabilityManifest(finalResource, null);
			Resource finalFallback = ResourcePool.findResourceByName("MaxUnits fallback only", reimported.getResourcePool());
			assertNotNull(finalFallback);
			assertAvailabilityManifest(finalFallback, 0.75D);

			String missingUnits = sourceXml.replace("<AvailableUnits>0.25</AvailableUnits>", "");
			assertMspdiRejectedWithoutPublication(missingUnits);
			String overlapping = sourceXml.replace("2030-06-03T09:00:12", "2030-06-03T09:00:03");
			assertMspdiRejectedWithoutPublication(overlapping);
		} finally {
			TimeZone.setDefault(originalZone);
		}
	}

	private static void assertAvailabilityManifest(Resource resource, Double scalarExpected) {
		if (scalarExpected != null) {
			assertAvailability(resource, modelTime("2030-06-03T08:59:59Z"), scalarExpected.doubleValue());
			assertAvailability(resource, modelTime("2030-06-03T09:00:00Z"), scalarExpected.doubleValue());
			assertAvailability(resource, modelTime("2030-06-03T09:00:15Z"), scalarExpected.doubleValue());
			return;
		}
		assertAvailability(resource, instant("2030-06-03T08:59:59Z"), 1.0D);
		assertAvailability(resource, instant("2030-06-03T09:00:00Z"), 0.25D);
		assertAvailability(resource, instant("2030-06-03T09:00:03Z"), 0.25D);
		assertAvailability(resource, instant("2030-06-03T09:00:04Z"), 0.0D,
			"inclusive AvailableTo 09:00:03 maps to a half-open end at 09:00:04");
		assertAvailability(resource, instant("2030-06-03T09:00:05Z"), 0.0D);
		assertAvailability(resource, instant("2030-06-03T09:00:07Z"), 0.0D);
		assertAvailability(resource, instant("2030-06-03T09:00:08Z"), 0.5D);
		assertAvailability(resource, instant("2030-06-03T09:00:10Z"), 0.5D);
		assertAvailability(resource, instant("2030-06-03T09:00:11Z"), 0.0D);
		assertAvailability(resource, instant("2030-06-03T09:00:12Z"), 1.5D);
		assertAvailability(resource, instant("2030-06-03T09:00:14Z"), 1.5D);
		assertAvailability(resource, instant("2030-06-03T09:00:15Z"), 1.5D,
			"omitted AvailableTo is unbounded through the end of the domain");
	}

	private static void assertMspdiRejectedWithoutPublication(String xml) {
		byte[] original = xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		byte[] inputBytes = original.clone();
		ArrayList<String> progressLabels = new ArrayList<>();
		assertThrows(IllegalArgumentException.class, () -> new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(inputBytes), "xml", (progress, label) -> progressLabels.add(label)));
		assertTrue(java.util.Arrays.equals(original, inputBytes), "rejection must not mutate caller input");
		assertFalse(progressLabels.contains("Completed"), "rejected MSPDI must not publish completed progress");
	}

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
		assertAvailability(resource, time, units, "availability at " + Instant.ofEpochMilli(time));
	}

	private static Project importMspdi(String xml) throws Exception {
		return new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)), "xml",
			(progress, label) -> {});
	}

	private static void assertAvailability(Resource resource, long time, double units, String message) {
		Availability value = (Availability) resource.getAvailabilityTable().findActive(time);
		assertNotNull(value, message);
		assertEquals(units, value.getMaximumUnits(), message);
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
