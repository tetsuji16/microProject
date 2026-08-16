package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.GanttBarFormatOverrides.BarFormat;
import com.microproject.pm.graphic.gantt.BarColorField;
import com.microproject.strings.Messages;

class GanttBarFormatDialogTest {
	@Test
	void colorLabelUsesSixDigitRgb() {
		assertEquals("#0012AB", GanttBarFormatDialog.colorLabel(0x0012AB));
		assertEquals("#CDEF01", GanttBarFormatDialog.colorLabel(0xFFCDEF01));
	}

	@Test
	void barColorFieldRgbRoundTripsThroughAutomatic() {
		// null means "Automatic" and must round-trip, since Task Information reuses
		// the same field and the renderer falls back to the view palette (issue #16).
		BarColorField field = new BarColorField(null, null, () -> { });
		assertEquals(null, field.getRgb());
		field.setRgb(0xFF0033);
		assertEquals(Integer.valueOf(0xFF0033), field.getRgb());
		field.setRgb(null);
		assertEquals(null, field.getRgb());
	}

	@Test
	void barColorFieldDoesNotNotifyWhileItIsBeingConstructed() {
		AtomicInteger changes = new AtomicInteger();
		BarColorField field = new BarColorField(null, 0xFF0033, changes::incrementAndGet);

		assertEquals(0, changes.get(), "initial model state is not a user edit");
		field.setRgb(0x00AA55);
		assertEquals(1, changes.get());
	}

	@Test
	void disablingAColorFieldDisablesEveryEditableChild() {
		BarColorField field = new BarColorField(null, 0xFF0033, () -> { });

		field.setEnabled(false);

		assertFalse(field.isEnabled());
		assertFalse(field.getSwatchButton().isEnabled());
		assertFalse(field.getAutomaticCheckBox().isEnabled());
		field.setEnabled(true);
		assertTrue(field.getSwatchButton().isEnabled());
		assertTrue(field.getAutomaticCheckBox().isEnabled());
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
				.getResourceAsStream("com/microproject/strings/client.properties")) {
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

		assertEquals(3, countComponents(panel, BarColorField.class));
		assertEquals(1, countComponents(panel, BarColorEditorPanel.class));
		assertTrue(allComponentsEnabled(panel, BarColorField.class));
		assertEquals(1, countComponents(panel, JTabbedPane.class));
		assertTrue(panel.getPreferredSize().width > 420);
	}

	@Test
	void automaticFormatBarColorsUseTheRendererResolvedColors() {
		JPanel panel = GanttBarFormatDialog.createPanelForTest(
				new BarFormat(null, null, null),
				new GanttRenderer.DisplayedBarColors(0x112233, 0x445566, 0x778899),
				false,
				false);

		List<BarColorField> fields = findComponents(panel, BarColorField.class);
		assertEquals(3, fields.size());
		assertEquals(null, fields.get(0).getRgb());
		assertEquals(null, fields.get(1).getRgb());
		assertEquals(null, fields.get(2).getRgb());
		assertEquals(Integer.valueOf(0x112233), fields.get(0).getDisplayRgb());
		assertEquals(Integer.valueOf(0x445566), fields.get(1).getDisplayRgb());
		assertEquals(Integer.valueOf(0x778899), fields.get(2).getDisplayRgb());
	}

	@Test
	void taskInformationColorEditsRemainLocalUntilConfirmed() {
		BarFormat original = new BarFormat(0x112233, 0x445566, 0x778899);
		BarColorEditorPanel editor = new BarColorEditorPanel(null, original, false, false, null);

		editor.getStart().setRgb(0xAABBCC);
		editor.getMiddle().setRgb(null);
		editor.getEnd().setRgb(0x010203);

		// Task Information applies editor.getFormat() only from its OK binding.
		// Editing its local fields must therefore leave the pre-dialog format intact
		// when the dialog is cancelled (issue #27).
		assertEquals(Integer.valueOf(0x112233), original.getStartRgb());
		assertEquals(Integer.valueOf(0x445566), original.getMiddleRgb());
		assertEquals(Integer.valueOf(0x778899), original.getEndRgb());
		assertEquals(Integer.valueOf(0xAABBCC), editor.getFormat().getStartRgb());
		assertEquals(null, editor.getFormat().getMiddleRgb());
		assertEquals(Integer.valueOf(0x010203), editor.getFormat().getEndRgb());
	}

	@Test
	void barColorControlsExposeTheirRoleToAssistiveTechnology() {
		JPanel panel = GanttBarFormatDialog.createPanelForTest(
				new BarFormat(null, null, null),
				false,
				false);

		List<BarColorField> fields = findComponents(panel, BarColorField.class);
		assertEquals(3, fields.size());
		String startColor = Messages.getString("Gantt.FormatBar.startColor"); //$NON-NLS-1$
		String chooseColor = Messages.getString("Gantt.FormatBar.chooseColor"); //$NON-NLS-1$
		String automatic = Messages.getString("Gantt.FormatBar.automatic"); //$NON-NLS-1$
		assertEquals(chooseColor + ": " + startColor,
				fields.get(0).getSwatchButton().getAccessibleContext().getAccessibleName());
		assertEquals(startColor + ": " + automatic,
				fields.get(0).getAutomaticCheckBox().getAccessibleContext().getAccessibleName());
		assertEquals(fields.get(0).getSwatchButton().getToolTipText(),
				fields.get(0).getSwatchButton().getAccessibleContext().getAccessibleDescription());
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

	private static <T> List<T> findComponents(Container root, Class<T> type) {
		List<T> result = new ArrayList<>();
		for (Component component : root.getComponents()) {
			if (type.isInstance(component))
				result.add(type.cast(component));
			if (component instanceof Container container)
				result.addAll(findComponents(container, type));
		}
		return result;
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
