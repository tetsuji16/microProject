package com.projectlibre1.dialog;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.projectlibre1.field.DynamicSelect;
import com.projectlibre1.util.ClassUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Diagnostic: check whether DynamicSelect.setList() actually resolves the
 * allBaseCalendars list method when given the exact string from configuration.xml.
 */
class DynamicSelectSetListDiagnostic {

	@Test
	void checkSetListResolves() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "skip on headless CI");

		String list = "com.projectlibre1.pm.calendar.CalendarService.allBaseCalendars";
		DynamicSelect sel = new DynamicSelect();
		sel.setList(list);

		java.lang.reflect.Field lm = DynamicSelect.class.getDeclaredField("listMethod");
		lm.setAccessible(true);
		Object resolved = lm.get(sel);
		System.out.println("AFTER setList('" + list + "') -> listMethod=" + resolved);

		// Also test with surrounding whitespace like XML might produce
		DynamicSelect sel2 = new DynamicSelect();
		sel2.setList("  " + list + "  ");
		Object resolved2 = lm.get(sel2);
		System.out.println("AFTER setList('  <ws>  ') -> listMethod=" + resolved2);

		assertNotNull(resolved, "listMethod should resolve for clean string");
	}
}
