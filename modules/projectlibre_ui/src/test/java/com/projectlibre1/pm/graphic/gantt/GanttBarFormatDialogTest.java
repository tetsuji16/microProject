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
		// The bar formatting tab exposes color selection (start/middle/end), so its
		// label must read "Bar Color", not "Bar Shape" (issue #16). Verify the English
		// resource directly so the assertion is independent of the test JVM locale.
		assertFalse(Messages.getString("Gantt.FormatBar.barColor").startsWith("!"));
		String english = readEnglishString("Gantt.FormatBar.barColor");
		assertEquals("Bar Color", english);
		assertFalse(Messages.getString("Gantt.FormatBar.reset").startsWith("!"));
	}

	private static String readEnglishString(String key) {
		try (java.io.InputStream in = GanttBarFormatDialogTest.class
				.getClassLoader()
				.getResourceAsStream("com/projectlibre1/strings/client.properties")) {
			java.util.Properties props = new java.util.Properties();
			props.load(in);
			return props.getProperty(key);
		} catch (java.io.IOException e) {
			throw new AssertionError("failed to load client.properties", e);
		}
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
