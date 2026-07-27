package com.projectlibre1.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.junit.jupiter.api.Test;

import com.projectlibre1.graphic.configuration.GanttBarFormatOverrides.BarFormat;
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
		assertFalse(Messages.getString("Gantt.FormatBar.barShape").startsWith("!"));
		assertFalse(Messages.getString("Gantt.FormatBar.reset").startsWith("!"));
	}

	@Test
	void formatPanelUsesThreeEnabledColorSelectorsAndSampleTab() {
		JPanel panel = GanttBarFormatDialog.createPanelForTest(
				new BarFormat(null, 0xFF0033, null),
				false,
				false);

		assertEquals(3, countComponents(panel, JComboBox.class));
		assertTrue(allComponentsEnabled(panel, JComboBox.class));
		assertEquals(1, countComponents(panel, JTabbedPane.class));
		assertTrue(panel.getPreferredSize().width > 420);
	}

	private static int countComponents(Container root, Class<?> type) {
		int count = 0;
		for (Component component : root.getComponents()) {
			if (type.isInstance(component))
				count++;
			if (component instanceof Container container)
				count += countComponents(container, type);
		}
		return count;
	}

	private static boolean allComponentsEnabled(Container root, Class<?> type) {
		for (Component component : root.getComponents()) {
			if (type.isInstance(component) && !component.isEnabled())
				return false;
			if (component instanceof Container container && !allComponentsEnabled(container, type))
				return false;
		}
		return true;
	}
}
