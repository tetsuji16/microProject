package com.projectlibre1.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.projectlibre1.strings.Messages;

class GanttBarFormatDialogTest {
	@Test
	void colorLabelUsesSixDigitRgb() {
		assertEquals("#0012AB", GanttBarFormatDialog.colorLabel(0x0012AB));
		assertEquals("#CDEF01", GanttBarFormatDialog.colorLabel(0xFFCDEF01));
	}

	@Test
	void formatBarMessagesAreAvailable() {
		assertFalse(Messages.getString("Gantt.FormatBar.title").startsWith("!"));
		assertFalse(Messages.getString("Gantt.FormatBar.automatic").startsWith("!"));
	}
}
