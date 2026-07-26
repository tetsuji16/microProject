package com.projectlibre1.graphic.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

import com.projectlibre1.graphic.configuration.GanttBarFormatOverrides.BarFormat;

class GanttBarFormatOverridesTest {
	@Test
	void storesFormattingByStableTaskIdAndView() {
		GanttBarFormatOverrides overrides = new GanttBarFormatOverrides();
		overrides.set(GanttBarFormatOverrides.STANDARD_VIEW, 42L, new BarFormat(null, 0x123456, null));
		overrides.set(GanttBarFormatOverrides.TRACKING_VIEW, 42L, new BarFormat(0x654321, null, null));

		assertEquals(0x123456, overrides.get(GanttBarFormatOverrides.STANDARD_VIEW, 42L).getMiddleRgb());
		assertEquals(0x654321, overrides.get(GanttBarFormatOverrides.TRACKING_VIEW, 42L).getStartRgb());
		assertTrue(overrides.get(GanttBarFormatOverrides.STANDARD_VIEW, 43L).isAutomatic());
	}

	@Test
	void automaticFormattingRemovesStoredOverride() {
		GanttBarFormatOverrides overrides = new GanttBarFormatOverrides();
		overrides.set(GanttBarFormatOverrides.STANDARD_VIEW, 42L, new BarFormat(null, 0x123456, null));
		overrides.set(GanttBarFormatOverrides.STANDARD_VIEW, 42L, BarFormat.automatic());

		assertTrue(overrides.isEmpty());
		assertTrue(overrides.get(GanttBarFormatOverrides.STANDARD_VIEW, 42L).isAutomatic());
	}

	@Test
	void normalizesRgbAndPreservesItAcrossSerialization() throws Exception {
		GanttBarFormatOverrides overrides = new GanttBarFormatOverrides();
		overrides.set(GanttBarFormatOverrides.STANDARD_VIEW, 7L, new BarFormat(0xFFABCDEF, null, 0x00123456));

		byte[] serialized;
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				ObjectOutputStream output = new ObjectOutputStream(bytes)) {
			output.writeObject(overrides);
			serialized = bytes.toByteArray();
		}

		GanttBarFormatOverrides restored;
		try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
			restored = (GanttBarFormatOverrides) input.readObject();
		}

		BarFormat format = restored.get(GanttBarFormatOverrides.STANDARD_VIEW, 7L);
		assertEquals(0xABCDEF, format.getStartRgb());
		assertEquals(0x123456, format.getEndRgb());
		assertNull(format.getMiddleRgb());
	}
}
