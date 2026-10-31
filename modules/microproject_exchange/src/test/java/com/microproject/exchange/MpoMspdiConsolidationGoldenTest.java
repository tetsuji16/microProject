/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;

/**
 * Golden bytes for the MPO and MSPDI writers (issue #982). The digests were captured on the
 * writers before the duplicated calendar / leveling / text-style / baseline-row code was
 * consolidated; a mismatch means the consolidation changed a persisted byte, not just a path.
 * A single small project covers base and derived calendars with exceptions, task leveling
 * options, task text styles, and assignment baseline work and cost rows.
 */
class MpoMspdiConsolidationGoldenTest {
	private static final String EXCEPTION = "<Exceptions><Exception><EnteredByOccurrences>0</EnteredByOccurrences>"
		+ "<TimePeriod><FromDate>2026-10-14T00:00:00</FromDate><ToDate>2026-10-14T23:59:00</ToDate></TimePeriod>"
		+ "<Occurrences>1</Occurrences><Name>%s</Name><Type>1</Type><DayWorking>0</DayWorking></Exception></Exceptions>";

	/** SHA-256 per MPO entry (zip entry names sorted) and for the MSPDI export, captured pre-consolidation. */
	private static final Map<String, String> GOLDEN = new TreeMap<>(Map.ofEntries(
		Map.entry("mpo:META-INF/manifest.xml",
			"61d8a018338fcd46de53980af75bec4d1e29cb7f9262ac02195548cfaa1ab524"),
		Map.entry("mpo:ccpm/history.jsonl",
			"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
		Map.entry("mpo:changes/task-identities.json",
			"002110d1d9da8da95128c8ade4ab33f62ab5a02b247bbc4a8d229a820d9a4b88"),
		Map.entry("mpo:content.xml",
			"b10e651f23935ac056389fbd787931f5956b53c468a66f3cc29db696a9817f48"),
		Map.entry("mpo:meta.xml",
			"9e1c3c32896f620ab0b937a329af41c59eb219d98f14c7641116e33d15601f08"),
		Map.entry("mpo:microproject/assignment-leveling-splits.json",
			"d08651ac0d6ba728e312fd8a690ff3717bde335bf7876fe7f6677eecd5fd6171"),
		Map.entry("mpo:microproject/baseline-profiles.json",
			"9de99943b2136eef7bcbd94d6e65b5e8c87b54a2ad7f084c5f632846e168fd90"),
		Map.entry("mpo:microproject/task-leveling-options.json",
			"8943e99b4d802623f46df4dec8dc4669a4596e4bd9877703469bab7319662ba2"),
		Map.entry("mpo:microproject/task-schedules.json",
			"191d2c6f7c6345ca55fa6dcc514ca6ed9ae951f06d39b1d7a9e461de0b06fcb5"),
		Map.entry("mpo:microproject/task-splits.json",
			"6b71b36759c13432a556fba204a6fd583716f1c78918ee0a5fffbf9180a9e6ab"),
		Map.entry("mpo:microproject/task-text-styles.json",
			"fa1bb1b3b29284848726f9aca286fbc8835c4fe7d91d8703b8b5957e3386f733"),
		Map.entry("mpo:microproject/visibility.json",
			"1d4493b16fabcc3b126d078a572fe359f791b3a521b0c5c33c725d4f0eb0a58f"),
		Map.entry("mpo:mimetype",
			"d20065074eed50aba9f9f5755528d78e8977bd0c011d4999fc60a4de492540af"),
		Map.entry("mpo:operations/log.jsonl",
			"c1d9b1e6d62e1ab9b3381951aac7ab482aef8d3533925e09cba13591cbeac8d8"),
		Map.entry("mpo:settings.xml",
			"d4cafce40b45ca0550508c61ad15e3441d687d23779448cf83c8f50cc7fae8b4"),
		Map.entry("mspdi",
			"b10e651f23935ac056389fbd787931f5956b53c468a66f3cc29db696a9817f48")));

	@Test
	void mpoAndMspdiBytesAreDeterministicAndMatchGolden() throws Exception {
		Project project = fixtureProject();
		byte[] mpo = mpo(project);
		byte[] mspdi = mspdi(project);

		Map<String, String> actual = digests(mpo, mspdi);
		assertEquals(actual, digests(mpo(project), mspdi(project)),
			"output must be deterministic across saves of one model once the export clock is masked");
		// Guard that the fixture really exercises the consolidated writers.
		assertTrue(actual.keySet().stream().anyMatch(name -> name.contains("level")), actual.keySet().toString());
		assertTrue(actual.keySet().stream().anyMatch(name -> name.contains("style")), actual.keySet().toString());
		String xml = new String(mspdi, StandardCharsets.UTF_8);
		assertTrue(xml.contains("<Exception>"), "MSPDI export carries calendar exceptions");
		assertTrue(xml.contains("<Type>4</Type>") && xml.contains("<Type>5</Type>"),
			"MSPDI export carries baseline work and cost timephased rows");

		assertEquals(GOLDEN, actual);
	}

	private static Map<String, String> digests(byte[] mpo, byte[] mspdi) throws Exception {
		Map<String, String> actual = new TreeMap<>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(mpo), StandardCharsets.UTF_8)) {
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
				actual.put("mpo:" + entry.getName(), sha256(withoutClock(zip.readAllBytes())));
		}
		actual.put("mspdi", sha256(withoutClock(mspdi)));
		return actual;
	}

	private static Project fixtureProject() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "Assignment Baseline Scalars.xml");
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "Assignment Baseline Scalars.xml");
		String xml = Files.readString(fixture, StandardCharsets.UTF_8)
			.replace("2026-10-12T17:00:00", "2026-10-13T17:00:00")
			.replace("<Duration>PT8H0M0S</Duration><DurationFormat>7", "<Duration>PT16H0M0S</Duration><DurationFormat>7");
		// Calendar exceptions: one on the base calendar, one on a derived task calendar.
		int baseWeekDaysEnd = xml.indexOf("</WeekDays>") + "</WeekDays>".length();
		xml = xml.substring(0, baseWeekDaysEnd) + String.format(EXCEPTION, "Base holiday") + xml.substring(baseWeekDaysEnd);
		xml = xml.replace("</Calendars>", "<Calendar><UID>2</UID><Name>Task calendar</Name><IsBaseCalendar>0</IsBaseCalendar>"
			+ "<BaseCalendarUID>1</BaseCalendarUID><WeekDays><WeekDay><DayType>4</DayType><DayWorking>1</DayWorking>"
			+ "<WorkingTimes><WorkingTime><FromTime>09:00:00</FromTime><ToTime>13:00:00</ToTime></WorkingTime>"
			+ "</WorkingTimes></WeekDay></WeekDays>" + String.format(EXCEPTION, "Task holiday") + "</Calendar></Calendars>");
		xml = xml.replace("<Milestone>0</Milestone><CalendarUID>1</CalendarUID>",
			"<Milestone>0</Milestone><CalendarUID>2</CalendarUID>");
		int assignmentStart = xml.indexOf("<Assignment>");
		int assignmentEnd = xml.indexOf("</Assignment>", assignmentStart);
		String assignmentXml = xml.substring(assignmentStart, assignmentEnd);
		assignmentXml = assignmentXml.replace("<Units>1</Units>", "<Units>1</Units><WorkContour>8</WorkContour>"
			+ "<TimephasedData><Type>1</Type><UID>1</UID><Start>2026-10-12T08:00:00</Start><Finish>2026-10-12T17:00:00</Finish><Unit>2</Unit><Value>PT2H0M0S</Value></TimephasedData>"
			+ "<TimephasedData><Type>1</Type><UID>2</UID><Start>2026-10-13T08:00:00</Start><Finish>2026-10-13T17:00:00</Finish><Unit>2</Unit><Value>PT6H0M0S</Value></TimephasedData>");
		xml = xml.substring(0, assignmentStart) + assignmentXml + xml.substring(assignmentEnd);
		xml = xml.replace("<Baseline><Number>0</Number><Start>2026-10-05T17:00:00</Start><Work>PT8H0M0S</Work></Baseline>",
			"<Baseline><TimephasedData><Type>4</Type><UID>1</UID><Start>2026-10-05T08:00:00</Start>"
				+ "<Finish>2026-10-05T17:00:00</Finish><Unit>2</Unit><Value>PT2H0M0S</Value></TimephasedData>"
				+ "<TimephasedData><Type>4</Type><UID>1</UID><Start>2026-10-06T08:00:00</Start>"
				+ "<Finish>2026-10-06T17:00:00</Finish><Unit>2</Unit><Value>PT6H0M0S</Value></TimephasedData>"
				+ "<TimephasedData><Type>5</Type><UID>1</UID><Start>2026-10-05T08:00:00</Start>"
				+ "<Finish>2026-10-05T17:00:00</Finish><Unit>2</Unit><Value>0.1</Value></TimephasedData>"
				+ "<TimephasedData><Type>5</Type><UID>1</UID><Start>2026-10-06T08:00:00</Start>"
				+ "<Finish>2026-10-06T17:00:00</Finish><Unit>2</Unit><Value>0.3</Value></TimephasedData>"
				+ "<Number>0</Number><Start>2026-10-05T08:00:00</Start><Finish>2026-10-06T17:00:00</Finish>"
				+ "<Work>PT8H0M0S</Work></Baseline>");
		Project project = new com.microproject.core.pm.exchange.MspImporter().importProject(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "xml", (progress, label) -> {});
		Task task = null;
		for (Task candidate : project.getTaskList())
			if ("Assignment baseline task".equals(candidate.getName())) task = candidate;
		// The document id is random per project and the serializer stamps CurrentDate; pin both.
		project.setDocumentId("00000000-0000-4000-8000-000000000982");
		task.setLevelingCanSplit(false);
		task.setLevelAssignments(false);
		task.setFontFamily("Dialog");
		task.setFontSize(15);
		task.setFontColor(Integer.valueOf(0x2457A6));
		task.setFontBold(true);
		task.setFontItalic(true);
		task.setFontStrikethrough(true);
		Assignment assignment = (Assignment) ((com.microproject.pm.task.NormalTask) task).getAssignments().iterator().next();
		assertTrue(assignment.getBaselineAssignment(Integer.valueOf(0), false).getWorkContour().isPersonal());
		return project;
	}

	private static byte[] mpo(Project project) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, output));
		return output.toByteArray();
	}

	private static byte[] mspdi(Project project) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		assertTrue(new MicrosoftImporter().saveProject(project, output));
		return output.toByteArray();
	}

	/**
	 * CurrentDate is the export wall clock. The manifest records the digest of content.xml as written
	 * (clock included), so that digest is masked, and likewise the baseline-profiles digest. profileUid comes from a
	 * process-wide id counter and depends on which tests ran first, so it is masked too. XML line ends follow the OS.
	 * The application version comes from the build (-PreleaseVersion), so it and the meta.xml checksum are masked.
	 */
	private static byte[] withoutClock(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8).replaceAll("<CurrentDate>[^<]*</CurrentDate>", "")
			.replace("\r\n", "\n")
			.replaceAll("\"profileUid\":-?\\d+", "\"profileUid\":0")
			.replaceAll("projectUniqueId=\"-?[0-9]+\"", "projectUniqueId=\"0\"")
			.replaceAll(" applicationVersion=\"[^\"]*\"", "")
			.replaceAll("projectSha256=\"[0-9a-f]+\"", "projectSha256=\"\"")
			.replaceAll("(path=\"(?:content.xml|meta.xml|microproject/baseline-profiles.json)\" sha256=)\"[0-9a-f]+\"", "$1\"\"")
			.getBytes(StandardCharsets.UTF_8);
	}

	private static String sha256(byte[] bytes) throws Exception {
		return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}
}
