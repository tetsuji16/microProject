/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.dialog;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.field.DynamicSelect;
import com.microproject.util.ClassUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Diagnostic: check whether DynamicSelect.setList() actually resolves the
 * allBaseCalendars list method when given the exact string from configuration.xml.
 */
class DynamicSelectSetListDiagnostic {

	@Test
	void checkSetListResolves() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "skip on headless CI");

		String list = "com.microproject.pm.calendar.CalendarService.allBaseCalendars";
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
