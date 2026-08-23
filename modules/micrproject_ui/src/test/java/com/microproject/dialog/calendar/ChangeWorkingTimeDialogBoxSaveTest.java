package com.microproject.dialog.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Regression test for issue #353: in the "Change Working Time" dialog the
 * day-type radio buttons (non-working / default / working) edited the scratch
 * calendar but never marked it for save, so OK silently discarded the change.
 *
 * Per the MS Project spec adopted in #353: OK commits every edit made inside
 * the dialog; Cancel discards them all.
 *
 * Two layers:
 * <ul>
 * <li>{@code sourceContractHolds} — runs headless; asserts the wiring contract:
 * every radio handler marks the dialog edited and OK's commit path consults
 * that flag.</li>
 * <li>{@code radioEditMarksCalendarEditedAndOkCommitsIt} — builds a real dialog
 * on the EDT and drives the flag → commit path end to end (skipped headless,
 * same as other dialog tests in this module).</li>
 * </ul>
 */
class ChangeWorkingTimeDialogBoxSaveTest {

	private static String source() {
		try {
			for (java.nio.file.Path current = java.nio.file.Path.of("").toAbsolutePath(); current != null; current = current
					.getParent()) {
				java.nio.file.Path candidate = current.resolve(
						"modules/micrproject_ui/src/main/java/com/microproject/dialog/calendar/ChangeWorkingTimeDialogBox.java")
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

		// OK's commit decision must consult the edited flag
		int saveIfNeeded = src.indexOf("public void saveIfNeeded()");
		assertTrue(saveIfNeeded > 0);
		String commitBlock = src.substring(saveIfNeeded,
				src.indexOf("}", src.indexOf("saveCalendar();", saveIfNeeded)) + 1);
		assertTrue(commitBlock.contains("calendarEdited"),
				"OK must commit when the calendar was edited (issue #353)");

		// and the commit must push the scratch copy back into the real calendar
		int saveCalendar = src.indexOf("private void saveCalendar()");
		String saveBody = src.substring(saveCalendar, src.indexOf("saveAndUpdate", saveCalendar));
		assertTrue(saveBody.contains("assignCalendar(editedCalendar"),
				"commit path must assign the scratch copy onto the edited calendar");
	}

	@Test
	void messagesResolveForRadioButtons() {
		for (String key : new String[] { "ChangeWorkingTimeDialogBox.UseDefault",
				"ChangeWorkingTimeDialogBox.NonWorkingTime",
				"ChangeWorkingTimeDialogBox.NonDefaultWorkingTime" }) {
			assertFalse(Messages.getString(key).startsWith("!"), key);
		}
	}

	@Test
	void radioEditMarksCalendarEditedAndOkCommitsIt() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"dialog construction needs a real graphics environment; skipped on headless CI");

		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		WorkingCalendar base = CalendarService.getInstance().getStandardInstance();

		SwingUtilities.invokeAndWait(() -> {
			ChangeWorkingTimeDialogBox dlg = ChangeWorkingTimeDialogBox.getInstance(null,
					project, base, null, false, undoController);

			// untouched dialog: OK must not push anything back
			dlg.saveIfNeeded();
			assertFalse(dlg.isCalendarCommitted());

			// simulate the radio handlers' postcondition: scratch modified + marked edited
			dlg.markCalendarEdited();
			dlg.saveIfNeeded();

			assertTrue(dlg.isCalendarCommitted(),
					"OK must commit a radio-button edit (issue #353: it was silently discarded)");
			assertEquals(base.getName(), dlg.getFormCalendarName(),
					"scratch copy content must be assigned back onto the edited calendar");
		});
	}
}
