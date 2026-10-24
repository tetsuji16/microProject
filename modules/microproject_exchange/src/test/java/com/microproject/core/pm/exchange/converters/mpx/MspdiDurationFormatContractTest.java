/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.core.pm.exchange.converters.mpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.exchange.MicrosoftImporter;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;

/**
 * Document-derived MSPDI duration contracts using hand-authored values.
 * Microsoft DurationFormat defines codes 3/5/7/9/11 as working units and
 * 4/6/8/10/12 as elapsed units:
 * https://learn.microsoft.com/en-us/office-project/xml-data-interchange/durationformat-element?view=project-client-2016
 * MSPDI does not define an elapsed-month day count; code 12 preserves the
 * supplied ISO clock quantity and the application's pinned 30-day conversion.
 */
class MspdiDurationFormatContractTest {
	private static final List<DurationCase> CASES = List.of(
		new DurationCase(3, "working-minute", "PT90M0S", "2030-10-07T09:30:00", 90.0D,
			com.microproject.datatype.TimeUnit.MINUTES, 90L * 60_000L),
		new DurationCase(5, "working-hour", "PT2H0M0S", "2030-10-07T10:00:00", 2.0D,
			com.microproject.datatype.TimeUnit.HOURS, 2L * 3_600_000L),
		new DurationCase(7, "working-day", "PT16H0M0S", "2030-10-08T17:00:00", 2.0D,
			com.microproject.datatype.TimeUnit.DAYS, 16L * 3_600_000L),
		new DurationCase(9, "working-week", "PT80H0M0S", "2030-10-18T17:00:00", 2.0D,
			com.microproject.datatype.TimeUnit.WEEKS, 80L * 3_600_000L),
		new DurationCase(11, "working-month", "PT320H0M0S", "2030-11-29T17:00:00", 2.0D,
			com.microproject.datatype.TimeUnit.MONTHS, 320L * 3_600_000L),
		new DurationCase(4, "elapsed-minute", "PT90M0S", "2030-10-07T09:30:00", 90.0D,
			com.microproject.datatype.TimeUnit.ELAPSED_MINUTES, 90L * 60_000L),
		new DurationCase(6, "elapsed-hour", "PT2H0M0S", "2030-10-07T10:00:00", 2.0D,
			com.microproject.datatype.TimeUnit.ELAPSED_HOURS, 2L * 3_600_000L),
		new DurationCase(8, "elapsed-day", "PT48H0M0S", "2030-10-09T08:00:00", 2.0D,
			com.microproject.datatype.TimeUnit.ELAPSED_DAYS, 48L * 3_600_000L),
		new DurationCase(10, "elapsed-week", "PT336H0M0S", "2030-10-21T08:00:00", 2.0D,
			com.microproject.datatype.TimeUnit.ELAPSED_WEEKS, 336L * 3_600_000L),
		new DurationCase(12, "elapsed-month", "PT1440H0M0S", "2030-12-06T08:00:00", 2.0D,
			com.microproject.datatype.TimeUnit.ELAPSED_MONTHS, 1440L * 3_600_000L));

	@Test
	void literalTaskAndBaselineDurationsKeepFormatSemanticsThroughXmlAndMpo() throws Exception {
		String source = literalProject();
		Project imported = importMspdi(source);
		for (DurationCase contract : CASES) assertTaskDuration(imported, contract);

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(imported, mpo));
		Project reopened = new MpoFileImporter().loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		for (DurationCase contract : CASES) assertTaskDuration(reopened, contract);

		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName("duration-format-contract.xml");
		assertTrue(exporter.saveProject(reopened, xml));
		String emitted = xml.toString(StandardCharsets.UTF_8);
		net.sf.mpxj.ProjectFile emittedFile = new net.sf.mpxj.mspdi.MSPDIReader().read(
			new ByteArrayInputStream(xml.toByteArray()));
		for (DurationCase contract : CASES) assertMpxjDuration(emittedFile, contract);
		Project reimported = importMspdi(emitted);
		for (DurationCase contract : CASES) assertSemanticDuration(reimported, contract);
	}

	@Test
	void malformedBaselineDurationIsRejectedWithoutCompletingOrMutatingInput() throws Exception {
		String source = literalProject().replace(
			"<Duration>PT90M0S</Duration><DurationFormat>3</DurationFormat></Baseline>",
			"<Duration>P</Duration><DurationFormat>3</DurationFormat></Baseline>");
		byte[] original = source.getBytes(StandardCharsets.UTF_8);
		byte[] supplied = original.clone();
		java.util.ArrayList<String> progressLabels = new java.util.ArrayList<>();
		assertThrows(IllegalArgumentException.class, () -> new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(supplied), "xml", (progress, label) -> progressLabels.add(label)));
		assertTrue(java.util.Arrays.equals(original, supplied), "malformed input bytes remain unchanged");
		assertTrue(!progressLabels.contains("Completed"), "malformed import must not report completed publication");
	}

	private static void assertTaskDuration(Project project, DurationCase contract) {
		NormalTask task = taskNamed(project, contract.name());
		assertDuration(task.getCurrentSchedule().getRawDuration(), contract, "task " + contract.name());
		assertDuration(task.getBaselineDuration(0), contract, "baseline " + contract.name());
		assertEquals(wallTime("2030-10-07T08:00:00"), task.getCurrentSchedule().getStart());
		assertEquals(wallTime(contract.finish()), task.getCurrentSchedule().getFinish());
		boolean elapsed = contract.expectedUnit() >= com.microproject.datatype.TimeUnit.ELAPSED_MINUTES;
		if (elapsed) {
			assertEquals(contract.expectedClockMillis(), task.getCurrentSchedule().getFinish()
				- task.getCurrentSchedule().getStart(), "elapsed source bounds " + contract.name());
		} else {
			assertEquals(contract.expectedClockMillis(), task.getEffectiveWorkCalendar().compare(
				task.getCurrentSchedule().getFinish(), task.getCurrentSchedule().getStart(), false),
				"working-time source bounds from explicit 08:00-12:00/13:00-17:00 calendar " + contract.name());
		}
	}

	private static void assertDuration(long actual, DurationCase contract, String context) {
		assertEquals(contract.expectedUnit(), com.microproject.datatype.Duration.getEffectiveType(actual), context + " unit");
		assertEquals(contract.expectedValue(), com.microproject.datatype.Duration.getValue(actual),
			context + " source quantity");
		assertEquals(contract.expectedClockMillis(), com.microproject.datatype.Duration.millis(actual), context + " ISO clock quantity");
	}

	private static void assertSemanticDuration(Project project, DurationCase contract) {
		NormalTask task = taskNamed(project, contract.name());
		assertEquals(contract.expectedClockMillis(),
			com.microproject.datatype.Duration.millis(task.getCurrentSchedule().getRawDuration()), "task clock " + contract.name());
		assertEquals(contract.expectedClockMillis(),
			com.microproject.datatype.Duration.millis(task.getBaselineDuration(0)), "baseline clock " + contract.name());
		boolean elapsed = contract.expectedUnit() >= com.microproject.datatype.TimeUnit.ELAPSED_MINUTES;
		assertEquals(elapsed, com.microproject.datatype.Duration.isElapsed(task.getCurrentSchedule().getRawDuration()),
			"task elapsed identity " + contract.name());
		assertEquals(elapsed, com.microproject.datatype.Duration.isElapsed(task.getBaselineDuration(0)),
			"baseline elapsed identity " + contract.name());
	}

	private static void assertMpxjDuration(net.sf.mpxj.ProjectFile file, DurationCase contract) {
		net.sf.mpxj.Task task = file.getTasks().stream().filter(candidate -> contract.name().equals(candidate.getName()))
			.findFirst().orElseThrow();
		assertMpxjDuration(task.getDuration(), file, contract, "task");
		assertMpxjDuration(task.getBaselineDuration(), file, contract, "baseline");
	}

	private static void assertMpxjDuration(net.sf.mpxj.Duration duration, net.sf.mpxj.ProjectFile file,
		DurationCase contract, String context) {
		assertTrue(duration.getUnits().isElapsed() == (contract.expectedUnit()
			>= com.microproject.datatype.TimeUnit.ELAPSED_MINUTES), context + " elapsed identity " + contract.name());
		net.sf.mpxj.TimeUnit minuteUnit = duration.getUnits().isElapsed()
			? net.sf.mpxj.TimeUnit.ELAPSED_MINUTES : net.sf.mpxj.TimeUnit.MINUTES;
		double actualMillis = duration.convertUnits(minuteUnit, file.getProjectProperties()).getDuration() * 60_000.0D;
		assertEquals((double) contract.expectedClockMillis(), actualMillis, context + " ISO clock quantity " + contract.name());
	}

	private static NormalTask taskNamed(Project project, String name) {
		return project.getTaskList().stream().filter(candidate -> name.equals(candidate.getName()))
			.map(NormalTask.class::cast).findFirst().orElseThrow();
	}

	private static Project importMspdi(String xml) throws Exception {
		return new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "xml", (progress, label) -> {});
	}

	private static long wallTime(String wallTime) {
		return java.time.LocalDateTime.parse(wallTime).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
	}

	private static String literalProject() {
		StringBuilder xml = new StringBuilder("""
			<Project xmlns="http://schemas.microsoft.com/project">
			  <Name>Duration format contract</Name><ScheduleFromStart>1</ScheduleFromStart>
			  <StartDate>2030-10-07T08:00:00</StartDate><CalendarUID>1</CalendarUID>
			  <DefaultStartTime>08:00:00</DefaultStartTime><DefaultFinishTime>17:00:00</DefaultFinishTime>
			  <MinutesPerDay>480</MinutesPerDay><MinutesPerWeek>2400</MinutesPerWeek><DaysPerMonth>20</DaysPerMonth>
			  <DurationFormat>7</DurationFormat>
			  <Calendars><Calendar><UID>1</UID><Name>Literal standard</Name><IsBaseCalendar>1</IsBaseCalendar><BaseCalendarUID>-1</BaseCalendarUID>
			    <WeekDays>
			      <WeekDay><DayType>1</DayType><DayWorking>0</DayWorking></WeekDay>
			      <WeekDay><DayType>2</DayType><DayWorking>1</DayWorking><WorkingTimes><WorkingTime><FromTime>08:00:00</FromTime><ToTime>12:00:00</ToTime></WorkingTime><WorkingTime><FromTime>13:00:00</FromTime><ToTime>17:00:00</ToTime></WorkingTime></WorkingTimes></WeekDay>
			      <WeekDay><DayType>3</DayType><DayWorking>1</DayWorking><WorkingTimes><WorkingTime><FromTime>08:00:00</FromTime><ToTime>12:00:00</ToTime></WorkingTime><WorkingTime><FromTime>13:00:00</FromTime><ToTime>17:00:00</ToTime></WorkingTime></WorkingTimes></WeekDay>
			      <WeekDay><DayType>4</DayType><DayWorking>1</DayWorking><WorkingTimes><WorkingTime><FromTime>08:00:00</FromTime><ToTime>12:00:00</ToTime></WorkingTime><WorkingTime><FromTime>13:00:00</FromTime><ToTime>17:00:00</ToTime></WorkingTime></WorkingTimes></WeekDay>
			      <WeekDay><DayType>5</DayType><DayWorking>1</DayWorking><WorkingTimes><WorkingTime><FromTime>08:00:00</FromTime><ToTime>12:00:00</ToTime></WorkingTime><WorkingTime><FromTime>13:00:00</FromTime><ToTime>17:00:00</ToTime></WorkingTime></WorkingTimes></WeekDay>
			      <WeekDay><DayType>6</DayType><DayWorking>1</DayWorking><WorkingTimes><WorkingTime><FromTime>08:00:00</FromTime><ToTime>12:00:00</ToTime></WorkingTime><WorkingTime><FromTime>13:00:00</FromTime><ToTime>17:00:00</ToTime></WorkingTime></WorkingTimes></WeekDay>
			      <WeekDay><DayType>7</DayType><DayWorking>0</DayWorking></WeekDay>
			    </WeekDays>
			  </Calendar></Calendars>
			  <Tasks>
			""");
		int uid = 1;
		for (DurationCase contract : CASES) {
			xml.append("<Task><UID>").append(uid).append("</UID><ID>").append(uid).append("</ID><Name>")
				.append(contract.name()).append("</Name><Type>1</Type><Manual>1</Manual>")
				.append("<Start>2030-10-07T08:00:00</Start><Finish>").append(contract.finish())
				.append("</Finish><Duration>").append(contract.isoDuration()).append("</Duration><DurationFormat>")
				.append(contract.code()).append("</DurationFormat><ConstraintType>2</ConstraintType>")
				.append("<Baseline><Number>0</Number><Start>2030-10-07T08:00:00</Start><Finish>")
				.append(contract.finish()).append("</Finish><Duration>").append(contract.isoDuration())
				.append("</Duration><DurationFormat>").append(contract.code()).append("</DurationFormat></Baseline></Task>");
			uid++;
		}
		return xml.append("</Tasks></Project>").toString();
	}

	private record DurationCase(int code, String name, String isoDuration, String finish, double expectedValue,
		int expectedUnit, long expectedClockMillis) { }
}
