/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.microproject.core.pm.exchange.MspImporter;
import com.microproject.pm.task.Project;
import com.microproject.server.data.MPXConverter;
import com.microproject.util.DateTime;

/**
 * MSPDI DefaultStartTime/DefaultFinishTime are floating times of day, so the host time zone must
 * not change them (#989). Each zone runs in its own JVM: MPXJ caches per-thread calendars, so the
 * default zone cannot be switched reliably inside one process.
 */
class MspdiDefaultTimeZoneTest {
	@ParameterizedTest
	@ValueSource(strings = {"UTC", "Asia/Tokyo", "America/Los_Angeles", "Asia/Kolkata"})
	void defaultTimesSurviveRoundTripInEveryHostZone(String zone, @TempDir Path tempDir) throws Exception {
		Path log = tempDir.resolve("worker.log");
		Process worker = new ProcessBuilder(List.of(
				Path.of(System.getProperty("java.home"), "bin", "java").toString(),
				"-Duser.timezone=" + zone, "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8",
				"-cp", System.getProperty("java.class.path"), MspdiDefaultTimeZoneTest.class.getName()))
			.redirectErrorStream(true).redirectOutput(log.toFile()).start();
		assertTrue(worker.waitFor(120, TimeUnit.SECONDS), "worker timed out in " + zone);
		assertEquals(0, worker.exitValue(), () -> zone + ": " + readLog(log));
	}

	/** Child JVM entry point; a non-zero exit fails the parameterized case. */
	public static void main(String[] args) {
		try {
			verifyInCurrentZone();
			System.exit(0);
		} catch (Throwable failure) {
			failure.printStackTrace();
			System.exit(1);
		}
	}

	private static void verifyInCurrentZone() throws Exception {
		// 08:00 on the 2026 US fall-back day prints as 07:00 in Los Angeles if that date's offset is applied.
		GregorianCalendar transitionDay = DateTime.calendarInstance(2026, Calendar.NOVEMBER, 1);
		transitionDay.set(Calendar.HOUR_OF_DAY, 8);
		Calendar local = Calendar.getInstance();
		local.setTime(MPXConverter.toMpxTimeOfDay(transitionDay));
		assertEquals(8, local.get(Calendar.HOUR_OF_DAY), "time of day on a DST transition date");

		Path fixture = Path.of("testdata", "Assignment Baseline Scalars.xml");
		if (!Files.isRegularFile(fixture))
			fixture = Path.of("modules", "microproject_exchange", "testdata", "Assignment Baseline Scalars.xml");
		Project project;
		try (InputStream input = Files.newInputStream(fixture)) {
			project = new MspImporter().importProject(input, "xml", (progress, label) -> { });
		}
		assertDefaultTimes(mspdi(project), "MSPDI export");
		byte[] mpo = mpo(project);
		assertDefaultTimes(contentXml(mpo), "MPO content.xml");

		Project reopened = new MpoFileImporter().loadProject(new ByteArrayInputStream(mpo));
		assertDefaultTimes(contentXml(mpo(reopened)), "MPO content.xml after reopen");
		assertDefaultTimes(mspdi(reopened), "MSPDI export after reopen");
	}

	private static void assertDefaultTimes(String xml, String route) {
		assertTrue(xml.contains("<DefaultStartTime>08:00:00</DefaultStartTime>"), route + " DefaultStartTime");
		assertTrue(xml.contains("<DefaultFinishTime>17:00:00</DefaultFinishTime>"), route + " DefaultFinishTime");
	}

	private static String mspdi(Project project) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		assertTrue(new MicrosoftImporter().saveProject(project, output));
		return output.toString(StandardCharsets.UTF_8);
	}

	private static byte[] mpo(Project project) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, output));
		return output.toByteArray();
	}

	private static String contentXml(byte[] mpo) throws Exception {
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(mpo), StandardCharsets.UTF_8)) {
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
				if ("content.xml".equals(entry.getName()))
					return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
		}
		throw new AssertionError("MPO archive has no content.xml");
	}

	private static String readLog(Path log) {
		try {
			return Files.readString(log, StandardCharsets.UTF_8);
		} catch (java.io.IOException e) {
			return "worker log unavailable: " + e;
		}
	}
}
