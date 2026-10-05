package com.microproject.dialog.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.strings.Messages;

/**
 * Regression test for issue #353: in the "Change Working Time" dialog the
 * day-type radio buttons (non-working / default / working) edited the scratch
 * calendar but never marked it for save, so OK silently discarded the change.
 *
 * Per the MS Project spec adopted in #353: OK commits every edit made inside
 * the dialog; Cancel discards them all.
 *
 * The source-level wiring check covers all three radio handlers. The physical
 * date-selection, radio-button, and parent-OK path is exercised by
 * {@code ChangeWorkingTimeDialogGuiAcceptanceTest}.
 */
class ChangeWorkingTimeDialogBoxSaveTest {

	private static String source() {
		try {
			for (java.nio.file.Path current = java.nio.file.Path.of("").toAbsolutePath(); current != null; current = current
					.getParent()) {
				java.nio.file.Path candidate = current.resolve(
						"modules/microproject_ui/src/main/java/com/microproject/dialog/calendar/ChangeWorkingTimeDialogBox.java")
						.normalize();
				if (java.nio.file.Files.exists(candidate)) {
					return java.nio.file.Files.readString(candidate);
				}
			}
		} catch (Exception e) {
			throw new AssertionError(e);
		}
		throw new AssertionError("ChangeWorkingTimeDialogBox.java not found");
	}

	@Test
	void sourceContractHolds() {
		String src = source();
		int initControls = src.indexOf("protected void initControls()");
		assertTrue(initControls > 0);

		// each of the three day-type radio handlers must mark the dialog edited
		for (String handler : new String[] { "defaultWorkingTime.addActionListener",
				"nonWorking.addActionListener", "working.addActionListener" }) {
			int a = src.indexOf(handler, initControls);
			assertTrue(a > 0, handler + " missing");
			int b = src.indexOf("}});", a);
			String body = src.substring(a, b);
			assertFalse(body.contains("dirtyWorkingHours"), handler + " still touches legacy flag");
			assertTrue(body.contains("markCalendarEdited()"),
					handler + " must mark the calendar edited (issue #353)");
		}

		// Edits are staged by calendar identity and assigned only on parent OK.
		int saveIfNeeded = src.indexOf("public boolean saveIfNeeded()");
		assertTrue(saveIfNeeded > 0);
		assertTrue(src.contains("Map<WorkingCalendar, WorkingCalendar> stagedCalendars"),
				"switching calendars must retain isolated edits until parent OK");
		assertTrue(src.contains("if (!saveIfNeeded()) return;"),
				"invalid working-hour input must keep the parent dialog open");

		// Commit all staged copies as one undo transaction.
		int saveCalendar = src.indexOf("private boolean saveCalendar()");
		assertTrue(saveCalendar > 0);
		String saveBody = src.substring(saveCalendar, src.indexOf("private void importNonWorkingDays()", saveCalendar));
		assertTrue(saveBody.contains("new CompoundEdit()"), "calendar edits must undo in one step");
		assertTrue(saveBody.contains("service.assignCalendar(original, updated)"),
				"commit path must assign every staged calendar copy");

		int importNonWorkingDays = src.indexOf("private void importNonWorkingDays()");
		assertTrue(importNonWorkingDays > 0,
				"Change Working Time must expose the holiday/leave import route");
		String importBody = src.substring(importNonWorkingDays, src.indexOf("/**", importNonWorkingDays));
		assertTrue(importBody.contains("CalendarExceptionImporter.applyNonWorkingDates"),
				"import must create calendar non-working exceptions");
		assertTrue(importBody.contains("markCalendarEdited()"),
				"imported exceptions must be committed by OK");
	}

	@Test
	void messagesResolveForRadioButtons() {
		for (String key : new String[] { "ChangeWorkingTimeDialogBox.UseDefault",
				"ChangeWorkingTimeDialogBox.NonWorkingTime",
				"ChangeWorkingTimeDialogBox.NonDefaultWorkingTime",
				"ChangeWorkingTimeDialogBox.ImportNonWorkingDays" }) {
			assertFalse(Messages.getString(key).startsWith("!"), key);
		}
	}

}
