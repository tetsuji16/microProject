package com.projectlibre.core.pm.exchange;

import java.io.ByteArrayInputStream;

import com.projectlibre.core.pm.exchange.converters.mpx.MpxImportState;
import com.projectlibre.pm.calendar.DefaultWorkCalendar;

import junit.framework.TestCase;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectFile;

public class MspImporterTest extends TestCase {
	public void testUnknownExtensionFailsExplicitly() throws Exception {
		MspImporter importer = new MspImporter();

		try {
			importer.importProject(new ByteArrayInputStream(new byte[0]), "bogus", new MspImporter.ProgressClosure() {
				public void updateProgress(float progress, String label) {
				}
			});
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("Unsupported import extension"));
		}
	}

	public void testNullExtensionFailsExplicitly() throws Exception {
		MspImporter importer = new MspImporter();

		try {
			importer.importProject(new ByteArrayInputStream(new byte[0]), null, new MspImporter.ProgressClosure() {
				public void updateProgress(float progress, String label) {
				}
			});
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("Unsupported import extension"));
		}
	}

	public void testImportedCalendarRejectsNullInputs() {
		MpxImportState state = new MpxImportState();

		try {
			state.registerImportedCalendar(null, new ProjectCalendar(new ProjectFile()));
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("calendar"));
		}

		try {
			state.registerImportedCalendar(new DefaultWorkCalendar(), null);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("mpxCalendar"));
		}
	}
}
